/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.core;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.channel.InvocationChannel;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.DefaultInvocationChannel.InvocationManager;
import com.github.dm.jrt.core.DefaultInvocationChannel.InvocationObserver;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationDeadlockException;
import com.github.dm.jrt.invocation.InvocationInterruptedException;
import com.github.dm.jrt.invocation.TemplateInvocation;
import com.github.dm.jrt.log.Logger;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.routine.TemplateRoutine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.runner.Runners;
import com.github.dm.jrt.runner.TemplateExecution;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Basic abstract implementation of a routine.
 * <p/>
 * This class provides a default implementation of all the routine features. The inheriting class
 * just needs to create invocation objects when required.
 * <p/>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class AbstractRoutine<IN, OUT> extends TemplateRoutine<IN, OUT> {

    private static final int DEFAULT_CORE_INVOCATIONS = 10;

    private static final int DEFAULT_MAX_INVOCATIONS = Integer.MAX_VALUE;

    private final LinkedList<Invocation<IN, OUT>> mAsyncInvocations =
            new LinkedList<Invocation<IN, OUT>>();

    private final Runner mAsyncRunner;

    private final InvocationConfiguration mConfiguration;

    private final int mCoreInvocations;

    private final Logger mLogger;

    private final int mMaxInvocations;

    private final Object mMutex = new Object();

    private final SimpleQueue<InvocationObserver<IN, OUT>> mObservers =
            new SimpleQueue<InvocationObserver<IN, OUT>>();

    private final Object mParallelMutex = new Object();

    private final LinkedList<Invocation<IN, OUT>> mSyncInvocations =
            new LinkedList<Invocation<IN, OUT>>();

    private final Runner mSyncRunner;

    private volatile DefaultInvocationManager mAsyncManager;

    private AbstractRoutine<IN, OUT> mParallelRoutine;

    private int mRunningCount;

    private volatile DefaultInvocationManager mSyncManager;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     */
    @SuppressWarnings("ConstantConditions")
    protected AbstractRoutine(@Nonnull final InvocationConfiguration configuration) {

        mConfiguration = configuration;
        mSyncRunner = configuration.getSyncRunnerOr(Runners.queuedRunner());

        final int priority = configuration.getPriorityOr(InvocationConfiguration.DEFAULT);
        final Runner asyncRunner = configuration.getAsyncRunnerOr(Runners.sharedRunner());

        if (priority != InvocationConfiguration.DEFAULT) {

            mAsyncRunner = Runners.priorityRunner(asyncRunner).getRunner(priority);

        } else {

            mAsyncRunner = asyncRunner;
        }

        mMaxInvocations = configuration.getMaxInstancesOr(DEFAULT_MAX_INVOCATIONS);
        mCoreInvocations = configuration.getCoreInstancesOr(DEFAULT_CORE_INVOCATIONS);
        mLogger = configuration.newLogger(this);
        mLogger.dbg("building routine with configuration: %s", configuration);
    }

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     * @param syncRunner    the runner used for synchronous invocation.
     * @param asyncRunner   the runner used for asynchronous invocation.
     * @param logger        the logger instance.
     */
    private AbstractRoutine(@Nonnull final InvocationConfiguration configuration,
            @Nonnull final Runner syncRunner, @Nonnull final Runner asyncRunner,
            @Nonnull final Logger logger) {

        mConfiguration = configuration;
        mSyncRunner = syncRunner;
        mAsyncRunner = asyncRunner;
        mMaxInvocations = DEFAULT_MAX_INVOCATIONS;
        mCoreInvocations = DEFAULT_CORE_INVOCATIONS;
        mLogger = logger.subContextLogger(this);
    }

    @Nonnull
    public InvocationChannel<IN, OUT> asyncInvoke() {

        return invoke(InvocationType.ASYNC);
    }

    @Nonnull
    public InvocationChannel<IN, OUT> parallelInvoke() {

        synchronized (mParallelMutex) {

            mLogger.dbg("invoking routine: parallel");

            if (mParallelRoutine == null) {

                mParallelRoutine =
                        new AbstractRoutine<IN, OUT>(mConfiguration, mSyncRunner, mAsyncRunner,
                                                     mLogger) {

                            @Nonnull
                            @Override
                            protected Invocation<IN, OUT> newInvocation(
                                    @Nonnull final InvocationType type) {

                                return new ParallelInvocation<IN, OUT>(AbstractRoutine.this);
                            }
                        };
            }
        }

        return mParallelRoutine.asyncInvoke();
    }

    @Nonnull
    public InvocationChannel<IN, OUT> syncInvoke() {

        return invoke(InvocationType.SYNC);
    }

    @Override
    public void purge() {

        synchronized (mMutex) {

            final Logger logger = mLogger;
            final LinkedList<Invocation<IN, OUT>> syncInvocations = mSyncInvocations;

            for (final Invocation<IN, OUT> invocation : syncInvocations) {

                try {

                    invocation.onDestroy();

                } catch (final Throwable t) {

                    InvocationInterruptedException.ignoreIfPossible(t);
                    logger.wrn(t, "ignoring exception while destroying invocation instance");
                }
            }

            syncInvocations.clear();

            final LinkedList<Invocation<IN, OUT>> asyncInvocations = mAsyncInvocations;

            for (final Invocation<IN, OUT> invocation : asyncInvocations) {

                try {

                    invocation.onDestroy();

                } catch (final Throwable t) {

                    InvocationInterruptedException.ignoreIfPossible(t);
                    logger.wrn(t, "ignoring exception while destroying invocation instance");
                }
            }

            asyncInvocations.clear();
        }
    }

    /**
     * Converts an invocation instance to the specified type.
     *
     * @param invocation the invocation to convert.
     * @param type       the converted invocation type.
     * @return the converted invocation.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected Invocation<IN, OUT> convertInvocation(@Nonnull final Invocation<IN, OUT> invocation,
            @Nonnull final InvocationType type) {

        return invocation;
    }

    /**
     * Returns the routine logger.
     *
     * @return the logger instance.
     */
    @Nonnull
    protected Logger getLogger() {

        return mLogger;
    }

    /**
     * Creates a new invocation instance.
     *
     * @param type the invocation type.
     * @return the invocation instance.
     */
    @Nonnull
    protected abstract Invocation<IN, OUT> newInvocation(@Nonnull InvocationType type);

    @Nonnull
    private DefaultInvocationManager getInvocationManager(@Nonnull final InvocationType type) {

        if (type == InvocationType.ASYNC) {

            if (mAsyncManager == null) {

                mAsyncManager = new DefaultInvocationManager(type, mAsyncRunner, mAsyncInvocations,
                                                             mSyncInvocations);
            }

            return mAsyncManager;
        }

        if (mSyncManager == null) {

            mSyncManager = new DefaultInvocationManager(type, mSyncRunner, mSyncInvocations,
                                                        mAsyncInvocations);
        }

        return mSyncManager;
    }

    @Nonnull
    private InvocationChannel<IN, OUT> invoke(@Nonnull final InvocationType type) {

        final Logger logger = mLogger;
        logger.dbg("invoking routine: %s", type);
        final Runner runner = (type == InvocationType.ASYNC) ? mAsyncRunner : mSyncRunner;
        return new DefaultInvocationChannel<IN, OUT>(mConfiguration, getInvocationManager(type),
                                                     runner, logger);
    }

    /**
     * Invocation type enumeration.
     */
    protected enum InvocationType {

        SYNC,   // synchronous
        ASYNC   // asynchronous
    }

    /**
     * Implementation of an invocation handling parallel mode.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class ParallelInvocation<IN, OUT> extends TemplateInvocation<IN, OUT> {

        private final Routine<IN, OUT> mRoutine;

        /**
         * Constructor.
         *
         * @param routine the routine to invoke in parallel mode.
         */
        private ParallelInvocation(@Nonnull final Routine<IN, OUT> routine) {

            mRoutine = routine;
        }

        @Override
        public void onInput(final IN input, @Nonnull final ResultChannel<OUT> result) {

            result.pass(mRoutine.asyncCall(input));
        }
    }

    /**
     * Execution implementation used to delay the creation of invocations.
     */
    private class CreateExecution extends TemplateExecution {

        private final DefaultInvocationManager mManager;

        /**
         * Constructor.
         *
         * @param invocationManager the invocation manager instance.
         */
        private CreateExecution(@Nonnull final DefaultInvocationManager invocationManager) {

            mManager = invocationManager;
        }

        public void run() {

            mManager.create(null, true);
        }
    }

    /**
     * Default implementation of an invocation manager supporting recycling of invocation instances.
     */
    private class DefaultInvocationManager implements InvocationManager<IN, OUT> {

        private final CreateExecution mCreateExecution;

        private final LinkedList<Invocation<IN, OUT>> mFallbackInvocations;

        private final InvocationType mInvocationType;

        private final LinkedList<Invocation<IN, OUT>> mPrimaryInvocations;

        private final Runner mRunner;

        /**
         * Constructor.
         *
         * @param type                the invocation type.
         * @param runner              the invocation runner.
         * @param primaryInvocations  the primary pool of invocations.
         * @param fallbackInvocations the fallback pool of invocations.
         */
        private DefaultInvocationManager(@Nonnull final InvocationType type,
                @Nonnull final Runner runner,
                @Nonnull final LinkedList<Invocation<IN, OUT>> primaryInvocations,
                @Nonnull final LinkedList<Invocation<IN, OUT>> fallbackInvocations) {

            mInvocationType = type;
            mRunner = runner;
            mPrimaryInvocations = primaryInvocations;
            mFallbackInvocations = fallbackInvocations;
            mCreateExecution = new CreateExecution(this);
        }

        public void create(@Nonnull final InvocationObserver<IN, OUT> observer) {

            create(observer, false);
        }

        public void discard(@Nonnull final Invocation<IN, OUT> invocation) {

            final boolean hasDelayed;

            synchronized (mMutex) {

                final Logger logger = mLogger;
                logger.wrn("discarding invocation instance after error: %s", invocation);

                try {

                    invocation.onDestroy();

                } catch (final Throwable t) {

                    InvocationInterruptedException.ignoreIfPossible(t);
                    logger.wrn(t, "ignoring exception while destroying invocation instance");
                }

                hasDelayed = !mObservers.isEmpty();
                --mRunningCount;
            }

            if (hasDelayed) {

                mRunner.run(mCreateExecution, 0, TimeUnit.MILLISECONDS);
            }
        }

        public void recycle(@Nonnull final Invocation<IN, OUT> invocation) {

            final boolean hasDelayed;

            synchronized (mMutex) {

                final Logger logger = mLogger;
                final int coreInvocations = mCoreInvocations;
                final LinkedList<Invocation<IN, OUT>> primaryInvocations = mPrimaryInvocations;
                final LinkedList<Invocation<IN, OUT>> fallbackInvocations = mFallbackInvocations;

                if ((primaryInvocations.size() + fallbackInvocations.size()) < coreInvocations) {

                    logger.dbg("recycling %s invocation instance [%d/%d]: %s", mInvocationType,
                               primaryInvocations.size() + 1, coreInvocations, invocation);
                    primaryInvocations.add(invocation);

                } else {

                    logger.wrn("discarding %s invocation instance [%d/%d]: %s", mInvocationType,
                               coreInvocations, coreInvocations, invocation);

                    try {

                        invocation.onDestroy();

                    } catch (final Throwable t) {

                        InvocationInterruptedException.ignoreIfPossible(t);
                        logger.wrn(t, "ignoring exception while destroying invocation instance");
                    }
                }

                hasDelayed = !mObservers.isEmpty();
                --mRunningCount;
            }

            if (hasDelayed) {

                mRunner.run(mCreateExecution, 0, TimeUnit.MILLISECONDS);
            }
        }

        @SuppressWarnings("ConstantConditions")
        private void create(@Nullable final InvocationObserver<IN, OUT> observer,
                final boolean isDelayed) {

            InvocationObserver<IN, OUT> invocationObserver = observer;

            try {

                Throwable error = null;
                Invocation<IN, OUT> invocation = null;

                synchronized (mMutex) {

                    final SimpleQueue<InvocationObserver<IN, OUT>> observers = mObservers;

                    if (isDelayed) {

                        if (observers.isEmpty()) {

                            return;
                        }

                        invocationObserver = observers.removeFirst();
                    }

                    if (isDelayed || (mRunningCount < (mMaxInvocations + observers.size()))) {

                        final InvocationType invocationType = mInvocationType;
                        final int coreInvocations = mCoreInvocations;
                        final LinkedList<Invocation<IN, OUT>> invocations = mPrimaryInvocations;

                        if (!invocations.isEmpty()) {

                            invocation = invocations.removeFirst();
                            mLogger.dbg("reusing %s invocation instance [%d/%d]: %s",
                                        invocationType, invocations.size() + 1, coreInvocations,
                                        invocation);

                        } else {

                            final LinkedList<Invocation<IN, OUT>> fallbackInvocations =
                                    mFallbackInvocations;

                            if (!fallbackInvocations.isEmpty()) {

                                final Invocation<IN, OUT> convertInvocation =
                                        fallbackInvocations.removeFirst();
                                mLogger.dbg("converting %s invocation instance [%d/%d]: %s",
                                            invocationType, invocations.size() + 1, coreInvocations,
                                            convertInvocation);
                                invocation = convertInvocation(convertInvocation, invocationType);

                            } else {

                                mLogger.dbg("creating %s invocation instance [1/%d]",
                                            invocationType, coreInvocations);
                                invocation = newInvocation(invocationType);
                            }
                        }

                        if (invocation != null) {

                            ++mRunningCount;
                        }

                    } else if (mInvocationType == InvocationType.SYNC) {

                        error = new InvocationDeadlockException(
                                "cannot wait for invocation instances on a synchronous runner "
                                        + "thread");

                    } else {

                        observers.add(invocationObserver);
                        return;
                    }
                }

                if (invocation != null) {

                    invocationObserver.onCreate(invocation);

                } else {

                    invocationObserver.onError((error != null) ? error : new NullPointerException(
                            "null invocation returned"));
                }

            } catch (final InvocationInterruptedException e) {

                throw e;

            } catch (final Throwable t) {

                mLogger.err(t, "error while creating new invocation instance", mMaxInvocations);
                invocationObserver.onError(t);
            }
        }
    }
}
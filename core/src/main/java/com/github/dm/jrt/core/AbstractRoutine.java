/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationDeadlockException;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.routine.TemplateRoutine;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.SimpleQueue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Basic abstract implementation of a routine.
 * <p>
 * This class provides a default implementation of all the routine features, like invocation modes
 * and recycling of invocation objects.
 * <br>
 * The inheriting class just needs to create invocation objects when required.
 * <p>
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

    private final LinkedList<Invocation<IN, OUT>> mSyncInvocations =
            new LinkedList<Invocation<IN, OUT>>();

    private final Runner mSyncRunner;

    private volatile DefaultInvocationManager mAsyncManager;

    private volatile AbstractRoutine<IN, OUT> mElementRoutine;

    private int mRunningCount;

    private volatile DefaultInvocationManager mSyncManager;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     */
    protected AbstractRoutine(@NotNull final InvocationConfiguration configuration) {
        mConfiguration = configuration;
        mSyncRunner = Runners.syncRunner();
        final int priority = configuration.getPriorityOrElse(InvocationConfiguration.DEFAULT);
        final Runner asyncRunner = configuration.getRunnerOrElse(Runners.sharedRunner());
        if (priority != InvocationConfiguration.DEFAULT) {
            mAsyncRunner = Runners.priorityRunner(asyncRunner).getRunner(priority);

        } else {
            mAsyncRunner = asyncRunner;
        }

        mMaxInvocations = configuration.getMaxInstancesOrElse(DEFAULT_MAX_INVOCATIONS);
        mCoreInvocations = configuration.getCoreInstancesOrElse(DEFAULT_CORE_INVOCATIONS);
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
    private AbstractRoutine(@NotNull final InvocationConfiguration configuration,
            @NotNull final Runner syncRunner, @NotNull final Runner asyncRunner,
            @NotNull final Logger logger) {
        mConfiguration = configuration;
        mSyncRunner = syncRunner;
        mAsyncRunner = asyncRunner;
        mMaxInvocations = DEFAULT_MAX_INVOCATIONS;
        mCoreInvocations = DEFAULT_CORE_INVOCATIONS;
        mLogger = logger.subContextLogger(this);
    }

    @NotNull
    public Channel<IN, OUT> async() {
        mLogger.dbg("invoking routine: %s", InvocationMode.ASYNC);
        return invoke(InvocationType.ASYNC);
    }

    @NotNull
    public Channel<IN, OUT> parallel() {
        mLogger.dbg("invoking routine: %s", InvocationMode.PARALLEL);
        return getElementRoutine().async();
    }

    @NotNull
    public Channel<IN, OUT> sequential() {
        mLogger.dbg("invoking routine: %s", InvocationMode.SEQUENTIAL);
        return getElementRoutine().sync();
    }

    @NotNull
    public Channel<IN, OUT> sync() {
        mLogger.dbg("invoking routine: %s", InvocationMode.SYNC);
        return invoke(InvocationType.SYNC);
    }

    @Override
    public void clear() {
        synchronized (mMutex) {
            final Logger logger = mLogger;
            final LinkedList<Invocation<IN, OUT>> syncInvocations = mSyncInvocations;
            for (final Invocation<IN, OUT> invocation : syncInvocations) {
                try {
                    invocation.onDiscard();

                } catch (final Throwable t) {
                    InvocationInterruptedException.throwIfInterrupt(t);
                    logger.wrn(t, "ignoring exception while discarding invocation instance");
                }
            }

            syncInvocations.clear();
            final LinkedList<Invocation<IN, OUT>> asyncInvocations = mAsyncInvocations;
            for (final Invocation<IN, OUT> invocation : asyncInvocations) {
                try {
                    invocation.onDiscard();

                } catch (final Throwable t) {
                    InvocationInterruptedException.throwIfInterrupt(t);
                    logger.wrn(t, "ignoring exception while discarding invocation instance");
                }
            }

            asyncInvocations.clear();
        }
    }

    /**
     * Converts an invocation instance to the specified type.
     * <br>
     * It is responsibility of the implementing class to call {@link Invocation#onDiscard()} on the
     * passed invocation, in case it gets discarded during the conversion.
     *
     * @param invocation the invocation to convert.
     * @param type       the type of the invocation after the conversion.
     * @return the converted invocation.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    @NotNull
    @SuppressWarnings("UnusedParameters")
    protected Invocation<IN, OUT> convertInvocation(@NotNull final Invocation<IN, OUT> invocation,
            @NotNull final InvocationType type) throws Exception {
        return invocation;
    }

    /**
     * Returns the routine invocation configuration.
     *
     * @return the invocation configuration.
     */
    @NotNull
    protected InvocationConfiguration getConfiguration() {
        return mConfiguration;
    }

    /**
     * Returns the routine logger.
     *
     * @return the logger instance.
     */
    @NotNull
    protected Logger getLogger() {
        return mLogger;
    }

    /**
     * Creates a new invocation instance.
     *
     * @param type the invocation type.
     * @return the invocation instance.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    @NotNull
    protected abstract Invocation<IN, OUT> newInvocation(@NotNull InvocationType type) throws
            Exception;

    @NotNull
    private Routine<IN, OUT> getElementRoutine() {
        if (mElementRoutine == null) {
            mElementRoutine =
                    new AbstractRoutine<IN, OUT>(mConfiguration, mSyncRunner, mAsyncRunner,
                            mLogger) {

                        @NotNull
                        @Override
                        protected Invocation<IN, OUT> convertInvocation(
                                @NotNull final Invocation<IN, OUT> invocation,
                                @NotNull final InvocationType type) throws Exception {
                            // No need to discard the old one
                            return newInvocation(type);
                        }

                        @NotNull
                        @Override
                        protected Invocation<IN, OUT> newInvocation(
                                @NotNull final InvocationType type) {
                            return (type == InvocationType.ASYNC) ? new ParallelInvocation<IN, OUT>(
                                    AbstractRoutine.this)
                                    : new SequentialInvocation<IN, OUT>(AbstractRoutine.this);
                        }
                    };
        }

        return mElementRoutine;
    }

    @NotNull
    private DefaultInvocationManager getInvocationManager(@NotNull final InvocationType type) {
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

    @NotNull
    private Channel<IN, OUT> invoke(@NotNull final InvocationType type) {
        final Logger logger = mLogger;
        logger.dbg("invoking routine: %s", type);
        final Runner runner = (type == InvocationType.ASYNC) ? mAsyncRunner : mSyncRunner;
        return new InvocationChannel<IN, OUT>(mConfiguration, getInvocationManager(type), runner,
                logger);
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

        private boolean mHasInputs;

        /**
         * Constructor.
         *
         * @param routine the routine to invoke in parallel mode.
         */
        private ParallelInvocation(@NotNull final Routine<IN, OUT> routine) {
            mRoutine = routine;
        }

        @Override
        public void onComplete(@NotNull final Channel<OUT, ?> result) {
            if (!mHasInputs) {
                result.pass(mRoutine.async().close());
            }
        }

        @Override
        public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) {
            mHasInputs = true;
            result.pass(mRoutine.async(input));
        }

        @Override
        public void onRecycle() {
            mHasInputs = false;
        }
    }

    /**
     * Implementation of an invocation handling sequential mode.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class SequentialInvocation<IN, OUT> extends TemplateInvocation<IN, OUT> {

        private final Routine<IN, OUT> mRoutine;

        private boolean mHasInputs;

        /**
         * Constructor.
         *
         * @param routine the routine to invoke in sequential mode.
         */
        private SequentialInvocation(@NotNull final Routine<IN, OUT> routine) {
            mRoutine = routine;
        }

        @Override
        public void onComplete(@NotNull final Channel<OUT, ?> result) {
            if (!mHasInputs) {
                result.pass(mRoutine.sync().close());
            }
        }

        @Override
        public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) {
            mHasInputs = true;
            result.pass(mRoutine.sync(input));
        }

        @Override
        public void onRecycle() {
            mHasInputs = false;
        }
    }

    /**
     * Execution implementation used to delay the creation of invocations.
     */
    private class CreateExecution implements Execution {

        private final DefaultInvocationManager mManager;

        /**
         * Constructor.
         *
         * @param invocationManager the invocation manager instance.
         */
        private CreateExecution(@NotNull final DefaultInvocationManager invocationManager) {
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
        private DefaultInvocationManager(@NotNull final InvocationType type,
                @NotNull final Runner runner,
                @NotNull final LinkedList<Invocation<IN, OUT>> primaryInvocations,
                @NotNull final LinkedList<Invocation<IN, OUT>> fallbackInvocations) {
            mInvocationType = type;
            mRunner = runner;
            mPrimaryInvocations = primaryInvocations;
            mFallbackInvocations = fallbackInvocations;
            mCreateExecution = new CreateExecution(this);
        }

        public void create(@NotNull final InvocationObserver<IN, OUT> observer) {
            create(observer, false);
        }

        public void discard(@NotNull final Invocation<IN, OUT> invocation) {
            final boolean hasDelayed;
            synchronized (mMutex) {
                final Logger logger = mLogger;
                logger.wrn("discarding invocation instance after error: %s", invocation);
                try {
                    invocation.onDiscard();

                } catch (final Throwable t) {
                    InvocationInterruptedException.throwIfInterrupt(t);
                    logger.wrn(t, "ignoring exception while discarding invocation instance");
                }

                hasDelayed = !mObservers.isEmpty();
                --mRunningCount;
            }

            if (hasDelayed) {
                mRunner.run(mCreateExecution, 0, TimeUnit.MILLISECONDS);
            }
        }

        public void recycle(@NotNull final Invocation<IN, OUT> invocation) {
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
                        invocation.onDiscard();

                    } catch (final Throwable t) {
                        InvocationInterruptedException.throwIfInterrupt(t);
                        logger.wrn(t, "ignoring exception while discarding invocation instance");
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
                                        + "thread\nTry increasing the max number of instances");

                    } else {
                        observers.add(invocationObserver);
                        return;
                    }
                }

                if (invocation != null) {
                    invocationObserver.onCreate(invocation);

                } else {
                    invocationObserver.onError((error != null) ? error
                            : new NullPointerException("null invocation returned"));
                }

            } catch (final InvocationInterruptedException e) {
                throw e;

            } catch (final Throwable t) {
                mLogger.err(t, "error while creating a new invocation instance [%d]",
                        mMaxInvocations);
                invocationObserver.onError(t);
            }
        }
    }
}

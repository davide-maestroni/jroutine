/**
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
package com.bmd.jrt.routine;

import com.bmd.jrt.builder.RoutineBuilder.RunnerType;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.common.RoutineInterruptedException;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.DefaultParameterChannel.InvocationManager;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;
import com.bmd.jrt.time.TimeDuration.Check;

import java.util.LinkedList;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.bmd.jrt.time.TimeDuration.seconds;

/**
 * Basic abstract implementation of a routine.
 * <p/>
 * This class provides implementations for all the routine functionalities. The inheriting class
 * just need to creates invocation objects when required.
 * <p/>
 * Created by davide on 9/7/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class AbstractRoutine<INPUT, OUTPUT> extends TemplateRoutine<INPUT, OUTPUT> {

    private static final TimeDuration DEFAULT_AVAIL_TIMEOUT = seconds(5);

    private static final int DEFAULT_MAX_RETAINED = 10;

    private static final int DEFAULT_MAX_RUNNING = Integer.MAX_VALUE;

    private final LinkedList<Invocation<INPUT, OUTPUT>> mAsyncInvocations =
            new LinkedList<Invocation<INPUT, OUTPUT>>();

    private final Runner mAsyncRunner;

    private final TimeDuration mAvailTimeout;

    private final RoutineConfiguration mConfiguration;

    private final Logger mLogger;

    private final int mMaxRetained;

    private final int mMaxRunning;

    private final Object mMutex = new Object();

    private final LinkedList<Invocation<INPUT, OUTPUT>> mSyncInvocations =
            new LinkedList<Invocation<INPUT, OUTPUT>>();

    private final Runner mSyncRunner;

    private volatile DefaultInvocationManager mAsyncManager;

    private volatile AbstractRoutine<INPUT, OUTPUT> mParallelRoutine;

    private int mRunningCount;

    private final Check mIsInvocationAvailable = new Check() {

        @Override
        public boolean isTrue() {

            return mRunningCount < mMaxRunning;
        }
    };

    private volatile DefaultInvocationManager mSyncManager;

    /**
     * Constructor.
     *
     * @param configuration the routine configuration.
     * @throws NullPointerException     if one of the parameters is null.
     * @throws IllegalArgumentException if at least one of the parameter is invalid.
     */
    @SuppressWarnings("ConstantConditions")
    protected AbstractRoutine(@Nonnull final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        mSyncRunner = (configuration.getSyncRunnerOr(RunnerType.QUEUED) == RunnerType.QUEUED)
                ? Runners.queuedRunner() : Runners.sequentialRunner();
        mAsyncRunner = configuration.getRunnerOr(Runners.sharedRunner());
        mMaxRunning = configuration.getMaxRunningOr(DEFAULT_MAX_RUNNING);
        mMaxRetained = configuration.getMaxRetainedOr(DEFAULT_MAX_RETAINED);
        mAvailTimeout = configuration.getAvailTimeoutOr(DEFAULT_AVAIL_TIMEOUT);
        mLogger = Logger.createLogger(configuration.getLogOr(Logger.getGlobalLog()),
                                      configuration.getLogLevelOr(Logger.getGlobalLogLevel()),
                                      this);
        mLogger.dbg("building routine with configuration: %s", configuration);
    }

    /**
     * Constructor.
     *
     * @param configuration the routine configuration.
     * @param syncRunner    the runner used for synchronous invocation.
     * @param asyncRunner   the runner used for asynchronous invocation.
     * @param maxRunning    the maximum number of running invocation instances.
     * @param maxRetained   the maximum number of retained invocation instances.
     * @param availTimeout  the maximum timeout to wait for available invocation instances.
     * @param logger        the logger instance.
     */
    private AbstractRoutine(@Nonnull final RoutineConfiguration configuration,
            @Nonnull final Runner syncRunner, @Nonnull final Runner asyncRunner,
            final int maxRunning, final int maxRetained, @Nonnull final TimeDuration availTimeout,
            @Nonnull final Logger logger) {

        mConfiguration = configuration;
        mSyncRunner = syncRunner;
        mAsyncRunner = asyncRunner;
        mMaxRunning = maxRunning;
        mMaxRetained = maxRetained;
        mAvailTimeout = availTimeout;
        mLogger = logger;
    }

    @Override
    public void flush() {

        synchronized (mMutex) {

            final Logger logger = mLogger;
            final LinkedList<Invocation<INPUT, OUTPUT>> syncInvocations = mSyncInvocations;

            for (final Invocation<INPUT, OUTPUT> syncInvocation : syncInvocations) {

                try {

                    syncInvocation.onDestroy();

                } catch (final RoutineInterruptedException e) {

                    throw e.interrupt();

                } catch (final Throwable t) {

                    logger.wrn(t, "ignoring exception while destroying invocation instance");
                }
            }

            syncInvocations.clear();

            final LinkedList<Invocation<INPUT, OUTPUT>> asyncInvocations = mAsyncInvocations;

            for (final Invocation<INPUT, OUTPUT> invocation : asyncInvocations) {

                try {

                    invocation.onDestroy();

                } catch (final RoutineInterruptedException e) {

                    throw e.interrupt();

                } catch (final Throwable t) {

                    logger.wrn(t, "ignoring exception while destroying invocation instance");
                }
            }

            asyncInvocations.clear();
        }
    }

    @Nonnull
    @Override
    public ParameterChannel<INPUT, OUTPUT> invokeAsync() {

        return invoke(true);
    }

    @Nonnull
    @Override
    public ParameterChannel<INPUT, OUTPUT> invokeParallel() {

        mLogger.dbg("invoking routine: parallel");

        if (mParallelRoutine == null) {

            mParallelRoutine =
                    new AbstractRoutine<INPUT, OUTPUT>(mConfiguration, mSyncRunner, mAsyncRunner,
                                                       mMaxRunning, mMaxRetained, mAvailTimeout,
                                                       mLogger) {

                        @Nonnull
                        @Override
                        protected Invocation<INPUT, OUTPUT> createInvocation(final boolean async) {

                            return new ParallelInvocation<INPUT, OUTPUT>(AbstractRoutine.this);
                        }
                    };
        }

        return mParallelRoutine.invokeAsync();
    }

    @Nonnull
    @Override
    public ParameterChannel<INPUT, OUTPUT> invokeSync() {

        return invoke(false);
    }

    /**
     * Converts an invocation instance
     *
     * @param async      whether the converted invocation is asynchronous.
     * @param invocation the invocation to convert.
     * @return the converted invocation.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected Invocation<INPUT, OUTPUT> convertInvocation(final boolean async,
            @Nonnull final Invocation<INPUT, OUTPUT> invocation) {

        return invocation;
    }

    /**
     * Creates a new invocation instance.
     *
     * @param async whether the invocation is asynchronous.
     * @return the invocation instance.
     */
    @Nonnull
    protected abstract Invocation<INPUT, OUTPUT> createInvocation(boolean async);

    /**
     * Returns the routine logger.
     *
     * @return the logger instance.
     */
    @Nonnull
    protected Logger getLogger() {

        return mLogger;
    }

    @Nonnull
    private DefaultInvocationManager getInvocationManager(final boolean async) {

        if (async) {

            if (mAsyncManager == null) {

                mAsyncManager = new DefaultInvocationManager(true);
            }

            return mAsyncManager;
        }

        if (mSyncManager == null) {

            mSyncManager = new DefaultInvocationManager(false);
        }

        return mSyncManager;
    }

    @Nonnull
    private ParameterChannel<INPUT, OUTPUT> invoke(final boolean async) {

        final Logger logger = mLogger;
        logger.dbg("invoking routine: %ssync", (async) ? "a" : "");
        return new DefaultParameterChannel<INPUT, OUTPUT>(mConfiguration,
                                                          getInvocationManager(async),
                                                          (async) ? mAsyncRunner : mSyncRunner,
                                                          logger);
    }

    /**
     * Default implementation of an invocation manager supporting recycling of invocation instances.
     */
    private class DefaultInvocationManager implements InvocationManager<INPUT, OUTPUT> {

        private final boolean mAsync;

        /**
         * Constructor.
         *
         * @param async whether the invocation is asynchronous.
         */
        private DefaultInvocationManager(final boolean async) {

            mAsync = async;
        }

        @Nonnull
        @Override
        public Invocation<INPUT, OUTPUT> create() {

            synchronized (mMutex) {

                boolean isTimeout = false;

                try {

                    isTimeout = !mAvailTimeout.waitTrue(mMutex, mIsInvocationAvailable);

                } catch (final InterruptedException e) {

                    mLogger.err(e, "waiting for available instance interrupted [#%d]", mMaxRunning);
                    RoutineInterruptedException.interrupt(e);
                }

                if (isTimeout) {

                    mLogger.wrn("routine instance not available after timeout [#%d]: %s",
                                mMaxRunning, mAvailTimeout);
                    throw new RoutineDeadlockException();
                }

                ++mRunningCount;

                final boolean async = mAsync;
                final LinkedList<Invocation<INPUT, OUTPUT>> invocations =
                        (async) ? mAsyncInvocations : mSyncInvocations;

                if (!invocations.isEmpty()) {

                    final Invocation<INPUT, OUTPUT> invocation = invocations.removeFirst();
                    mLogger.dbg("reusing %ssync invocation instance [%d/%d]: %s",
                                (async) ? "a" : "", invocations.size() + 1, mMaxRetained,
                                invocation);
                    return invocation;

                } else {

                    final LinkedList<Invocation<INPUT, OUTPUT>> fallbackInvocations =
                            (async) ? mSyncInvocations : mAsyncInvocations;

                    if (!fallbackInvocations.isEmpty()) {

                        final Invocation<INPUT, OUTPUT> invocation =
                                fallbackInvocations.removeFirst();
                        mLogger.dbg("converting %ssync invocation instance [%d/%d]: %s",
                                    (async) ? "a" : "", invocations.size() + 1, mMaxRetained,
                                    invocation);
                        return convertInvocation(async, invocation);
                    }
                }

                mLogger.dbg("creating %ssync invocation instance [1/%d]", (async) ? "a" : "",
                            mMaxRetained);
                return createInvocation(async);
            }
        }

        @Override
        @SuppressFBWarnings(value = "NO_NOTIFY_NOT_NOTIFYALL",
                            justification = "only one invocation is released")
        public void discard(@Nonnull final Invocation<INPUT, OUTPUT> invocation) {

            synchronized (mMutex) {

                mLogger.wrn("discarding invocation instance after error: %s", invocation);
                --mRunningCount;
                mMutex.notify();
            }
        }

        @Override
        @SuppressFBWarnings(value = "NO_NOTIFY_NOT_NOTIFYALL",
                            justification = "only one invocation is released")
        public void recycle(@Nonnull final Invocation<INPUT, OUTPUT> invocation) {

            synchronized (mMutex) {

                final Logger logger = mLogger;
                final boolean async = mAsync;
                final LinkedList<Invocation<INPUT, OUTPUT>> syncInvocations = mSyncInvocations;
                final LinkedList<Invocation<INPUT, OUTPUT>> asyncInvocations = mAsyncInvocations;

                if ((syncInvocations.size() + asyncInvocations.size()) < mMaxRetained) {

                    final LinkedList<Invocation<INPUT, OUTPUT>> invocations =
                            (async) ? asyncInvocations : syncInvocations;
                    logger.dbg("recycling %ssync invocation instance [%d/%d]: %s",
                               (async) ? "a" : "", invocations.size() + 1, mMaxRetained,
                               invocation);
                    invocations.add(invocation);

                } else {

                    logger.wrn("discarding %ssync invocation instance [%d/%d]: %s",
                               (async) ? "a" : "", mMaxRetained, mMaxRetained, invocation);

                    try {

                        invocation.onDestroy();

                    } catch (final RoutineInterruptedException e) {

                        throw e.interrupt();

                    } catch (final Throwable t) {

                        logger.wrn(t, "ignoring exception while destroying invocation instance");
                    }
                }

                --mRunningCount;
                mMutex.notify();
            }
        }
    }
}

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
package com.gh.bmd.jrt.android.v4.core;

import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.channel.TemplateOutputConsumer;
import com.gh.bmd.jrt.channel.TransportChannel.TransportInput;
import com.gh.bmd.jrt.common.AbortException;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.InvocationInterruptedException;
import com.gh.bmd.jrt.common.RoutineException;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Execution;
import com.gh.bmd.jrt.runner.Runner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class storing the invocation loader results.
 * <p/>
 * Created by davide-maestroni on 12/12/14.
 *
 * @param <OUTPUT> the output data type.
 */
class InvocationOutputConsumer<OUTPUT> extends TemplateOutputConsumer<OUTPUT> {

    private static final Runner sMainRunner = Runners.mainRunner();

    private final ArrayList<OUTPUT> mCachedResults = new ArrayList<OUTPUT>();

    private final Execution mDeliverResult;

    private final ArrayList<OUTPUT> mLastResults = new ArrayList<OUTPUT>();

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private RoutineException mAbortException;

    private boolean mIsComplete;

    /**
     * Constructor.
     *
     * @param loader the loader instance.
     * @param logger the logger instance.
     */
    @SuppressWarnings("ConstantConditions")
    InvocationOutputConsumer(@Nonnull final RoutineLoader<?, OUTPUT> loader,
            @Nonnull final Logger logger) {

        if (loader == null) {

            throw new NullPointerException("the loader must not be null");
        }

        mDeliverResult = new Execution() {

            public void run() {

                loader.deliverResult(createResult());
            }
        };
        mLogger = logger.subContextLogger(this);
    }

    @Override
    public void onComplete() {

        final boolean deliverResult;

        synchronized (mMutex) {

            mIsComplete = true;

            if (mAbortException != null) {

                mLogger.dbg("aborting channel");
                throw mAbortException;
            }

            deliverResult = mLastResults.isEmpty();
        }

        if (deliverResult) {

            mLogger.dbg("delivering final result");
            deliverResult();
        }
    }

    @Override
    public void onError(@Nullable final Throwable error) {

        final boolean deliverResult;
        final InvocationException abortException;

        synchronized (mMutex) {

            mIsComplete = true;
            abortException = new InvocationException(error);
            mAbortException = abortException;
            deliverResult = mLastResults.isEmpty();
        }

        if (deliverResult) {

            mLogger.dbg(abortException, "delivering error");
            deliverResult();
        }
    }

    @Override
    public void onOutput(final OUTPUT output) {

        final boolean deliverResult;

        synchronized (mMutex) {

            if (mAbortException != null) {

                mLogger.dbg("aborting channel");
                throw mAbortException;
            }

            deliverResult = mLastResults.isEmpty();
            mLastResults.add(output);
        }

        if (deliverResult) {

            mLogger.dbg("delivering result: %s", output);
            deliverResult();
        }
    }

    /**
     * Creates and returns a new invocation result object.
     *
     * @return the result object.
     */
    InvocationResult<OUTPUT> createResult() {

        // need to create a new instance each time to trick the loader manager into thinking that a
        // brand new result is available
        return new Result();
    }

    private void deliverResult() {

        sMainRunner.run(mDeliverResult, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Implementation of an invocation result.
     */
    private class Result implements InvocationResult<OUTPUT> {

        public void abort() {

            synchronized (mMutex) {

                mIsComplete = true;
                mAbortException = new AbortException(null);
            }
        }

        @Nullable
        public Throwable getAbortException() {

            synchronized (mMutex) {

                return mAbortException.getCause();
            }
        }

        public boolean isError() {

            synchronized (mMutex) {

                return (mAbortException != null);
            }
        }

        public boolean passTo(@Nonnull final Collection<TransportInput<OUTPUT>> newChannels,
                @Nonnull final Collection<TransportInput<OUTPUT>> oldChannels,
                @Nonnull final Collection<TransportInput<OUTPUT>> abortedChannels) {

            synchronized (mMutex) {

                final Logger logger = mLogger;
                final ArrayList<OUTPUT> lastResults = mLastResults;
                final ArrayList<OUTPUT> cachedResults = mCachedResults;

                if (mAbortException != null) {

                    logger.dbg("avoiding passing results since invocation is aborted");

                    lastResults.clear();
                    cachedResults.clear();
                    return true;

                } else {

                    logger.dbg("passing result: %s + %s", cachedResults, lastResults);

                    for (final TransportInput<OUTPUT> newChannel : newChannels) {

                        try {

                            newChannel.pass(cachedResults).pass(lastResults);

                        } catch (final InvocationInterruptedException e) {

                            throw e;

                        } catch (final Throwable t) {

                            abortedChannels.add(newChannel);
                        }
                    }

                    for (final TransportInput<OUTPUT> channel : oldChannels) {

                        try {

                            channel.pass(lastResults);

                        } catch (final InvocationInterruptedException e) {

                            throw e;

                        } catch (final Throwable t) {

                            abortedChannels.add(channel);
                        }
                    }

                    cachedResults.addAll(lastResults);
                    lastResults.clear();
                }

                logger.dbg("invocation is complete: %s", mIsComplete);
                return mIsComplete;
            }
        }
    }
}

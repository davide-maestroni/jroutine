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

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.invocation.InvocationInterruptedException;
import com.gh.bmd.jrt.log.Logger;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Loader implementation performing the routine invocation.
 * <p/>
 * Created by davide-maestroni on 12/8/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class RoutineLoader<INPUT, OUTPUT> extends AsyncTaskLoader<InvocationResult<OUTPUT>> {

    private final List<? extends INPUT> mInputs;

    private final ContextInvocation<INPUT, OUTPUT> mInvocation;

    private final ContextInvocationFactory<INPUT, OUTPUT> mInvocationFactory;

    private final Logger mLogger;

    private final OrderType mOrderType;

    private int mInvocationCount;

    private InvocationResult<OUTPUT> mResult;

    /**
     * Constructor.
     *
     * @param context           used to retrieve the application context.
     * @param invocation        the invocation instance.
     * @param invocationFactory the invocation factory.
     * @param inputs            the input data.
     * @param order             the data order.
     * @param logger            the logger instance.
     */
    @SuppressWarnings("ConstantConditions")
    RoutineLoader(@Nonnull final Context context,
            @Nonnull final ContextInvocation<INPUT, OUTPUT> invocation,
            @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> invocationFactory,
            @Nonnull final List<? extends INPUT> inputs, @Nullable final OrderType order,
            @Nonnull final Logger logger) {

        super(context);

        if (invocation == null) {

            throw new NullPointerException("the invocation instance must not be null");
        }

        if (invocationFactory == null) {

            throw new NullPointerException("the invocation factory must not be null");
        }

        if (inputs == null) {

            throw new NullPointerException("the list of input data must not be null");
        }

        mInvocation = invocation;
        mInvocationFactory = invocationFactory;
        mInputs = inputs;
        mOrderType = order;
        mLogger = logger.subContextLogger(this);
    }

    /**
     * Checks if the loader inputs are equal to the specified ones.
     *
     * @param inputs the input data.
     * @return whether the inputs are equal.
     */
    public boolean areSameInputs(@Nullable final List<? extends INPUT> inputs) {

        return mInputs.equals(inputs);
    }

    @Override
    public void deliverResult(final InvocationResult<OUTPUT> data) {

        mLogger.dbg("delivering result: %s", data);
        mResult = data;
        super.deliverResult(data);
    }

    @Override
    protected void onStartLoading() {

        super.onStartLoading();
        final Logger logger = mLogger;
        logger.dbg("start background invocation");
        final InvocationResult<OUTPUT> result = mResult;

        if (takeContentChanged() || (result == null)) {

            forceLoad();

        } else {

            logger.dbg("re-delivering result: %s", result);
            super.deliverResult(result);
        }
    }

    @Override
    protected void onReset() {

        try {

            mInvocation.onDestroy();

        } catch (final Throwable t) {

            InvocationInterruptedException.ignoreIfPossible(t);
            mLogger.wrn(t, "ignoring exception while destroying invocation instance");
        }

        mLogger.dbg("resetting result");
        mResult = null;
        super.onReset();
    }

    @Override
    public InvocationResult<OUTPUT> loadInBackground() {

        final Logger logger = mLogger;
        final InvocationOutputConsumer<OUTPUT> consumer =
                new InvocationOutputConsumer<OUTPUT>(this, logger);
        JRoutine.on(new LoaderInvocationFactory<INPUT, OUTPUT>(getContext(), mInvocation))
                .invocations()
                .withSyncRunner(Runners.sequentialRunner())
                .withOutputOrder(mOrderType)
                .withLog(logger.getLog())
                .withLogLevel(logger.getLogLevel())
                .set()
                .syncCall(mInputs)
                .passTo(consumer);
        return consumer.createResult();
    }

    /**
     * Gets this loader invocation count.
     *
     * @return the invocation count.
     */
    int getInvocationCount() {

        return mInvocationCount;
    }

    /**
     * Sets the invocation count.
     *
     * @param count the invocation count.
     */
    void setInvocationCount(final int count) {

        mInvocationCount = count;
    }

    /**
     * Returns the invocation factory.
     *
     * @return the factory.
     */
    ContextInvocationFactory<INPUT, OUTPUT> getInvocationFactory() {

        return mInvocationFactory;
    }

    /**
     * Checks if the last result has been delivered more than the specified milliseconds in the
     * past.
     *
     * @param staleTimeMillis the stale time in milliseconds.
     * @return whether the result is stale.
     */
    boolean isStaleResult(final long staleTimeMillis) {

        final InvocationResult<OUTPUT> result = mResult;
        return (result != null) && ((System.currentTimeMillis() - result.getResultTimestamp())
                > staleTimeMillis);
    }

    /**
     * Invocation factory implementation.
     *
     * @param <INPUT>
     * @param <OUTPUT>
     */
    private static class LoaderInvocationFactory<INPUT, OUTPUT>
            extends InvocationFactory<INPUT, OUTPUT> {

        private final Context mContext;

        private final ContextInvocation<INPUT, OUTPUT> mInvocation;

        /**
         * Constructor.
         *
         * @param context    the loader context.
         * @param invocation the invocation instance.
         */
        private LoaderInvocationFactory(@Nonnull final Context context,
                @Nonnull final ContextInvocation<INPUT, OUTPUT> invocation) {

            mContext = context;
            mInvocation = invocation;
        }

        @Nonnull
        @Override
        public Invocation<INPUT, OUTPUT> newInvocation() {

            final ContextInvocation<INPUT, OUTPUT> invocation = mInvocation;
            invocation.onContext(mContext);
            return invocation;
        }
    }
}

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
package com.github.dm.jrt.android.v4.core;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.github.dm.jrt.android.invocation.ContextInvocation;
import com.github.dm.jrt.android.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.InvocationInterruptedException;
import com.github.dm.jrt.log.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.android.invocation.ContextInvocations.fromFactory;

/**
 * Loader implementation performing the routine invocation.
 * <p/>
 * Created by davide-maestroni on 12/08/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class InvocationLoader<IN, OUT> extends AsyncTaskLoader<InvocationResult<OUT>> {

    private final List<? extends IN> mInputs;

    private final ContextInvocation<IN, OUT> mInvocation;

    private final FunctionContextInvocationFactory<IN, OUT> mInvocationFactory;

    private final Logger mLogger;

    private final OrderType mOrderType;

    private int mInvocationCount;

    private InvocationResult<OUT> mResult;

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
    InvocationLoader(@NotNull final Context context,
            @NotNull final ContextInvocation<IN, OUT> invocation,
            @NotNull final FunctionContextInvocationFactory<IN, OUT> invocationFactory,
            @NotNull final List<? extends IN> inputs, @Nullable final OrderType order,
            @NotNull final Logger logger) {

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
    public boolean areSameInputs(@Nullable final List<? extends IN> inputs) {

        return mInputs.equals(inputs);
    }

    @Override
    public void deliverResult(final InvocationResult<OUT> data) {

        mLogger.dbg("delivering result: %s", data);
        mResult = data;
        super.deliverResult(data);
    }

    @Override
    protected void onStartLoading() {

        super.onStartLoading();
        final Logger logger = mLogger;
        logger.dbg("start background invocation");
        final InvocationResult<OUT> result = mResult;
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
            InvocationInterruptedException.throwIfInterrupt(t);
            mLogger.wrn(t, "ignoring exception while destroying invocation instance");
        }

        mLogger.dbg("resetting result");
        mResult = null;
        super.onReset();
    }

    @Override
    public InvocationResult<OUT> loadInBackground() {

        final Logger logger = mLogger;
        final InvocationOutputConsumer<OUT> consumer =
                new InvocationOutputConsumer<OUT>(this, logger);
        final LoaderContextInvocationFactory<IN, OUT> factory =
                new LoaderContextInvocationFactory<IN, OUT>(mInvocation);
        JRoutine.on(fromFactory(getContext(), factory))
                .withInvocations()
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
    @NotNull
    FunctionContextInvocationFactory<IN, OUT> getInvocationFactory() {

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

        final InvocationResult<OUT> result = mResult;
        return (result != null) && ((System.currentTimeMillis() - result.getResultTimestamp())
                > staleTimeMillis);
    }

    /**
     * Context invocation factory implementation.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class LoaderContextInvocationFactory<IN, OUT>
            extends ContextInvocationFactory<IN, OUT> {

        private final ContextInvocation<IN, OUT> mInvocation;

        /**
         * Constructor.
         *
         * @param invocation the loader invocation instance.
         */
        private LoaderContextInvocationFactory(
                @NotNull final ContextInvocation<IN, OUT> invocation) {

            super(null);
            mInvocation = invocation;
        }

        @NotNull
        @Override
        public ContextInvocation<IN, OUT> newInvocation() {

            return mInvocation;
        }
    }
}

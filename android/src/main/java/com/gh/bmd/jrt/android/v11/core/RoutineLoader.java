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
package com.gh.bmd.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Build.VERSION_CODES;

import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.invocation.InvocationInterruptedException;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.util.TimeDuration;

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
@TargetApi(VERSION_CODES.HONEYCOMB)
class RoutineLoader<INPUT, OUTPUT> extends AsyncTaskLoader<InvocationResult<OUTPUT>>
        implements InvocationFactory<INPUT, OUTPUT> {

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
        mLogger.dbg("start background invocation");

        if (mResult != null) {

            deliverResult(mResult);
        }

        if (takeContentChanged() || (mResult == null)) {

            forceLoad();
        }
    }

    @Override
    protected void onReset() {

        try {

            mInvocation.onDestroy();

        } catch (final InvocationInterruptedException e) {

            throw e;

        } catch (final Throwable ignored) {

            mLogger.wrn(ignored, "ignoring exception while destroying invocation instance");
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
        JRoutine.on(this)
                .invocations()
                .withSyncRunner(Runners.sequentialRunner())
                .withOutputOrder(mOrderType)
                .withOutputMaxSize(Integer.MAX_VALUE)
                .withOutputTimeout(TimeDuration.ZERO)
                .withLog(logger.getLog())
                .withLogLevel(logger.getLogLevel())
                .set()
                .callSync(mInputs)
                .passTo(consumer);
        return consumer.createResult();
    }

    @Nonnull
    @Override
    public Invocation<INPUT, OUTPUT> newInvocation() {

        final ContextInvocation<INPUT, OUTPUT> invocation = mInvocation;
        invocation.onContext(getContext());
        return invocation;
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
}

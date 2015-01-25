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
package com.bmd.jrt.android.v4.routine;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.bmd.jrt.android.invocation.AndroidInvocation;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.channel.Tunnel;
import com.bmd.jrt.channel.Tunnel.TunnelInput;
import com.bmd.jrt.common.InvocationException;
import com.bmd.jrt.common.InvocationInterruptedException;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.time.TimeDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Loader implementation performing the routine invocation.
 * <p/>
 * Created by davide on 12/8/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class RoutineLoader<INPUT, OUTPUT> extends AsyncTaskLoader<InvocationResult<OUTPUT>> {

    private final DataOrder mDataOrder;

    private final List<? extends INPUT> mInputs;

    private final AndroidInvocation<INPUT, OUTPUT> mInvocation;

    private final Logger mLogger;

    private int mInvocationCount;

    private InvocationResult<OUTPUT> mResult;

    /**
     * Stores away the application context associated with context.
     * Since Loaders can be used across multiple activities it's dangerous to
     * store the context directly; always use {@link #getContext()} to retrieve
     * the Loader's Context, don't use the constructor argument directly.
     * The Context returned by {@link #getContext} is safe to use across
     * Activity instances.
     *
     * @param context    used to retrieve the application context.
     * @param invocation the invocation instance.
     * @param inputs     the input data.
     * @param order      the data order.
     * @param logger     the logger instance.
     * @throws NullPointerException if any of the specified parameters is null.
     */
    @SuppressWarnings("ConstantConditions")
    RoutineLoader(@Nonnull final Context context,
            @Nonnull final AndroidInvocation<INPUT, OUTPUT> invocation,
            @Nonnull final List<? extends INPUT> inputs, @Nonnull final DataOrder order,
            @Nonnull final Logger logger) {

        super(context);

        if (invocation == null) {

            throw new NullPointerException("the invocation instance must not be null");
        }

        if (inputs == null) {

            throw new NullPointerException("the list of input data must not be null");
        }

        if (order == null) {

            throw new NullPointerException("the data order must not be null");
        }

        mInvocation = invocation;
        mInputs = inputs;
        mDataOrder = order;
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

            throw e.interrupt();

        } catch (final Throwable t) {

            mLogger.wrn(t, "ignoring exception while destroying invocation instance");
        }

        mLogger.dbg("resetting result");
        mResult = null;
        super.onReset();
    }

    /**
     * Returns the type of the loader invocation.
     *
     * @return the invocation class.
     */
    @Nonnull
    public Class<?> getInvocationType() {

        return mInvocation.getClass();
    }

    @Override
    public InvocationResult<OUTPUT> loadInBackground() {

        final Logger logger = mLogger;
        final AndroidInvocation<INPUT, OUTPUT> invocation = mInvocation;
        final LoaderResultChannel<OUTPUT> channel =
                new LoaderResultChannel<OUTPUT>(mDataOrder, logger);
        final InvocationOutputConsumer<OUTPUT> consumer =
                new InvocationOutputConsumer<OUTPUT>(this, logger);
        channel.output().bind(consumer);

        Throwable abortException = null;
        logger.dbg("running invocation");

        try {

            invocation.onInit();

            for (final INPUT input : mInputs) {

                invocation.onInput(input, channel);
            }

            invocation.onResult(channel);
            invocation.onReturn();

        } catch (final InvocationException e) {

            abortException = e.getCause();

        } catch (final Throwable t) {

            abortException = t;
        }

        if (abortException != null) {

            logger.dbg(abortException, "aborting invocation");

            try {

                invocation.onAbort(abortException);

            } catch (final InvocationException e) {

                abortException = e.getCause();

            } catch (final Throwable t) {

                abortException = t;
            }

            logger.dbg(abortException, "aborted invocation");
            channel.abort(abortException);
            return consumer.createResult();
        }

        logger.dbg("reading invocation results");
        channel.close();
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
     * Loader result channel.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class LoaderResultChannel<OUTPUT> implements ResultChannel<OUTPUT> {

        private final TunnelInput<OUTPUT> mTunnelInput;

        private final Tunnel<OUTPUT> mmTunnel;

        /**
         * Constructor.
         *
         * @param order  the data order.
         * @param logger the logger instance.
         */
        private LoaderResultChannel(final DataOrder order, @Nonnull final Logger logger) {

            mmTunnel = JRoutine.on()
                               .dataOrder(order)
                               .maxSize(Integer.MAX_VALUE)
                               .bufferTimeout(TimeDuration.ZERO)
                               .loggedWith(logger.getLog())
                               .logLevel(logger.getLogLevel())
                               .buildTunnel();
            mTunnelInput = mmTunnel.input();
        }

        @Override
        public boolean abort() {

            return mTunnelInput.abort();
        }

        @Override
        public boolean abort(@Nullable final Throwable reason) {

            return mTunnelInput.abort(reason);
        }

        @Override
        public boolean isOpen() {

            return mTunnelInput.isOpen();
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> after(@Nonnull final TimeDuration delay) {

            mTunnelInput.after(delay);
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> after(final long delay, @Nonnull final TimeUnit timeUnit) {

            mTunnelInput.after(delay, timeUnit);
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> now() {

            mTunnelInput.now();
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> pass(@Nullable final OutputChannel<OUTPUT> channel) {

            mTunnelInput.pass(channel);
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

            mTunnelInput.pass(outputs);
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> pass(@Nullable final OUTPUT output) {

            mTunnelInput.pass(output);
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> pass(@Nullable final OUTPUT... outputs) {

            mTunnelInput.pass(outputs);
            return this;
        }

        private void close() {

            mTunnelInput.close();
        }

        @Nonnull
        private OutputChannel<OUTPUT> output() {

            return mmTunnel.output();
        }
    }
}

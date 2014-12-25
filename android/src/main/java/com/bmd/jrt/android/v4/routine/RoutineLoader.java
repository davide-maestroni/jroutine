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

import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.channel.IOChannel.IOChannelInput;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.invocation.Invocation;
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

    private final List<? extends INPUT> mInputs;

    private final Invocation<INPUT, OUTPUT> mInvocation;

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
     * @param logger     the logger instance.
     * @throws NullPointerException if any of the specified parameters is null.
     */
    @SuppressWarnings("ConstantConditions")
    RoutineLoader(@Nonnull final Context context,
            @Nonnull final Invocation<INPUT, OUTPUT> invocation,
            @Nonnull final List<? extends INPUT> inputs, @Nonnull final Logger logger) {

        super(context);

        if (invocation == null) {

            throw new NullPointerException("the invocation instance must not be null");
        }

        if (inputs == null) {

            throw new NullPointerException("the list of input data must not be null");
        }

        mInvocation = invocation;
        mInputs = inputs;
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
        final LoaderResultChannel<OUTPUT> channel = new LoaderResultChannel<OUTPUT>(logger);
        final Invocation<INPUT, OUTPUT> invocation = mInvocation;
        RoutineException exception = null;

        logger.dbg("running invocation");

        try {

            invocation.onInit();

            for (final INPUT input : mInputs) {

                invocation.onInput(input, channel);
            }

            invocation.onResult(channel);
            invocation.onReturn();

        } catch (final RoutineException e) {

            exception = e;

        } catch (final Throwable t) {

            exception = new RoutineException(t);
        }

        if (exception != null) {

            logger.dbg(exception, "aborting invocation");

            try {

                invocation.onAbort(exception);

            } catch (final RoutineException e) {

                exception = e;

            } catch (final Throwable t) {

                exception = new RoutineException(t);
            }

            logger.dbg(exception, "aborted invocation");
            return new InvocationResult<OUTPUT>(exception);
        }

        try {

            logger.dbg("reading invocation results");
            return new InvocationResult<OUTPUT>(channel.result().readAll());

        } catch (final RoutineException e) {

            exception = e;

        } catch (final Throwable t) {

            exception = new RoutineException(t);
        }

        logger.dbg(exception, "aborted invocation");
        return new InvocationResult<OUTPUT>(exception);
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

        private final IOChannel<OUTPUT> mChannel;

        private final IOChannelInput<OUTPUT> mChannelInput;

        /**
         * Constructor.
         *
         * @param logger the logger instance.
         */
        private LoaderResultChannel(@Nonnull final Logger logger) {

            mChannel = JRoutine.io()
                               .loggedWith(logger.getLog())
                               .logLevel(logger.getLogLevel())
                               .buildChannel();
            mChannelInput = mChannel.input();
        }

        @Override
        public boolean abort() {

            return mChannelInput.abort();
        }

        @Override
        public boolean abort(@Nullable final Throwable reason) {

            return mChannelInput.abort(reason);
        }

        @Override
        public boolean isOpen() {

            return mChannelInput.isOpen();
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> after(@Nonnull final TimeDuration delay) {

            mChannelInput.after(delay);
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> after(final long delay, @Nonnull final TimeUnit timeUnit) {

            mChannelInput.after(delay, timeUnit);
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> now() {

            mChannelInput.now();
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> pass(@Nullable final OutputChannel<OUTPUT> channel) {

            mChannelInput.pass(channel);
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> pass(@Nullable final Iterable<? extends OUTPUT> outputs) {

            mChannelInput.pass(outputs);
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> pass(@Nullable final OUTPUT output) {

            mChannelInput.pass(output);
            return this;
        }

        @Nonnull
        @Override
        public ResultChannel<OUTPUT> pass(@Nullable final OUTPUT... outputs) {

            mChannelInput.pass(outputs);
            return this;
        }

        /**
         * Close the input channel and returns the output one.
         *
         * @return the output channel.
         */
        @Nonnull
        private OutputChannel<OUTPUT> result() {

            mChannelInput.close();
            return mChannel.output();
        }
    }
}

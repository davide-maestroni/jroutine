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
     * @throws NullPointerException if one of the specified parameters is null.
     */
    @SuppressWarnings("ConstantConditions")
    RoutineLoader(@Nonnull final Context context,
            @Nonnull final Invocation<INPUT, OUTPUT> invocation,
            @Nonnull final List<? extends INPUT> inputs) {

        super(context);

        if (invocation == null) {

            throw new NullPointerException("the invocation instance must not be null");
        }

        if (inputs == null) {

            throw new NullPointerException("the list of input data must not be null");
        }

        mInvocation = invocation;
        mInputs = inputs;
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

        if (isReset()) {

            return;
        }

        mResult = data;

        if (isStarted()) {

            super.deliverResult(data);
        }
    }

    @Override
    protected void onStartLoading() {

        super.onStartLoading();

        if (mResult != null) {

            deliverResult(mResult);
        }

        if (takeContentChanged() || (mResult == null)) {

            forceLoad();
        }
    }

    @Override
    protected void onReset() {

        super.onReset();

        mResult = null;
    }

    /**
     * Checks if the loader type is the same as the specified one.
     *
     * @param type the invocation type.
     * @return whether the types are the same.
     */
    public boolean isSameInvocationType(@Nullable final Class<? extends Invocation<?, ?>> type) {

        return (mInvocation.getClass() == type);
    }

    @Override
    public InvocationResult<OUTPUT> loadInBackground() {

        final LoaderResultChannel<OUTPUT> channel = new LoaderResultChannel<OUTPUT>();
        final Invocation<INPUT, OUTPUT> invocation = mInvocation;
        RoutineException exception = null;

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

            try {

                invocation.onAbort(exception);

            } catch (final RoutineException e) {

                exception = e;

            } catch (final Throwable t) {

                exception = new RoutineException(t);
            }

            return new InvocationResult<OUTPUT>(exception);
        }

        try {

            return new InvocationResult<OUTPUT>(channel.result().readAll());

        } catch (final RoutineException e) {

            exception = e;

        } catch (final Throwable t) {

            exception = new RoutineException(t);
        }

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
         */
        private LoaderResultChannel() {

            mChannel = JRoutine.io().buildChannel();
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

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

import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.OutputConsumer;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.DefaultResultChannel.AbortHandler;
import com.bmd.jrt.time.TimeDuration;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of an input/output channel.
 * <p/>
 * Created by davide on 10/24/14.
 *
 * @param <TYPE> the data type.
 */
class DefaultIOChannel<TYPE> implements IOChannel<TYPE> {

    private final DefaultIOChannelInput<TYPE> mInputChannel;

    private final DefaultIOChannelOutput<TYPE> mOutputChannel;

    /**
     * Constructor.
     *
     * @param configuration the routine configuration.
     */
    DefaultIOChannel(@Nonnull final RoutineConfiguration configuration) {

        final IOChannelAbortHandler abortHandler = new IOChannelAbortHandler();
        final DefaultResultChannel<TYPE> inputChannel =
                new DefaultResultChannel<TYPE>(configuration, abortHandler,
                                               configuration.getRunner(null),
                                               Logger.create(configuration.getLog(null),
                                                             configuration.getLogLevel(null),
                                                             IOChannel.class));
        abortHandler.setInputChannel(inputChannel);
        mInputChannel = new DefaultIOChannelInput<TYPE>(inputChannel);
        mOutputChannel = new DefaultIOChannelOutput<TYPE>(inputChannel.getOutput());
    }

    @Nonnull
    @Override
    public IOChannelInput<TYPE> input() {

        return mInputChannel;
    }

    @Nonnull
    @Override
    public IOChannelOutput<TYPE> output() {

        return mOutputChannel;
    }

    /**
     * Default implementation of an I/O channel input.
     *
     * @param <INPUT> the input data type.
     */
    private static class DefaultIOChannelInput<INPUT> implements IOChannelInput<INPUT> {

        private final DefaultResultChannel<INPUT> mChannel;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped result channel.
         */
        private DefaultIOChannelInput(@Nonnull final DefaultResultChannel<INPUT> wrapped) {

            mChannel = wrapped;
        }

        @Override
        public boolean abort() {

            return mChannel.abort();
        }

        @Nonnull
        @Override
        public IOChannelInput<INPUT> after(@Nonnull final TimeDuration delay) {

            mChannel.after(delay);

            return this;
        }

        @Nonnull
        @Override
        public IOChannelInput<INPUT> after(final long delay, @Nonnull final TimeUnit timeUnit) {

            mChannel.after(delay, timeUnit);

            return this;
        }

        @Nonnull
        @Override
        public IOChannelInput<INPUT> now() {

            mChannel.now();

            return this;
        }

        @Nonnull
        @Override
        public IOChannelInput<INPUT> pass(@Nullable final OutputChannel<INPUT> channel) {

            mChannel.pass(channel);

            return this;
        }

        @Nonnull
        @Override
        public IOChannelInput<INPUT> pass(@Nullable final Iterable<? extends INPUT> inputs) {

            mChannel.pass(inputs);

            return this;
        }

        @Nonnull
        @Override
        public IOChannelInput<INPUT> pass(@Nullable final INPUT input) {

            mChannel.pass(input);

            return this;
        }

        @Nonnull
        @Override
        public IOChannelInput<INPUT> pass(@Nullable final INPUT... inputs) {

            mChannel.pass(inputs);

            return this;
        }

        @Override
        public void close() {

            mChannel.close();
        }

        @Override
        public boolean abort(@Nullable final Throwable reason) {

            return mChannel.abort(reason);
        }

        @Override
        public boolean isOpen() {

            return mChannel.isOpen();
        }
    }

    /**
     * Default implementation of an I/O channel output.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class DefaultIOChannelOutput<OUTPUT> implements IOChannelOutput<OUTPUT> {

        private final OutputChannel<OUTPUT> mChannel;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped output channel.
         */
        private DefaultIOChannelOutput(@Nonnull final OutputChannel<OUTPUT> wrapped) {

            mChannel = wrapped;
        }

        @Nonnull
        @Override
        public IOChannelOutput<OUTPUT> afterMax(@Nonnull final TimeDuration timeout) {

            mChannel.afterMax(timeout);

            return this;
        }

        @Nonnull
        @Override
        public IOChannelOutput<OUTPUT> afterMax(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            mChannel.afterMax(timeout, timeUnit);

            return this;
        }

        @Nonnull
        @Override
        public IOChannelOutput<OUTPUT> bind(@Nonnull final OutputConsumer<OUTPUT> consumer) {

            mChannel.bind(consumer);

            return this;
        }

        @Nonnull
        @Override
        @SuppressWarnings("BooleanParameter")
        public IOChannelOutput<OUTPUT> eventuallyDeadLock(final boolean throwException) {

            mChannel.eventuallyDeadLock(throwException);

            return this;
        }

        @Nonnull
        @Override
        public IOChannelOutput<OUTPUT> immediately() {

            mChannel.immediately();

            return this;
        }

        @Nonnull
        @Override
        public IOChannelOutput<OUTPUT> readAllInto(
                @Nonnull final Collection<? super OUTPUT> result) {

            mChannel.readAllInto(result);

            return this;
        }

        @Override
        public boolean isBound() {

            return mChannel.isBound();
        }

        @Override
        public boolean isComplete() {

            return mChannel.isComplete();
        }

        @Nonnull
        @Override
        public List<OUTPUT> readAll() {

            return mChannel.readAll();
        }

        @Override
        public OUTPUT readFirst() {

            return mChannel.readFirst();
        }

        @Nonnull
        @Override
        public IOChannelOutput<OUTPUT> unbind(@Nullable final OutputConsumer<OUTPUT> consumer) {

            mChannel.unbind(consumer);

            return this;
        }

        @Override
        public Iterator<OUTPUT> iterator() {

            return mChannel.iterator();
        }

        @Override
        public boolean abort() {

            return mChannel.abort();
        }

        @Override
        public boolean abort(@Nullable final Throwable reason) {

            return mChannel.abort(reason);
        }

        @Override
        public boolean isOpen() {

            return mChannel.isOpen();
        }
    }

    /**
     * Abort handler used to close the input channel on abort.
     */
    private static class IOChannelAbortHandler implements AbortHandler {

        private DefaultResultChannel<?> mInputChannel;

        @Override
        public void onAbort(@Nullable final Throwable reason, final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mInputChannel.close(reason);
        }

        public void setInputChannel(@Nonnull final DefaultResultChannel<?> inputChannel) {

            mInputChannel = inputChannel;
        }
    }
}

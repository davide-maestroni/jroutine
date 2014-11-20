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
import com.bmd.jrt.runner.Execution;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;
import com.bmd.jrt.time.TimeDuration;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
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

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final HashSet<ChannelWeakReference> sReferences =
            new HashSet<ChannelWeakReference>();

    private static ReferenceQueue<DefaultIOChannel<?>> sReferenceQueue;

    private static Runner sWeakRunner;

    private final DefaultChannelInput<TYPE> mInputChannel;

    private final DefaultChannelOutput<TYPE> mOutputChannel;

    /**
     * Constructor.
     *
     * @param configuration the routine configuration.
     */
    DefaultIOChannel(@Nonnull final RoutineConfiguration configuration) {

        final ChannelAbortHandler abortHandler = new ChannelAbortHandler();
        final DefaultResultChannel<TYPE> inputChannel =
                new DefaultResultChannel<TYPE>(configuration, abortHandler,
                                               configuration.getRunner(null),
                                               Logger.create(configuration.getLog(null),
                                                             configuration.getLogLevel(null),
                                                             IOChannel.class));
        abortHandler.setInputChannel(inputChannel);
        mInputChannel = new DefaultChannelInput<TYPE>(inputChannel);
        mOutputChannel = new DefaultChannelOutput<TYPE>(inputChannel.getOutput());

        addChannelReference(this);
    }

    private static void addChannelReference(@Nonnull final DefaultIOChannel<?> channel) {

        synchronized (sReferences) {

            if (sWeakRunner == null) {

                sWeakRunner = Runners.poolRunner(1);
            }

            if (sReferenceQueue == null) {

                sReferenceQueue = new ReferenceQueue<DefaultIOChannel<?>>();
                sWeakRunner.run(new ChannelExecution(), 0, TimeUnit.MILLISECONDS);
            }

            sReferences.add(new ChannelWeakReference(channel, sReferenceQueue));
        }
    }

    @Nonnull
    @Override
    public ChannelInput<TYPE> input() {

        return mInputChannel;
    }

    @Nonnull
    @Override
    public ChannelOutput<TYPE> output() {

        return mOutputChannel;
    }

    /**
     * Abort handler used to close the input channel on abort.
     */
    private static class ChannelAbortHandler implements AbortHandler {

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

    /**
     * Execution used to wait on the weak reference queue.
     */
    private static class ChannelExecution implements Execution {

        @Override
        public void run() {

            try {

                ChannelWeakReference reference;

                while ((reference = (ChannelWeakReference) sReferenceQueue.remove()) != null) {

                    reference.closeInput();

                    synchronized (sReferences) {

                        sReferences.remove(reference);
                    }
                }

            } catch (final InterruptedException ignored) {

            }
        }
    }

    /**
     * Weak reference used to close the input channel.
     */
    private static class ChannelWeakReference extends WeakReference<DefaultIOChannel<?>> {

        private final DefaultChannelInput<?> mInputChannel;

        /**
         * Constructor.
         *
         * @param referent the referent channel instance.
         * @param queue    the reference queue.
         */
        private ChannelWeakReference(@Nonnull final DefaultIOChannel<?> referent,
                @Nonnull final ReferenceQueue<? super DefaultIOChannel<?>> queue) {

            super(referent, queue);

            mInputChannel = referent.mInputChannel;
        }

        public void closeInput() {

            mInputChannel.close();
        }
    }

    /**
     * Default implementation of an I/O channel input.
     *
     * @param <INPUT> the input type.
     */
    private static class DefaultChannelInput<INPUT> implements ChannelInput<INPUT> {

        private final DefaultResultChannel<INPUT> mChannel;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped result channel.
         */
        private DefaultChannelInput(@Nonnull final DefaultResultChannel<INPUT> wrapped) {

            mChannel = wrapped;
        }

        @Override
        public boolean abort() {

            return mChannel.abort();
        }

        @Nonnull
        @Override
        public ChannelInput<INPUT> after(@Nonnull final TimeDuration delay) {

            mChannel.after(delay);

            return this;
        }

        @Nonnull
        @Override
        public ChannelInput<INPUT> after(final long delay, @Nonnull final TimeUnit timeUnit) {

            mChannel.after(delay, timeUnit);

            return this;
        }

        @Nonnull
        @Override
        public ChannelInput<INPUT> now() {

            mChannel.now();

            return this;
        }

        @Nonnull
        @Override
        public ChannelInput<INPUT> pass(@Nullable final OutputChannel<INPUT> channel) {

            mChannel.pass(channel);

            return this;
        }

        @Nonnull
        @Override
        public ChannelInput<INPUT> pass(@Nullable final Iterable<? extends INPUT> inputs) {

            mChannel.pass(inputs);

            return this;
        }

        @Nonnull
        @Override
        public ChannelInput<INPUT> pass(@Nullable final INPUT input) {

            mChannel.pass(input);

            return this;
        }

        @Nonnull
        @Override
        public ChannelInput<INPUT> pass(@Nullable final INPUT... inputs) {

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
     * @param <OUTPUT> the output type.
     */
    private static class DefaultChannelOutput<OUTPUT> implements ChannelOutput<OUTPUT> {

        private final OutputChannel<OUTPUT> mChannel;

        /**
         * Constructor.
         *
         * @param wrapped the wrapped output channel.
         */
        private DefaultChannelOutput(@Nonnull final OutputChannel<OUTPUT> wrapped) {

            mChannel = wrapped;
        }

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> afterMax(@Nonnull final TimeDuration timeout) {

            mChannel.afterMax(timeout);

            return this;
        }

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> afterMax(final long timeout,
                @Nonnull final TimeUnit timeUnit) {

            mChannel.afterMax(timeout, timeUnit);

            return this;
        }

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> bind(@Nullable final OutputConsumer<OUTPUT> consumer) {

            mChannel.bind(consumer);

            return this;
        }

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> eventuallyThrow(@Nullable final RuntimeException exception) {

            mChannel.eventuallyThrow(exception);

            return this;
        }

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> immediately() {

            mChannel.immediately();

            return this;
        }

        @Nonnull
        @Override
        public ChannelOutput<OUTPUT> readAllInto(
                @Nonnull final Collection<? super OUTPUT> results) {

            mChannel.readAllInto(results);

            return this;
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
}

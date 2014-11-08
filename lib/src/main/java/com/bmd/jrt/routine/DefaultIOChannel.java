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

import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.channel.InputChannel;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.DefaultResultChannel.AbortHandler;
import com.bmd.jrt.runner.Execution;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.runner.Runners;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.runner.Runners.sharedRunner;

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

    private final DefaultResultChannel<TYPE> mInputChannel;

    private final OutputChannel<TYPE> mOutputChannel;

    /**
     * Constructor.
     *
     * @param isOrdered whether the input is ordered.
     * @param log       the log instance.
     * @param level     the log level.
     */
    DefaultIOChannel(final boolean isOrdered, @Nonnull final Log log,
            @Nonnull final LogLevel level) {

        final ChannelAbortHandler abortHandler = new ChannelAbortHandler();
        final DefaultResultChannel<TYPE> inputChannel =
                new DefaultResultChannel<TYPE>(abortHandler, sharedRunner(), isOrdered,
                                               Logger.create(log, level, IOChannel.class));
        abortHandler.setInputChannel(inputChannel);
        mInputChannel = inputChannel;
        mOutputChannel = inputChannel.getOutput();

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

    @Override
    public void close() {

        mInputChannel.close();
    }

    @Override
    @Nonnull
    public InputChannel<TYPE> input() {

        return mInputChannel;
    }

    @Override
    @Nonnull
    public OutputChannel<TYPE> output() {

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

        private final DefaultResultChannel<?> mInputChannel;

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
}

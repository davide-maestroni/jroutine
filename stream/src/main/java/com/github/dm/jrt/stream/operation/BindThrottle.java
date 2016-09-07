/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.stream.operation;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.SimpleQueue;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.stream.builder.StreamBuilder.StreamConfiguration;
import com.github.dm.jrt.stream.modifier.ThrottleChannelConsumer.CompletionHandler;

import org.jetbrains.annotations.NotNull;

/**
 * Invocation throttle binding function.
 * <p>
 * Created by davide-maestroni on 07/29/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class BindThrottle<IN, OUT> implements
        BiFunction<StreamConfiguration, Function<Channel<?, IN>, Channel<?, OUT>>, Function<?
                super Channel<?, IN>, ? extends Channel<?, OUT>>> {

    private final int mMaxCount;

    private final Object mMutex = new Object();

    private final SimpleQueue<Runnable> mQueue = new SimpleQueue<Runnable>();

    private int mCount;

    /**
     * Constructor.
     *
     * @param count the maximum invocation count.
     */
    BindThrottle(final int count) {
        mMaxCount = ConstantConditions.positive("max count", count);
    }

    public Function<? super Channel<?, IN>, ? extends Channel<?, OUT>> apply(
            final StreamConfiguration streamConfiguration,
            final Function<Channel<?, IN>, Channel<?, OUT>> function) {
        return new BindingFunction(streamConfiguration.asChannelConfiguration(), function);
    }

    /**
     * Binding function implementation.
     */
    private class BindingFunction
            implements Function<Channel<?, IN>, Channel<?, OUT>>, CompletionHandler {

        private final Function<? super Channel<?, IN>, ? extends Channel<?, OUT>> mBindingFunction;

        private final ChannelConfiguration mConfiguration;

        /**
         * Constructor.
         *
         * @param configuration   the channel configuration.
         * @param bindingFunction the binding function.
         */
        private BindingFunction(@NotNull final ChannelConfiguration configuration,
                @NotNull final Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>
                        bindingFunction) {
            mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
            mBindingFunction = ConstantConditions.notNull("binding function", bindingFunction);
        }

        public Channel<?, OUT> apply(final Channel<?, IN> channel) throws Exception {
            final ChannelConfiguration configuration = mConfiguration;
            final Channel<OUT, OUT> outputChannel =
                    JRoutineCore.io().apply(configuration).buildChannel();
            final boolean isBind;
            synchronized (mMutex) {
                isBind = (++mCount <= mMaxCount);
                if (!isBind) {
                    mQueue.add(new Runnable() {

                        public void run() {
                            try {
                                mBindingFunction.apply(channel)
                                                .bind(new ThrottleChannelConsumer<OUT>(
                                                        BindingFunction.this, outputChannel));
                            } catch (final Exception e) {
                                outputChannel.abort(e);
                                onComplete();
                            }
                        }
                    });
                }
            }

            if (isBind) {
                mBindingFunction.apply(channel)
                                .bind(new ThrottleChannelConsumer<OUT>(this, outputChannel));
            }

            return outputChannel;
        }

        public void onComplete() {
            final Runnable runnable;
            synchronized (mMutex) {
                --mCount;
                final SimpleQueue<Runnable> queue = mQueue;
                if (queue.isEmpty()) {
                    return;
                }

                runnable = queue.removeFirst();
            }

            runnable.run();
        }
    }
}

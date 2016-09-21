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

package com.github.dm.jrt.stream.transform;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.SimpleQueue;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.stream.builder.StreamBuilder.StreamConfiguration;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Invocation time range throttle binding function.
 * <p>
 * Created by davide-maestroni on 07/30/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class BindTimeThrottle<IN, OUT> implements
        BiFunction<StreamConfiguration, Function<Channel<?, IN>, Channel<?, OUT>>, Function<?
                super Channel<?, IN>, ? extends Channel<?, OUT>>> {

    private final int mMaxCount;

    private final Object mMutex = new Object();

    private final SimpleQueue<Runnable> mQueue = new SimpleQueue<Runnable>();

    private final long mRangeMillis;

    private int mCount;

    private long mNextTimeSlot = Long.MIN_VALUE;

    /**
     * Constructor.
     *
     * @param count    the maximum invocation count.
     * @param range    the time range value.
     * @param timeUnit the time range unit.
     */
    BindTimeThrottle(final int count, final long range, @NotNull final TimeUnit timeUnit) {
        mMaxCount = ConstantConditions.positive("max count", count);
        mRangeMillis = timeUnit.toMillis(range);
    }

    public Function<? super Channel<?, IN>, ? extends Channel<?, OUT>> apply(
            final StreamConfiguration streamConfiguration,
            final Function<Channel<?, IN>, Channel<?, OUT>> function) {
        return new BindingFunction(streamConfiguration.toChannelConfiguration(), function);
    }

    /**
     * Binding function implementation.
     */
    private class BindingFunction implements Function<Channel<?, IN>, Channel<?, OUT>>, Execution {

        private final Function<? super Channel<?, IN>, ? extends Channel<?, OUT>> mBindingFunction;

        private final ChannelConfiguration mConfiguration;

        private final Runner mRunner;

        /**
         * Constructor.
         *
         * @param configuration   the channel configuration.
         * @param bindingFunction the binding function.
         */
        BindingFunction(@NotNull final ChannelConfiguration configuration,
                @NotNull final Function<? super Channel<?, IN>, ? extends Channel<?, OUT>>
                        bindingFunction) {
            mConfiguration = ConstantConditions.notNull("channel configuration", configuration);
            mBindingFunction = ConstantConditions.notNull("binding function", bindingFunction);
            mRunner = configuration.getRunnerOrElse(Runners.sharedRunner());
        }

        public Channel<?, OUT> apply(final Channel<?, IN> channel) throws Exception {
            final ChannelConfiguration configuration = mConfiguration;
            final Channel<OUT, OUT> outputChannel =
                    JRoutineCore.io().apply(configuration).buildChannel();
            final long delay;
            final boolean isBind;
            final Runner runner = mRunner;
            synchronized (mMutex) {
                final long rangeMillis = mRangeMillis;
                final long nextTimeSlot = mNextTimeSlot;
                final long now = System.currentTimeMillis();
                if ((nextTimeSlot == Long.MIN_VALUE) || (now >= (nextTimeSlot + rangeMillis))) {
                    mNextTimeSlot = now + rangeMillis;
                    mCount = 0;
                }

                final int maxCount = mMaxCount;
                isBind = (++mCount <= maxCount);
                if (!isBind) {
                    final SimpleQueue<Runnable> queue = mQueue;
                    queue.add(new Runnable() {

                        public void run() {
                            try {
                                mBindingFunction.apply(channel).bind(outputChannel);

                            } catch (final Throwable t) {
                                outputChannel.abort(t);
                                InvocationInterruptedException.throwIfInterrupt(t);
                            }
                        }
                    });

                    delay = (((queue.size() - 1) / maxCount) + 1) * rangeMillis;

                } else {
                    delay = 0;
                }
            }

            if (isBind) {
                mBindingFunction.apply(channel).bind(outputChannel);

            } else {
                runner.run(this, delay, TimeUnit.MILLISECONDS);
            }

            return outputChannel;
        }

        public void run() {
            final Runnable runnable;
            synchronized (mMutex) {
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

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

package com.github.dm.jrt.stream.modifier;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.operator.Operators;

import org.jetbrains.annotations.NotNull;

/**
 * Binding function generating the output returned by the specified consumer.
 * <p>
 * Created by davide-maestroni on 07/12/2016.
 *
 * @param <OUT> the output data type.
 */
class BindOutputConsumer<OUT> implements Function<Channel<?, ?>, Channel<?, OUT>> {

    private final InvocationConfiguration mConfiguration;

    private final long mCount;

    private final InvocationMode mInvocationMode;

    private final Consumer<? super Channel<OUT, ?>> mOutputConsumer;

    /**
     * Constructor.
     *
     * @param configuration   the invocation configuration.
     * @param invocationMode  the invocation mode.
     * @param count           the loop count.
     * @param outputsConsumer the consumer instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    BindOutputConsumer(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, final long count,
            @NotNull final Consumer<? super Channel<OUT, ?>> outputsConsumer) {
        mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
        mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        mCount = ConstantConditions.positive("count number", count);
        mOutputConsumer = ConstantConditions.notNull("consumer instance", outputsConsumer);
    }

    public Channel<?, OUT> apply(final Channel<?, ?> channel) {
        return mInvocationMode.invoke(
                JRoutineCore.with(Operators.appendAccept(mCount, mOutputConsumer))
                            .apply(mConfiguration)).close();
    }
}

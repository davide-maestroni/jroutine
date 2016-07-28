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

package com.github.dm.jrt.stream.processor;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.operator.Operators;

import org.jetbrains.annotations.NotNull;

/**
 * Binding function generating the output returned by the specified supplier.
 * <p>
 * Created by davide-maestroni on 07/12/2016.
 *
 * @param <OUT> the output data type.
 */
class BindOutputSupplier<OUT> implements Function<Channel<?, ?>, Channel<?, OUT>> {

    private final InvocationConfiguration mConfiguration;

    private final long mCount;

    private final InvocationMode mInvocationMode;

    private final Supplier<? extends OUT> mOutputSupplier;

    /**
     * Constructor.
     *
     * @param configuration  the invocation configuration.
     * @param invocationMode the invocation mode.
     * @param count          the loop count.
     * @param outputSupplier the supplier instance.
     * @throws java.lang.IllegalArgumentException if the specified count number is 0 or negative.
     */
    BindOutputSupplier(@NotNull final InvocationConfiguration configuration,
            @NotNull final InvocationMode invocationMode, final long count,
            @NotNull final Supplier<? extends OUT> outputSupplier) {
        mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
        mInvocationMode = ConstantConditions.notNull("invocation mode", invocationMode);
        mCount = ConstantConditions.positive("count number", count);
        mOutputSupplier = ConstantConditions.notNull("supplier instance", outputSupplier);
    }

    public Channel<?, OUT> apply(final Channel<?, ?> channel) {
        return mInvocationMode.invoke(
                JRoutineCore.with(Operators.appendGet(mCount, mOutputSupplier))
                            .invocationConfiguration()
                            .with(mConfiguration)
                            .configured()).close();
    }
}

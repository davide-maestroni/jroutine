/*
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
package com.github.dm.jrt.function;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.PassingInvocation;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.function.Functions.consumerCommand;
import static com.github.dm.jrt.function.Functions.supplierCommand;

/**
 * Default implementation of a functional routine builder.
 * <p/>
 * Created by davide-maestroni on 10/18/2015.
 */
class DefaultFunctionalRoutineBuilder
        implements FunctionalRoutineBuilder, Configurable<FunctionalRoutineBuilder> {

    private InvocationConfiguration mConfiguration = InvocationConfiguration.DEFAULT_CONFIGURATION;

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> buildFrom(
            @NotNull final CommandInvocation<OUT> invocation) {

        return new DefaultFunctionalRoutine<Void, OUT>(
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine());
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> buildFrom(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return buildFrom(consumerCommand(consumer));
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> buildFrom(@NotNull final Supplier<OUT> supplier) {

        return buildFrom(supplierCommand(supplier));
    }

    @NotNull
    public <DATA> FunctionalRoutine<DATA, DATA> buildRoutine() {

        return new DefaultFunctionalRoutine<DATA, DATA>(
                JRoutine.on(PassingInvocation.<DATA>factoryOf())
                        .invocations()
                        .with(mConfiguration)
                        .set()
                        .buildRoutine());
    }

    @NotNull
    public Builder<? extends FunctionalRoutineBuilder> invocations() {

        return new Builder<FunctionalRoutineBuilder>(this, mConfiguration);
    }

    @NotNull
    @SuppressWarnings("ConstantConditions")
    public FunctionalRoutineBuilder setConfiguration(
            @NotNull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the invocation configuration must not be null");
        }

        mConfiguration = configuration;
        return this;
    }
}

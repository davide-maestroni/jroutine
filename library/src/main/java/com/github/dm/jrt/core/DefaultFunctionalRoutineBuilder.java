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
package com.github.dm.jrt.core;

import com.github.dm.jrt.builder.FunctionalRoutineBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.Configurable;
import com.github.dm.jrt.functional.Functions;
import com.github.dm.jrt.functional.Supplier;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.PassingInvocation;
import com.github.dm.jrt.routine.FunctionalRoutine;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;

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

        final Routine<Void, OUT> routine =
                JRoutine.on(invocation).invocations().with(mConfiguration).set().buildRoutine();
        return new DefaultFunctionalRoutine<Void, OUT>(mConfiguration, routine);
    }

    @NotNull
    public <OUT> FunctionalRoutine<Void, OUT> buildFrom(@NotNull final Supplier<OUT> supplier) {

        return buildFrom(Functions.supplierCommand(supplier));
    }

    @NotNull
    public <DATA> FunctionalRoutine<DATA, DATA> buildRoutine() {

        final Routine<DATA, DATA> routine = JRoutine.on(PassingInvocation.<DATA>factoryOf())
                                                    .invocations()
                                                    .with(mConfiguration)
                                                    .set()
                                                    .buildRoutine();
        return new DefaultFunctionalRoutine<DATA, DATA>(mConfiguration, routine);
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

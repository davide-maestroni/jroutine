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

package com.github.dm.jrt.android.v4;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.SupplierDecorator;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Implementation of a context invocation factory based on a supplier function.
 * <p>
 * Created by davide-maestroni on 05/13/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class SupplierContextInvocationFactory<IN, OUT> extends ContextInvocationFactory<IN, OUT> {

    private final SupplierDecorator<? extends ContextInvocation<? super IN, ? extends OUT>>
            mSupplier;

    /**
     * Constructor.
     *
     * @param supplier the supplier function.
     */
    SupplierContextInvocationFactory(
            @NotNull final SupplierDecorator<? extends ContextInvocation<? super IN, ? extends
                    OUT>> supplier) {
        super(asArgs(ConstantConditions.notNull("supplier wrapper", supplier)));
        mSupplier = supplier;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public ContextInvocation<IN, OUT> newInvocation() throws Exception {
        return (ContextInvocation<IN, OUT>) mSupplier.get();
    }
}

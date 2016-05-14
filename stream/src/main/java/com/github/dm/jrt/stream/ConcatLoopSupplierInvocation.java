/*
 * Copyright (c) 2016. Davide Maestroni
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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.SupplierWrapper;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Invocation concatenating the outputs produced by a supplier.
 * <p>
 * Created by davide-maestroni on 05/12/2016.
 *
 * @param <DATA> the data type.
 */
class ConcatLoopSupplierInvocation<DATA> extends GenerateInvocation<DATA, DATA> {

    private final long mCount;

    private final SupplierWrapper<? extends DATA> mSupplier;

    /**
     * Constructor.
     *
     * @param count    the loop count.
     * @param supplier the supplier instance.
     */
    ConcatLoopSupplierInvocation(final long count,
            @NotNull final SupplierWrapper<? extends DATA> supplier) {

        super(asArgs(ConstantConditions.positive("count number", count),
                ConstantConditions.notNull("supplier instance", supplier)));
        mCount = count;
        mSupplier = supplier;
    }

    public void onInput(final DATA input, @NotNull final ResultChannel<DATA> result) {

        result.pass(input);
    }

    public void onResult(@NotNull final ResultChannel<DATA> result) throws Exception {

        final long count = mCount;
        final SupplierWrapper<? extends DATA> supplier = mSupplier;
        for (long i = 0; i < count; ++i) {
            result.pass(supplier.get());
        }
    }
}

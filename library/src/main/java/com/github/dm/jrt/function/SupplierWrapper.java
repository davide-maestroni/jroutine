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

import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;

/**
 * Class wrapping a supplier instance.
 * <p/>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <OUT> the output data type.
 */
public class SupplierWrapper<OUT> implements Supplier<OUT> {

    private final FunctionWrapper<?, OUT> mFunction;

    private final Supplier<?> mSupplier;

    /**
     * Constructor.
     *
     * @param supplier the initial wrapped supplier.
     * @param function the concatenated function chain.
     */
    @SuppressWarnings("ConstantConditions")
    SupplierWrapper(@NotNull final Supplier<?> supplier,
            @NotNull final FunctionWrapper<?, OUT> function) {

        if (supplier == null) {

            throw new NullPointerException("the supplier instance must not be null");
        }

        if (function == null) {

            throw new NullPointerException("the function chain must not be null");
        }

        mSupplier = supplier;
        mFunction = function;
    }

    /**
     * Returns a composed supplier wrapper that first gets this supplier result, and then applies
     * the after function to it.
     *
     * @param after   the function to apply after this function is applied.
     * @param <AFTER> the type of output of the after function.
     * @return the composed function.
     */
    @NotNull
    public <AFTER> SupplierWrapper<AFTER> andThen(
            @NotNull final Function<? super OUT, AFTER> after) {

        return new SupplierWrapper<AFTER>(mSupplier, mFunction.andThen(after));
    }

    @SuppressWarnings("unchecked")
    public OUT get() {

        return ((Function<Object, OUT>) mFunction).apply(mSupplier.get());
    }

    /**
     * Checks if the supplier and functions wrapped by this instance have a static context.
     *
     * @return whether the supplier and functions have a static context.
     */
    public boolean hasStaticContext() {

        final Supplier<?> supplier = mSupplier;
        return Reflection.hasStaticContext(supplier.getClass()) && mFunction.hasStaticContext();
    }

    @Override
    public int hashCode() {

        int result = mFunction.hashCode();
        result = 31 * result + mSupplier.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {

            return false;
        }

        final SupplierWrapper<?> that = (SupplierWrapper<?>) o;
        return mFunction.equals(that.mFunction) && mSupplier.equals(that.mSupplier);
    }
}

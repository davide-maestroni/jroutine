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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

/**
 * Class wrapping a supplier instance.
 * <p>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <OUT> the output data type.
 */
public class SupplierWrapper<OUT> implements Supplier<OUT>, Wrapper {

    private final FunctionWrapper<?, OUT> mFunction;

    private final Supplier<?> mSupplier;

    /**
     * Constructor.
     *
     * @param supplier the initial wrapped supplier.
     */
    SupplierWrapper(@NotNull final Supplier<?> supplier) {

        this(ConstantConditions.notNull("supplier instance", supplier),
                FunctionWrapper.<OUT>identity());
    }

    /**
     * Constructor.
     *
     * @param supplier the initial wrapped supplier.
     * @param function the concatenated function chain.
     */
    private SupplierWrapper(@NotNull final Supplier<?> supplier,
            @NotNull final FunctionWrapper<?, OUT> function) {

        mSupplier = supplier;
        mFunction = function;
    }

    /**
     * Returns a supplier wrapper always returning the same result.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param result the result.
     * @param <OUT>  the output data type.
     * @return the supplier wrapper.
     */
    @NotNull
    public static <OUT> SupplierWrapper<OUT> constant(final OUT result) {

        return new SupplierWrapper<OUT>(new ConstantSupplier<OUT>(result));
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
            @NotNull final Function<? super OUT, ? extends AFTER> after) {

        return new SupplierWrapper<AFTER>(mSupplier, mFunction.andThen(after));
    }

    @SuppressWarnings("unchecked")
    public OUT get() {

        return ((Function<Object, OUT>) mFunction).apply(mSupplier.get());
    }

    public boolean hasStaticScope() {

        return Reflection.hasStaticScope(mSupplier) && mFunction.hasStaticScope();
    }

    @Override
    public int hashCode() {

        // AUTO-GENERATED CODE
        int result = mFunction.hashCode();
        result = 31 * result + mSupplier.hashCode();
        return result;
    }

    /**
     * Supplier implementation returning always the same object.
     *
     * @param <OUT> the output data type.
     */
    private static class ConstantSupplier<OUT> implements Supplier<OUT> {

        private final OUT mResult;

        /**
         * Constructor.
         *
         * @param result the object to return.
         */
        private ConstantSupplier(final OUT result) {

            mResult = result;
        }

        public OUT get() {

            return mResult;
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            return mResult != null ? mResult.hashCode() : 0;
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {
                return true;
            }

            if (!(o instanceof ConstantSupplier)) {
                return false;
            }

            final ConstantSupplier<?> that = (ConstantSupplier<?>) o;
            return (mResult == that.mResult);
        }
    }

    @Override
    public boolean equals(final Object o) {

        // AUTO-GENERATED CODE
        if (this == o) {
            return true;
        }

        if (!(o instanceof SupplierWrapper)) {
            return false;
        }

        final SupplierWrapper<?> that = (SupplierWrapper<?>) o;
        return mFunction.equals(that.mFunction) && mSupplier.equals(that.mSupplier);
    }
}

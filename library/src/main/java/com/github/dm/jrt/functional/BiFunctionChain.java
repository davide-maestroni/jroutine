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
package com.github.dm.jrt.functional;

import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN1> the first input data type.
 * @param <IN2> the second input data type.
 * @param <OUT> the output data type.
 */
public class BiFunctionChain<IN1, IN2, OUT> implements BiFunction<IN1, IN2, OUT> {

    private final BiFunction<IN1, IN2, ?> mBiFunction;

    private final FunctionChain<?, OUT> mFunction;

    /**
     * Constructor.
     *
     * @param biFunction the initial wrapped supplier.
     * @param function   the concatenated function chain.
     */
    @SuppressWarnings("ConstantConditions")
    BiFunctionChain(@NotNull final BiFunction<IN1, IN2, ?> biFunction,
            @NotNull final FunctionChain<?, OUT> function) {

        if (biFunction == null) {

            throw new NullPointerException("the bi-function instance must not be null");
        }

        if (function == null) {

            throw new NullPointerException("the function chain must not be null");
        }

        mBiFunction = biFunction;
        mFunction = function;
    }

    /**
     * Returns a composed bi-function chain that first applies this function to its input, and then
     * applies the after function to the result.
     *
     * @param after   the function to apply after this function is applied.
     * @param <AFTER> the type of output of the after function.
     * @return the composed function.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public <AFTER> BiFunctionChain<IN1, IN2, AFTER> andThen(
            @NotNull final Function<? super OUT, AFTER> after) {

        return new BiFunctionChain<IN1, IN2, AFTER>(mBiFunction, mFunction.andThen(after));
    }

    @SuppressWarnings("unchecked")
    public OUT apply(final IN1 in1, final IN2 in2) {

        return ((FunctionChain<Object, OUT>) mFunction).apply(mBiFunction.apply(in1, in2));
    }

    /**
     * Checks if this supplier chain has a static context.
     *
     * @return whether this instance has a static context.
     */
    public boolean hasStaticContext() {

        final BiFunction<IN1, IN2, ?> biFunction = mBiFunction;
        return Reflection.hasStaticContext(biFunction.getClass()) && mFunction.hasStaticContext();
    }

    @Override
    public int hashCode() {

        int result = mBiFunction.getClass().hashCode();
        result = 31 * result + mFunction.hashCode();
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

        final BiFunctionChain<?, ?, ?> that = (BiFunctionChain<?, ?, ?>) o;
        return (mBiFunction.getClass() == that.mBiFunction.getClass()) && mFunction.equals(
                that.mFunction);
    }
}

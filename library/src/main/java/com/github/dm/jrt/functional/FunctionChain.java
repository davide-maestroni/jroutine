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

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Class wrapping a function instance.
 * <p/>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class FunctionChain<IN, OUT> implements Function<IN, OUT> {

    private final List<Function<?, ?>> mFunctions;

    /**
     * Constructor.
     *
     * @param functions the list of wrapped functions.
     */
    FunctionChain(@NotNull final List<Function<?, ?>> functions) {

        mFunctions = functions;
    }

    /**
     * Returns a composed function chain that first applies this function to its input, and then
     * applies the after function to the result.
     *
     * @param after   the function to apply after this function is applied.
     * @param <AFTER> the type of output of the after function.
     * @return the composed function.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public <AFTER> FunctionChain<IN, AFTER> andThen(
            @NotNull final Function<? super OUT, AFTER> after) {

        final Class<? extends Function> functionClass = after.getClass();
        final List<Function<?, ?>> functions = mFunctions;
        final ArrayList<Function<?, ?>> newFunctions =
                new ArrayList<Function<?, ?>>(functions.size() + 1);
        newFunctions.addAll(functions);

        if (functionClass == FunctionChain.class) {

            newFunctions.addAll(((FunctionChain<?, ?>) after).mFunctions);

        } else {

            newFunctions.add(after);
        }

        return new FunctionChain<IN, AFTER>(newFunctions);
    }

    /**
     * Applies this function to the given argument.
     *
     * @param in the input argument.
     * @return the function result.
     */
    @SuppressWarnings("unchecked")
    public OUT apply(final IN in) {

        Object result = in;

        for (final Function<?, ?> function : mFunctions) {

            result = ((Function<Object, Object>) function).apply(result);
        }

        return (OUT) result;
    }

    /**
     * Returns a composed function chain that first applies the before function to its input,
     * and then applies this function to the result.
     *
     * @param before   the function to apply before this function is applied.
     * @param <BEFORE> the type of input to the before function.
     * @return the composed function.
     */
    @NotNull
    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "class comparison with == is done")
    public <BEFORE> FunctionChain<BEFORE, OUT> compose(
            @NotNull final Function<BEFORE, ? extends IN> before) {

        final Class<? extends Function> functionClass = before.getClass();
        final List<Function<?, ?>> functions = mFunctions;
        final ArrayList<Function<?, ?>> newFunctions =
                new ArrayList<Function<?, ?>>(functions.size() + 1);

        if (functionClass == FunctionChain.class) {

            newFunctions.addAll(((FunctionChain<?, ?>) before).mFunctions);

        } else {

            newFunctions.add(before);
        }

        newFunctions.addAll(functions);
        return new FunctionChain<BEFORE, OUT>(newFunctions);
    }

    /**
     * Checks if this function chain has a static context.
     *
     * @return whether this instance has a static context.
     */
    public boolean hasStaticContext() {

        for (final Function<?, ?> function : mFunctions) {

            if (!Reflection.hasStaticContext(function.getClass())) {

                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {

        int result = 0;

        for (final Function<?, ?> function : mFunctions) {

            result = 31 * result + function.getClass().hashCode();
        }

        return result;
    }

    @Override
    @SuppressFBWarnings(value = "EQ_GETCLASS_AND_CLASS_CONSTANT",
            justification = "comparing class of the internal list objects")
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {

            return false;
        }

        final FunctionChain<?, ?> that = (FunctionChain<?, ?>) o;
        final List<Function<?, ?>> thisFunctions = mFunctions;
        final List<Function<?, ?>> thatFunctions = that.mFunctions;
        final int size = thisFunctions.size();

        if (size != thatFunctions.size()) {

            return false;
        }

        for (int i = 0; i < size; ++i) {

            if (thisFunctions.get(i).getClass() != thatFunctions.get(i).getClass()) {

                return false;
            }
        }

        return true;
    }
}

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

import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class wrapping a function instance.
 * <p>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class FunctionWrapper<IN, OUT> implements Function<IN, OUT>, Wrapper {

    private static final FunctionWrapper<Object, Object> sIdentity =
            new FunctionWrapper<Object, Object>(new Function<Object, Object>() {

                public Object apply(final Object in) {

                    return in;
                }
            });

    private final List<Function<?, ?>> mFunctions;

    /**
     * Constructor.
     *
     * @param function the wrapped function.
     */
    FunctionWrapper(@NotNull final Function<?, ?> function) {

        this(Collections.<Function<?, ?>>singletonList(
                ConstantConditions.notNull("function instance", function)));
    }

    /**
     * Constructor.
     *
     * @param functions the list of wrapped functions.
     */
    private FunctionWrapper(@NotNull final List<Function<?, ?>> functions) {

        mFunctions = functions;
    }

    /**
     * Returns a function wrapper casting the passed inputs to the specified class.<br>
     * The returned object will support concatenation and comparison.
     *
     * @param type  the class type.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the function wrapper.
     */
    @NotNull
    public static <IN, OUT> FunctionWrapper<IN, OUT> castTo(
            @NotNull final Class<? extends OUT> type) {

        return new FunctionWrapper<IN, OUT>(
                new ClassCastFunction<IN, OUT>(ConstantConditions.notNull("type", type)));
    }

    /**
     * Returns a function wrapper casting the passed inputs to the specified class token type.<br>
     * The returned object will support concatenation and comparison.
     *
     * @param token the class token.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the function wrapper.
     */
    @NotNull
    public static <IN, OUT> FunctionWrapper<IN, OUT> castTo(
            @NotNull final ClassToken<? extends OUT> token) {

        return castTo(token.getRawClass());
    }

    /**
     * Returns the identity function wrapper.<br>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the function wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> FunctionWrapper<IN, IN> identity() {

        return (FunctionWrapper<IN, IN>) sIdentity;
    }

    /**
     * Returns a composed function wrapper that first applies this function to its input, and then
     * applies the after function to the result.
     *
     * @param after   the function to apply after this function is applied.
     * @param <AFTER> the type of output of the after function.
     * @return the composed function.
     */
    @NotNull
    public <AFTER> FunctionWrapper<IN, AFTER> andThen(
            @NotNull final Function<? super OUT, ? extends AFTER> after) {

        final List<Function<?, ?>> functions = mFunctions;
        final ArrayList<Function<?, ?>> newFunctions =
                new ArrayList<Function<?, ?>>(functions.size() + 1);
        newFunctions.addAll(functions);
        if (after instanceof FunctionWrapper) {
            newFunctions.addAll(((FunctionWrapper<?, ?>) after).mFunctions);

        } else {
            newFunctions.add(ConstantConditions.notNull("function instance", after));
        }

        return new FunctionWrapper<IN, AFTER>(newFunctions);
    }

    /**
     * Returns a composed function wrapper that first applies the before function to its input,
     * and then applies this function to the result.
     *
     * @param before   the function to apply before this function is applied.
     * @param <BEFORE> the type of input to the before function.
     * @return the composed function.
     */
    @NotNull
    public <BEFORE> FunctionWrapper<BEFORE, OUT> compose(
            @NotNull final Function<? super BEFORE, ? extends IN> before) {

        final List<Function<?, ?>> functions = mFunctions;
        final ArrayList<Function<?, ?>> newFunctions =
                new ArrayList<Function<?, ?>>(functions.size() + 1);
        if (before instanceof FunctionWrapper) {
            newFunctions.addAll(((FunctionWrapper<?, ?>) before).mFunctions);

        } else {
            newFunctions.add(ConstantConditions.notNull("function instance", before));
        }

        newFunctions.addAll(functions);
        return new FunctionWrapper<BEFORE, OUT>(newFunctions);
    }

    public boolean hasStaticScope() {

        for (final Function<?, ?> function : mFunctions) {
            if (!Reflection.hasStaticScope(function)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {

        // AUTO-GENERATED CODE
        return mFunctions.hashCode();
    }

    /**
     * Function implementation casting inputs to the specified class.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class ClassCastFunction<IN, OUT> implements Function<IN, OUT> {

        private final Class<? extends OUT> mType;

        /**
         * Constructor.
         *
         * @param type the output class type.
         */
        private ClassCastFunction(@NotNull final Class<? extends OUT> type) {

            mType = type;
        }

        public OUT apply(final IN in) {

            return mType.cast(in);
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            return mType.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {
                return true;
            }

            if (!(o instanceof ClassCastFunction)) {
                return false;
            }

            final ClassCastFunction<?, ?> that = (ClassCastFunction<?, ?>) o;
            return mType.equals(that.mType);
        }
    }

    @Override
    public boolean equals(final Object o) {

        // AUTO-GENERATED CODE
        if (this == o) {
            return true;
        }

        if (!(o instanceof FunctionWrapper)) {
            return false;
        }

        final FunctionWrapper<?, ?> that = (FunctionWrapper<?, ?>) o;
        return mFunctions.equals(that.mFunctions);
    }

    @SuppressWarnings("unchecked")
    public OUT apply(final IN in) {

        Object result = in;
        for (final Function<?, ?> function : mFunctions) {
            result = ((Function<Object, Object>) function).apply(result);
        }

        return (OUT) result;
    }
}

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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.FunctionWrapper;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of invocations collecting inputs into maps.
 * <p>
 * Created by davide-maestroni on 05/05/2016.
 *
 * @param <IN>  the input data type.
 * @param <KEY> the map key type.
 */
class ToMapInvocationFactory<IN, KEY> extends InvocationFactory<IN, Map<KEY, IN>> {

    private final FunctionWrapper<? super IN, KEY> mFunction;

    /**
     * Constructor.
     *
     * @param function the function extracting the key.
     */
    ToMapInvocationFactory(@NotNull final FunctionWrapper<? super IN, KEY> function) {
        super(asArgs(ConstantConditions.notNull("function instance", function)));
        mFunction = function;
    }

    @NotNull
    @Override
    public Invocation<IN, Map<KEY, IN>> newInvocation() {
        return new ToMapInvocation<IN, KEY>(mFunction);
    }

    /**
     * Invocation collecting inputs into a map.
     *
     * @param <IN>  the input data type.
     * @param <KEY> the map key type.
     */
    private static class ToMapInvocation<IN, KEY> extends TemplateInvocation<IN, Map<KEY, IN>> {

        private final FunctionWrapper<? super IN, KEY> mFunction;

        private HashMap<KEY, IN> mMap;

        /**
         * Constructor.
         *
         * @param function the function extracting the key.
         */
        private ToMapInvocation(@NotNull final FunctionWrapper<? super IN, KEY> function) {
            mFunction = function;
        }

        @Override
        public void onRecycle() {
            mMap = new HashMap<KEY, IN>();
        }

        @Override
        public void onInput(final IN input,
                @NotNull final ResultChannel<Map<KEY, IN>> result) throws Exception {
            mMap.put(mFunction.apply(input), input);
        }

        @Override
        public void onResult(@NotNull final ResultChannel<Map<KEY, IN>> result) {
            result.pass(mMap);
            mMap = null;
        }

        @Override
        public void onTerminate() {
            mMap = null;
        }
    }
}

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
package com.github.dm.jrt.stream;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.function.BiFunction;
import com.github.dm.jrt.function.BiFunctionWrapper;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.TemplateInvocation;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.function.Functions.wrapBiFunction;

/**
 * Invocation implementation accumulating the result returned by a bi-function instance.
 * <p/>
 * Created by davide-maestroni on 10/18/2015.
 *
 * @param <IN> the input data type.
 */
class AccumulateInvocation<IN> extends TemplateInvocation<IN, IN> {

    private final BiFunction<? super IN, ? super IN, ? extends IN> mFunction;

    private IN mAccumulated;

    private boolean mIsFirst;

    /**
     * Constructor.
     *
     * @param function the bi-function instance.
     */
    private AccumulateInvocation(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function) {

        mFunction = function;
    }

    /**
     * Builds and returns a new accumulating invocation factory backed by the specified bi-function
     * instance.
     *
     * @param function the bi-function instance.
     * @param <IN>     the input data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN> InvocationFactory<IN, IN> functionFactory(
            @NotNull final BiFunction<? super IN, ? super IN, ? extends IN> function) {

        return new AccumulateInvocationFactory<IN>(wrapBiFunction(function));
    }

    @Override
    public void onInitialize() {

        mIsFirst = true;
    }

    @Override
    public void onInput(final IN input, @NotNull final ResultChannel<IN> result) {

        if (mIsFirst) {

            mIsFirst = false;
            mAccumulated = input;

        } else {

            mAccumulated = mFunction.apply(mAccumulated, input);
        }
    }

    @Override
    public void onResult(@NotNull final ResultChannel<IN> result) {

        result.pass(mAccumulated);
    }

    @Override
    public void onTerminate() {

        mAccumulated = null;
    }

    /**
     * Class implementing an accumulating invocation factory.
     *
     * @param <IN> the input data type.
     */
    private static class AccumulateInvocationFactory<IN> extends InvocationFactory<IN, IN> {

        private final BiFunctionWrapper<? super IN, ? super IN, ? extends IN> mFunction;

        /**
         * Constructor.
         *
         * @param function the bi-function instance.
         */
        private AccumulateInvocationFactory(
                @NotNull final BiFunctionWrapper<? super IN, ? super IN, ? extends IN> function) {

            mFunction = function;
        }

        @Override
        public int hashCode() {

            return mFunction.typeHashCode();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof AccumulateInvocationFactory)) {

                return false;
            }

            final AccumulateInvocationFactory<?> that = (AccumulateInvocationFactory<?>) o;
            return mFunction.typeEquals(that.mFunction);
        }

        @NotNull
        @Override
        public Invocation<IN, IN> newInvocation() {

            return new AccumulateInvocation<IN>(mFunction);
        }
    }
}

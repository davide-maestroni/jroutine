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

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.operator.math.Numbers.addOptimistic;

/**
 * Invocation computing the average of the input numbers in integer precision.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 */
class AverageIntegerInvocation extends TemplateInvocation<Number, Integer> {

    private static final InvocationFactory<Number, Integer> sFactory =
            new InvocationFactory<Number, Integer>(null) {

                @NotNull
                @Override
                public Invocation<Number, Integer> newInvocation() {
                    return new AverageIntegerInvocation();
                }
            };

    private int mCount;

    private Number mSum;

    /**
     * Constructor.
     */
    private AverageIntegerInvocation() {
    }

    /**
     * Returns a factory of invocations computing the average of the input numbers in integer
     * precision.
     *
     * @return the factory instance.
     */
    @NotNull
    static InvocationFactory<Number, Integer> factoryOf() {
        return sFactory;
    }

    @Override
    public void onRecycle() {
        mSum = (byte) 0;
        mCount = 0;
    }

    @Override
    public void onInput(final Number input, @NotNull final ResultChannel<Integer> result) {
        mSum = addOptimistic(mSum, input).intValue();
        ++mCount;
    }

    @Override
    public void onResult(@NotNull final ResultChannel<Integer> result) {
        if (mCount == 0) {
            result.pass(0);

        } else {
            result.pass(mSum.intValue() / mCount);
        }
    }
}

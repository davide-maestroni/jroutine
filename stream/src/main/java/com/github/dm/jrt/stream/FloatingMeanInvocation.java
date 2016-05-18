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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static com.github.dm.jrt.stream.util.Numbers.addOptimistic;

/**
 * Invocation computing the mean of the input numbers in floating precision.
 * <br>
 * The result will have the type matching the input with the highest precision.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 */
class FloatingMeanInvocation extends TemplateInvocation<Number, Number> {

    private static final InvocationFactory<Number, Number> sFactory =
            new InvocationFactory<Number, Number>(null) {

                @NotNull
                @Override
                public Invocation<Number, Number> newInvocation() {

                    return new FloatingMeanInvocation();
                }
            };

    private int mCount;

    private Number mSum;

    /**
     * Constructor.
     */
    private FloatingMeanInvocation() {

    }

    /**
     * Returns a factory of invocations computing the mean of the input numbers in floating
     * precision.
     *
     * @return the factory instance.
     */
    @NotNull
    public static InvocationFactory<Number, Number> factoryOf() {

        return sFactory;
    }

    @Override
    public void onInitialize() {

        mSum = (byte) 0;
        mCount = 0;
    }

    @Override
    public void onInput(final Number input, @NotNull final ResultChannel<Number> result) {

        mSum = addOptimistic(mSum, input);
        ++mCount;
    }

    @Override
    public void onResult(@NotNull final ResultChannel<Number> result) {

        if (mCount == 0) {
            result.pass(0f);

        } else {
            final Number mean;
            final Number sum = mSum;
            if (sum instanceof BigDecimal) {
                mean = ((BigDecimal) sum).divide(new BigDecimal(mCount), 15,
                        RoundingMode.HALF_EVEN);

            } else if (sum instanceof BigInteger) {
                mean = new BigDecimal((BigInteger) sum).divide(new BigDecimal(mCount), 15,
                        RoundingMode.HALF_EVEN);

            } else if (sum instanceof Double) {
                mean = sum.doubleValue() / mCount;

            } else if (sum instanceof Float) {
                mean = sum.floatValue() / mCount;

            } else if (sum instanceof Long) {
                mean = sum.doubleValue() / mCount;

            } else if (sum instanceof Integer) {
                mean = sum.floatValue() / mCount;

            } else if (sum instanceof Short) {
                mean = sum.floatValue() / mCount;

            } else if (sum instanceof Byte) {
                mean = sum.floatValue() / mCount;

            } else {
                mean = sum.doubleValue() / mCount;
            }

            result.pass(mean);
        }
    }
}

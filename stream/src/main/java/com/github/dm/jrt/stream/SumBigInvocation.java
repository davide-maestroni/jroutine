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

import static com.github.dm.jrt.stream.util.Numbers.toBigDecimalOptimistic;

/**
 * Invocation computing the sum of the input numbers by employing a {@code BigDecimal}.
 * <p>
 * Created by davide-maestroni on 05/02/2016.
 */
class SumBigInvocation extends TemplateInvocation<Number, BigDecimal> {

    private static final InvocationFactory<Number, BigDecimal> sFactory =
            new InvocationFactory<Number, BigDecimal>(null) {

                @NotNull
                @Override
                public Invocation<Number, BigDecimal> newInvocation() {

                    return new SumBigInvocation();
                }
            };

    private BigDecimal mSum;

    /**
     * Constructor.
     */
    private SumBigInvocation() {

    }

    /**
     * Returns a factory of invocations computing the sum of the input numbers by employing a
     * {@code BigDecimal}.
     *
     * @return the factory instance.
     */
    @NotNull
    public static InvocationFactory<Number, BigDecimal> factoryOf() {

        return sFactory;
    }

    @Override
    public void onInitialize() {

        mSum = BigDecimal.ZERO;
    }

    @Override
    public void onInput(final Number input, @NotNull final ResultChannel<BigDecimal> result) {

        mSum = mSum.add(toBigDecimalOptimistic(input));
    }

    @Override
    public void onResult(@NotNull final ResultChannel<BigDecimal> result) {

        result.pass(mSum);
    }
}

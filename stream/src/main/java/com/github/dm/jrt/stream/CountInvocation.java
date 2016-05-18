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

/**
 * Invocation computing the number of outputs.
 * <p>
 * Created by davide-maestroni on 04/26/2016.
 */
class CountInvocation extends TemplateInvocation<Object, Long> {

    private static final InvocationFactory<Object, Long> sFactory =
            new InvocationFactory<Object, Long>(null) {

                @NotNull
                @Override
                public Invocation<Object, Long> newInvocation() throws Exception {

                    return new CountInvocation();
                }
            };

    private long mCount;

    /**
     * Constructor.
     */
    private CountInvocation() {

    }

    /**
     * Returns the factory of counting invocations.
     *
     * @return the factory instance.
     */
    @NotNull
    public static InvocationFactory<?, Long> factoryOf() {

        return sFactory;
    }

    @Override
    public void onInitialize() {

        mCount = 0;
    }

    @Override
    public void onInput(final Object input, @NotNull final ResultChannel<Long> result) {

        ++mCount;
    }

    @Override
    public void onResult(@NotNull final ResultChannel<Long> result) {

        result.pass(mCount);
    }
}

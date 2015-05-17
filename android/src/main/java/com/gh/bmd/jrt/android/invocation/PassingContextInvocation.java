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
package com.gh.bmd.jrt.android.invocation;

import com.gh.bmd.jrt.channel.ResultChannel;

import javax.annotation.Nonnull;

/**
 * Implementation of an invocation simply passing on the input data.
 * <p/>
 * Created by davide-maestroni on 1/12/14.
 *
 * @param <DATA> the data type.
 */
public class PassingContextInvocation<DATA> extends FilterContextInvocation<DATA, DATA> {

    private static final PassingContextInvocation<Object> sInvocation =
            new PassingContextInvocation<Object>();

    /**
     * Avoid instantiation.
     */
    private PassingContextInvocation() {

    }

    /**
     * Returns a factory of passing invocations.
     *
     * @param <DATA> the data type.
     * @return the factory.
     */
    @SuppressWarnings("unchecked")
    public static <DATA> ContextInvocationFactory<DATA, DATA> factoryOf() {

        return (ContextInvocationFactory<DATA, DATA>) sInvocation;
    }

    public void onInput(final DATA input, @Nonnull final ResultChannel<DATA> result) {

        result.pass(input);
    }
}

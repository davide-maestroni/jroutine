/**
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
package com.bmd.jrt.android.v11.routine;

import com.bmd.jrt.android.builder.InvocationMissingException;
import com.bmd.jrt.android.invocation.AndroidTemplateInvocation;
import com.bmd.jrt.channel.ResultChannel;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.invocation.InvocationFactory;

import javax.annotation.Nonnull;

/**
 * Invocation used to know whether a loader with a specific ID is present or not.
 * <p/>
 * Created by davide on 1/14/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class MissingLoaderInvocation<INPUT, OUTPUT> extends AndroidTemplateInvocation<INPUT, OUTPUT> {

    private static final MissingLoaderInvocation<Object, Object> sInvocation =
            new MissingLoaderInvocation<Object, Object>();

    private static final InvocationFactory<Object, Object> sFactory =
            new InvocationFactory<Object, Object>() {

                @Nonnull
                @Override
                public Invocation<Object, Object> createInvocation() {

                    return sInvocation;
                }
            };

    /**
     * Returns a factory of missing loader invocations.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the factory.
     */
    @SuppressWarnings("unchecked")
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf() {

        return (InvocationFactory<INPUT, OUTPUT>) sFactory;
    }

    @Override
    public void onResult(@Nonnull final ResultChannel<OUTPUT> result) {

        result.abort(new InvocationMissingException());
    }
}

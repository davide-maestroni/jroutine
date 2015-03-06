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
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.builder.RoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.invocation.InvocationFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class implementing a builder of routine objects based on an invocation class token.
 * <p/>
 * Created by davide on 9/21/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public class InvocationRoutineBuilder<INPUT, OUTPUT> implements RoutineBuilder<INPUT, OUTPUT> {

    private final InvocationFactory<INPUT, OUTPUT> mFactory;

    private RoutineConfiguration mConfiguration;

    /**
     * Constructor.
     *
     * @param factory the invocation factory.
     * @throws java.lang.NullPointerException if the class token is null.
     */
    @SuppressWarnings("ConstantConditions")
    InvocationRoutineBuilder(@Nonnull final InvocationFactory<INPUT, OUTPUT> factory) {

        if (factory == null) {

            throw new NullPointerException("the invocation factory must not be null");
        }

        mFactory = factory;
    }

    @Nonnull
    @Override
    public Routine<INPUT, OUTPUT> buildRoutine() {

        return new DefaultRoutine<INPUT, OUTPUT>(RoutineConfiguration.notNull(mConfiguration),
                                                 mFactory);
    }

    @Nonnull
    @Override
    public InvocationRoutineBuilder<INPUT, OUTPUT> withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }
}

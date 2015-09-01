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
package com.github.dm.jrt.core;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.log.Logger;

import javax.annotation.Nonnull;

/**
 * Default implementation of a routine object instantiating invocation objects through a factory.
 * <p/>
 * Created by davide-maestroni on 09/09/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultRoutine<IN, OUT> extends AbstractRoutine<IN, OUT> {

    private final InvocationFactory<IN, OUT> mFactory;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     * @param factory       the invocation factory.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultRoutine(@Nonnull final InvocationConfiguration configuration,
            @Nonnull final InvocationFactory<IN, OUT> factory) {

        super(configuration);

        if (factory == null) {

            throw new NullPointerException("the invocation factory must not be null");
        }

        mFactory = factory;
    }

    @Nonnull
    @Override
    protected Invocation<IN, OUT> newInvocation(@Nonnull final InvocationType type) {

        final Logger logger = getLogger();

        try {

            final InvocationFactory<IN, OUT> factory = mFactory;
            logger.dbg("creating a new invocation instance with factory: %s", factory);
            final Invocation<IN, OUT> invocation = factory.newInvocation();
            logger.dbg("created a new instance of class: %s", invocation.getClass());
            return invocation;

        } catch (final Throwable t) {

            logger.err(t, "error creating the invocation instance");
            throw InvocationException.wrapIfNeeded(t);
        }
    }
}

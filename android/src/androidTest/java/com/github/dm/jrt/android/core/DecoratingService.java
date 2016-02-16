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

package com.github.dm.jrt.android.core;

import com.github.dm.jrt.android.invocation.ContextInvocation;
import com.github.dm.jrt.android.invocation.ContextInvocationDecorator;
import com.github.dm.jrt.android.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.invocation.DecoratingContextInvocationFactory;
import com.github.dm.jrt.android.service.InvocationService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Invocation service decorating the invocation factory.
 * <p/>
 * Created by davide-maestroni on 10/06/2015.
 */
public class DecoratingService extends InvocationService {

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public ContextInvocationFactory<?, ?> getInvocationFactory(
            @NotNull final Class<? extends ContextInvocation<?, ?>> targetClass,
            @Nullable final Object... args) throws Exception {

        final ContextInvocationFactory<?, ?> factory =
                super.getInvocationFactory(targetClass, args);
        if (StringInvocation.class.isAssignableFrom(targetClass)) {
            return new TestInvocationFactory((ContextInvocationFactory<String, String>) factory);
        }

        return factory;
    }

    public interface StringInvocation extends ContextInvocation<String, String> {

    }

    private static class TestInvocationDecorator
            extends ContextInvocationDecorator<String, String> {

        /**
         * Constructor.
         *
         * @param wrapped the wrapped invocation instance.
         */
        public TestInvocationDecorator(@NotNull final ContextInvocation<String, String> wrapped) {

            super(wrapped);
        }
    }

    private static class TestInvocationFactory
            extends DecoratingContextInvocationFactory<String, String> {

        /**
         * Constructor.
         *
         * @param wrapped the wrapped factory instance.
         */
        public TestInvocationFactory(
                @NotNull final ContextInvocationFactory<String, String> wrapped) {

            super(wrapped);
        }

        @NotNull
        @Override
        protected ContextInvocation<String, String> decorate(
                @NotNull final ContextInvocation<String, String> invocation) throws Exception {

            return new TestInvocationDecorator(invocation);
        }
    }
}

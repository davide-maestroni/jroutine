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

import android.content.Context;

import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.invocation.Invocations;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.Reflection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class for creating context invocation factory objects.
 * <p/>
 * Created by davide-maestroni on 5/1/15.
 */
public class ContextInvocations {

    /**
     * Avoid direct instantiation.
     */
    protected ContextInvocations() {

    }

    /**
     * Converts the specified context invocation factory into a factory of invocations.
     *
     * @param context  the routine context.
     * @param factory  the context invocation factory.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryFrom(
            @Nonnull final Context context,
            @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> factory) {

        return new AdaptingContextInvocationFactory<INPUT, OUTPUT>(context, factory);
    }

    /**
     * Converts the specified invocation factory into a factory of context invocations.
     *
     * @param factory  the invocation factory.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the context invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> factoryFrom(
            @Nonnull final InvocationFactory<INPUT, OUTPUT> factory) {

        //TODO: equals/hashCode

        return new DecoratingContextInvocationFactory<INPUT, OUTPUT>(factory);
    }

    /**
     * Builds and returns a new context invocation factory creating instances of the specified
     * class.
     * <p/>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
     * first constructor parameter, and anonymous classes has both the outer instance and all the
     * variables captured in the closure.
     *
     * @param invocationClass the invocation class.
     * @param args            the invocation constructor arguments.
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass,
            @Nullable final Object... args) {

        return new DefaultContextInvocationFactory<INPUT, OUTPUT>(invocationClass, args);
    }

    /**
     * Builds and returns a new context invocation factory creating instances of the specified class
     * token.
     * <p/>
     * Note that class tokens of inner and anonymous classes can be passed as well. Remember however
     * that Java creates synthetic constructors for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation. In fact, inner classes always have the outer
     * instance as first constructor parameter, and anonymous classes has both the outer instance
     * and all the variables captured in the closure.
     *
     * @param invocationToken the invocation class token.
     * @param args            the invocation constructor arguments.
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> invocationToken,
            @Nullable final Object... args) {

        return factoryOf(invocationToken.getRawClass(), args);
    }

    /**
     * Implementation of an invocation factory.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class AdaptingContextInvocationFactory<INPUT, OUTPUT>
            implements InvocationFactory<INPUT, OUTPUT> {

        private final Context mContext;

        private final ContextInvocationFactory<INPUT, OUTPUT> mFactory;

        /**
         * Constructor.
         *
         * @param factory the context invocation class.
         */
        @SuppressWarnings("ConstantConditions")
        private AdaptingContextInvocationFactory(@Nonnull final Context context,
                @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> factory) {

            if (context == null) {

                throw new NullPointerException("the routine context must not be null");
            }

            if (factory == null) {

                throw new NullPointerException("the context invocation factory must not be null");
            }

            mContext = context;
            mFactory = factory;
        }

        @Nonnull
        public Invocation<INPUT, OUTPUT> newInvocation() {

            final ContextInvocation<INPUT, OUTPUT> invocation = mFactory.newInvocation();
            invocation.onContext(mContext);
            return invocation;
        }
    }

    /**
     * Implementation of an invocation factory decorating base invocations.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class DecoratingContextInvocationFactory<INPUT, OUTPUT>
            extends AbstractContextInvocationFactory<INPUT, OUTPUT> {

        private final InvocationFactory<INPUT, OUTPUT> mFactory;

        /**
         * Constructor.
         *
         * @param factory the invocation factory.
         */
        @SuppressWarnings("ConstantConditions")
        private DecoratingContextInvocationFactory(
                @Nonnull final InvocationFactory<INPUT, OUTPUT> factory) {

            super(factory);

            if (factory == null) {

                throw new NullPointerException("the invocation factory must not be null");
            }

            mFactory = factory;
        }

        @Nonnull
        public ContextInvocation<INPUT, OUTPUT> newInvocation() {

            return new ContextInvocationDecorator<INPUT, OUTPUT>(mFactory.newInvocation());
        }
    }

    /**
     * Default implementation of an invocation factory.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class DefaultContextInvocationFactory<INPUT, OUTPUT>
            extends AbstractContextInvocationFactory<INPUT, OUTPUT> {

        private final InvocationFactory<INPUT, OUTPUT> mFactory;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param args            the invocation constructor arguments.
         */
        private DefaultContextInvocationFactory(
                @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass,
                @Nullable final Object[] args) {

            super(invocationClass, (args != null) ? args : Reflection.NO_ARGS);
            mFactory = Invocations.factoryOf(
                    (Class<? extends Invocation<INPUT, OUTPUT>>) invocationClass, args);
        }

        @Nonnull
        public ContextInvocation<INPUT, OUTPUT> newInvocation() {

            return (ContextInvocation<INPUT, OUTPUT>) mFactory.newInvocation();
        }
    }
}

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
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> contextFactoryOf(
            @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> invocationToken,
            @Nullable final Object... args) {

        return contextFactoryOf(invocationToken.getRawClass(), args);
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
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> contextFactoryOf(
            @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass,
            @Nullable final Object... args) {

        return new DecoratingContextInvocationFactory<INPUT, OUTPUT>(invocationClass, args);
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
     * Builds and returns a new invocation factory creating instances of the specified class.
     * <p/>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructor for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
     * first constructor parameter, and anonymous classes has both the outer instance and all the
     * variables captured in the closure.
     *
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @param context         the routine context.
     * @param invocationClass the invocation class.
     * @param args            the invocation constructor arguments.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> invocationFactoryOf(
            @Nonnull final Context context,
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass,
            @Nullable final Object... args) {

        return new AdaptingContextInvocationFactory<INPUT, OUTPUT>(context, invocationClass, args);
    }

    /**
     * Builds and returns a new invocation factory creating instances of the specified class token.
     * <p/>
     * Note that class tokens of inner and anonymous classes can be passed as well. Remember however
     * that Java creates synthetic constructors for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation. In fact, inner classes always have the outer
     * instance as first constructor parameter, and anonymous classes has both the outer instance
     * and all the variables captured in the closure.
     *
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @param context         the routine context.
     * @param invocationToken the invocation class token.
     * @param args            the invocation constructor arguments.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> invocationFactoryOf(
            @Nonnull final Context context,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> invocationToken,
            @Nullable final Object... args) {

        return invocationFactoryOf(context, invocationToken.getRawClass(), args);
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

        private final InvocationFactory<INPUT, OUTPUT> mFactory;

        /**
         * Constructor.
         *
         * @param context         the routine context.
         * @param invocationClass the invocation class.
         */
        @SuppressWarnings("ConstantConditions")
        private AdaptingContextInvocationFactory(@Nonnull final Context context,
                @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass,
                @Nullable final Object[] args) {

            if (context == null) {

                throw new NullPointerException("the routine context must not be null");
            }

            mContext = context;
            mFactory = Invocations.factoryOf(invocationClass, args);
        }

        @Nonnull
        public Invocation<INPUT, OUTPUT> newInvocation() {

            final ContextInvocation<INPUT, OUTPUT> invocation =
                    (ContextInvocation<INPUT, OUTPUT>) mFactory.newInvocation();
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
            extends TemplateContextInvocationFactory<INPUT, OUTPUT> {

        private final InvocationFactory<INPUT, OUTPUT> mFactory;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param args            the invocation constructor arguments.
         */
        private DecoratingContextInvocationFactory(
                @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass,
                @Nullable final Object[] args) {

            super(invocationClass, (args != null) ? args : Reflection.NO_ARGS);
            mFactory = Invocations.factoryOf(invocationClass, args);
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
            extends TemplateContextInvocationFactory<INPUT, OUTPUT> {

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

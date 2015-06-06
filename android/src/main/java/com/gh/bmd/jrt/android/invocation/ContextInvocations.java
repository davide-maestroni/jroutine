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

import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationFactory;
import com.gh.bmd.jrt.invocation.Invocations;
import com.gh.bmd.jrt.invocation.Invocations.Function;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.Reflection;

import java.util.List;

import javax.annotation.Nonnull;

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
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> contextFactoryOf(
            @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> invocationToken) {

        return contextFactoryOf(invocationToken.getRawClass());
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
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> contextFactoryOf(
            @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass) {

        return new DecoratingContextInvocationFactory<INPUT, OUTPUT>(invocationClass);
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
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> invocationToken) {

        return factoryOf(invocationToken.getRawClass());
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
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> ContextInvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass) {

        return new DefaultContextInvocationFactory<INPUT, OUTPUT>(invocationClass);
    }

    /**
     * Builds and returns a new factory of context invocations calling the specified function.<br/>
     * In order to prevent undesired leaks, the class of the specified function must be static.<br/>
     * The function class will be used as invocation type.<br/>
     * Remember to force the input order type, in case the function parameter position needs to be
     * preserved.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     * @throws java.lang.IllegalArgumentException if the class of the specified function is not
     *                                            static.
     */
    @Nonnull
    public static <OUTPUT> ContextInvocationFactory<Object, OUTPUT> factoryOf(
            @Nonnull final Function<OUTPUT> function) {

        return new FunctionContextInvocationFactory<OUTPUT>(function);
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
     * @param invocationClass the invocation class.
     * @param context         the routine context.
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> invocationFactoryOf(
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass,
            @Nonnull final Context context) {

        return new AdaptingContextInvocationFactory<INPUT, OUTPUT>(invocationClass, context);
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
     * @param invocationToken the invocation class token.
     * @param context         the routine context.
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> invocationFactoryOf(
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> invocationToken,
            @Nonnull final Context context) {

        return invocationFactoryOf(invocationToken.getRawClass(), context);
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

        private final String mInvocationType;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param context         the routine context.
         */
        @SuppressWarnings("ConstantConditions")
        private AdaptingContextInvocationFactory(
                @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass,
                @Nonnull final Context context) {

            if (context == null) {

                throw new NullPointerException("the routine context must not be null");
            }

            mContext = context;
            mFactory = Invocations.factoryOf(invocationClass);
            mInvocationType = invocationClass.getName();
        }

        @Nonnull
        public String getInvocationType() {

            return mInvocationType;
        }

        @Nonnull
        public Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

            final ContextInvocation<INPUT, OUTPUT> invocation =
                    (ContextInvocation<INPUT, OUTPUT>) mFactory.newInvocation(args);
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
            implements ContextInvocationFactory<INPUT, OUTPUT> {

        private final InvocationFactory<INPUT, OUTPUT> mFactory;

        private final String mInvocationType;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         */
        private DecoratingContextInvocationFactory(
                @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass) {

            mFactory = Invocations.factoryOf(invocationClass);
            mInvocationType = invocationClass.getName();
        }

        @Nonnull
        public String getInvocationType() {

            return mInvocationType;
        }

        @Nonnull
        public ContextInvocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

            return new ContextInvocationDecorator<INPUT, OUTPUT>(mFactory.newInvocation(args));
        }
    }

    /**
     * Default implementation of an invocation factory.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class DefaultContextInvocationFactory<INPUT, OUTPUT>
            implements ContextInvocationFactory<INPUT, OUTPUT> {

        private final InvocationFactory<INPUT, OUTPUT> mFactory;

        private final String mInvocationType;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         */
        private DefaultContextInvocationFactory(
                @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass) {

            mFactory = Invocations.factoryOf(
                    (Class<? extends Invocation<INPUT, OUTPUT>>) invocationClass);
            mInvocationType = invocationClass.getName();
        }

        @Nonnull
        public String getInvocationType() {

            return mInvocationType;
        }

        @Nonnull
        public ContextInvocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

            return (ContextInvocation<INPUT, OUTPUT>) mFactory.newInvocation(args);
        }
    }

    /**
     * Implementation of a factory of invocations calling a specific function.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class FunctionContextInvocationFactory<OUTPUT>
            implements ContextInvocationFactory<Object, OUTPUT> {

        private final Function<OUTPUT> mFunction;

        private final String mInvocationType;

        /**
         * Constructor.
         *
         * @param function the function instance.
         */
        @SuppressWarnings("ConstantConditions")
        private FunctionContextInvocationFactory(@Nonnull final Function<OUTPUT> function) {

            final Class<? extends Function> functionClass = function.getClass();

            if (!Reflection.isStaticClass(functionClass)) {

                throw new IllegalArgumentException(
                        "the function class must be static: " + functionClass.getName());
            }

            mFunction = function;
            mInvocationType = functionClass.getName();
        }

        @Nonnull
        public String getInvocationType() {

            return mInvocationType;
        }

        @Nonnull
        public ContextInvocation<Object, OUTPUT> newInvocation(@Nonnull final Object... args) {

            return new FunctionContextInvocation<Object, OUTPUT>() {

                @Override
                public void onCall(@Nonnull final List<?> objects,
                        @Nonnull final ResultChannel<OUTPUT> result) {

                    result.pass(mFunction.call(objects.toArray()));
                }
            };
        }
    }
}

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
package com.bmd.jrt.invocation;

import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.InvocationException;
import com.bmd.jrt.common.Reflection;
import com.bmd.jrt.common.RoutineException;

import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.common.Reflection.findConstructor;

/**
 * Utility class for creating invocation factory objects.
 * <p/>
 * Created by davide on 2/12/15.
 */
public class Invocations {

    /**
     * Avoid direct instantiation.
     */
    protected Invocations() {

    }

    /**
     * Builds an returns a new invocation factory creating instances of the specified class token.
     * <p/>
     * Note that class tokens of inner and anonymous class can be passed as well. Remember however
     * that Java creates synthetic constructor for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation. In fact, inner classes always have the outer
     * instance as first constructor parameter, and anonymous classes has both the outer instance
     * and all the variables captured in the closure.
     *
     * @param invocationToken the invocation class token.
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
     *                                            parameters was found.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> invocationToken) {

        return factoryOf(invocationToken.getRawClass());
    }

    /**
     * Builds an returns a new invocation factory always returning the specified invocation
     * instance.<br/>
     * Note that, in order to avoid unexpected behaviors, the invocation instance must be stateless.
     *
     * @param invocation the invocation instance.
     * @param <INPUT>    the input data type.
     * @param <OUTPUT>   the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final StatelessInvocation<INPUT, OUTPUT> invocation) {

        return new StatelessInvocationFactory<INPUT, OUTPUT>(invocation);
    }

    /**
     * Builds an returns a new invocation factory creating instances of the specified class.
     * <p/>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructor for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
     * first constructor parameter, and anonymous classes has both the outer instance and all the
     * variables captured in the closure.
     *
     * @param invocationClass the invocation class.
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
     *                                            parameters was found.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass) {

        return new DefaultInvocationFactory<INPUT, OUTPUT>(findConstructor(invocationClass),
                                                           Reflection.NO_ARGS);
    }

    /**
     * Creates and returns a factory builder passing the specified arguments to the invocation
     * constructor.<br/>
     * Note that, in case no constructor taking the specified arguments as parameter is found, an
     * exception will be thrown.
     *
     * @param args the constructor arguments.
     * @return the factory builder.
     */
    @Nonnull
    public static FactoryBuilder withArgs(@Nullable final Object... args) {

        return new FactoryBuilder(args);
    }

    /**
     * Implementation of a builder of invocation factories.
     */
    public static class FactoryBuilder {

        private final Object[] mArgs;

        /**
         * Constructor.
         *
         * @param args the invocation constructor arguments.
         */
        private FactoryBuilder(@Nullable final Object[] args) {

            mArgs = (args == null) ? Reflection.NO_ARGS : args;
        }

        /**
         * Builds an returns a new invocation factory creating instances of the specified class
         * token.
         * <p/>
         * Note that class tokens of inner and anonymous class can be passed as well. Remember
         * however that Java creates synthetic constructor for such classes, so be sure to specify
         * the correct arguments to guarantee proper instantiation. In fact, inner classes always
         * have the outer instance as first constructor parameter, and anonymous classes has both
         * the outer instance and all the variables captured in the closure.
         *
         * @param invocationToken the invocation class token.
         * @param <INPUT>         the input data type.
         * @param <OUTPUT>        the output data type.
         * @return the invocation factory.
         * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects
         *                                            as parameters was found.
         */
        @Nonnull
        public <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf(
                @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> invocationToken) {

            return factoryOf(invocationToken.getRawClass());
        }

        /**
         * Builds an returns a new invocation factory creating instances of the specified class.
         * <p/>
         * Note that inner and anonymous classes can be passed as well. Remember however that Java
         * creates synthetic constructor for such classes, so be sure to specify the correct
         * arguments to guarantee proper instantiation. In fact, inner classes always have the outer
         * instance as first constructor parameter, and anonymous classes has both the outer
         * instance and all the variables captured in the closure.
         *
         * @param invocationClass the invocation class.
         * @param <INPUT>         the input data type.
         * @param <OUTPUT>        the output data type.
         * @return the invocation factory.
         * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects
         *                                            as parameters was found.
         */
        @Nonnull
        public <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf(
                @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass) {

            final Object[] args = mArgs;
            return new DefaultInvocationFactory<INPUT, OUTPUT>(
                    findConstructor(invocationClass, args), args);
        }
    }

    /**
     * Default implementation of an invocation factory.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class DefaultInvocationFactory<INPUT, OUTPUT>
            implements InvocationFactory<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private Constructor<? extends Invocation<INPUT, OUTPUT>> mConstructor;

        /**
         * Constructor.
         *
         * @param constructor the invocation constructor.
         * @param args        the invocation constructor arguments.
         */
        private DefaultInvocationFactory(
                @Nonnull final Constructor<? extends Invocation<INPUT, OUTPUT>> constructor,
                @Nonnull final Object[] args) {

            mConstructor = constructor;
            mArgs = args;
        }

        @Nonnull
        @Override
        public Invocation<INPUT, OUTPUT> createInvocation() {

            try {

                return mConstructor.newInstance(mArgs);

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }

    /**
     * Implementation of a factory of stateless invocations.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class StatelessInvocationFactory<INPUT, OUTPUT>
            implements InvocationFactory<INPUT, OUTPUT> {

        private final StatelessInvocation<INPUT, OUTPUT> mInvocation;

        /**
         * Constructor.
         *
         * @param invocation the invocation instance.
         * @throws java.lang.NullPointerException if the instance is null.
         */
        @SuppressWarnings("ConstantConditions")
        private StatelessInvocationFactory(
                @Nonnull final StatelessInvocation<INPUT, OUTPUT> invocation) {

            if (invocation == null) {

                throw new NullPointerException("the invocation instance must not be null");
            }

            mInvocation = invocation;
        }

        @Nonnull
        @Override
        public Invocation<INPUT, OUTPUT> createInvocation() {

            return mInvocation;
        }
    }
}

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
package com.gh.bmd.jrt.invocation;

import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.util.ClassToken;

import java.lang.reflect.Constructor;
import java.util.List;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.util.Reflection.findConstructor;

/**
 * Utility class for creating invocation factory objects.
 * <p/>
 * Created by davide-maestroni on 2/12/15.
 */
public class Invocations {

    /**
     * Avoid direct instantiation.
     */
    protected Invocations() {

    }

    /**
     * Builds and returns a new invocation factory creating instances of the specified class token.
     * <p/>
     * Note that class tokens of inner and anonymous classes can be passed as well. Remember however
     * that Java creates synthetic constructors for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation. In fact, inner classes always have the outer
     * instance as first constructor parameter, and anonymous classes have both the outer instance
     * and all the variables captured in the closure.
     *
     * @param invocationToken the invocation class token.
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> invocationToken) {

        return factoryOf(invocationToken.getRawClass());
    }

    /**
     * Builds and returns a new invocation factory creating instances of the specified class.
     * <p/>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
     * first constructor parameter, and anonymous classes have both the outer instance and all the
     * variables captured in the closure.
     *
     * @param invocationClass the invocation class.
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass) {

        return new DefaultInvocationFactory<INPUT, OUTPUT>(invocationClass);
    }

    /**
     * Builds and returns a new factory of invocations calling the specified function.<br/>
     * Remember to force the input order type, in case the function parameter position needs to be
     * preserved.
     * <p/>
     * Note that the function object must be stateless in order to avoid concurrency issues.
     *
     * @param function the function instance.
     * @param <OUTPUT> the output data type.
     * @return the builder instance.
     */
    @Nonnull
    public static <OUTPUT> InvocationFactory<Object, OUTPUT> factoryOf(
            @Nonnull final Function<OUTPUT> function) {

        return new FunctionInvocationFactory<OUTPUT>(function);
    }

    /**
     * Interface defining a function taking an undefined number of parameters.
     *
     * @param <OUTPUT> the output data type.
     */
    public interface Function<OUTPUT> {

        /**
         * Calls this function.
         *
         * @param params the array of parameters.
         * @return the result.
         */
        OUTPUT call(@Nonnull Object... params);
    }

    /**
     * Default implementation of an invocation factory.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class DefaultInvocationFactory<INPUT, OUTPUT>
            implements InvocationFactory<INPUT, OUTPUT> {

        private final Class<? extends Invocation<INPUT, OUTPUT>> mInvocationClass;

        private Constructor<? extends Invocation<INPUT, OUTPUT>> mConstructor;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         */
        @SuppressWarnings("ConstantConditions")
        private DefaultInvocationFactory(
                @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass) {

            if (invocationClass == null) {

                throw new NullPointerException("the invocation class must be null");
            }

            mInvocationClass = invocationClass;
        }

        @Nonnull
        public Invocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

            // the arguments should never change, so it is safe to cache the constructor
            final Constructor<? extends Invocation<INPUT, OUTPUT>> constructor;

            if (mConstructor == null) {

                constructor = findConstructor(mInvocationClass, args);
                mConstructor = constructor;

            } else {

                constructor = mConstructor;
            }

            try {

                return constructor.newInstance(args);

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }

    /**
     * Implementation of a factory of invocations calling a specific function.
     *
     * @param <OUTPUT> the output data type.
     */
    private static class FunctionInvocationFactory<OUTPUT>
            implements InvocationFactory<Object, OUTPUT> {

        private final Function<OUTPUT> mFunction;

        /**
         * Constructor.
         *
         * @param function the function instance.
         */
        @SuppressWarnings("ConstantConditions")
        private FunctionInvocationFactory(@Nonnull final Function<OUTPUT> function) {

            if (function == null) {

                throw new NullPointerException("the function must not be null");
            }

            mFunction = function;
        }

        @Nonnull
        public Invocation<Object, OUTPUT> newInvocation(@Nonnull final Object... args) {

            return new FunctionInvocation<Object, OUTPUT>() {

                @Override
                public void onCall(@Nonnull final List<?> objects,
                        @Nonnull final ResultChannel<OUTPUT> result) {

                    result.pass(mFunction.call(objects.toArray()));
                }
            };
        }
    }
}

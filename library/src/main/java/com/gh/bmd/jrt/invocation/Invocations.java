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

import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.RoutineException;

import java.lang.reflect.Constructor;
import java.util.List;

import javax.annotation.Nonnull;

import static com.gh.bmd.jrt.common.Reflection.findConstructor;

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
     * Builds and returns a new invocation factory creating instances of the specified class.
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

        return new DefaultInvocationFactory<INPUT, OUTPUT>(invocationClass);
    }

    /**
     * Builds and returns a new invocation factory creating instances of the specified class token.
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
     * Interface defining a function taking no parameters.
     *
     * @param <OUTPUT> the result data type.
     */
    public interface Function0<OUTPUT> {

        /**
         * Calls this function.
         *
         * @return the result.
         */
        OUTPUT call();
    }

    /**
     * Interface defining a function taking a single parameter.
     *
     * @param <INPUT1> the first parameter type.
     * @param <OUTPUT> the result data type.
     */
    public interface Function1<INPUT1, OUTPUT> {

        /**
         * Calls this function.
         *
         * @param param1 the first parameter.
         * @return the result.
         */
        OUTPUT call(INPUT1 param1);
    }

    /**
     * Interface defining a function taking two parameters.
     *
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <OUTPUT> the result data type.
     */
    public interface Function2<INPUT1, INPUT2, OUTPUT> {

        /**
         * Calls this function.
         *
         * @param param1 the first parameter.
         * @param param2 the second parameter.
         * @return the result.
         */
        OUTPUT call(INPUT1 param1, INPUT2 param2);
    }

    /**
     * Interface defining a function taking three parameters.
     *
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <INPUT3> the third parameter type.
     * @param <OUTPUT> the result data type.
     */
    public interface Function3<INPUT1, INPUT2, INPUT3, OUTPUT> {

        /**
         * Calls this function.
         *
         * @param param1 the first parameter.
         * @param param2 the second parameter.
         * @param param3 the third parameter.
         * @return the result.
         */
        OUTPUT call(INPUT1 param1, INPUT2 param2, INPUT3 param3);
    }

    /**
     * Interface defining a function taking four parameters.
     *
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <INPUT3> the third parameter type.
     * @param <INPUT4> the fourth parameter type.
     * @param <OUTPUT> the result data type.
     */
    public interface Function4<INPUT1, INPUT2, INPUT3, INPUT4, OUTPUT> {

        /**
         * Calls this function.
         *
         * @param param1 the first parameter.
         * @param param2 the second parameter.
         * @param param3 the third parameter.
         * @param param4 the fourth parameter.
         * @return the result.
         */
        OUTPUT call(INPUT1 param1, INPUT2 param2, INPUT3 param3, INPUT4 param4);
    }

    /**
     * Interface defining a function taking an undefined number of parameters.
     *
     * @param <PARAM>  the parameters type.
     * @param <OUTPUT> the result data type.
     */
    public interface FunctionN<PARAM, OUTPUT> {

        /**
         * Calls this function.
         *
         * @param params the list of parameters.
         * @return the result.
         */
        OUTPUT call(@Nonnull final List<? extends PARAM> params);
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
         * @throws java.lang.NullPointerException is the specified class is null.
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
}

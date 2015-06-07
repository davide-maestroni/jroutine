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

import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.util.ClassToken;

import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.util.Reflection.NO_ARGS;
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
     * Builds and returns a new invocation factory creating instances of the specified object class.
     * <p/>
     * Note that inner and anonymous objects can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
     * first constructor parameter, and anonymous classes have both the outer instance and all the
     * variables captured in the closure.
     *
     * @param invocation the invocation instance.
     * @param args       the invocation constructor arguments.
     * @param <INPUT>    the input data type.
     * @param <OUTPUT>   the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final Invocation<INPUT, OUTPUT> invocation, @Nullable final Object... args) {

        return factoryOf((Class<? extends Invocation<INPUT, OUTPUT>>) invocation.getClass(), args);
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
     * @param args            the invocation constructor arguments.
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final ClassToken<? extends Invocation<INPUT, OUTPUT>> invocationToken,
            @Nullable final Object... args) {

        return factoryOf(invocationToken.getRawClass(), args);
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
     * @param args            the invocation constructor arguments.
     * @param <INPUT>         the input data type.
     * @param <OUTPUT>        the output data type.
     * @return the invocation factory.
     */
    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactory<INPUT, OUTPUT> factoryOf(
            @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass,
            @Nullable final Object... args) {

        return new DefaultInvocationFactory<INPUT, OUTPUT>(invocationClass, args);
    }

    /**
     * Default implementation of an invocation factory.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    public static class DefaultInvocationFactory<INPUT, OUTPUT>
            implements InvocationFactory<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final Constructor<? extends Invocation<INPUT, OUTPUT>> mConstructor;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param args            the invocation constructor arguments.
         */
        @SuppressWarnings("ConstantConditions")
        private DefaultInvocationFactory(
                @Nonnull final Class<? extends Invocation<INPUT, OUTPUT>> invocationClass,
                @Nullable final Object[] args) {

            final Object[] invocationArgs = (mArgs = (args != null) ? args : NO_ARGS);
            mConstructor = findConstructor(invocationClass, invocationArgs);
        }

        @Nonnull
        public Invocation<INPUT, OUTPUT> newInvocation() {

            try {

                return mConstructor.newInstance(mArgs);

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }
}

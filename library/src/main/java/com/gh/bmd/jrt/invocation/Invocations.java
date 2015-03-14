/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.invocation;

import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.Reflection;
import com.gh.bmd.jrt.common.RoutineException;

import java.lang.reflect.Constructor;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
     * Interface defining a procedure taking no parameters.
     */
    public interface Procedure0 {

        /**
         * Executes this procedure.
         */
        void execute();
    }

    /**
     * Interface defining a procedure taking a single parameter.
     *
     * @param <INPUT1> the first parameter type.
     */
    public interface Procedure1<INPUT1> {

        /**
         * Executes this procedure.
         *
         * @param param1 the first parameter.
         */
        void execute(INPUT1 param1);
    }

    /**
     * Interface defining a procedure taking two parameters.
     *
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     */
    public interface Procedure2<INPUT1, INPUT2> {

        /**
         * Executes this procedure.
         *
         * @param param1 the first parameter.
         * @param param2 the second parameter.
         */
        void execute(INPUT1 param1, INPUT2 param2);
    }

    /**
     * Interface defining a procedure taking three parameters.
     *
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <INPUT3> the third parameter type.
     */
    public interface Procedure3<INPUT1, INPUT2, INPUT3> {

        /**
         * Executes this procedure.
         *
         * @param param1 the first parameter.
         * @param param2 the second parameter.
         * @param param3 the third parameter.
         */
        void execute(INPUT1 param1, INPUT2 param2, INPUT3 param3);
    }

    /**
     * Interface defining a procedure taking four parameters.
     *
     * @param <INPUT1> the first parameter type.
     * @param <INPUT2> the second parameter type.
     * @param <INPUT3> the third parameter type.
     * @param <INPUT4> the fourth parameter type.
     */
    public interface Procedure4<INPUT1, INPUT2, INPUT3, INPUT4> {

        /**
         * Executes this procedure.
         *
         * @param param1 the first parameter.
         * @param param2 the second parameter.
         * @param param3 the third parameter.
         * @param param4 the fourth parameter.
         */
        void execute(INPUT1 param1, INPUT2 param2, INPUT3 param3, INPUT4 param4);
    }

    /**
     * Interface defining a procedure taking an undefined number of parameters.
     *
     * @param <PARAM> the parameters type.
     */
    public interface ProcedureN<PARAM> {

        /**
         * Executes this procedure.
         *
         * @param params the list of parameters.
         */
        void execute(@Nonnull final List<? extends PARAM> params);
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

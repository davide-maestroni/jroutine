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
package com.gh.bmd.jrt.builder;

import com.gh.bmd.jrt.builder.InvocationConfiguration.Builder;
import com.gh.bmd.jrt.routine.Routine;

import java.lang.reflect.Method;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routines wrapping a class method.
 * <p/>
 * Note that only static methods can be asynchronously invoked through the routines created by this
 * builder.
 * <p/>
 * Created by davide-maestroni on 3/7/15.
 *
 * @see com.gh.bmd.jrt.annotation.Alias
 * @see com.gh.bmd.jrt.annotation.Priority
 * @see com.gh.bmd.jrt.annotation.ShareGroup
 * @see com.gh.bmd.jrt.annotation.Timeout
 * @see com.gh.bmd.jrt.annotation.TimeoutAction
 */
public interface ClassRoutineBuilder extends ConfigurableBuilder<ClassRoutineBuilder>,
        ProxyConfigurableBuilder<ClassRoutineBuilder> {

    /**
     * Returns a routine used to call the method whose identifying name is specified in a
     * {@link com.gh.bmd.jrt.annotation.Alias} annotation.<br/>
     * Optional {@link com.gh.bmd.jrt.annotation.Priority},
     * {@link com.gh.bmd.jrt.annotation.ShareGroup}, {@link com.gh.bmd.jrt.annotation.Timeout} and
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction} method annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * Note that it is up to the caller to ensure that the input data are passed to the routine in
     * the correct order.
     *
     * @param name     the name specified in the annotation.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine.
     * @throws java.lang.IllegalArgumentException if the specified method is not found.
     */
    @Nonnull
    <INPUT, OUTPUT> Routine<INPUT, OUTPUT> aliasMethod(@Nonnull String name);

    /**
     * Note that all the options related to the output and input channels will be ignored.
     *
     * @return the invocation configuration builder.
     */
    @Nonnull
    Builder<? extends ClassRoutineBuilder> invocations();

    /**
     * Returns a routine used to call the specified method.
     * <p/>
     * The method is searched via reflection ignoring a name specified in a
     * {@link com.gh.bmd.jrt.annotation.Alias} annotation. Though, optional
     * {@link com.gh.bmd.jrt.annotation.Priority}, {@link com.gh.bmd.jrt.annotation.ShareGroup},
     * {@link com.gh.bmd.jrt.annotation.Timeout} and {@link com.gh.bmd.jrt.annotation.TimeoutAction}
     * method annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * Note that it is up to the caller to ensure that the input data are passed to the routine in
     * the correct order.
     *
     * @param name           the method name.
     * @param parameterTypes the method parameter types.
     * @return the routine.
     * @throws java.lang.IllegalArgumentException if no matching method is found.
     */
    @Nonnull
    <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull String name,
            @Nonnull Class<?>... parameterTypes);

    /**
     * Returns a routine used to call the specified method.
     * <p/>
     * The method is invoked ignoring a name specified in a
     * {@link com.gh.bmd.jrt.annotation.Alias} annotation. Though, optional
     * {@link com.gh.bmd.jrt.annotation.Priority}, {@link com.gh.bmd.jrt.annotation.ShareGroup},
     * {@link com.gh.bmd.jrt.annotation.Timeout} and {@link com.gh.bmd.jrt.annotation.TimeoutAction}
     * method annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * Note that it is up to the caller to ensure that the input data are passed to the routine in
     * the correct order.
     *
     * @param method   the method instance.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine.
     */
    @Nonnull
    <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull Method method);
}

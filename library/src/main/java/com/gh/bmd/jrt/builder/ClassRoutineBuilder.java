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

import com.gh.bmd.jrt.builder.ProxyConfiguration.Builder;
import com.gh.bmd.jrt.routine.Routine;

import java.lang.reflect.Method;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routines wrapping a class method.
 * <p/>
 * Note that only static methods can be asynchronously invoked through the routines created by this
 * builder.
 * <p/>
 * Created by davide on 3/7/15.
 *
 * @see com.gh.bmd.jrt.annotation.Bind
 * @see com.gh.bmd.jrt.annotation.ShareGroup
 * @see com.gh.bmd.jrt.annotation.Timeout
 * @see com.gh.bmd.jrt.annotation.TimeoutAction
 */
public interface ClassRoutineBuilder {

    /**
     * Returns a routine used to call the method whose identifying name is specified in a
     * {@link com.gh.bmd.jrt.annotation.Bind} annotation.<br/>
     * Optional {@link com.gh.bmd.jrt.annotation.ShareGroup},
     * {@link com.gh.bmd.jrt.annotation.Timeout} and {@link com.gh.bmd.jrt.annotation.TimeoutAction}
     * method annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param name     the name specified in the annotation.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine.
     * @throws java.lang.IllegalArgumentException if the specified method is not found.
     */
    @Nonnull
    <INPUT, OUTPUT> Routine<INPUT, OUTPUT> boundMethod(@Nonnull String name);

    /**
     * TODO
     * <p/>
     * Note that all the options related to the output and input channels will be ignored.
     *
     * @return the configuration builder.
     */
    @Nonnull
    Builder<? extends ClassRoutineBuilder> configure();

    /**
     * Returns a routine used to call the specified method.
     * <p/>
     * The method is invoked ignoring a name specified in a
     * {@link com.gh.bmd.jrt.annotation.Bind} annotation. Though, optional
     * {@link com.gh.bmd.jrt.annotation.ShareGroup}, {@link com.gh.bmd.jrt.annotation.Timeout} and
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction} method annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param method   the method instance.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the routine.
     * @throws java.lang.NullPointerException if the specified method is null.
     */
    @Nonnull
    <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull Method method);

    /**
     * Returns a routine used to call the specified method.
     * <p/>
     * The method is searched via reflection ignoring a name specified in a
     * {@link com.gh.bmd.jrt.annotation.Bind} annotation. Though, optional
     * {@link com.gh.bmd.jrt.annotation.ShareGroup}, {@link com.gh.bmd.jrt.annotation.Timeout} and
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction} method annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param name           the method name.
     * @param parameterTypes the method parameter types.
     * @return the routine.
     * @throws java.lang.IllegalArgumentException if no matching method is found.
     * @throws java.lang.NullPointerException     if one of the parameter is null.
     */
    @Nonnull
    <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull String name,
            @Nonnull Class<?>... parameterTypes);
}

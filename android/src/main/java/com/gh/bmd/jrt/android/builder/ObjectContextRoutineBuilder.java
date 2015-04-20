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
package com.gh.bmd.jrt.android.builder;

import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.ShareConfiguration;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.routine.Routine;

import java.lang.reflect.Method;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class implementing a builder of routine objects based on methods of a concrete object instance.
 * <p/>
 * The single methods can be accessed via reflection or the whole instance can be proxied through
 * an interface.
 * <p/>
 * Created by Davide on 4/6/2015.
 */
public interface ObjectContextRoutineBuilder extends ContextRoutineBuilder, ObjectRoutineBuilder {

    /**
     * Returns a routine used to call the method whose identifying name is specified in a
     * {@link com.gh.bmd.jrt.annotation.Bind} annotation.<br/>
     * Optional {@link com.gh.bmd.jrt.annotation.Timeout},
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction},
     * {@link com.gh.bmd.jrt.annotation.ShareGroup}, as well as
     * {@link com.gh.bmd.jrt.android.annotation.CacheStrategy},
     * {@link com.gh.bmd.jrt.android.annotation.ClashResolution} and
     * {@link com.gh.bmd.jrt.android.annotation.Id} method annotations will be honored.<br/>
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
     * Returns a routine used to call the specified method.
     * <p/>
     * The method is searched via reflection ignoring a name specified in a
     * {@link com.gh.bmd.jrt.annotation.Bind} annotation. Though, optional
     * {@link com.gh.bmd.jrt.annotation.Timeout}, {@link com.gh.bmd.jrt.annotation.TimeoutAction},
     * {@link com.gh.bmd.jrt.annotation.ShareGroup}, as well as
     * {@link com.gh.bmd.jrt.android.annotation.CacheStrategy},
     * {@link com.gh.bmd.jrt.android.annotation.ClashResolution} and
     * {@link com.gh.bmd.jrt.android.annotation.Id} method annotations will be honored.<br/>
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

    /**
     * Returns a routine used to call the specified method.
     * <p/>
     * The method is invoked ignoring a name specified in a
     * {@link com.gh.bmd.jrt.annotation.Bind} annotation. Though, optional
     * {@link com.gh.bmd.jrt.annotation.ShareGroup}, as well as
     * {@link com.gh.bmd.jrt.android.annotation.CacheStrategy},
     * {@link com.gh.bmd.jrt.android.annotation.ClashResolution} and
     * {@link com.gh.bmd.jrt.android.annotation.Id} method annotations will be honored.<br/>
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
     * Returns a proxy object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout},
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction},
     * {@link com.gh.bmd.jrt.annotation.ShareGroup}, as well as
     * {@link com.gh.bmd.jrt.android.annotation.CacheStrategy},
     * {@link com.gh.bmd.jrt.android.annotation.ClashResolution} and
     * {@link com.gh.bmd.jrt.android.annotation.Id} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class token does not represent an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified class token is null.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull Class<TYPE> itf);

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout},
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction},
     * {@link com.gh.bmd.jrt.annotation.ShareGroup}, as well as
     * {@link com.gh.bmd.jrt.android.annotation.CacheStrategy},
     * {@link com.gh.bmd.jrt.android.annotation.ClashResolution} and
     * {@link com.gh.bmd.jrt.android.annotation.Id} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class token does not represent an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified class token is null.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull ClassToken<TYPE> itf);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder withShare(@Nullable ShareConfiguration configuration);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder withShare(@Nonnull ShareConfiguration.Builder builder);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder withConfig(@Nullable RoutineConfiguration configuration);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder withConfig(@Nonnull RoutineConfiguration.Builder builder);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder withInvocations(
            @Nullable ContextInvocationConfiguration configuration);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectContextRoutineBuilder withInvocations(
            @Nonnull ContextInvocationConfiguration.Builder builder);
}

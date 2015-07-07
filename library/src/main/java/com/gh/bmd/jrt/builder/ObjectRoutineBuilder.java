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

import com.gh.bmd.jrt.util.ClassToken;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routines wrapping an object instance.
 * <p/>
 * Note that only instance methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide-maestroni on 3/7/15.
 *
 * @see com.gh.bmd.jrt.annotation.Alias Alias
 * @see com.gh.bmd.jrt.annotation.Input Input
 * @see com.gh.bmd.jrt.annotation.Inputs Inputs
 * @see com.gh.bmd.jrt.annotation.Output Output
 * @see com.gh.bmd.jrt.annotation.Priority Priority
 * @see com.gh.bmd.jrt.annotation.ShareGroup ShareGroup
 * @see com.gh.bmd.jrt.annotation.Timeout Timeout
 * @see com.gh.bmd.jrt.annotation.TimeoutAction TimeoutAction
 */
public interface ObjectRoutineBuilder extends ClassRoutineBuilder {

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Alias Alias},
     * {@link com.gh.bmd.jrt.annotation.Priority Priority},
     * {@link com.gh.bmd.jrt.annotation.ShareGroup ShareGroup},
     * {@link com.gh.bmd.jrt.annotation.Timeout Timeout} and
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction TimeoutAction} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * In case the wrapped object does not implement the specified interface, the alias annotation
     * value will be used to bind the interface method with the instance ones. If no annotation is
     * present, the method name will be used instead.<br/>
     * The interface will be interpreted as a proxy of the target object methods, and the optional
     * {@link com.gh.bmd.jrt.annotation.Input Input},
     * {@link com.gh.bmd.jrt.annotation.Inputs Inputs} and
     * {@link com.gh.bmd.jrt.annotation.Output Output} annotations will be honored.
     *
     * @param itf    the interface implemented by the returned object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull Class<TYPE> itf);

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Alias Alias},
     * {@link com.gh.bmd.jrt.annotation.Priority Priority},
     * {@link com.gh.bmd.jrt.annotation.ShareGroup ShareGroup},
     * {@link com.gh.bmd.jrt.annotation.Timeout Timeout} and
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction TimeoutAction} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * In case the wrapped object does not implement the specified interface, the alias annotation
     * value will be used to bind the interface method with the instance ones. If no annotation is
     * present, the method name will be used instead.<br/>
     * The interface will be interpreted as a proxy of the target object methods, and the optional
     * {@link com.gh.bmd.jrt.annotation.Input Input},
     * {@link com.gh.bmd.jrt.annotation.Inputs Inputs} and
     * {@link com.gh.bmd.jrt.annotation.Output Output} annotations will be honored.
     *
     * @param itf    the token of the interface implemented by the returned object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class token does not represent an
     *                                            interface.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull ClassToken<TYPE> itf);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationConfiguration.Builder<? extends ObjectRoutineBuilder> invocations();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ProxyConfiguration.Builder<? extends ObjectRoutineBuilder> proxies();
}

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
package com.gh.bmd.jrt.routine;

import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.common.ClassToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining a builder of routines wrapping an object instance.
 * <p/>
 * Note that only instance methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide on 3/7/15.
 *
 * @see com.gh.bmd.jrt.annotation.Bind
 * @see com.gh.bmd.jrt.annotation.Pass
 * @see com.gh.bmd.jrt.annotation.Share
 * @see com.gh.bmd.jrt.annotation.Timeout
 */
public interface ObjectRoutineBuilder extends ClassRoutineBuilder {

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind} and {@link com.gh.bmd.jrt.annotation.Timeout}
     * annotations.<br/>
     * In case the wrapped object does not implement the specified interface, the binding annotation
     * value will be used to bind the interface method with the instance ones. If no annotation is
     * present, the method name will be used instead.<br/>
     * The interface will be interpreted as a mirror of the target object methods, and the optional
     * {@link com.gh.bmd.jrt.annotation.Pass} annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param itf    the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified class is null.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull Class<TYPE> itf);

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind} and {@link com.gh.bmd.jrt.annotation.Timeout}
     * annotations.<br/>
     * In case the wrapped object does not implement the specified interface, the binding annotation
     * value will be used to bind the interface method with the instance ones. If no annotation is
     * present, the method name will be used instead.<br/>
     * The interface will be interpreted as a mirror of the target object methods, and the optional
     * {@link com.gh.bmd.jrt.annotation.Pass} annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     * @throws java.lang.NullPointerException     if the specified class is null.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull ClassToken<TYPE> itf);

    /**
     * Note that all the options related to the output and input channels will be ignored.
     *
     * @param configuration the routine configuration.
     * @return this builder.
     */
    @Nonnull
    ObjectRoutineBuilder withConfiguration(@Nullable RoutineConfiguration configuration);

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ObjectRoutineBuilder withShareGroup(@Nullable String group);
}

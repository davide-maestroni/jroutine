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
 * @see com.gh.bmd.jrt.annotation.Wrap
 */
public interface ObjectRoutineBuilder extends ClassRoutineBuilder {

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind} and {@link com.gh.bmd.jrt.annotation.Timeout}
     * annotation.<br/>
     * In case the wrapped object does not implement the specified interface, the binding annotation
     * value will be used to bind the interface method with the instance ones. If no annotation is
     * present, the method name will be used instead to map it.<br/>
     * The interface will be interpreted as a mirror of the target object methods, and the optional
     * {@link com.gh.bmd.jrt.annotation.Pass} annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param itf    the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     * @throws NullPointerException     if the specified class is null.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull Class<TYPE> itf);

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind} and {@link com.gh.bmd.jrt.annotation.Timeout}
     * annotation.<br/>
     * In case the wrapped object does not implement the specified interface, the binding annotation
     * value will be used to bind the interface method with the instance ones. If no annotation is
     * present, the method name will be used instead to map it.<br/>
     * The interface will be interpreted as a mirror of the target object methods, and the optional
     * {@link com.gh.bmd.jrt.annotation.Pass} annotations will be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     * @throws NullPointerException     if the specified class is null.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull ClassToken<TYPE> itf);

    /**
     * Returns a wrapper object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout}
     * and {@link com.gh.bmd.jrt.annotation.Pass} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The wrapping object is created through code generation based on the interfaces annotated
     * with {@link com.gh.bmd.jrt.annotation.Wrap}. The generated class will share the same package
     * of the specified interface and will have a name of the type: JRoutine_&lt;itf_simple_name&gt;
     * <br/>
     * It is actually possible to avoid the use of reflection for the wrapper instantiation by
     * explicitly calling the <code>JRoutine_&lt;itf_simple_name&gt;.on()</code> method. Note,
     * however, that, since the class is generated, a generic IDE may highlight an error even if the
     * compilation is successful.
     * <br/>
     * Note also that you'll need to enable annotation pre-processing by adding the processor
     * package to the specific project dependencies.
     *
     * @param itf    the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the wrapping object.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     * @throws NullPointerException     if the specified class is null.
     */
    @Nonnull
    <TYPE> TYPE buildWrapper(@Nonnull Class<TYPE> itf);

    /**
     * Returns a wrapper object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout}
     * and {@link com.gh.bmd.jrt.annotation.Pass} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The wrapping object is created through code generation based on the interfaces annotated
     * with {@link com.gh.bmd.jrt.annotation.Wrap}. The generated class will share the same package
     * of the specified interface and will have a name of the type: JRoutine_&lt;itf_simple_name&gt;
     * <br/>
     * It is actually possible to avoid the use of reflection for the wrapper instantiation by
     * explicitly calling the <code>JRoutine_&lt;itf_simple_name&gt;.on()</code> method. Note,
     * however, that, since the class is generated, a generic IDE may highlight an error even if the
     * compilation is successful.
     * <br/>
     * Note also that you'll need to enable annotation pre-processing by adding the processor
     * package to the specific project dependencies.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the wrapping object.
     * @throws IllegalArgumentException if the specified class does not represent an interface.
     * @throws NullPointerException     if the specified class is null.
     */
    @Nonnull
    <TYPE> TYPE buildWrapper(@Nonnull ClassToken<TYPE> itf);

    /**
     * Note that all the options related to the output and input channels will be ignored.
     *
     * @param configuration the routine configuration.
     * @return this builder.
     */
    @Nonnull
    @Override
    ObjectRoutineBuilder withConfiguration(@Nullable RoutineConfiguration configuration);

    @Nonnull
    @Override
    ObjectRoutineBuilder withShareGroup(@Nullable String group);
}

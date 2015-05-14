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
package com.gh.bmd.jrt.proxy.builder;

import com.gh.bmd.jrt.builder.ConfigurableBuilder;
import com.gh.bmd.jrt.builder.ProxyConfigurableBuilder;
import com.gh.bmd.jrt.builder.RoutineConfiguration.Builder;
import com.gh.bmd.jrt.common.ClassToken;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routines wrapping an object instance.
 * <p/>
 * Note that only instance methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide on 3/7/15.
 */
public interface ProxyRoutineBuilder extends ConfigurableBuilder<ProxyRoutineBuilder>,
        ProxyConfigurableBuilder<ProxyRoutineBuilder> {

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout},
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction} and {@link com.gh.bmd.jrt.annotation.Param}
     * annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.gh.bmd.jrt.processor.annotation.Proxy}. The generated class will share the same
     * package of the specified interface and will have a name of the type: "&lt;itf_simple_name&gt;
     * {@value com.gh.bmd.jrt.processor.annotation.Proxy#CLASS_NAME_SUFFIX}".<br/>
     * In case the specific interface is not a top level class, the simple name of the outer classes
     * will be prepended to the interface one.<br/>
     * It is actually possible to avoid the use of reflection for the proxy object instantiation by
     * explicitly calling the "&lt;itf_simple_name&gt;
     * {@value com.gh.bmd.jrt.processor.annotation.Proxy#CLASS_NAME_SUFFIX}.on()" method.
     * Note, however, that, since the class is generated, a generic IDE may highlight an error even
     * if the compilation is successful.<br/>
     * Note also that you'll need to enable annotation pre-processing by adding the processor
     * artifact to the specific project dependencies.
     *
     * @param itf    the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull Class<TYPE> itf);

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout},
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction} and {@link com.gh.bmd.jrt.annotation.Param}
     * annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.gh.bmd.jrt.processor.annotation.Proxy}. The generated class will share the same
     * package of the specified interface and will have a name of the type: "&lt;itf_simple_name&gt;
     * {@value com.gh.bmd.jrt.processor.annotation.Proxy#CLASS_NAME_SUFFIX}".<br/>
     * In case the specific interface is not a top level class, the simple name of the outer classes
     * will be prepended to the interface one.<br/>
     * It is actually possible to avoid the use of reflection for the proxy object instantiation by
     * explicitly calling the "&lt;itf_simple_name&gt;
     * {@value com.gh.bmd.jrt.processor.annotation.Proxy#CLASS_NAME_SUFFIX}.on()" method.
     * Note, however, that, since the class is generated, a generic IDE may highlight an error even
     * if the compilation is successful.<br/>
     * Note also that you'll need to enable annotation pre-processing by adding the processor
     * artifact to the specific project dependencies.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull ClassToken<TYPE> itf);

    /**
     * Note that all the options related to the output and input channels will be ignored.
     *
     * @return the routine configuration builder.
     */
    @Nonnull
    Builder<? extends ProxyRoutineBuilder> withRoutine();
}

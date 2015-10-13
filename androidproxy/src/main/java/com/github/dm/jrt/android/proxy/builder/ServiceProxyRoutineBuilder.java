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
package com.github.dm.jrt.android.proxy.builder;

import com.github.dm.jrt.android.builder.ServiceConfigurableBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.proxy.builder.ProxyRoutineBuilder;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of routines wrapping an object, whose methods are executed in a
 * dedicated service.
 * <p/>
 * Created by davide-maestroni on 05/13/2015.
 */
public interface ServiceProxyRoutineBuilder
        extends ProxyRoutineBuilder, ServiceConfigurableBuilder<ServiceProxyRoutineBuilder> {

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.github.dm.jrt.annotation com.github.dm.jrt.annotation.*} annotations.
     * <br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.github.dm.jrt.android.proxy.annotation.ServiceProxy ServiceProxy}. The generated
     * class name and package will be chosen according to the specific annotation attributes.<br/>
     * It is actually possible to avoid the use of reflection for the proxy object instantiation by
     * explicitly calling the <code>&lt;generated_class_name&gt;.with()</code> method.<br/>
     * Note, however, that, since the class is generated, a generic IDE may highlight an error even
     * if the compilation is successful.
     *
     * @param itf    the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     * @see com.github.dm.jrt.annotation.Alias Alias
     * @see com.github.dm.jrt.annotation.CoreInstances CoreInstances
     * @see com.github.dm.jrt.annotation.Input Input
     * @see com.github.dm.jrt.annotation.InputMaxSize InputMaxSize
     * @see com.github.dm.jrt.annotation.InputOrder InputOrder
     * @see com.github.dm.jrt.annotation.Inputs Inputs
     * @see com.github.dm.jrt.annotation.InputTimeout InputTimeout
     * @see com.github.dm.jrt.annotation.MaxInstances MaxInstances
     * @see com.github.dm.jrt.annotation.Invoke Invoke
     * @see com.github.dm.jrt.annotation.Output Output
     * @see com.github.dm.jrt.annotation.OutputMaxSize OutputMaxSize
     * @see com.github.dm.jrt.annotation.OutputOrder OutputOrder
     * @see com.github.dm.jrt.annotation.OutputTimeout OutputTimeout
     * @see com.github.dm.jrt.annotation.Priority Priority
     * @see com.github.dm.jrt.annotation.SharedFields SharedFields
     * @see com.github.dm.jrt.annotation.Timeout Timeout
     * @see com.github.dm.jrt.annotation.TimeoutAction TimeoutAction
     */
    @NotNull
    <TYPE> TYPE buildProxy(@NotNull Class<TYPE> itf);

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.github.dm.jrt.annotation com.github.dm.jrt.annotation.*} annotations.
     * <br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.github.dm.jrt.android.proxy.annotation.ServiceProxy ServiceProxy}. The generated
     * class name and package will be chosen according to the specific annotation attributes.<br/>
     * It is actually possible to avoid the use of reflection for the proxy object instantiation by
     * explicitly calling the <code>&lt;generated_class_name&gt;.with()</code> method.<br/>
     * Note, however, that, since the class is generated, a generic IDE may highlight an error even
     * if the compilation is successful.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     * @see com.github.dm.jrt.annotation.Alias Alias
     * @see com.github.dm.jrt.annotation.CoreInstances CoreInstances
     * @see com.github.dm.jrt.annotation.Input Input
     * @see com.github.dm.jrt.annotation.InputMaxSize InputMaxSize
     * @see com.github.dm.jrt.annotation.InputOrder InputOrder
     * @see com.github.dm.jrt.annotation.Inputs Inputs
     * @see com.github.dm.jrt.annotation.InputTimeout InputTimeout
     * @see com.github.dm.jrt.annotation.MaxInstances MaxInstances
     * @see com.github.dm.jrt.annotation.Invoke Invoke
     * @see com.github.dm.jrt.annotation.Output Output
     * @see com.github.dm.jrt.annotation.OutputMaxSize OutputMaxSize
     * @see com.github.dm.jrt.annotation.OutputOrder OutputOrder
     * @see com.github.dm.jrt.annotation.OutputTimeout OutputTimeout
     * @see com.github.dm.jrt.annotation.Priority Priority
     * @see com.github.dm.jrt.annotation.SharedFields SharedFields
     * @see com.github.dm.jrt.annotation.Timeout Timeout
     * @see com.github.dm.jrt.annotation.TimeoutAction TimeoutAction
     */
    @NotNull
    <TYPE> TYPE buildProxy(@NotNull ClassToken<TYPE> itf);

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends ServiceProxyRoutineBuilder> invocations();

    /**
     * {@inheritDoc}
     */
    @NotNull
    ProxyConfiguration.Builder<? extends ServiceProxyRoutineBuilder> proxies();
}

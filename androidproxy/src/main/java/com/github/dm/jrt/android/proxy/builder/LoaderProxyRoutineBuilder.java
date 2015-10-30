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

import com.github.dm.jrt.android.builder.LoaderConfigurableBuilder;
import com.github.dm.jrt.annotation.ReadTimeout;
import com.github.dm.jrt.annotation.ReadTimeoutAction;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.proxy.builder.ProxyRoutineBuilder;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of routines wrapping an object methods, bound to a context
 * lifecycle.
 * <p/>
 * Created by davide-maestroni on 05/06/2015.
 */
public interface LoaderProxyRoutineBuilder
        extends ProxyRoutineBuilder, LoaderConfigurableBuilder<LoaderProxyRoutineBuilder> {

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any optional
     * <i>{@code com.github.dm.jrt.annotation.*}</i> as well as
     * <i>{@code com.github.dm.jrt.android.annotation.*}</i> annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.github.dm.jrt.android.proxy.annotation.V4Proxy V4Proxy} or
     * {@link com.github.dm.jrt.android.proxy.annotation.V11Proxy V11Proxy}. The generated class
     * name and package will be chosen according to the specific annotation attributes.<br/>
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
     * @see com.github.dm.jrt.android.annotation.CacheStrategy CacheStrategy
     * @see com.github.dm.jrt.android.annotation.ClashResolution ClashResolution
     * @see com.github.dm.jrt.android.annotation.InputClashResolution InputClashResolution
     * @see com.github.dm.jrt.android.annotation.LoaderId LoaderId
     * @see com.github.dm.jrt.android.annotation.ResultStaleTime ResultStaleTime
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
     * @see ReadTimeout ReadTimeout
     * @see ReadTimeoutAction ReadTimeoutAction
     */
    @NotNull
    <TYPE> TYPE buildProxy(@NotNull Class<TYPE> itf);

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any optional
     * <i>{@code com.github.dm.jrt.annotation.*}</i> as well as
     * <i>{@code com.github.dm.jrt.android.annotation.*}</i> annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.github.dm.jrt.android.proxy.annotation.V4Proxy V4Proxy} or
     * {@link com.github.dm.jrt.android.proxy.annotation.V11Proxy V11Proxy}. The generated class
     * name and package will be chosen according to the specific annotation attributes.<br/>
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
     * @see com.github.dm.jrt.android.annotation.CacheStrategy CacheStrategy
     * @see com.github.dm.jrt.android.annotation.ClashResolution ClashResolution
     * @see com.github.dm.jrt.android.annotation.InputClashResolution InputClashResolution
     * @see com.github.dm.jrt.android.annotation.LoaderId LoaderId
     * @see com.github.dm.jrt.android.annotation.ResultStaleTime ResultStaleTime
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
     * @see ReadTimeout ReadTimeout
     * @see ReadTimeoutAction ReadTimeoutAction
     */
    @NotNull
    <TYPE> TYPE buildProxy(@NotNull ClassToken<TYPE> itf);

    /**
     * Note that the configured asynchronous runner will be ignored.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderProxyRoutineBuilder> invocations();

    /**
     * {@inheritDoc}
     */
    @NotNull
    ProxyConfiguration.Builder<? extends LoaderProxyRoutineBuilder> proxies();
}

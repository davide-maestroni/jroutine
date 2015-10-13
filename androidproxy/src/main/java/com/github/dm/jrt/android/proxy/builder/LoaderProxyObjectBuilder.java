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
import com.github.dm.jrt.proxy.builder.ProxyObjectBuilder;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of async proxy objects, bound to a context lifecycle.
 * <p/>
 * Created by davide-maestroni on 05/06/2015.
 *
 * @param <TYPE> the interface type.
 */
public interface LoaderProxyObjectBuilder<TYPE> extends ProxyObjectBuilder<TYPE>,
        LoaderConfigurableBuilder<LoaderProxyObjectBuilder<TYPE>> {

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.github.dm.jrt.annotation com.github.dm.jrt.annotation.*} as well as
     * {@link com.github.dm.jrt.android.annotation com.github.dm.jrt.android.annotation.*}
     * annotations.
     * <br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.github.dm.jrt.android.proxy.annotation.V4Proxy V4Proxy} or
     * {@link com.github.dm.jrt.android.proxy.annotation.V11Proxy V11Proxy}.<br/>
     * Note that, you'll need to enable annotation pre-processing by adding the processor artifact
     * to the specific project dependencies.
     *
     * @return the proxy object.
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
    TYPE buildProxy();

    /**
     * Note that the configured asynchronous runner will be ignored.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderProxyObjectBuilder<TYPE>> invocations();

    /**
     * {@inheritDoc}
     */
    @NotNull
    ProxyConfiguration.Builder<? extends LoaderProxyObjectBuilder<TYPE>> proxies();
}

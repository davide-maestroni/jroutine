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
package com.gh.bmd.jrt.android.proxy.builder;

import com.gh.bmd.jrt.android.builder.ServiceConfigurableBuilder;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.proxy.builder.ProxyBuilder;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of async proxy objects, whose methods are executed in a dedicated
 * service.
 * <p/>
 * Created by davide-maestroni on 13/05/15.
 *
 * @param <TYPE> the interface type.
 */
public interface ServiceProxyBuilder<TYPE>
        extends ProxyBuilder<TYPE>, ServiceConfigurableBuilder<ServiceProxyBuilder<TYPE>> {

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Alias Alias},
     * {@link com.gh.bmd.jrt.annotation.Input Input},
     * {@link com.gh.bmd.jrt.annotation.Inputs Inputs},
     * {@link com.gh.bmd.jrt.annotation.Output Output},
     * {@link com.gh.bmd.jrt.annotation.Priority Priority},
     * {@link com.gh.bmd.jrt.annotation.ShareGroup ShareGroup},
     * {@link com.gh.bmd.jrt.annotation.Timeout Timeout} and
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction TimeoutAction} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.gh.bmd.jrt.android.proxy.annotation.ServiceProxy ServiceProxy}.<br/>
     * Note that, you'll need to enable annotation pre-processing by adding the processor artifact
     * to the specific project dependencies.
     *
     * @return the proxy object.
     */
    @Nonnull
    TYPE buildProxy();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    InvocationConfiguration.Builder<? extends ServiceProxyBuilder<TYPE>> invocations();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ProxyConfiguration.Builder<? extends ServiceProxyBuilder<TYPE>> proxies();
}

/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.proxy.builder;

import com.github.dm.jrt.android.core.builder.ServiceConfigurableBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.object.config.ProxyConfiguration;
import com.github.dm.jrt.proxy.builder.ProxyObjectBuilder;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of async proxy objects, whose methods are executed in a dedicated
 * service.
 * <p>
 * Created by davide-maestroni on 05/13/2015.
 *
 * @param <TYPE> the interface type.
 */
public interface ServiceProxyObjectBuilder<TYPE> extends ProxyObjectBuilder<TYPE>,
        ServiceConfigurableBuilder<ServiceProxyObjectBuilder<TYPE>> {

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p>
     * The routines used for calling the methods will honor the attributes specified in any optional
     * <i>{@code com.github.dm.jrt.object.annotation.*}</i> annotations.
     * <br>
     * Note that such annotations will override any configuration set through the builder.
     * <p>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.github.dm.jrt.android.proxy.annotation.ServiceProxy ServiceProxy}.
     * <br>
     * Note that, you'll need to enable annotation pre-processing by adding the processor artifact
     * to the specific project dependencies.
     *
     * @return the proxy object.
     * @see com.github.dm.jrt.object.annotation Annotations
     */
    @NotNull
    TYPE buildProxy();

    /**
     * {@inheritDoc}
     */
    @NotNull
    InvocationConfiguration.Builder<? extends ServiceProxyObjectBuilder<TYPE>> getInvocationConfiguration();

    /**
     * {@inheritDoc}
     */
    @NotNull
    ProxyConfiguration.Builder<? extends ServiceProxyObjectBuilder<TYPE>> getProxyConfiguration();
}

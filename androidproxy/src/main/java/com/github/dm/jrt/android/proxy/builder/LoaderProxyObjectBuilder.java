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

import com.github.dm.jrt.android.core.builder.LoaderConfigurableBuilder;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.object.config.ObjectConfiguration;
import com.github.dm.jrt.proxy.builder.ProxyObjectBuilder;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a builder of async proxy objects, bound to a context lifecycle.
 * <p>
 * Created by davide-maestroni on 05/06/2015.
 *
 * @param <TYPE> the interface type.
 */
public interface LoaderProxyObjectBuilder<TYPE> extends ProxyObjectBuilder<TYPE>,
        LoaderConfigurableBuilder<LoaderProxyObjectBuilder<TYPE>> {

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p>
     * The routines used for calling the methods will honor the attributes specified in any optional
     * <i>{@code com.github.dm.jrt.object.annotation.*}</i> as well as
     * <i>{@code com.github.dm.jrt.android.object.annotation.*}</i> annotations.
     * <br>
     * Note that such annotations will override any configuration set through the builder.
     * <p>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.github.dm.jrt.android.proxy.annotation.LoaderProxyCompat LoaderProxyCompat} or
     * {@link com.github.dm.jrt.android.proxy.annotation.LoaderProxy LoaderProxy}.
     * <br>
     * Note that, you'll need to enable annotation pre-processing by adding the processor artifact
     * to the specific project dependencies.
     *
     * @return the proxy object.
     * @see com.github.dm.jrt.android.object.annotation Android Annotations
     * @see com.github.dm.jrt.object.annotation Annotations
     */
    @NotNull
    @Override
    TYPE buildProxy();

    /**
     * Note that the configured asynchronous runner will be ignored.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    @Override
    InvocationConfiguration.Builder<? extends LoaderProxyObjectBuilder<TYPE>>
    invocationConfiguration();

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    ObjectConfiguration.Builder<? extends LoaderProxyObjectBuilder<TYPE>> objectConfiguration();
}

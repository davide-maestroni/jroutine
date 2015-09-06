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
package com.github.dm.jrt.proxy.builder;

import com.github.dm.jrt.builder.ConfigurableBuilder;
import com.github.dm.jrt.builder.ProxyConfigurableBuilder;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of async proxy objects.
 * <p/>
 * Created by davide-maestroni on 03/07/2015.
 *
 * @param <TYPE> the interface type.
 */
public interface ProxyBuilder<TYPE> extends ConfigurableBuilder<ProxyBuilder<TYPE>>,
        ProxyConfigurableBuilder<ProxyBuilder<TYPE>> {

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.github.dm.jrt.annotation.Alias Alias},
     * {@link com.github.dm.jrt.annotation.Input Input},
     * {@link com.github.dm.jrt.annotation.Inputs Inputs},
     * {@link com.github.dm.jrt.annotation.Output Output},
     * {@link com.github.dm.jrt.annotation.Priority Priority},
     * {@link com.github.dm.jrt.annotation.ShareGroup ShareGroup},
     * {@link com.github.dm.jrt.annotation.Timeout Timeout} and
     * {@link com.github.dm.jrt.annotation.TimeoutAction TimeoutAction} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.github.dm.jrt.proxy.annotation.Proxy Proxy}.
     *
     * @return the proxy object.
     */
    @Nonnull
    TYPE buildProxy();
}
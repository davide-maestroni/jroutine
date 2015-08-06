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

import com.gh.bmd.jrt.android.builder.LoaderConfigurableBuilder;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.proxy.builder.ProxyRoutineBuilder;
import com.gh.bmd.jrt.util.ClassToken;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routines wrapping an object instance, bound to a context
 * lifecycle.
 * <p/>
 * Note that only instance methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide-maestroni on 06/05/15.
 */
public interface LoaderProxyRoutineBuilder
        extends ProxyRoutineBuilder, LoaderConfigurableBuilder<LoaderProxyRoutineBuilder> {

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
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction TimeoutAction}, as well as
     * {@link com.gh.bmd.jrt.android.annotation.CacheStrategy CacheStrategy},
     * {@link com.gh.bmd.jrt.android.annotation.ClashResolution ClashResolution},
     * {@link com.gh.bmd.jrt.android.annotation.InputClashResolution InputClashResolution},
     * {@link com.gh.bmd.jrt.android.annotation.LoaderId LoaderId} and
     * {@link com.gh.bmd.jrt.android.annotation.StaleTime StaleTime} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.gh.bmd.jrt.android.proxy.annotation.V4Proxy V4Proxy} or
     * {@link com.gh.bmd.jrt.android.proxy.annotation.V11Proxy V11Proxy}. The generated class name
     * and package will be chosen according to the specific annotation attributes.<br/>
     * It is actually possible to avoid the use of reflection for the proxy object instantiation by
     * explicitly calling the <code>&lt;generated_class_name&gt;.on()</code> methods.<br/>
     * Note, however, that, since the class is generated, a generic IDE may highlight an error even
     * if the compilation is successful.
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
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction TimeoutAction}, as well as
     * {@link com.gh.bmd.jrt.android.annotation.ClashResolution ClashResolution},
     * {@link com.gh.bmd.jrt.android.annotation.CacheStrategy CacheStrategy} and
     * {@link com.gh.bmd.jrt.android.annotation.LoaderId LoaderId} and
     * {@link com.gh.bmd.jrt.android.annotation.StaleTime StaleTime} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link com.gh.bmd.jrt.android.proxy.annotation.V4Proxy V4Proxy} or
     * {@link com.gh.bmd.jrt.android.proxy.annotation.V11Proxy V11Proxy}. The generated class name
     * and package will be chosen according to the specific annotation attributes.<br/>
     * It is actually possible to avoid the use of reflection for the proxy object instantiation by
     * explicitly calling the <code>&lt;generated_class_name&gt;.on()</code> methods.<br/>
     * Note, however, that, since the class is generated, a generic IDE may highlight an error even
     * if the compilation is successful.
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
     * Note that the configured asynchronous runner will be ignored.
     *
     * @return the invocation configuration builder.
     */
    @Nonnull
    InvocationConfiguration.Builder<? extends LoaderProxyRoutineBuilder> invocations();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ProxyConfiguration.Builder<? extends LoaderProxyRoutineBuilder> proxies();
}

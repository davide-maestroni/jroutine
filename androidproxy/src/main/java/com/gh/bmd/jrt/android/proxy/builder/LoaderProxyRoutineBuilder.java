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
import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.proxy.annotation.V11Proxy;
import com.gh.bmd.jrt.android.proxy.annotation.V4Proxy;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.proxy.builder.ProxyRoutineBuilder;

import javax.annotation.Nonnull;

/**
 * Interface defining a builder of routines wrapping an object instance, bound to a context
 * lifecycle.
 * <p/>
 * Note that only instance methods can be asynchronously invoked through the routines created by
 * this builder.
 * <p/>
 * Created by davide on 06/05/15.
 */
public interface LoaderProxyRoutineBuilder
        extends ProxyRoutineBuilder, LoaderConfigurableBuilder<LoaderProxyRoutineBuilder> {

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout},
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction} and {@link com.gh.bmd.jrt.annotation.Param},
     * as well as {@link com.gh.bmd.jrt.android.annotation.LoaderId},
     * {@link com.gh.bmd.jrt.android.annotation.ClashResolution} and
     * {@link com.gh.bmd.jrt.android.annotation.CacheStrategy} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link V4Proxy} or
     * {@link V11Proxy}. The generated class will
     * share the same package of the specified interface and will have a name of the type:
     * "&lt;itf_simple_name&gt;
     * {@value V4Proxy#DEFAULT_CLASS_SUFFIX}" or
     * "&lt;itf_simple_name&gt;
     * {@value V11Proxy#DEFAULT_CLASS_SUFFIX}".<br/>
     * In case the specific interface is not a top level class, the simple name of the outer classes
     * will be prepended to the interface one.<br/>
     * It is actually possible to avoid the use of reflection for the proxy object instantiation by
     * explicitly calling the "&lt;itf_simple_name&gt;
     * {@value V4Proxy#DEFAULT_CLASS_SUFFIX}.onXXX()" or
     * "&lt;itf_simple_name&gt;
     * {@value V11Proxy#DEFAULT_CLASS_SUFFIX}.onXXX()"
     * methods. Note, however, that, since the class is generated, a generic IDE may highlight an
     * error even if the compilation is successful.<br/>
     * Note also that you'll need to enable annotation pre-processing by adding the processor
     * artifact to the specific project dependencies.
     *
     * @param itf    the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws IllegalArgumentException if the specified class does not represent an
     *                                  interface.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull Class<TYPE> itf);

    /**
     * Returns a proxy object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout},
     * {@link com.gh.bmd.jrt.annotation.TimeoutAction} and {@link com.gh.bmd.jrt.annotation.Param},
     * as well as {@link com.gh.bmd.jrt.android.annotation.LoaderId},
     * {@link com.gh.bmd.jrt.android.annotation.ClashResolution} and
     * {@link com.gh.bmd.jrt.android.annotation.CacheStrategy} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The proxy object is created through code generation based on the interfaces annotated with
     * {@link V4Proxy} or
     * {@link V11Proxy}. The generated class will
     * share the same package of the specified interface and will have a name of the type:
     * "&lt;itf_simple_name&gt;
     * {@value V4Proxy#DEFAULT_CLASS_SUFFIX}" or
     * "&lt;itf_simple_name&gt;
     * {@value V11Proxy#DEFAULT_CLASS_SUFFIX}".<br/>
     * In case the specific interface is not a top level class, the simple name of the outer classes
     * will be prepended to the interface one.<br/>
     * It is actually possible to avoid the use of reflection for the proxy object instantiation by
     * explicitly calling the "&lt;itf_simple_name&gt;
     * {@value V4Proxy#DEFAULT_CLASS_SUFFIX}.onXXX()" or
     * "&lt;itf_simple_name&gt;
     * {@value V11Proxy#DEFAULT_CLASS_SUFFIX}.onXXX()"
     * methods. Note, however, that, since the class is generated, a generic IDE may highlight an
     * error even if the compilation is successful.<br/>
     * Note also that you'll need to enable annotation pre-processing by adding the processor
     * artifact to the specific project dependencies.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws IllegalArgumentException if the specified class does not represent an
     *                                  interface.
     */
    @Nonnull
    <TYPE> TYPE buildProxy(@Nonnull ClassToken<TYPE> itf);

    /**
     * Note that all the options related to the output and input channels will be ignored.
     *
     * @return the routine configuration builder.
     */
    @Nonnull
    RoutineConfiguration.Builder<? extends LoaderProxyRoutineBuilder> withRoutine();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    LoaderConfiguration.Builder<? extends LoaderProxyRoutineBuilder> withLoader();

    /**
     * {@inheritDoc}
     */
    @Nonnull
    ProxyConfiguration.Builder<? extends LoaderProxyRoutineBuilder> withProxy();
}

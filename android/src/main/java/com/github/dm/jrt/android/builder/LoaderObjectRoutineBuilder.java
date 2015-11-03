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
package com.github.dm.jrt.android.builder;

import com.github.dm.jrt.android.routine.LoaderRoutine;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.ObjectRoutineBuilder;
import com.github.dm.jrt.builder.ProxyConfiguration;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * Interface defining a builder of routines wrapping an object methods.
 * <p/>
 * The single methods can be accessed via reflection or the whole instance can be proxied through
 * an interface.
 * <p/>
 * Created by davide-maestroni on 04/06/2015.
 */
public interface LoaderObjectRoutineBuilder
        extends ObjectRoutineBuilder, LoaderConfigurableBuilder<LoaderObjectRoutineBuilder> {

    /**
     * Returns a routine used to call the method whose identifying name is specified in an
     * {@link com.github.dm.jrt.annotation.Alias Alias} annotation.<br/>
     * Optional <i>{@code com.github.dm.jrt.annotation.*}</i> as well as
     * <i>{@code com.github.dm.jrt.android.annotation.*}</i> method annotations will be honored.
     * <br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param name  the name specified in the annotation.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the routine.
     * @throws java.lang.IllegalArgumentException if the specified method is not found.
     * @see <a href='{@docRoot}/com/github/dm/jrt/android/annotation/package-summary.html'>
     * Android Annotations</a>
     * @see com.github.dm.jrt.annotation Annotations
     */
    @NotNull
    <IN, OUT> LoaderRoutine<IN, OUT> aliasMethod(@NotNull String name);

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any optional
     * <i>{@code com.github.dm.jrt.annotation.*}</i> as well as
     * <i>{@code com.github.dm.jrt.android.annotation.*}</i> annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class token does not represent an
     *                                            interface.
     * @see <a href='{@docRoot}/com/github/dm/jrt/android/annotation/package-summary.html'>
     * Android Annotations</a>
     * @see com.github.dm.jrt.annotation Annotations
     */
    @NotNull
    <TYPE> TYPE buildProxy(@NotNull ClassToken<TYPE> itf);

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any optional
     * <i>{@code com.github.dm.jrt.annotation.*}</i> as well as
     * <i>{@code com.github.dm.jrt.android.annotation.*}</i> annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param itf    the token of the interface implemented by the return object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class does not represent an
     *                                            interface.
     * @see <a href='{@docRoot}/com/github/dm/jrt/android/annotation/package-summary.html'>
     * Android Annotations</a>
     * @see com.github.dm.jrt.annotation Annotations
     */
    @NotNull
    <TYPE> TYPE buildProxy(@NotNull Class<TYPE> itf);

    /**
     * Returns a routine used to call the specified method.
     * <p/>
     * The method is searched via reflection ignoring a name specified in an
     * {@link com.github.dm.jrt.annotation.Alias Alias} annotation. Though, optional
     * <i>{@code com.github.dm.jrt.annotation.*}</i> as well as
     * <i>{@code com.github.dm.jrt.android.annotation.*}</i> method annotations will be honored.
     * <br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param name           the method name.
     * @param parameterTypes the method parameter types.
     * @param <IN>           the input data type.
     * @param <OUT>          the output data type.
     * @return the routine.
     * @throws java.lang.IllegalArgumentException if no matching method is found.
     * @see <a href='{@docRoot}/com/github/dm/jrt/android/annotation/package-summary.html'>
     * Android Annotations</a>
     * @see com.github.dm.jrt.annotation Annotations
     */
    @NotNull
    <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull String name,
            @NotNull Class<?>... parameterTypes);

    /**
     * Returns a routine used to call the specified method.
     * <p/>
     * The method is invoked ignoring a name specified in an
     * {@link com.github.dm.jrt.annotation.Alias Alias} annotation. Though, optional
     * <i>{@code com.github.dm.jrt.annotation.*}</i> as well as
     * <i>{@code com.github.dm.jrt.android.annotation.*}</i> method annotations will be honored.
     * <br/>
     * Note that such annotations will override any configuration set through the builder.
     *
     * @param method the method instance.
     * @param <IN>   the input data type.
     * @param <OUT>  the output data type.
     * @return the routine.
     * @see <a href='{@docRoot}/com/github/dm/jrt/android/annotation/package-summary.html'>
     * Android Annotations</a>
     * @see com.github.dm.jrt.annotation Annotations
     */
    @NotNull
    <IN, OUT> LoaderRoutine<IN, OUT> method(@NotNull Method method);

    /**
     * Note that the configured asynchronous runner will be ignored.
     *
     * @return the invocation configuration builder.
     */
    @NotNull
    InvocationConfiguration.Builder<? extends LoaderObjectRoutineBuilder> invocations();

    /**
     * {@inheritDoc}
     */
    @NotNull
    ProxyConfiguration.Builder<? extends LoaderObjectRoutineBuilder> proxies();
}

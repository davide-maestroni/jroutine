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
package com.github.dm.jrt.builder;

import com.github.dm.jrt.annotation.Timeout;
import com.github.dm.jrt.annotation.TimeoutAction;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.util.ClassToken;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * Interface defining a builder of routines wrapping an object methods.
 * <p/>
 * Created by davide-maestroni on 03/07/2015.
 *
 * @see com.github.dm.jrt.annotation.Alias Alias
 * @see com.github.dm.jrt.annotation.Input Input
 * @see com.github.dm.jrt.annotation.Inputs Inputs
 * @see com.github.dm.jrt.annotation.Invoke Invoke
 * @see com.github.dm.jrt.annotation.Output Output
 * @see com.github.dm.jrt.annotation.Priority Priority
 * @see com.github.dm.jrt.annotation.SharedFields SharedFields
 * @see Timeout Timeout
 * @see TimeoutAction TimeoutAction
 */
public interface ObjectRoutineBuilder extends ConfigurableBuilder<ObjectRoutineBuilder>,
        ProxyConfigurableBuilder<ObjectRoutineBuilder> {

    /**
     * Returns a routine used to call the method whose identifying name is specified in a
     * {@link com.github.dm.jrt.annotation.Alias Alias} annotation.<br/>
     * Optional {@link com.github.dm.jrt.annotation com.github.dm.jrt.annotation.*} method
     * annotations will be honored as well.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * Note that it is up to the caller to ensure that the input data are passed to the routine in
     * the correct order.
     *
     * @param name  the name specified in the annotation.
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     * @return the routine.
     * @throws java.lang.IllegalArgumentException if the specified method is not found.
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
    <IN, OUT> Routine<IN, OUT> aliasMethod(@NotNull String name);

    /**
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.github.dm.jrt.annotation com.github.dm.jrt.annotation.*} annotations.
     * <br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * In case the wrapped object does not implement the specified interface, the alias annotation
     * value will be used to bind the interface method with the instance ones. If no annotation is
     * present, the method name will be used instead.<br/>
     * The interface will be interpreted as a proxy of the target object methods, and the optional
     * {@link com.github.dm.jrt.annotation.Input Input},
     * {@link com.github.dm.jrt.annotation.Inputs Inputs},
     * {@link com.github.dm.jrt.annotation.Invoke Invoke} and
     * {@link com.github.dm.jrt.annotation.Output Output} annotations will be honored.
     *
     * @param itf    the token of the interface implemented by the returned object.
     * @param <TYPE> the interface type.
     * @return the proxy object.
     * @throws java.lang.IllegalArgumentException if the specified class token does not represent an
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
     * Returns a proxy object enabling asynchronous call of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.github.dm.jrt.annotation com.github.dm.jrt.annotation.*} annotations.
     * <br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * In case the wrapped object does not implement the specified interface, the alias annotation
     * value will be used to bind the interface method with the instance ones. If no annotation is
     * present, the method name will be used instead.<br/>
     * The interface will be interpreted as a proxy of the target object methods, and the optional
     * {@link com.github.dm.jrt.annotation.Input Input},
     * {@link com.github.dm.jrt.annotation.Inputs Inputs},
     * {@link com.github.dm.jrt.annotation.Invoke Invoke} and
     * {@link com.github.dm.jrt.annotation.Output Output} annotations will be honored.
     *
     * @param itf    the interface implemented by the returned object.
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
     * Returns a routine used to call the specified method.
     * <p/>
     * The method is searched via reflection ignoring a name specified in a
     * {@link com.github.dm.jrt.annotation.Alias Alias} annotation. Though, optional
     * {@link com.github.dm.jrt.annotation com.github.dm.jrt.annotation.*} method annotations will
     * be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * Note that it is up to the caller to ensure that the input data are passed to the routine in
     * the correct order.
     *
     * @param name           the method name.
     * @param parameterTypes the method parameter types.
     * @return the routine.
     * @throws java.lang.IllegalArgumentException if no matching method is found.
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
    <IN, OUT> Routine<IN, OUT> method(@NotNull String name, @NotNull Class<?>... parameterTypes);

    /**
     * Returns a routine used to call the specified method.
     * <p/>
     * The method is invoked ignoring a name specified in a
     * {@link com.github.dm.jrt.annotation.Alias Alias} annotation. Though, optional
     * {@link com.github.dm.jrt.annotation com.github.dm.jrt.annotation.*} method annotations will
     * be honored.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * Note that it is up to the caller to ensure that the input data are passed to the routine in
     * the correct order.
     *
     * @param method the method instance.
     * @param <IN>   the input data type.
     * @param <OUT>  the output data type.
     * @return the routine.
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
    <IN, OUT> Routine<IN, OUT> method(@NotNull Method method);
}

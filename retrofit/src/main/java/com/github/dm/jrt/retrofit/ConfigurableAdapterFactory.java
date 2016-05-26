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

package com.github.dm.jrt.retrofit;

import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.InvocationMode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Interface defining a configurable adapter factory.
 * <p>
 * Created by davide-maestroni on 05/26/2016.
 */
public interface ConfigurableAdapterFactory {

    /**
     * Gets the adapter used to convert a Retrofit call into the method return type.
     *
     * @param configuration  the invocation configuration.
     * @param invocationMode the invocation mode.
     * @param returnRawType  the return raw type to be adapted.
     * @param responseType   the type of the call response.
     * @param annotations    the method annotations.
     * @param retrofit       the Retrofit instance.
     * @return the call adapter or null.
     */
    @Nullable
    CallAdapter<?> get(@NotNull InvocationConfiguration configuration,
            @NotNull InvocationMode invocationMode, @NotNull Type returnRawType,
            @NotNull Type responseType, @NotNull Annotation[] annotations,
            @NotNull Retrofit retrofit);

}

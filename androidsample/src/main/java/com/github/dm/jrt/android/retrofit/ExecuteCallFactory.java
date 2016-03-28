/*
 * Copyright (c) 2016. Davide Maestroni
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

package com.github.dm.jrt.android.retrofit;

import com.github.dm.jrt.core.invocation.ComparableInvocationFactory;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;

/**
 * Implementation of a factory of invocations handling call instances.
 *
 * @param <T> the response type.
 */
public class ExecuteCallFactory<T> extends ComparableInvocationFactory<Call<T>, T> {

    private static final ExecuteCallFactory<Object> sFactory = new ExecuteCallFactory<>();

    /**
     * Constructor.
     */
    public ExecuteCallFactory() {

        super(null);
    }

    /**
     * Returns the default invocation factory used to handle calls.
     *
     * @param <T> the response type.
     * @return the factory instance.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> InvocationFactory<? extends Call<T>, T> getInstance() {

        return (InvocationFactory<? extends Call<T>, T>) sFactory;
    }

    @NotNull
    @Override
    public Invocation<Call<T>, T> newInvocation() {

        return new ExecuteCall<>();
    }
}

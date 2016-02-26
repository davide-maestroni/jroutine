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

package com.github.dm.jrt.android.invocation;

import android.content.Context;

import com.github.dm.jrt.invocation.Invocation;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining an invocation aware of the specific Android context.
 * <p/>
 * Created by davide-maestroni on 01/08/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public interface ContextInvocation<IN, OUT> extends Invocation<IN, OUT> {

    /**
     * Called right after the instantiation to specify the invocation context.
     *
     * @param context the context of the invocation.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    void onContext(@NotNull Context context) throws Exception;
}

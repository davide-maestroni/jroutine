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
import com.github.dm.jrt.invocation.InvocationDecorator;

import org.jetbrains.annotations.NotNull;

/**
 * Implementation of a platform specific Android invocation wrapping a base invocation instance.
 * <p/>
 * Created by davide-maestroni on 03/21/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public class ContextInvocationWrapper<IN, OUT> extends InvocationDecorator<IN, OUT>
        implements ContextInvocation<IN, OUT> {

    /**
     * Constructor.
     *
     * @param invocation the wrapped invocation.
     */
    public ContextInvocationWrapper(@NotNull final Invocation<IN, OUT> invocation) {

        super(invocation);
    }

    public void onContext(@NotNull final Context context) {

    }
}

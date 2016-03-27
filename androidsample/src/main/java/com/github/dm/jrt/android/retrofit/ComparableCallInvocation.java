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

package com.github.dm.jrt.android.retrofit;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.FilterInvocation;

import org.jetbrains.annotations.NotNull;

/**
 * Implementation of an invocation handling the execution of call instances.
 *
 * @param <T> the response type.
 */
public class ComparableCallInvocation<T> extends FilterInvocation<ComparableCall<T>, T> {

    @Override
    public void onInput(final ComparableCall<T> call, @NotNull final ResultChannel<T> result) throws
            Exception {

        result.pass(call.execute().body());
    }
}

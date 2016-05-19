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

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.MappingInvocation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import retrofit2.Call;

/**
 * Implementation of an invocation handling the execution of {@link Call}s.
 * <p>
 * Created by davide-maestroni on 03/26/2016.
 *
 * @param <T> the response type.
 */
public class RetrofitCallInvocation<T> extends MappingInvocation<Call<T>, T> {

    /**
     * Constructor.
     */
    public RetrofitCallInvocation() {

        super(null);
    }

    public void onInput(final Call<T> call, @NotNull final ResultChannel<T> result) throws
            IOException {

        result.pass(call.execute().body());
    }
}

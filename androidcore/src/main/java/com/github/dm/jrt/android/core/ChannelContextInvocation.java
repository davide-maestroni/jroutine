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

package com.github.dm.jrt.android.core;

import android.content.Context;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.core.ChannelInvocation;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract base invocation transforming data coming from an input channel into an output one.
 * <p>
 * Created by davide-maestroni on 06/11/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class ChannelContextInvocation<IN, OUT> extends ChannelInvocation<IN, OUT>
        implements ContextInvocation<IN, OUT> {

    private Context mContext;

    @Override
    public void onContext(@NotNull final Context context) throws Exception {
        mContext = context;
    }

    /**
     * Returns this invocation context.
     *
     * @return the context of this invocation.
     */
    protected Context getContext() {
        return mContext;
    }
}

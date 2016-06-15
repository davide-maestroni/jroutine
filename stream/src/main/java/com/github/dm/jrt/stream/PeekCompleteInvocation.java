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

package com.github.dm.jrt.stream;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Generate invocation peeking output data completion.
 * <p>
 * Created by davide-maestroni on 06/15/2016.
 *
 * @param <DATA> the data type.
 */
class PeekCompleteInvocation<DATA> extends GenerateInvocation<DATA, DATA> {

    private final Runnable mPeekAction;

    /**
     * Constructor.
     *
     * @param peekAction the runnable instance.
     */
    PeekCompleteInvocation(final Runnable peekAction) {
        super(asArgs(ConstantConditions.notNull("runnable instance", peekAction)));
        mPeekAction = peekAction;
    }

    public void onInput(final DATA input, @NotNull final ResultChannel<DATA> result) throws
            Exception {
        result.pass(input);
    }

    public void onResult(@NotNull final ResultChannel<DATA> result) {
        mPeekAction.run();
    }
}

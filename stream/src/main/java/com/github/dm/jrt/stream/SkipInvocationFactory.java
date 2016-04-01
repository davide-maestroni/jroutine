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
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of skip invocations.
 * <p>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <DATA> the data type.
 */
class SkipInvocationFactory<DATA> extends InvocationFactory<DATA, DATA> {

    private final int mCount;

    /**
     * Constructor.
     *
     * @param count the number of data to skip.
     * @throws java.lang.IllegalArgumentException if the count is negative.
     */
    SkipInvocationFactory(final int count) {

        super(asArgs(count));
        if (count < 0) {
            throw new IllegalArgumentException("the count must not be negative: " + count);
        }

        mCount = count;
    }

    @NotNull
    @Override
    public Invocation<DATA, DATA> newInvocation() {

        return new SkipInvocation<DATA>(mCount);
    }

    /**
     * Routine invocation skipping input data.
     *
     * @param <DATA> the data type.
     */
    private static class SkipInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

        private final int mCount;

        private int mCurrent;

        /**
         * Constructor.
         *
         * @param count the number of data to skip.
         */
        private SkipInvocation(final int count) {

            mCount = count;
        }

        @Override
        public void onInitialize() {

            mCurrent = 0;
        }

        @Override
        public void onInput(final DATA input, @NotNull final ResultChannel<DATA> result) {

            if (mCurrent < mCount) {
                ++mCurrent;

            } else {
                result.pass(input);
            }
        }
    }
}

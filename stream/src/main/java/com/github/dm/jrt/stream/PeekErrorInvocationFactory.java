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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.ConsumerWrapper;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of invocations peeking errors as they are passed along.
 * <p>
 * Created by davide-maestroni on 07/01/2016.
 *
 * @param <DATA> the data type.
 */
class PeekErrorInvocationFactory<DATA> extends InvocationFactory<DATA, DATA> {

    private final ConsumerWrapper<? super RoutineException> mErrorConsumer;

    /**
     * Constructor.
     *
     * @param errorConsumer the consumer instance.
     */
    PeekErrorInvocationFactory(
            @NotNull final ConsumerWrapper<? super RoutineException> errorConsumer) {
        super(asArgs(ConstantConditions.notNull("consumer instance", errorConsumer)));
        mErrorConsumer = errorConsumer;
    }

    @NotNull
    @Override
    public Invocation<DATA, DATA> newInvocation() throws Exception {
        return new PeekErrorInvocation<DATA>(mErrorConsumer);
    }

    // TODO: 01/07/16 javadoc
    private static class PeekErrorInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

        private final ConsumerWrapper<? super RoutineException> mErrorConsumer;

        private PeekErrorInvocation(
                @NotNull final ConsumerWrapper<? super RoutineException> errorConsumer) {
            mErrorConsumer = errorConsumer;
        }

        @Override
        public void onAbort(@NotNull final RoutineException reason) throws Exception {
            mErrorConsumer.accept(reason);
        }

        @Override
        public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) {
            result.pass(input);
        }
    }
}

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

package com.github.dm.jrt.android.v4.core;

import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.CallContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.MissingLoaderException;
import com.github.dm.jrt.core.channel.ResultChannel;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Invocation factory used to know whether a loader with a specific ID is present or not.
 * <p/>
 * Created by davide-maestroni on 01/14/2015.
 *
 * @param <OUT> the output data type.
 */
final class MissingLoaderInvocationFactory<OUT> extends CallContextInvocationFactory<Void, OUT> {

    private final int mId;

    /**
     * Constructor.
     *
     * @param id the loader ID.
     */
    MissingLoaderInvocationFactory(final int id) {

        super(asArgs(id));
        mId = id;
    }

    @NotNull
    @Override
    public CallContextInvocation<Void, OUT> newInvocation() {

        return new MissingLoaderInvocation<OUT>(mId);
    }

    /**
     * Function context invocation implementation.
     *
     * @param <OUT> the output data type.
     */
    private static class MissingLoaderInvocation<OUT> extends CallContextInvocation<Void, OUT> {

        private final int mId;

        /**
         * Constructor.
         *
         * @param id the loader ID.
         */
        private MissingLoaderInvocation(final int id) {

            mId = id;
        }

        @Override
        protected void onCall(@NotNull final List<? extends Void> inputs,
                @NotNull final ResultChannel<OUT> result) {

            result.abort(new MissingLoaderException(mId));
        }
    }
}

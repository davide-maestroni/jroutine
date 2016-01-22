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

import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Abstract class defining a factory of context invocations.
 * <p/>
 * Created by davide-maestroni on 05/01/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class ContextInvocationFactory<IN, OUT> {

    private final Object[] mArgs;

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    protected ContextInvocationFactory(@Nullable final Object[] args) {

        mArgs = (args != null) ? args.clone() : Reflection.NO_ARGS;
    }

    /**
     * Constructor.
     * <p/>
     * Forces the inheriting classes to explicitly pass the arguments.
     */
    @SuppressWarnings("unused")
    private ContextInvocationFactory() {

        mArgs = Reflection.NO_ARGS;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }

        if (!getClass().isInstance(o)) {
            return false;
        }

        final ContextInvocationFactory<?, ?> that = (ContextInvocationFactory<?, ?>) o;
        return Arrays.deepEquals(mArgs, that.mArgs);
    }

    @Override
    public int hashCode() {

        return 31 * getClass().hashCode() + Arrays.deepHashCode(mArgs);
    }

    /**
     * Creates and return a new context invocation instance.<br/>
     * A proper implementation will return a new invocation instance each time it is called, unless
     * the returned object is immutable and does not cause any side effect.<br/>
     * Any behavior other than that may lead to unexpected results.
     *
     * @return the context invocation instance.
     */
    @NotNull
    public abstract ContextInvocation<IN, OUT> newInvocation();
}

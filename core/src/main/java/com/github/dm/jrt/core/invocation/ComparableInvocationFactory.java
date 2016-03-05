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

package com.github.dm.jrt.core.invocation;

import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Invocation factory implementing {@code equals()} and {@code hashCode()}.
 * <p/>
 * Created by davide-maestroni on 02/10/2016.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class ComparableInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

    private final Object[] mArgs;

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    protected ComparableInvocationFactory(@Nullable final Object[] args) {

        mArgs = (args != null) ? args.clone() : Reflection.NO_ARGS;
    }

    @Override
    public int hashCode() {

        return 31 * getClass().hashCode() + Arrays.deepHashCode(mArgs);
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }

        if (!getClass().isInstance(o)) {
            return false;
        }

        final ComparableInvocationFactory<?, ?> that = (ComparableInvocationFactory<?, ?>) o;
        return Arrays.deepEquals(mArgs, that.mArgs);
    }
}

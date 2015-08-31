/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dm.jrt.android.invocation;

import com.github.dm.jrt.util.Reflection;

import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Abstract implementation of a context invocation factory.
 * <p/>
 * This class implements {@code equals()} and {@code hashCode()} methods employing the arguments
 * specified in the constructor. Note, however, that based on the specific object implementation,
 * the
 * equality check may unexpectedly fail. For example, in case of a collection of arrays, the object
 * will not be equal to another collection containing different arrays storing the same data. In
 * fact an array {@code equals()} implementation does not override the {@code Object} one.
 * <p/>
 * Created by davide-maestroni on 28/05/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class AbstractContextInvocationFactory<INPUT, OUTPUT>
        extends ContextInvocationFactory<INPUT, OUTPUT> {

    private final Object[] mArgs;

    /**
     * Forces the inheriting classes to explicitly pass the arguments.
     *
     * @throws java.lang.IllegalArgumentException always.
     */
    @SuppressWarnings("unused")
    protected AbstractContextInvocationFactory() {

        throw new IllegalArgumentException("no argument objects passed");
    }

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    protected AbstractContextInvocationFactory(@Nullable final Object... args) {

        mArgs = (args != null) ? args.clone() : Reflection.NO_ARGS;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!getClass().isInstance(o)) {

            return false;
        }

        final AbstractContextInvocationFactory<?, ?> that =
                (AbstractContextInvocationFactory<?, ?>) o;
        return Arrays.deepEquals(mArgs, that.mArgs);
    }

    @Override
    public int hashCode() {

        return 31 * getClass().hashCode() + Arrays.deepHashCode(mArgs);
    }
}

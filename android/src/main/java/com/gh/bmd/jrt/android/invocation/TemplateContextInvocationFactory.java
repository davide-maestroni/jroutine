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
package com.gh.bmd.jrt.android.invocation;

import com.gh.bmd.jrt.util.Reflection;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Empty abstract implementation of a context invocation factory.
 * <p/>
 * This class is useful to avoid the need of implementing all the methods defined in the interface.
 * <p/>
 * Created by davide on 28/05/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
public abstract class TemplateContextInvocationFactory<INPUT, OUTPUT>
        implements ContextInvocationFactory<INPUT, OUTPUT> {

    private final Object[] mArgs;

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    public TemplateContextInvocationFactory(@Nullable final Object... args) {

        mArgs = (args != null) ? args : Reflection.NO_ARGS;
    }

    private static boolean recursiveEquals(@Nullable final Object object1,
            @Nullable final Object object2) {

        if (object1 == object2) {

            return true;
        }

        if ((object1 == null) || (object2 == null)) {

            return false;
        }

        if (object1.getClass().isArray()) {

            if (!object2.getClass().isArray()) {

                return false;
            }

            final int length1 = Array.getLength(object1);
            final int length2 = Array.getLength(object2);

            if (length1 != length2) {

                return false;
            }

            for (int i = 0; i < length1; i++) {

                if (!recursiveEquals(Array.get(object1, i), Array.get(object2, i))) {

                    return false;
                }
            }

            return true;

        } else if (object1 instanceof Iterable) {

            if (!(object2 instanceof Iterable)) {

                return false;
            }

            final Iterator<?> iterator1 = ((Iterable<?>) object1).iterator();
            final Iterator<?> iterator2 = ((Iterable<?>) object2).iterator();

            while (iterator1.hasNext()) {

                if (!iterator2.hasNext()) {

                    return false;
                }

                if (!recursiveEquals(iterator1.next(), iterator2.next())) {

                    return false;
                }
            }

            return !iterator2.hasNext();

        } else if (object1 instanceof Map) {

            return (object2 instanceof Map) && recursiveEquals(((Map<?, ?>) object1).entrySet(),
                                                               ((Map<?, ?>) object2).entrySet());
        }

        return object1.equals(object2);
    }

    private static int recursiveHashCode(@Nullable final Object object) {

        if (object == null) {

            return 0;
        }

        if (object.getClass().isArray()) {

            int hashCode = 0;
            final int length = Array.getLength(object);

            for (int i = 0; i < length; i++) {

                hashCode = 31 * hashCode + recursiveHashCode(Array.get(object, i));
            }

            return hashCode;

        } else if (object instanceof Iterable) {

            int hashCode = 0;

            for (final Object o : ((Iterable<?>) object)) {

                hashCode = 31 * hashCode + recursiveHashCode(o);
            }

            return hashCode;

        } else if (object instanceof Map) {

            return recursiveHashCode(((Map<?, ?>) object).entrySet());
        }

        return object.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!getClass().isInstance(o)) {

            return false;
        }

        final TemplateContextInvocationFactory<?, ?> that =
                (TemplateContextInvocationFactory<?, ?>) o;
        final Object[] thisArgs = mArgs;
        final Object[] thatArgs = that.mArgs;
        final int length = thisArgs.length;

        if (length != thatArgs.length) {

            return false;
        }

        for (int i = 0; i < length; i++) {

            if (!recursiveEquals(thisArgs[i], thatArgs[i])) {

                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {

        return 31 * getClass().hashCode() + recursiveHashCode(mArgs);
    }
}

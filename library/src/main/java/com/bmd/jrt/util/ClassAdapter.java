/**
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
package com.bmd.jrt.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utility abstract class used to work around Java type erasure.
 * <p/>
 * By using class objects it is impossible to distinguish between two different generic classes.
 * For example there is no way to declare a <code>Class&lt;List&lt;String&gt;&gt;</code> as
 * opposed to <code>Class&lt;List&lt;Integer&gt;&gt;</code>.<br/>
 * The workaround consists in forcing the inheritance from a special generic class, then inspected
 * via reflection, to obtain the generic type rather than the class object.
 * <p/>
 * Created by davide on 6/14/14.
 *
 * @param <CLASS> the class type.
 */
public abstract class ClassAdapter<CLASS> {

    private Type mGenericType;

    private Class<CLASS> mRawClass;

    /**
     * Creates a new adapter from the specified raw class.
     *
     * @param rawClass the raw class.
     * @param <CLASS>  the class type.
     * @return the newly created adapter.
     * @throws IllegalArgumentException if the raw class is null.
     */
    public static <CLASS> ClassAdapter<CLASS> adapt(final Class<CLASS> rawClass) {

        if (rawClass == null) {

            throw new IllegalArgumentException("the classification raw type cannot be null");
        }

        final ClassAdapter<CLASS> classAdapter = new ClassAdapter<CLASS>() {};
        classAdapter.mGenericType = rawClass;
        classAdapter.mRawClass = rawClass;

        return classAdapter;
    }

    /**
     * TODO
     *
     * @param object
     * @param <CLASS>
     * @return
     */
    public static <CLASS> ClassAdapter<CLASS> classOf(final CLASS object) {

        if (object == null) {

            throw new IllegalArgumentException("the classification object cannot be null");
        }

        //noinspection unchecked
        return adapt((Class<CLASS>) object.getClass());
    }

    /**
     * Casts the specified object to this adapter raw class.
     * <p/>
     * Note that the cast is unsafe and may raise an exception.
     *
     * @param object the object to cast.
     * @return the casted object.
     */
    public final CLASS cast(final Object object) {

        //noinspection unchecked
        return (CLASS) object;
    }

    /**
     * Gets the generic type of this adapter.
     *
     * @return the generic type.
     */
    public final Type getGenericType() {

        if (mGenericType == null) {

            Class<?> subClass = getClass();
            Class<?> superClass = subClass.getSuperclass();

            while (!ClassAdapter.class.equals(superClass)) {

                subClass = superClass;
                superClass = subClass.getSuperclass();
            }

            final Type type = subClass.getGenericSuperclass();

            if (type instanceof ParameterizedType) {

                final ParameterizedType paramType = (ParameterizedType) type;

                mGenericType = paramType.getActualTypeArguments()[0];

            } else {

                throw new IllegalStateException(
                        "the class does not correctly extends classification: " + getClass());
            }
        }

        return mGenericType;
    }

    /**
     * Gets the raw class of this adapter.
     *
     * @return the raw class.
     */
    public final Class<CLASS> getRawClass() {

        if (mRawClass == null) {

            final Type type = getGenericType();

            if (type instanceof Class) {

                //noinspection unchecked
                mRawClass = ((Class<CLASS>) type);

            } else if (type instanceof ParameterizedType) {

                //noinspection unchecked
                mRawClass = ((Class<CLASS>) ((ParameterizedType) type).getRawType());

            } else {

                throw new IllegalStateException(
                        "the class does not correctly extends an adapter: " + getClass());
            }
        }

        return mRawClass;
    }

    @Override
    public int hashCode() {

        return getRawClass().hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof ClassAdapter)) {

            return false;
        }

        final ClassAdapter adapter = (ClassAdapter) o;

        return getRawClass().equals(adapter.getRawClass());
    }

    /**
     * Checks if this adapter raw class is equal to or is a super class of the specified one.
     *
     * @param other the adapter to compare.
     * @return whether this adapter raw class is equal to or is a super class.
     */
    public final boolean isAssignableFrom(final ClassAdapter<?> other) {

        return getRawClass().isAssignableFrom(other.getRawClass());
    }

    /**
     * Checks if this adapter raw class represent an interface.
     *
     * @return whether this adapter raw class is an interface.
     */
    public final boolean isInterface() {

        return getRawClass().isInterface();
    }
}
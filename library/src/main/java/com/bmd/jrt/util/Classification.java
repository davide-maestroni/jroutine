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
 * The workaround here is to force the inheritance from a special generic class, then inspected via
 * reflection in order to obtain the generic type rather than the class object.
 * <p/>
 * Created by davide on 6/14/14.
 *
 * @param <TYPE> the generic type.
 */
public abstract class Classification<TYPE> {

    private Class<TYPE> mRawType;

    private Type mType;

    /**
     * TODO
     *
     * @param object
     * @param <TYPE>
     * @return
     */
    public static <TYPE> Classification<TYPE> of(final TYPE object) {

        if (object == null) {

            throw new IllegalArgumentException("the classification object cannot be null");
        }

        //noinspection unchecked
        return ofType((Class<TYPE>) object.getClass());
    }

    /**
     * Creates a new classification from the specified raw type.
     *
     * @param rawType the raw type.
     * @param <TYPE>  the type.
     * @return the newly created classification.
     * @throws IllegalArgumentException if the raw type is null.
     */
    public static <TYPE> Classification<TYPE> ofType(final Class<TYPE> rawType) {

        if (rawType == null) {

            throw new IllegalArgumentException("the classification raw type cannot be null");
        }

        final Classification<TYPE> classification = new Classification<TYPE>() {};
        classification.mType = rawType;
        classification.mRawType = rawType;

        return classification;
    }

    /**
     * Casts the specified object to this classification type.
     * <p/>
     * Note that the cast is unsafe and may raise an exception.
     *
     * @param object the object to cast.
     * @return the casted object.
     */
    public final TYPE cast(final Object object) {

        //noinspection unchecked
        return (TYPE) object;
    }

    /**
     * Gets the classification raw type.
     *
     * @return the raw type.
     */
    public final Class<TYPE> getRawType() {

        if (mRawType == null) {

            final Type type = getType();

            if (type instanceof Class) {

                //noinspection unchecked
                mRawType = ((Class<TYPE>) type);

            } else if (type instanceof ParameterizedType) {

                //noinspection unchecked
                mRawType = ((Class<TYPE>) ((ParameterizedType) type).getRawType());

            } else {

                throw new IllegalStateException(
                        "the class does not correctly extends classification: " + getClass());
            }
        }

        return mRawType;
    }

    /**
     * Gets the classification type.
     *
     * @return the generic type.
     */
    public final Type getType() {

        if (mType == null) {

            Class<?> subClass = getClass();
            Class<?> superClass = subClass.getSuperclass();

            while (!Classification.class.equals(superClass)) {

                subClass = superClass;
                superClass = subClass.getSuperclass();
            }

            final Type type = subClass.getGenericSuperclass();

            if (type instanceof ParameterizedType) {

                final ParameterizedType paramType = (ParameterizedType) type;

                mType = paramType.getActualTypeArguments()[0];

            } else {

                throw new IllegalStateException(
                        "the class does not correctly extends classification: " + getClass());
            }
        }

        return mType;
    }

    @Override
    public final int hashCode() {

        int result = getType().hashCode();

        return 31 * result + getRawType().hashCode();
    }

    @Override
    public final boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof Classification)) {

            return false;
        }

        final Classification classification = (Classification) o;

        //noinspection SimplifiableIfStatement
        if (!getType().equals(classification.getType())) {

            return false;
        }

        return getRawType().equals(classification.getRawType());
    }

    /**
     * Checks if this classification is equal to or is a super class of the specified one.
     *
     * @param other the classification to compare.
     * @return whether this classification is equal to or is a super class.
     */
    public final boolean isAssignableFrom(final Classification<?> other) {

        return getRawType().isAssignableFrom(other.getRawType());
    }

    /**
     * Checks if this classification represent an interface.
     *
     * @return whether this classification is an interface.
     */
    public final boolean isInterface() {

        return getRawType().isInterface();
    }
}
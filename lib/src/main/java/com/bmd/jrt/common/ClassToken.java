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
package com.bmd.jrt.common;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.annotation.Nonnull;

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
public abstract class ClassToken<CLASS> {

    private Type mGenericType;

    private Class<CLASS> mRawClass;

    /**
     * Creates a new token from the class of the specified object.
     *
     * @param object  the object.
     * @param <CLASS> the class type.
     * @return the newly created token.
     * @throws NullPointerException if the object is null.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <CLASS> ClassToken<CLASS> tokenOf(@Nonnull final CLASS object) {

        return tokenOf((Class<CLASS>) object.getClass());
    }

    /**
     * Creates a new token from the specified raw class.
     *
     * @param rawClass the raw class.
     * @param <CLASS>  the class type.
     * @return the newly created token.
     * @throws NullPointerException if the raw class is null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public static <CLASS> ClassToken<CLASS> tokenOf(@Nonnull final Class<CLASS> rawClass) {

        if (rawClass == null) {

            throw new NullPointerException("the classification raw type must not be null");
        }

        final ClassToken<CLASS> classToken = new ClassToken<CLASS>() {};
        classToken.mGenericType = rawClass;
        classToken.mRawClass = rawClass;

        return classToken;
    }

    /**
     * Casts the specified object to this token type.
     * <p/>
     * Note that the cast is unsafe and may raise an exception.
     *
     * @param object the object to cast.
     * @return the casted object.
     */
    @SuppressWarnings("unchecked")
    public final CLASS cast(final Object object) {

        return (CLASS) object;
    }

    /**
     * Gets the generic type of this token.
     *
     * @return the generic type.
     * @throws IllegalStateException if this class does not correctly extends a class token.
     */
    @Nonnull
    public final Type getGenericType() {

        if (mGenericType == null) {

            Class<?> subClass = getClass();
            Class<?> superClass = subClass.getSuperclass();

            while (!ClassToken.class.equals(superClass)) {

                subClass = superClass;
                superClass = subClass.getSuperclass();
            }

            final Type type = subClass.getGenericSuperclass();

            if (type instanceof ParameterizedType) {

                final ParameterizedType paramType = (ParameterizedType) type;

                mGenericType = paramType.getActualTypeArguments()[0];

            } else {

                throw new IllegalStateException("the class does not correctly extends class token: "
                                                        + getClass().getCanonicalName());
            }
        }

        return mGenericType;
    }

    /**
     * Gets the raw class of this token.
     *
     * @return the raw class.
     * @throws IllegalStateException if this class does not correctly extends a class token.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public final Class<CLASS> getRawClass() {

        if (mRawClass == null) {

            final Type type = getGenericType();

            if (type instanceof Class) {

                mRawClass = ((Class<CLASS>) type);

            } else if (type instanceof ParameterizedType) {

                mRawClass = ((Class<CLASS>) ((ParameterizedType) type).getRawType());

            } else {

                throw new IllegalStateException(
                        "the class does not correctly extends a class token: "
                                + getClass().getCanonicalName());
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

        if (!(o instanceof ClassToken)) {

            return false;
        }

        final ClassToken that = (ClassToken) o;

        return getRawClass().equals(that.getRawClass());
    }

    /**
     * Checks if this token raw class is equal to or is a super class of the specified one.
     *
     * @param other the class token to compare.
     * @return whether this token raw class is equal to or is a super class.
     * @throws IllegalStateException if this class does not correctly extends a class token.
     * @throws NullPointerException  if the other class token is null.
     */
    public final boolean isAssignableFrom(@Nonnull final ClassToken<?> other) {

        return getRawClass().isAssignableFrom(other.getRawClass());
    }

    /**
     * Checks if this token raw class represent an interface.
     *
     * @return whether this token raw class is an interface.
     * @throws IllegalStateException if this class does not correctly extends a class token.
     */
    public final boolean isInterface() {

        return getRawClass().isInterface();
    }
}

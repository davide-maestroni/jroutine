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
package com.github.dm.jrt.util;

import org.jetbrains.annotations.NotNull;

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
 * Remember that, in order for the workaround to properly work at run time, you will need to add the
 * following rule to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes Signature
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 06/14/2014.
 *
 * @param <TYPE> the class type.
 */
public abstract class ClassToken<TYPE> {

    // TODO: 13/10/15 export Proguard file...

    private Type mGenericType;

    private Class<TYPE> mRawClass;

    /**
     * Creates a new token from the specified raw class.
     *
     * @param rawClass the raw class.
     * @param <TYPE>   the class type.
     * @return the newly created token.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <TYPE> ClassToken<TYPE> tokenOf(@NotNull final Class<TYPE> rawClass) {

        if (rawClass == null) {

            throw new NullPointerException("the classification raw type must not be null");
        }

        final ClassToken<TYPE> classToken = new GenericClassToken<TYPE>();
        classToken.mGenericType = rawClass;
        classToken.mRawClass = rawClass;
        return classToken;
    }

    /**
     * Creates a new token from the class of the specified object.
     *
     * @param object the object.
     * @param <TYPE> the class type.
     * @return the newly created token.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <TYPE> ClassToken<TYPE> tokenOf(@NotNull final TYPE object) {

        return tokenOf((Class<TYPE>) object.getClass());
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
    public final TYPE cast(final Object object) {

        return (TYPE) object;
    }

    /**
     * Gets the generic type of this token.
     *
     * @return the generic type.
     * @throws java.lang.IllegalStateException if this class does not correctly extend a class
     *                                         token.
     */
    @NotNull
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

                throw new IllegalStateException(
                        "the class does not correctly extend a class token: "
                                + getClass().getName());
            }
        }

        return mGenericType;
    }

    /**
     * Gets the raw class of this token.
     *
     * @return the raw class.
     * @throws java.lang.IllegalStateException if this class does not correctly extend a class
     *                                         token.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public final Class<TYPE> getRawClass() {

        if (mRawClass == null) {

            final Type type = getGenericType();

            if (type instanceof Class) {

                mRawClass = ((Class<TYPE>) type);

            } else if (type instanceof ParameterizedType) {

                mRawClass = ((Class<TYPE>) ((ParameterizedType) type).getRawType());

            } else {

                throw new IllegalStateException(
                        "the class does not correctly extend a class token: "
                                + getClass().getName());
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

        final ClassToken<?> that = (ClassToken) o;
        return getRawClass().equals(that.getRawClass());
    }

    /**
     * Checks if this token raw class is equal to or is a superclass of the specified one.
     *
     * @param other the class token to compare.
     * @return whether this token raw class is equal to or is a superclass.
     * @throws java.lang.IllegalStateException if this class does not correctly extend a class
     *                                         token.
     */
    public final boolean isAssignableFrom(@NotNull final ClassToken<?> other) {

        return getRawClass().isAssignableFrom(other.getRawClass());
    }

    /**
     * Checks if this token raw class represents an interface.
     *
     * @return whether this token raw class is an interface.
     * @throws java.lang.IllegalStateException if this class does not correctly extend a class
     *                                         token.
     */
    public final boolean isInterface() {

        return getRawClass().isInterface();
    }

    /**
     * Generic class token implementation.
     *
     * @param <TYPE> the class type.
     */
    private static class GenericClassToken<TYPE> extends ClassToken<TYPE> {

    }
}

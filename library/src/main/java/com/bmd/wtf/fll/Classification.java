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
package com.bmd.wtf.fll;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * Created by davide on 6/14/14.
 */
public abstract class Classification<TYPE> {

    private Class<?> mRawType;

    private Type mType;

    public static <RTYPE> Classification<RTYPE> from(final Class<RTYPE> rawType) {

        if (rawType == null) {

            throw new IllegalArgumentException("the classification type cannot be null");
        }

        final Classification<RTYPE> classification = new Classification<RTYPE>() {};
        classification.mRawType = rawType;

        return classification;
    }

    public final TYPE cast(final Object object) {

        //noinspection unchecked
        return (TYPE) object;
    }

    public final Class<?> getRawType() {

        if (mRawType == null) {

            final Type type = getType();

            if (type instanceof Class) {

                //noinspection unchecked
                mRawType = ((Class<?>) type);

            } else if (type instanceof ParameterizedType) {

                //noinspection unchecked
                mRawType = ((Class<?>) ((ParameterizedType) type).getRawType());
            }
        }

        return mRawType;
    }

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

            } else if (type instanceof WildcardType) {

                final Type[] bounds = ((WildcardType) type).getUpperBounds();

                if ((bounds == null) || (bounds.length == 0)) {

                    mType = Object.class;

                } else {

                    mType = bounds[0];
                }
            }
        }

        return mType;
    }

    @Override
    public final int hashCode() {

        return getType().hashCode();
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

        return getType().equals(classification.getType());
    }

    public final boolean isAssignableFrom(final Classification<?> other) {

        return getRawType().isAssignableFrom(other.getRawType());
    }

    public final boolean isInterface() {

        return getRawType().isInterface();
    }
}
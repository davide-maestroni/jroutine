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
package com.bmd.jrt.routine;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Reflection utility class.
 * <p/>
 * Created by davide on 9/9/14.
 */
class ReflectionUtils {

    /**
     * Constant defining an empty argument array for methods or constructors.
     */
    public static final Object[] NO_ARGS = new Object[0];

    private static final HashMap<Class<?>, Class<?>> sBoxMap = new HashMap<Class<?>, Class<?>>(9);

    static {

        final HashMap<Class<?>, Class<?>> boxMap = sBoxMap;

        boxMap.put(boolean.class, Boolean.class);
        boxMap.put(byte.class, Byte.class);
        boxMap.put(char.class, Character.class);
        boxMap.put(double.class, Double.class);
        boxMap.put(float.class, Float.class);
        boxMap.put(int.class, Integer.class);
        boxMap.put(long.class, Long.class);
        boxMap.put(short.class, Short.class);
        boxMap.put(void.class, Void.class);
    }

    /**
     * Avoid direct instantiation.
     */
    private ReflectionUtils() {

    }

    /**
     * Returns the class boxing the specified primitive type.
     * <p/>
     * If the passed class does not represent a primitive type the same class is returned.
     *
     * @param type the primitive type.
     * @return the boxing class.
     */
    public static Class<?> boxingClass(final Class<?> type) {

        if (type == null) {

            return null;
        }

        if (!type.isPrimitive()) {

            return type;
        }

        return sBoxMap.get(type);
    }

    /**
     * Finds the constructor of the specified class best matching the passed arguments.
     * <p/>
     * Note that clashing of signature is automatically avoided, since constructors are not
     * identified by their name. Hence the best match will be always unique in the class.
     *
     * @param type     the target class.
     * @param ctorArgs the constructor arguments.
     * @param <TYPE>   the target type.
     * @return the best matching constructor.
     * @throws IllegalArgumentException if no constructor taking the specified object as parameters
     *                                  was found.
     */
    @SuppressWarnings("unchecked")
    public static <TYPE> Constructor<TYPE> findConstructor(final Class<TYPE> type,
            final Object... ctorArgs) {

        Constructor<?> constructor = findBestMatchingConstructor(type.getConstructors(), ctorArgs);

        if (constructor == null) {

            constructor = findBestMatchingConstructor(type.getDeclaredConstructors(), ctorArgs);

            if (constructor == null) {

                throw new IllegalArgumentException(
                        "no suitable constructor found for type " + type.getCanonicalName());
            }
        }

        if (!constructor.isAccessible()) {

            constructor.setAccessible(true);
        }

        return (Constructor<TYPE>) constructor;
    }

    private static Constructor<?> findBestMatchingConstructor(final Constructor<?>[] constructors,
            final Object[] ctorArgs) {

        final int argsLength = ctorArgs.length;

        Constructor<?> bestMatch = null;
        int maxConfidence = 0;

        for (final Constructor<?> constructor : constructors) {

            final Class<?>[] params = constructor.getParameterTypes();
            final int length = params.length;

            if (length != argsLength) {

                continue;
            }

            boolean isValid = true;
            int confidence = 0;

            for (int i = 0; i < argsLength; ++i) {

                final Object contextArg = ctorArgs[i];
                final Class<?> param = params[i];

                if (contextArg != null) {

                    final Class<?> boxingClass = boxingClass(param);

                    if (!boxingClass.isInstance(contextArg)) {

                        isValid = false;

                        break;
                    }

                    if (boxingClass.equals(contextArg.getClass())) {

                        ++confidence;
                    }

                } else if (param.isPrimitive()) {

                    isValid = false;

                    break;
                }
            }

            if (!isValid) {

                continue;
            }

            if ((bestMatch == null) || (confidence > maxConfidence)) {

                bestMatch = constructor;
                maxConfidence = confidence;

            } else if (confidence == maxConfidence) {

                throw new IllegalArgumentException(
                        "more than one constructor found for arguments: " + Arrays.toString(
                                ctorArgs));

            }
        }

        return bestMatch;
    }
}
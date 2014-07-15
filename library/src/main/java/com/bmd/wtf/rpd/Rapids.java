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
package com.bmd.wtf.rpd;

import java.util.HashMap;

/**
 * Reflection utility class.
 * <p/>
 * Created by davide on 7/11/14.
 */
public class Rapids {

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
    private Rapids() {

    }

    /**
     * Returns the class boxing the specified primitive type.
     * <p/>
     * If the passed class does not represent a primitive type the same class is returned.
     *
     * @param type The primitive type.
     * @return The boxing class.
     */
    public static Class<?> boxedClass(final Class<?> type) {

        if (!type.isPrimitive()) {

            return type;
        }

        return sBoxMap.get(type);
    }
}
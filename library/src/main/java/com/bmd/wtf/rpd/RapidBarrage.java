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

import com.bmd.wtf.flw.Barrage;
import com.bmd.wtf.rpd.RapidAnnotations.FlowPath;
import com.bmd.wtf.rpd.RapidLeap.ValidPaths;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class provides a different way to distribute data inside a barrage.
 * <p/>
 * The two main ways to use it is to inherit the class or to wrap an object inside a rapid barrage.
 * <br/>
 * In both cases the object instance is analyzed searching for methods taking a single object as
 * parameter and returning a primitive integer.<br/>
 * Every time a data drop flows through the barrage, the method whose parameter closely match the
 * drop type is called. In order to properly handle null objects, the inheriting class can implement
 * a method taking a parameter of type <code>Void</code>.
 * <p/>
 * Created by davide on 6/23/14.
 */
public abstract class RapidBarrage implements Barrage<Object> {

    //TODO: proguard rule

    private final HashMap<Class<?>, Method> mMethodMap = new HashMap<Class<?>, Method>();

    private final Object mTarget;

    private final ValidPaths mValidPaths;

    /**
     * Constructor.
     *
     * @param validPaths Whether only the annotated methods must be called when data flow through
     *                   this barrage.
     */
    public RapidBarrage(final ValidPaths validPaths) {

        mTarget = this;
        mValidPaths = validPaths;

        fillMethods();
    }

    /**
     * Constructor.
     * <p/>
     * By default all methods are analyzed.
     *
     * @see #RapidBarrage(com.bmd.wtf.rpd.RapidLeap.ValidPaths)
     */
    public RapidBarrage() {

        this(ValidPaths.ALL);
    }

    /**
     * Constructor.
     *
     * @param wrapped    The wrapped object.
     * @param validPaths Whether only the annotated methods must be called when data flow through
     *                   this barrage.
     */
    private RapidBarrage(final Object wrapped, final ValidPaths validPaths) {

        mTarget = wrapped;
        mValidPaths = validPaths;

        fillMethods();
    }

    /**
     * Creates and returns a rapid barrage wrapping the specified object.
     *
     * @param wrapped The wrapped object.
     * @return The new rapid barrage.
     */
    public static RapidBarrage from(final Object wrapped) {

        return new RapidBarrage(wrapped, ValidPaths.ALL) {};
    }

    /**
     * Creates and returns a rapid barrage wrapping the specified object.
     * <p/>
     * Note that only the annotated method will be considered when handling flowing data.
     *
     * @param wrapped The wrapped object.
     * @return The new rapid barrage.
     */
    public static RapidBarrage fromAnnotated(final Object wrapped) {

        return new RapidBarrage(wrapped, ValidPaths.ANNOTATED_ONLY) {};
    }

    private static void fillMethodMap(final HashMap<Class<?>, Method> methodMap,
            final Method method, final Class<?> parameterType) {

        final Method currentMethod = methodMap.get(parameterType);

        if ((currentMethod != null) && !currentMethod.equals(method)) {

            throw new IllegalArgumentException(
                    "cannot override a method already handling data of type: "
                            + parameterType.getCanonicalName());
        }

        methodMap.put(parameterType, method);
    }

    @Override
    public final int onPush(final Object drop) {

        final Method method;

        if (drop != null) {

            method = findMethod(drop.getClass());

        } else {

            method = mMethodMap.get(Void.class);
        }

        if (method != null) {

            try {

                return (Integer) method.invoke(mTarget, drop);

            } catch (final InvocationTargetException e) {

                throw new RapidException(e.getCause());

            } catch (final IllegalAccessException e) {

                throw new RapidException(e);
            }
        }

        return DEFAULT_STREAM;
    }

    private void fillMethods() {

        final Class<RapidBarrage> rapidLeapClass = RapidBarrage.class;
        final Class<Object> objectClass = Object.class;

        final HashMap<Class<?>, Method> globalMap = mMethodMap;

        Class<?> type = mTarget.getClass();

        while (!rapidLeapClass.equals(type) && !objectClass.equals(type)) {

            final Method[] methods = type.getDeclaredMethods();

            for (Entry<Class<?>, Method> entry : getMethods(methods).entrySet()) {

                final Class<?> dropType = entry.getKey();

                if (globalMap.containsKey(dropType)) {

                    continue;
                }

                final Method method = entry.getValue();

                if (!method.isAccessible()) {

                    if (method.isAnnotationPresent(FlowPath.class) || (
                            (method.getModifiers() & Modifier.PUBLIC) != 0)) {

                        method.setAccessible(true);

                    } else {

                        continue;
                    }
                }

                globalMap.put(dropType, method);
            }

            type = type.getSuperclass();
        }
    }

    private Method findMethod(final Class<?> dropType) {

        final HashMap<Class<?>, Method> methodMap = mMethodMap;

        Method method = methodMap.get(dropType);

        if (method == null) {

            Class<?> bestMatch = null;

            for (final Entry<Class<?>, Method> entry : methodMap.entrySet()) {

                final Class<?> type = entry.getKey();

                if (type.isAssignableFrom(dropType)) {

                    final Method candidate = entry.getValue();
                    final FlowPath annotation = candidate.getAnnotation(FlowPath.class);

                    if (((annotation == null) || (annotation.value().length == 0) || type.equals(
                            dropType)) && ((bestMatch == null) || bestMatch.isAssignableFrom(
                            type))) {

                        method = candidate;
                        bestMatch = type;
                    }
                }
            }
        }

        return method;
    }

    private Map<Class<?>, Method> getMethods(final Method[] methods) {

        final HashMap<Class<?>, Method> methodMap = new HashMap<Class<?>, Method>(methods.length);

        final boolean isAnnotatedOnly = (mValidPaths == ValidPaths.ANNOTATED_ONLY);

        for (final Method method : methods) {

            final Class<?>[] parameterTypes = method.getParameterTypes();
            final FlowPath annotation = method.getAnnotation(FlowPath.class);

            boolean validMethod =
                    (parameterTypes.length == 1) && (method.getReturnType().equals(int.class));

            if (annotation != null) {

                final Class<?>[] annotationTypes = annotation.value();

                if (validMethod) {

                    final Class<?> parameterType = parameterTypes[0];

                    for (final Class<?> annotationType : annotationTypes) {

                        if (!parameterType.isAssignableFrom(annotationType)) {

                            validMethod = false;

                            break;
                        }

                        fillMethodMap(methodMap, method, annotationType);
                    }
                }

                if (!validMethod) {

                    throw new IllegalArgumentException(
                            "invalid annotated method: " + method + "\nAn "
                                    + FlowPath.class.getSimpleName()
                                    + " method must take a single parameter");
                }

                if (annotationTypes.length > 0) {

                    continue;
                }

            } else if (!validMethod || isAnnotatedOnly) {

                continue;
            }

            fillMethodMap(methodMap, method, parameterTypes[0]);
        }

        return methodMap;
    }
}
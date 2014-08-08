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
package com.bmd.wtf.xtr.rpd;

import com.bmd.wtf.flw.Pump;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.DataFlow;
import com.bmd.wtf.xtr.rpd.RapidGate.ValidFlows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class provides an alternative way to distribute data inside a pump.
 * <p/>
 * The two main ways to use it is to inherit the class or to wrap an object inside a rapid pump.
 * <br/>
 * In both cases the object instance is analyzed searching for methods taking a single object as
 * parameter and returning a primitive integer.<br/>
 * Every time a data drop flows through the pump, the method whose parameter closely match the
 * drop type is called. In order to properly handle null objects, the inheriting class can implement
 * a method taking a parameter of type <code>Void</code>.
 * <p/>
 * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
 * file:
 * <pre>
 *     <code>
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$DataFlow *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 6/23/14.
 */
public abstract class RapidPump implements Pump<Object> {

    private final HashMap<Class<?>, Method> mMethodMap = new HashMap<Class<?>, Method>();

    private final Object mTarget;

    private final ValidFlows mValidFlows;

    /**
     * Constructor.
     *
     * @param validFlows whether only the annotated methods must be called when data flow through
     *                   this pump.
     */
    public RapidPump(final ValidFlows validFlows) {

        mTarget = this;
        mValidFlows = validFlows;

        fillMethods();
    }

    /**
     * Constructor.
     * <p/>
     * By default all methods are analyzed.
     *
     * @see #RapidPump(RapidGate.ValidFlows)
     */
    public RapidPump() {

        this(ValidFlows.ALL);
    }

    /**
     * Constructor.
     *
     * @param wrapped    the wrapped object.
     * @param validFlows whether only the annotated methods must be called when data flow through
     *                   this pump.
     */
    private RapidPump(final Object wrapped, final ValidFlows validFlows) {

        mTarget = wrapped;
        mValidFlows = validFlows;

        fillMethods();
    }

    /**
     * Creates and returns a rapid pump wrapping the specified object.
     *
     * @param wrapped the wrapped object.
     * @return the new rapid pump.
     */
    public static RapidPump from(final Object wrapped) {

        return new RapidPump(wrapped, ValidFlows.ALL) {};
    }

    /**
     * Creates and returns a rapid pump wrapping the specified object.
     * <p/>
     * Note that only the annotated method will be considered when handling flowing data.
     *
     * @param wrapped the wrapped object.
     * @return the new rapid pump.
     */
    public static RapidPump fromAnnotated(final Object wrapped) {

        return new RapidPump(wrapped, ValidFlows.ANNOTATED_ONLY) {};
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

        final Class<RapidPump> rapidPumpClass = RapidPump.class;
        final Class<Object> objectClass = Object.class;

        final HashMap<Class<?>, Method> globalMap = mMethodMap;

        Class<?> type = mTarget.getClass();

        while (!rapidPumpClass.equals(type) && !objectClass.equals(type)) {

            final Method[] methods = type.getDeclaredMethods();

            for (Entry<Class<?>, Method> entry : getMethods(methods).entrySet()) {

                final Class<?> dropType = entry.getKey();

                if (globalMap.containsKey(dropType)) {

                    continue;
                }

                final Method method = entry.getValue();

                if (!method.isAccessible()) {

                    if (method.isAnnotationPresent(DataFlow.class) || (
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
                    final DataFlow annotation = candidate.getAnnotation(DataFlow.class);

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

        final boolean isAnnotatedOnly = (mValidFlows == ValidFlows.ANNOTATED_ONLY);

        for (final Method method : methods) {

            final Class<?>[] parameterTypes = method.getParameterTypes();
            final DataFlow annotation = method.getAnnotation(DataFlow.class);

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
                                    + DataFlow.class.getSimpleName()
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
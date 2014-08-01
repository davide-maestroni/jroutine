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

import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.rpd.RapidAnnotations.FlowPath;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class provides a different way to filter and transform data inside a leap.
 * <p/>
 * The two main ways to use it is to inherit the class or to wrap an object inside a rapid leap.
 * <br/>
 * In both cases the object instance is analyzed searching for methods taking a single object as
 * parameter.<br/>
 * Every time a data drop flows through the leap, the method whose parameter closely match the
 * drop type is called. In order to properly handle null objects and discharge command, the
 * inheriting class can implement a method taking a parameter of type <code>Void</code> and a one
 * taking a parameter of type <code>Discharge</code> respectively.<br/>
 * In a dual way, a method returning a <code>Throwable</code> object will cause a forward of an
 * unhandled exception, while a one returning a <code>Discharge</code> will cause a discharge of
 * the downstream river. Finally, if a method does not return any result, nothing will be
 * propagated downstream.
 * <p/>
 * The inheriting class may also make use of the protected method provided by this class to access
 * the downstream and upstream rivers, and the waterfall gates.
 * <p/>
 * Created by davide on 6/23/14.
 */
public abstract class RapidLeap implements Leap<Object, Object> {

    //TODO: proguard rule

    private final HashMap<Class<?>, Method> mMethodMap = new HashMap<Class<?>, Method>();

    private final Object mTarget;

    private final ValidPaths mValidPaths;

    private River<Object> mDownRiver;

    private int mFallNumber;

    private River<Object> mUpRiver;

    /**
     * Constructor.
     *
     * @param validPaths Whether only the annotated methods must be called when data flow through
     *                   this leap.
     */
    public RapidLeap(final ValidPaths validPaths) {

        mTarget = this;
        mValidPaths = validPaths;

        fillMethods();
    }

    /**
     * Constructor.
     * <p/>
     * By default all methods are analyzed.
     *
     * @see #RapidLeap(com.bmd.wtf.rpd.RapidLeap.ValidPaths)
     */
    public RapidLeap() {

        this(ValidPaths.ALL);
    }

    /**
     * Constructor.
     *
     * @param wrapped    The wrapped object.
     * @param validPaths Whether only the annotated methods must be called when data flow through
     *                   this leap.
     */
    private RapidLeap(final Object wrapped, final ValidPaths validPaths) {

        mTarget = wrapped;
        mValidPaths = validPaths;

        fillMethods();
    }

    /**
     * Creates and returns a rapid leap wrapping the specified object.
     *
     * @param wrapped The wrapped object.
     * @return The new rapid leap.
     */
    public static RapidLeap from(final Object wrapped) {

        return new RapidLeap(wrapped, ValidPaths.ALL) {};
    }

    /**
     * Creates and returns a rapid leap wrapping the specified object.
     * <p/>
     * Note that only the annotated method will be considered when handling flowing data.
     *
     * @param wrapped The wrapped object.
     * @return The new rapid leap.
     */
    public static RapidLeap fromAnnotated(final Object wrapped) {

        return new RapidLeap(wrapped, ValidPaths.ANNOTATED_ONLY) {};
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
    public final void onDischarge(final River<Object> upRiver, final River<Object> downRiver,
            final int fallNumber) {

        setup(upRiver, downRiver, fallNumber);

        final Method method = mMethodMap.get(Discharge.class);

        if (method != null) {

            try {

                final Object result = method.invoke(mTarget, (Discharge) null);

                propagateResult(downRiver, method.getReturnType(), result);

            } catch (final InvocationTargetException e) {

                throw new RapidException(e.getCause());

            } catch (final IllegalAccessException e) {

                throw new RapidException(e);
            }

        } else {

            downRiver.discharge();
        }
    }

    @Override
    public final void onPush(final River<Object> upRiver, final River<Object> downRiver,
            final int fallNumber, final Object drop) {

        setup(upRiver, downRiver, fallNumber);

        final Method method;

        if (drop != null) {

            method = findMethod(drop.getClass());

        } else {

            method = mMethodMap.get(Void.class);
        }

        if (method != null) {

            try {

                final Object result = method.invoke(mTarget, drop);

                propagateResult(downRiver, method.getReturnType(), result);

            } catch (final InvocationTargetException e) {

                throw new RapidException(e.getCause());

            } catch (final IllegalAccessException e) {

                throw new RapidException(e);
            }

        } else {

            downRiver.push(drop);
        }
    }

    @Override
    public final void onUnhandled(final River<Object> upRiver, final River<Object> downRiver,
            final int fallNumber, final Throwable throwable) {

        setup(upRiver, downRiver, fallNumber);

        final Method method;

        if (throwable != null) {

            method = findMethod(throwable.getClass());

        } else {

            method = mMethodMap.get(Void.class);
        }

        if (method != null) {

            try {

                final Object result = method.invoke(mTarget, throwable);

                propagateResult(downRiver, method.getReturnType(), result);

            } catch (final InvocationTargetException e) {

                throw new RapidException(e.getCause());

            } catch (final IllegalAccessException e) {

                throw new RapidException(e);
            }

        } else {

            downRiver.forward(throwable);
        }
    }

    /**
     * Returns the downstream river instance.
     *
     * @return The river instance.
     */
    protected River<Object> downRiver() {

        return mDownRiver;
    }

    /**
     * Dries up this leap by draining both the downstream and upstream rivers.
     *
     * @see com.bmd.wtf.flw.River#drain()
     */
    protected void dryUp() {

        mUpRiver.drain();
        mDownRiver.drain();
    }

    /**
     * Returns the number identifying the fall formed by this leap.
     *
     * @return The fall number.
     */
    protected int fallNumber() {

        return mFallNumber;
    }

    /**
     * Isolates this leap by deviating both the downstream and upstream rivers.
     *
     * @see com.bmd.wtf.flw.River#deviate()
     */
    protected void isolate() {

        mUpRiver.deviate();
        mDownRiver.deviate();
    }

    /**
     * Returns a rapid gate handling a leap of the specified type.
     * <p/>
     * If no leap of that type is not found inside the waterfall an exception will be thrown.
     *
     * @param gateClass The gate class.
     * @param <TYPE>    The leap type.
     * @return The gate.
     */
    protected <TYPE> RapidGate<TYPE> on(final Class<TYPE> gateClass) {

        return new DefaultRapidGate<TYPE>(mUpRiver.on(gateClass), gateClass);
    }

    /**
     * Returns a rapid gate handling a leap of the specified type.
     * <p/>
     * If the leap is not found inside the waterfall an exception will be thrown.
     *
     * @param gateClassification The gate classification.
     * @param <TYPE>             The leap type.
     * @return The gate.
     */
    protected <TYPE> RapidGate<TYPE> on(final Classification<TYPE> gateClassification) {

        return new DefaultRapidGate<TYPE>(mUpRiver.on(gateClassification),
                                          gateClassification.getRawType());
    }

    /**
     * Returns the upstream river instance.
     *
     * @return The river instance.
     */
    protected River<Object> upRiver() {

        return mUpRiver;
    }

    private void fillMethods() {

        final Class<RapidLeap> rapidLeapClass = RapidLeap.class;
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

            boolean validMethod = (parameterTypes.length == 1);

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

    private void propagateResult(final River<Object> downRiver, final Class<?> returnType,
            final Object result) {

        if (!returnType.equals(void.class)) {

            final Class<Throwable> throwableClass = Throwable.class;

            if (throwableClass.isAssignableFrom(returnType)) {

                downRiver.forward(throwableClass.cast(result));

            } else if (Discharge.class.isAssignableFrom(returnType)) {

                downRiver.discharge();

            } else {

                downRiver.push(result);
            }
        }
    }

    private void setup(final River<Object> upRiver, final River<Object> downRiver,
            final int fallNumber) {

        mUpRiver = upRiver;
        mDownRiver = downRiver;
        mFallNumber = fallNumber;
    }

    /**
     * The rule to decide whether a method is a valid flow path or not.
     */
    public enum ValidPaths {

        ANNOTATED_ONLY,
        ALL
    }

    /**
     * Non-instantiable class used to identified the method responsible for handling discharge notifications.
     */
    public static final class Discharge {

        private Discharge() {

        }
    }
}
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

import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.gts.Gate;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.DataFlow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class provides an alternative way to filter and transform data inside a gate.
 * <p/>
 * The two main ways to use it is to inherit the class or to wrap an object inside a rapid gate.
 * <br/>
 * In both cases the object instance is analyzed searching for methods taking a single object as
 * parameter.<br/>
 * Every time a data drop flows through the gate, the method whose parameter closely match the
 * drop type is called. In order to properly handle null objects and flush command, the inheriting
 * class can implement a method taking a parameter of type <code>Void</code> and a one taking a
 * parameter of type <code>Flush</code> respectively.<br/>
 * In a dual way, a method returning a <code>Throwable</code> object will cause a forward of an
 * unhandled exception, while a one returning a <code>Flush</code> will cause a flush of the
 * downstream river. Finally, if a method does not return any result, nothing will be propagated
 * downstream.
 * <p/>
 * The inheriting class may also make use of the protected method provided by this class to access
 * the downstream and upstream rivers, and the waterfall dams.
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
public abstract class RapidGate implements Gate<Object, Object> {

    private final HashMap<Class<?>, Method> mMethodMap = new HashMap<Class<?>, Method>();

    private final Object mTarget;

    private final ValidFlows mValidFlows;

    private River<Object> mDownRiver;

    private int mFallNumber;

    private River<Object> mUpRiver;

    /**
     * Constructor.
     *
     * @param validFlows whether only the annotated methods must be called when data flow through
     *                   this gate.
     */
    public RapidGate(final ValidFlows validFlows) {

        mTarget = this;
        mValidFlows = validFlows;

        fillMethods();
    }

    /**
     * Constructor.
     * <p/>
     * By default all methods are analyzed.
     *
     * @see #RapidGate(RapidGate.ValidFlows)
     */
    public RapidGate() {

        this(ValidFlows.ALL);
    }

    /**
     * Constructor.
     *
     * @param wrapped    the wrapped object.
     * @param validFlows whether only the annotated methods must be called when data flow through
     *                   this gate.
     */
    private RapidGate(final Object wrapped, final ValidFlows validFlows) {

        mTarget = wrapped;
        mValidFlows = validFlows;

        fillMethods();
    }

    /**
     * Creates and returns a rapid gate wrapping the specified object.
     *
     * @param wrapped the wrapped object.
     * @return the new rapid gate.
     */
    public static RapidGate from(final Object wrapped) {

        return new RapidGate(wrapped, ValidFlows.ALL) {};
    }

    /**
     * Creates and returns a rapid gate wrapping the specified object.
     * <p/>
     * Note that only the annotated method will be considered when handling flowing data.
     *
     * @param wrapped the wrapped object.
     * @return the new rapid gate.
     */
    public static RapidGate fromAnnotated(final Object wrapped) {

        return new RapidGate(wrapped, ValidFlows.ANNOTATED_ONLY) {};
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
    public final void onException(final River<Object> upRiver, final River<Object> downRiver,
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

            downRiver.exception(throwable);
        }
    }

    @Override
    public final void onFlush(final River<Object> upRiver, final River<Object> downRiver,
            final int fallNumber) {

        setup(upRiver, downRiver, fallNumber);

        final Method method = mMethodMap.get(Flush.class);

        if (method != null) {

            try {

                final Object result = method.invoke(mTarget, (Flush) null);

                propagateResult(downRiver, method.getReturnType(), result);

            } catch (final InvocationTargetException e) {

                throw new RapidException(e.getCause());

            } catch (final IllegalAccessException e) {

                throw new RapidException(e);
            }

        } else {

            downRiver.flush();
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

    /**
     * Returns the downstream river instance.
     *
     * @return the river instance.
     */
    protected River<Object> downRiver() {

        return mDownRiver;
    }

    /**
     * Dries up this gate by draining both the downstream and upstream rivers.
     *
     * @see com.bmd.wtf.flw.River#drain()
     */
    protected void dryUp() {

        mUpRiver.drain();
        mDownRiver.drain();
    }

    /**
     * Returns the number identifying the fall formed by this gate.
     *
     * @return the fall number.
     */
    protected int fallNumber() {

        return mFallNumber;
    }

    /**
     * Isolates this gate by deviating both the downstream and upstream rivers.
     *
     * @see com.bmd.wtf.flw.River#deviate()
     */
    protected void isolate() {

        mUpRiver.deviate();
        mDownRiver.deviate();
    }

    /**
     * Returns a rapid dam handling a gate of the specified type.
     * <p/>
     * If no gate of that type is not found inside the waterfall an exception will be thrown.
     *
     * @param damClass the dam class.
     * @param <TYPE>   the gate type.
     * @return the dam.
     */
    protected <TYPE> RapidDam<TYPE> on(final Class<TYPE> damClass) {

        return new DefaultRapidDam<TYPE>(mUpRiver.on(damClass), damClass);
    }

    /**
     * Returns a rapid dam handling a gate of the specified type.
     * <p/>
     * If the gate is not found inside the waterfall an exception will be thrown.
     *
     * @param damClassification the dam classification.
     * @param <TYPE>            the gate type.
     * @return the dam.
     */
    protected <TYPE> RapidDam<TYPE> on(final Classification<TYPE> damClassification) {

        return new DefaultRapidDam<TYPE>(mUpRiver.on(damClassification),
                                         damClassification.getRawType());
    }

    /**
     * Returns the upstream river instance.
     *
     * @return the river instance.
     */
    protected River<Object> upRiver() {

        return mUpRiver;
    }

    private void fillMethods() {

        final Class<RapidGate> rapidGateClass = RapidGate.class;
        final Class<Object> objectClass = Object.class;

        final HashMap<Class<?>, Method> globalMap = mMethodMap;

        Class<?> type = mTarget.getClass();

        while (!rapidGateClass.equals(type) && !objectClass.equals(type)) {

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

    private void propagateResult(final River<Object> downRiver, final Class<?> returnType,
            final Object result) {

        if (!returnType.equals(void.class)) {

            final Class<Throwable> throwableClass = Throwable.class;

            if (throwableClass.isAssignableFrom(returnType)) {

                downRiver.exception(throwableClass.cast(result));

            } else if (Flush.class.isAssignableFrom(returnType)) {

                downRiver.flush();

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
     * The rule to decide whether a method is a valid flow or not.
     */
    public enum ValidFlows {

        ANNOTATED_ONLY,
        ALL
    }

    /**
     * Non-instantiable class used to identify the method responsible for handling flush
     * notifications.
     */
    public static final class Flush {

        private Flush() {

        }
    }
}
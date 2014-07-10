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
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.OnData;

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
 * In both the case the object must implements specific methods to handle the data flowing through
 * the leap.
 * <p/>
 * Created by davide on 6/23/14.
 *
 * @param <SOURCE> The source data type.
 */
public abstract class RapidLeap<SOURCE> implements Leap<SOURCE, Object, Object> {

    //TODO: proguard rule

    private final boolean mIsAnnotatedOnly;

    private final HashMap<Class<?>, Method> mMethodMap = new HashMap<Class<?>, Method>();

    private final Object mTarget;

    private River<SOURCE, Object> mDownRiver;

    private int mFallNumber;

    private River<SOURCE, Object> mUpRiver;

    public RapidLeap(final boolean annotatedOnly) {

        mTarget = this;
        mIsAnnotatedOnly = annotatedOnly;

        fillMethods();
    }

    public RapidLeap() {

        this(false);
    }

    private RapidLeap(final Object wrapped, final boolean annotatedOnly) {

        mTarget = wrapped;
        mIsAnnotatedOnly = annotatedOnly;

        fillMethods();
    }

    public static <SOURCE> RapidLeap<SOURCE> from(final Object wrapped) {

        return new RapidLeap<SOURCE>(wrapped, false) {};
    }

    public static <SOURCE> RapidLeap<SOURCE> from(final Object wrapped,
            final Class<SOURCE> sourceType) {

        return from(wrapped);
    }

    public static <SOURCE> RapidLeap<SOURCE> from(final Object wrapped,
            final Classification<SOURCE> sourceClassification) {

        return from(wrapped);
    }

    public static <SOURCE> RapidLeap<SOURCE> fromAnnotated(final Object wrapped) {

        return new RapidLeap<SOURCE>(wrapped, true) {};
    }

    public static <SOURCE> RapidLeap<SOURCE> fromAnnotated(final Object wrapped,
            final Class<SOURCE> sourceType) {

        return fromAnnotated(wrapped);
    }

    public static <SOURCE> RapidLeap<SOURCE> fromAnnotated(final Object wrapped,
            final Classification<SOURCE> sourceClassification) {

        return fromAnnotated(wrapped);
    }

    @Override
    public final void onDischarge(final River<SOURCE, Object> upRiver,
            final River<SOURCE, Object> downRiver, final int fallNumber) {

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
    public final void onPush(final River<SOURCE, Object> upRiver,
            final River<SOURCE, Object> downRiver, final int fallNumber, final Object drop) {

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
    public final void onUnhandled(final River<SOURCE, Object> upRiver,
            final River<SOURCE, Object> downRiver, final int fallNumber,
            final Throwable throwable) {

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

    protected River<SOURCE, Object> downRiver() {

        return mDownRiver;
    }

    protected void dryUp() {

        mUpRiver.drain();
        mDownRiver.drain();
    }

    protected int fallNumber() {

        return mFallNumber;
    }

    protected void isolate() {

        mUpRiver.deviate();
        mDownRiver.deviate();
    }

    protected River<SOURCE, SOURCE> source() {

        return mUpRiver.source();
    }

    protected River<SOURCE, Object> upRiver() {

        return mUpRiver;
    }

    protected <TYPE> RapidGate<TYPE> when(Class<TYPE> gateClass) {

        return new DefaultRapidGate<TYPE>(mUpRiver.on(gateClass), gateClass);
    }

    protected <TYPE> RapidGate<TYPE> when(Classification<TYPE> gateClassification) {

        return new DefaultRapidGate<TYPE>(mUpRiver.on(gateClassification),
                                          gateClassification.getRawType());
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

                    if (method.isAnnotationPresent(OnData.class) || (
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

                    if ((bestMatch == null) || bestMatch.isAssignableFrom(type)) {

                        method = entry.getValue();

                        bestMatch = type;
                    }
                }
            }
        }

        return method;
    }

    private Map<Class<?>, Method> getMethods(final Method[] methods) {

        final HashMap<Class<?>, Method> methodMap = new HashMap<Class<?>, Method>(methods.length);

        final boolean isAnnotatedOnly = mIsAnnotatedOnly;

        for (final Method method : methods) {

            final Class<?>[] parameterTypes = method.getParameterTypes();
            final boolean isAnnotated = method.isAnnotationPresent(OnData.class);

            if (parameterTypes.length != 1) {

                if (isAnnotated) {

                    throw new IllegalArgumentException(
                            "invalid annotated method: " + method + "\nAn "
                                    + OnData.class.getSimpleName()
                                    + " method must take a single parameter"
                    );

                } else {

                    continue;
                }
            }

            if (!isAnnotated && isAnnotatedOnly) {

                continue;
            }

            final Class<?> parameterType = parameterTypes[0];
            final Method currentMethod = methodMap.get(parameterType);

            if ((currentMethod != null) && !currentMethod.equals(method)) {

                throw new IllegalArgumentException(
                        "cannot override a method already handling data of type: "
                                + parameterType.getCanonicalName()
                );
            }

            methodMap.put(parameterType, method);
        }

        return methodMap;
    }

    private void propagateResult(final River<SOURCE, Object> downRiver, final Class<?> returnType,
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

    private void setup(final River<SOURCE, Object> upRiver, final River<SOURCE, Object> downRiver,
            final int fallNumber) {

        mUpRiver = upRiver;
        mDownRiver = downRiver;
        mFallNumber = fallNumber;
    }

    /**
     * Non-instantiable class used to identified the method responsible for handling discharge notifications.
     */
    public static final class Discharge {

        private Discharge() {

        }
    }
}
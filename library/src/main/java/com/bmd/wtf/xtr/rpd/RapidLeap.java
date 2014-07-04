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
import com.bmd.wtf.xtr.rpd.RapidAnnotations.OnDischarge;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.OnNull;

import java.lang.reflect.Method;
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

    private Method mOnDischarge;

    private Method mOnNull;

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

        final Method onDischarge = mOnDischarge;

        if (onDischarge != null) {

            try {

                final Object result = onDischarge.invoke(this);

                final Class<?> returnType = onDischarge.getReturnType();

                if (!returnType.equals(void.class)) {

                    downRiver.push(result).discharge();
                }

            } catch (final Throwable t) {

                throw new RapidException(t);
            }

        } else {

            downRiver.discharge();
        }
    }

    @Override
    public final void onPush(final River<SOURCE, Object> upRiver,
            final River<SOURCE, Object> downRiver, final int fallNumber, final Object drop) {

        setup(upRiver, downRiver, fallNumber);

        if (drop == null) {

            final Method onNull = mOnNull;

            if (onNull != null) {

                try {

                    final Object result = onNull.invoke(this);

                    final Class<?> returnType = onNull.getReturnType();

                    if (!returnType.equals(void.class)) {

                        downRiver.push(result);
                    }

                } catch (final Throwable t) {

                    throw new RapidException(t);
                }

            } else {

                downRiver.push((Object) null);
            }

            return;
        }

        final Method method = findMethod(drop);

        if (method == null) {

            downRiver.push(drop);

        } else {

            try {

                final Object result = method.invoke(this, drop);

                final Class<?> returnType = method.getReturnType();

                if (!returnType.equals(void.class)) {

                    downRiver.push(result);
                }

            } catch (final Throwable t) {

                throw new RapidException(t);
            }
        }
    }

    @Override
    public final void onUnhandled(final River<SOURCE, Object> upRiver,
            final River<SOURCE, Object> downRiver, final int fallNumber,
            final Throwable throwable) {

        setup(upRiver, downRiver, fallNumber);

        if (throwable == null) {

            final Method onNull = mOnNull;

            if (onNull != null) {

                try {

                    final Object result = onNull.invoke(this);

                    final Class<?> returnType = onNull.getReturnType();

                    if (!returnType.equals(void.class)) {

                        downRiver.push(result);
                    }

                } catch (final Throwable t) {

                    throw new RapidException(t);
                }

            } else {

                downRiver.forward(null);
            }

            return;
        }

        final Method method = findMethod(throwable);

        if (method == null) {

            downRiver.forward(throwable);

        } else {

            try {

                final Object result = method.invoke(this, throwable);

                final Class<?> returnType = method.getReturnType();

                if (!returnType.equals(void.class)) {

                    downRiver.push(result);
                }

            } catch (final Throwable t) {

                throw new RapidException(t);
            }
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

        final Class<?> objectClass = mTarget.getClass();

        final Method[] methods = objectClass.getMethods();
        final Method[] declaredMethods = objectClass.getDeclaredMethods();

        final HashMap<Class<?>, Method> methodMap = mMethodMap;

        fillOnData(methods, methodMap);

        for (final Method method : methodMap.values()) {

            if (!method.isAccessible()) {

                method.setAccessible(true);
            }
        }

        final HashMap<Class<?>, Method> declaredMethodMap = new HashMap<Class<?>, Method>();

        fillOnData(declaredMethods, declaredMethodMap);

        for (final Entry<Class<?>, Method> entry : declaredMethodMap.entrySet()) {

            final Class<?> key = entry.getKey();

            if (!methodMap.containsKey(key)) {

                final Method method = entry.getValue();

                if (!method.isAccessible()) {

                    method.setAccessible(true);
                }

                methodMap.put(key, method);
            }
        }

        Method onNull = fillOnNull(methods);

        if (onNull == null) {

            onNull = fillOnNull(declaredMethods);
        }

        if (onNull != null) {

            if (!onNull.isAccessible()) {

                onNull.setAccessible(true);
            }

            mOnNull = onNull;
        }

        Method onDischarge = fillOnDischarge(methods);

        if (onDischarge == null) {

            onDischarge = fillOnDischarge(declaredMethods);
        }

        if (onDischarge != null) {

            if (!onDischarge.isAccessible()) {

                onDischarge.setAccessible(true);
            }

            mOnDischarge = onDischarge;
        }
    }

    private void fillOnData(final Method[] methods, final Map<Class<?>, Method> methodMap) {

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

            if (!isAnnotated) {

                if (isAnnotatedOnly) {

                    continue;
                }

                final String methodName = method.getName();

                if (!methodName.startsWith("on")) {

                    continue;
                }

                if (methodName.length() > 2) {

                    final char c = methodName.charAt(2);

                    if (!Character.isDigit(c) && !Character.isUpperCase(c)) {

                        continue;
                    }
                }
            }

            final Class<?> parameterType = parameterTypes[0];

            final Method currentMethod = methodMap.get(parameterType);

            if ((currentMethod != null) && !currentMethod.equals(method)) {

                throw new IllegalArgumentException(
                        "cannot override a method already handling data of type: "
                                + parameterType.getSimpleName()
                );
            }

            methodMap.put(parameterType, method);
        }
    }

    private Method fillOnDischarge(final Method[] methods) {

        Method onDischarge = null;

        final boolean isAnnotatedOnly = mIsAnnotatedOnly;

        for (final Method method : methods) {

            final Class<?>[] parameterTypes = method.getParameterTypes();

            final boolean isAnnotated = method.isAnnotationPresent(OnDischarge.class);

            if (parameterTypes.length != 0) {

                if (isAnnotated) {

                    throw new IllegalArgumentException(
                            "invalid annotated method: " + method + "\nAn "
                                    + OnDischarge.class.getSimpleName()
                                    + " method must take no parameters"
                    );

                } else {

                    continue;
                }
            }

            if (!isAnnotated && (isAnnotatedOnly || !method.getName().equals("onDischarge"))) {

                continue;
            }

            if ((onDischarge != null) && !onDischarge.equals(method)) {

                throw new IllegalArgumentException(
                        "cannot override a method already handling data discharge");
            }

            onDischarge = method;
        }

        return onDischarge;
    }

    private Method fillOnNull(final Method[] methods) {

        Method onNull = null;

        final boolean isAnnotatedOnly = mIsAnnotatedOnly;

        for (final Method method : methods) {

            final Class<?>[] parameterTypes = method.getParameterTypes();

            final boolean isAnnotated = method.isAnnotationPresent(OnNull.class);

            if (parameterTypes.length != 0) {

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

            if (!isAnnotated && (isAnnotatedOnly || !method.getName().equals("onNull"))) {

                continue;
            }

            if ((onNull != null) && !onNull.equals(method)) {

                throw new IllegalArgumentException(
                        "cannot override a method already handling null data");
            }

            onNull = method;
        }

        return onNull;
    }

    private Method findMethod(final Object drop) {

        final Class<?> dropClass = drop.getClass();

        Method method = null;

        Class<?> bestMatch = null;

        for (final Entry<Class<?>, Method> entry : mMethodMap.entrySet()) {

            final Class<?> type = entry.getKey();

            if (dropClass.isAssignableFrom(type)) {

                if ((bestMatch == null) || type.isAssignableFrom(bestMatch)) {

                    method = entry.getValue();

                    bestMatch = type;
                }
            }
        }

        return method;
    }

    private void setup(final River<SOURCE, Object> upRiver, final River<SOURCE, Object> downRiver,
            final int fallNumber) {

        mUpRiver = upRiver;
        mDownRiver = downRiver;
        mFallNumber = fallNumber;
    }
}
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
import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.OnData;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.OnDischarge;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.OnNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/23/14.
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

    protected final void deviate(final boolean downStream) {

        river(downStream).deviate();
    }

    protected final void deviate(final boolean downStream, final int streamNumber) {

        river(downStream).deviate(streamNumber);
    }

    protected final void discharge(final boolean downStream) {

        river(downStream).discharge();
    }

    protected final void discharge(final boolean downStream, final int streamNumber) {

        river(downStream).discharge(streamNumber);
    }

    protected final void drain(final boolean downStream) {

        river(downStream).drain();
    }

    protected final void drain(final boolean downStream, final int streamNumber) {

        river(downStream).drain(streamNumber);
    }

    protected final void dryUp() {

        mUpRiver.drain();
        mDownRiver.drain();
    }

    protected final int fallNumber() {

        return mFallNumber;
    }

    protected final void isolate() {

        mUpRiver.deviate();
        mDownRiver.deviate();
    }

    protected final void loopBack(final int fallNumber, final Iterable<Object> drops) {

        mUpRiver.push(fallNumber, drops);
    }

    protected final void loopBack(final Iterable<Object> drops) {

        mUpRiver.push(drops);
    }

    protected final void loopBack(final int fallNumber, final Object... drops) {

        mUpRiver.push(fallNumber, drops);
    }

    protected final void loopBack(final Object... drops) {

        mUpRiver.push(drops);
    }

    protected final void loopBack(final int fallNumber, final Object drop) {

        mUpRiver.push(fallNumber, drop);
    }

    protected final void loopBack(final Object drop) {

        mUpRiver.push(drop);
    }

    protected final void loopBackAfter(final long delay, final TimeUnit timeUnit,
            final int fallNumber, final Iterable<Object> drops) {

        mUpRiver.pushAfter(delay, timeUnit, fallNumber, drops);
    }

    protected final void loopBackAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<Object> drops) {

        mUpRiver.pushAfter(delay, timeUnit, drops);
    }

    protected final void loopBackAfter(final long delay, final TimeUnit timeUnit,
            final int fallNumber, final Object... drops) {

        mUpRiver.pushAfter(delay, timeUnit, fallNumber, drops);
    }

    protected final void loopBackAfter(final long delay, final TimeUnit timeUnit,
            final Object... drops) {

        mUpRiver.pushAfter(delay, timeUnit, drops);
    }

    protected final void loopBackAfter(final long delay, final TimeUnit timeUnit,
            final int fallNumber, final Object drop) {

        mUpRiver.pushAfter(delay, timeUnit, fallNumber, drop);
    }

    protected final void loopBackAfter(final long delay, final TimeUnit timeUnit,
            final Object drop) {

        mUpRiver.pushAfter(delay, timeUnit, drop);
    }

    protected final void push(final int streamNumber, final Iterable<Object> drops) {

        mDownRiver.push(streamNumber, drops);
    }

    protected final void push(final Iterable<Object> drops) {

        mDownRiver.push(drops);
    }

    protected final void push(final int streamNumber, final Object... drops) {

        mDownRiver.push(streamNumber, drops);
    }

    protected final void push(final Object... drops) {

        mDownRiver.push(drops);
    }

    protected final void push(final int streamNumber, final Object drop) {

        mDownRiver.push(streamNumber, drop);
    }

    protected final void push(final Object drop) {

        mDownRiver.push(drop);
    }

    protected final void pushAfter(final long delay, final TimeUnit timeUnit,
            final int streamNumber, final Iterable<Object> drops) {

        mDownRiver.pushAfter(delay, timeUnit, streamNumber, drops);
    }

    protected final void pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<Object> drops) {

        mDownRiver.pushAfter(delay, timeUnit, drops);
    }

    protected final void pushAfter(final long delay, final TimeUnit timeUnit,
            final int streamNumber, final Object... drops) {

        mDownRiver.pushAfter(delay, timeUnit, streamNumber, drops);
    }

    protected final void pushAfter(final long delay, final TimeUnit timeUnit,
            final Object... drops) {

        mDownRiver.pushAfter(delay, timeUnit, drops);
    }

    protected final void pushAfter(final long delay, final TimeUnit timeUnit,
            final int streamNumber, final Object drop) {

        mDownRiver.pushAfter(delay, timeUnit, streamNumber, drop);
    }

    protected final void pushAfter(final long delay, final TimeUnit timeUnit, final Object drop) {

        mDownRiver.pushAfter(delay, timeUnit, drop);
    }

    protected final River<SOURCE, Object> river(final boolean downStream) {

        return (downStream) ? mDownRiver : mUpRiver;
    }

    protected final int size(final boolean downStream) {

        return river(downStream).size();
    }

    protected final River<SOURCE, SOURCE> source() {

        return mUpRiver.source();
    }

    protected final <TYPE> Gate<TYPE> when(Class<TYPE> gateType) {

        return mUpRiver.when(gateType);
    }

    protected final <TYPE> Gate<TYPE> when(Classification<TYPE> gateClassification) {

        return mUpRiver.when(gateClassification);
    }

    private void fillMethods() {

        final Class<?> objectClass = mTarget.getClass();

        final Method[] methods = objectClass.getMethods();

        fillOnData(methods);
        fillOnNull(methods);
        fillOnDischarge(methods);

        final Method[] declaredMethods = objectClass.getDeclaredMethods();

        fillOnData(declaredMethods);
        fillOnNull(declaredMethods);
        fillOnDischarge(declaredMethods);
    }

    private void fillOnData(final Method[] methods) {

        final boolean isAnnotatedOnly = mIsAnnotatedOnly;

        final HashMap<Class<?>, Method> methodMap = mMethodMap;

        for (final Method method : methods) {

            final Class<?>[] parameterTypes = method.getParameterTypes();

            final boolean isAnnotated = method.isAnnotationPresent(OnData.class);

            if (parameterTypes.length != 1) {

                if (isAnnotated) {

                    throw new IllegalArgumentException(
                            "invalid annotated method: " + method + "\nAn " + OnData.class
                                    .getSimpleName() + " method must take a single parameter"
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
                        "cannot override a method already handling data of type: " + parameterType
                                .getSimpleName()
                );
            }

            if (!method.isAccessible()) {

                method.setAccessible(true);
            }

            methodMap.put(parameterType, method);
        }
    }

    private void fillOnDischarge(final Method[] methods) {

        final boolean isAnnotatedOnly = mIsAnnotatedOnly;

        for (final Method method : methods) {

            final Class<?>[] parameterTypes = method.getParameterTypes();

            final boolean isAnnotated = method.isAnnotationPresent(OnDischarge.class);

            if (parameterTypes.length != 0) {

                if (isAnnotated) {

                    throw new IllegalArgumentException(
                            "invalid annotated method: " + method + "\nAn " + OnDischarge.class
                                    .getSimpleName() + " method must take no parameters"
                    );

                } else {

                    continue;
                }
            }

            if (!isAnnotated && (isAnnotatedOnly || !method.getName().equals("onDischarge"))) {

                continue;
            }

            final Method onDischarge = mOnDischarge;

            if ((onDischarge != null) && !onDischarge.equals(method)) {

                throw new IllegalArgumentException(
                        "cannot override a method already handling data discharge");
            }

            if (!method.isAccessible()) {

                method.setAccessible(true);
            }

            mOnDischarge = method;
        }
    }

    private void fillOnNull(final Method[] methods) {

        final boolean isAnnotatedOnly = mIsAnnotatedOnly;

        for (final Method method : methods) {

            final Class<?>[] parameterTypes = method.getParameterTypes();

            final boolean isAnnotated = method.isAnnotationPresent(OnNull.class);

            if (parameterTypes.length != 0) {

                if (isAnnotated) {

                    throw new IllegalArgumentException(
                            "invalid annotated method: " + method + "\nAn " + OnData.class
                                    .getSimpleName() + " method must take a single parameter"
                    );

                } else {

                    continue;
                }
            }

            if (!isAnnotated && (isAnnotatedOnly || !method.getName().equals("onNull"))) {

                continue;
            }

            final Method onNull = mOnNull;

            if ((onNull != null) && !onNull.equals(method)) {

                throw new IllegalArgumentException(
                        "cannot override a method already handling null data");
            }

            if (!method.isAccessible()) {

                method.setAccessible(true);
            }

            mOnNull = method;
        }
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
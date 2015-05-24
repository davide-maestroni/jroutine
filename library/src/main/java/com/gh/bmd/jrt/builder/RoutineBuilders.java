/*
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
package com.gh.bmd.jrt.builder;

import com.gh.bmd.jrt.annotation.Input;
import com.gh.bmd.jrt.annotation.Input.InputMode;
import com.gh.bmd.jrt.annotation.Inputs;
import com.gh.bmd.jrt.annotation.Param;
import com.gh.bmd.jrt.annotation.Param.PassMode;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.common.Reflection.boxingClass;

/**
 * Utility class used to manage cached objects shared by routine builders.
 * <p/>
 * Created by davide-maestroni on 3/23/15.
 */
public class RoutineBuilders {

    private static final WeakIdentityHashMap<Object, Map<String, Object>> sMutexCache =
            new WeakIdentityHashMap<Object, Map<String, Object>>();

    /**
     * Avoid direct instantiation.
     */
    protected RoutineBuilders() {

    }

    /**
     * TODO
     *
     * @param method
     * @return
     */
    @Nullable
    public static InputMode getInputMode(@Nonnull final Method method) {

        final Inputs methodAnnotation = method.getAnnotation(Inputs.class);

        if (methodAnnotation == null) {

            return null;
        }

        InputMode inputMode = methodAnnotation.mode();

        if (inputMode == InputMode.AUTO) {

            final Class<?>[] parameterTypes = methodAnnotation.value();

            if (parameterTypes.length == 1) {

                final Class<?> parameterType = parameterTypes[0];

                if (parameterType.isArray() || parameterType.isAssignableFrom(List.class)) {

                    inputMode = InputMode.COLLECTION;

                } else {

                    inputMode = InputMode.ELEMENT;
                }

            } else {

                inputMode = InputMode.VALUE;
            }

        } else if (inputMode == InputMode.COLLECTION) {

            final Class<?>[] parameterTypes = methodAnnotation.value();
            final Class<?> parameterType = parameterTypes[0];

            if (!parameterType.isArray() && !parameterType.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.COLLECTION
                                + " must be bound to an array or a superclass of "
                                + List.class.getCanonicalName());
            }

            if (parameterTypes.length > 1) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.COLLECTION +
                                " cannot be applied to a method taking " + parameterTypes.length
                                + " input parameters");
            }

        } else if (inputMode == InputMode.ELEMENT) {

            final Class<?>[] parameterTypes = methodAnnotation.value();

            if (parameterTypes.length > 1) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.ELEMENT +
                                " cannot be applied to a method taking " + parameterTypes.length
                                + " input parameters");
            }
        }

        return inputMode;
    }

    /**
     * Gets the async input mode associated to the specified method parameter.
     *
     * @param method the target method.
     * @param index  the index of the parameter.
     * @return the async input mode.
     */
    @Nullable
    public static InputMode getInputMode(@Nonnull final Method method, final int index) {

        Input inputAnnotation = null;
        final Annotation[][] annotations = method.getParameterAnnotations();

        for (final Annotation annotation : annotations[index]) {

            if (annotation.annotationType() == Input.class) {

                inputAnnotation = (Input) annotation;
                break;
            }
        }

        if (inputAnnotation == null) {

            return null;
        }

        InputMode inputMode = inputAnnotation.mode();
        final Class<?> paramClass = inputAnnotation.value();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Class<?> parameterType = parameterTypes[index];
        final int length = parameterTypes.length;
        final boolean isArray = parameterType.isArray();

        if (inputMode == InputMode.AUTO) {

            if (OutputChannel.class.isAssignableFrom(parameterType)) {

                if ((length == 1) && (paramClass.isArray() || paramClass.isAssignableFrom(
                        List.class))) {

                    inputMode = InputMode.COLLECTION;

                } else {

                    inputMode = InputMode.VALUE;
                }

            } else if (isArray || Iterable.class.isAssignableFrom(parameterType)) {

                if (isArray && !boxingClass(paramClass).isAssignableFrom(
                        boxingClass(parameterType.getComponentType()))) {

                    throw new IllegalArgumentException(
                            "[" + method + "] the async input array with mode " + InputMode.ELEMENT
                                    + " does not match the bound type: "
                                    + paramClass.getCanonicalName());
                }

                if (length > 1) {

                    throw new IllegalArgumentException(
                            "[" + method + "] an async input with mode " + InputMode.ELEMENT
                                    + " cannot be applied to a method taking " + length +
                                    " input parameters");

                }

                inputMode = InputMode.ELEMENT;

            } else {

                throw new IllegalArgumentException(
                        "[" + method + "] cannot automatically choose an "
                                + "input mode for an output of type: "
                                + parameterType.getCanonicalName());
            }

        } else if (inputMode == InputMode.VALUE) {

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.VALUE
                                + " must extends an " + OutputChannel.class.getCanonicalName());
            }

        } else if (inputMode == InputMode.COLLECTION) {

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.COLLECTION
                                + " must extends an " + OutputChannel.class.getCanonicalName());
            }

            if (!paramClass.isArray() && !paramClass.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.COLLECTION
                                + " must be bound to an array or a superclass of "
                                + List.class.getCanonicalName());
            }

            if (length > 1) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.COLLECTION +
                                " cannot be applied to a method taking " + length
                                + " input parameters");
            }

        } else { // InputMode.ELEMENT

            if (!isArray && !Iterable.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.ELEMENT
                                + " must be an array or implement an "
                                + Iterable.class.getCanonicalName());
            }

            if (isArray && !boxingClass(paramClass).isAssignableFrom(
                    boxingClass(parameterType.getComponentType()))) {

                throw new IllegalArgumentException(
                        "[" + method + "] the async input array with mode " + InputMode.ELEMENT
                                + " does not match the bound type: "
                                + paramClass.getCanonicalName());
            }

            if (length > 1) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with mode " + InputMode.ELEMENT
                                + " cannot be applied to a method taking " + length
                                + " input parameters");
            }
        }

        return inputMode;
    }

    /**
     * Gets the async pass mode of the return type of the specified method.
     *
     * @param method the target method.
     * @return the async pass mode.
     */
    @Nullable
    public static PassMode getReturnMode(@Nonnull final Method method) {

        final Param annotation = method.getAnnotation(Param.class);

        if (annotation == null) {

            return null;
        }

        final Class<?> returnType = method.getReturnType();
        PassMode passMode = annotation.mode();

        if (passMode == PassMode.AUTO) {

            if (returnType.isArray() || returnType.isAssignableFrom(List.class)) {

                final Class<?> returnClass = annotation.value();

                if (returnType.isArray() && !boxingClass(
                        returnType.getComponentType()).isAssignableFrom(boxingClass(returnClass))) {

                    throw new IllegalArgumentException(
                            "[" + method + "] the async output array with pass mode "
                                    + PassMode.PARALLEL + " does not match the bound type: "
                                    + returnClass.getCanonicalName());
                }

                passMode = PassMode.PARALLEL;

            } else if (returnType.isAssignableFrom(OutputChannel.class)) {

                final Class<?> returnClass = annotation.value();

                if (returnClass.isArray() || Iterable.class.isAssignableFrom(returnClass)) {

                    passMode = PassMode.COLLECTION;

                } else {

                    passMode = PassMode.VALUE;
                }

            } else {

                throw new IllegalArgumentException("[" + method + "] cannot automatically choose a "
                                                           + "pass mode for an input of type: "
                                                           + returnType.getCanonicalName());
            }

        } else if (passMode == PassMode.VALUE) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException(
                        "[" + method + "] an async output with pass mode " + PassMode.VALUE
                                + " must be a superclass of " + channelClassName);
            }

        } else if (passMode == PassMode.COLLECTION) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException(
                        "[" + method + "] an async output with pass mode " + PassMode.VALUE
                                + " must be a superclass of " + channelClassName);
            }

            final Class<?> returnClass = annotation.value();

            if (!returnClass.isArray() && !Iterable.class.isAssignableFrom(returnClass)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async output with pass mode " + PassMode.COLLECTION
                                + " must be bound to an array or a type implementing an "
                                + Iterable.class.getCanonicalName());
            }

        } else { // PassMode.PARALLEL

            if (!returnType.isArray() && !returnType.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async output with pass mode " + PassMode.PARALLEL
                                + " must be an array or a superclass of "
                                + List.class.getCanonicalName());
            }

            final Class<?> returnClass = annotation.value();

            if (returnType.isArray() && !boxingClass(
                    returnType.getComponentType()).isAssignableFrom(boxingClass(returnClass))) {

                throw new IllegalArgumentException(
                        "[" + method + "] the async output array with pass mode "
                                + PassMode.PARALLEL + " does not match the bound type: "
                                + returnClass.getCanonicalName());
            }
        }

        return passMode;
    }

    /**
     * Returns the cached mutex associated with the specified target and share group.<br/>
     * If the cache was empty, it is filled with a new object automatically created.
     *
     * @param target     the target object instance.
     * @param shareGroup the share group name.
     * @return the cached mutex.
     */
    @Nonnull
    public static Object getSharedMutex(@Nonnull final Object target,
            @Nullable final String shareGroup) {

        synchronized (sMutexCache) {

            final WeakIdentityHashMap<Object, Map<String, Object>> mutexCache = sMutexCache;
            Map<String, Object> mutexMap = mutexCache.get(target);

            if (mutexMap == null) {

                mutexMap = new HashMap<String, Object>();
                mutexCache.put(target, mutexMap);
            }

            final String groupName = (shareGroup != null) ? shareGroup : ShareGroup.ALL;
            Object mutex = mutexMap.get(groupName);

            if (mutex == null) {

                mutex = new Object();
                mutexMap.put(groupName, mutex);
            }

            return mutex;
        }
    }
}

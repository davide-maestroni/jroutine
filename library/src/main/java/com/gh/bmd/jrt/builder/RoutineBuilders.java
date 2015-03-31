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

import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Pass.ParamMode;
import com.gh.bmd.jrt.annotation.Share;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;

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
 * Created by davide on 3/23/15.
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
     * @param passAnnotation
     * @param parameterType
     * @param length
     * @return
     */
    @Nonnull
    public static ParamMode getParamMode(@Nonnull final Method method,
            @Nonnull final Pass passAnnotation, @Nonnull final Class<?> parameterType,
            final int length) {

        ParamMode paramMode = passAnnotation.mode();
        final Class<?> paramClass = passAnnotation.value();
        final boolean isArray = parameterType.isArray();

        if (paramMode == ParamMode.AUTO) {

            if (OutputChannel.class.isAssignableFrom(parameterType)) {

                if ((length == 1) && (paramClass.isArray() || paramClass.isAssignableFrom(
                        List.class))) {

                    paramMode = ParamMode.COLLECTION;

                } else {

                    paramMode = ParamMode.OBJECT;
                }

            } else if (isArray || Iterable.class.isAssignableFrom(parameterType)) {

                if (isArray && !boxingClass(paramClass).isAssignableFrom(
                        boxingClass(parameterType.getComponentType()))) {

                    throw new IllegalArgumentException(
                            "[" + method + "] the async input array with param mode "
                                    + ParamMode.PARALLEL + " does not match the bound type: "
                                    + paramClass.getCanonicalName());
                }

                if (length > 1) {

                    throw new IllegalArgumentException(
                            "[" + method + "] an async input with param mode " + ParamMode.PARALLEL
                                    + " cannot be applied to a method taking " + length +
                                    " input parameters");

                }

                paramMode = ParamMode.PARALLEL;

            } else {

                throw new IllegalArgumentException("[" + method + "] cannot automatically choose a "
                                                           + "param mode for an output of type: "
                                                           + parameterType.getCanonicalName());
            }

        } else if (paramMode == ParamMode.OBJECT) {

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with param mode " + ParamMode.OBJECT
                                + " must extends an " + OutputChannel.class.getCanonicalName());
            }

        } else if (paramMode == ParamMode.COLLECTION) {

            if (!OutputChannel.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with param mode " + ParamMode.COLLECTION
                                + " must extends an " + OutputChannel.class.getCanonicalName());
            }

            if (!paramClass.isArray() && !paramClass.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with param mode " + ParamMode.COLLECTION
                                + " must be bound to an array or a superclass of "
                                + List.class.getCanonicalName());
            }

            if (length > 1) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with param mode " + ParamMode.COLLECTION +
                                " cannot be applied to a method taking " + length
                                + " input parameters");
            }

        } else { // ParamMode.PARALLEL

            if (!isArray && !Iterable.class.isAssignableFrom(parameterType)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with param mode " + ParamMode.PARALLEL
                                + " must be an array or implement an "
                                + Iterable.class.getCanonicalName());
            }

            if (isArray && !boxingClass(paramClass).isAssignableFrom(
                    boxingClass(parameterType.getComponentType()))) {

                throw new IllegalArgumentException(
                        "[" + method + "] the async input array with param mode "
                                + ParamMode.PARALLEL + " does not match the bound type: "
                                + paramClass.getCanonicalName());
            }

            if (length > 1) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async input with param mode " + ParamMode.PARALLEL
                                + " cannot be applied to a method taking " + length
                                + " input parameters");
            }
        }

        return paramMode;
    }

    /**
     * TODO
     *
     * @param method
     * @return
     */
    @Nullable
    public static ParamMode getReturnMode(@Nonnull final Method method) {

        final Pass annotation = method.getAnnotation(Pass.class);

        if (annotation == null) {

            return null;
        }

        final Class<?> returnType = method.getReturnType();
        ParamMode paramMode = annotation.mode();

        if (paramMode == ParamMode.AUTO) {

            if (returnType.isArray() || returnType.isAssignableFrom(List.class)) {

                final Class<?> returnClass = annotation.value();

                if (returnType.isArray() && !boxingClass(
                        returnType.getComponentType()).isAssignableFrom(boxingClass(returnClass))) {

                    throw new IllegalArgumentException(
                            "[" + method + "] the async output array with param mode "
                                    + ParamMode.PARALLEL + " does not match the bound type: "
                                    + returnClass.getCanonicalName());
                }

                paramMode = ParamMode.PARALLEL;

            } else if (returnType.isAssignableFrom(OutputChannel.class)) {

                final Class<?> returnClass = annotation.value();

                if (returnClass.isArray() || Iterable.class.isAssignableFrom(returnClass)) {

                    paramMode = ParamMode.COLLECTION;

                } else {

                    paramMode = ParamMode.OBJECT;
                }

            } else {

                throw new IllegalArgumentException("[" + method + "] cannot automatically choose a "
                                                           + "param mode for an input of type: "
                                                           + returnType.getCanonicalName());
            }

        } else if (paramMode == ParamMode.OBJECT) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException(
                        "[" + method + "] an async output with param mode " + ParamMode.OBJECT
                                + " must be a superclass of " + channelClassName);
            }

        } else if (paramMode == ParamMode.COLLECTION) {

            if (!returnType.isAssignableFrom(OutputChannel.class)) {

                final String channelClassName = OutputChannel.class.getCanonicalName();
                throw new IllegalArgumentException(
                        "[" + method + "] an async output with param mode " + ParamMode.OBJECT
                                + " must be a superclass of " + channelClassName);
            }

            final Class<?> returnClass = annotation.value();

            if (!returnClass.isArray() && !Iterable.class.isAssignableFrom(returnClass)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async output with param mode " + ParamMode.COLLECTION
                                + " must be bound to an array or a type implementing an "
                                + Iterable.class.getCanonicalName());
            }

        } else { // ParamMode.PARALLEL

            if (!returnType.isArray() && !returnType.isAssignableFrom(List.class)) {

                throw new IllegalArgumentException(
                        "[" + method + "] an async output with param mode " + ParamMode.PARALLEL
                                + " must be an array or a superclass " +
                                "of " + List.class.getCanonicalName());
            }

            final Class<?> returnClass = annotation.value();

            if (returnType.isArray() && !boxingClass(
                    returnType.getComponentType()).isAssignableFrom(boxingClass(returnClass))) {

                throw new IllegalArgumentException(
                        "[" + method + "] the async output array with param mode "
                                + ParamMode.PARALLEL + " does not match the bound type: "
                                + returnClass.getCanonicalName());
            }
        }

        return paramMode;
    }

    /**
     * Returns the cached mutex associated with the specified target and share group.<br/>
     * If the cache was empty, it is filled with a new object automatically created.
     *
     * @param target     the target object instance.
     * @param shareGroup the share group name.
     * @return the cached mutex.
     * @throws java.lang.NullPointerException if the specified target or group name are null.
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

            final String groupName = (shareGroup != null) ? shareGroup : Share.ALL;
            Object mutex = mutexMap.get(groupName);

            if (mutex == null) {

                mutex = new Object();
                mutexMap.put(groupName, mutex);
            }

            return mutex;
        }
    }
}

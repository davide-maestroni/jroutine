/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.builder;

import com.gh.bmd.jrt.annotation.Share;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;
import com.gh.bmd.jrt.routine.Routine;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class used to manage cached objects shared by routine builders.
 * <p/>
 * Created by davide on 3/23/15.
 */
public class RoutineBuilders {

    private static final Initializer<Object> DEFAULT_MUTEX_INITIALIZER = new Initializer<Object>() {

        @Nonnull
        public Object initialValue() {

            return new Object();
        }
    };

    private static final WeakIdentityHashMap<Object, HashMap<Method, Method>> sMethodCache =
            new WeakIdentityHashMap<Object, HashMap<Method, Method>>();

    private static final WeakIdentityHashMap<Object, Map<String, Object>> sMutexCache =
            new WeakIdentityHashMap<Object, Map<String, Object>>();

    private static final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>
            sRoutineCache = new WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>();

    /**
     * Avoid direct instantiation.
     */
    protected RoutineBuilders() {

    }

    /**
     * Returns the cached routine associated with the specified target and routine info.
     *
     * @param target   the target object instance.
     * @param info     the routine info.
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     * @return the cached routine or null.
     * @throws java.lang.NullPointerException if the specified target or info are null.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <INPUT, OUTPUT> Routine<INPUT, OUTPUT> getMethodRoutine(
            @Nonnull final Object target, @Nonnull final RoutineInfo info) {

        synchronized (sRoutineCache) {

            final HashMap<RoutineInfo, Routine<?, ?>> routineMap = sRoutineCache.get(target);
            return (routineMap != null) ? (Routine<INPUT, OUTPUT>) routineMap.get(info) : null;
        }
    }

    /**
     * Returns the cached routine associated with the specified target and routine info.<br/>
     * If the cache was empty, it is filled with the object returned by the specified initializer.
     *
     * @param target      the target object instance.
     * @param info        the routine info.
     * @param initializer the routine initializer.
     * @param <INPUT>     the input data type.
     * @param <OUTPUT>    the output data type.
     * @return the cached routine.
     * @throws java.lang.NullPointerException if the specified target, info or initializer are null.
     */
    @Nonnull
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static <INPUT, OUTPUT> Routine<INPUT, OUTPUT> getMethodRoutine(
            @Nonnull final Object target, @Nonnull final RoutineInfo info,
            @Nonnull final Initializer<Routine<INPUT, OUTPUT>> initializer) {

        synchronized (sRoutineCache) {

            final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>> routineCache =
                    sRoutineCache;
            HashMap<RoutineInfo, Routine<?, ?>> routineMap = routineCache.get(target);

            if (routineMap == null) {

                routineMap = new HashMap<RoutineInfo, Routine<?, ?>>();
                routineCache.put(target, routineMap);
            }

            Routine<INPUT, OUTPUT> routine = (Routine<INPUT, OUTPUT>) routineMap.get(info);

            if (routine == null) {

                routine = initializer.initialValue();

                if (routine == null) {

                    throw new NullPointerException("the initialized routine must not be null");
                }

                routineMap.put(info, routine);
            }

            return routine;
        }
    }

    /**
     * Returns the cached target method associated with the specified target and proxy method.
     *
     * @param target      the target object instance.
     * @param proxyMethod the proxy method.
     * @return the cached target method or null.
     * @throws java.lang.NullPointerException if the specified target or proxy method are null.
     */
    @Nullable
    public static Method getProxiedMethod(@Nonnull final Object target,
            @Nonnull final Method proxyMethod) {

        synchronized (sMethodCache) {

            final HashMap<Method, Method> methodMap = sMethodCache.get(target);
            return (methodMap != null) ? methodMap.get(proxyMethod) : null;
        }
    }

    /**
     * Returns the cached target method associated with the specified target and proxy method.<br/>
     * If the cache was empty, it is filled with the object returned by the specified initializer.
     *
     * @param target      the target object instance.
     * @param proxyMethod the proxy method.
     * @param initializer the method initializer.
     * @return the cached target method.
     * @throws java.lang.NullPointerException if the specified target, method or initializer are
     *                                        null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public static Method getProxiedMethod(@Nonnull final Object target,
            @Nonnull final Method proxyMethod, @Nonnull final Initializer<Method> initializer) {

        synchronized (sMethodCache) {

            final WeakIdentityHashMap<Object, HashMap<Method, Method>> methodCache = sMethodCache;
            HashMap<Method, Method> methodMap = methodCache.get(target);

            if (methodMap == null) {

                methodMap = new HashMap<Method, Method>();
                methodCache.put(target, methodMap);
            }

            Method method = methodMap.get(proxyMethod);

            if (method == null) {

                method = initializer.initialValue();

                if (method == null) {

                    throw new NullPointerException("the initialized method must not be null");
                }

                methodMap.put(proxyMethod, method);
            }

            return method;
        }
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

        return getSharedMutex(target, shareGroup, DEFAULT_MUTEX_INITIALIZER);
    }

    /**
     * Returns the cached mutex associated with the specified target and share group.<br/>
     * If the cache was empty, it is filled with the object returned by the specified initializer.
     *
     * @param target      the target object instance.
     * @param shareGroup  the share group name.
     * @param initializer the mutex initializer.
     * @return the cached mutex.
     * @throws java.lang.NullPointerException if the specified target, group name or initializer are
     *                                        null.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public static Object getSharedMutex(@Nonnull final Object target,
            @Nullable final String shareGroup, @Nonnull final Initializer<Object> initializer) {

        synchronized (sMethodCache) {

            final WeakIdentityHashMap<Object, Map<String, Object>> mutexCache = sMutexCache;
            Map<String, Object> mutexMap = mutexCache.get(target);

            if (mutexMap == null) {

                mutexMap = new HashMap<String, Object>();
                mutexCache.put(target, mutexMap);
            }

            final String groupName = (shareGroup != null) ? shareGroup : Share.ALL;
            Object mutex = mutexMap.get(groupName);

            if (mutex == null) {

                mutex = initializer.initialValue();

                if (mutex == null) {

                    throw new NullPointerException("the initialized mutex must not be null");
                }

                mutexMap.put(groupName, mutex);
            }

            return mutex;
        }
    }

    /**
     * Interface defining an object initializer.
     *
     * @param <TYPE> the object type.
     */
    public interface Initializer<TYPE> {

        /**
         * Returns the initial object value.
         *
         * @return the object value.
         */
        @Nonnull
        TYPE initialValue();
    }

    /**
     * Class used as key to identify a specific routine instance.
     */
    public static final class RoutineInfo {

        private final RoutineConfiguration mConfiguration;

        private final boolean mIsCollectParam;

        private final boolean mIsCollectResult;

        private final Method mMethod;

        private final String mShareGroup;

        /**
         * Constructor.
         *
         * @param configuration   the routine configuration.
         * @param method          the method to wrap.
         * @param shareGroup      the group name.
         * @param isCollectParam  whether we need to collect the input parameters.
         * @param isCollectResult whether the output is a collection.
         */
        public RoutineInfo(@Nonnull final RoutineConfiguration configuration,
                @Nonnull final Method method, @Nonnull final String shareGroup,
                final boolean isCollectParam, final boolean isCollectResult) {

            mMethod = method;
            mShareGroup = shareGroup;
            mConfiguration = configuration;
            mIsCollectParam = isCollectParam;
            mIsCollectResult = isCollectResult;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + (mIsCollectParam ? 1 : 0);
            result = 31 * result + (mIsCollectResult ? 1 : 0);
            result = 31 * result + mMethod.hashCode();
            result = 31 * result + mShareGroup.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof RoutineInfo)) {

                return false;
            }

            final RoutineInfo that = (RoutineInfo) o;

            return mIsCollectParam == that.mIsCollectParam
                    && mIsCollectResult == that.mIsCollectResult && mConfiguration.equals(
                    that.mConfiguration) && mMethod.equals(that.mMethod) && mShareGroup.equals(
                    that.mShareGroup);
        }
    }
}

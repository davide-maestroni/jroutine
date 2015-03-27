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

import com.gh.bmd.jrt.annotation.Share;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;

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

    private static final WeakIdentityHashMap<Object, Map<String, Object>> sMutexCache =
            new WeakIdentityHashMap<Object, Map<String, Object>>();

    /**
     * Avoid direct instantiation.
     */
    protected RoutineBuilders() {

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

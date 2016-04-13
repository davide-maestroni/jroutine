/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.object;

import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.object.builder.FactoryContext;
import com.github.dm.jrt.core.util.DeepEqualObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.findConstructor;

/**
 * Test service.
 * <p>
 * Created by davide-maestroni on 03/12/2016.
 */
public class RemoteTestService extends InvocationService implements FactoryContext {

    private final HashMap<InstanceInfo, Object> mInstances = new HashMap<InstanceInfo, Object>();

    @Nullable
    @SuppressWarnings("unchecked")
    public <TYPE> TYPE geInstance(@NotNull final Class<? extends TYPE> type,
            @NotNull final Object[] args) throws Exception {

        final HashMap<InstanceInfo, Object> instances = mInstances;
        final InstanceInfo instanceInfo = new InstanceInfo(type, args);
        Object instance = instances.get(instanceInfo);
        if (instance == null) {
            instance = findConstructor(type, args).newInstance(args);
            instances.put(instanceInfo, instance);
        }

        return (TYPE) instance;
    }

    private static class InstanceInfo extends DeepEqualObject {

        private InstanceInfo(@NotNull final Class<?> type, @NotNull final Object[] args) {

            super(asArgs(type, args));
        }
    }
}

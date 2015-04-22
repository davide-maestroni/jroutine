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
package com.gh.bmd.jrt.android.core;

import com.gh.bmd.jrt.android.builder.FactoryContext;
import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.RoutineException;

import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Test service.
 * <p/>
 * Created by davide on 1/16/15.
 */
public class TestService extends RoutineService implements FactoryContext {

    private static final HashMap<Class<?>, Object> sInstanceMap = new HashMap<Class<?>, Object>();

    @Nullable
    @SuppressWarnings("unchecked")
    public <TYPE> TYPE geInstance(@Nonnull final Class<? extends TYPE> type,
            @Nonnull final Object[] args) {

        final HashMap<Class<?>, Object> instanceMap = sInstanceMap;
        Object instance = instanceMap.get(type);

        if (instance == null) {

            try {

                instance = findConstructor(type, args).newInstance(args);
                instanceMap.put(type, instance);

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }

        return (TYPE) instance;
    }
}

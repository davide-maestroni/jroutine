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
package com.github.dm.jrt.android.app;

import android.app.Application;

import com.github.dm.jrt.android.builder.FactoryContext;
import com.github.dm.jrt.invocation.InvocationException;

import java.util.Arrays;
import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.dm.jrt.util.Reflection.findConstructor;

/**
 * Test application.
 * <p/>
 * Created by davide-maestroni on 04/06/2015.
 */
@SuppressWarnings("unused")
public class TestApp extends Application implements FactoryContext {

    private final HashMap<InstanceInfo, Object> mInstances = new HashMap<InstanceInfo, Object>();

    @Nullable
    @SuppressWarnings("unchecked")
    public <TYPE> TYPE geInstance(@Nonnull final Class<? extends TYPE> type,
            @Nonnull final Object[] args) {

        final HashMap<InstanceInfo, Object> instances = mInstances;
        final InstanceInfo instanceInfo = new InstanceInfo(type, args);
        Object instance = instances.get(instanceInfo);

        if (instance == null) {

            try {

                instance = findConstructor(type, args).newInstance(args);
                instances.put(instanceInfo, instance);

            } catch (final Throwable t) {

                throw InvocationException.wrapIfNeeded(t);
            }
        }

        return (TYPE) instance;
    }

    private static class InstanceInfo {

        private final Object[] mArgs;

        private final Class<?> mType;

        private InstanceInfo(@Nonnull final Class<?> type, @Nonnull final Object[] args) {

            mType = type;
            mArgs = args;
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof InstanceInfo)) {

                return false;
            }

            final InstanceInfo that = (InstanceInfo) o;
            return Arrays.equals(mArgs, that.mArgs) && mType.equals(that.mType);
        }

        @Override
        public int hashCode() {

            int result = Arrays.hashCode(mArgs);
            result = 31 * result + mType.hashCode();
            return result;
        }
    }
}
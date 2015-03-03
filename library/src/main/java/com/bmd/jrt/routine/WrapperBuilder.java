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
package com.bmd.jrt.routine;

import com.bmd.jrt.annotation.Share;
import com.bmd.jrt.builder.RoutineBuilder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.common.WeakIdentityHashMap;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.routine.ClassRoutineBuilder.sMutexCache;

/**
 * Builder of async wrapper objects.
 * <p/>
 * Created by davide on 2/26/15.
 *
 * @param <CLASS>
 */
public abstract class WrapperBuilder<CLASS> implements RoutineBuilder {

    private static final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> sClassMap =
            new WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>>();

    private RoutineConfiguration mConfiguration;

    private String mShareGroup;

    /**
     * Returns a wrapper object enabling asynchronous calling of the target instance methods.
     * <p/>
     * The routines used for calling the methods will honor the attributes specified in any
     * optional {@link com.bmd.jrt.annotation.Bind}, {@link com.bmd.jrt.annotation.Timeout} and
     * {@link com.bmd.jrt.annotation.Pass} annotations.<br/>
     * Note that such annotations will override any configuration set through the builder.
     * <p/>
     * The wrapping object is created through code generation based on the interfaces annotated
     * with {@link com.bmd.jrt.annotation.Wrap}.<br/>
     * Note that, you'll need to enable annotation pre-processing by adding the processor package
     * to the specific project dependencies.
     *
     * @return the wrapping object.
     */
    @Nonnull
    public CLASS buildWrapper() {

        synchronized (sClassMap) {

            final Object target = getTarget();
            final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> classMap = sClassMap;
            HashMap<ClassInfo, Object> classes = classMap.get(target);

            if (classes == null) {

                classes = new HashMap<ClassInfo, Object>();
                classMap.put(target, classes);
            }

            final String shareGroup = mShareGroup;
            final String classShareGroup = (shareGroup != null) ? shareGroup : Share.ALL;
            final RoutineConfiguration configuration = RoutineConfiguration.notNull(mConfiguration);
            final Class<CLASS> itf = getWrapperClass();
            final ClassInfo classInfo = new ClassInfo(itf, configuration, classShareGroup);
            final Object instance = classes.get(classInfo);

            if (instance != null) {

                return itf.cast(instance);
            }

            try {

                final CLASS newInstance = newWrapper(sMutexCache, classShareGroup, configuration);
                classes.put(classInfo, newInstance);
                return newInstance;

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }

    /**
     * Note that all the options related to the output and input channels will be ignored.
     *
     * @param configuration the routine configuration.
     * @return this builder.
     */
    @Nonnull
    @Override
    public WrapperBuilder<CLASS> withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }

    /**
     * Tells the builder to create a routine using the specified share tag.
     *
     * @param group the group name.
     * @return this builder.
     * @see com.bmd.jrt.annotation.Share
     */
    @Nonnull
    public WrapperBuilder<CLASS> withShareGroup(@Nullable final String group) {

        mShareGroup = group;
        return this;
    }

    /**
     * Returns the builder target object.
     *
     * @return the target object.
     */
    @Nonnull
    protected abstract Object getTarget();

    /**
     * Returns the builder wrapper class.
     *
     * @return the wrapper class.
     */
    @Nonnull
    protected abstract Class<CLASS> getWrapperClass();

    /**
     * Creates and return a new wrapper instance.
     *
     * @param mutexMap      the map of mutexes used to synchronize the method invocations.
     * @param shareGroup    the share group name.
     * @param configuration the routine configuration.
     * @return the wrapper instance.
     */
    @Nonnull
    protected abstract CLASS newWrapper(
            @Nonnull final WeakIdentityHashMap<Object, Map<String, Object>> mutexMap,
            @Nonnull final String shareGroup, @Nonnull final RoutineConfiguration configuration);

    /**
     * Class used as key to identify a specific wrapper instance.
     */
    private static class ClassInfo {

        private final RoutineConfiguration mConfiguration;

        private final Class<?> mItf;

        private final String mShareGroup;

        /**
         * Constructor.
         *
         * @param itf           the wrapper interface.
         * @param configuration the routine configuration.
         * @param shareGroup    the share group name.
         */
        private ClassInfo(@Nonnull final Class<?> itf,
                @Nonnull final RoutineConfiguration configuration,
                @Nonnull final String shareGroup) {

            mItf = itf;
            mConfiguration = configuration;
            mShareGroup = shareGroup;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + mItf.hashCode();
            result = 31 * result + mShareGroup.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            // auto-generated code
            if (this == o) {

                return true;
            }

            if (!(o instanceof ClassInfo)) {

                return false;
            }

            final ClassInfo that = (ClassInfo) o;
            return mConfiguration.equals(that.mConfiguration) && mItf.equals(that.mItf)
                    && mShareGroup.equals(that.mShareGroup);
        }
    }
}

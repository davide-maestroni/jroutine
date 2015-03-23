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
package com.gh.bmd.jrt.processor.routine;

import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.common.ClassToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Default implementation of a wrapper builder.
 * <p/>
 * Created by davide on 3/23/15.
 */
class DefaultWrapperRoutineBuilder implements WrapperRoutineBuilder {

    private final WeakReference<?> mTargetReference;

    private RoutineConfiguration mConfiguration;

    private String mShareGroup;

    /**
     * Constructor.
     *
     * @param target the target object.
     * @throws java.lang.IllegalArgumentException if a duplicate name in the annotations is
     *                                            detected.
     * @throws java.lang.NullPointerException     if the specified target is null.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultWrapperRoutineBuilder(@Nonnull final Object target) {

        if (target == null) {

            throw new IllegalStateException("the target object must not be null");
        }

        mTargetReference = new WeakReference<Object>(target);
    }

    @Nonnull
    public <TYPE> TYPE buildWrapper(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getCanonicalName());
        }

        final Object target = mTargetReference.get();

        if (target == null) {

            throw new IllegalStateException("target object has been destroyed");
        }

        final RoutineConfiguration configuration = mConfiguration;
        final ObjectWrapperBuilder<TYPE> builder = new ObjectWrapperBuilder<TYPE>(target, itf);

        if (configuration != null) {

            builder.withConfiguration(configuration);
        }

        return builder.withShareGroup(mShareGroup).buildWrapper();
    }

    @Nonnull
    public <TYPE> TYPE buildWrapper(@Nonnull final ClassToken<TYPE> itf) {

        return buildWrapper(itf.getRawClass());
    }

    @Nonnull
    public WrapperRoutineBuilder withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }

    @Nonnull
    public WrapperRoutineBuilder withShareGroup(@Nullable final String group) {

        mShareGroup = group;
        return this;
    }

    /**
     * Wrapper builder implementation.
     *
     * @param <TYPE> the interface type.
     */
    private static class ObjectWrapperBuilder<TYPE> extends AbstractWrapperBuilder<TYPE> {

        private final Object mTarget;

        private final Class<TYPE> mWrapperClass;

        /**
         * Constructor.
         *
         * @param target       the target object instance.
         * @param wrapperClass the wrapper class.
         */
        private ObjectWrapperBuilder(@Nonnull final Object target,
                @Nonnull final Class<TYPE> wrapperClass) {

            mTarget = target;
            mWrapperClass = wrapperClass;
        }

        @Nonnull
        @Override
        protected Object getTarget() {

            return mTarget;
        }

        @Nonnull
        @Override
        protected Class<TYPE> getWrapperClass() {

            return mWrapperClass;
        }

        @Nonnull
        @Override
        protected TYPE newWrapper(@Nonnull final String shareGroup,
                @Nonnull final RoutineConfiguration configuration) {

            try {

                final Object target = mTarget;
                final Class<TYPE> wrapperClass = mWrapperClass;
                final Package classPackage = wrapperClass.getPackage();
                final String packageName =
                        (classPackage != null) ? classPackage.getName() + "." : "";
                final String className = packageName + "JRoutine_" + wrapperClass.getSimpleName();
                final Constructor<?> constructor =
                        findConstructor(Class.forName(className), target, shareGroup,
                                        configuration);
                return wrapperClass.cast(
                        constructor.newInstance(target, shareGroup, configuration));

            } catch (final InstantiationException e) {

                throw new IllegalArgumentException(e.getCause());

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }
}

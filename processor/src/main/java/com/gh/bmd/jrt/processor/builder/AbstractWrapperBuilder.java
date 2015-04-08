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
package com.gh.bmd.jrt.processor.builder;

import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.reflect.Type;
import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Abstract implementation of a builder of async wrapper objects.
 * <p/>
 * Created by davide on 2/26/15.
 *
 * @param <TYPE> the interface type.
 */
public abstract class AbstractWrapperBuilder<TYPE> implements WrapperBuilder<TYPE> {

    private static final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> sClassMap =
            new WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>>();

    private RoutineConfiguration mConfiguration;

    private String mShareGroup;

    @Nonnull
    public TYPE buildWrapper() {

        synchronized (sClassMap) {

            final Object target = getTarget();
            final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> classMap = sClassMap;
            HashMap<ClassInfo, Object> classes = classMap.get(target);

            if (classes == null) {

                classes = new HashMap<ClassInfo, Object>();
                classMap.put(target, classes);
            }

            final String shareGroup = mShareGroup;
            final String classShareGroup = (shareGroup != null) ? shareGroup : ShareGroup.ALL;
            final RoutineConfiguration configuration = RoutineConfiguration.notNull(mConfiguration);
            final ClassToken<TYPE> token = getInterfaceToken();
            final ClassInfo classInfo = new ClassInfo(token, configuration, classShareGroup);
            final Object instance = classes.get(classInfo);

            if (instance != null) {

                return token.cast(instance);
            }

            warn(configuration);

            try {

                final TYPE newInstance = newWrapper(classShareGroup, configuration);
                classes.put(classInfo, newInstance);
                return newInstance;

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }

    @Nonnull
    public WrapperBuilder<TYPE> withConfiguration(
            @Nullable final RoutineConfiguration configuration) {

        mConfiguration = configuration;
        return this;
    }

    @Nonnull
    public WrapperBuilder<TYPE> withShareGroup(@Nullable final String group) {

        mShareGroup = group;
        return this;
    }

    /**
     * Returns the builder wrapper class token.
     *
     * @return the wrapper class token.
     */
    @Nonnull
    protected abstract ClassToken<TYPE> getInterfaceToken();

    /**
     * Returns the builder target object.
     *
     * @return the target object.
     */
    @Nonnull
    protected abstract Object getTarget();

    /**
     * Creates and return a new wrapper instance.
     *
     * @param shareGroup    the share group name.
     * @param configuration the routine configuration.
     * @return the wrapper instance.
     */
    @Nonnull
    protected abstract TYPE newWrapper(@Nonnull final String shareGroup,
            @Nonnull final RoutineConfiguration configuration);

    /**
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param configuration the routine configuration.
     */
    private void warn(@Nonnull final RoutineConfiguration configuration) {

        Logger logger = null;
        final OrderType inputOrder = configuration.getInputOrderOr(null);

        if (inputOrder != null) {

            logger = Logger.newLogger(configuration, this);
            logger.wrn("the specified input order will be ignored: %s", inputOrder);
        }

        final int inputSize = configuration.getInputSizeOr(RoutineConfiguration.DEFAULT);

        if (inputSize != RoutineConfiguration.DEFAULT) {

            if (logger == null) {

                logger = Logger.newLogger(configuration, this);
            }

            logger.wrn("the specified maximum input size will be ignored: %d", inputSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeoutOr(null);

        if (inputTimeout != null) {

            if (logger == null) {

                logger = Logger.newLogger(configuration, this);
            }

            logger.wrn("the specified input timeout will be ignored: %s", inputTimeout);
        }

        final OrderType outputOrder = configuration.getOutputOrderOr(null);

        if (outputOrder != null) {

            if (logger == null) {

                logger = Logger.newLogger(configuration, this);
            }

            logger.wrn("the specified output order will be ignored: %s", outputOrder);
        }

        final int outputSize = configuration.getOutputSizeOr(RoutineConfiguration.DEFAULT);

        if (outputSize != RoutineConfiguration.DEFAULT) {

            if (logger == null) {

                logger = Logger.newLogger(configuration, this);
            }

            logger.wrn("the specified maximum output size will be ignored: %d", outputSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeoutOr(null);

        if (outputTimeout != null) {

            if (logger == null) {

                logger = Logger.newLogger(configuration, this);
            }

            logger.wrn("the specified output timeout will be ignored: %s", outputTimeout);
        }
    }

    /**
     * Class used as key to identify a specific wrapper instance.
     */
    private static class ClassInfo {

        private final RoutineConfiguration mConfiguration;

        private final String mShareGroup;

        private final Type mType;

        /**
         * Constructor.
         *
         * @param token         the wrapper interface token.
         * @param configuration the routine configuration.
         * @param shareGroup    the share group name.
         */
        private ClassInfo(@Nonnull final ClassToken<?> token,
                @Nonnull final RoutineConfiguration configuration,
                @Nonnull final String shareGroup) {

            mType = token.getRawClass();
            mConfiguration = configuration;
            mShareGroup = shareGroup;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mConfiguration.hashCode();
            result = 31 * result + mType.hashCode();
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
            return mConfiguration.equals(that.mConfiguration) && mType.equals(that.mType)
                    && mShareGroup.equals(that.mShareGroup);
        }
    }
}

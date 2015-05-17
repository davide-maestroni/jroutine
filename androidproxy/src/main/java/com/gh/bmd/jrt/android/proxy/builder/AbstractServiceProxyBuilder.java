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
package com.gh.bmd.jrt.android.proxy.builder;

import com.gh.bmd.jrt.android.builder.ServiceConfiguration;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.reflect.Type;
import java.util.HashMap;

import javax.annotation.Nonnull;

/**
 * Abstract implementation of a builder of async proxy objects, whose methods are executed in a
 * dedicated service.
 * <p/>
 * Created by davide-maestroni on 13/05/15.
 *
 * @param <TYPE> the interface type.
 */
public abstract class AbstractServiceProxyBuilder<TYPE> implements ServiceProxyBuilder<TYPE>,
        RoutineConfiguration.Configurable<ServiceProxyBuilder<TYPE>>,
        ProxyConfiguration.Configurable<ServiceProxyBuilder<TYPE>>,
        ServiceConfiguration.Configurable<ServiceProxyBuilder<TYPE>> {

    private static final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> sClassMap =
            new WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>>();

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private RoutineConfiguration mRoutineConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

    private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.DEFAULT_CONFIGURATION;

    @Nonnull
    public TYPE buildProxy() {

        synchronized (sClassMap) {

            final Object target = getTargetClass();
            final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> classMap = sClassMap;
            HashMap<ClassInfo, Object> classes = classMap.get(target);

            if (classes == null) {

                classes = new HashMap<ClassInfo, Object>();
                classMap.put(target, classes);
            }

            final RoutineConfiguration routineConfiguration = mRoutineConfiguration;
            final ProxyConfiguration proxyConfiguration = mProxyConfiguration;
            final ServiceConfiguration serviceConfiguration = mServiceConfiguration;
            final ClassToken<TYPE> token = getInterfaceToken();
            final ClassInfo classInfo =
                    new ClassInfo(token, routineConfiguration, proxyConfiguration,
                                  serviceConfiguration);
            final Object instance = classes.get(classInfo);

            if (instance != null) {

                return token.cast(instance);
            }

            warn(routineConfiguration);

            try {

                final TYPE newInstance =
                        newProxy(routineConfiguration, proxyConfiguration, serviceConfiguration);
                classes.put(classInfo, newInstance);
                return newInstance;

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }

    @Nonnull
    public RoutineConfiguration.Builder<? extends ServiceProxyBuilder<TYPE>> withRoutine() {

        final RoutineConfiguration config = mRoutineConfiguration;
        return new RoutineConfiguration.Builder<ServiceProxyBuilder<TYPE>>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ServiceProxyBuilder<TYPE>> withProxy() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ServiceProxyBuilder<TYPE>>(this, config);
    }

    @Nonnull
    public ServiceConfiguration.Builder<? extends ServiceProxyBuilder<TYPE>> withService() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceProxyBuilder<TYPE>>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceProxyBuilder<TYPE> setConfiguration(
            @Nonnull final ServiceConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mServiceConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceProxyBuilder<TYPE> setConfiguration(
            @Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mRoutineConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceProxyBuilder<TYPE> setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    /**
     * Returns the builder proxy class token.
     *
     * @return the proxy class token.
     */
    @Nonnull
    protected abstract ClassToken<TYPE> getInterfaceToken();

    /**
     * Returns the builder target class.
     *
     * @return the target class.
     */
    @Nonnull
    protected abstract Class<?> getTargetClass();

    /**
     * Creates and return a new proxy instance.
     *
     * @param routineConfiguration the routine configuration.
     * @param proxyConfiguration   the proxy configuration.
     * @param serviceConfiguration the service configuration.
     * @return the proxy instance.
     */
    @Nonnull
    protected abstract TYPE newProxy(@Nonnull final RoutineConfiguration routineConfiguration,
            @Nonnull final ProxyConfiguration proxyConfiguration,
            @Nonnull final ServiceConfiguration serviceConfiguration);

    /**
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param configuration the routine configuration.
     */
    private void warn(@Nonnull final RoutineConfiguration configuration) {

        Logger logger = null;
        final Runner asyncRunner = configuration.getAsyncRunnerOr(null);

        if (asyncRunner != null) {

            logger = configuration.newLogger(this);
            logger.wrn("the specified runner will be ignored: %s", asyncRunner);
        }

        final OrderType inputOrderType = configuration.getInputOrderTypeOr(null);

        if (inputOrderType != null) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified input order type will be ignored: %s", inputOrderType);
        }

        final int inputSize = configuration.getInputMaxSizeOr(RoutineConfiguration.DEFAULT);

        if (inputSize != RoutineConfiguration.DEFAULT) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified maximum input size will be ignored: %d", inputSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeoutOr(null);

        if (inputTimeout != null) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified input timeout will be ignored: %s", inputTimeout);
        }

        final OrderType outputOrderType = configuration.getOutputOrderTypeOr(null);

        if (outputOrderType != null) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified output order type will be ignored: %s", outputOrderType);
        }

        final int outputSize = configuration.getOutputMaxSizeOr(RoutineConfiguration.DEFAULT);

        if (outputSize != RoutineConfiguration.DEFAULT) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified maximum output size will be ignored: %d", outputSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeoutOr(null);

        if (outputTimeout != null) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified output timeout will be ignored: %s", outputTimeout);
        }
    }

    /**
     * Class used as key to identify a specific proxy instance.
     */
    private static class ClassInfo {

        private final ProxyConfiguration mProxyConfiguration;

        private final RoutineConfiguration mRoutineConfiguration;

        private final ServiceConfiguration mServiceConfiguration;

        private final Type mType;

        /**
         * Constructor.
         *
         * @param token                the proxy interface token.
         * @param routineConfiguration the routine configuration.
         * @param proxyConfiguration   the proxy configuration.
         * @param serviceConfiguration the service configuration.
         */
        private ClassInfo(@Nonnull final ClassToken<?> token,
                @Nonnull final RoutineConfiguration routineConfiguration,
                @Nonnull final ProxyConfiguration proxyConfiguration,
                @Nonnull final ServiceConfiguration serviceConfiguration) {

            mType = token.getRawClass();
            mRoutineConfiguration = routineConfiguration;
            mProxyConfiguration = proxyConfiguration;
            mServiceConfiguration = serviceConfiguration;
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

            final ClassInfo classInfo = (ClassInfo) o;
            return mProxyConfiguration.equals(classInfo.mProxyConfiguration)
                    && mRoutineConfiguration.equals(classInfo.mRoutineConfiguration)
                    && mServiceConfiguration.equals(classInfo.mServiceConfiguration)
                    && mType.equals(classInfo.mType);
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mProxyConfiguration.hashCode();
            result = 31 * result + mRoutineConfiguration.hashCode();
            result = 31 * result + mServiceConfiguration.hashCode();
            result = 31 * result + mType.hashCode();
            return result;
        }
    }
}

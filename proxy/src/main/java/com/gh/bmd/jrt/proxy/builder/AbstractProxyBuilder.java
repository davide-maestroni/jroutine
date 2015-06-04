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
package com.gh.bmd.jrt.proxy.builder;

import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.TimeDuration;
import com.gh.bmd.jrt.util.WeakIdentityHashMap;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;

import javax.annotation.Nonnull;

/**
 * Abstract implementation of a builder of async proxy objects.
 * <p/>
 * Created by davide-maestroni on 2/26/15.
 *
 * @param <TYPE> the interface type.
 */
public abstract class AbstractProxyBuilder<TYPE>
        implements ProxyBuilder<TYPE>, InvocationConfiguration.Configurable<ProxyBuilder<TYPE>>,
        ProxyConfiguration.Configurable<ProxyBuilder<TYPE>> {

    private static final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> sClassMap =
            new WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>>();

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    @Nonnull
    public TYPE buildProxy() {

        synchronized (sClassMap) {

            final Object target = getTarget();
            final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> classMap = sClassMap;
            HashMap<ClassInfo, Object> classes = classMap.get(target);

            if (classes == null) {

                classes = new HashMap<ClassInfo, Object>();
                classMap.put(target, classes);
            }

            final InvocationConfiguration invocationConfiguration = mInvocationConfiguration;
            final ProxyConfiguration proxyConfiguration = mProxyConfiguration;
            final ClassToken<TYPE> token = getInterfaceToken();
            final ClassInfo classInfo =
                    new ClassInfo(token, invocationConfiguration, proxyConfiguration);
            final Object instance = classes.get(classInfo);

            if (instance != null) {

                return token.cast(instance);
            }

            warn(invocationConfiguration);

            try {

                final TYPE newInstance = newProxy(invocationConfiguration, proxyConfiguration);
                classes.put(classInfo, newInstance);
                return newInstance;

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ProxyBuilder<TYPE>> withInvocation() {

        final InvocationConfiguration configuration = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ProxyBuilder<TYPE>>(this, configuration);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ProxyBuilder<TYPE> setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ProxyBuilder<TYPE> setConfiguration(@Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ProxyBuilder<TYPE>> withProxy() {

        final ProxyConfiguration configuration = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ProxyBuilder<TYPE>>(this, configuration);
    }

    /**
     * Returns the builder proxy class token.
     *
     * @return the proxy class token.
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
     * Creates and return a new proxy instance.
     *
     * @param invocationConfiguration the invocation configuration.
     * @param proxyConfiguration      the proxy configuration.
     * @return the proxy instance.
     */
    @Nonnull
    protected abstract TYPE newProxy(@Nonnull final InvocationConfiguration invocationConfiguration,
            @Nonnull final ProxyConfiguration proxyConfiguration);

    /**
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param configuration the invocation configuration.
     */
    private void warn(@Nonnull final InvocationConfiguration configuration) {

        Logger logger = null;
        final Object[] args = configuration.getFactoryArgsOr(null);

        if (args != null) {

            logger = configuration.newLogger(this);
            logger.wrn("the specified factory arguments will be ignored: %s",
                       Arrays.toString(args));
        }

        final OrderType inputOrderType = configuration.getInputOrderTypeOr(null);

        if (inputOrderType != null) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified input order type will be ignored: %s", inputOrderType);
        }

        final int inputSize = configuration.getInputMaxSizeOr(InvocationConfiguration.DEFAULT);

        if (inputSize != InvocationConfiguration.DEFAULT) {

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

        final int outputSize = configuration.getOutputMaxSizeOr(InvocationConfiguration.DEFAULT);

        if (outputSize != InvocationConfiguration.DEFAULT) {

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

        private final InvocationConfiguration mInvocationConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        private final Type mType;

        /**
         * Constructor.
         *
         * @param token                   the proxy interface token.
         * @param invocationConfiguration the invocation configuration.
         * @param proxyConfiguration      the proxy configuration.
         */
        private ClassInfo(@Nonnull final ClassToken<?> token,
                @Nonnull final InvocationConfiguration invocationConfiguration,
                @Nonnull final ProxyConfiguration proxyConfiguration) {

            mType = token.getRawClass();
            mInvocationConfiguration = invocationConfiguration;
            mProxyConfiguration = proxyConfiguration;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mProxyConfiguration.hashCode();
            result = 31 * result + mInvocationConfiguration.hashCode();
            result = 31 * result + mType.hashCode();
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

            final ClassInfo classInfo = (ClassInfo) o;
            return mProxyConfiguration.equals(classInfo.mProxyConfiguration)
                    && mInvocationConfiguration.equals(classInfo.mInvocationConfiguration)
                    && mType.equals(classInfo.mType);
        }
    }
}

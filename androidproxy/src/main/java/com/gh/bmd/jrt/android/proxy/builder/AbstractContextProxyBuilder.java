package com.gh.bmd.jrt.android.proxy.builder;

import com.gh.bmd.jrt.android.builder.InvocationConfiguration;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.WeakIdentityHashMap;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;

import javax.annotation.Nonnull;

/**
 * Created by davide on 06/05/15.
 */
public abstract class AbstractContextProxyBuilder<TYPE> implements ContextProxyBuilder<TYPE>,
        RoutineConfiguration.Configurable<ContextProxyBuilder<TYPE>>,
        ProxyConfiguration.Configurable<ContextProxyBuilder<TYPE>>,
        InvocationConfiguration.Configurable<ContextProxyBuilder<TYPE>> {

    private static final WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>> sClassMap =
            new WeakIdentityHashMap<Object, HashMap<ClassInfo, Object>>();

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private RoutineConfiguration mRoutineConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

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

            final String shareGroup = mProxyConfiguration.getShareGroupOr(null);
            final String classShareGroup = (shareGroup != null) ? shareGroup : ShareGroup.ALL;
            final RoutineConfiguration routineConfiguration = mRoutineConfiguration;
            final InvocationConfiguration invocationConfiguration = mInvocationConfiguration;
            final ClassToken<TYPE> token = getInterfaceToken();
            final ClassInfo classInfo =
                    new ClassInfo(token, routineConfiguration, invocationConfiguration,
                                  classShareGroup);
            final Object instance = classes.get(classInfo);

            if (instance != null) {

                return token.cast(instance);
            }

            warn(routineConfiguration);

            try {

                final TYPE newInstance =
                        newProxy(classShareGroup, routineConfiguration, invocationConfiguration);
                classes.put(classInfo, newInstance);
                return newInstance;

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextProxyBuilder<TYPE> setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextProxyBuilder<TYPE> setConfiguration(
            @Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mRoutineConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextProxyBuilder<TYPE> setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the proxy configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ContextProxyBuilder<TYPE>>
    withInvocationConfiguration() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ContextProxyBuilder<TYPE>>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ContextProxyBuilder<TYPE>> withProxyConfiguration
            () {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ContextProxyBuilder<TYPE>>(this, config);
    }

    @Nonnull
    public RoutineConfiguration.Builder<? extends ContextProxyBuilder<TYPE>>
    withRoutineConfiguration() {

        final RoutineConfiguration config = mRoutineConfiguration;
        return new RoutineConfiguration.Builder<ContextProxyBuilder<TYPE>>(this, config);
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
     * @param shareGroup              the share group name.
     * @param routineConfiguration    the routine configuration.
     * @param invocationConfiguration the invocation configuration.
     * @return the proxy instance.
     */
    @Nonnull
    protected abstract TYPE newProxy(@Nonnull final String shareGroup,
            @Nonnull final RoutineConfiguration routineConfiguration,
            @Nonnull final InvocationConfiguration invocationConfiguration);

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

        private final InvocationConfiguration mInvocationConfiguration;

        private final RoutineConfiguration mRoutineConfiguration;

        private final String mShareGroup;

        private final Type mType;

        /**
         * Constructor.
         *
         * @param token                   the proxy interface token.
         * @param routineConfiguration    the routine configuration.
         * @param invocationConfiguration the invocation configuration.
         * @param shareGroup              the share group name.
         */
        private ClassInfo(@Nonnull final ClassToken<?> token,
                @Nonnull final RoutineConfiguration routineConfiguration,
                @Nonnull final InvocationConfiguration invocationConfiguration,
                @Nonnull final String shareGroup) {

            mType = token.getRawClass();
            mRoutineConfiguration = routineConfiguration;
            mInvocationConfiguration = invocationConfiguration;
            mShareGroup = shareGroup;
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mInvocationConfiguration.hashCode();
            result = 31 * result + mRoutineConfiguration.hashCode();
            result = 31 * result + mShareGroup.hashCode();
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
            return mInvocationConfiguration.equals(classInfo.mInvocationConfiguration)
                    && mRoutineConfiguration.equals(classInfo.mRoutineConfiguration)
                    && mShareGroup.equals(classInfo.mShareGroup) && mType.equals(classInfo.mType);
        }
    }
}

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
package com.gh.bmd.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build.VERSION_CODES;

import com.gh.bmd.jrt.android.builder.ContextRoutineBuilder;
import com.gh.bmd.jrt.android.builder.InvocationConfiguration;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.routine.ContextRoutine;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.TemplateRoutineBuilder;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of an Android routine builder.
 * <p/>
 * Created by davide on 12/9/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
class DefaultContextRoutineBuilder<INPUT, OUTPUT> extends TemplateRoutineBuilder<INPUT, OUTPUT>
        implements ContextRoutineBuilder<INPUT, OUTPUT>,
        InvocationConfiguration.Configurable<ContextRoutineBuilder<INPUT, OUTPUT>> {

    private final WeakReference<Object> mContext;

    private final Class<? extends ContextInvocation<INPUT, OUTPUT>> mInvocationClass;

    private final RoutineConfiguration.Configurable<ContextRoutineBuilder<INPUT, OUTPUT>>
            mRoutineConfigurable =
            new RoutineConfiguration.Configurable<ContextRoutineBuilder<INPUT, OUTPUT>>() {

                @Nonnull
                public ContextRoutineBuilder<INPUT, OUTPUT> apply(
                        @Nonnull final RoutineConfiguration configuration) {

                    return DefaultContextRoutineBuilder.this.apply(configuration);
                }
            };

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param activity   the context activity.
     * @param classToken the invocation class token.
     * @throws java.lang.NullPointerException if the activity or class token are null.
     */
    DefaultContextRoutineBuilder(@Nonnull final Activity activity,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> classToken) {

        this((Object) activity, classToken);
    }

    /**
     * Constructor.
     *
     * @param fragment   the context fragment.
     * @param classToken the invocation class token.
     * @throws java.lang.NullPointerException if the fragment or class token are null.
     */
    DefaultContextRoutineBuilder(@Nonnull final Fragment fragment,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> classToken) {

        this((Object) fragment, classToken);
    }

    /**
     * Constructor.
     *
     * @param context    the context instance.
     * @param classToken the invocation class token.
     * @throws java.lang.NullPointerException if the context or class token are null.
     */
    @SuppressWarnings("ConstantConditions")
    private DefaultContextRoutineBuilder(@Nonnull final Object context,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> classToken) {

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        mContext = new WeakReference<Object>(context);
        mInvocationClass = classToken.getRawClass();
    }

    @Nonnull
    public ContextRoutineBuilder<INPUT, OUTPUT> apply(
            @Nonnull final RoutineConfiguration configuration) {

        super.apply(configuration);
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfiguration.Builder<? extends
            ContextRoutineBuilder<INPUT, OUTPUT>> withConfiguration() {

        return new RoutineConfiguration.Builder<ContextRoutineBuilder<INPUT, OUTPUT>>(
                mRoutineConfigurable, getConfiguration());
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextRoutineBuilder<INPUT, OUTPUT> apply(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    public ContextRoutine<INPUT, OUTPUT> buildRoutine() {

        final RoutineConfiguration configuration = getConfiguration();
        warn(configuration);
        final RoutineConfiguration.Builder<RoutineConfiguration> builder =
                configuration.builderFrom()
                             .withAsyncRunner(Runners.mainRunner())
                             .withInputMaxSize(Integer.MAX_VALUE)
                             .withInputTimeout(TimeDuration.INFINITY)
                             .withOutputMaxSize(Integer.MAX_VALUE)
                             .withOutputTimeout(TimeDuration.INFINITY);
        return new DefaultContextRoutine<INPUT, OUTPUT>(mContext, mInvocationClass, builder.apply(),
                                                        mInvocationConfiguration);
    }

    @Override
    public void purge() {

        buildRoutine().purge();
    }

    public void purge(@Nullable final INPUT input) {

        buildRoutine().purge(input);
    }

    public void purge(@Nullable final INPUT... inputs) {

        buildRoutine().purge(inputs);
    }

    public void purge(@Nullable final Iterable<? extends INPUT> inputs) {

        buildRoutine().purge(inputs);
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ContextRoutineBuilder<INPUT, OUTPUT>>
    withInvocation() {


        return new InvocationConfiguration.Builder<ContextRoutineBuilder<INPUT, OUTPUT>>(this,
                                                                                         mInvocationConfiguration);
    }

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
}

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

import android.content.Context;

import com.gh.bmd.jrt.android.builder.ServiceConfiguration;
import com.gh.bmd.jrt.android.builder.ServiceRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.TemplateRoutineBuilder;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.routine.Routine;

import javax.annotation.Nonnull;

/**
 * Class implementing a builder of routine objects based on an invocation class token.
 * <p/>
 * Created by davide on 1/8/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultServiceRoutineBuilder<INPUT, OUTPUT> extends TemplateRoutineBuilder<INPUT, OUTPUT>
        implements ServiceRoutineBuilder<INPUT, OUTPUT>,
        ServiceConfiguration.Configurable<ServiceRoutineBuilder<INPUT, OUTPUT>> {

    private final RoutineConfiguration.Configurable<ServiceRoutineBuilder<INPUT, OUTPUT>>
            mConfigurable =
            new RoutineConfiguration.Configurable<ServiceRoutineBuilder<INPUT, OUTPUT>>() {

                @Nonnull
                public ServiceRoutineBuilder<INPUT, OUTPUT> setConfiguration(
                        @Nonnull final RoutineConfiguration configuration) {

                    return DefaultServiceRoutineBuilder.this.setConfiguration(configuration);
                }
            };

    private final Context mContext;

    private final Class<? extends ContextInvocation<INPUT, OUTPUT>> mInvocationClass;

    private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context    the routine context.
     * @param classToken the invocation class token.
     * @throws java.lang.NullPointerException if the context or the class token are null.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultServiceRoutineBuilder(@Nonnull final Context context,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> classToken) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        mContext = context;
        mInvocationClass = classToken.getRawClass();
    }

    @Nonnull
    public Routine<INPUT, OUTPUT> buildRoutine() {

        return new ServiceRoutine<INPUT, OUTPUT>(mContext, mInvocationClass, getConfiguration(),
                                                 mServiceConfiguration);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceRoutineBuilder<INPUT, OUTPUT> setConfiguration(
            @Nonnull final ServiceConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mServiceConfiguration = configuration;
        return this;
    }

    @Nonnull
    @Override
    public ServiceRoutineBuilder<INPUT, OUTPUT> setConfiguration(
            @Nonnull final RoutineConfiguration configuration) {

        super.setConfiguration(configuration);
        return this;
    }

    @Nonnull
    @Override
    public RoutineConfiguration.Builder<? extends ServiceRoutineBuilder<INPUT, OUTPUT>>
    withRoutineConfiguration() {


        final RoutineConfiguration configuration = getConfiguration();
        return new RoutineConfiguration.Builder<ServiceRoutineBuilder<INPUT, OUTPUT>>(mConfigurable,
                                                                                      configuration);
    }

    @Nonnull
    public ServiceConfiguration.Builder<? extends ServiceRoutineBuilder<INPUT, OUTPUT>>
    withServiceConfiguration() {


        final ServiceConfiguration configuration = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceRoutineBuilder<INPUT, OUTPUT>>(this,
                                                                                      configuration);
    }
}

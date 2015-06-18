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
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.TemplateRoutineBuilder;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class implementing a builder of routine objects executed in a dedicated service.
 * <p/>
 * Created by davide-maestroni on 1/8/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultServiceRoutineBuilder<INPUT, OUTPUT> extends TemplateRoutineBuilder<INPUT, OUTPUT>
        implements ServiceRoutineBuilder<INPUT, OUTPUT>,
        ServiceConfiguration.Configurable<ServiceRoutineBuilder<INPUT, OUTPUT>> {

    private final InvocationConfiguration.Configurable<ServiceRoutineBuilder<INPUT, OUTPUT>>
            mConfigurable =
            new InvocationConfiguration.Configurable<ServiceRoutineBuilder<INPUT, OUTPUT>>() {

                @Nonnull
                public ServiceRoutineBuilder<INPUT, OUTPUT> setConfiguration(
                        @Nonnull final InvocationConfiguration configuration) {

                    return DefaultServiceRoutineBuilder.this.setConfiguration(configuration);
                }
            };

    private final Context mContext;

    private final Object[] mFactoryArgs;

    private final Class<? extends ContextInvocation<INPUT, OUTPUT>> mInvocationClass;

    private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context     the routine context.
     * @param classToken  the invocation class token.
     * @param factoryArgs the invocation factory arguments.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultServiceRoutineBuilder(@Nonnull final Context context,
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> classToken,
            @Nullable final Object[] factoryArgs) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        mContext = context;
        mInvocationClass = classToken.getRawClass();
        mFactoryArgs = factoryArgs;
    }

    @Nonnull
    public Routine<INPUT, OUTPUT> buildRoutine() {

        return new ServiceRoutine<INPUT, OUTPUT>(mContext, mInvocationClass, mFactoryArgs,
                                                 getConfiguration(), mServiceConfiguration);
    }

    @Nonnull
    @Override
    public InvocationConfiguration.Builder<? extends ServiceRoutineBuilder<INPUT, OUTPUT>>
    invocations() {


        final InvocationConfiguration config = getConfiguration();
        return new InvocationConfiguration.Builder<ServiceRoutineBuilder<INPUT, OUTPUT>>(
                mConfigurable, config);
    }

    @Nonnull
    @Override
    public ServiceRoutineBuilder<INPUT, OUTPUT> setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        super.setConfiguration(configuration);
        return this;
    }

    @Nonnull
    public ServiceConfiguration.Builder<? extends ServiceRoutineBuilder<INPUT, OUTPUT>> service() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceRoutineBuilder<INPUT, OUTPUT>>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceRoutineBuilder<INPUT, OUTPUT> setConfiguration(
            @Nonnull final ServiceConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the service configuration must not be null");
        }

        mServiceConfiguration = configuration;
        return this;
    }
}

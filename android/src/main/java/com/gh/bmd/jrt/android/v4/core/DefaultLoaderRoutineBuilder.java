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
package com.gh.bmd.jrt.android.v4.core;

import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.builder.LoaderRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.routine.LoaderRoutine;
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.TemplateRoutineBuilder;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.util.Reflection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of a loader routine builder.
 * <p/>
 * Created by davide-maestroni on 12/9/14.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class DefaultLoaderRoutineBuilder<INPUT, OUTPUT> extends TemplateRoutineBuilder<INPUT, OUTPUT>
        implements LoaderRoutineBuilder<INPUT, OUTPUT>,
        LoaderConfiguration.Configurable<LoaderRoutineBuilder<INPUT, OUTPUT>> {

    private final LoaderContext mContext;

    private final ContextInvocationFactory<INPUT, OUTPUT> mFactory;

    private final InvocationConfiguration.Configurable<LoaderRoutineBuilder<INPUT, OUTPUT>>
            mRoutineConfigurable =
            new InvocationConfiguration.Configurable<LoaderRoutineBuilder<INPUT, OUTPUT>>() {

                @Nonnull
                public LoaderRoutineBuilder<INPUT, OUTPUT> setConfiguration(
                        @Nonnull final InvocationConfiguration configuration) {

                    return DefaultLoaderRoutineBuilder.this.setConfiguration(configuration);
                }
            };

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context the routine context.
     * @param factory the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified factory is not
     *                                            static.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultLoaderRoutineBuilder(@Nonnull final LoaderContext context,
            @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> factory) {

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        final Class<? extends ContextInvocationFactory> factoryClass = factory.getClass();

        if (!Reflection.isStaticClass(factoryClass)) {

            throw new IllegalArgumentException(
                    "the factory class must be static: " + factoryClass.getName());
        }

        mContext = context;
        mFactory = factory;
    }

    @Nonnull
    public LoaderRoutine<INPUT, OUTPUT> buildRoutine() {

        final InvocationConfiguration configuration = getConfiguration();
        final Runner mainRunner = Runners.mainRunner();
        final Runner asyncRunner = configuration.getAsyncRunnerOr(mainRunner);

        if (asyncRunner != mainRunner) {

            configuration.newLogger(this)
                         .wrn("the specified async runner will be ignored: %s", asyncRunner);
        }

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                configuration.builderFrom().withAsyncRunner(mainRunner);
        return new DefaultLoaderRoutine<INPUT, OUTPUT>(mContext, mFactory, builder.set(),
                                                       mLoaderConfiguration);
    }

    @Nonnull
    @Override
    public InvocationConfiguration.Builder<? extends
            LoaderRoutineBuilder<INPUT, OUTPUT>> invocations() {

        return new InvocationConfiguration.Builder<LoaderRoutineBuilder<INPUT, OUTPUT>>(
                mRoutineConfigurable, getConfiguration());
    }

    @Nonnull
    public LoaderRoutineBuilder<INPUT, OUTPUT> setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        super.setConfiguration(configuration);
        return this;
    }

    @Nonnull
    public LoaderConfiguration.Builder<? extends LoaderRoutineBuilder<INPUT, OUTPUT>> loaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderRoutineBuilder<INPUT, OUTPUT>>(this, config);
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
    @SuppressWarnings("ConstantConditions")
    public LoaderRoutineBuilder<INPUT, OUTPUT> setConfiguration(
            @Nonnull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mLoaderConfiguration = configuration;
        return this;
    }
}

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
package com.github.dm.jrt.android.v4.core;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.routine.LoaderRoutine;
import com.github.dm.jrt.android.runner.Runners;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.builder.TemplateRoutineBuilder;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.util.Reflection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of a loader routine builder.
 * <p/>
 * Created by davide-maestroni on 12/09/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class DefaultLoaderRoutineBuilder<IN, OUT> extends TemplateRoutineBuilder<IN, OUT>
        implements LoaderRoutineBuilder<IN, OUT>,
        LoaderConfiguration.Configurable<LoaderRoutineBuilder<IN, OUT>> {

    private final LoaderContext mContext;

    private final ContextInvocationFactory<IN, OUT> mFactory;

    private final InvocationConfiguration.Configurable<LoaderRoutineBuilder<IN, OUT>>
            mRoutineConfigurable =
            new InvocationConfiguration.Configurable<LoaderRoutineBuilder<IN, OUT>>() {

                @Nonnull
                public LoaderRoutineBuilder<IN, OUT> setConfiguration(
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
            @Nonnull final ContextInvocationFactory<IN, OUT> factory) {

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        final Class<? extends ContextInvocationFactory> factoryClass = factory.getClass();

        if (!Reflection.hasStaticContext(factoryClass)) {

            throw new IllegalArgumentException(
                    "the factory class must be static: " + factoryClass.getName());
        }

        mContext = context;
        mFactory = factory;
    }

    @Nonnull
    public LoaderRoutine<IN, OUT> buildRoutine() {

        final InvocationConfiguration configuration = getConfiguration();
        final Runner mainRunner = Runners.mainRunner();
        final Runner asyncRunner = configuration.getRunnerOr(mainRunner);

        if (asyncRunner != mainRunner) {

            configuration.newLogger(this)
                         .wrn("the specified async runner will be ignored: %s", asyncRunner);
        }

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                configuration.builderFrom().withRunner(mainRunner);
        return new DefaultLoaderRoutine<IN, OUT>(mContext, mFactory, builder.set(),
                                                 mLoaderConfiguration);
    }

    @Nonnull
    @Override
    public InvocationConfiguration.Builder<? extends
            LoaderRoutineBuilder<IN, OUT>> invocations() {

        return new InvocationConfiguration.Builder<LoaderRoutineBuilder<IN, OUT>>(
                mRoutineConfigurable, getConfiguration());
    }

    @Nonnull
    public LoaderRoutineBuilder<IN, OUT> setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        super.setConfiguration(configuration);
        return this;
    }

    @Nonnull
    public LoaderConfiguration.Builder<? extends LoaderRoutineBuilder<IN, OUT>> loaders() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderRoutineBuilder<IN, OUT>>(this, config);
    }

    @Override
    public void purge() {

        buildRoutine().purge();
    }

    public void purge(@Nullable final IN input) {

        buildRoutine().purge(input);
    }

    public void purge(@Nullable final IN... inputs) {

        buildRoutine().purge(inputs);
    }

    public void purge(@Nullable final Iterable<? extends IN> inputs) {

        buildRoutine().purge(inputs);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderRoutineBuilder<IN, OUT> setConfiguration(
            @Nonnull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the loader configuration must not be null");
        }

        mLoaderConfiguration = configuration;
        return this;
    }
}

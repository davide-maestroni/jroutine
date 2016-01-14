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
package com.github.dm.jrt.android.v11.stream;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.invocation.FunctionContextInvocationFactory;
import com.github.dm.jrt.android.stream.AbstractLoaderStreamOutputChannel;
import com.github.dm.jrt.android.stream.LoaderStreamOutputChannel;
import com.github.dm.jrt.android.v11.core.JRoutine;
import com.github.dm.jrt.android.v11.core.JRoutine.ContextBuilder;
import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.core.DelegatingInvocation.DelegationType;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.android.core.DelegatingContextInvocation.factoryFrom;

/**
 * Default implementation of a loader stream output channel.
 * <p/>
 * Created by davide-maestroni on 01/12/2016.
 *
 * @param <OUT> the output data type.
 */
public class DefaultLoaderStreamOutputChannel<OUT> extends AbstractLoaderStreamOutputChannel<OUT> {

    private final ContextBuilder mContextBuilder;

    /**
     * Constructor.
     *
     * @param builder                 the context builder.
     * @param invocationConfiguration the initial invocation configuration.
     * @param loaderConfiguration     the initial loader configuration.
     * @param channel                 the wrapped output channel.
     */
    DefaultLoaderStreamOutputChannel(@Nullable final ContextBuilder builder,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final OutputChannel<OUT> channel) {

        super(invocationConfiguration, loaderConfiguration, channel);
        mContextBuilder = builder;
    }

    /**
     * Constructor.
     *
     * @param builder the context builder.
     * @param channel the wrapped output channel.
     */
    DefaultLoaderStreamOutputChannel(@Nullable final ContextBuilder builder,
            @NotNull final OutputChannel<OUT> channel) {

        super(InvocationConfiguration.DEFAULT_CONFIGURATION,
              LoaderConfiguration.DEFAULT_CONFIGURATION, channel);
        mContextBuilder = builder;
    }

    @NotNull
    @Override
    protected <AFTER> LoaderStreamOutputChannel<AFTER> newChannel(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final OutputChannel<AFTER> channel) {

        return new DefaultLoaderStreamOutputChannel<AFTER>(mContextBuilder, invocationConfiguration,
                                                           loaderConfiguration, channel);
    }

    @NotNull
    @Override
    protected <AFTER> Routine<? super OUT, ? extends AFTER> newRoutine(
            @NotNull final InvocationConfiguration invocationConfiguration,
            @NotNull final LoaderConfiguration loaderConfiguration,
            @NotNull final InvocationFactory<? super OUT, ? extends AFTER> factory) {

        final ContextBuilder contextBuilder = mContextBuilder;

        if (contextBuilder == null) {

            return JRoutine.on(factory)
                           .withInvocations()
                           .with(invocationConfiguration)
                           .set()
                           .buildRoutine();
        }

        final FunctionContextInvocationFactory<? super OUT, ? extends AFTER> invocationFactory =
                factoryFrom(JRoutine.on(factory).buildRoutine(), factory.hashCode(),
                            DelegationType.SYNC);
        return contextBuilder.on(invocationFactory)
                             .withInvocations()
                             .with(invocationConfiguration)
                             .set()
                             .withLoaders()
                             .with(loaderConfiguration)
                             .set()
                             .buildRoutine();
    }
}

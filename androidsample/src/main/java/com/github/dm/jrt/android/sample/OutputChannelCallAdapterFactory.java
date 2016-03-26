/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.sample;

import android.support.v4.app.FragmentActivity;

import com.github.dm.jrt.android.JRoutineAndroidCompat;
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.object.AndroidBuilders;
import com.github.dm.jrt.android.retrofit.AbstractCallAdapterFactory;
import com.github.dm.jrt.android.retrofit.ComparableCall;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.object.Builders;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

import retrofit2.Retrofit;

/**
 * Output channel adapter implementation.
 * <p/>
 * Created by davide-maestroni on 03/25/2016.
 */
public class OutputChannelCallAdapterFactory extends AbstractCallAdapterFactory<OutputChannel> {

    private final LoaderRoutineBuilder<ComparableCall<Object>, Object> mBuilder;

    /**
     * Constructor.
     *
     * @param activity the context activity.
     */
    public OutputChannelCallAdapterFactory(@NotNull final FragmentActivity activity) {

        super(OutputChannel.class, 0);
        mBuilder = JRoutineAndroidCompat.with(activity).on(getFactory());
    }

    @NotNull
    @Override
    protected OutputChannel<?> adapt(@NotNull final ComparableCall<Object> call,
            @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) {

        final InvocationConfiguration invocationConfiguration =
                Builders.configurationWithAnnotations(InvocationConfiguration.DEFAULT_CONFIGURATION,
                        annotations);
        final LoaderConfiguration loaderConfiguration =
                AndroidBuilders.configurationWithAnnotations(
                        LoaderConfiguration.DEFAULT_CONFIGURATION, annotations);
        return mBuilder.withInvocations()
                       .with(invocationConfiguration)
                       .setConfiguration()
                       .withLoaders()
                       .with(loaderConfiguration)
                       .setConfiguration()
                       .asyncCall(call);
    }
}

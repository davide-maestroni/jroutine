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

import com.github.dm.jrt.android.JRoutineAndroidCompat;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.object.AndroidBuilders;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.object.Builders;
import com.github.dm.jrt.retrofit.AbstractCallAdapterFactory;
import com.github.dm.jrt.retrofit.ComparableCall;
import com.github.dm.jrt.retrofit.ExecuteCall;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.Retrofit;

/**
 * Output channel adapter implementation.
 * <p/>
 * Created by davide-maestroni on 03/25/2016.
 */
public class OutputChannelCallAdapterFactory extends AbstractCallAdapterFactory<OutputChannel> {

    private static final ExecuteCall<OutputChannel> sFactory = new ExecuteCall<>();

    private final LoaderContextCompat mContext;

    /**
     * Constructor.
     *
     * @param context the loader context.
     */
    private OutputChannelCallAdapterFactory(@NotNull final LoaderContextCompat context) {

        super(OutputChannel.class, 0);
        mContext = ConstantConditions.notNull("loader context", context);
    }

    /**
     * Returns a new factory based on the specified context.
     *
     * @param context the loader context.
     * @return the factory instance.
     */
    @NotNull
    public static OutputChannelCallAdapterFactory with(@NotNull final LoaderContextCompat context) {

        return new OutputChannelCallAdapterFactory(context);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected <C extends Call<OutputChannel>> OutputChannel adapt(
            @NotNull final Routine<C, OutputChannel> routine, @NotNull final Call<?> call) {

        // Makes the call comparable so to ensure the correct computation of the loader ID
        final ComparableCall<OutputChannel> comparableCall = ComparableCall.of(call);
        return routine.asyncCall((C) comparableCall);
    }

    @NotNull
    @Override
    protected Routine<? extends Call<OutputChannel>, OutputChannel> getRoutine(
            @NotNull final Type responseType, @NotNull final Annotation[] annotations,
            @NotNull final Retrofit retrofit) {

        // Use annotations to configure the routine
        final InvocationConfiguration invocationConfiguration =
                Builders.configurationWithAnnotations(InvocationConfiguration.DEFAULT_CONFIGURATION,
                        annotations);
        final LoaderConfiguration loaderConfiguration =
                AndroidBuilders.configurationWithAnnotations(
                        LoaderConfiguration.DEFAULT_CONFIGURATION, annotations);
        return JRoutineAndroidCompat.with(mContext)
                                    .on(sFactory)
                                    .invocationConfiguration()
                                    .with(invocationConfiguration)
                                    .setConfiguration()
                                    .loaderConfiguration()
                                    .with(loaderConfiguration)
                                    .setConfiguration()
                                    .buildRoutine();
    }
}

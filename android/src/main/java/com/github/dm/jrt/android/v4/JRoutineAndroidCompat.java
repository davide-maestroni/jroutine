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

package com.github.dm.jrt.android.v4;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.github.dm.jrt.JRoutine;
import com.github.dm.jrt.android.ServiceBuilder;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.v4.channel.SparseChannelsCompat;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.android.v4.stream.JRoutineLoaderStreamCompat;
import com.github.dm.jrt.android.v4.stream.LoaderStreamBuilderCompat;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;

/**
 * Class acting as a fa&ccedil;ade of all the JRoutine library features, with support for the
 * Android compatibility library.
 * <p>
 * Created by davide-maestroni on 03/06/2016.
 */
public class JRoutineAndroidCompat extends SparseChannelsCompat {

    /**
     * Avoid explicit instantiation.
     */
    protected JRoutineAndroidCompat() {
        ConstantConditions.avoid();
    }

    /**
     * Returns a channel builder.
     *
     * @return the channel builder instance.
     */
    @NotNull
    public static ChannelBuilder io() {
        return JRoutine.io();
    }

    /**
     * Returns a Context based builder of Loader routine builders.
     *
     * @param activity the Loader Activity.
     * @return the Context based builder.
     */
    @NotNull
    public static LoaderBuilderCompat on(@NotNull final FragmentActivity activity) {
        return on(loaderFrom(activity));
    }

    /**
     * Returns a Context based builder of Loader routine builders.
     *
     * @param activity the Loader Activity.
     * @param context  the Context used to get the application one.
     * @return the Context based builder.
     */
    @NotNull
    public static LoaderBuilderCompat on(@NotNull final FragmentActivity activity,
            @NotNull final Context context) {
        return on(loaderFrom(activity, context));
    }

    /**
     * Returns a Context based builder of Service routine builders.
     *
     * @param context the Service Context.
     * @return the Context based builder.
     */
    @NotNull
    public static ServiceBuilder on(@NotNull final Context context) {
        return on(serviceFrom(context));
    }

    /**
     * Returns a Context based builder of Service routine builders.
     *
     * @param context      the Service Context.
     * @param serviceClass the Service class.
     * @return the Context based builder.
     */
    @NotNull
    public static ServiceBuilder on(@NotNull final Context context,
            @NotNull final Class<? extends InvocationService> serviceClass) {
        return on(serviceFrom(context, serviceClass));
    }

    /**
     * Returns a Context based builder of Service routine builders.
     *
     * @param context the Service Context.
     * @param service the Service Intent.
     * @return the Context based builder.
     */
    @NotNull
    public static ServiceBuilder on(@NotNull final Context context, @NotNull final Intent service) {
        return on(serviceFrom(context, service));
    }

    /**
     * Returns a Context based builder of Loader routine builders.
     *
     * @param fragment the Loader Fragment.
     * @return the Context based builder.
     */
    @NotNull
    public static LoaderBuilderCompat on(@NotNull final Fragment fragment) {
        return on(loaderFrom(fragment));
    }

    /**
     * Returns a Context based builder of Loader routine builders.
     *
     * @param fragment the Loader Fragment.
     * @param context  the Context used to get the application one.
     * @return the Context based builder.
     */
    @NotNull
    public static LoaderBuilderCompat on(@NotNull final Fragment fragment,
            @NotNull final Context context) {
        return on(loaderFrom(fragment, context));
    }

    /**
     * Returns a Context based builder of Loader routine builders.
     *
     * @param context the Loader context.
     * @return the Context based builder.
     */
    @NotNull
    public static LoaderBuilderCompat on(@NotNull final LoaderContextCompat context) {
        return new LoaderBuilderCompat(context);
    }

    /**
     * Returns a Context based builder of Service routine builders.
     *
     * @param context the Service context.
     * @return the Context based builder.
     */
    @NotNull
    public static ServiceBuilder on(@NotNull final ServiceContext context) {
        return new ServiceBuilder(context) {};
    }

    /**
     * Returns a stream routine builder.
     *
     * @param <IN> the input data type.
     * @return the routine builder instance.
     */
    @NotNull
    public static <IN> LoaderStreamBuilderCompat<IN, IN> withStream() {
        return JRoutineLoaderStreamCompat.withStream();
    }
}

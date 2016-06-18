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

package com.github.dm.jrt.android.v11;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;

import com.github.dm.jrt.JRoutine;
import com.github.dm.jrt.android.ServiceBuilder;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.stream.LoaderStreams;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.android.v11.core.LoaderContext.loaderFrom;

/**
 * Class acting as a fa&ccedil;ade of all the JRoutine library features, specific to the Android
 * platform.
 * <p>
 * Created by davide-maestroni on 03/06/2016.
 */
public class JRoutineAndroid extends LoaderStreams {

    /**
     * Avoid explicit instantiation.
     */
    protected JRoutineAndroid() {
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
     * Returns a context based builder of loader routine builders.
     *
     * @param activity the loader activity.
     * @return the context based builder.
     */
    @NotNull
    public static LoaderBuilder with(@NotNull final Activity activity) {
        return with(loaderFrom(activity));
    }

    /**
     * Returns a context based builder of loader routine builders.
     *
     * @param activity the loader activity.
     * @param context  the context used to get the application one.
     * @return the context based builder.
     */
    @NotNull
    public static LoaderBuilder with(@NotNull final Activity activity,
            @NotNull final Context context) {
        return with(loaderFrom(activity, context));
    }

    /**
     * Returns a context based builder of service routine builders.
     *
     * @param context the service context.
     * @return the context based builder.
     */
    @NotNull
    public static ServiceBuilder with(@NotNull final Context context) {
        return with(serviceFrom(context));
    }

    /**
     * Returns a context based builder of service routine builders.
     *
     * @param context      the service context.
     * @param serviceClass the service class.
     * @return the context based builder.
     */
    @NotNull
    public static ServiceBuilder with(@NotNull final Context context,
            @NotNull final Class<? extends InvocationService> serviceClass) {
        return with(serviceFrom(context, serviceClass));
    }

    /**
     * Returns a context based builder of service routine builders.
     *
     * @param context the service context.
     * @param service the service intent.
     * @return the context based builder.
     */
    @NotNull
    public static ServiceBuilder with(@NotNull final Context context,
            @NotNull final Intent service) {
        return with(serviceFrom(context, service));
    }

    /**
     * Returns a context based builder of loader routine builders.
     *
     * @param fragment the loader fragment.
     * @return the context based builder.
     */
    @NotNull
    public static LoaderBuilder with(@NotNull final Fragment fragment) {
        return with(loaderFrom(fragment));
    }

    /**
     * Returns a context based builder of loader routine builders.
     *
     * @param fragment the loader fragment.
     * @param context  the context used to get the application one.
     * @return the context based builder.
     */
    @NotNull
    public static LoaderBuilder with(@NotNull final Fragment fragment,
            @NotNull final Context context) {
        return with(loaderFrom(fragment, context));
    }

    /**
     * Returns a context based builder of loader routine builders.
     *
     * @param context the loader context.
     * @return the context based builder.
     */
    @NotNull
    public static LoaderBuilder with(@NotNull final LoaderContext context) {
        return new LoaderBuilder(context);
    }

    /**
     * Returns a context based builder of service routine builders.
     *
     * @param context the service context.
     * @return the context based builder.
     */
    @NotNull
    public static ServiceBuilder with(@NotNull final ServiceContext context) {
        return new ServiceBuilder(context) {};
    }
}

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
import android.content.Intent;

import com.gh.bmd.jrt.android.service.RoutineService;

import javax.annotation.Nonnull;

/**
 * Class representing an Android service context.
 * <p/>
 * Created by davide on 11/07/15.
 */
public abstract class ServiceContext {

    /**
     * Avoid direct instantiation.
     */
    private ServiceContext() {

    }

    /**
     * Returns a context based on the specified instance.<br/>
     * The default {@link com.gh.bmd.jrt.android.service.RoutineService RoutineService} class will
     * be employed.
     *
     * @param context the context.
     * @return the service context.
     */
    @Nonnull
    public static ServiceContext serviceFrom(@Nonnull final Context context) {

        return serviceFrom(context, RoutineService.class);
    }

    /**
     * Returns a context based on the specified instance, employing a service of the specified
     * type.
     *
     * @param context      the context.
     * @param serviceClass the service type.
     * @return the service context.
     */
    @Nonnull
    public static ServiceContext serviceFrom(@Nonnull final Context context,
            @Nonnull final Class<? extends RoutineService> serviceClass) {

        return serviceFrom(context, new Intent(context, serviceClass));
    }

    /**
     * Returns a context based on the specified instance, employing the specified intent to start
     * the service.
     *
     * @param context the context.
     * @param service the service intent.
     * @return the service context.
     */
    @Nonnull
    public static ServiceContext serviceFrom(@Nonnull final Context context,
            @Nonnull final Intent service) {

        return new IntentServiceContext(context, service);
    }

    /**
     * Returns the routine context.
     *
     * @return the context.
     */
    @Nonnull
    public abstract Context getRoutineContext();

    /**
     * Returns the service intent.
     *
     * @return the intent.
     */
    @Nonnull
    public abstract Intent getServiceIntent();

    /**
     * Service context wrapping a service intent.
     */
    private static class IntentServiceContext extends ServiceContext {

        private final Context mContext;

        private final Intent mIntent;

        /**
         * Constructor.
         *
         * @param context the context.
         * @param service the service intent.
         */
        @SuppressWarnings("ConstantConditions")
        private IntentServiceContext(@Nonnull final Context context,
                @Nonnull final Intent service) {

            if (service == null) {

                throw new NullPointerException("the service intent must not be null");
            }

            mContext = context.getApplicationContext();
            mIntent = service;
        }

        @Nonnull
        @Override
        public Context getRoutineContext() {

            return mContext;
        }

        @Nonnull
        @Override
        public Intent getServiceIntent() {

            return mIntent;
        }
    }
}

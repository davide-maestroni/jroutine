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

package com.github.dm.jrt.android.core;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Class representing an Android service context.
 * <p>
 * No strong reference to the wrapped contexts will be retained by this class implementations. So,
 * it is up to the caller to ensure that they are not garbage collected before being called.
 * <p>
 * Created by davide-maestroni on 07/11/2015.
 */
public abstract class ServiceContext {

    /**
     * Avoid explicit instantiation.
     */
    private ServiceContext() {

    }

    /**
     * Returns a context based on the specified instance.<br>
     * The default {@link com.github.dm.jrt.android.core.service.InvocationService
     * InvocationService} class will be employed.
     *
     * @param context the context.
     * @return the service context.
     */
    @NotNull
    public static ServiceContext serviceFrom(@NotNull final Context context) {

        return serviceFrom(context, InvocationService.class);
    }

    /**
     * Returns a context based on the specified instance, employing a service of the specified
     * type.
     *
     * @param context      the context.
     * @param serviceClass the service type.
     * @return the service context.
     */
    @NotNull
    public static ServiceContext serviceFrom(@NotNull final Context context,
            @NotNull final Class<? extends InvocationService> serviceClass) {

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
    @NotNull
    public static ServiceContext serviceFrom(@NotNull final Context context,
            @NotNull final Intent service) {

        return new IntentServiceContext(context, service);
    }

    private static boolean bundleEquals(@Nullable final Bundle bundle1,
            @Nullable final Bundle bundle2) {

        if (bundle1 == bundle2) {
            return true;
        }

        if ((bundle1 == null) || (bundle2 == null)) {
            return false;
        }

        if (bundle1.size() != bundle2.size()) {
            return false;
        }

        for (final String key : bundle1.keySet()) {
            final Object value1 = bundle1.get(key);
            final Object value2 = bundle2.get(key);
            if ((value1 instanceof Bundle) && (value2 instanceof Bundle) &&
                    !bundleEquals((Bundle) value1, (Bundle) value2)) {
                return false;

            } else if (value1 == null) {
                if ((value2 != null) || !bundle2.containsKey(key)) {
                    return false;
                }

            } else if (!value1.equals(value2)) {
                return false;
            }
        }

        return true;
    }

    private static int bundleHashCode(@Nullable final Bundle bundle) {

        if (bundle == null) {
            return 0;
        }

        int result = 0;
        for (final String key : bundle.keySet()) {
            final Object value = bundle.get(key);
            if (value instanceof Bundle) {
                result = 31 * result + bundleHashCode((Bundle) value);

            } else if (value == null) {
                result = 31 * result;

            } else {
                result = 31 * result + value.hashCode();
            }
        }

        return result;
    }

    /**
     * Returns the service context.
     *
     * @return the context.
     */
    @Nullable
    public abstract Context getServiceContext();

    /**
     * Returns the service intent.
     *
     * @return the intent.
     */
    @NotNull
    public abstract Intent getServiceIntent();

    /**
     * Service context wrapping a service intent.
     */
    private static class IntentServiceContext extends ServiceContext {

        private final WeakReference<Context> mContext;

        private final Intent mIntent;

        /**
         * Constructor.
         *
         * @param context the context.
         * @param service the service intent.
         */
        private IntentServiceContext(@NotNull final Context context,
                @NotNull final Intent service) {

            mContext = new WeakReference<Context>(
                    ConstantConditions.notNull("service context", context));
            mIntent = ConstantConditions.notNull("service intent", service);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof IntentServiceContext)) {
                return false;
            }

            final IntentServiceContext that = (IntentServiceContext) o;
            final Context referent = mContext.get();
            return (referent != null) && referent.equals(that.mContext.get())
                    && mIntent.filterEquals(that.mIntent) && bundleEquals(mIntent.getExtras(),
                    that.mIntent.getExtras());
        }

        @Override
        public int hashCode() {

            final Context referent = mContext.get();
            int result = (referent != null) ? referent.hashCode() : 0;
            result = 31 * result + mIntent.filterHashCode();
            result = 31 * result + bundleHashCode(mIntent.getExtras());
            return result;
        }

        @Nullable
        @Override
        public Context getServiceContext() {

            return mContext.get();
        }

        @NotNull
        @Override
        public Intent getServiceIntent() {

            return mIntent;
        }
    }
}

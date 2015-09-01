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
package com.github.dm.jrt.android.core;

import android.content.Context;
import android.content.Intent;

import com.github.dm.jrt.android.service.RoutineService;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class representing an Android service context.
 * <p/>
 * No strong reference to the wrapped contexts will be retained by this class implementations. So,
 * it is up to the caller to ensure that they are not garbage collected before time.
 * <p/>
 * Created by davide-maestroni on 07/11/15.
 */
public abstract class ServiceContext {

    /**
     * Avoid direct instantiation.
     */
    private ServiceContext() {

    }

    /**
     * Returns a context based on the specified instance.<br/>
     * The default {@link com.github.dm.jrt.android.service.RoutineService RoutineService} class
     * will
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
    @Nonnull
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
        @SuppressWarnings("ConstantConditions")
        private IntentServiceContext(@Nonnull final Context context,
                @Nonnull final Intent service) {

            if (context == null) {

                throw new NullPointerException("the service context must not be null");
            }

            if (service == null) {

                throw new NullPointerException("the service intent must not be null");
            }

            mContext = new WeakReference<Context>(context);
            mIntent = service;
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
            return (referent != null) && referent.equals(that.mContext.get()) && mIntent.equals(
                    that.mIntent);
        }

        @Override
        public int hashCode() {

            final Context referent = mContext.get();
            int result = (referent != null) ? referent.hashCode() : 0;
            result = 31 * result + mIntent.hashCode();
            return result;
        }

        @Nullable
        @Override
        public Context getServiceContext() {

            return mContext.get();
        }

        @Nonnull
        @Override
        public Intent getServiceIntent() {

            return mIntent;
        }
    }
}

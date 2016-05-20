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

package com.github.dm.jrt.android.retrofit.service;

import com.github.dm.jrt.android.channel.ParcelableSelectable;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.retrofit.ServiceCallInvocation;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.OkHttpClient;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Implementation of a service properly handling Retrofit requests.
 * <p>
 * Created by davide-maestroni on 05/18/2016.
 */
public abstract class RetrofitInvocationService extends InvocationService {

    /**
     * Constructor.
     */
    public RetrofitInvocationService() {

    }

    /**
     * Constructor.
     *
     * @param log      the log instance.
     * @param logLevel the log level.
     */
    @SuppressWarnings("unused")
    public RetrofitInvocationService(@Nullable final Log log, @Nullable final Level logLevel) {

        super(log, logLevel);
    }

    @NotNull
    @Override
    public ContextInvocationFactory<?, ?> getInvocationFactory(
            @NotNull final Class<? extends ContextInvocation<?, ?>> targetClass,
            @Nullable final Object... args) throws Exception {

        if (targetClass == ServiceCallInvocation.class) {
            return new ServiceCallInvocationFactory(getHttpClient());
        }

        return super.getInvocationFactory(targetClass, args);
    }

    /**
     * Returns the client to be used to fulfill HTTP requests.
     *
     * @return the OkHttp client.
     */
    @NotNull
    protected abstract OkHttpClient getHttpClient();

    /**
     * Factory of invocations handling OkHttp requests.
     */
    private static class ServiceCallInvocationFactory extends
            ContextInvocationFactory<ParcelableSelectable<Object>, ParcelableSelectable<Object>> {

        private final OkHttpClient mClient;

        /**
         * Constructor.
         *
         * @param client the OkHttp client.
         */
        private ServiceCallInvocationFactory(@NotNull final OkHttpClient client) {

            super(asArgs(ConstantConditions.notNull("http client instance", client)));
            mClient = client;
        }

        @NotNull
        @Override
        public ContextInvocation<ParcelableSelectable<Object>, ParcelableSelectable<Object>>
        newInvocation() {

            return new ServiceCallInvocation(mClient);
        }
    }
}

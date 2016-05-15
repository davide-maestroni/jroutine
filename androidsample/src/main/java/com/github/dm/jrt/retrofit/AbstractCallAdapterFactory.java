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

package com.github.dm.jrt.retrofit;

import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Abstract implementation of a call adapter factory providing boilerplate code for common features,
 * like extraction of the response type and adapter instantiation.
 * <p>
 * Created by davide-maestroni on 03/26/2016.
 *
 * @param <OUT> the output data type.
 * @param <T>   the return type.
 */
public abstract class AbstractCallAdapterFactory<OUT, T> extends CallAdapter.Factory {

    private final int mParameterIndex;

    private final Class<T> mReturnType;

    /**
     * Constructor.
     *
     * @param returnType     the return type.
     * @param parameterIndex the index of the response type in the return type generic parameters
     *                       (-1 if not applicable).
     */
    protected AbstractCallAdapterFactory(final Class<T> returnType, final int parameterIndex) {

        mReturnType = returnType;
        mParameterIndex = parameterIndex;
    }

    @Override
    public CallAdapter<?> get(final Type returnType, final Annotation[] annotations,
            final Retrofit retrofit) {

        final Type responseType;
        if (returnType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) returnType;
            if (parameterizedType.getRawType() != mReturnType) {
                return null;
            }

            final int parameterIndex = mParameterIndex;
            if (parameterIndex >= 0) {
                responseType = parameterizedType.getActualTypeArguments()[parameterIndex];
            } else {

                responseType = Object.class;
            }

        } else if (returnType instanceof Class) {
            if (returnType != mReturnType) {
                return null;
            }

            responseType = Object.class;

        } else {
            return null;
        }

        final Routine<? extends Call<OUT>, OUT> routine =
                getRoutine(responseType, annotations, retrofit);
        return new CallAdapterInternal(ConstantConditions.notNull("builder", routine),
                responseType);
    }

    /**
     * Adapts the specified call and returns the adapted object.
     *
     * @param routine the routine instance
     * @param call    the call instance.
     * @param <C>     the call type.
     * @return the adapted instance.
     */
    @NotNull
    protected abstract <C extends Call<OUT>> T adapt(@NotNull Routine<C, OUT> routine,
            @NotNull Call<?> call);

    /**
     * Gets the routine used to handle the invocation of the method with the specified response type
     * and annotations.
     *
     * @param responseType the response type.
     * @param annotations  the method annotations.
     * @param retrofit     the retrofit instance.
     * @return the routine instance.
     */
    @NotNull
    protected abstract Routine<? extends Call<OUT>, OUT> getRoutine(@NotNull Type responseType,
            @NotNull Annotation[] annotations, @NotNull Retrofit retrofit);

    /**
     * Internal adapter implementation.
     */
    private class CallAdapterInternal implements CallAdapter<T> {

        private final Type mResponseType;

        private final Routine<? extends Call<OUT>, OUT> mRoutine;

        /**
         * @param routine      the routine instance.
         * @param responseType the response type.
         */
        private CallAdapterInternal(@NotNull final Routine<? extends Call<OUT>, OUT> routine,
                @NotNull final Type responseType) {

            mResponseType = responseType;
            mRoutine = routine;
        }

        @Override
        public Type responseType() {

            return mResponseType;
        }

        @Override
        public <R> T adapt(final Call<R> call) {

            return AbstractCallAdapterFactory.this.adapt(mRoutine, call);
        }
    }
}

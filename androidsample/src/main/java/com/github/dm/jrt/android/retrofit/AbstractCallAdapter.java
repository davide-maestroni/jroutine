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

package com.github.dm.jrt.android.retrofit;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.invocation.ComparableInvocationFactory;
import com.github.dm.jrt.core.invocation.FilterInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Abstract implementation of a call adapter factory providing boilerplate code for common features,
 * like extraction of the response type and adapter instantiation.
 * <p/>
 * It is possible to customize the adapter instantiation by overriding the
 * {@link #newAdapter(Type, Annotation[], Retrofit)} method. In such case the implementation of
 * {@link #adapt(ComparableCall, Annotation[], Retrofit)} is not mandatory.<br/>
 * On the contrary, the {@code adapt()} method must be implemented by the inheriting class, and will
 * be called at each new network request.
 * <p/>
 * Created by davide-maestroni on 03/26/2016.
 *
 * @param <T> the return type.
 */
public abstract class AbstractCallAdapter<T> extends CallAdapter.Factory {

    private static final CallInvocationFactory<Object> sFactory = new CallInvocationFactory<>();

    private final int mParameterIndex;

    private final Class<?> mReturnType;

    /**
     * Constructor.
     *
     * @param returnType     the return type.
     * @param parameterIndex the index of the response type in the return type generic parameters
     *                       (-1 if not applicable).
     */
    protected AbstractCallAdapter(final Class<T> returnType, final int parameterIndex) {

        mReturnType = returnType;
        mParameterIndex = parameterIndex;
    }

    @Override
    public CallAdapter<T> get(final Type returnType, final Annotation[] annotations,
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

        return newAdapter(responseType, annotations, retrofit);
    }

    /**
     * Adapts the specified call and returns the adapted object.
     *
     * @param call        the call instance.
     * @param annotations the method annotations.
     * @param retrofit    the retrofit instance
     * @return the adapted instance.
     * @throws java.lang.UnsupportedOperationException if this method has not been implemented by
     *                                                 the inheriting class.
     */
    @NotNull
    protected T adapt(@NotNull final ComparableCall<Object> call,
            @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) throws
            UnsupportedOperationException {

        try {
            final Method method =
                    getClass().getMethod("adapt", ComparableCall.class, Annotation[].class,
                            Retrofit.class);
            throw new UnsupportedOperationException("the method " + method + " is not implemented");

        } catch (final NoSuchMethodException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Returns the default invocation factory used to handle calls.
     *
     * @return the factory instance.
     */
    @NotNull
    protected InvocationFactory<ComparableCall<Object>, Object> getFactory() {

        return sFactory;
    }

    /**
     * Creates a new adapter
     *
     * @param responseType the response type.
     * @param annotations  the method annotations.
     * @param retrofit     the retrofit instance
     * @return the adapter instance.
     */
    @NotNull
    protected CallAdapter<T> newAdapter(@NotNull final Type responseType,
            @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) {

        return new CallAdapter<T>() {

            @Override
            public Type responseType() {

                return responseType;
            }

            @Override
            public <R> T adapt(final Call<R> call) {

                return AbstractCallAdapter.this.adapt(ComparableCall.wrap(call),
                        annotations.clone(), retrofit);
            }
        };
    }

    /**
     * Implementation of an invocation handling call instances.
     *
     * @param <T> the response type.
     */
    private static class CallInvocation<T> extends FilterInvocation<ComparableCall<T>, T> {

        @Override
        public void onInput(final ComparableCall<T> input,
                @NotNull final ResultChannel<T> result) throws Exception {

            result.pass(input.execute().body());
        }
    }

    /**
     * Implementation of a factory of invocations handling call instances.
     *
     * @param <T> the response type.
     */
    private static class CallInvocationFactory<T>
            extends ComparableInvocationFactory<ComparableCall<T>, T> {

        /**
         * Constructor.
         */
        private CallInvocationFactory() {

            super(null);
        }

        @NotNull
        @Override
        public Invocation<ComparableCall<T>, T> newInvocation() {

            return new CallInvocation<>();
        }
    }
}

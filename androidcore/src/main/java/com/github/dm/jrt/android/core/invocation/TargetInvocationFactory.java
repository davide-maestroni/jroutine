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

package com.github.dm.jrt.android.core.invocation;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.cloneArgs;

/**
 * Class representing a context invocation factory target.
 * <p>
 * Note that, in case a class not representing a {@link ContextInvocation} is passed to the factory,
 * the specified invocation class will be passed as the first argument to a special context
 * invocation in order to be automatically instantiated via reflection.
 * <br>
 * The latter class will inherit from {@link ContextInvocationWrapper}.
 * <br>
 * It is possible to avoid that, by creating a target invocation factory of a common invocation by
 * defining a specialized class like:
 * <pre>
 *     <code>
 *
 *         public class MyInvocationWrapper&lt;IN, OUT&gt;
 *                 extends ContextInvocationWrapper&lt;IN, OUT&gt; {
 *
 *             public MyInvocationWrapper(final String arg) {
 *
 *                 super(new MyInvocation&lt;IN, OUT&gt;(arg));
 *             }
 *         }
 *     </code>
 * </pre>
 * <p>
 * Created by davide-maestroni on 08/20/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class TargetInvocationFactory<IN, OUT> extends DeepEqualObject
        implements Parcelable {

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    private TargetInvocationFactory(@Nullable final Object[] args) {

        super(args);
    }

    /**
     * Returns a target based on the specified invocation class.
     * <br>
     * The method accepts also classes implementing {@link ContextInvocation}.
     * <p>
     * Note that, in case a class not representing a {@link ContextInvocation} is passed to the
     * factory, the specified invocation class will be passed as the first argument to a special
     * context invocation in order to be automatically instantiated via reflection.
     *
     * @param targetClass the target invocation class.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends Invocation<IN, OUT>> targetClass) {

        return factoryOf(targetClass, (Object[]) null);
    }

    /**
     * Returns a target based on the specified invocation class.
     * <br>
     * The method accepts also classes implementing {@link ContextInvocation}.
     * <p>
     * Note that, in case a class not representing a {@link ContextInvocation} is passed to the
     * factory, the specified invocation class will be passed as the first argument to a special
     * context invocation in order to be automatically instantiated via reflection.
     *
     * @param targetClass the target invocation class.
     * @param factoryArgs the invocation factory arguments.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends Invocation<IN, OUT>> targetClass,
            @Nullable final Object... factoryArgs) {

        if (ContextInvocation.class.isAssignableFrom(targetClass)) {
            return new DefaultTargetInvocationFactory<IN, OUT>(
                    (Class<? extends ContextInvocation<IN, OUT>>) targetClass, factoryArgs);
        }

        final Class<? extends TargetInvocationWrapper<IN, OUT>> targetWrapper =
                new ClassToken<TargetInvocationWrapper<IN, OUT>>() {}.getRawClass();
        return new DefaultTargetInvocationFactory<IN, OUT>(targetWrapper,
                asArgs(targetClass, factoryArgs));
    }

    /**
     * Returns a target based on the specified invocation token.
     * <br>
     * The method accepts also classes implementing {@link ContextInvocation}.
     * <p>
     * Note that, in case a class not representing a {@link ContextInvocation} is passed to the
     * factory, the specified invocation class will be passed as the first argument to a special
     * context invocation in order to be automatically instantiated via reflection.
     *
     * @param targetToken the target invocation token.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends Invocation<IN, OUT>> targetToken) {

        return factoryOf(targetToken.getRawClass());
    }

    /**
     * Returns a target based on the specified invocation token.
     * <br>
     * The method accepts also classes implementing {@link ContextInvocation}.
     * <p>
     * Note that, in case a class not representing a {@link ContextInvocation} is passed to the
     * factory, the specified invocation class will be passed as the first argument to a special
     * context invocation in order to be automatically instantiated via reflection.
     *
     * @param targetToken the target invocation token.
     * @param factoryArgs the invocation factory arguments.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends Invocation<IN, OUT>> targetToken,
            @Nullable final Object... factoryArgs) {

        return factoryOf(targetToken.getRawClass(), factoryArgs);
    }

    /**
     * Returns a target based on the specified invocation.
     * <br>
     * The method accepts also instances implementing {@link ContextInvocation}.
     * <p>
     * Note that, in case a class not representing a {@link ContextInvocation} is passed to the
     * factory, the specified invocation class will be passed as the first argument to a special
     * context invocation in order to be automatically instantiated via reflection.
     *
     * @param targetInvocation the target invocation.
     * @param <IN>             the input data type.
     * @param <OUT>            the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Invocation<IN, OUT> targetInvocation) {

        return factoryOf(tokenOf(targetInvocation));
    }

    /**
     * Returns a target based on the specified invocation.
     * <br>
     * The method accepts also instances implementing {@link ContextInvocation}.
     * <p>
     * Note that, in case a class not representing a {@link ContextInvocation} is passed to the
     * factory, the specified invocation class will be passed as the first argument to a special
     * context invocation in order to be automatically instantiated via reflection.
     *
     * @param targetInvocation the target invocation.
     * @param factoryArgs      the invocation factory arguments.
     * @param <IN>             the input data type.
     * @param <OUT>            the output data type.
     * @return the invocation factory target.
     */
    @NotNull
    public static <IN, OUT> TargetInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Invocation<IN, OUT> targetInvocation,
            @Nullable final Object... factoryArgs) {

        return factoryOf(tokenOf(targetInvocation), factoryArgs);
    }

    /**
     * Returns the factory arguments.
     *
     * @return the factory arguments.
     */
    @NotNull
    public abstract Object[] getFactoryArgs();

    /**
     * Returns the target class.
     *
     * @return the target class.
     */
    @NotNull
    public abstract Class<? extends ContextInvocation<IN, OUT>> getInvocationClass();

    /**
     * Invocation factory target implementation.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DefaultTargetInvocationFactory<IN, OUT>
            extends TargetInvocationFactory<IN, OUT> {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<DefaultTargetInvocationFactory> CREATOR =
                new Creator<DefaultTargetInvocationFactory>() {

                    public DefaultTargetInvocationFactory createFromParcel(final Parcel source) {

                        return new DefaultTargetInvocationFactory(source);
                    }

                    public DefaultTargetInvocationFactory[] newArray(final int size) {

                        return new DefaultTargetInvocationFactory[size];
                    }
                };

        private final Object[] mFactoryArgs;

        private final Class<? extends ContextInvocation<IN, OUT>> mTargetClass;

        /**
         * Constructor.
         *
         * @param source the source parcel.
         */
        @SuppressWarnings("unchecked")
        private DefaultTargetInvocationFactory(@NotNull final Parcel source) {

            this((Class<? extends ContextInvocation<IN, OUT>>) source.readSerializable(),
                    source.readArray(TargetInvocationFactory.class.getClassLoader()));
        }

        /**
         * Constructor.
         *
         * @param targetClass the target invocation class.
         * @param factoryArgs the invocation factory arguments.
         */
        private DefaultTargetInvocationFactory(
                @NotNull final Class<? extends ContextInvocation<IN, OUT>> targetClass,
                @Nullable final Object[] factoryArgs) {

            super(asArgs(targetClass, cloneArgs(factoryArgs)));
            mTargetClass = ConstantConditions.notNull("target invocation class", targetClass);
            mFactoryArgs = cloneArgs(factoryArgs);
        }

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(final Parcel dest, final int flags) {

            dest.writeSerializable(mTargetClass);
            dest.writeArray(mFactoryArgs);
        }

        @NotNull
        @Override
        public Object[] getFactoryArgs() {

            return mFactoryArgs;
        }

        @NotNull
        @Override
        public Class<? extends ContextInvocation<IN, OUT>> getInvocationClass() {

            return mTargetClass;
        }
    }

    /**
     * Context invocation wrapping a base one.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    @SuppressWarnings("unused")
    private static class TargetInvocationWrapper<IN, OUT>
            extends ContextInvocationWrapper<IN, OUT> {

        /**
         * Constructor.
         *
         * @param invocationClass the wrapped invocation class.
         * @throws java.lang.Exception if the invocation instantiation failed.
         */
        public TargetInvocationWrapper(
                @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass) throws
                Exception {

            this(invocationClass, (Object[]) null);
        }

        /**
         * Constructor.
         *
         * @param invocationClass the wrapped invocation class.
         * @param invocationArgs  the invocation constructor arguments
         * @throws java.lang.Exception if the invocation instantiation failed.
         */
        public TargetInvocationWrapper(
                @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass,
                @Nullable final Object... invocationArgs) throws Exception {

            super(newInvocation(invocationClass, invocationArgs));
        }

        @NotNull
        private static <IN, OUT> Invocation<IN, OUT> newInvocation(
                @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass,
                @Nullable final Object... invocationArgs) throws Exception {

            final Object[] args = (invocationArgs != null) ? invocationArgs : Reflection.NO_ARGS;
            return Reflection.findConstructor(invocationClass, args).newInstance(args);
        }
    }
}

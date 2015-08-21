package com.gh.bmd.jrt.android.core;

import android.os.Parcel;
import android.os.Parcelable;

import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.Reflection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * // TODO: 20/08/15 javadoc
 * <p/>
 * Created by davide-maestroni on 20/08/15.
 */
public abstract class InvocationFactoryTarget<INPUT, OUTPUT> implements Parcelable {

    private InvocationFactoryTarget() {

    }

    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> targetClass) {

        return new DefaultInvocationFactoryTarget<INPUT, OUTPUT>(targetClass, null);
    }

    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> targetClass,
            @Nullable final Object... factoryArgs) {

        return new DefaultInvocationFactoryTarget<INPUT, OUTPUT>(targetClass, factoryArgs);
    }

    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> targetToken) {

        return new DefaultInvocationFactoryTarget<INPUT, OUTPUT>(targetToken.getRawClass(), null);
    }

    @Nonnull
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> targetToken,
            @Nullable final Object... factoryArgs) {

        return new DefaultInvocationFactoryTarget<INPUT, OUTPUT>(targetToken.getRawClass(),
                                                                 factoryArgs);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ContextInvocation<INPUT, OUTPUT> targetInvocation) {

        return new DefaultInvocationFactoryTarget<INPUT, OUTPUT>(
                (Class<? extends ContextInvocation<INPUT, OUTPUT>>) targetInvocation.getClass(),
                null);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <INPUT, OUTPUT> InvocationFactoryTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ContextInvocation<INPUT, OUTPUT> targetInvocation,
            @Nullable final Object... factoryArgs) {

        return new DefaultInvocationFactoryTarget<INPUT, OUTPUT>(
                (Class<? extends ContextInvocation<INPUT, OUTPUT>>) targetInvocation.getClass(),
                factoryArgs);
    }

    @Nonnull
    public abstract Object[] getFactoryArgs();

    /**
     * Returns the target class.
     *
     * @return the target class.
     */
    @Nonnull
    public abstract Class<? extends ContextInvocation<INPUT, OUTPUT>> getInvocationClass();

    private static class DefaultInvocationFactoryTarget<INPUT, OUTPUT>
            extends InvocationFactoryTarget<INPUT, OUTPUT> {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<DefaultInvocationFactoryTarget> CREATOR =
                new Creator<DefaultInvocationFactoryTarget>() {

                    public DefaultInvocationFactoryTarget createFromParcel(
                            @Nonnull final Parcel source) {

                        return new DefaultInvocationFactoryTarget(source);
                    }

                    @Nonnull
                    public DefaultInvocationFactoryTarget[] newArray(final int size) {

                        return new DefaultInvocationFactoryTarget[size];
                    }
                };

        private final Object[] mFactoryArgs;

        private final Class<? extends ContextInvocation<INPUT, OUTPUT>> mTargetClass;

        @SuppressWarnings("unchecked")
        private DefaultInvocationFactoryTarget(@Nonnull final Parcel source) {

            this((Class<? extends ContextInvocation<INPUT, OUTPUT>>) source.readSerializable(),
                 source.readArray(InvocationFactoryTarget.class.getClassLoader()));
        }

        @SuppressWarnings("ConstantConditions")
        private DefaultInvocationFactoryTarget(
                @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> targetClass,
                @Nullable final Object[] factoryArgs) {


            if (targetClass == null) {

                throw new NullPointerException("the target class must not be null");
            }

            mTargetClass = targetClass;
            mFactoryArgs = (factoryArgs != null) ? factoryArgs.clone() : Reflection.NO_ARGS;
        }

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(final Parcel dest, final int flags) {

            dest.writeSerializable(mTargetClass);
            dest.writeArray(mFactoryArgs);
        }

        @Nonnull
        @Override
        public Object[] getFactoryArgs() {

            return mFactoryArgs;
        }

        @Nonnull
        @Override
        public Class<? extends ContextInvocation<INPUT, OUTPUT>> getInvocationClass() {

            return mTargetClass;
        }
    }
}

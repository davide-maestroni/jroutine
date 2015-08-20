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
public abstract class ServiceTarget implements Parcelable {

    private ServiceTarget() {

    }

    @Nonnull
    public static ClassServiceTarget targetClass(@Nonnull final Class<?> targetClass) {

        return new ClassServiceTarget(targetClass);
    }

    @Nonnull
    public static <INPUT, OUTPUT> InvocationServiceTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> targetClass) {

        return new InvocationServiceTarget<INPUT, OUTPUT>(targetClass, null);
    }

    @Nonnull
    public static <INPUT, OUTPUT> InvocationServiceTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> targetClass,
            @Nullable final Object... factoryArgs) {

        return new InvocationServiceTarget<INPUT, OUTPUT>(targetClass, factoryArgs);
    }

    @Nonnull
    public static <INPUT, OUTPUT> InvocationServiceTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> targetToken) {

        return new InvocationServiceTarget<INPUT, OUTPUT>(targetToken.getRawClass(), null);
    }

    @Nonnull
    public static <INPUT, OUTPUT> InvocationServiceTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ClassToken<? extends ContextInvocation<INPUT, OUTPUT>> targetToken,
            @Nullable final Object... factoryArgs) {

        return new InvocationServiceTarget<INPUT, OUTPUT>(targetToken.getRawClass(), factoryArgs);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <INPUT, OUTPUT> InvocationServiceTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ContextInvocation<INPUT, OUTPUT> targetInvocation) {

        return new InvocationServiceTarget<INPUT, OUTPUT>(
                (Class<? extends ContextInvocation<INPUT, OUTPUT>>) targetInvocation.getClass(),
                null);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <INPUT, OUTPUT> InvocationServiceTarget<INPUT, OUTPUT> targetInvocation(
            @Nonnull final ContextInvocation<INPUT, OUTPUT> targetInvocation,
            @Nullable final Object... factoryArgs) {

        return new InvocationServiceTarget<INPUT, OUTPUT>(
                (Class<? extends ContextInvocation<INPUT, OUTPUT>>) targetInvocation.getClass(),
                factoryArgs);
    }

    @Nonnull
    public static ObjectServiceTarget targetObject(@Nonnull final Class<?> targetClass) {

        return new ObjectServiceTarget(targetClass, null);
    }

    @Nonnull
    public static ObjectServiceTarget targetObject(@Nonnull final Class<?> targetClass,
            @Nullable final Object... factoryArgs) {

        return new ObjectServiceTarget(targetClass, factoryArgs);
    }

    @Nonnull
    public abstract Object[] getFactoryArgs();

    /**
     * Returns the target class.
     *
     * @return the target class.
     */
    @Nonnull
    public abstract Class<?> getTargetClass();

    public static class ClassServiceTarget extends ServiceTarget {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<ClassServiceTarget> CREATOR =
                new Creator<ClassServiceTarget>() {

                    public ClassServiceTarget createFromParcel(@Nonnull final Parcel source) {

                        return new ClassServiceTarget(source);
                    }

                    @Nonnull
                    public ClassServiceTarget[] newArray(final int size) {

                        return new ClassServiceTarget[size];
                    }
                };

        private final Class<?> mTargetClass;

        private ClassServiceTarget(@Nonnull final Parcel source) {

            this((Class<?>) source.readSerializable());
        }

        @SuppressWarnings("ConstantConditions")
        private ClassServiceTarget(@Nonnull final Class<?> targetClass) {

            if (targetClass == null) {

                throw new NullPointerException("the target class must not be null");
            }

            mTargetClass = targetClass;
        }

        public int describeContents() {

            return 0;
        }

        @Nonnull
        @Override
        public Object[] getFactoryArgs() {

            return Reflection.NO_ARGS;
        }

        public void writeToParcel(final Parcel dest, final int flags) {

            dest.writeSerializable(mTargetClass);
        }

        @Nonnull
        @Override
        public Class<?> getTargetClass() {

            return mTargetClass;
        }
    }

    public static class InvocationServiceTarget<INPUT, OUTPUT> extends ServiceTarget {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<InvocationServiceTarget> CREATOR =
                new Creator<InvocationServiceTarget>() {

                    public InvocationServiceTarget createFromParcel(@Nonnull final Parcel source) {

                        return new InvocationServiceTarget(source);
                    }

                    @Nonnull
                    public InvocationServiceTarget[] newArray(final int size) {

                        return new InvocationServiceTarget[size];
                    }
                };

        private final Object[] mFactoryArgs;

        private final Class<? extends ContextInvocation<INPUT, OUTPUT>> mTargetClass;

        @SuppressWarnings("unchecked")
        private InvocationServiceTarget(@Nonnull final Parcel source) {

            this((Class<? extends ContextInvocation<INPUT, OUTPUT>>) source.readSerializable(),
                 source.readArray(ServiceTarget.class.getClassLoader()));
        }

        @SuppressWarnings("ConstantConditions")
        private InvocationServiceTarget(
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
        public Class<? extends ContextInvocation<INPUT, OUTPUT>> getTargetClass() {

            return mTargetClass;
        }
    }

    public static class ObjectServiceTarget extends ServiceTarget {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<ObjectServiceTarget> CREATOR =
                new Creator<ObjectServiceTarget>() {

                    public ObjectServiceTarget createFromParcel(@Nonnull final Parcel source) {

                        return new ObjectServiceTarget(source);
                    }

                    @Nonnull
                    public ObjectServiceTarget[] newArray(final int size) {

                        return new ObjectServiceTarget[size];
                    }
                };

        private final Object[] mFactoryArgs;

        private final Class<?> mTargetClass;

        private ObjectServiceTarget(@Nonnull final Parcel source) {

            this((Class<?>) source.readSerializable(),
                 source.readArray(ServiceTarget.class.getClassLoader()));
        }

        @SuppressWarnings("ConstantConditions")
        private ObjectServiceTarget(@Nonnull final Class<?> targetClass,
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
        public Class<?> getTargetClass() {

            return mTargetClass;
        }
    }
}

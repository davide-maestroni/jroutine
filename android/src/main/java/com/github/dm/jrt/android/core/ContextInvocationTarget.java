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
import android.os.Parcel;
import android.os.Parcelable;

import com.github.dm.jrt.android.builder.FactoryContext;
import com.github.dm.jrt.channel.RoutineException;
import com.github.dm.jrt.core.InvocationTarget;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.util.Reflection;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.github.dm.jrt.util.Reflection.findConstructor;

/**
 * Class representing a context invocation target.
 * <p/>
 * Created by davide-maestroni on 08/21/2015.
 *
 * @param <TYPE> the target object type.
 */
public abstract class ContextInvocationTarget<TYPE> implements Parcelable {

    /**
     * Avoid direct instantiation.
     */
    private ContextInvocationTarget() {

    }

    /**
     * Returns a target based on the specified class.
     *
     * @param targetClass the target class.
     * @param <TYPE>      the target object type.
     * @return the context invocation target.
     */
    @Nonnull
    public static <TYPE> ClassContextInvocationTarget<TYPE> classOfType(
            @Nonnull final Class<TYPE> targetClass) {

        return new ClassContextInvocationTarget<TYPE>(targetClass);
    }

    /**
     * Returns a target based on the specified instance.<br/>
     * No argument will be passed to the object factory.
     *
     * @param targetClass the target class.
     * @param <TYPE>      the target object type.
     * @return the context invocation target.
     */
    @Nonnull
    public static <TYPE> ObjectContextInvocationTarget<TYPE> instanceOf(
            @Nonnull final Class<TYPE> targetClass) {

        return instanceOf(targetClass, (Object[]) null);
    }

    /**
     * Returns a target based on the specified instance.
     *
     * @param targetClass the target class.
     * @param factoryArgs the object factory arguments.
     * @param <TYPE>      the target object type.
     * @return the context invocation target.
     */
    @Nonnull
    public static <TYPE> ObjectContextInvocationTarget<TYPE> instanceOf(
            @Nonnull final Class<TYPE> targetClass, @Nullable final Object... factoryArgs) {

        return new ObjectContextInvocationTarget<TYPE>(targetClass, factoryArgs);
    }

    /**
     * Returns an invocation target based on the specified context.<br/>
     * Note that a new instance will be returned each time the method is invoked.
     *
     * @param context the target context.
     * @return the invocation target.
     */
    @Nonnull
    public abstract InvocationTarget<TYPE> getInvocationTarget(@Nonnull Context context);

    /**
     * Returns the target class.
     *
     * @return the target class.
     */
    @Nonnull
    public abstract Class<? extends TYPE> getTargetClass();

    /**
     * Checks if this invocation target is assignable to the specified class.
     *
     * @param otherClass the other class.
     * @return whether the invocation target is assignable to the class.
     */
    public abstract boolean isAssignableTo(@Nonnull Class<?> otherClass);

    /**
     * Checks if this invocation target is of the specified type.
     *
     * @param type the type class.
     * @return whether the invocation target is of the specified type.
     */
    public abstract boolean isSameTypeOf(@Nonnull Class<?> type);

    /**
     * Context invocation target wrapping a class.
     *
     * @param <TYPE> the target object type.
     */
    public static class ClassContextInvocationTarget<TYPE> extends ContextInvocationTarget<TYPE> {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<ClassContextInvocationTarget> CREATOR =
                new Creator<ClassContextInvocationTarget>() {

                    @SuppressWarnings("unchecked")
                    public ClassContextInvocationTarget createFromParcel(
                            @Nonnull final Parcel source) {

                        return new ClassContextInvocationTarget(
                                (Class<?>) source.readSerializable());
                    }

                    @Nonnull
                    public ClassContextInvocationTarget[] newArray(final int size) {

                        return new ClassContextInvocationTarget[size];
                    }
                };

        private final Class<TYPE> mTargetClass;

        /**
         * Constructor.
         *
         * @param targetClass the target class.
         */
        @SuppressWarnings("ConstantConditions")
        private ClassContextInvocationTarget(@Nonnull final Class<TYPE> targetClass) {

            if (targetClass.isPrimitive()) {

                // The parceling of primitive classes is broken...
                throw new IllegalArgumentException("the target class cannot be primitive");
            }

            mTargetClass = targetClass;
        }

        public int describeContents() {

            return 0;
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof ClassContextInvocationTarget)) {

                return false;
            }

            final ClassContextInvocationTarget<?> that = (ClassContextInvocationTarget<?>) o;
            return mTargetClass.equals(that.mTargetClass);
        }

        @Nonnull
        @Override
        public InvocationTarget<TYPE> getInvocationTarget(@Nonnull final Context context) {

            return InvocationTarget.classOfType(mTargetClass);
        }

        @Override
        public int hashCode() {

            return mTargetClass.hashCode();
        }

        @Nonnull
        @Override
        public Class<? extends TYPE> getTargetClass() {

            return mTargetClass;
        }

        @Override
        public boolean isAssignableTo(@Nonnull final Class<?> otherClass) {

            return otherClass.isAssignableFrom(mTargetClass);
        }

        @Override
        public boolean isSameTypeOf(@Nonnull final Class<?> type) {

            return (mTargetClass == type);
        }

        public void writeToParcel(@Nonnull final Parcel dest, final int flags) {

            dest.writeSerializable(mTargetClass);
        }
    }

    /**
     * Context invocation target wrapping an object instance.
     *
     * @param <TYPE> the target object type.
     */
    public static class ObjectContextInvocationTarget<TYPE> extends ContextInvocationTarget<TYPE> {

        /**
         * Creator instance needed by the parcelable protocol.
         */
        public static final Creator<ObjectContextInvocationTarget> CREATOR =
                new Creator<ObjectContextInvocationTarget>() {

                    @SuppressWarnings("unchecked")
                    public ObjectContextInvocationTarget createFromParcel(
                            @Nonnull final Parcel source) {

                        return new ObjectContextInvocationTarget(
                                (Class<?>) source.readSerializable(),
                                source.readArray(ContextInvocationTarget.class.getClassLoader()));
                    }

                    @Nonnull
                    public ObjectContextInvocationTarget[] newArray(final int size) {

                        return new ObjectContextInvocationTarget[size];
                    }
                };

        private final Object[] mFactoryArgs;

        private final Class<TYPE> mTargetClass;

        /**
         * Constructor.
         *
         * @param targetClass the target class.
         * @param factoryArgs the object factory arguments.
         */
        @SuppressWarnings("ConstantConditions")
        private ObjectContextInvocationTarget(@Nonnull final Class<TYPE> targetClass,
                @Nullable final Object[] factoryArgs) {

            if (targetClass.isPrimitive()) {

                // The parceling of primitive classes is broken...
                throw new IllegalArgumentException("the target class cannot be primitive");
            }

            mTargetClass = targetClass;
            mFactoryArgs = (factoryArgs != null) ? factoryArgs.clone() : Reflection.NO_ARGS;
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof ObjectContextInvocationTarget)) {

                return false;
            }

            final ObjectContextInvocationTarget that = (ObjectContextInvocationTarget) o;
            return Arrays.deepEquals(mFactoryArgs, that.mFactoryArgs) && mTargetClass.equals(
                    that.mTargetClass);
        }

        @Override
        public int hashCode() {

            int result = Arrays.deepHashCode(mFactoryArgs);
            result = 31 * result + mTargetClass.hashCode();
            return result;
        }

        @Nonnull
        @Override
        @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
        public InvocationTarget<TYPE> getInvocationTarget(@Nonnull final Context context) {

            TYPE target = null;
            final Class<TYPE> targetClass = mTargetClass;
            final Object[] factoryArgs = mFactoryArgs;

            if (context instanceof FactoryContext) {

                // The only safe way is to synchronize the factory using the very same instance
                synchronized (context) {

                    target = ((FactoryContext) context).geInstance(targetClass, factoryArgs);
                }
            }

            if (target == null) {

                try {

                    target = findConstructor(targetClass, factoryArgs).newInstance(factoryArgs);

                } catch (final Throwable t) {

                    throw InvocationException.wrapIfNeeded(t);
                }

            } else if (!targetClass.isInstance(target)) {

                throw new RoutineException(
                        target + " is not an instance of " + targetClass.getName());
            }

            return InvocationTarget.instance(target);
        }

        @Nonnull
        @Override
        public Class<? extends TYPE> getTargetClass() {

            return mTargetClass;
        }

        @Override
        public boolean isAssignableTo(@Nonnull final Class<?> otherClass) {

            return otherClass.isAssignableFrom(mTargetClass);
        }

        @Override
        public boolean isSameTypeOf(@Nonnull final Class<?> type) {

            return isAssignableTo(type);
        }

        public int describeContents() {

            return 0;
        }

        public void writeToParcel(@Nonnull final Parcel dest, final int flags) {

            dest.writeSerializable(mTargetClass);
            dest.writeArray(mFactoryArgs);
        }
    }
}

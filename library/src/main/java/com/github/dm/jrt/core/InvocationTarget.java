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
package com.github.dm.jrt.core;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class representing an invocation target.
 * <p/>
 * Created by davide-maestroni on 08/20/2015.
 */
public abstract class InvocationTarget {

    /**
     * Avoid direct instantiation.
     */
    private InvocationTarget() {

    }

    /**
     * Returns a target based on the specified class.
     *
     * @param targetClass the target class.
     * @return the invocation target.
     */
    @Nonnull
    public static InvocationTarget targetClass(@Nonnull final Class<?> targetClass) {

        return new ClassInvocationTarget(targetClass);
    }

    /**
     * Returns a target based on the specified instance.
     *
     * @param target the target instance.
     * @return the invocation target.
     */
    @Nonnull
    public static InvocationTarget targetObject(@Nonnull final Object target) {

        return new ObjectInvocationTarget(target);
    }

    /**
     * Returns the target of the invocation.
     *
     * @return the target.
     */
    @Nullable
    public abstract Object getTarget();

    /**
     * Returns the target class.
     *
     * @return the target class.
     */
    @Nonnull
    public abstract Class<?> getTargetClass();

    /**
     * Checks if this invocation target is assignable to the specified class.
     *
     * @param targetClass the target class.
     * @return whether the invocation target is assignable to the class.
     */
    public abstract boolean isAssignableTo(@Nonnull final Class<?> targetClass);

    /**
     * Invocation target wrapping a class.
     */
    private static class ClassInvocationTarget extends InvocationTarget {

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param targetClass the target class.
         */
        @SuppressWarnings("ConstantConditions")
        private ClassInvocationTarget(@Nonnull final Class<?> targetClass) {

            if (targetClass == null) {

                throw new NullPointerException("the target class must not be null");
            }

            mTargetClass = targetClass;
        }

        @Nullable
        @Override
        public Object getTarget() {

            return mTargetClass;
        }

        @Override
        public int hashCode() {

            // AUTO-GENERATED CODE
            return mTargetClass.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            // AUTO-GENERATED CODE
            if (this == o) {

                return true;
            }

            if (!(o instanceof ClassInvocationTarget)) {

                return false;
            }

            final ClassInvocationTarget that = (ClassInvocationTarget) o;
            return mTargetClass.equals(that.mTargetClass);
        }

        @Nonnull
        @Override
        public Class<?> getTargetClass() {

            return mTargetClass;
        }

        @Override
        public boolean isAssignableTo(@Nonnull final Class<?> targetClass) {

            return targetClass.isAssignableFrom(mTargetClass);
        }
    }

    /**
     * Invocation target wrapping an object instance.
     */
    private static class ObjectInvocationTarget extends InvocationTarget {

        private final WeakReference<Object> mTarget;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param target the target instance.
         */
        private ObjectInvocationTarget(@Nonnull final Object target) {

            mTarget = new WeakReference<Object>(target);
            mTargetClass = target.getClass();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof ObjectInvocationTarget)) {

                return false;
            }

            final ObjectInvocationTarget that = (ObjectInvocationTarget) o;
            final Object referent = mTarget.get();
            return (referent != null) && referent.equals(that.mTarget.get()) && mTargetClass.equals(
                    that.mTargetClass);
        }

        @Override
        public int hashCode() {

            final Object referent = mTarget.get();
            int result = (referent != null) ? referent.hashCode() : 0;
            result = 31 * result + mTargetClass.hashCode();
            return result;
        }

        @Nullable
        @Override
        public Object getTarget() {

            return mTarget.get();
        }

        @Nonnull
        @Override
        public Class<?> getTargetClass() {

            return mTargetClass;
        }

        @Override
        public boolean isAssignableTo(@Nonnull final Class<?> targetClass) {

            return targetClass.isInstance(mTarget.get());
        }
    }
}

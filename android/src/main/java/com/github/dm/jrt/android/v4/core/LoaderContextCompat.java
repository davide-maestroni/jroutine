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
package com.github.dm.jrt.android.v4.core;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;

import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Class representing an Android loader context (like activities or fragments).
 * <p/>
 * No strong reference to the wrapped objects will be retained by this class implementations. So,
 * it is up to the caller to ensure that they are not garbage collected before time.
 * <p/>
 * Created by davide-maestroni on 07/08/2015.
 */
public abstract class LoaderContextCompat {

    /**
     * Avoid direct instantiation.
     */
    private LoaderContextCompat() {

    }

    /**
     * Returns a context wrapping the specified fragment.
     *
     * @param fragment the fragment instance.
     * @return the loader context.
     */
    @NotNull
    public static LoaderContextCompat loaderFrom(@NotNull final Fragment fragment) {

        return new FragmentContextCompat(fragment);
    }

    /**
     * Returns a context wrapping the specified fragment, with the specified instance as base
     * context, that is, the one used by the loader.<br/>
     * In order to prevent undesired leaks, the class of the specified context must be static.
     *
     * @param fragment the fragment instance.
     * @param context  the context used to get the application one.
     * @return the loader context.
     */
    @NotNull
    public static LoaderContextCompat loaderFrom(@NotNull final Fragment fragment,
            @NotNull final Context context) {

        return new WrappedFragmentContextCompat(fragment, context);
    }

    /**
     * Returns a context wrapping the specified activity.
     *
     * @param activity the activity instance.
     * @return the loader context.
     */
    @NotNull
    public static LoaderContextCompat loaderFrom(@NotNull final FragmentActivity activity) {

        return new ActivityContextCompat(activity);
    }

    /**
     * Returns a context wrapping the specified activity, with the specified instance as base
     * context, that is, the one used by the loader.<br/>
     * In order to prevent undesired leaks, the class of the specified context must be static.
     *
     * @param activity the activity instance.
     * @param context  the context used to get the application one.
     * @return the loader context.
     */
    @NotNull
    public static LoaderContextCompat loaderFrom(@NotNull final FragmentActivity activity,
            @NotNull final Context context) {

        return new WrappedActivityContextCompat(activity, context);
    }

    /**
     * Returns the wrapped component.
     *
     * @return the component or null.
     */
    @Nullable
    public abstract Object getComponent();

    /**
     * Returns the loader context.
     *
     * @return the context or null.
     */
    @Nullable
    public abstract Context getLoaderContext();

    /**
     * Returns the loader manager of the specific component.
     *
     * @return the loader manager or null.
     */
    @Nullable
    public abstract LoaderManager getLoaderManager();

    /**
     * Loader context wrapping an activity.
     */
    private static class ActivityContextCompat extends LoaderContextCompat {

        private final WeakReference<FragmentActivity> mActivity;

        /**
         * Constructor.
         *
         * @param activity the wrapped activity.
         */
        @SuppressWarnings("ConstantConditions")
        private ActivityContextCompat(@NotNull final FragmentActivity activity) {

            if (activity == null) {
                throw new NullPointerException("the activity must not be null");
            }

            mActivity = new WeakReference<FragmentActivity>(activity);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof ActivityContextCompat)) {
                return false;
            }

            final ActivityContextCompat that = (ActivityContextCompat) o;
            final FragmentActivity referent = mActivity.get();
            return (referent != null) && referent.equals(that.mActivity.get());
        }

        @Nullable
        @Override
        public Object getComponent() {

            return mActivity.get();
        }

        @Override
        public int hashCode() {

            final FragmentActivity referent = mActivity.get();
            return (referent != null) ? referent.hashCode() : 0;
        }

        @Nullable
        @Override
        public Context getLoaderContext() {

            return mActivity.get();
        }

        @Nullable
        @Override
        public LoaderManager getLoaderManager() {

            final FragmentActivity activity = mActivity.get();
            return (activity != null) ? activity.getSupportLoaderManager() : null;
        }
    }

    /**
     * Loader context wrapping a fragment.
     */
    private static class FragmentContextCompat extends LoaderContextCompat {

        private final WeakReference<Fragment> mFragment;

        /**
         * Constructor.
         *
         * @param fragment the wrapped fragment.
         */
        @SuppressWarnings("ConstantConditions")
        private FragmentContextCompat(@NotNull final Fragment fragment) {

            if (fragment == null) {
                throw new NullPointerException("the fragment must not be null");
            }

            mFragment = new WeakReference<Fragment>(fragment);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof FragmentContextCompat)) {
                return false;
            }

            final FragmentContextCompat that = (FragmentContextCompat) o;
            final Fragment referent = mFragment.get();
            return (referent != null) && referent.equals(that.mFragment.get());
        }

        @Override
        public int hashCode() {

            final Fragment referent = mFragment.get();
            return (referent != null) ? referent.hashCode() : 0;
        }

        @Nullable
        @Override
        public Context getLoaderContext() {

            final Fragment fragment = mFragment.get();
            return (fragment != null) ? fragment.getActivity() : null;
        }

        @Nullable
        @Override
        public LoaderManager getLoaderManager() {

            final Fragment fragment = mFragment.get();
            return (fragment != null) ? fragment.getLoaderManager() : null;
        }

        @Nullable
        @Override
        public Object getComponent() {

            return mFragment.get();
        }
    }

    /**
     * Loader context wrapping an activity and its loader context.
     */
    private static class WrappedActivityContextCompat extends ActivityContextCompat {

        private final WeakReference<Context> mContext;

        /**
         * Constructor.
         *
         * @param activity the wrapped activity.
         * @param context  the wrapped context.
         */
        @SuppressWarnings("ConstantConditions")
        private WrappedActivityContextCompat(@NotNull final FragmentActivity activity,
                @NotNull final Context context) {

            super(activity);
            final Class<? extends Context> contextClass = context.getClass();
            if (!Reflection.hasStaticContext(contextClass)) {
                throw new IllegalArgumentException(
                        "the context class must have a static context: " + contextClass.getName());
            }

            mContext = new WeakReference<Context>(context);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof WrappedActivityContextCompat)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            final WrappedActivityContextCompat that = (WrappedActivityContextCompat) o;
            final Context referent = mContext.get();
            return (referent != null) && referent.equals(that.mContext.get());
        }

        @Override
        public int hashCode() {

            int result = super.hashCode();
            final Context referent = mContext.get();
            result = 31 * result + (referent != null ? referent.hashCode() : 0);
            return result;
        }

        @Nullable
        @Override
        public Context getLoaderContext() {

            return mContext.get();
        }
    }

    /**
     * Loader context wrapping a fragment and its loader context.
     */
    private static class WrappedFragmentContextCompat extends FragmentContextCompat {

        private final WeakReference<Context> mContext;

        /**
         * Constructor.
         *
         * @param fragment the wrapped fragment.
         * @param context  the wrapped context.
         */
        @SuppressWarnings("ConstantConditions")
        private WrappedFragmentContextCompat(@NotNull final Fragment fragment,
                @NotNull final Context context) {

            super(fragment);
            final Class<? extends Context> contextClass = context.getClass();
            if (!Reflection.hasStaticContext(contextClass)) {
                throw new IllegalArgumentException(
                        "the context class must have a static context: " + contextClass.getName());
            }

            mContext = new WeakReference<Context>(context);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof WrappedFragmentContextCompat)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            final WrappedFragmentContextCompat that = (WrappedFragmentContextCompat) o;
            final Context referent = mContext.get();
            return (referent != null) && referent.equals(that.mContext.get());
        }

        @Override
        public int hashCode() {

            int result = super.hashCode();
            final Context referent = mContext.get();
            result = 31 * result + (referent != null ? referent.hashCode() : 0);
            return result;
        }

        @Nullable
        @Override
        public Context getLoaderContext() {

            return mContext.get();
        }
    }
}

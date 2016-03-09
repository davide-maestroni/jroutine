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

package com.github.dm.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Class representing an Android loader context (like activities or fragments).
 * <p/>
 * No strong reference to the wrapped objects will be retained by this class implementations. So,
 * it is up to the caller to ensure that they are not garbage collected before being called.
 * <p/>
 * Created by davide-maestroni on 07/08/2015.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public abstract class LoaderContext {

    /**
     * Avoid direct instantiation.
     */
    private LoaderContext() {

    }

    /**
     * Returns a context wrapping the specified activity.
     *
     * @param activity the activity instance.
     * @return the loader context.
     */
    @NotNull
    public static LoaderContext loaderFrom(@NotNull final Activity activity) {

        checkVersion();
        return new ActivityContext(activity, activity);
    }

    /**
     * Returns a context wrapping the specified activity, with the specified instance as base
     * context, that is, the one used by the loader.<br/>
     * In order to prevent undesired leaks, the class of the specified context must be static.
     *
     * @param activity the activity instance.
     * @param context  the context used to get the application one.
     * @return the loader context.
     * @throws java.lang.IllegalArgumentException if the class of the specified context has not a
     *                                            static scope.
     */
    @NotNull
    public static LoaderContext loaderFrom(@NotNull final Activity activity,
            @NotNull final Context context) {

        checkVersion();
        return new ActivityContext(activity, context);
    }

    /**
     * Returns a context wrapping the specified fragment.
     *
     * @param fragment the fragment instance.
     * @return the loader context.
     */
    @NotNull
    public static LoaderContext loaderFrom(@NotNull final Fragment fragment) {

        checkVersion();
        return new FragmentContext(fragment, fragment.getActivity());
    }

    /**
     * Returns a context wrapping the specified fragment, with the specified instance as base
     * context, that is, the one used by the loader.<br/>
     * In order to prevent undesired leaks, the class of the specified context must be static.
     *
     * @param fragment the fragment instance.
     * @param context  the context used to get the application one.
     * @return the loader context.
     * @throws java.lang.IllegalArgumentException if the class of the specified context has not a
     *                                            static scope.
     */
    @NotNull
    public static LoaderContext loaderFrom(@NotNull final Fragment fragment,
            @NotNull final Context context) {

        checkVersion();
        return new FragmentContext(fragment, context);
    }

    private static void checkVersion() {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            throw new UnsupportedOperationException(
                    "this method is supported only for API level >= " + VERSION_CODES.HONEYCOMB
                            + ": use com.github.dm.jrt.android.v4.routine.JRoutineLoaderCompat "
                            + "class instead");
        }
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
    @TargetApi(VERSION_CODES.HONEYCOMB)
    private static class ActivityContext extends LoaderContext {

        private final WeakReference<Activity> mActivity;

        private final WeakReference<Context> mContext;

        /**
         * Constructor.
         *
         * @param activity the wrapped activity.
         * @param context  the wrapped context.
         * @throws java.lang.IllegalArgumentException if the class of the specified context has not
         *                                            a static scope.
         */
        @SuppressWarnings("ConstantConditions")
        private ActivityContext(@NotNull final Activity activity, @NotNull final Context context) {

            if (activity == null) {
                throw new NullPointerException("the activity must not be null");
            }

            final Class<? extends Context> contextClass = context.getClass();
            if (!Reflection.hasStaticScope(contextClass)) {
                throw new IllegalArgumentException(
                        "the context class must have a static scope: " + contextClass.getName());
            }

            mActivity = new WeakReference<Activity>(activity);
            mContext = new WeakReference<Context>(context);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof ActivityContext)) {
                return false;
            }

            final ActivityContext that = (ActivityContext) o;
            final Activity activity = mActivity.get();
            if ((activity == null) || !activity.equals(that.mActivity.get())) {
                return false;
            }

            final Context context = mContext.get();
            return (context != null) && context.equals(that.mContext.get());
        }

        @Nullable
        @Override
        public Object getComponent() {

            return mActivity.get();
        }

        @Override
        public int hashCode() {

            final Activity activity = mActivity.get();
            int result = (activity != null) ? activity.hashCode() : 0;
            final Context context = mContext.get();
            result = 31 * result + (context != null ? context.hashCode() : 0);
            return result;
        }

        @Nullable
        @Override
        public Context getLoaderContext() {

            return mContext.get();
        }

        @Nullable
        @Override
        public LoaderManager getLoaderManager() {

            final Activity activity = mActivity.get();
            return (activity != null) ? activity.getLoaderManager() : null;
        }
    }

    /**
     * Loader context wrapping a fragment.
     */
    @TargetApi(VERSION_CODES.HONEYCOMB)
    private static class FragmentContext extends LoaderContext {

        private final WeakReference<Context> mContext;

        private final WeakReference<Fragment> mFragment;

        /**
         * Constructor.
         *
         * @param fragment the wrapped fragment.
         * @param context  the wrapped context.
         * @throws java.lang.IllegalArgumentException if the class of the specified context has not
         *                                            a static scope.
         */
        @SuppressWarnings("ConstantConditions")
        private FragmentContext(@NotNull final Fragment fragment, @NotNull final Context context) {

            if (fragment == null) {
                throw new NullPointerException("the fragment must not be null");
            }

            final Class<? extends Context> contextClass = context.getClass();
            if (!Reflection.hasStaticScope(contextClass)) {
                throw new IllegalArgumentException(
                        "the context class must have a static scope: " + contextClass.getName());
            }

            mFragment = new WeakReference<Fragment>(fragment);
            mContext = new WeakReference<Context>(context);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof FragmentContext)) {
                return false;
            }

            final FragmentContext that = (FragmentContext) o;
            final Fragment fragment = mFragment.get();
            if ((fragment == null) || !fragment.equals(that.mFragment.get())) {
                return false;
            }

            final Context context = mContext.get();
            return (context != null) && context.equals(that.mContext.get());
        }

        @Override
        public int hashCode() {

            final Fragment fragment = mFragment.get();
            int result = (fragment != null) ? fragment.hashCode() : 0;
            final Context context = mContext.get();
            result = 31 * result + (context != null ? context.hashCode() : 0);
            return result;
        }

        @Nullable
        @Override
        public Context getLoaderContext() {

            return mContext.get();
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
}

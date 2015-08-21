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
package com.gh.bmd.jrt.android.v11.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.gh.bmd.jrt.util.Reflection;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class representing an Android loader context (like activities or fragments).
 * <p/>
 * No strong reference to the wrapped objects will be retained by this class implementations. So,
 * it is up to the caller to ensure that they are not garbage collected before time.
 * <p/>
 * Created by davide-maestroni on 08/07/15.
 */
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
    @Nonnull
    public static LoaderContext contextFrom(@Nonnull final Activity activity) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only for API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        return new ActivityContext(activity);
    }

    /**
     * Returns a context wrapping the specified activity, with the specified instance as base
     * context.<br/>
     * In order to prevent undesired leaks, the class of the specified context must be static.
     *
     * @param activity the activity instance.
     * @param context  the context used to get the application one.
     * @return the loader context.
     */
    @Nonnull
    public static LoaderContext contextFrom(@Nonnull final Activity activity,
            @Nonnull final Context context) {

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {

            throw new UnsupportedOperationException(
                    "this method is supported only for API level >= " +
                            VERSION_CODES.HONEYCOMB
                            + ": use com.gh.bmd.jrt.android.v4.routine.JRoutine class instead");
        }

        return new WrappedActivityContext(activity, context);
    }

    /**
     * Returns a context wrapping the specified fragment.
     *
     * @param fragment the fragment instance.
     * @return the loader context.
     */
    @Nonnull
    public static LoaderContext contextFrom(@Nonnull final Fragment fragment) {

        return new FragmentContext(fragment);
    }

    /**
     * Returns a context wrapping the specified fragment, with the specified instance as base
     * context.<br/>
     * In order to prevent undesired leaks, the class of the specified context must be static.
     *
     * @param fragment the fragment instance.
     * @param context  the context used to get the application one.
     * @return the loader context.
     */
    @Nonnull
    public static LoaderContext contextFrom(@Nonnull final Fragment fragment,
            @Nonnull final Context context) {

        return new WrappedFragmentContext(fragment, context);
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

        /**
         * Constructor.
         *
         * @param activity the wrapped activity.
         */
        @SuppressWarnings("ConstantConditions")
        private ActivityContext(@Nonnull final Activity activity) {

            if (activity == null) {

                throw new NullPointerException("the activity must not be null");
            }

            mActivity = new WeakReference<Activity>(activity);
        }

        @Nullable
        @Override
        public Object getComponent() {

            return mActivity.get();
        }

        @Nullable
        @Override
        public Context getLoaderContext() {

            return mActivity.get();
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

        private final WeakReference<Fragment> mFragment;

        /**
         * Constructor.
         *
         * @param fragment the wrapped fragment.
         */
        @SuppressWarnings("ConstantConditions")
        private FragmentContext(@Nonnull final Fragment fragment) {

            if (fragment == null) {

                throw new NullPointerException("the fragment must not be null");
            }

            mFragment = new WeakReference<Fragment>(fragment);
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
    private static class WrappedActivityContext extends ActivityContext {

        private final WeakReference<Context> mContext;

        /**
         * Constructor.
         *
         * @param activity the wrapped activity.
         * @param context  the wrapped context.
         */
        @SuppressWarnings("ConstantConditions")
        private WrappedActivityContext(@Nonnull final Activity activity,
                @Nonnull final Context context) {

            super(activity);

            final Class<? extends Context> contextClass = context.getClass();

            if (!Reflection.isStaticClass(contextClass)) {

                throw new IllegalArgumentException(
                        "the context class must be static: " + contextClass.getName());
            }

            mContext = new WeakReference<Context>(context);
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
    private static class WrappedFragmentContext extends FragmentContext {

        private final WeakReference<Context> mContext;

        /**
         * Constructor.
         *
         * @param fragment the wrapped fragment.
         * @param context  the wrapped context.
         */
        @SuppressWarnings("ConstantConditions")
        private WrappedFragmentContext(@Nonnull final Fragment fragment,
                @Nonnull final Context context) {

            super(fragment);

            final Class<? extends Context> contextClass = context.getClass();

            if (!Reflection.isStaticClass(contextClass)) {

                throw new IllegalArgumentException(
                        "the context class must be static: " + contextClass.getName());
            }

            mContext = new WeakReference<Context>(context);
        }

        @Nullable
        @Override
        public Context getLoaderContext() {

            return mContext.get();
        }
    }
}

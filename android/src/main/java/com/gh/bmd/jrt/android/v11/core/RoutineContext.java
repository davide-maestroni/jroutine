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
import android.os.Build.VERSION_CODES;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class representing an Android routine context (like activities or fragments).
 * <p/>
 * No strong reference to the wrapped objects will be retained by this class implementations. So, it
 * is up to the caller to ensure that they are not garbage collected before time.
 * <p/>
 * Created by davide on 08/07/15.
 */
public abstract class RoutineContext {

    /**
     * Avoid direct instantiation.
     */
    private RoutineContext() {

    }

    /**
     * Returns a context wrapping the specified activity.
     *
     * @param activity the activity instance.
     * @return the UI context.
     */
    @Nonnull
    public static RoutineContext contextFrom(@Nonnull final Activity activity) {

        return new ActivityContext(activity);
    }

    /**
     * Returns a context wrapping the specified fragment.
     *
     * @param fragment the fragment instance.
     * @return the UI context.
     */
    @Nonnull
    public static RoutineContext contextFrom(@Nonnull final Fragment fragment) {

        return new FragmentContext(fragment);
    }

    /**
     * Returns a context wrapping the specified activity, with the specified instance as base
     * context.
     *
     * @param activity the activity instance.
     * @param context  the context used to get the application one.
     * @return the UI context.
     */
    @Nonnull
    public static RoutineContext contextFrom(@Nonnull final Activity activity,
            @Nonnull final Context context) {

        return new WrappedActivityContext(activity, context);
    }

    /**
     * Returns a context wrapping the specified fragment, with the specified instance as base
     * context.
     *
     * @param fragment the fragment instance.
     * @param context  the context used to get the application one.
     * @return the UI context.
     */
    @Nonnull
    public static RoutineContext contextFrom(@Nonnull final Fragment fragment,
            @Nonnull final Context context) {

        return new WrappedFragmentContext(fragment, context);
    }

    /**
     * Returns the application context.
     *
     * @return the context or null.
     */
    @Nullable
    public abstract Context getApplicationContext();

    /**
     * Returns the wrapped component.
     *
     * @return the component or null.
     */
    @Nullable
    public abstract Object getComponent();

    /**
     * Returns the loader manager of the specific component.
     *
     * @return the loader manager or null.
     */
    @Nullable
    public abstract LoaderManager getLoaderManager();

    /**
     * UI context wrapping an activity.
     */
    @TargetApi(VERSION_CODES.HONEYCOMB)
    private static class ActivityContext extends RoutineContext {

        private final WeakReference<Activity> mActivity;

        /**
         * Constructor.
         *
         * @param activity the wrapped activity.
         */
        @SuppressWarnings("ConstantConditions")
        public ActivityContext(@Nonnull final Activity activity) {

            if (activity == null) {

                throw new NullPointerException("the activity must not be null");
            }

            mActivity = new WeakReference<Activity>(activity);
        }

        @Nullable
        @Override
        public Context getApplicationContext() {

            final Activity activity = mActivity.get();
            return (activity != null) ? activity.getApplicationContext() : null;
        }

        @Nullable
        @Override
        public Object getComponent() {

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
     * UI context wrapping a fragment.
     */
    @TargetApi(VERSION_CODES.HONEYCOMB)
    private static class FragmentContext extends RoutineContext {

        private final WeakReference<Fragment> mFragment;

        /**
         * Constructor.
         *
         * @param fragment the wrapped fragment.
         */
        @SuppressWarnings("ConstantConditions")
        public FragmentContext(@Nonnull final Fragment fragment) {

            if (fragment == null) {

                throw new NullPointerException("the fragment must not be null");
            }

            mFragment = new WeakReference<Fragment>(fragment);
        }

        @Nullable
        @Override
        public Context getApplicationContext() {

            final Fragment fragment = mFragment.get();

            if (fragment != null) {

                final Activity activity = fragment.getActivity();
                return (activity != null) ? activity.getApplicationContext() : null;
            }

            return null;
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
     * UI context wrapping an activity and its application context.
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
        public WrappedActivityContext(@Nonnull final Activity activity,
                @Nonnull final Context context) {

            super(activity);

            if (context == null) {

                throw new NullPointerException("the context must not be null");
            }

            mContext = new WeakReference<Context>(context);
        }

        @Nullable
        @Override
        public Context getApplicationContext() {

            final Context context = mContext.get();
            return (context != null) ? context.getApplicationContext() : null;
        }
    }

    /**
     * UI context wrapping a fragment and its application context.
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
        public WrappedFragmentContext(@Nonnull final Fragment fragment,
                @Nonnull final Context context) {

            super(fragment);

            if (context == null) {

                throw new NullPointerException("the context must not be null");
            }

            mContext = new WeakReference<Context>(context);
        }

        @Nullable
        @Override
        public Context getApplicationContext() {

            final Context context = mContext.get();
            return (context != null) ? context.getApplicationContext() : null;
        }
    }
}

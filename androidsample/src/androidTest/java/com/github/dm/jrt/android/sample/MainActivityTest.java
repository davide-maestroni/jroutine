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

package com.github.dm.jrt.android.sample;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.Stage;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance;
import static org.hamcrest.CoreMatchers.anything;

/**
 * Main activity unit test.
 * <p/>
 * Created by davide-maestroni on 03/26/2016.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testRepoList() {

        onData(anything()).inAdapterView(withId(R.id.repo_list))
                          .atPosition(0)
                          .check(matches(isDisplayed()));
    }

    @Test
    public void testRepoListRotation() {

        onData(anything()).inAdapterView(withId(R.id.repo_list))
                          .atPosition(0)
                          .check(matches(isDisplayed()));
        onView(isRoot()).perform(
                new OrientationChangeAction(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        onData(anything()).inAdapterView(withId(R.id.repo_list))
                          .atPosition(0)
                          .check(matches(isDisplayed()));
    }

    private static class OrientationChangeAction implements ViewAction {

        private final int mOrientation;

        private OrientationChangeAction(final int orientation) {

            mOrientation = orientation;
        }

        @Override
        public Matcher<View> getConstraints() {

            return isRoot();
        }

        @Override
        public String getDescription() {

            return "change device orientation to " + mOrientation;
        }

        @Override
        public void perform(final UiController uiController, final View view) {

            uiController.loopMainThreadUntilIdle();
            ((Activity) view.getContext()).setRequestedOrientation(mOrientation);
            if (getInstance().getActivitiesInStage(Stage.RESUMED).isEmpty()) {
                throw new RuntimeException("orientation change failed");
            }
        }
    }
}

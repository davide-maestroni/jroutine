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
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.PerformException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.Stage;
import android.view.View;

import com.github.dm.jrt.core.util.DurationMeasure;

import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance;
import static org.hamcrest.CoreMatchers.anything;

/**
 * Main Activity unit test.
 * <p>
 * Created by davide-maestroni on 03/26/2016.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

  @Rule
  public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

  @Test
  public void testRepoList() throws InterruptedException {
    try {
      // Wait for the network request to complete
      DurationMeasure.seconds(10).sleepAtLeast();
      IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
      onData(anything()).inAdapterView(withId(R.id.repo_list))
                        .atPosition(0)
                        .check(matches(isDisplayed()));

    } catch (final PerformException ignored) {
    }
  }

  @Test
  public void testRepoListRotation() throws InterruptedException {
    try {
      // Wait for the network request to complete
      DurationMeasure.seconds(10).sleepAtLeast();
      IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
      onData(anything()).inAdapterView(withId(R.id.repo_list))
                        .atPosition(0)
                        .check(matches(isDisplayed()));
      onView(isRoot()).perform(new LandscapeOrientationAction(mActivityRule.getActivity()));
      onData(anything()).inAdapterView(withId(R.id.repo_list))
                        .atPosition(0)
                        .check(matches(isDisplayed()));

    } catch (final PerformException ignored) {
    }
  }

  private static class LandscapeOrientationAction implements ViewAction {

    private final Activity mActivity;

    private LandscapeOrientationAction(@NotNull final Activity activity) {
      mActivity = activity;
    }

    @Override
    public Matcher<View> getConstraints() {
      return isRoot();
    }

    @Override
    public String getDescription() {
      return "change device orientation to landscape";
    }

    @Override
    public void perform(final UiController uiController, final View view) {
      uiController.loopMainThreadUntilIdle();
      mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      if (getInstance().getActivitiesInStage(Stage.RESUMED).isEmpty()) {
        throw new RuntimeException("orientation change failed");
      }
    }
  }
}

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
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.WindowManager.LayoutParams;

import com.github.dm.jrt.android.core.test.R;

/**
 * Test Activity ensuring the creation of the Loader manager during call to {@code onCreate()}.
 * <p>
 * Created by davide-maestroni on 01/28/2015.
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public class RotationTestActivity extends Activity {

  @Override
  protected void onCreate(final Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    getWindow().addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD |
        LayoutParams.FLAG_SHOW_WHEN_LOCKED |
        LayoutParams.FLAG_TURN_SCREEN_ON | LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.test_v11_layout);
    // Need to initialize the loader manager here in order to successfully simulate rotation
    getLoaderManager();
  }
}

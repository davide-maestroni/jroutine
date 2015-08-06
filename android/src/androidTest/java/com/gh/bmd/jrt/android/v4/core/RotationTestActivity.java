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
package com.gh.bmd.jrt.android.v4.core;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager.LayoutParams;

import com.gh.bmd.jrt.android.R;

/**
 * Test activity ensuring the creation of the loader manager during call to <code>onCreate()</code>.
 * <p/>
 * Created by davide-maestroni on 1/28/15.
 */
public class RotationTestActivity extends FragmentActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_v4_layout);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        // need to initialize the loader manager here in order to successfully simulate rotation
        getSupportLoaderManager();
    }
}

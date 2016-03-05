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

package com.github.dm.jrt.android.core.runner;

import android.os.Looper;

/**
 * Implementation of a runner employing the main UI thread looper.<br/>
 * Note that, when the invocation runs in the main thread, the executions with a delay of 0 will be
 * performed synchronously, while the ones with a positive delay will be posted on the UI thread.
 * <p/>
 * Created by davide-maestroni on 12/17/2014.
 */
public class MainRunner extends LooperRunner {

    /**
     * Constructor.
     */
    public MainRunner() {

        super(Looper.getMainLooper());
    }
}

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
package com.gh.bmd.jrt.android.log;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for creating and sharing log instances, employing specific Android classes.
 * <p/>
 * Created by davide-maestroni on 12/22/14.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "utility class extending the functions of another utility class")
public class Logs extends com.gh.bmd.jrt.log.Logs {

    private static final AndroidLog sAndroidLog = new AndroidLog();

    /**
     * Returns the Android log shared instance.
     *
     * @return the shared instance.
     */
    @Nonnull
    public static AndroidLog androidLog() {

        return sAndroidLog;
    }
}

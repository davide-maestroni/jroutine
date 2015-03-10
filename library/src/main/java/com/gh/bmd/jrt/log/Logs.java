/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.log;

import javax.annotation.Nonnull;

/**
 * Utility class for creating and sharing log instances.
 * <p/>
 * Created by davide on 12/22/14.
 */
public class Logs {

    private static final SystemLog sSystemLog = new SystemLog();

    private static volatile NullLog sNullLog = new NullLog();

    /**
     * Avoid direct instantiation.
     */
    protected Logs() {

    }

    /**
     * Returns the null log shared instance.
     *
     * @return the shared instance.
     */
    @Nonnull
    public static NullLog nullLog() {

        return sNullLog;
    }

    /**
     * Returns the system output log shared instance.
     *
     * @return the shared instance.
     */
    @Nonnull
    public static SystemLog systemLog() {

        return sSystemLog;
    }
}

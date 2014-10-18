/**
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
package com.bmd.jrt.log;

import javax.annotation.Nonnull;

/**
 * Simple log implementation writing messages to the system output.
 * <p/>
 * Created by davide on 10/3/14.
 */
public class SystemLog extends BasicLog {

    @Override
    public void log(@Nonnull final String message) {

        System.out.println(message);
    }
}
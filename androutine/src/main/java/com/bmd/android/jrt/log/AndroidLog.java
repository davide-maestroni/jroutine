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
package com.bmd.android.jrt.log;

import android.util.Log;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Android specific log implementation.
 * <p/>
 * Created by davide on 10/7/14.
 */
public class AndroidLog implements com.bmd.jrt.log.Log {

    @Override
    public void dbg(@NonNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        if (throwable != null) {

            Log.d(contexts.toString(), message, throwable);

        } else {

            Log.d(contexts.toString(), message);
        }
    }

    @Override
    public void err(@NonNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        if (throwable != null) {

            Log.e(contexts.toString(), message, throwable);

        } else {

            Log.e(contexts.toString(), message);
        }
    }

    @Override
    public void wrn(@NonNull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        if (throwable != null) {

            Log.w(contexts.toString(), message, throwable);

        } else {

            Log.w(contexts.toString(), message);
        }
    }
}
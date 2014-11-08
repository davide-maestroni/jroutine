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
package com.bmd.jrt.android.log;

import android.util.Log;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Android specific log implementation.
 * <p/>
 * Created by davide on 10/7/14.
 */
public class AndroidLog implements com.bmd.jrt.log.Log {

    @Override
    public void dbg(@Nonnull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        if (throwable != null) {

            Log.d(contexts.get(contexts.size() - 1).toString(), message, throwable);

        } else {

            Log.d(contexts.get(contexts.size() - 1).toString(), message);
        }
    }

    @Override
    public void err(@Nonnull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        if (throwable != null) {

            Log.e(contexts.get(contexts.size() - 1).toString(), message, throwable);

        } else {

            Log.e(contexts.get(contexts.size() - 1).toString(), message);
        }
    }

    @Override
    public void wrn(@Nonnull final List<Object> contexts, @Nullable final String message,
            @Nullable final Throwable throwable) {

        if (throwable != null) {

            Log.w(contexts.get(contexts.size() - 1).toString(), message, throwable);

        } else {

            Log.w(contexts.get(contexts.size() - 1).toString(), message);
        }
    }
}

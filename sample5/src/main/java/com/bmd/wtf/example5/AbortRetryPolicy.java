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
package com.bmd.wtf.example5;

import com.bmd.wtf.example1.DownloadFailure;
import com.bmd.wtf.example3.RetryPolicy;
import com.bmd.wtf.example4.AbortException;
import com.bmd.wtf.flw.River;

/**
 * Extension of retry class properly handling abort failures.
 */
public class AbortRetryPolicy extends RetryPolicy {

    public AbortRetryPolicy(final River<Object> river, final int maxCount) {

        super(river, maxCount);
    }

    @Override
    public void onFailure(final DownloadFailure download) {

        if (download.getReason() instanceof AbortException) {

            downRiver().push(download);

        } else {

            super.onFailure(download);
        }
    }
}
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
package com.bmd.wtf.example3;

import com.bmd.wtf.example1.Download;
import com.bmd.wtf.example1.DownloadFailure;
import com.bmd.wtf.example1.DownloadSuccess;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.xtr.rpd.RapidGate;

import java.net.URI;
import java.util.HashMap;

/**
 * This class is meant to retry the discharge of data in case an error occurred.
 */
public class RetryPolicy extends RapidGate {

    private final int mMaxCount;

    private final HashMap<URI, Integer> mRetryCounts = new HashMap<URI, Integer>();

    private final River<Object> mRiver;

    public RetryPolicy(final River<Object> river, final int maxCount) {

        mRiver = river;
        mMaxCount = maxCount;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onFailure(final DownloadFailure download) {

        final URI uri = download.getUri();

        final Integer count = mRetryCounts.get(uri);

        final int currentCount = (count != null) ? count : 0;

        if (currentCount < mMaxCount) {

            mRetryCounts.put(uri, currentCount + 1);
            mRiver.push(new Download(uri, download.getFile()));

        } else {

            mRetryCounts.remove(uri);
            downRiver().push(download);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public DownloadSuccess onSuccess(final DownloadSuccess download) {

        mRetryCounts.remove(download.getUri());

        return download;
    }
}
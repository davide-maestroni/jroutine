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
package com.bmd.wtf.example4;

import com.bmd.wtf.example2.ConsumeObserver;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.xtr.qdc.QueueArchway;

import java.io.File;
import java.util.HashSet;

/**
 * Observer of downloaded urls supporting abort operation.
 */
public class CancelableObserver extends ConsumeObserver {

    private final HashSet<String> mAbortedDownloadUrls = new HashSet<String>();

    public CancelableObserver(final QueueArchway<String> archway, final File downloadDir) {

        super(archway, downloadDir);
    }

    @Override
    public void onDischarge(final Floodgate<String, String> gate, final String drop) {

        // A new download is requested so we remove the url from the aborted ones

        mAbortedDownloadUrls.remove(drop);

        super.onDischarge(gate, drop);
    }

    @Override
    public void onDrop(final Floodgate<String, String> gate, final Object debris) {

        if (debris instanceof AbortException) {

            final AbortException error = (AbortException) debris;

            final String url = error.getMessage();

            mAbortedDownloadUrls.add(url);

            onFailure(url, error);
        }

        super.onDrop(gate, debris);
    }

    @Override
    protected void onComplete(final String url) {

        if (mAbortedDownloadUrls.remove(url)) {

            onFailure(url, new AbortException(url));

        } else {

            super.onComplete(url);
        }
    }
}
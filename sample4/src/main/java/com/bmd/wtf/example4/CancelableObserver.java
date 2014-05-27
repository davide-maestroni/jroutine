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

import com.bmd.wtf.example1.DownloadObserver;
import com.bmd.wtf.example1.DownloadUtils;
import com.bmd.wtf.src.Floodgate;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Observer of downloaded urls supporting abort operation.
 */
public class CancelableObserver extends DownloadObserver {

    private final HashSet<String> mAbortedDownloadUrls = new HashSet<String>();

    private final File mDir;

    private final int mMaxThreads;

    private final ArrayList<String> mPendingUrls = new ArrayList<String>();

    public CancelableObserver(final File downloadDir, final int maxThreads) {

        mDir = downloadDir;
        mMaxThreads = maxThreads;
    }

    @Override
    public void onDischarge(final Floodgate<String, String> gate, final String drop) {

        // A new download is requested so we remove the url from the aborted ones

        mAbortedDownloadUrls.remove(drop);

        if (downloading().size() < mMaxThreads) {

            // Discharge only if at least one stream is available

            return super.onDischarge(gate, drop);
        }

        final ArrayList<String> pendingUrls = mPendingUrls;

        if (!pendingUrls.contains(drop)) {

            // Add the url to the pending ones only if not already present

            pendingUrls.add(drop);
        }

        return null;
    }

    @Override
    public void onDrop(final Floodgate<String, String> gate, final Object debris) {

        if (debris instanceof AbortException) {

            final String url = ((AbortException) debris).getMessage();

            if (downloaded().remove(url)) {

                // If already downloaded just delete it

                delete(url);

            } else if (downloading().contains(url)) {

                // If still in progress wait for completion

                mAbortedDownloadUrls.add(url);
            }

            mPendingUrls.remove(url);
        }

        return super.onDrop(gate, debris);
    }

    @Override
    public Object onPullDebris(final Floodgate<String, String> gate, final Object debris) {

        final String url;

        if (debris instanceof String) {

            url = (String) debris;

        } else if (debris instanceof Throwable) {

            url = ((Throwable) debris).getMessage();

        } else {

            url = null;
        }

        final Object outDebris = super.onPullDebris(gate, debris);

        if (mAbortedDownloadUrls.remove(url)) {

            System.out.println("Download aborted: " + url);

            downloaded().remove(url);

            delete(url);
        }

        if (downloading().size() < mMaxThreads) {

            // If a stream became available we check for the presence of pending urls

            final ArrayList<String> pendingUrls = mPendingUrls;

            if (!pendingUrls.isEmpty()) {

                super.onDischarge(gate, pendingUrls.remove(0));
            }
        }

        return outDebris;
    }

    private boolean delete(final String url) {

        try {

            final File file = new File(mDir, DownloadUtils.getFileName(new URL(url)));
            return file.delete();

        } catch (final MalformedURLException ignored) {

        }

        return false;
    }
}
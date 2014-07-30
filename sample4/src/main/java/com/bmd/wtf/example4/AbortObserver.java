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

import com.bmd.wtf.example1.Download;
import com.bmd.wtf.example1.DownloadFailure;
import com.bmd.wtf.example1.DownloadSuccess;
import com.bmd.wtf.example2.DownloadObserver;

import java.net.URI;
import java.util.HashSet;

/**
 * Observer of downloaded urls supporting abort operation by remembering the ongoing donwloads and
 * then cancel them when completed.
 */
public class AbortObserver extends DownloadObserver implements UriAbortObserver {

    private final HashSet<URI> mAbortedDownloads = new HashSet<URI>();

    @Override
    public void abort(final URI uri) {

        if (!isDownloaded(uri)) {

            mAbortedDownloads.add(uri);
        }
    }

    @Override
    public void onDownload(final Download download) {

        super.onDownload(download);

        final URI uri = download.getUri();

        if (isDownloading(uri)) {

            // A new download is requested so we remove the url from the aborted ones
            mAbortedDownloads.remove(uri);
        }
    }

    @Override
    public void onFailure(final DownloadFailure download) {

        super.onFailure(download);

        final URI uri = download.getUri();

        if (!isDownloading(uri)) {

            mAbortedDownloads.remove(uri);
        }
    }

    @Override
    public void onSuccess(final DownloadSuccess download) {

        if (!mAbortedDownloads.remove(download.getUri())) {

            super.onSuccess(download);

        } else {

            super.onFailure(new DownloadFailure(download.getDownload(), new AbortException()));
        }
    }
}
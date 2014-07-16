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

import com.bmd.wtf.example1.Download;
import com.bmd.wtf.example1.DownloadFailure;
import com.bmd.wtf.example1.DownloadSuccess;
import com.bmd.wtf.example4.AbortException;
import com.bmd.wtf.rpd.RapidLeap;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;

/**
 * Class responsible for the handling of download operations.
 */
public class CancelableDownloader extends RapidLeap<Object> {

    private static final int CHUNK_SIZE = 1024;

    private final byte[] mBuffer = new byte[CHUNK_SIZE];

    private final HashSet<URI> mAborted = new HashSet<URI>();

    private final HashSet<URI> mDownloading = new HashSet<URI>();

    @SuppressWarnings("UnusedDeclaration")
    public void onAbort(final DownloadAbort download) {

        final URI uri = download.getUri();

        if (mDownloading.contains(uri)) {

            mAborted.add(uri);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onDownload(final Download download) {

        try {

            final DownloadHandler handler = new DownloadHandler(download);

            final int error = handler.getError();

            if (error != 0) {

                downRiver().push(new DownloadFailure(download, error));

            } else {

                mDownloading.add(download.getUri());

                upRiver().push(handler);
            }

        } catch (final IOException e) {

            downRiver().push(new DownloadFailure(download, e));
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onHandler(final DownloadHandler handler) {

        final URI uri = handler.getUri();
        final HashSet<URI> downloading = mDownloading;

        if (mAborted.remove(uri)) {

            handler.abort();

            downloading.remove(uri);

            downRiver().push(new DownloadFailure(handler, new AbortException()));

        } else {

            downloading.add(uri);

            try {

                if (handler.transferBytes(mBuffer)) {

                    upRiver().push(handler);

                } else {

                    downRiver().push(new DownloadSuccess(handler));
                }

            } catch (final IOException e) {

                downloading.remove(uri);

                downRiver().push(new DownloadFailure(handler, e));
            }
        }
    }
}
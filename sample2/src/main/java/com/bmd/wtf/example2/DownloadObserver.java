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
package com.bmd.wtf.example2;

import com.bmd.wtf.example1.Download;
import com.bmd.wtf.example1.DownloadFailure;
import com.bmd.wtf.example1.DownloadSuccess;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.DataFlow;
import com.bmd.wtf.xtr.rpd.RapidLeap;

import java.io.File;
import java.net.URI;
import java.util.HashMap;

/**
 * Observer of downloaded urls filtering the ones already in progress.
 */
public class DownloadObserver extends RapidLeap implements UriObserver {

    private final HashMap<URI, Download> mDownloaded = new HashMap<URI, Download>();

    private final HashMap<URI, Download> mDownloading = new HashMap<URI, Download>();

    public DownloadObserver() {

        super(ValidFlows.ANNOTATED_ONLY);
    }

    @Override
    public boolean isDownloaded(final URI uri) {

        return mDownloaded.containsKey(uri);
    }

    @Override
    public boolean isDownloading(final URI uri) {

        return mDownloading.containsKey(uri);
    }

    @DataFlow
    public void onDownload(final Download download) {

        final URI uri = download.getUri();
        final HashMap<URI, Download> downloading = mDownloading;

        if (!downloading.containsKey(uri)) {

            downloading.put(uri, download);
            mDownloaded.remove(uri);

            downRiver().push(download);
        }
    }

    @DataFlow
    public void onFailure(final DownloadFailure download) {

        final URI uri = download.getUri();
        final HashMap<URI, Download> downloading = mDownloading;

        if (downloading.get(uri) == download.getDownload()) {

            System.out.println("Download failed: " + uri);

            downloading.remove(uri);

            delete(download.getFile());
        }
    }

    @DataFlow
    public void onSuccess(final DownloadSuccess download) {

        final URI uri = download.getUri();
        final HashMap<URI, Download> downloading = mDownloading;

        if (downloading.get(uri) == download.getDownload()) {

            System.out.println("Download complete: " + uri);

            downloading.remove(uri);

            mDownloaded.put(uri, download);
        }
    }

    private boolean delete(final File file) {

        return file.delete();
    }
}
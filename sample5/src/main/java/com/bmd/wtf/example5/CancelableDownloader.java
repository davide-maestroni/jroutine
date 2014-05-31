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

import com.bmd.wtf.bdr.FloatingException;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.example1.DownloadUtils;
import com.bmd.wtf.example4.AbortException;
import com.bmd.wtf.src.Floodgate;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * Class responsible for the handling of download operations.
 */
public class CancelableDownloader extends OpenDam<String> {

    private static final int CHUNK_SIZE = 1024;

    private final byte[] mBuffer = new byte[CHUNK_SIZE];

    private final HashSet<String> mAborted = new HashSet<String>();

    private final File mDir;

    private final HashSet<String> mDownloading = new HashSet<String>();

    public CancelableDownloader(final File downloadDir) {

        mDir = downloadDir;
    }

    @Override
    public void onDischarge(final Floodgate<String, String> gate, final String drop) {

        gate.redropAfter(0, TimeUnit.MILLISECONDS, new Download(drop));
    }

    @Override
    public void onDrop(final Floodgate<String, String> gate, final Object debris) {

        if (debris instanceof Download) {

            onDownload(gate, (Download) debris);

        } else if (debris instanceof AbortException) {

            final String url = ((AbortException) debris).getMessage();

            if (mDownloading.contains(url)) {

                mAborted.add(url);
            }

        } else {

            super.onDrop(gate, debris);
        }
    }

    private void onDownload(final Floodgate<String, String> gate, final Download download) {

        final String url = download.getUrl();

        if (mAborted.remove(url)) {

            download.abort();

            mDownloading.remove(url);

            gate.drop(new AbortException(url));

        } else {

            mDownloading.add(url);

            try {

                if (download.tranferBytes(new File(mDir, DownloadUtils.getFileName(new URL(url))), mBuffer)) {

                    gate.redropAfter(0, TimeUnit.MILLISECONDS, download);

                } else {

                    mDownloading.remove(url);

                    gate.discharge(url);
                }

            } catch (final IOException e) {

                download.abort();

                throw new FloatingException(url, e);
            }
        }
    }
}
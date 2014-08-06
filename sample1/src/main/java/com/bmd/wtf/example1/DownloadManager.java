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
package com.bmd.wtf.example1;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.crr.Currents;
import com.bmd.wtf.xtr.rpd.RapidLeap;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.bmd.wtf.fll.Waterfall.fall;

/**
 * Simple download manager implementation.<br/>
 * The approach is to create a new waterfall for each download (like RxJava does).
 */
public class DownloadManager {

    private final Current mCurrent;

    private final File mDownloadDir;

    public DownloadManager(final int maxThreads, final File downloadDir) throws IOException {

        if (!downloadDir.isDirectory() && !downloadDir.mkdirs()) {

            throw new IOException(
                    "Could not create temp directory: " + downloadDir.getAbsolutePath());
        }

        mDownloadDir = downloadDir;
        mCurrent = Currents.pool(maxThreads);
    }

    public static void main(final String args[]) throws IOException, URISyntaxException {

        final int maxThreads = Integer.parseInt(args[0]);

        final File tempDir = new File(args[1]);

        final DownloadManager manager = new DownloadManager(maxThreads, tempDir);

        for (int i = 2; i < args.length; i++) {

            manager.download(new URI(args[i]));
        }
    }

    public boolean download(final URI uri) throws URISyntaxException {

        final Download download =
                new Download(uri, new File(mDownloadDir, DownloadUtils.getFileName(uri)));

        return (Boolean) fall().in(mCurrent).chain(new Downloader()).chain(new RapidLeap() {

            @SuppressWarnings("UnusedDeclaration")
            public boolean onSuccess(final DownloadSuccess ignored) {

                return true;
            }

            @SuppressWarnings("UnusedDeclaration")
            public boolean onFailure(final DownloadFailure ignored) {

                return false;
            }

        }).pull(download).next();
    }
}
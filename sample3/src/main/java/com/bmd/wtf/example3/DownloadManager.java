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
import com.bmd.wtf.example1.DownloadUtils;
import com.bmd.wtf.example1.Downloader;
import com.bmd.wtf.example2.DownloadObserver;
import com.bmd.wtf.example2.UriObserver;
import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.xtr.rpd.Rapid;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.bmd.wtf.fll.Waterfall.fall;

/**
 * Optimized download manager with support for retry.<br/>
 * The same waterfall, split in several parallel streams, is re-used for every download.
 */
public class DownloadManager {

    private final File mDownloadDir;

    private final UriObserver mGate;

    private final Waterfall<Object, Object, Object> mWaterfall;

    public DownloadManager(final int maxThreads, final File downloadDir) throws IOException {

        if (!downloadDir.isDirectory() && !downloadDir.mkdirs()) {

            throw new IOException(
                    "Could not create temp directory: " + downloadDir.getAbsolutePath());
        }

        mDownloadDir = downloadDir;
        final Waterfall<Object, Object, Object> waterfall = fall().bridge()
                                                                  .start(new DownloadObserver())
                                                                  .inBackground(maxThreads)
                                                                  .distribute()
                                                                  .chain(Rapid.gateGenerator(
                                                                          Downloader.class));
        // chain the retry gates
        waterfall.chain(Rapid.gateGenerator(RetryPolicy.class, waterfall));
        // merge the streams and finally chain the observer
        mWaterfall = waterfall.in(1).chain(Classification.ofType(DownloadObserver.class));
        mGate = Rapid.bridge(waterfall.on(DownloadObserver.class)).performAs(UriObserver.class);
    }

    public static void main(final String args[]) throws IOException, URISyntaxException {

        final int maxThreads = Integer.parseInt(args[0]);

        final File tempDir = new File(args[1]);

        final DownloadManager manager = new DownloadManager(maxThreads, tempDir);

        for (int i = 2; i < args.length; i++) {

            manager.download(new URI(args[i]));
        }
    }

    public void download(final URI uri) throws URISyntaxException {

        mWaterfall.source()
                  .push(new Download(uri, new File(mDownloadDir, DownloadUtils.getFileName(uri))));
    }

    public boolean isComplete(final URI uri) {

        return !mGate.isDownloading(uri);
    }

    public boolean isDownloaded(final URI uri) {

        return mGate.isDownloaded(uri);
    }
}
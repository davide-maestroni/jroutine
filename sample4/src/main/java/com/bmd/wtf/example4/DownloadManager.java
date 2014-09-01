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
import com.bmd.wtf.example1.DownloadUtils;
import com.bmd.wtf.example1.Downloader;
import com.bmd.wtf.example3.RetryPolicy;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.xtr.rpd.Rapid;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.bmd.wtf.fll.Waterfall.fall;

/**
 * Download manager with support for abort operation.
 */
public class DownloadManager {

    private final File mDownloadDir;

    private final AbortFilter mGate;

    private final Waterfall<Object, Object, ?> mSource;

    public DownloadManager(final int maxThreads, final File downloadDir) throws IOException {

        if (!downloadDir.isDirectory() && !downloadDir.mkdirs()) {

            throw new IOException(
                    "Could not create temp directory: " + downloadDir.getAbsolutePath());
        }

        mDownloadDir = downloadDir;

        final Waterfall<Object, Object, Object> waterfall = fall().start(new AbortDownloadFilter())
                                                                  .inBackground(maxThreads)
                                                                  .distribute()
                                                                  .chain(Rapid.gateGenerator(
                                                                          Downloader.class));
        // chain the retry gates then merge the streams and finally chain the observer
        mSource = waterfall.chain(Rapid.gateGenerator(RetryPolicy.class, waterfall, 3))
                           .in(1)
                           .chain(new AbortDownloadObserver(
                                   waterfall.source().bridge(AbortFilter.class)))
                           .source();
        mGate = Rapid.bridge(waterfall.source().bridge(AbortFilter.class)).visit();
    }

    public static void main(final String args[]) throws IOException, URISyntaxException {

        final int maxThreads = Integer.parseInt(args[0]);

        final File tempDir = new File(args[1]);

        final DownloadManager manager = new DownloadManager(maxThreads, tempDir);

        for (int i = 2; i < args.length; i++) {

            manager.download(new URI(args[i]));
        }
    }

    public void abort(final URI uri) {

        mGate.abort(uri);
    }

    public void download(final URI uri) throws URISyntaxException {

        mSource.push(new Download(uri, new File(mDownloadDir, DownloadUtils.getFileName(uri))));
    }

    public boolean isComplete(final URI uri) {

        return !mGate.isDownloading(uri);
    }

    public boolean isDownloaded(final URI uri) {

        return mGate.isDownloaded(uri);
    }
}
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

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.flw.Flow;
import com.bmd.wtf.flw.Flows;
import com.bmd.wtf.xtr.flood.FloodControl;

import java.io.File;
import java.io.IOException;

/**
 * Simple download manager implementation.<br/>
 * The approach is to create a new waterfall for each download (like RxJava does).
 */
public class DownloadManager {

    private final FloodControl<String, String, Downloaded> mControl =
            new FloodControl<String, String, Downloaded>(Downloaded.class);

    private final File mDownloadDir;

    private final DownloadedObserver mDownloaded = new DownloadedObserver();

    private final Flow mFlow;

    public DownloadManager(final int maxThreads, final File downloadDir) throws IOException {

        if (!downloadDir.isDirectory() && !downloadDir.mkdirs()) {

            throw new IOException(
                    "Could not create temp directory: " + downloadDir.getAbsolutePath());
        }

        mDownloadDir = downloadDir;
        mFlow = Flows.threadPoolFlow(maxThreads);
    }

    public static void main(final String args[]) throws IOException {

        final int maxThreads = Integer.parseInt(args[0]);

        final File tempDir = new File(args[1]);

        final DownloadManager manager = new DownloadManager(maxThreads, tempDir);

        for (int i = 2; i < args.length; i++) {

            manager.download(args[i]);
        }
    }

    public void download(final String url) {

        Waterfall.flowingInto(mFlow).thenFlowingThrough(new Downloader(mDownloadDir))
                 .thenFlowingThrough(mControl.leveeControlledBy(mDownloaded)).backToSource()
                 .discharge(url);
    }

    public boolean isComplete(final String url) {

        return mControl.controller().isDownloaded(url);
    }
}
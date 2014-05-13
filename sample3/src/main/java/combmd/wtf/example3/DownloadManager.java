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
package combmd.wtf.example3;

import com.bmd.wtf.Waterfall;
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.example1.Downloaded;
import com.bmd.wtf.example1.DownloadedObserver;
import com.bmd.wtf.example1.Downloader;
import com.bmd.wtf.example2.Balancer;
import com.bmd.wtf.flw.Flows;
import com.bmd.wtf.src.Spring;
import com.bmd.wtf.xtr.array.DamFactory;
import com.bmd.wtf.xtr.array.FlowFactories;
import com.bmd.wtf.xtr.array.WaterfallArray;
import com.bmd.wtf.xtr.flood.FloodControl;

import java.io.File;
import java.io.IOException;

/**
 * Download manager with the addition of a retry module.
 */
public class DownloadManager {

    private final FloodControl<String, String, Downloaded> mControl =
            new FloodControl<String, String, Downloaded>(Downloaded.class);

    private final Spring<String> mSpring;

    public DownloadManager(final int maxThreads, final File downloadDir) throws IOException {

        if (!downloadDir.isDirectory() && !downloadDir.mkdirs()) {

            throw new IOException(
                    "Could not create temp directory: " + downloadDir.getAbsolutePath());
        }

        mSpring = WaterfallArray.formingFrom(Waterfall.fallingFrom(new OpenDam<String>()))
                                .thenSplittingIn(maxThreads)
                                .thenFlowingThrough(new Balancer<String>(maxThreads))
                                .thenFlowingInto(FlowFactories.singletonFlowFactory(
                                        Flows.threadPoolFlow(maxThreads)))
                                .thenFlowingThrough(new DamFactory<String, String>() {

                                    @Override
                                    public Dam<String, String> createForStream(
                                            final int streamNumber) {

                                        return new RetryPolicy<String>(3);
                                    }
                                }).thenFlowingThrough(new DamFactory<String, String>() {

                    @Override
                    public Dam<String, String> createForStream(final int streamNumber) {

                        return new Downloader(downloadDir);
                    }
                }).thenJoiningThrough(mControl.leveeControlledBy(new DownloadedObserver()))
                                .backToSource();
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

        mSpring.discharge(url);
    }

    public boolean isComplete(final String url) {

        return mControl.controller().isDownloaded(url);
    }
}
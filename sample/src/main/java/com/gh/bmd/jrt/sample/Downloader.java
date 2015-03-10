/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.sample;

import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.invocation.Invocations;
import com.gh.bmd.jrt.routine.JRoutine;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.time.TimeDuration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;

import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.gh.bmd.jrt.time.TimeDuration.seconds;

/**
 * The downloader implementation.
 * <p/>
 * Created by davide on 10/17/14.
 */
public class Downloader {

    private final HashMap<URI, OutputChannel<Boolean>> mDownloadMap =
            new HashMap<URI, OutputChannel<Boolean>>();

    private final HashSet<URI> mDownloadedSet = new HashSet<URI>();

    private final Routine<URI, Chunk> mReadConnection;

    public Downloader(final int maxParallelDownloads) {

        mReadConnection = JRoutine.on(Invocations.factoryOf(ReadConnection.class))
                                  .withConfiguration(
                                          builder().withMaxInvocations(maxParallelDownloads)
                                                   .withAvailableTimeout(seconds(30))
                                                   .buildConfiguration())
                                  .buildRoutine();
    }

    public static String getFileName(final URI uri) {

        final String path = uri.getPath();
        final String fileName = path.substring(path.lastIndexOf('/') + 1);

        if (fileName.equals("")) {

            return Long.toString(path.hashCode()) + ".tmp";
        }

        return fileName;
    }

    public static void main(final String args[]) throws IOException, URISyntaxException {

        final File downloadDir = new File(args[0]);
        final Downloader manager = new Downloader(Integer.parseInt(args[1]));

        for (int i = 2; i < args.length; i++) {

            final URI uri = new URI(args[i]);
            manager.download(uri, new File(downloadDir, getFileName(uri)));
        }
    }

    public boolean abort(final URI uri) {

        final OutputChannel<Boolean> channel = mDownloadMap.remove(uri);
        return (channel != null) && channel.abort();
    }

    public boolean abortAndWait(final URI uri, final TimeDuration timeout) {

        final OutputChannel<Boolean> channel = mDownloadMap.remove(uri);
        return (channel != null) && channel.abort() && channel.afterMax(timeout).checkComplete();
    }

    public void download(final URI uri, final File dstFile) {

        final HashMap<URI, OutputChannel<Boolean>> downloadMap = mDownloadMap;

        if (!downloadMap.containsKey(uri)) {

            mDownloadedSet.remove(uri);

            final Routine<Chunk, Boolean> writeFile =
                    JRoutine.on(Invocations.withArgs(dstFile).factoryOf(WriteFile.class))
                            .withConfiguration(builder().withInputSize(8)
                                                        .withInputTimeout(seconds(30))
                                                        .buildConfiguration())
                            .buildRoutine();

            try {

                downloadMap.put(uri, writeFile.callAsync(mReadConnection.callAsync(uri)));

            } catch (final InvocationException ignored) {

            }
        }
    }

    public boolean isDownloaded(final URI uri) {

        return waitDone(uri, TimeDuration.ZERO);
    }

    public boolean isDownloading(final URI uri) {

        return mDownloadMap.containsKey(uri);
    }

    public boolean waitDone(final URI uri, final TimeDuration timeout) {

        final HashMap<URI, OutputChannel<Boolean>> downloadMap = mDownloadMap;
        final OutputChannel<Boolean> channel = downloadMap.get(uri);

        if (channel != null) {

            try {

                if (channel.afterMax(timeout).checkComplete()) {

                    downloadMap.remove(uri);

                    if (channel.readNext()) {

                        mDownloadedSet.add(uri);
                        return true;
                    }
                }

            } catch (final InvocationException ignored) {

                downloadMap.remove(uri);
            }
        }

        return mDownloadedSet.contains(uri);
    }
}

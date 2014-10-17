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
package com.bmd.jrt.sample;

import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.time.TimeDuration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;

import static com.bmd.jrt.common.ClassToken.tokenOf;
import static com.bmd.jrt.routine.JavaRoutine.on;

/**
 * The downloader implementation.
 * <p/>
 * Created by davide on 10/17/14.
 */
public class Downloader {

    private final HashMap<URI, OutputChannel<Boolean>> mDownloadMap =
            new HashMap<URI, OutputChannel<Boolean>>();

    private final HashSet<URI> mDownloadedSet = new HashSet<URI>();

    private final Routine<URI, Chunk> mReadConnection =
            on(tokenOf(ReadConnection.class)).buildRoutine();

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
        final Downloader manager = new Downloader();

        for (int i = 1; i < args.length; i++) {

            final URI uri = new URI(args[i]);
            manager.download(uri, new File(downloadDir, getFileName(uri)));
        }
    }

    public boolean abort(final URI uri) {

        final HashMap<URI, OutputChannel<Boolean>> downloadMap = mDownloadMap;
        final OutputChannel<Boolean> channel = downloadMap.get(uri);

        if (channel != null) {

            try {

                return channel.abort();

            } finally {

                downloadMap.remove(uri);
            }
        }

        return false;
    }

    public boolean abort(final URI uri, final TimeDuration timeout) {

        final HashMap<URI, OutputChannel<Boolean>> downloadMap = mDownloadMap;
        final OutputChannel<Boolean> channel = downloadMap.get(uri);

        if (channel != null) {

            try {

                if (channel.abort()) {

                    return channel.afterMax(timeout).waitComplete();
                }

            } finally {

                downloadMap.remove(uri);
            }
        }

        return false;
    }

    public void download(final URI uri, final File dst) {

        final HashMap<URI, OutputChannel<Boolean>> downloadMap = mDownloadMap;

        if (!downloadMap.containsKey(uri)) {

            mDownloadedSet.remove(uri);

            final Routine<Chunk, Boolean> writeFile =
                    on(tokenOf(WriteFile.class)).withArgs(dst).buildRoutine();

            downloadMap.put(uri, writeFile.runAsync(mReadConnection.runAsync(uri)));
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

                if (channel.afterMax(timeout).readFirst()) {

                    mDownloadedSet.add(uri);

                    return true;
                }

            } catch (final NoSuchElementException ignored) {

            } catch (final RoutineException ignored) {

                downloadMap.remove(uri);
            }
        }

        return mDownloadedSet.contains(uri);
    }
}
/*
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
package com.gh.bmd.jrt.sample;

import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.core.JRoutine;
import com.gh.bmd.jrt.invocation.Invocations;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.time.TimeDuration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;

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

    /**
     * Constructor.
     *
     * @param maxParallelDownloads the max number of parallel downloads running at the same time.
     */
    public Downloader(final int maxParallelDownloads) {

        // the read connection invocation is stateless so we can just use a single instance of it
        mReadConnection = JRoutine.on(new ReadConnection()).withRoutine()
                // by setting the maximum number of parallel invocations we effectively limit the
                // number of parallel downloads...
                .withMaxInvocations(maxParallelDownloads)
                        // ...though we need to set a timeout in case the downloads outnumber it
                .withAvailInvocationTimeout(seconds(30)).set().buildRoutine();
    }

    /**
     * Utility method to get the name of the downloaded file from its URI.
     *
     * @param uri the URI of the resource to download.
     * @return the file name.
     */
    public static String getFileName(final URI uri) {

        final String path = uri.getPath();
        final String fileName = path.substring(path.lastIndexOf('/') + 1);

        if (fileName.equals("")) {

            return Long.toString(path.hashCode()) + ".tmp";
        }

        return fileName;
    }

    /**
     * Main.<br/>
     * The first argument is the path to the download directory, the second one is the maximum
     * number of parallel downloads, and all the further ones are the URIs of the resources to
     * download.
     *
     * @param args the arguments.
     * @throws IOException        if an I/O error occurred.
     * @throws URISyntaxException if one of the specified URIs is not correctly formatted.
     */
    public static void main(final String args[]) throws IOException, URISyntaxException {

        final File downloadDir = new File(args[0]);
        final Downloader downloader = new Downloader(Integer.parseInt(args[1]));

        for (int i = 2; i < args.length; i++) {

            final URI uri = new URI(args[i]);
            downloader.download(uri, new File(downloadDir, getFileName(uri)));
        }
    }

    /**
     * Aborts the download of the specified URI.
     *
     * @param uri the URI.
     * @return whether the download was running and has been successfully aborted.
     */
    public boolean abort(final URI uri) {

        final OutputChannel<Boolean> channel = mDownloadMap.remove(uri);
        return (channel != null) && channel.abort();
    }

    /**
     * Aborts the download of the specified URI by waiting for the specified timeout for completion.
     *
     * @param uri     the URI.
     * @param timeout the time to wait for the abortion to complete.
     * @return whether the download was running and has been successfully aborted before the timeout
     * elapsed.
     */
    public boolean abortAndWait(final URI uri, final TimeDuration timeout) {

        final OutputChannel<Boolean> channel = mDownloadMap.remove(uri);
        return (channel != null) && channel.abort() && channel.afterMax(timeout).checkComplete();
    }

    /**
     * Downloads the specified resources to the destination file.
     *
     * @param uri     the URI of the resource to download.
     * @param dstFile the destination file.
     */
    public void download(final URI uri, final File dstFile) {

        final HashMap<URI, OutputChannel<Boolean>> downloadMap = mDownloadMap;

        // check if we are already downloading the same resource
        if (!downloadMap.containsKey(uri)) {

            // remove it from the downloaded set
            mDownloadedSet.remove(uri);
            // in order to be able to abort the download at any time we need to split the processing
            // between the routine responsible for reading the data from the socket and the one
            // writing the next chunk of bytes to the local file
            // in such way we can abort the download by aborting the writing between two chunks are
            // passed to the specific routine
            // for this reason we store the routine output channel in an internal map
            final Routine<Chunk, Boolean> writeFile =
                    JRoutine.on(Invocations.factoryOf(WriteFile.class))
                            .withRoutine()
                            .withFactoryArgs(dstFile)
                            .withInputMaxSize(8)
                            .withInputTimeout(seconds(30))
                            .set()
                            .buildRoutine();
            downloadMap.put(uri, writeFile.callAsync(mReadConnection.callAsync(uri)));
        }
    }

    /**
     * Checks if the specified resource was successfully downloaded.
     *
     * @param uri the URI of the resource.
     * @return whether the resource was downloaded.
     */
    public boolean isDownloaded(final URI uri) {

        return waitDone(uri, TimeDuration.ZERO);
    }

    /**
     * Checks if the specified resource is currently downloading.
     *
     * @param uri the URI of the resource.
     * @return whether the resource is downloading.
     */
    public boolean isDownloading(final URI uri) {

        return mDownloadMap.containsKey(uri);
    }

    /**
     * Waits for the specified time for the resource to complete downloading.
     *
     * @param uri     the URI of the resource.
     * @param timeout the time to wait for the download to complete.
     * @return whether the resource was successfully downloaded.
     */
    public boolean waitDone(final URI uri, final TimeDuration timeout) {

        final HashMap<URI, OutputChannel<Boolean>> downloadMap = mDownloadMap;
        final OutputChannel<Boolean> channel = downloadMap.get(uri);

        // check if the output channel is in the map, that is, the resource is currently downloading
        if (channel != null) {

            try {

                // wait for the routine to complete
                if (channel.afterMax(timeout).checkComplete()) {

                    // if completed, remove the resource from the download map
                    downloadMap.remove(uri);

                    // read the result
                    if (channel.readNext()) {

                        // if successful, add the resource to the downloaded set
                        mDownloadedSet.add(uri);
                        return true;
                    }
                }

            } catch (final InvocationException ignored) {

                // something went wrong or the routine has been aborted
                // just remove the resource from the download map
                downloadMap.remove(uri);
            }
        }

        // check if the resource is in the downloaded set
        return mDownloadedSet.contains(uri);
    }
}

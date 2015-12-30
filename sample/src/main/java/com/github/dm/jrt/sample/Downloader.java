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
package com.github.dm.jrt.sample;

import com.github.dm.jrt.channel.Channel.OutputChannel;
import com.github.dm.jrt.core.ByteChannel.ByteBuffer;
import com.github.dm.jrt.core.JRoutine;
import com.github.dm.jrt.invocation.InvocationException;
import com.github.dm.jrt.routine.Routine;
import com.github.dm.jrt.runner.Runner;
import com.github.dm.jrt.runner.Runners;
import com.github.dm.jrt.util.TimeDuration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;

import static com.github.dm.jrt.invocation.Invocations.factoryOf;
import static com.github.dm.jrt.util.TimeDuration.seconds;

/**
 * The downloader implementation.
 * <p/>
 * Created by davide-maestroni on 10/17/2014.
 */
public class Downloader {

    private static final Runner sReadRunner = Runners.poolRunner();

    private static final Runner sWriteRunner = Runners.poolRunner(1);

    private final HashSet<URI> mDownloaded = new HashSet<URI>();

    private final HashMap<URI, OutputChannel<Boolean>> mDownloads =
            new HashMap<URI, OutputChannel<Boolean>>();

    private final Routine<URI, ByteBuffer> mReadConnection;

    /**
     * Constructor.
     *
     * @param maxParallelDownloads the max number of parallel downloads running at the same time.
     */
    public Downloader(final int maxParallelDownloads) {

        // The read connection invocation is stateless so we can just use a single instance of it
        mReadConnection = JRoutine.on(new ReadConnection()).withInvocations()
                // Since each download may take a long time to complete, we use a dedicated runner
                .withRunner(sReadRunner)
                        // By setting the maximum number of parallel invocations we effectively
                        // limit the
                        // number of parallel downloads
                .withMaxInstances(maxParallelDownloads).set().buildRoutine();
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
     * @throws java.io.IOException         if an I/O error occurred.
     * @throws java.net.URISyntaxException if one of the specified URIs is not correctly formatted.
     */
    public static void main(final String args[]) throws IOException, URISyntaxException {

        final File downloadDir = new File(args[0]);
        final Downloader downloader = new Downloader(Integer.parseInt(args[1]));

        for (int i = 2; i < args.length; ++i) {

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

        final OutputChannel<Boolean> channel = mDownloads.remove(uri);
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

        final OutputChannel<Boolean> channel = mDownloads.remove(uri);
        return (channel != null) && channel.abort() && channel.afterMax(timeout).checkComplete();
    }

    /**
     * Downloads the specified resources to the destination file.
     *
     * @param uri     the URI of the resource to download.
     * @param dstFile the destination file.
     */
    public void download(final URI uri, final File dstFile) {

        final HashMap<URI, OutputChannel<Boolean>> downloads = mDownloads;

        // Check if we are already downloading the same resource
        if (!downloads.containsKey(uri)) {

            // Remove it from the downloaded set
            mDownloaded.remove(uri);
            // In order to be able to abort the download at any time, we need to split the
            // processing between the routine responsible for reading the data from the socket, and
            // the one writing the next chunk of bytes to the local file
            // In such way we can abort the download between two chunks, while they are passed to
            // the specific routine
            // That's why we store the routine output channel in an internal map
            final Routine<ByteBuffer, Boolean> writeFile =
                    JRoutine.on(factoryOf(WriteFile.class, dstFile)).withInvocations()
                            // Since we want to limit the number of allocated chunks, we have to
                            // make the writing happen in a dedicated runner, so that waiting for
                            // available space becomes allowed
                            .withRunner(sWriteRunner)
                            .withInputMaxSize(32)
                            .withInputTimeout(seconds(30))
                            .set()
                            .buildRoutine();
            downloads.put(uri, writeFile.asyncCall(mReadConnection.asyncCall(uri)));
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

        return mDownloads.containsKey(uri);
    }

    /**
     * Waits for the specified time for the resource to complete the downloading.
     *
     * @param uri     the URI of the resource.
     * @param timeout the time to wait for the download to complete.
     * @return whether the resource was successfully downloaded.
     */
    public boolean waitDone(final URI uri, final TimeDuration timeout) {

        final HashMap<URI, OutputChannel<Boolean>> downloads = mDownloads;
        final OutputChannel<Boolean> channel = downloads.get(uri);

        // Check if the output channel is in the map, that is, the resource is currently downloading
        if (channel != null) {

            try {

                // Wait for the routine to complete
                if (channel.afterMax(timeout).checkComplete()) {

                    // If completed, remove the resource from the download map
                    downloads.remove(uri);
                    // Read the result and, if successful, add the resource to the downloaded set
                    return channel.next() && mDownloaded.add(uri);
                }

            } catch (final InvocationException ignored) {

                // Something went wrong or the routine has been aborted
                // Just remove the resource from the download map
                downloads.remove(uri);
            }
        }

        // Check if the resource is in the downloaded set
        return mDownloaded.contains(uri);
    }
}

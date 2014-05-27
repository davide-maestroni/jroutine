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
import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.example1.DownloadUtils;
import com.bmd.wtf.example4.AbortException;
import com.bmd.wtf.src.Floodgate;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

/**
 * Class responsible for the connection setup and the reading of remote data.
 */
public class InputHandler extends AbstractDam<String, Chunk> {

    private static final int CHUNK_SIZE = 1024;

    private boolean mAborted;

    private long mContentLength;

    private String mCurrent;

    private InputStream mInputStream;

    @Override
    public void onDischarge(final Floodgate<String, Chunk> gate, final String drop) {

        if (mCurrent == null) {

            // Setup new connection

            resetConnection();

            try {

                setupConnection(drop);

            } catch (final IOException e) {

                return new FloatingException(drop, e);
            }

            mCurrent = drop;

        } else if (drop.equals(mCurrent)) {

            if (mAborted) {

                // Abort downloading

                mAborted = false;

                mCurrent = null;

                return null;
            }

        } else {

            // Cannot receive another download request when one is already in progress

            return new FloatingException(mCurrent, drop);
        }

        try {

            // Read a chunk of data

            final Chunk chunk = new Chunk(drop, CHUNK_SIZE);
            chunk.readFrom(mInputStream);

            gate.discharge(chunk);

            if (!chunk.isComplete()) {

                // Call again this dam to continue with the download

                gate.rechargeAfter(0, TimeUnit.MILLISECONDS, drop);

            } else {

                resetConnection();

                mCurrent = null;
            }

        } catch (final IOException e) {

            resetConnection();

            return new FloatingException(drop, e);
        }

        // Let the writer signal the completion

        return null;
    }

    @Override
    public void onDrop(final Floodgate<String, Chunk> gate, final Object debris) {

        if (debris instanceof AbortException) {

            final AbortException ex = (AbortException) debris;

            if (!mAborted && ex.getMessage().equals(mCurrent)) {

                final Chunk chunk = new Chunk(mCurrent, CHUNK_SIZE);

                // Abort the download

                abort();

                // Let's pretend download is complete

                gate.discharge(chunk);

                return null;
            }
        }

        return super.onDrop(gate, debris);
    }

    @Override
    public Object onPullDebris(final Floodgate<String, Chunk> gate, final Object debris) {

        if (debris instanceof Throwable) {

            // An error occurred downstream

            if ((mCurrent != null) && !mAborted) {

                // Abort the download

                abort();

                return null;
            }
        }

        return super.onPullDebris(gate, debris);
    }

    private void abort() {

        mAborted = true;

        resetConnection();
    }

    private void resetConnection() {

        DownloadUtils.safeClose(mInputStream);

        mInputStream = null;
    }

    private void setupConnection(final String url) throws IOException {

        mInputStream = null;

        final URLConnection connection = new URL(url).openConnection();

        // Open the input stream

        final InputStream inputStream = connection.getInputStream();

        if (connection instanceof HttpURLConnection) {

            final int responseCode = ((HttpURLConnection) connection).getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {

                // The request has failed...

                throw new FloatingException(mCurrent, responseCode);
            }
        }

        mContentLength = connection.getContentLengthLong();

        mInputStream = inputStream;
    }
}
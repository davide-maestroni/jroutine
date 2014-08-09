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

import com.bmd.wtf.example1.Download;
import com.bmd.wtf.example1.DownloadUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;

/**
 * Class actually handling the initial connection and the downloading of data.
 */
public class DownloadHandler extends Download {

    private final Download mDownload;

    private int mError;

    private InputStream mInputStream;

    private FileOutputStream mOutputStream;

    public DownloadHandler(final Download download) throws IOException {

        super(download.getUri(), download.getFile());

        mDownload = download;

        resetConnection();

        try {

            setupConnection(download.getUri());

        } catch (final IOException e) {

            resetConnection();

            throw e;
        }
    }

    public void abort() {

        resetOutput();

        resetConnection();
    }

    public Download getDownload() {

        return mDownload;
    }

    public int getError() {

        return mError;
    }

    public boolean transferBytes(final byte[] buffer) throws IOException {

        if ((buffer == null) || (mInputStream == null)) {

            throw new IOException();
        }

        try {

            if (mOutputStream == null) {

                setupOutput();

                return true;

            } else {

                final int read = mInputStream.read(buffer);

                if (read != -1) {

                    mOutputStream.write(buffer, 0, read);

                    return true;
                }

                return false;
            }

        } catch (final IOException e) {

            resetOutput();

            resetConnection();

            throw e;
        }
    }

    private void resetConnection() {

        DownloadUtils.safeClose(mInputStream);

        mInputStream = null;
    }

    private void resetOutput() {

        DownloadUtils.safeClose(mOutputStream);

        mOutputStream = null;

        DownloadUtils.safeDelete(getFile());
    }

    private void setupConnection(final URI uri) throws IOException {

        mInputStream = null;

        final URLConnection connection = uri.toURL().openConnection();

        // Open the input stream

        final InputStream inputStream = connection.getInputStream();

        if (connection instanceof HttpURLConnection) {

            final int responseCode = ((HttpURLConnection) connection).getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {

                // The request has failed...

                mError = responseCode;
            }
        }

        mInputStream = inputStream;
    }

    private void setupOutput() throws IOException {

        mOutputStream = null;

        mOutputStream = new FileOutputStream(getFile());
    }
}
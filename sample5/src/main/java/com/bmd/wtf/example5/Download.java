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
import com.bmd.wtf.example1.DownloadUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Class actually handling the initial connection and the downloading of data.
 */
public class Download {

    private InputStream mInputStream;

    private File mOutputFile;

    private FileOutputStream mOutputStream;

    private String mUrl;

    public Download(final String url) {

        resetConnection();

        try {

            setupConnection(url);

            mUrl = url;

        } catch (final IOException e) {

            resetConnection();

            throw new FloatingException(url, e);
        }
    }

    public void abort() {

        resetOutput(true);

        resetConnection();
    }

    public String getUrl() {

        return mUrl;
    }

    public boolean tranferBytes(final File outFile, final byte[] buffer) {

        if ((outFile == null) || (buffer == null) || (mInputStream == null)) {

            throw new FloatingException(mUrl);
        }

        try {

            if (!outFile.equals(mOutputFile)) {

                resetOutput(false);
            }

            if (mOutputStream == null) {

                setupOutput(outFile);

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

            resetOutput(true);

            resetConnection();

            throw new FloatingException(mUrl, e);
        }
    }

    private void resetConnection() {

        DownloadUtils.safeClose(mInputStream);

        mInputStream = null;
    }

    private void resetOutput(final boolean deleteFile) {

        DownloadUtils.safeClose(mOutputStream);

        mOutputStream = null;

        if (deleteFile && (mOutputFile != null)) {

            //noinspection ResultOfMethodCallIgnored
            mOutputFile.delete();

            mOutputFile = null;
        }
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

                throw new FloatingException(mUrl, responseCode);
            }
        }

        mInputStream = inputStream;
    }

    private void setupOutput(final File outFile) throws IOException {

        mOutputStream = null;

        outFile.deleteOnExit();

        mOutputStream = new FileOutputStream(outFile);
        mOutputFile = outFile;
    }
}
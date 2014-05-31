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

import com.bmd.wtf.bdr.FloatingException;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Class responsible for the actual downloading of data.
 */
public class Downloader extends OpenDam<String> {

    private final File mDir;

    public Downloader(final File downloadDir) {

        mDir = downloadDir;
    }

    @Override
    public void onDischarge(final Floodgate<String, String> gate, final String drop) {

        InputStream inputStream = null;

        FileOutputStream outputStream = null;

        try {

            final URL url = new URL(drop);

            final File outFile = new File(mDir, DownloadUtils.getFileName(url));
            outFile.deleteOnExit();

            final URLConnection connection = url.openConnection();

            // Open the input stream

            inputStream = connection.getInputStream();

            if (connection instanceof HttpURLConnection) {

                final int responseCode = ((HttpURLConnection) connection).getResponseCode();

                if (responseCode < 200 || responseCode >= 300) {

                    // The request has failed...

                    throw new FloatingException(drop, responseCode);
                }
            }

            // Open the output stream

            outputStream = new FileOutputStream(outFile);

            final byte[] buffer = new byte[1024];

            int b;

            while ((b = inputStream.read(buffer)) != -1) {

                outputStream.write(buffer, 0, b);
            }

            // Discharge the url if everything worked as expected

            super.onDischarge(gate, drop);

        } catch (final IOException e) {

            throw new FloatingException(drop, e);

        } finally {

            // Clean up

            DownloadUtils.safeClose(outputStream);
            DownloadUtils.safeClose(inputStream);
        }
    }
}
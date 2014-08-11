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

import com.bmd.wtf.xtr.rpd.RapidGate;

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
public class Downloader extends RapidGate {

    @SuppressWarnings("UnusedDeclaration")
    public void onDownload(final Download download) {

        InputStream inputStream = null;

        FileOutputStream outputStream = null;

        try {

            final URL url = download.getUri().toURL();

            final File outFile = download.getFile();
            outFile.deleteOnExit();

            final URLConnection connection = url.openConnection();

            // open the input stream

            inputStream = connection.getInputStream();

            if (connection instanceof HttpURLConnection) {

                final int responseCode = ((HttpURLConnection) connection).getResponseCode();

                if (responseCode < 200 || responseCode >= 300) {

                    // the request has failed...

                    downRiver().push(new DownloadFailure(download, responseCode));

                    return;
                }
            }

            // open the output stream

            outputStream = new FileOutputStream(outFile);

            final byte[] buffer = new byte[1024];

            int b;

            while ((b = inputStream.read(buffer)) != -1) {

                outputStream.write(buffer, 0, b);
            }

            // push the url if everything worked as expected

            downRiver().push(new DownloadSuccess(download));

        } catch (final IOException e) {

            downRiver().push(new DownloadFailure(download, e));

        } finally {

            // clean up

            DownloadUtils.safeClose(outputStream);
            DownloadUtils.safeClose(inputStream);
        }
    }
}
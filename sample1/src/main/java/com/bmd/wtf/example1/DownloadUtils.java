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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Utility class.
 */
public class DownloadUtils {

    private DownloadUtils() {

    }

    /**
     * Extracts the downloaded file name from the download URL.
     *
     * @param url The URL of the resource to download.
     * @return The downloaded file name.
     */
    public static String getFileName(final URL url) {

        final String path = url.getPath();

        final String fileName = path.substring(path.lastIndexOf('/') + 1);

        if (fileName.equals("")) {

            return Long.toString(System.nanoTime()) + ".tmp";
        }

        return fileName;
    }

    /**
     * Closes the specified input stream in a safe way.
     *
     * @param inputStream The input stream to close.
     */
    public static void safeClose(final InputStream inputStream) {

        if (inputStream != null) {

            try {

                inputStream.close();

            } catch (final IOException e) {

                // ignore it
            }
        }
    }

    /**
     * Closes the specified output stream in a safe way.
     *
     * @param outputStream The output stream to close.
     */
    public static void safeClose(final OutputStream outputStream) {

        if (outputStream != null) {

            try {

                outputStream.close();

            } catch (final IOException e) {

                // ignore it
            }
        }
    }
}
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

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

/**
 * Utility class.
 */
public class DownloadUtils {

    private DownloadUtils() {

    }

    /**
     * Extracts the downloaded file name from the download URL.
     *
     * @param uri The URI of the resource to download.
     * @return The downloaded file name.
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
     * Closes the specified stream in a safe way.
     *
     * @param closeable The stream to close.
     */
    public static void safeClose(final Closeable closeable) {

        if (closeable != null) {

            try {

                closeable.close();

            } catch (final IOException ignored) {

            }
        }
    }
}
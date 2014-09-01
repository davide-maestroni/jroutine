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
package com.bmd.wtf.example2;

import com.bmd.wtf.example1.Download;

import java.net.URI;

/**
 * Filter of downloaded URIs.
 */
public interface DownloadFilter {

    /**
     * Removes the specified URI from the downloading or downloaded ones.
     *
     * @param uri the URI.
     */
    public void cancelDownload(URI uri);

    /**
     * Returns the download object relative to the specified URI only if in progress.
     *
     * @param uri the URI.
     * @return the download or null.
     */
    public Download getDownload(URI uri);

    /**
     * Checks if the download of the specified URI has successfully completed.
     *
     * @param uri the URI.
     * @return whether the download was successful.
     */
    public boolean isDownloaded(URI uri);

    /**
     * Checks if the specified URI is being currently downloaded.
     *
     * @param uri the URI.
     * @return whether the download is in progress.
     */
    public boolean isDownloading(URI uri);

    /**
     * Sets the specified download as successfully completed.
     *
     * @param download the download.
     */
    public void setDownloaded(Download download);
}
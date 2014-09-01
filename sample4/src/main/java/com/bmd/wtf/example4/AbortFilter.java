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
package com.bmd.wtf.example4;

import com.bmd.wtf.example2.DownloadFilter;

import java.net.URI;

/**
 * Filter of downloaded urls supporting the abort of the download.
 */
public interface AbortFilter extends DownloadFilter {

    /**
     * Aborts the downloading of the specified URI.
     *
     * @param uri the URI.
     */
    public void abort(URI uri);

    /**
     * Resets the abort of the specified URI.
     *
     * @param uri the URI.
     * @return whether the downloading of the specified URI was flagged as aborted.
     */
    public boolean reset(URI uri);
}
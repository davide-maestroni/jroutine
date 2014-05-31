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

import com.bmd.wtf.example1.DownloadObserver;
import com.bmd.wtf.example4.AbortException;
import com.bmd.wtf.src.Floodgate;

import java.io.File;

/**
 * Observer of downloaded urls handling abort operation.
 */
public class CancelableObserver extends DownloadObserver {

    public CancelableObserver(final File downloadDir) {

        super(downloadDir);
    }

    @Override
    public void onDrop(final Floodgate<String, String> gate, final Object debris) {

        if (debris instanceof AbortException) {

            final AbortException error = (AbortException) debris;

            final String url = error.getMessage();

            if (isDownloaded(url)) {

                onFailure(url, error);

            } else if (isDownloading(url)) {

                gate.drop(debris);
            }

        } else {

            super.onDrop(gate, debris);
        }
    }
}
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

import com.bmd.wtf.example1.DownloadObserver;
import com.bmd.wtf.xtr.qdc.QueueArchway;

import java.io.File;

/**
 * Observer of downloaded urls ensuring that each stream serves a single download.
 */
public class ConsumeObserver extends DownloadObserver {

    private final QueueArchway<String> mArchway;

    public ConsumeObserver(final QueueArchway<String> archway, final File downloadDir) {

        super(downloadDir);

        mArchway = archway;
    }

    @Override
    protected void onComplete(final String url) {

        super.onComplete(url);

        mArchway.consume(url);
    }

    @Override
    protected void onFailure(final String url, final Throwable error) {

        super.onFailure(url, error);

        mArchway.consume(url);
    }
}
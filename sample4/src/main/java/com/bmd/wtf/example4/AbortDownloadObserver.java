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

import com.bmd.wtf.example1.DownloadFailure;
import com.bmd.wtf.example1.DownloadSuccess;
import com.bmd.wtf.example2.DownloadObserver;
import com.bmd.wtf.flw.Bridge;
import com.bmd.wtf.flw.Bridge.Visitor;

import java.net.URI;

/**
 * Observer of download results which updates the URIs filter through a bridge over it.
 */
public class AbortDownloadObserver extends DownloadObserver {

    private final Bridge<? extends AbortFilter> mBridge;

    private final Visitor<Void, AbortFilter> mFailureVisitor = new Visitor<Void, AbortFilter>() {

        @Override
        public Void doInspect(final AbortFilter gate, final Object... args) {

            final DownloadFailure download = (DownloadFailure) args[0];

            AbortDownloadObserver.super.onFailure(download);

            final URI uri = download.getUri();

            if (!gate.isDownloading(uri)) {

                gate.reset(uri);
            }

            return null;
        }
    };

    private final Visitor<Void, AbortFilter> mSuccessVisitor = new Visitor<Void, AbortFilter>() {

        @Override
        public Void doInspect(final AbortFilter gate, final Object... args) {

            final DownloadSuccess download = (DownloadSuccess) args[0];

            if (!gate.reset(download.getUri())) {

                AbortDownloadObserver.super.onSuccess(download);

            } else {

                AbortDownloadObserver.super.onFailure(
                        new DownloadFailure(download.getDownload(), new AbortException()));
            }

            return null;
        }
    };

    public AbortDownloadObserver(final Bridge<? extends AbortFilter> bridge) {

        super(bridge);

        mBridge = bridge;
    }

    @Override
    public void onFailure(final DownloadFailure download) {

        mBridge.visit(mFailureVisitor, download);
    }

    @Override
    public void onSuccess(final DownloadSuccess download) {

        mBridge.visit(mSuccessVisitor, download);
    }
}
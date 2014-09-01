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

import com.bmd.wtf.example1.DownloadFailure;
import com.bmd.wtf.example1.DownloadSuccess;
import com.bmd.wtf.example1.DownloadUtils;
import com.bmd.wtf.flw.Bridge;
import com.bmd.wtf.flw.Bridge.Visitor;
import com.bmd.wtf.xtr.rpd.RapidAnnotations.DataFlow;
import com.bmd.wtf.xtr.rpd.RapidGate;

import java.net.URI;

/**
 * Observer of download results which updates the URIs filter through a bridge over it.
 */
public class DownloadObserver extends RapidGate {

    private final Bridge<? extends DownloadFilter> mBridge;

    private final Visitor<Void, DownloadFilter> mFailureVisitor =
            new Visitor<Void, DownloadFilter>() {

                @Override
                public Void doInspect(final DownloadFilter gate, final Object... args) {

                    final DownloadFailure download = (DownloadFailure) args[0];
                    final URI uri = download.getUri();

                    if (gate.getDownload(uri) == download.getDownload()) {

                        System.out.println("Download failed: " + uri);

                        gate.cancelDownload(uri);

                        DownloadUtils.safeDelete(download.getFile());
                    }

                    return null;
                }
            };

    private final Visitor<Void, DownloadFilter> mSuccessVisitor =
            new Visitor<Void, DownloadFilter>() {

                @Override
                public Void doInspect(final DownloadFilter gate, final Object... args) {

                    final DownloadSuccess download = (DownloadSuccess) args[0];
                    final URI uri = download.getUri();

                    if (gate.getDownload(uri) == download.getDownload()) {

                        System.out.println("Download complete: " + uri);

                        gate.setDownloaded(download.getDownload());
                    }

                    return null;
                }
            };

    public DownloadObserver(final Bridge<? extends DownloadFilter> bridge) {

        mBridge = bridge;
    }

    @DataFlow
    public void onFailure(final DownloadFailure download) {

        mBridge.visit(mFailureVisitor, download);
    }

    @DataFlow
    public void onSuccess(final DownloadSuccess download) {

        mBridge.visit(mSuccessVisitor, download);
    }
}
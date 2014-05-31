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

import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

/**
 * Observer of downloaded urls filtering the ones already in progress.
 */
public class DownloadObserver extends OpenDam<String> implements UrlObserver {

    private final File mDir;

    private final HashSet<String> mDownloadedUrls = new HashSet<String>();

    private final HashSet<String> mDownloadingUrls = new HashSet<String>();

    public DownloadObserver(final File downloadDir) {

        mDir = downloadDir;
    }

    @Override
    public boolean isDownloaded(final String url) {

        return mDownloadedUrls.contains(url);
    }

    @Override
    public boolean isDownloading(final String url) {

        return mDownloadingUrls.contains(url);
    }

    @Override
    public void onDischarge(final Floodgate<String, String> gate, final String drop) {

        if (mDownloadingUrls.add(drop)) {

            mDownloadedUrls.remove(drop);

            super.onDischarge(gate, drop);
        }
    }

    @Override
    public void onDrop(final Floodgate<String, String> gate, final Object debris) {

        final String url;

        if (debris instanceof String) {

            url = (String) debris;

            if (mDownloadingUrls.remove(url)) {

                System.out.println("Download complete: " + url);

                onComplete(url);
            }

        } else if (debris instanceof Throwable) {

            final Throwable error = (Throwable) debris;

            url = error.getMessage();

            if (mDownloadingUrls.remove(url)) {

                System.out.println("Download failed: " + url);

                onFailure(url, error);
            }

        } else {

            super.onDrop(gate, debris);
        }
    }

    protected void onComplete(final String url) {

        mDownloadedUrls.add(url);
    }

    protected void onFailure(final String url, final Throwable error) {

        mDownloadedUrls.remove(url);

        delete(url);
    }

    private boolean delete(final String url) {

        try {

            final File file = new File(mDir, DownloadUtils.getFileName(new URL(url)));
            return file.delete();

        } catch (final MalformedURLException ignored) {

        }

        return false;
    }
}
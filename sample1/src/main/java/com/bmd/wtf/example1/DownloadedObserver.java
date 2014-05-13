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

import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.src.Floodgate;

import java.util.HashSet;

/**
 * Observer of downloaded files implementation.
 */
public class DownloadedObserver extends AbstractDam<String, String> implements Downloaded {

    private final HashSet<String> mUrls = new HashSet<String>();

    public boolean isDownloaded(final String url) {

        return mUrls.contains(url);
    }

    @Override
    public Object onDischarge(final Floodgate<String, String> gate, final String drop) {

        System.out.println("Downloaded file: " + drop);

        mUrls.add(drop);

        return null;
    }
}
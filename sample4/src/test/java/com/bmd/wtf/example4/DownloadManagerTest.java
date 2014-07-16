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

import com.bmd.wtf.example1.DownloadUtils;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * {@link DownloadManager} unit test.
 */
public class DownloadManagerTest extends TestCase {

    private static final String FAIL_URL = "http://this.domain.does.not.exist/test.txt";

    private static final String SMALL_FILE_URL1 =
            "http://upload.wikimedia.org/wikipedia/commons/4/4a/Logo_2013_Google.png";

    private static final String SMALL_FILE_URL2 =
            "http://upload.wikimedia.org/wikipedia/commons/2/24/Yahoo%21_logo.svg";

    private static final String SMALL_FILE_URL3 =
            "http://upload.wikimedia.org/wikipedia/commons/b/b1/Bing_logo_%282013%29.svg";

    private final DownloadManager mDownloadManager;

    private final String mTmpDirPath;

    public DownloadManagerTest() throws IOException {

        mTmpDirPath = System.getProperty("java.io.tmpdir");
        mDownloadManager = new DownloadManager(2, new File(mTmpDirPath));
    }

    public void testAll() throws IOException, URISyntaxException {

        final URI uri = new URI(FAIL_URL);
        final URI uri1 = new URI(SMALL_FILE_URL1);
        final URI uri2 = new URI(SMALL_FILE_URL2);
        final URI uri3 = new URI(SMALL_FILE_URL3);

        final String fileName = DownloadUtils.getFileName(uri);
        final String fileName1 = DownloadUtils.getFileName(uri1);
        final String fileName2 = DownloadUtils.getFileName(uri2);
        final String fileName3 = DownloadUtils.getFileName(uri3);

        final File outFile = new File(mTmpDirPath, fileName);
        final File outFile1 = new File(mTmpDirPath, fileName1);
        final File outFile2 = new File(mTmpDirPath, fileName2);
        final File outFile3 = new File(mTmpDirPath, fileName3);

        assertThat(outFile).doesNotExist();
        assertThat(outFile1).doesNotExist();
        assertThat(outFile2).doesNotExist();
        assertThat(outFile3).doesNotExist();

        mDownloadManager.download(uri3);
        mDownloadManager.download(uri);
        mDownloadManager.download(uri1);
        mDownloadManager.download(uri2);

        final long startTime = System.currentTimeMillis();

        waitFor(uri, startTime, 30000);
        waitFor(uri1, startTime, 30000);
        waitFor(uri2, startTime, 30000);
        waitFor(uri3, startTime, 30000);

        assertThat(mDownloadManager.isDownloaded(uri)).isFalse();
        assertThat(mDownloadManager.isDownloaded(uri1)).isTrue();
        assertThat(mDownloadManager.isDownloaded(uri2)).isTrue();
        assertThat(mDownloadManager.isDownloaded(uri3)).isTrue();

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();
        assertThat(outFile).doesNotExist();

        mDownloadManager.abort(uri1);
        mDownloadManager.abort(uri2);
        mDownloadManager.abort(uri3);

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();
    }

    public void testDownload() throws IOException, URISyntaxException {

        final URI uri1 = new URI(SMALL_FILE_URL1);
        final URI uri2 = new URI(SMALL_FILE_URL2);
        final URI uri3 = new URI(SMALL_FILE_URL3);

        final String fileName1 = DownloadUtils.getFileName(uri1);
        final String fileName2 = DownloadUtils.getFileName(uri2);
        final String fileName3 = DownloadUtils.getFileName(uri3);

        final File outFile1 = new File(mTmpDirPath, fileName1);
        final File outFile2 = new File(mTmpDirPath, fileName2);
        final File outFile3 = new File(mTmpDirPath, fileName3);

        mDownloadManager.abort(uri1);
        mDownloadManager.abort(uri2);
        mDownloadManager.abort(uri3);

        assertThat(outFile1).doesNotExist();
        assertThat(outFile2).doesNotExist();
        assertThat(outFile3).doesNotExist();

        mDownloadManager.download(uri1);
        mDownloadManager.download(uri2);
        mDownloadManager.download(uri3);

        final long startTime = System.currentTimeMillis();

        waitFor(uri1, startTime, 30000);
        waitFor(uri2, startTime, 30000);
        waitFor(uri3, startTime, 30000);

        assertThat(mDownloadManager.isDownloaded(uri1)).isTrue();
        assertThat(mDownloadManager.isDownloaded(uri2)).isTrue();
        assertThat(mDownloadManager.isDownloaded(uri3)).isTrue();

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();

        mDownloadManager.abort(uri1);
        mDownloadManager.abort(uri2);
        mDownloadManager.abort(uri3);

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();
    }

    public void testFail() throws IOException, URISyntaxException {

        final URI uri = new URI(FAIL_URL);

        final String fileName = DownloadUtils.getFileName(uri);

        final File outFile = new File(mTmpDirPath, fileName);

        assertThat(outFile).doesNotExist();

        mDownloadManager.download(uri);

        final long startTime = System.currentTimeMillis();

        waitFor(uri, startTime, 5000);

        assertThat(mDownloadManager.isDownloaded(uri)).isFalse();

        assertThat(outFile).doesNotExist();
    }

    public void testRepeatedAbort() throws IOException, URISyntaxException {

        final URI uri = new URI(SMALL_FILE_URL1);

        final String fileName = DownloadUtils.getFileName(uri);

        final File outFile = new File(mTmpDirPath, fileName);

        mDownloadManager.abort(uri);

        assertThat(outFile).doesNotExist();

        for (int i = 0; i < 10; i++) {

            mDownloadManager.download(uri);
            mDownloadManager.abort(uri);
        }

        mDownloadManager.abort(uri);
        mDownloadManager.download(uri);
        mDownloadManager.abort(uri);

        final long startTime = System.currentTimeMillis();

        waitFor(uri, startTime, 20000);

        assertThat(mDownloadManager.isDownloaded(uri)).isFalse();

        assertThat(outFile).doesNotExist();
    }

    public void testSimpleAbort() throws IOException, URISyntaxException {

        final URI uri = new URI(SMALL_FILE_URL1);

        final String fileName = DownloadUtils.getFileName(uri);

        final File outFile = new File(mTmpDirPath, fileName);

        mDownloadManager.abort(uri);

        assertThat(outFile).doesNotExist();

        mDownloadManager.download(uri);
        mDownloadManager.abort(uri);

        final long startTime = System.currentTimeMillis();

        waitFor(uri, startTime, 20000);

        assertThat(mDownloadManager.isDownloaded(uri)).isFalse();

        assertThat(outFile).doesNotExist();
    }

    @Override
    protected void setUp() throws Exception {

        super.setUp();

        delete(SMALL_FILE_URL1);
        delete(SMALL_FILE_URL2);
        delete(SMALL_FILE_URL3);
    }

    private boolean delete(final String url) throws MalformedURLException, URISyntaxException {

        return new File(mTmpDirPath, DownloadUtils.getFileName(new URI(url))).delete();
    }

    private void waitFor(final URI uri, final long startTime, final long timeoutMs) throws
            IOException {

        while (!mDownloadManager.isComplete(uri)) {

            try {

                Thread.sleep(100);

            } catch (final InterruptedException ignored) {

            }

            if ((System.currentTimeMillis() - startTime) > timeoutMs) {

                throw new IOException();
            }
        }
    }
}
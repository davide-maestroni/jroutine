/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.sample;

import com.github.dm.jrt.core.util.UnitDuration;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.github.dm.jrt.sample.Downloader.getFileName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Downloader unit tests.
 * <p>
 * Created by davide-maestroni on 10/17/2014.
 */
public class DownloaderTest {

    private static final String FAIL_URL = "https://this.domain.does.not.exist/test.txt";

    private static final String HUGE_FILE_URL =
            "https://dl.google.com/android/studio/install/0.4.6/android-studio-bundle-133"
                    + ".1028713-linux.tgz";

    private static final String SMALL_FILE_URL1 =
            "https://upload.wikimedia.org/wikipedia/commons/4/4a/Logo_2013_Google.png";

    private static final String SMALL_FILE_URL2 =
            "https://upload.wikimedia.org/wikipedia/commons/2/24/Yahoo%21_logo.svg";

    private static final String SMALL_FILE_URL3 =
            "https://upload.wikimedia.org/wikipedia/commons/b/b1/Bing_logo_%282013%29.svg";

    private final Downloader mDownloader;

    private final String mTmpDirPath;

    public DownloaderTest() throws IOException {

        mTmpDirPath = System.getProperty("java.io.tmpdir");
        mDownloader = new Downloader(2);
    }

    @Before
    public void setUp() throws Exception {

        delete(FAIL_URL);
        delete(HUGE_FILE_URL);
        delete(SMALL_FILE_URL1);
        delete(SMALL_FILE_URL2);
        delete(SMALL_FILE_URL3);
    }

    @Test
    public void testAll() throws IOException, URISyntaxException {

        final String tmpDirPath = mTmpDirPath;
        final Downloader downloader = mDownloader;

        final URI uri = new URI(FAIL_URL);
        final URI uriH = new URI(HUGE_FILE_URL);
        final URI uri1 = new URI(SMALL_FILE_URL1);
        final URI uri2 = new URI(SMALL_FILE_URL2);
        final URI uri3 = new URI(SMALL_FILE_URL3);

        final String fileName = getFileName(uri);
        final String fileNameH = getFileName(uriH);
        final String fileName1 = getFileName(uri1);
        final String fileName2 = getFileName(uri2);
        final String fileName3 = getFileName(uri3);

        final File outFile = new File(tmpDirPath, fileName);
        final File outFileH = new File(tmpDirPath, fileNameH);
        final File outFile1 = new File(tmpDirPath, fileName1);
        final File outFile2 = new File(tmpDirPath, fileName2);
        final File outFile3 = new File(tmpDirPath, fileName3);

        assertThat(outFile).doesNotExist();
        assertThat(outFileH).doesNotExist();
        assertThat(outFile1).doesNotExist();
        assertThat(outFile2).doesNotExist();
        assertThat(outFile3).doesNotExist();

        downloader.download(uri, outFile);
        downloader.download(uri3, outFile3);
        downloader.download(uriH, outFileH);
        downloader.download(uri1, outFile1);
        downloader.download(uri2, outFile2);

        downloader.abort(uriH);

        final long startTime = System.currentTimeMillis();

        waitFor(uri, startTime, 30000);
        waitFor(uriH, startTime, 30000);
        waitFor(uri1, startTime, 30000);
        waitFor(uri2, startTime, 30000);
        waitFor(uri3, startTime, 30000);

        assertThat(downloader.isDownloaded(uri)).isFalse();
        assertThat(downloader.isDownloaded(uriH)).isFalse();
        assertThat(downloader.isDownloaded(uri1)).isTrue();
        assertThat(downloader.isDownloaded(uri2)).isTrue();
        assertThat(downloader.isDownloaded(uri3)).isTrue();

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();
        assertThat(outFile).doesNotExist();
        checkNotExists(outFileH);

        downloader.abort(uri1);
        downloader.abort(uri2);
        downloader.abort(uri3);

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();
    }

    @Test
    public void testDownload() throws IOException, URISyntaxException {

        final String tmpDirPath = mTmpDirPath;
        final Downloader downloader = mDownloader;

        final URI uri1 = new URI(SMALL_FILE_URL1);
        final URI uri2 = new URI(SMALL_FILE_URL2);
        final URI uri3 = new URI(SMALL_FILE_URL3);

        final String fileName1 = getFileName(uri1);
        final String fileName2 = getFileName(uri2);
        final String fileName3 = getFileName(uri3);

        final File outFile1 = new File(tmpDirPath, fileName1);
        final File outFile2 = new File(tmpDirPath, fileName2);
        final File outFile3 = new File(tmpDirPath, fileName3);

        downloader.abort(uri1);
        downloader.abort(uri2);
        downloader.abort(uri3);

        assertThat(outFile1).doesNotExist();
        assertThat(outFile2).doesNotExist();
        assertThat(outFile3).doesNotExist();

        downloader.download(uri1, outFile1);
        downloader.download(uri2, outFile2);
        downloader.download(uri3, outFile3);

        assertThat(downloader.isDownloading(uri1));
        assertThat(downloader.isDownloading(uri2));
        assertThat(downloader.isDownloading(uri3));

        final long startTime = System.currentTimeMillis();

        waitFor(uri1, startTime, 30000);
        waitFor(uri2, startTime, 30000);
        waitFor(uri3, startTime, 30000);

        assertThat(downloader.isDownloaded(uri1)).isTrue();
        assertThat(downloader.isDownloaded(uri2)).isTrue();
        assertThat(downloader.isDownloaded(uri3)).isTrue();

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();

        downloader.abort(uri1);
        downloader.abort(uri2);
        downloader.abort(uri3);

        assertThat(outFile1).exists();
        assertThat(outFile2).exists();
        assertThat(outFile3).exists();
    }

    @Test
    public void testFail() throws IOException, URISyntaxException {

        final String tmpDirPath = mTmpDirPath;
        final Downloader downloader = mDownloader;

        final URI uri = new URI(FAIL_URL);

        final String fileName = getFileName(uri);
        final File outFile = new File(tmpDirPath, fileName);

        assertThat(outFile).doesNotExist();

        downloader.download(uri, outFile);

        final long startTime = System.currentTimeMillis();

        waitFor(uri, startTime, 5000);

        assertThat(downloader.isDownloaded(uri)).isFalse();
        checkNotExists(outFile);
    }

    @Test
    public void testRepeatedAbort() throws IOException, URISyntaxException {

        final String tmpDirPath = mTmpDirPath;
        final Downloader downloader = mDownloader;

        final URI uri = new URI(HUGE_FILE_URL);

        final String fileName = getFileName(uri);
        final File outFile = new File(tmpDirPath, fileName);

        downloader.abort(uri);

        assertThat(outFile).doesNotExist();

        for (int i = 0; i < 10; i++) {

            downloader.download(uri, outFile);
            downloader.abort(uri);
        }

        downloader.abort(uri);
        downloader.download(uri, outFile);
        downloader.abort(uri);

        final long startTime = System.currentTimeMillis();

        waitFor(uri, startTime, 20000);

        assertThat(downloader.isDownloaded(uri)).isFalse();
        checkNotExists(outFile);
    }

    @Test
    public void testSimpleAbort() throws IOException, URISyntaxException {

        final String tmpDirPath = mTmpDirPath;
        final Downloader downloader = mDownloader;

        final URI uri = new URI(HUGE_FILE_URL);

        final String fileName = getFileName(uri);
        final File outFile = new File(tmpDirPath, fileName);

        downloader.abort(uri);

        assertThat(outFile).doesNotExist();

        downloader.download(uri, outFile);

        final long startTime = System.currentTimeMillis();

        while (!outFile.exists()) {

            try {

                Thread.sleep(100);

            } catch (final InterruptedException ignored) {

            }

            if ((System.currentTimeMillis() - startTime) > 20000) {

                throw new IOException();
            }
        }

        downloader.abortAndWait(uri, UnitDuration.seconds(20));

        assertThat(downloader.isDownloaded(uri)).isFalse();
        checkNotExists(outFile);
    }

    private void checkNotExists(final File file) {

        try {

            // The only way to be sure that a file does not exists is to read it
            assertThat(new FileInputStream(file).read()).isEqualTo(-1);

        } catch (IOException ignored) {

        }
    }

    private boolean delete(final String url) throws MalformedURLException, URISyntaxException {

        return new File(mTmpDirPath, getFileName(new URI(url))).delete();
    }

    private void waitFor(final URI uri, final long startTime, final long timeoutMs) throws
            IOException {

        final long timeout = startTime + timeoutMs - System.currentTimeMillis();

        if (timeout < 0) {

            throw new IOException();
        }

        mDownloader.waitDone(uri, UnitDuration.millis(timeout));

        if ((startTime + timeoutMs - System.currentTimeMillis()) < 0) {

            throw new IOException();
        }
    }
}

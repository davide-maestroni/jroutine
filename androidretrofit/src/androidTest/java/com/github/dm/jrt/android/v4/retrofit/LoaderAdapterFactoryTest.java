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

package com.github.dm.jrt.android.v4.retrofit;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.retrofit.GitHubService;
import com.github.dm.jrt.android.retrofit.R;
import com.github.dm.jrt.android.retrofit.Repo;
import com.github.dm.jrt.android.v4.core.LoaderContextCompat;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.stream.Streams;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.github.dm.jrt.android.v4.core.LoaderContextCompat.loaderFrom;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader routine adapter factory unit tests.
 * <p>
 * Created by davide-maestroni on 05/19/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class LoaderAdapterFactoryTest extends ActivityInstrumentationTestCase2<TestActivity> {

    private static final String BODY = "[{\"id\":\"1\", \"name\":\"Repo1\"}, {\"id\":\"2\","
            + " \"name\":\"Repo2\"}, {\"id\":\"3\", \"name\":\"Repo3\", \"isPrivate\":true}]";

    public LoaderAdapterFactoryTest() {

        super(TestActivity.class);
    }

    private static void testLoaderStreamChannelAdapter(
            @Nullable final LoaderContextCompat context) throws IOException {

        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.start();
        try {
            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.defaultFactory(context);
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService2 service = retrofit.create(GitHubService2.class);
                assertThat(service.streamLoaderRepos("octocat")
                                  .map(Streams.<Repo>unfold())
                                  .onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  })
                                  .afterMax(seconds(10))
                                  .getError()).isNull();
            }

            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.builder()
                                                  .with(context)
                                                  .invocationMode(InvocationMode.PARALLEL)
                                                  .loaderConfiguration()
                                                  .withResultStaleTime(seconds(3))
                                                  .apply()
                                                  .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService2 service = retrofit.create(GitHubService2.class);
                assertThat(service.streamLoaderRepos("octocat")
                                  .map(Streams.<Repo>unfold())
                                  .onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  })
                                  .afterMax(seconds(10))
                                  .getError()).isNull();
            }

            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.builder()
                                                  .with(context)
                                                  .invocationMode(InvocationMode.SYNC)
                                                  .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService2 service = retrofit.create(GitHubService2.class);
                assertThat(service.streamLoaderRepos("octocat")
                                  .map(Streams.<Repo>unfold())
                                  .onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  })
                                  .afterMax(seconds(10))
                                  .getError()).isNull();
            }

            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.builder()
                                                  .with(context)
                                                  .invocationMode(InvocationMode.SERIAL)
                                                  .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService2 service = retrofit.create(GitHubService2.class);
                assertThat(service.streamLoaderRepos("octocat")
                                  .map(Streams.<Repo>unfold())
                                  .onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  })
                                  .afterMax(seconds(10))
                                  .getError()).isNull();
            }

        } finally {
            server.shutdown();
        }
    }

    private static void testOutputChannelAdapter(@Nullable final LoaderContextCompat context) throws
            IOException {

        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.start();
        try {
            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.builder()
                                                  .with(context)
                                                  .invocationConfiguration()
                                                  .withReadTimeout(seconds(10))
                                                  .apply()
                                                  .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                final List<Repo> repos = service.listRepos("octocat").next();
                assertThat(repos).hasSize(3);
                assertThat(repos.get(0).getId()).isEqualTo("1");
                assertThat(repos.get(0).getName()).isEqualTo("Repo1");
                assertThat(repos.get(0).isPrivate()).isFalse();
                assertThat(repos.get(1).getId()).isEqualTo("2");
                assertThat(repos.get(1).getName()).isEqualTo("Repo2");
                assertThat(repos.get(1).isPrivate()).isFalse();
                assertThat(repos.get(2).getId()).isEqualTo("3");
                assertThat(repos.get(2).getName()).isEqualTo("Repo3");
                assertThat(repos.get(2).isPrivate()).isTrue();
            }

            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.builder()
                                                  .with(context)
                                                  .invocationMode(InvocationMode.PARALLEL)
                                                  .invocationConfiguration()
                                                  .withReadTimeout(seconds(10))
                                                  .apply()
                                                  .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                final List<Repo> repos = service.listRepos("octocat").next();
                assertThat(repos).hasSize(3);
                assertThat(repos.get(0).getId()).isEqualTo("1");
                assertThat(repos.get(0).getName()).isEqualTo("Repo1");
                assertThat(repos.get(0).isPrivate()).isFalse();
                assertThat(repos.get(1).getId()).isEqualTo("2");
                assertThat(repos.get(1).getName()).isEqualTo("Repo2");
                assertThat(repos.get(1).isPrivate()).isFalse();
                assertThat(repos.get(2).getId()).isEqualTo("3");
                assertThat(repos.get(2).getName()).isEqualTo("Repo3");
                assertThat(repos.get(2).isPrivate()).isTrue();
            }

            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.builder()
                                                  .with(context)
                                                  .invocationMode(InvocationMode.SYNC)
                                                  .invocationConfiguration()
                                                  .withReadTimeout(seconds(10))
                                                  .apply()
                                                  .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                final List<Repo> repos = service.listRepos("octocat").next();
                assertThat(repos).hasSize(3);
                assertThat(repos.get(0).getId()).isEqualTo("1");
                assertThat(repos.get(0).getName()).isEqualTo("Repo1");
                assertThat(repos.get(0).isPrivate()).isFalse();
                assertThat(repos.get(1).getId()).isEqualTo("2");
                assertThat(repos.get(1).getName()).isEqualTo("Repo2");
                assertThat(repos.get(1).isPrivate()).isFalse();
                assertThat(repos.get(2).getId()).isEqualTo("3");
                assertThat(repos.get(2).getName()).isEqualTo("Repo3");
                assertThat(repos.get(2).isPrivate()).isTrue();
            }

            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.builder()
                                                  .with(context)
                                                  .invocationMode(InvocationMode.SERIAL)
                                                  .invocationConfiguration()
                                                  .withReadTimeout(seconds(10))
                                                  .apply()
                                                  .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                final List<Repo> repos = service.listRepos("octocat").next();
                assertThat(repos).hasSize(3);
                assertThat(repos.get(0).getId()).isEqualTo("1");
                assertThat(repos.get(0).getName()).isEqualTo("Repo1");
                assertThat(repos.get(0).isPrivate()).isFalse();
                assertThat(repos.get(1).getId()).isEqualTo("2");
                assertThat(repos.get(1).getName()).isEqualTo("Repo2");
                assertThat(repos.get(1).isPrivate()).isFalse();
                assertThat(repos.get(2).getId()).isEqualTo("3");
                assertThat(repos.get(2).getName()).isEqualTo("Repo3");
                assertThat(repos.get(2).isPrivate()).isTrue();
            }

        } finally {
            server.shutdown();
        }
    }

    private static void testStreamChannelAdapter(@Nullable final LoaderContextCompat context) throws
            IOException {

        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.start();
        try {
            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.defaultFactory(context);
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                assertThat(service.streamRepos("octocat")
                                  .map(Streams.<Repo>unfold())
                                  .onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  })
                                  .afterMax(seconds(10))
                                  .getError()).isNull();
            }

            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.builder()
                                                  .with(context)
                                                  .invocationMode(InvocationMode.PARALLEL)
                                                  .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                assertThat(service.streamRepos("octocat")
                                  .map(Streams.<Repo>unfold())
                                  .onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  })
                                  .afterMax(seconds(10))
                                  .getError()).isNull();
            }

            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.builder()
                                                  .with(context)
                                                  .invocationMode(InvocationMode.SYNC)
                                                  .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                assertThat(service.streamRepos("octocat")
                                  .map(Streams.<Repo>unfold())
                                  .onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  })
                                  .afterMax(seconds(10))
                                  .getError()).isNull();
            }

            {
                final LoaderAdapterFactoryCompat adapterFactory =
                        LoaderAdapterFactoryCompat.builder()
                                                  .with(context)
                                                  .invocationMode(InvocationMode.SERIAL)
                                                  .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                assertThat(service.streamRepos("octocat")
                                  .map(Streams.<Repo>unfold())
                                  .onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  })
                                  .afterMax(seconds(10))
                                  .getError()).isNull();
            }

        } finally {
            server.shutdown();
        }
    }

    public void testLoaderStreamChannelAdapter() throws IOException {

        testLoaderStreamChannelAdapter(loaderFrom(getActivity()));
    }

    public void testLoaderStreamChannelAdapterFragment() throws IOException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        testLoaderStreamChannelAdapter(loaderFrom(fragment));
    }

    public void testLoaderStreamChannelAdapterNullContext() throws IOException {

        testLoaderStreamChannelAdapter(null);
    }

    public void testOutputChannelAdapter() throws IOException {

        testOutputChannelAdapter(loaderFrom(getActivity()));
    }

    public void testOutputChannelAdapterFragment() throws IOException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        testOutputChannelAdapter(loaderFrom(fragment));
    }

    public void testOutputChannelAdapterNullContext() throws IOException {

        testOutputChannelAdapter(null);
    }

    public void testStreamChannelAdapter() throws IOException {

        testStreamChannelAdapter(loaderFrom(getActivity()));
    }

    public void testStreamChannelAdapterFragment() throws IOException {

        final TestFragment fragment = (TestFragment) getActivity().getSupportFragmentManager()
                                                                  .findFragmentById(
                                                                          R.id.test_fragment);
        testStreamChannelAdapter(loaderFrom(fragment));
    }

    public void testStreamChannelAdapterNullContext() throws IOException {

        testStreamChannelAdapter(null);
    }
}
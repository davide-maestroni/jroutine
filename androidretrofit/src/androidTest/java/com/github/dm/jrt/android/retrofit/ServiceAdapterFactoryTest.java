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

package com.github.dm.jrt.android.retrofit;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.retrofit.service.RemoteTestService;
import com.github.dm.jrt.android.retrofit.service.TestService;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.operator.Operators;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.github.dm.jrt.android.core.ServiceContext.serviceFrom;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static com.github.dm.jrt.function.Functions.onOutput;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service routine adapter factory unit tests.
 * <p>
 * Created by davide-maestroni on 05/17/2016.
 */
@TargetApi(VERSION_CODES.FROYO)
public class ServiceAdapterFactoryTest extends ActivityInstrumentationTestCase2<TestActivity> {

    private static final String BODY = "[{\"id\":\"1\", \"name\":\"Repo1\"}, {\"id\":\"2\","
            + " \"name\":\"Repo2\"}, {\"id\":\"3\", \"name\":\"Repo3\", \"isPrivate\":true}]";

    public ServiceAdapterFactoryTest() {

        super(TestActivity.class);
    }

    private static void testOutputChannelAdapter(@NotNull final ServiceContext context) throws
            IOException {

        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.start();
        try {
            {
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
                                             .applyInvocationConfiguration()
                                             .withOutputTimeout(seconds(10))
                                             .configured()
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
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
                                             .invocationMode(InvocationMode.PARALLEL)
                                             .applyInvocationConfiguration()
                                             .withOutputTimeout(seconds(10))
                                             .configured()
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
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
                                             .invocationMode(InvocationMode.SYNC)
                                             .applyInvocationConfiguration()
                                             .withOutputTimeout(seconds(10))
                                             .configured()
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
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
                                             .invocationMode(InvocationMode.SEQUENTIAL)
                                             .applyInvocationConfiguration()
                                             .withOutputTimeout(seconds(10))
                                             .configured()
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

    private static void testPostAdapter(@NotNull final ServiceContext context) throws IOException {

        final Repo repo = new Repo();
        repo.setName("Test Repo");
        repo.setPrivate(true);
        final Repo response = new Repo();
        response.setId("7");
        response.setName(repo.getName());
        response.setPrivate(repo.isPrivate());
        final String json = new Gson().toJson(response);
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.enqueue(new MockResponse().setBody(json));
        server.enqueue(new MockResponse().setBody(json));
        server.enqueue(new MockResponse().setBody(json));
        server.start();
        try {
            {
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
                                             .applyInvocationConfiguration()
                                             .withOutputTimeout(seconds(10))
                                             .configured()
                                             .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService2 service = retrofit.create(GitHubService2.class);
                final Repo newRepo = service.createRepo("octocat", repo).next();
                assertThat(newRepo.getId()).isEqualTo(response.getId());
                assertThat(newRepo.getName()).isEqualTo(response.getName());
                assertThat(newRepo.isPrivate()).isEqualTo(response.isPrivate());
            }

            {
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
                                             .invocationMode(InvocationMode.PARALLEL)
                                             .applyInvocationConfiguration()
                                             .withOutputTimeout(seconds(10))
                                             .configured()
                                             .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService2 service = retrofit.create(GitHubService2.class);
                final Repo newRepo = service.createRepo("octocat", repo).next();
                assertThat(newRepo.getId()).isEqualTo(response.getId());
                assertThat(newRepo.getName()).isEqualTo(response.getName());
                assertThat(newRepo.isPrivate()).isEqualTo(response.isPrivate());
            }

            {
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
                                             .invocationMode(InvocationMode.SYNC)
                                             .applyInvocationConfiguration()
                                             .withOutputTimeout(seconds(10))
                                             .configured()
                                             .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService2 service = retrofit.create(GitHubService2.class);
                final Repo newRepo = service.createRepo("octocat", repo).next();
                assertThat(newRepo.getId()).isEqualTo(response.getId());
                assertThat(newRepo.getName()).isEqualTo(response.getName());
                assertThat(newRepo.isPrivate()).isEqualTo(response.isPrivate());
            }

            {
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
                                             .invocationMode(InvocationMode.SEQUENTIAL)
                                             .applyInvocationConfiguration()
                                             .withOutputTimeout(seconds(10))
                                             .configured()
                                             .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService2 service = retrofit.create(GitHubService2.class);
                final Repo newRepo = service.createRepo("octocat", repo).next();
                assertThat(newRepo.getId()).isEqualTo(response.getId());
                assertThat(newRepo.getName()).isEqualTo(response.getName());
                assertThat(newRepo.isPrivate()).isEqualTo(response.isPrivate());
            }

        } finally {
            server.shutdown();
        }
    }

    private static void testStreamBuilderAdapter(@NotNull final ServiceContext context) throws
            IOException {

        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.enqueue(new MockResponse().setBody(BODY));
        server.start();
        try {
            {
                final ServiceAdapterFactory adapterFactory =
                        ServiceAdapterFactory.on(context).buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                assertThat(service.streamRepos("octocat")
                                  .map(Operators.<Repo>unfold())
                                  .bind(onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  }))
                                  .close()
                                  .after(seconds(10))
                                  .getError()).isNull();
            }

            {
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
                                             .invocationMode(InvocationMode.PARALLEL)
                                             .applyServiceConfiguration()
                                             .withLogClass(NullLog.class)
                                             .configured()
                                             .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                assertThat(service.streamRepos("octocat")
                                  .map(Operators.<Repo>unfold())
                                  .bind(onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  }))
                                  .close()
                                  .after(seconds(10))
                                  .getError()).isNull();
            }

            {
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
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
                                  .map(Operators.<Repo>unfold())
                                  .bind(onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  }))
                                  .close()
                                  .after(seconds(10))
                                  .getError()).isNull();
            }

            {
                final ServiceAdapterFactory adapterFactory = //
                        ServiceAdapterFactory.on(context)
                                             .invocationMode(InvocationMode.SEQUENTIAL)
                                             .buildFactory();
                final GsonConverterFactory converterFactory = GsonConverterFactory.create();
                final Retrofit retrofit =
                        new Builder().baseUrl("http://localhost:" + server.getPort())
                                     .addCallAdapterFactory(adapterFactory)
                                     .addConverterFactory(converterFactory)
                                     .build();
                final GitHubService service = retrofit.create(GitHubService.class);
                assertThat(service.streamRepos("octocat")
                                  .map(Operators.<Repo>unfold())
                                  .bind(onOutput(new Consumer<Repo>() {

                                      public void accept(final Repo repo) throws Exception {

                                          final int id = Integer.parseInt(repo.getId());
                                          assertThat(id).isBetween(1, 3);
                                          assertThat(repo.getName()).isEqualTo("Repo" + id);
                                          assertThat(repo.isPrivate()).isEqualTo(id == 3);
                                      }
                                  }))
                                  .close()
                                  .after(seconds(10))
                                  .getError()).isNull();
            }

        } finally {
            server.shutdown();
        }
    }

    public void testOutputChannelAdapter() throws IOException {

        testOutputChannelAdapter(serviceFrom(getActivity(), TestService.class));
    }

    public void testOutputChannelAdapterRemote() throws IOException {

        testOutputChannelAdapter(serviceFrom(getActivity(), RemoteTestService.class));
    }

    public void testPostAdapter() throws IOException {

        testPostAdapter(serviceFrom(getActivity(), TestService.class));
    }

    public void testPostAdapterRemote() throws IOException {

        testPostAdapter(serviceFrom(getActivity(), RemoteTestService.class));
    }

    public void testStreamBuilderAdapter() throws IOException {

        testStreamBuilderAdapter(serviceFrom(getActivity(), TestService.class));
    }

    public void testStreamBuilderAdapterRemote() throws IOException {

        testStreamBuilderAdapter(serviceFrom(getActivity(), RemoteTestService.class));
    }
}

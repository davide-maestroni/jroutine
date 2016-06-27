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

import android.test.AndroidTestCase;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comparable call unit tests.
 * <p>
 * Created by davide-maestroni on 05/19/2016.
 */
public class ComparableCallTest extends AndroidTestCase {

    public void testCall() throws IOException, InterruptedException {
        final Retrofit retrofit = new Builder().baseUrl("https://api.github.com")
                                               .addConverterFactory(GsonConverterFactory.create())
                                               .build();
        final GitHubService service = retrofit.create(GitHubService.class);
        ComparableCall<List<Repo>> call = ComparableCall.of(service.listRepos("octocat"));
        assertThat(call.execute().body()).isNotEmpty();
        call = ComparableCall.of(service.listRepos("octocat"));
        final Semaphore semaphore = new Semaphore(0);
        final AtomicBoolean isSuccess = new AtomicBoolean(false);
        call.enqueue(new Callback<List<Repo>>() {

            @Override
            public void onResponse(final Call<List<Repo>> call,
                    final Response<List<Repo>> response) {
                isSuccess.set(true);
                semaphore.release();
            }

            @Override
            public void onFailure(final Call<List<Repo>> call, final Throwable t) {
                semaphore.release();
            }
        });
        assertThat(semaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
        assertThat(call.isCanceled()).isFalse();
        assertThat(call.isExecuted()).isTrue();
        call.cancel();
        assertThat(call.isCanceled()).isTrue();
        assertThat(call.isExecuted()).isTrue();
    }

    public void testEquals() {
        final Retrofit retrofit = new Builder().baseUrl("https://api.github.com")
                                               .addConverterFactory(GsonConverterFactory.create())
                                               .build();
        final GitHubService service = retrofit.create(GitHubService.class);
        final ComparableCall<List<Repo>> call =
                ComparableCall.of(service.listRepos("octocat", "test"));
        assertThat(call).isEqualTo(call);
        assertThat(call).isEqualTo(call.clone());
        assertThat(call).isNotEqualTo(null);
        assertThat(call).isNotEqualTo("test");
        assertThat(call).isNotEqualTo(ComparableCall.of(service.listRepos("octocat")));
        assertThat(call).isNotEqualTo(ComparableCall.of(service.listRepos("octocat", "")));
        assertThat(call).isEqualTo(ComparableCall.of(service.listRepos("octocat", "test")));
        assertThat(call.hashCode()).isEqualTo(
                ComparableCall.of(service.listRepos("octocat", "test")).hashCode());
    }

    public interface GitHubService {

        @POST("users/{user}/repos")
        @Headers({"Cache-Control: max-age=640000", "Content-type: application/json"})
        Call<List<Repo>> listRepos(@Path("user") String user, @Body String body);

        @GET("users/{user}/repos")
        @Headers("Cache-Control: max-age=640000")
        Call<List<Repo>> listRepos(@Path("user") String user);
    }
}

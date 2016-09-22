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

package com.github.dm.jrt.android.sample;

import com.google.gson.annotations.SerializedName;

/**
 * GitHub repository resource model.
 * <p>
 * Created by davide-maestroni on 03/25/2016.
 */
@SuppressWarnings("unused")
public class Repo {

  @SerializedName("id")
  private String mId;

  @SerializedName("isPrivate")
  private boolean mIsPrivate;

  @SerializedName("name")
  private String mName;

  public String getId() {
    return mId;
  }

  public void setId(final String id) {
    mId = id;
  }

  public String getName() {
    return mName;
  }

  public void setName(final String name) {
    mName = name;
  }

  public boolean isPrivate() {
    return mIsPrivate;
  }

  public void setPrivate(final boolean isPrivate) {
    mIsPrivate = isPrivate;
  }

  @Override
  public String toString() {
    return mName;
  }
}

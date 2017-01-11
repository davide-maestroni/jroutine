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

package com.github.dm.jrt.swagger.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by davide-maestroni on 12/24/2016.
 */
class ProjectProperties {

  private static final ObjectMapper sMapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private String mProjectVersion;

  private String mSwaggerVersion;

  @NotNull
  public static ProjectProperties readProjectProperties(@NotNull final String path) throws
      IOException {
    final Properties properties = new Properties();
    InputStream inputStream = null;
    try {
      inputStream = ProjectProperties.class.getResourceAsStream(path);
      properties.load(inputStream);
      inputStream.close();

    } catch (final IOException e) {
      if (inputStream != null) {
        inputStream.close();
      }

      throw e;
    }

    return sMapper.convertValue(properties, ProjectProperties.class);
  }

  @NotNull
  public static ProjectProperties readProjectProperties() throws IOException {
    return readProjectProperties("/project.properties");
  }

  public String getProjectVersion() {
    return mProjectVersion;
  }

  public void setProjectVersion(final String projectVersion) {
    this.mProjectVersion = projectVersion;
  }

  public String getSwaggerVersion() {
    return mSwaggerVersion;
  }

  public void setSwaggerVersion(final String swaggerVersion) {
    this.mSwaggerVersion = swaggerVersion;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(mProjectVersion).append(mSwaggerVersion).toHashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ProjectProperties that = (ProjectProperties) o;
    return new EqualsBuilder().append(mProjectVersion, that.mProjectVersion)
                              .append(mSwaggerVersion, that.mSwaggerVersion)
                              .isEquals();
  }

  @Override
  public String toString() {
    return "ProjectProperties{" + "mProjectVersion='" + mProjectVersion + '\''
        + ", mSwaggerVersion='" + mSwaggerVersion + '\'' + '}';
  }
}

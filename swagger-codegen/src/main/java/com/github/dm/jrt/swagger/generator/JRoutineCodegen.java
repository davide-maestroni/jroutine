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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.languages.JavaClientCodegen;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;

/**
 * Swagger generator based on the {@code jroutine-retrofit} library.
 * <p>
 * Created by davide-maestroni on 12/07/2016.
 */
public class JRoutineCodegen extends JavaClientCodegen {

  private static final String BASE_URL = "baseUrl";

  private static final String PROJECT_PREFIX = "projectPrefix";

  /**
   * Constructor.
   */
  public JRoutineCodegen() {
    setLibrary(RETROFIT_2);
  }

  @Override
  public void addOperationToGroup(final String tag, final String resourcePath,
      final Operation operation, final CodegenOperation co,
      final Map<String, List<CodegenOperation>> operations) {
    String basePath = co.path;
    if (basePath.startsWith("/")) {
      basePath = basePath.substring(1);
    }

    int pos = basePath.indexOf("/");
    if (pos > 0) {
      basePath = basePath.substring(0, pos);
    }

    if (basePath.length() == 0) {
      basePath = "default";

    } else {
      final String prefix = "/" + basePath;
      if (co.path.startsWith(prefix)) {
        co.subresourceOperation = co.path.length() > prefix.length();

      } else {
        co.subresourceOperation = co.path.length() != 0;
      }
    }

    List<CodegenOperation> opList = operations.get(basePath);
    if (opList == null) {
      opList = new ArrayList<CodegenOperation>();
      operations.put(basePath, opList);
    }

    opList.add(co);
    co.baseName = basePath;
  }

  @Override
  public String getName() {
    return "jroutine";
  }

  @Override
  public void processOpts() {
    super.processOpts();
    final Map<String, String> apiTemplateFiles = this.apiTemplateFiles;
    apiTemplateFiles.remove("api.mustache");
    apiTemplateFiles.put("jroutine_api.mustache", ".java");
    final Map<String, String> modelTemplateFiles = this.modelTemplateFiles;
    modelTemplateFiles.remove("model.mustache");
    modelTemplateFiles.put("jroutine_model.mustache", ".java");
  }

  @Override
  public void preprocessSwagger(final Swagger swagger) {
    super.preprocessSwagger(swagger);
    final String projectPrefix = buildProjectPrefix(swagger);
    final Map<String, Object> additionalProperties = this.additionalProperties;
    additionalProperties.put(PROJECT_PREFIX, projectPrefix);
    additionalProperties.put(BASE_URL, buildBaseUrl(swagger));
    for (final SupportingFile supportingFile : supportingFiles) {
      if ("ApiClient.mustache".equals(supportingFile.templateFile)) {
        supportingFile.templateFile = "jroutine_ApiClient.mustache";
        supportingFile.destinationFilename = projectPrefix + "ApiClient.java";
      }

      if ("auth/OAuthOkHttpClient.mustache".equals(supportingFile.templateFile)) {
        supportingFile.templateFile = "auth/jroutine_OAuthOkHttpClient.mustache";
      }
    }
  }

  @Override
  public CodegenOperation fromOperation(final String path, final String httpMethod,
      final Operation operation, final Map<String, Model> definitions, final Swagger swagger) {
    final CodegenOperation codegenOperation =
        super.fromOperation(path, httpMethod, operation, definitions, swagger);
    codegenOperation.path = swagger.getBasePath() + codegenOperation.path;
    return codegenOperation;
  }

  @NotNull
  private String buildBaseUrl(final Swagger swagger) {
    final String scheme;
    if (swagger.getSchemes() != null && swagger.getSchemes().size() > 0) {
      scheme = escapeText(swagger.getSchemes().get(0).toValue());

    } else {
      scheme = "https";
    }

    final String host;
    if (swagger.getHost() != null) {
      host = swagger.getHost();

    } else {
      host = "localhost";
    }

    return new URIBuilder().setScheme(escapeText(scheme)).setHost(host).toString();
  }

  private String buildProjectPrefix(final Swagger swagger) {
    final String basePath = swagger.getBasePath();
    final StringBuilder builder = new StringBuilder();
    for (final String path : basePath.split("/")) {
      builder.append(StringUtils.capitalize(escapeUnsafeCharacters(path)));
    }

    return builder.toString();
  }
}

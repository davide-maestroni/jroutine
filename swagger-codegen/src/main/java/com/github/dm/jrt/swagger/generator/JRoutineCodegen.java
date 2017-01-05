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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.swagger.codegen.CliOption;
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

  private static final String ENABLE_LOADERS = "enableLoaders";

  private static final String JROUTINE_CODEGEN_VERSION = "jroutineCodegenVersion";

  private static final String PROJECT_NAME = "projectName";

  private static final String PROJECT_PREFIX = "projectPrefix";

  private static final String SWAGGER_CODEGEN_VERSION = "swaggerCodegenVersion";

  private static final String USE_SUPPORT_LIBRARY = "useSupportLibrary";

  /**
   * Constructor.
   */
  public JRoutineCodegen() {
    ProjectProperties projectProperties = new ProjectProperties();
    try {
      projectProperties = ProjectProperties.readProjectProperties();

    } catch (final IOException e) {
      e.printStackTrace();
    }

    final List<CliOption> cliOptions = this.cliOptions;
    cliOptions.add(CliOption.newString(PROJECT_NAME,
        "The name of the project to prepend to the API classes."));
    cliOptions.add(CliOption.newBoolean(ENABLE_LOADERS,
        "Whether to enable requests made through Android Loaders."));
    cliOptions.add(CliOption.newBoolean(USE_SUPPORT_LIBRARY,
        "Whether to use the Android Support Library to generate the source code."));
    super.setLibrary(RETROFIT_2);
    final Map<String, Object> additionalProperties = this.additionalProperties;
    additionalProperties.put(JROUTINE_CODEGEN_VERSION, projectProperties.getProjectVersion());
    additionalProperties.put(SWAGGER_CODEGEN_VERSION, projectProperties.getSwaggerVersion());
  }

  @Override
  public String getName() {
    return "jroutine";
  }

  @Override
  public void processOpts() {
    final Map<String, Object> additionalProperties = this.additionalProperties;
    additionalProperties.remove(USE_RX_JAVA);
    additionalProperties.remove("usePlay24WS");
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
    final Map<String, Object> additionalProperties = this.additionalProperties;
    final Object projectName = additionalProperties.get(PROJECT_NAME);
    final String projectPrefix;
    if (projectName != null) {
      projectPrefix = camelize(projectName.toString());

    } else {
      projectPrefix = buildProjectPrefix(swagger);
    }

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

  @Override
  public String toApiName(final String name) {
    final Map<String, Object> additionalProperties = this.additionalProperties;
    if (additionalProperties.containsKey(ENABLE_LOADERS)) {
      if (additionalProperties.containsKey(USE_SUPPORT_LIBRARY)) {
        return ((name.length() == 0) ? "Default" : this.initialCaps(name)) + "LoaderApiCompat";

      } else {
        return ((name.length() == 0) ? "Default" : this.initialCaps(name)) + "LoaderApi";
      }
    }

    return super.toApiName(name);
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
  public void setLibrary(final String library) {
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
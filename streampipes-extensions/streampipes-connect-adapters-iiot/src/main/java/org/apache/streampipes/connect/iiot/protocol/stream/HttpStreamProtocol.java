/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.streampipes.connect.iiot.protocol.stream;

import org.apache.streampipes.commons.exceptions.connect.AdapterException;
import org.apache.streampipes.extensions.api.connect.IAdapterConfiguration;
import org.apache.streampipes.extensions.api.connect.IEventCollector;
import org.apache.streampipes.extensions.api.connect.IParser;
import org.apache.streampipes.extensions.api.connect.IPullAdapter;
import org.apache.streampipes.extensions.api.connect.StreamPipesAdapter;
import org.apache.streampipes.extensions.api.connect.context.IAdapterGuessSchemaContext;
import org.apache.streampipes.extensions.api.connect.context.IAdapterRuntimeContext;
import org.apache.streampipes.extensions.api.extractor.IAdapterParameterExtractor;
import org.apache.streampipes.extensions.api.extractor.IStaticPropertyExtractor;
import org.apache.streampipes.extensions.management.connect.PullAdapterScheduler;
import org.apache.streampipes.extensions.management.connect.adapter.parser.Parsers;
import org.apache.streampipes.extensions.management.connect.adapter.util.PollingSettings;
import org.apache.streampipes.model.connect.guess.SampleData;
import org.apache.streampipes.model.extensions.ExtensionAssetType;
import org.apache.streampipes.model.staticproperty.CollectionStaticProperty;
import org.apache.streampipes.model.staticproperty.FreeTextStaticProperty;
import org.apache.streampipes.model.staticproperty.StaticPropertyAlternative;
import org.apache.streampipes.model.staticproperty.StaticPropertyAlternatives;
import org.apache.streampipes.model.staticproperty.StaticPropertyGroup;
import org.apache.streampipes.sdk.StaticProperties;
import org.apache.streampipes.sdk.builder.adapter.AdapterConfigurationBuilder;
import org.apache.streampipes.sdk.extractor.StaticPropertyExtractor;
import org.apache.streampipes.sdk.helpers.Alternatives;
import org.apache.streampipes.sdk.helpers.Labels;
import org.apache.streampipes.sdk.helpers.Locales;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HttpStreamProtocol implements StreamPipesAdapter, IPullAdapter {

  public static final String ID = "org.apache.streampipes.connect.iiot.protocol.stream.http";

  public static final String HTTP_METHOD = "http-method";
  public static final String HEADER_COLLECTION = "header-collection";
  public static final String HEADER_KEY = "header-key";
  public static final String HEADER_VALUE = "header-value";

  static final String URL_PROPERTY = "url";
  static final String INTERVAL_PROPERTY = "interval";

  private static final HttpMethod DEFAULT_METHOD = HttpMethod.GET;

  private final HttpStreamClient client;

  private HttpRequestConfig requestConfig;
  private PollingSettings pollingSettings;
  private PullAdapterScheduler pullAdapterScheduler;
  private IEventCollector collector;
  private IParser parser;

  public HttpStreamProtocol() {
    this(new HttpStreamClient());
  }

  HttpStreamProtocol(HttpStreamClient client) {
    this.client = client;
  }

  @Override
  public IAdapterConfiguration declareConfig() {
    return AdapterConfigurationBuilder
        .create(ID, 2, HttpStreamProtocol::new)
        .withSupportedParsers(Parsers.defaultParsers())
        .withAssets(ExtensionAssetType.DOCUMENTATION, ExtensionAssetType.ICON)
        .withLocales(Locales.EN)
        .requiredTextParameter(Labels.withId(URL_PROPERTY))
        .requiredIntegerParameter(Labels.withId(INTERVAL_PROPERTY))
        .requiredStaticProperty(httpMethodSelection())
        .requiredStaticProperty(headerCollection())
        .buildConfiguration();
  }

  @Override
  public void onAdapterStarted(IAdapterParameterExtractor extractor,
                               IEventCollector collector,
                               IAdapterRuntimeContext adapterRuntimeContext) throws AdapterException {
    var staticProperties = extractor.getStaticPropertyExtractor();
    this.requestConfig = toRequestConfig(staticProperties);
    this.pollingSettings = PollingSettings.from(
        TimeUnit.SECONDS,
        staticProperties.singleValueParameter(INTERVAL_PROPERTY, Integer.class)
    );
    this.parser = extractor.selectedParser();
    this.collector = collector;
    this.pullAdapterScheduler = new PullAdapterScheduler();
    this.pullAdapterScheduler.schedule(this, extractor.getAdapterDescription().getElementId());
  }

  @Override
  public void onAdapterStopped(IAdapterParameterExtractor extractor,
                               IAdapterRuntimeContext adapterRuntimeContext) {
    if (pullAdapterScheduler != null) {
      pullAdapterScheduler.shutdown();
    }
  }

  @Override
  public SampleData onSampleDataRequested(IAdapterParameterExtractor extractor,
                                          IAdapterGuessSchemaContext adapterGuessSchemaContext)
      throws AdapterException {
    var config = toRequestConfig(extractor.getStaticPropertyExtractor());
    return extractor.selectedParser().getSampleData(client.fetch(config));
  }

  @Override
  public void pullData() {
    parser.parse(client.fetch(requestConfig), collector::collect);
  }

  @Override
  public PollingSettings getPollingInterval() {
    return pollingSettings;
  }

  /**
   * UI selection of the HTTP method. Methods with a body show content type and body fields.
   */
  public static StaticPropertyAlternatives httpMethodSelection() {
    return StaticProperties.alternatives(
        Labels.withId(HTTP_METHOD),
        Arrays.stream(HttpMethod.values())
            .map(HttpStreamProtocol::toAlternative)
            .collect(Collectors.toList())
    );
  }

  static HttpRequestConfig toRequestConfig(IStaticPropertyExtractor extractor) throws AdapterException {
    var url = extractor.singleValueParameter(URL_PROPERTY, String.class);
    var method = HttpMethod.fromAlternativeId(extractor.selectedAlternativeInternalId(HTTP_METHOD));
    var headers = extractHeaders(extractor);

    if (!method.hasBody()) {
      return HttpRequestConfig.withoutBody(method, url, headers);
    }
    try {
      return HttpRequestConfig.withBody(
          method,
          url,
          headers,
          textOrEmpty(extractor, method.contentTypeId()),
          textOrEmpty(extractor, method.requestBodyId())
      );
    } catch (IllegalArgumentException e) {
      throw new AdapterException(e.getMessage(), e);
    }
  }

  private static StaticPropertyAlternative toAlternative(HttpMethod method) {
    var label = Labels.withId(method.alternativeId());
    var selected = method == DEFAULT_METHOD;
    return method.hasBody()
        ? Alternatives.from(label, bodyOptions(method), selected)
        : Alternatives.from(label, selected);
  }

  private static StaticPropertyGroup bodyOptions(HttpMethod method) {
    var contentType = StaticProperties.stringFreeTextProperty(
        Labels.withId(method.contentTypeId()),
        HttpRequestConfig.DEFAULT_CONTENT_TYPE
    );
    var body = optionalText(method.requestBodyId());
    body.setMultiLine(true);

    return StaticProperties.group(Labels.withId(method.bodyOptionsId()), contentType, body);
  }

  private static CollectionStaticProperty headerCollection() {
    return StaticProperties.collection(
        Labels.withId(HEADER_COLLECTION),
        false,
        optionalText(HEADER_KEY),
        optionalText(HEADER_VALUE)
    );
  }

  private static FreeTextStaticProperty optionalText(String id) {
    var property = StaticProperties.stringFreeTextProperty(Labels.withId(id), "");
    property.setOptional(true);
    return property;
  }

  private static List<HttpRequestConfig.Header> extractHeaders(IStaticPropertyExtractor extractor) {
    var collection = extractor.getStaticPropertyByName(HEADER_COLLECTION, CollectionStaticProperty.class);
    // members stay null until the user adds the first header
    if (collection == null || collection.getMembers() == null) {
      return List.of();
    }
    return collection.getMembers().stream()
        .filter(StaticPropertyGroup.class::isInstance)
        .map(member -> StaticPropertyExtractor.from(((StaticPropertyGroup) member).getStaticProperties()))
        .filter(member -> !textOrEmpty(member, HEADER_KEY).isBlank())
        .map(member -> new HttpRequestConfig.Header(
            textOrEmpty(member, HEADER_KEY).trim(),
            textOrEmpty(member, HEADER_VALUE)))
        .toList();
  }

  private static String textOrEmpty(IStaticPropertyExtractor extractor, String id) {
    var property = extractor.getStaticPropertyByName(id, FreeTextStaticProperty.class);
    return property == null ? "" : Objects.requireNonNullElse(property.getValue(), "");
  }
}

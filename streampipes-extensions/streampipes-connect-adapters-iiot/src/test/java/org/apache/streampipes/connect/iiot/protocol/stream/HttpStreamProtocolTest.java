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
import org.apache.streampipes.model.staticproperty.CollectionStaticProperty;
import org.apache.streampipes.model.staticproperty.FreeTextStaticProperty;
import org.apache.streampipes.model.staticproperty.StaticProperty;
import org.apache.streampipes.model.staticproperty.StaticPropertyAlternative;
import org.apache.streampipes.model.staticproperty.StaticPropertyAlternatives;
import org.apache.streampipes.model.staticproperty.StaticPropertyGroup;
import org.apache.streampipes.sdk.extractor.StaticPropertyExtractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Works on the adapter's real UI definition, filled in the way the UI would do it.
 */
class HttpStreamProtocolTest {

  private static final String URL = "http://localhost:8080/data";
  private static final String STRINGS = "org.apache.streampipes.connect.iiot.protocol.stream.http/strings.en";
  private static final String PARSER_FORMAT = "format";

  private List<StaticProperty> config;

  @BeforeEach
  void setUp() {
    config = new HttpStreamProtocol().declareConfig().getAdapterDescription().getConfig();
    // StreamPipes appends the parser selection ("format"); its labels belong to the parsers, not to this adapter
    config.removeIf(property -> PARSER_FORMAT.equals(property.getInternalName()));
    setText(HttpStreamProtocol.URL_PROPERTY, URL);
  }

  @Test
  void toRequestConfig_shouldDefaultToGetWithoutBody() throws AdapterException {
    var requestConfig = toRequestConfig();

    assertEquals(HttpMethod.GET, requestConfig.method());
    assertEquals(URL, requestConfig.url());
    assertNull(requestConfig.body());
    assertTrue(requestConfig.headers().isEmpty());
  }

  @Test
  void toRequestConfig_shouldReadBodyOfSelectedMethod() throws AdapterException {
    selectMethod(HttpMethod.POST);
    setText(HttpMethod.POST.contentTypeId(), "application/xml");
    setText(HttpMethod.POST.requestBodyId(), "<query/>");

    var requestConfig = toRequestConfig();

    assertEquals(HttpMethod.POST, requestConfig.method());
    assertEquals("application/xml", requestConfig.contentType().getMimeType());
    assertEquals("<query/>", requestConfig.body());
  }

  @Test
  void declareConfig_shouldHaveLabelsForAllProperties() throws IOException {
    var strings = new Properties();
    try (var in = getClass().getClassLoader().getResourceAsStream(STRINGS)) {
      strings.load(in);
    }

    var missing = config.stream()
        .flatMap(HttpStreamProtocolTest::flatten)
        .map(StaticProperty::getInternalName)
        .distinct()
        .filter(id -> !strings.containsKey(id + ".title") || !strings.containsKey(id + ".description"))
        .toList();

    assertEquals(List.of(), missing, "Missing entries in strings.en");
  }

  private HttpRequestConfig toRequestConfig() throws AdapterException {
    return HttpStreamProtocol.toRequestConfig(StaticPropertyExtractor.from(config));
  }

  private void selectMethod(HttpMethod method) {
    find(HttpStreamProtocol.HTTP_METHOD, StaticPropertyAlternatives.class)
        .getAlternatives()
        .forEach(alternative -> alternative.setSelected(
            alternative.getInternalName().equals(method.alternativeId())));
  }

  private void setText(String id, String value) {
    find(id, FreeTextStaticProperty.class).setValue(value);
  }

  private <T extends StaticProperty> T find(String id, Class<T> type) {
    return config.stream()
        .flatMap(HttpStreamProtocolTest::flatten)
        .filter(property -> property.getInternalName().equals(id))
        .map(type::cast)
        .findFirst()
        .orElseThrow(() -> new AssertionError("No property " + id));
  }

  private static Stream<StaticProperty> flatten(StaticProperty property) {
    Stream<StaticProperty> children = Stream.empty();
    if (property instanceof StaticPropertyAlternatives alternatives) {
      children = alternatives.getAlternatives().stream().map(StaticProperty.class::cast);
    } else if (property instanceof StaticPropertyAlternative alternative) {
      children = Stream.ofNullable(alternative.getStaticProperty());
    } else if (property instanceof StaticPropertyGroup group) {
      children = group.getStaticProperties().stream();
    } else if (property instanceof CollectionStaticProperty collection) {
      children = Stream.of(collection.getStaticPropertyTemplate());
    }
    return Stream.concat(Stream.of(property), children.flatMap(HttpStreamProtocolTest::flatten));
  }
}
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

package org.apache.streampipes.connect.iiot.migration;

import org.apache.streampipes.connect.iiot.protocol.stream.HttpMethod;
import org.apache.streampipes.connect.iiot.protocol.stream.HttpStreamProtocol;
import org.apache.streampipes.model.connect.adapter.AdapterDescription;
import org.apache.streampipes.model.staticproperty.StaticProperty;
import org.apache.streampipes.sdk.StaticProperties;
import org.apache.streampipes.sdk.extractor.StaticPropertyExtractor;
import org.apache.streampipes.sdk.helpers.Labels;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpStreamProtocolMigrationV2Test {

  @Test
  void migrate_shouldAddHttpMethodSelectionWithGetSelected() {
    var adapter = new AdapterDescription();
    adapter.setConfig(new ArrayList<>(List.of(
        StaticProperties.stringFreeTextProperty(Labels.withId("url"), "http://localhost:8080/data"),
        StaticProperties.integerFreeTextProperty(Labels.withId("interval"), 5),
        StaticProperties.collection(
            Labels.withId(HttpStreamProtocol.HEADER_COLLECTION),
            false,
            StaticProperties.stringFreeTextProperty(Labels.withId(HttpStreamProtocol.HEADER_KEY)),
            StaticProperties.stringFreeTextProperty(Labels.withId(HttpStreamProtocol.HEADER_VALUE)))
    )));

    var result = new HttpStreamProtocolMigrationV2().migrate(adapter, null);

    assertTrue(result.success());
    var config = result.element().getConfig();
    assertEquals(
        List.of("url", "interval", HttpStreamProtocol.HTTP_METHOD, HttpStreamProtocol.HEADER_COLLECTION),
        config.stream().map(StaticProperty::getInternalName).toList()
    );
    assertEquals(
        HttpMethod.GET.alternativeId(),
        StaticPropertyExtractor.from(config).selectedAlternativeInternalId(HttpStreamProtocol.HTTP_METHOD)
    );
  }
}

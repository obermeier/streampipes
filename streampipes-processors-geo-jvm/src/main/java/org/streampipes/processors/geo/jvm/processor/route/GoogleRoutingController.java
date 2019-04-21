/*
 * Copyright 2018 FZI Forschungszentrum Informatik
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
 *
 */

package org.streampipes.processors.geo.jvm.processor.route;

import org.streampipes.model.graph.DataProcessorDescription;
import org.streampipes.model.graph.DataProcessorInvocation;
import org.streampipes.model.schema.PropertyScope;
import org.streampipes.processors.geo.jvm.config.GeoJvmConfig;
import org.streampipes.sdk.builder.ProcessingElementBuilder;
import org.streampipes.sdk.builder.StreamRequirementsBuilder;
import org.streampipes.sdk.extractor.ProcessingElementParameterExtractor;
import org.streampipes.sdk.helpers.EpProperties;
import org.streampipes.sdk.helpers.EpRequirements;
import org.streampipes.sdk.helpers.Labels;
import org.streampipes.sdk.helpers.Locales;
import org.streampipes.sdk.helpers.OutputStrategies;
import org.streampipes.sdk.helpers.SupportedFormats;
import org.streampipes.sdk.helpers.SupportedProtocols;
import org.streampipes.vocabulary.SO;
import org.streampipes.wrapper.standalone.ConfiguredEventProcessor;
import org.streampipes.wrapper.standalone.declarer.StandaloneEventProcessingDeclarer;

public class GoogleRoutingController extends StandaloneEventProcessingDeclarer<GoogleRoutingParameters> {

  private static final String CITY_MAPPING = "city";
  private static final String STREET_MAPPING = "street";
  private static final String STREET_NUMBER_MAPPING = "street-number";
  private static final String START_ADDRESS = "start-address";

  @Override
  public DataProcessorDescription declareModel() {
    return ProcessingElementBuilder.create("org.streampipes.processors.geo.jvm.google-routing")
            .iconUrl(GeoJvmConfig.iconBaseUrl + "Map_Icon_HQ.png")
            .withLocales(Locales.EN)
            .requiredStream(
                    StreamRequirementsBuilder.create()
                            .requiredPropertyWithUnaryMapping(
                                    EpRequirements
                                            .domainPropertyReq("http://schema.org/city"),
                                    Labels.withId(CITY_MAPPING),
                                    PropertyScope.NONE)
                            .requiredPropertyWithUnaryMapping(
                                    EpRequirements
                                            .domainPropertyReq(SO.StreetAddress),
                                    Labels.withId(STREET_MAPPING),
                                    PropertyScope.NONE)
                            .requiredPropertyWithUnaryMapping(
                                    EpRequirements
                                            .domainPropertyReq("http://schema"
                                                    + ".org/streetNumber"),
                                    Labels.withId(STREET_NUMBER_MAPPING),
                                    PropertyScope.NONE)
                            .build())

            .requiredTextParameter(Labels.withId(START_ADDRESS))
            .outputStrategy(OutputStrategies.append(EpProperties.stringEp(Labels.withId("kvi"),
                    "kvi", "http://kvi.de")))
            .supportedFormats(SupportedFormats.jsonFormat())
            .supportedProtocols(SupportedProtocols.kafka(), SupportedProtocols.jms())
            .build();
  }

  /*
      TUTORIAL:
      Here you get the Configuration Parameters which the User has entered
  */
  @Override
  public ConfiguredEventProcessor<GoogleRoutingParameters> onInvocation(DataProcessorInvocation graph, ProcessingElementParameterExtractor extractor) {
    String city = extractor.mappingPropertyValue(CITY_MAPPING);
    String street = extractor.mappingPropertyValue(STREET_MAPPING);
    String number = extractor.mappingPropertyValue(STREET_NUMBER_MAPPING);

    String home = extractor.singleValueParameter(START_ADDRESS, String.class);

    GoogleRoutingParameters params = new GoogleRoutingParameters(graph, city, street, number, home);
    return new ConfiguredEventProcessor<>(params, GoogleRouting::new);
  }
}

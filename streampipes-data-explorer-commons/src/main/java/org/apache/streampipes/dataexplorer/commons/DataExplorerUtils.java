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
package org.apache.streampipes.dataexplorer.commons;

import org.apache.streampipes.client.StreamPipesClient;
import org.apache.streampipes.commons.exceptions.SpRuntimeException;
import org.apache.streampipes.dataexplorer.commons.influx.InfluxNameSanitizer;
import org.apache.streampipes.model.datalake.DataLakeMeasure;
import org.apache.streampipes.model.schema.EventProperty;

import java.util.List;
import java.util.stream.Collectors;

public class DataExplorerUtils {
    /**
     * Sanitizes the event schema and stores the DataLakeMeasurement to the couchDB
     *
     * @param client
     * @param measure
     * @throws SpRuntimeException
     */
    public static DataLakeMeasure sanitizeAndRegisterAtDataLake(StreamPipesClient client,
                                                     DataLakeMeasure measure) throws SpRuntimeException {
        measure = sanitizeDataLakeMeasure(measure);
        registerAtDataLake(client, measure);

        return measure;
    }

    private static void registerAtDataLake(StreamPipesClient client,
                                                     DataLakeMeasure measure) throws SpRuntimeException {
        client
          .customRequest()
          .sendPost("api/v4/datalake/measure/", measure);
    }


    private static DataLakeMeasure sanitizeDataLakeMeasure(DataLakeMeasure measure) throws SpRuntimeException {

        // Removes selected timestamp from event schema
        measure = removeTimestampsFromEventSchema(measure);

        // Escapes all spaces with _
        measure.setMeasureName(InfluxNameSanitizer.prepareString(measure.getMeasureName()));

        // Removes all spaces with _ and validates that no special terms are used as runtime names
        measure.getEventSchema()
                .getEventProperties()
                .forEach(ep -> ep.setRuntimeName(InfluxNameSanitizer.sanitizePropertyRuntimeName(ep.getRuntimeName())));

        return measure;
    }

    private static DataLakeMeasure removeTimestampsFromEventSchema(DataLakeMeasure measure) {
        List<EventProperty> eventPropertiesWithoutTimestamp = measure.getEventSchema().getEventProperties()
          .stream()
          .filter(eventProperty -> !measure.getTimestampField().endsWith(eventProperty.getRuntimeName()))
          .collect(Collectors.toList());
        measure.getEventSchema().setEventProperties(eventPropertiesWithoutTimestamp);

        return measure;
    }

}

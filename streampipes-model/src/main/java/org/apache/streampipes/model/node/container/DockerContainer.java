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
package org.apache.streampipes.model.node.container;

import io.fogsy.empire.annotations.RdfsClass;
import org.apache.streampipes.model.shared.annotation.TsModel;
import org.apache.streampipes.vocabulary.StreamPipes;

import javax.persistence.Entity;
import java.util.List;
import java.util.Map;

@RdfsClass(StreamPipes.DEPLOYMENT_DOCKER_CONTAINER)
@Entity
@TsModel
public class DockerContainer extends DeploymentContainer {

    public DockerContainer(String elementId) {
        super(elementId);
    }

    public DockerContainer() {
        super();
    }

    public DockerContainer(String imageTag, String containerName, String serviceId, String[] containerPorts,
                           List<String> envVars, Map<String, String> labels, List<String> volumes,
                           List<String> supportedArchitectures, List<String> supportedOperatingSystemTypes,
                           List<String> dependsOnContainers) {
        super(imageTag, containerName, serviceId, containerPorts, envVars, labels, volumes, supportedArchitectures,
                supportedOperatingSystemTypes, dependsOnContainers);
    }
}

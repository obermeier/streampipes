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

import { Directive } from '@angular/core';
import {
    AdapterDescription,
    AdapterService,
    ChartService,
    Dashboard,
    DashboardService,
    DataExplorerWidgetModel,
    DataLakeMeasure,
    DatalakeRestService,
    FileMetadata,
    FilesService,
    GenericStorageService,
    Pipeline,
    PipelineElementService,
    PipelineService,
    SpDataStream,
} from '@streampipes/platform-services';
import { zip } from 'rxjs';

@Directive()
export abstract class BaseAssetLinksDirective {
    // Resources
    pipelines: Pipeline[];
    charts: DataExplorerWidgetModel[];
    dashboards: Dashboard[];
    dataLakeMeasures: DataLakeMeasure[];
    dataSources: SpDataStream[];
    adapters: AdapterDescription[];
    files: FileMetadata[];

    allResources: any[] = [];

    constructor(
        protected genericStorageService: GenericStorageService,
        protected pipelineService: PipelineService,
        protected chartService: ChartService,
        protected dashboardService: DashboardService,
        protected dataLakeService: DatalakeRestService,
        protected pipelineElementService: PipelineElementService,
        protected adapterService: AdapterService,
        protected filesService: FilesService,
    ) {}

    onInit() {
        this.getAllResources();
    }

    getAllResources() {
        zip(
            this.pipelineService.getPipelines(),
            this.chartService.getAllCharts(),
            this.dashboardService.getDashboards(),
            this.pipelineElementService.getDataStreams(),
            this.dataLakeService.getAllMeasurementSeries(),
            this.filesService.getFileMetadata(),
            this.adapterService.getAdapters(),
        ).subscribe(
            ([
                pipelines,
                charts,
                dashboards,
                streams,
                measurements,
                files,
                adapters,
            ]) => {
                this.pipelines = pipelines.sort((a, b) =>
                    a.name.localeCompare(b.name),
                );
                this.charts = charts.sort((a, b) =>
                    a.baseAppearanceConfig.widgetTitle.localeCompare(
                        b.baseAppearanceConfig.widgetTitle,
                    ),
                );
                this.dashboards = dashboards.sort((a, b) =>
                    a.name.localeCompare(b.name),
                );
                this.dataSources = streams.sort((a, b) =>
                    a.name.localeCompare(b.name),
                );
                this.dataLakeMeasures = measurements.sort((a, b) =>
                    a.measureName.localeCompare(b.measureName),
                );
                this.files = files.sort((a, b) =>
                    a.filename.localeCompare(b.filename),
                );
                this.adapters = adapters.sort((a, b) =>
                    a.name.localeCompare(b.name),
                );

                this.allResources = [
                    ...this.pipelines,
                    ...this.charts,
                    ...this.dashboards,
                    ...this.dataSources,
                    ...this.dataLakeMeasures,
                    ...this.files,
                    ...this.adapters,
                ];
                this.afterResourcesLoaded();
            },
        );
    }

    abstract afterResourcesLoaded(): void;
}

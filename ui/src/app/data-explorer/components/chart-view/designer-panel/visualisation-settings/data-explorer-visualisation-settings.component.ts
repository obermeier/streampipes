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

import {
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
} from '@angular/core';
import { DataExplorerWidgetModel } from '@streampipes/platform-services';
import { ChartTypeService } from '../../../../../data-explorer-shared/services/chart-type.service';
import { MatSelectChange } from '@angular/material/select';
import { IWidget } from '../../../../../data-explorer-shared/models/dataview-dashboard.model';
import { DataExplorerChartRegistry } from '../../../../../data-explorer-shared/registry/data-explorer-chart-registry';

@Component({
    selector: 'sp-explorer-visualisation-settings',
    templateUrl: './data-explorer-visualisation-settings.component.html',
    styleUrls: ['./data-explorer-visualisation-settings.component.scss'],
})
export class DataExplorerVisualisationSettingsComponent
    implements OnInit, OnChanges
{
    @Input() currentlyConfiguredWidget: DataExplorerWidgetModel;

    constructor(
        private widgetTypeService: ChartTypeService,
        private widgetRegistryService: DataExplorerChartRegistry,
    ) {}

    availableWidgets: IWidget<any>[];
    activeWidgetType: IWidget<any>;

    ngOnInit(): void {
        this.availableWidgets =
            this.widgetRegistryService.getAvailableChartTemplates();
        this.selectWidget();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.currentlyConfiguredWidget) {
            this.selectWidget();
        }
    }

    selectWidget(): void {
        this.activeWidgetType = this.widgetRegistryService.getChartTemplate(
            this.currentlyConfiguredWidget.widgetType,
        );
    }

    triggerWidgetTypeChange(ev: MatSelectChange) {
        this.widgetTypeService.notify({
            widgetId: this.currentlyConfiguredWidget.elementId,
            newWidgetTypeId: ev.value,
        });
        this.selectWidget();
    }
}

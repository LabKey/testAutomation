/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.components.assay;

import org.labkey.test.Locator;

public class AssayConstants
{
    public static final String PARTICIPANT_VISIT_RESOLVER_FIELD_NAME = "ParticipantVisitResolver";
    public static final String THAW_LIST_TYPE_FIELD_NAME = "ThawListType";
    public static final String TARGET_STUDY_FIELD_NAME = "TargetStudy";

    public static final Locator ASSAY_NAME_FIELD_LOCATOR = Locator.name("Name");
    public static final Locator COMMENTS_FIELD_LOCATOR = Locator.name("Comments");
    public static final Locator TARGET_STUDY_FIELD_LOCATOR = Locator.name(TARGET_STUDY_FIELD_NAME);
    public static final Locator TEXT_AREA_DATA_PROVIDER_LOCATOR = Locator.xpath("//input[@value='textAreaDataProvider']");
    public static final String TEXT_AREA_DATA_COLLECTOR_TEXT_AREA_NAME = "TextAreaDataCollector.textArea";
    public static final Locator TEXT_AREA_DATA_COLLECTOR_LOCATOR = Locator.textarea(TEXT_AREA_DATA_COLLECTOR_TEXT_AREA_NAME);
}

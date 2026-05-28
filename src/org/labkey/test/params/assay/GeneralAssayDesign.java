/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.test.params.assay;

import org.labkey.remoteapi.domain.PropertyDescriptor;

import java.util.List;

public class GeneralAssayDesign extends AssayDesign<GeneralAssayDesign>
{
    public GeneralAssayDesign(String name)
    {
        super("General", name);
    }

    public GeneralAssayDesign setBatchFields(List<PropertyDescriptor> fields, boolean keepExisting)
    {
        return setFields("Batch", fields, keepExisting);
    }

    public GeneralAssayDesign setRunFields(List<PropertyDescriptor> fields, boolean keepExisting)
    {
        return setFields("Run", fields, keepExisting);
    }

    public GeneralAssayDesign setDataFields(List<PropertyDescriptor> fields, boolean keepExisting)
    {
        return setFields("Data", fields, keepExisting);
    }

    @Override
    protected GeneralAssayDesign getThis()
    {
        return this;
    }
}

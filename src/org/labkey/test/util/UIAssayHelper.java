/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class UIAssayHelper extends AbstractAssayHelper
{
    public UIAssayHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    public void importAssay(String assayName, File file, String projectPath) throws CommandException, IOException
    {
        importAssay(assayName, file, projectPath, null, null);
    }

    @Override
    public void importAssay(String assayName, File file, String projectPath, Map<String, Object> batchProperties) throws CommandException, IOException
    {
        importAssay(assayName, file, projectPath, batchProperties, null);
    }

    public void importAssay(String assayName, File file, String projectPath, @Nullable Map<String, Object> batchProperties, @Nullable Map<String, Object> runProperties)
    {
        String[] folders = projectPath.split("/");
        _test.clickProject(folders[0]);
        if (folders.length > 1)
            _test.clickFolder(folders[folders.length - 1]);

        _test.clickAndWait(Locator.linkWithText(assayName));
        _test.clickButton("Import Data");

        if(null != batchProperties)
        {
            for(String tagName : batchProperties.keySet())
            {
                _test.setFormElement(Locator.name(tagName), batchProperties.get(tagName).toString());
            }
            _test.clickButton("Next");
        }

        if(null != runProperties)
        {
            for(String tagName : runProperties.keySet())
            {
                _test.setFormElement(Locator.name(tagName), runProperties.get(tagName).toString());
            }
        }

        _test.checkRadioButton(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));

        _test.setFormElement(Locator.name("__primaryFile__"), file);

        _test.clickButton("Save and Finish");

    }

    @Override
    public void goToUploadXarPage()
    {
        _test.goToManageAssays();
        _test.clickButton("New Assay Design");
        _test.clickAndWait(Locator.linkWithText("upload"));
    }
}

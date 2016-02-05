/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

    public void importAssay(int assayID, File file, String projectPath) throws CommandException, IOException
    {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void importAssay(String assayName, File file, String projectPath) throws CommandException, IOException
    {
        String[] folders = projectPath.split("/");
        _test.clickProject(folders[0]);
        if (folders.length > 1)
            _test.clickFolder(folders[folders.length - 1]);
        _test.clickAndWait(Locator.linkWithText(assayName));
        _test.clickButton("Import Data");
        _test.clickButton("Next");

        _test.checkRadioButton(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));
        _test.setFormElement(Locator.name("__primaryFile__"), file);
        _test.clickButton("Save and Finish");
    }

    @Override
    public void importAssay(String assayName, File file, String projectPath, Map<String, Object> batchProperties) throws CommandException, IOException
    {
        importAssay(assayName, file, projectPath); //UI doesn't need to worry about the batch properties, it's done automatically
    }

    @Override
    public void goToUploadXarPage()
    {
        _test.goToManageAssays();
        _test.clickButton("New Assay Design");
        _test.clickAndWait(Locator.linkWithText("upload"));
    }
}

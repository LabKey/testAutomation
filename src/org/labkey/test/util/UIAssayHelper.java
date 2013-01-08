/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/18/12
 * Time: 1:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class UIAssayHelper extends AbstractAssayHelper
{
    public UIAssayHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    public void importAssay(int assayID, String file, String projectPath) throws CommandException, IOException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void importAssay(String assayName, String file, String projectPath) throws CommandException, IOException
    {
        String[] folders = projectPath.split("/");
        for(String folder : folders)
            _test.clickAndWait(Locator.linkWithText(folder));
        _test.clickAndWait(Locator.linkContainingText(assayName));
        _test.clickButton("Import Data");
        _test.clickButton("Next");

        _test.checkRadioButton("dataCollectorName", "File upload");
        _test.setFormElement("__primaryFile__", new File(file));
        _test.clickButton("Save and Finish");


        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void importAssay(String assayName, String file, String projectPath, Map<String, Object> batchProperties) throws CommandException, IOException
    {
        importAssay(assayName, file, projectPath); //UI doesn't need to worry about the batch properties, it's done automatically
    }
}

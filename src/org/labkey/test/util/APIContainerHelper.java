/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.CreateContainerCommand;
import org.labkey.test.BaseSeleniumWebTest;

/**
 * User: jeckels
 * Date: Jul 20, 2012
 */
public class APIContainerHelper extends AbstractContainerHelper
{
    public APIContainerHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    @Override
    public void doCreateProject(String projectName, String folderType)
    {
        doCreateFolder(projectName, "/", folderType);
    }

    public final void createSubfolder(String parent, String folderName, String folderType)
    {
       doCreateFolder(folderName,  "/" + parent, folderType);
    }

    public void doCreateFolder(String folderName, String path, String folderType)
    {
        _test.log("Creating project with name via API " + folderName);
        Connection connection = new Connection(_test.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        CreateContainerCommand command = new CreateContainerCommand(folderName);
        command.setFolderType(folderType);
        try
        {
            command.execute(connection, path);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        _test.beginAt("/project" + path + "/" + folderName +  "/begin.view?");
    }
}

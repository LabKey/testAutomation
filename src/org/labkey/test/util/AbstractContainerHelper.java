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

import org.labkey.test.BaseSeleniumWebTest;

import java.util.ArrayList;
import java.util.List;

/**
 * User: jeckels
 * Date: Jul 20, 2012
 */
public abstract class AbstractContainerHelper extends AbstractHelper
{
    private List<String> _createdProjects = new ArrayList<String>();

    public AbstractContainerHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    /** @param folderType the name of the type of container to create.
     * May be null, in which case you get the server's default folder type */
    public final void createProject(String projectName, String folderType)
    {
        doCreateProject(projectName, folderType);
        _createdProjects.add(projectName);
    }

    public abstract void createSubfolder(String parent, String folderName, String folderType);


    protected abstract void doCreateProject(String projectName, String folderType);
    protected abstract void doCreateFolder(String projectName, String folderType, String path);

    public List<String> getCreatedProjects()
    {
        return _createdProjects;
    }

    // Projects might be created by other means
    public void addCreatedProject(String projectName)
    {
        _createdProjects.add(projectName);
    }

    public void deleteProject(String projectName)
    {
        deleteProject(projectName, true, 90000);
    }

    public abstract void deleteProject(String projectName, boolean failIfNotFound, int wait);
}

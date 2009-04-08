/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey.test.drt;

/**
 * User: adam
 * Date: Apr 3, 2009
 * Time: 9:18:32 AM
 */
public class ImportStudyTest extends StudyTest
{
    protected static final String PROJECT_NAME = "ImportStudyVerifyProject";
    protected static final String FOLDER_NAME = "My Import Study";

    @Override
    protected String getProjectName()
    {
        return "ImportStudyVerifyProject";
    }

    @Override
    protected String getFolderName()
    {
        return "My Import Study";
    }

    @Override
    protected String getSampleDataPath()
    {
        return super.getSampleDataPath() + "import/";
    }

    @Override
    protected void createStudy()
    {
        initializeFolder();
        initializePipeline();

        beginAt("study/" + getProjectName() + "/" + getFolderName() + "/importStudy.view");
        clickButtonContainingText("Import Study");

        // TODO: Verify that pipeline is done?
    }

    private void initializePipeline()
    {
        clickLinkWithText("Customize Folder");
        toggleCheckboxByTitle("Pipeline");
        submit();
        addWebPart("Data Pipeline");
        clickNavButton("Setup");
        setFormElement("path", getPipelinePath());
        submit();
    }

    @Override
    protected void importSpecimenArchive(String archivePath)
    {
        // Do nothing -- we already loaded the specimen archive with the initial study load
    }
}

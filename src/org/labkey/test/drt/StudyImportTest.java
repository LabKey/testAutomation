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
public class StudyImportTest extends StudyTest
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
        checkRadioButton("source", "pipeline");
        clickButtonContainingText("Import Study");
    }

    private void initializePipeline()
    {
        clickLinkWithText("Folder Settings");
        toggleCheckboxByTitle("Pipeline");
        submit();
        addWebPart("Data Pipeline");
        clickNavButton("Setup");
        setFormElement("path", getPipelinePath());
        submit();
    }

    @Override
    protected void waitForInitialUpload()
    {
        startTimer();
        while (!isLinkPresentWithTextCount("COMPLETE", 3) && !isLinkPresentWithText("ERROR") && elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            log("Waiting for study import");
            sleep(1000);
            refresh();
        }
        assertLinkNotPresentWithText("ERROR");  // Must be surrounded by an anchor tag.
        assertLinkPresentWithTextCount("COMPLETE", 3);
    }

    @Override
    protected void importSpecimenArchive(String archivePath)
    {
        // Do nothing -- we already loaded the specimen archive with the initial study load
    }
}

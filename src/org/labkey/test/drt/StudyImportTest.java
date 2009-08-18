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

import java.io.File;

/**
 * User: adam
 * Date: Apr 3, 2009
 * Time: 9:18:32 AM
 */
public class StudyImportTest extends StudyBaseTest
{
    protected static final String PROJECT_NAME = "ImportStudyVerifyProject";
    protected static final String FOLDER_NAME = "My Import Study";

    private SpecimenImporter _specimenImporter;

    @Override
    protected void createStudy()
    {
        initializeFolder();
        initializePipeline();

        // Import a study.xml to create the study and load all the datasets.  We'll wait for this import to complete
        // before doing any further tests.
        beginAt("study/" + getProjectName() + "/" + getFolderName() + "/importStudy.view");
        checkRadioButton("source", "pipeline");
        clickButtonContainingText("Import Study");

        // Start importing the specimens as well.  We'll let this load in the background while executing the first set of
        // verification steps.  Doing this in parallel speeds up the test.
        _specimenImporter = new SpecimenImporter(new File(getPipelinePath()), new File(getLabKeyRoot(), SPECIMEN_ARCHIVE_A), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getFolderName(), 2);
        _specimenImporter.startImport();
    }

    @Override
    protected void loadSpecimens()
    {
        // Already started this load, just need to wait for it to complete.
        _specimenImporter.waitForComplete();
    }

    protected void afterCreateStudy()
    {
        // Do nothing -- import handles all creation steps
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
        while (!isLinkPresentWithTextCount("COMPLETE", 2) && !isLinkPresentWithText("ERROR") && elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            log("Waiting for study import");
            sleep(1000);
            refresh();
        }
        assertLinkNotPresentWithText("ERROR");  // Must be surrounded by an anchor tag.
        assertLinkPresentWithTextCount("COMPLETE", 2);
    }
}

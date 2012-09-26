/*
 * Copyright (c) 2011-2012 LabKey Corporation
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
package org.labkey.test.tests;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;

/**
 * User: kevink
 * Date: Mar 28, 2011
 *
 * CreateVialsTest also uses the specimen merge feature.
 */
public class SpecimenMergeTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "SpecimenMergeTest";
    private static final String FOLDER_NAME = "My Study";
    private static final String STUDY_NAME = "My Study Study";

    private static final String LAB19_SPECIMENS = "/sampledata/study/specimens/lab19.specimens";
    private static final String LAB20_SPECIMENS = "/sampledata/study/specimens/lab20.specimens";
    private static final String LAB21_SPECIMENS = "/sampledata/study/specimens/lab21.specimens";

    private static final String SPECIMEN_TEMP_DIR = "/sampledata/study/drt_temp";

    private String _studyDataRoot = null;

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected void doCleanup() throws Exception
    {
        _studyDataRoot = getLabKeyRoot() + "/sampledata/study";
        File tempDir = new File(getLabKeyRoot() + SPECIMEN_TEMP_DIR);
        if (tempDir.exists())
        {
            for (File file : tempDir.listFiles())
                file.delete();
            tempDir.delete();
        }
        try { deleteProject(PROJECT_NAME); } catch (Throwable e) {}
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        _studyDataRoot = getLabKeyRoot() + "/sampledata/study";

        _containerHelper.createProject(PROJECT_NAME, null);

        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "Study", null);
        clickButton("Create Study");
        click(Locator.radioButtonByNameAndValue("simpleRepository", "true"));
        clickButton("Create Study");

        setPipelineRoot(_studyDataRoot);
        clickLinkWithText("My Study");
        clickLinkWithText("Manage Files");

        File[] archives = new File[] {
                new File(getLabKeyRoot(), LAB19_SPECIMENS),
                new File(getLabKeyRoot(), LAB20_SPECIMENS),
                new File(getLabKeyRoot(), LAB21_SPECIMENS)
        };
        SpecimenImporter importer = new SpecimenImporter(new File(_studyDataRoot), archives, new File(getLabKeyRoot(), SPECIMEN_TEMP_DIR), FOLDER_NAME, 2);
        importer.setExpectError(true);
        importer.importAndWaitForComplete();

        // Check there was an error in the specimen merge.
        clickLinkWithText("ERROR");
        assertTextPresent("lab20");
        assertTextPresent("Conflicting specimens found for GlobalUniqueId 'AAA07XK5-02'");
        checkExpectedErrors(2);
    }

}

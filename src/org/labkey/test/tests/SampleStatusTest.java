/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
public class SampleStatusTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleStatusTestProject";

    private Boolean previousSampleStatusFlag = null;

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void setupProject()
    {
        SampleStatusTest init = (SampleStatusTest) getCurrentTest();

        // Comment out this line (after you run once) it will make iterating on tests much easier.
        init.doSetup();
    }

    private void doSetup()
    {
        previousSampleStatusFlag = SampleTypeHelper.setSampleStatusEnabled(true);
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME, null);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");

        portalHelper.exitAdminMode();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        if (previousSampleStatusFlag != null)
            SampleTypeHelper.setSampleStatusEnabled(previousSampleStatusFlag);
        else
            SampleTypeHelper.setSampleStatusEnabled(false);
        // If you are debugging tests change this function to do nothing.
        // It can make re-running faster but you need to valid the integrity of the test data on your own.
//        log("Do nothing.");
    }

    @Test
    public void testDeleteSampleTypeWithLockedSamples()
    {
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        log("Add a locked sample status.");
        goToProjectHome();
        goToSchemaBrowser();
        selectQuery("core", "DataStates");
        sampleTypeHelper.addSampleStates(Map.of("TestLocked", SampleTypeHelper.StatusType.Locked));

        log("Add a sample type so we can lock some samples");
        final String sampleTypeName = "SamplesWithLocks";
        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName);
        goToProjectHome();
        sampleTypeHelper.createSampleType(sampleTypeDefinition);
        sampleTypeHelper.goToSampleType(sampleTypeName);
        log("Add a single unlocked sample");
        Map<String, String> fieldMap = Map.of("Name", "U-1");
        sampleTypeHelper.insertRow(fieldMap);
        log("Add a single locked sample");
        fieldMap = Map.of("Name", "L-1", "SampleState", "TestLocked");
        sampleTypeHelper.insertRow(fieldMap);
        log("Delete the sample type, which should produce no errors.");
        Locator.linkWithText("Sample Types").findElement(this.getDriver()).click();
        DataRegionTable drt = sampleTypeHelper.getSampleTypesList();
        drt.checkCheckbox(drt.getRowIndex("Name", sampleTypeName));
        drt.clickHeaderButton("Delete");
        waitForText(WAIT_FOR_JAVASCRIPT, "Confirm Deletion");
        clickButton("Confirm Delete");
        waitForText(WAIT_FOR_JAVASCRIPT, "Sample Types");
    }


    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}

/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Specimen;
import org.labkey.test.util.LogMethod;

import java.io.File;

@Category({Specimen.class})
public class SpecimenExtendedTest extends SpecimenBaseTest
{
    protected static final String PROJECT_NAME = "SpecimenExtendedVerifyProject";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsersIfPresent(USER1, USER2);
        super.doCleanup(afterTest);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        initializeFolder();
        _containerHelper.enableModule("nPOD");

        importStudyFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"));

        setPipelineRoot(getPipelinePath());

        setupRequestabilityRules();
        startSpecimenImport(2);
        waitForSpecimenImport();
        setupRequestStatuses();
        setupActorsAndGroups();
    }

    @Override
    protected void doVerifySteps()
    {
        clickTab("Specimen Data");
        click(Locator.linkContainingText("Vial Search"));
        waitForElement(Locator.button("Search"));
        click(Locator.button("Search"));

        selectSpecimens("999320812", "999320396");

        _extHelper.clickMenuButton("Request Options", "Create New Request");
        selectOptionByText(Locator.name("destinationLocation"), DESTINATION_SITE);
        setFormElement(Locator.id("input0"), "Assay Plan");
        setFormElement(Locator.id("input1"), "Shipping");
        setFormElement(Locator.id("input2"), "Comments");
        clickButton("Save & Continue");

        waitForText("Please contact an Administrator to establish");
        clickButton("OK");
        waitForText("This request has not been submitted.");

        click(Locator.linkContainingText("Update Extended Request"));
        waitForText("Failed to load Extended Specimen Request.");
        clickButton("OK");

        click(Locator.linkContainingText("Specimen Requests"));
    }

    private void selectSpecimens(String... specimens)
    {
        for (String specimen : specimens)
        {
            checkCheckboxByNameInDataRegion(specimen);
        }
    }
}

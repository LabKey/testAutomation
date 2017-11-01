/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.List;

@Category({DailyA.class})
public class UniprotAnnotationTest extends BaseWebDriverTest
{
    private static final String UNIPROT_FILENAME = "tinyuniprot.xml";
    private static final String PROJECT_NAME = "ProteinAnnotationVerifier";

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("ms2");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Test
    public void testSteps()
    {
        log("Starting UniprotAnnotationTest");

        ensureAdminMode();
        goToAdminConsole().goToAdminConsoleLinksSection();
        clickAndWait(Locator.linkWithText("protein databases"));

        boolean alreadyLoaded = isTextPresent(UNIPROT_FILENAME);

        final DataRegionTable annotInsertions = new DataRegionTable("AnnotInsertions", this);
        if (alreadyLoaded)
        {
            annotInsertions.setFilter("FileName", "Contains", UNIPROT_FILENAME);
            annotInsertions.checkAll();
            doAndWaitForPageToLoad(() ->
            {
                annotInsertions.clickHeaderButtonByText("Delete");
                acceptAlert();
            });
        }

        clickButton("Import Data");
        setFormElement(Locator.id("fname"), TestFileUtils.getLabKeyRoot() + "/sampledata/proteinAnnotations/" + UNIPROT_FILENAME);
        selectOptionByText(Locator.name("fileType"), "uniprot");
        if (alreadyLoaded) checkCheckbox(Locator.name("clearExisting"));
        clickButton("Load Annotations");

        waitForRunningPipelineJobs(60000);

        annotInsertions.setFilter("FileName", "Contains", UNIPROT_FILENAME);
        annotInsertions.setFilter("CompletionDate", "Is Not Blank", null);
        assertElementPresent(Locator.tag("a").withAttributeContaining("href", "showAnnotInsertDetails"));

        _containerHelper.createProject(PROJECT_NAME, "MS2");
        clickProject(PROJECT_NAME);
        setFormElement(Locator.name("identifier"), "Ppia");
        uncheckCheckbox(Locator.checkboxByName("restrictProteins"));
        clickButton("Search");

        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0); // Search results are hidden by default.
        assertTextPresent("Peptidyl-prolyl cis-trans isomerase A");

        click(Locator.linkWithText("PPIA_MOUSE"));
        //opens in separate window
        Object[] windows = getDriver().getWindowHandles().toArray();
        getDriver().switchTo().window((String) windows[1]);
        waitAndClick(Locator.id("expandCollapse-ProteinAnnotationsView"));
        assertTextPresent("PPIA_MOUSE");
        waitForText(WAIT_FOR_JAVASCRIPT, "Q9CWJ5");
        assertTextPresent("Q9R137", "Mus musculus");

        getDriver().close();
        getDriver().switchTo().window((String) windows[0]);

        clickProject(PROJECT_NAME);
        setFormElement(Locator.name("identifier"), "Defa1");
        uncheckCheckbox(Locator.checkboxByName("restrictProteins"));
        clickButton("Search");

        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0); // Search results are hidden by default.
        assertTextPresent("Defensin-1 precursor");

        click(Locator.linkWithText("DEF1_MOUSE"));
        //opens in separate window
        windows = getDriver().getWindowHandles().toArray();
        getDriver().switchTo().window((String) windows[1]);
        waitAndClick(Locator.id("expandCollapse-ProteinAnnotationsView"));
        waitForText(WAIT_FOR_JAVASCRIPT, "P11477");
        assertTextPresent("Q61448", "DEF1_MOUSE", "ENSMUSG00000074440", "Mus musculus");
        getDriver().close();
        getDriver().switchTo().window((String) windows[0]);
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }
}

/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;

/**
 * User: jeckels
 * Date: Dec 4, 2007
 */
@Category({DailyA.class})
public class UniprotAnnotationTest extends BaseSeleniumWebTest
{
    private static final String UNIPROT_FILENAME = "tinyuniprot.xml";
    private static final String PROJECT_NAME = "ProteinAnnotationVerifier";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/ms2";
    }

    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doTestSteps() throws Exception
    {
        log("Starting UniprotAnnotationTest");

        ensureAdminMode();
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("protein databases"));
        assertTextNotPresent(UNIPROT_FILENAME);

        clickButton("Load New Annot File");
        setFormElement("fname", getLabKeyRoot() + "/sampledata/proteinAnnotations/" + UNIPROT_FILENAME);
        setFormElement("fileType", "uniprot");
        clickButton("Load Annotations");

        setFilter("AnnotInsertions", "FileName", "Contains", UNIPROT_FILENAME);
        setFilter("AnnotInsertions", "CompletionDate", "Is Not Blank");

        int seconds = 0;
        while (!isTextPresent(UNIPROT_FILENAME) && seconds++ < 60)
        {
            Thread.sleep(1000);
            refresh();
        }

        assertTextPresent(UNIPROT_FILENAME);

        _containerHelper.createProject(PROJECT_NAME, "MS2");
        clickProject(PROJECT_NAME);
        setFormElement("identifier", "Ppia");
        uncheckCheckbox("restrictProteins");
        clickButton("Search");

        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"),0); // Search results are hidden by default.
        assertTextPresent("Peptidyl-prolyl cis-trans isomerase A");

        selenium.openWindow("", "prot");
        click(Locator.linkWithText("PPIA_MOUSE"));
        //opens in separate window
        selenium.waitForPopUp("prot", "10000");
        selenium.selectWindow("prot");
        assertTextPresent("PPIA_MOUSE");
        click(Locator.id("expandCollapse-ProteinAnnotationsView"));
        waitForText("Q9CWJ5", WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Q9R137");
        assertTextPresent("Mus musculus");
        selenium.close();
        selenium.selectWindow(null);

        clickProject(PROJECT_NAME);
        setFormElement("identifier", "Defa1");
        uncheckCheckbox("restrictProteins");
        clickButton("Search");

        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"),0); // Search results are hidden by default.
        assertTextPresent("Defensin-1 precursor");

        selenium.openWindow("", "prot");
        click(Locator.linkWithText("DEF1_MOUSE"));
        //opens in separate window
        selenium.waitForPopUp("prot", "10000");
        selenium.selectWindow("prot");
        click(Locator.id("expandCollapse-ProteinAnnotationsView"));
        waitForText("P11477", WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Q61448");
        assertTextPresent("DEF1_MOUSE");
        assertTextPresent("ENSMUSG00000074440");
        assertTextPresent("Mus musculus");
        selenium.close();
        selenium.selectWindow(null);
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }
}

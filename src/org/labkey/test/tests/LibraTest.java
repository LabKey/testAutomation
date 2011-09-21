/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ExtHelper;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 8/19/11
 * Time: 3:32 PM
 */
public class LibraTest extends MS2Test
{
    private String standardView = "Standard View";
    protected String proteinProphetView = "Protein Prophet View";
    private String iTRAQ_QUANTITATION_RATIO = "iTRAQ Quantitation Ratio ";

    @Override
    protected String getProjectName()
    {
        return "LibraTest" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void configure()
    {
        createProject(getProjectName(), "MS2");
        setPipelineRoot(getLabKeyRoot() + "/sampledata/xarfiles/ms2pipe/iTRAQ/");
        clickLinkContainingText(getProjectName());

        clickButton("Process and Import Data");
        waitAndClick(Locator.fileTreeByName("Libra"));
        ExtHelper.clickFileBrowserFileCheckbox(this, "iTRAQ.search.xar.xml");
        selectImportDataAction("Import Experiment");
        waitForTextToDisappear("LOADING");
        sleep(200); // Takes a moment for run to appear after import.
        refresh();

        //set xar


        clickLinkContainingText(runName);
    }


    String runName = "itraq/iTRAQ (Libra)";
    int normalizationCount = 8;

    @Override
    protected void doTestSteps()
    {
        configure();
        waitForText("Grouping");
        selenium.select("viewTypeGrouping", "Standard");
        clickNavButton("Go");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        addNormalizationCount();

        CustomizeViewsHelper.saveCustomView(this, standardView);
        System.out.print("foo");

        checkForITRAQNormalization();

        proteinProphetTest();
        groupTest();
        specificProteinTest();
    }

    protected void newWindowTest(String linkToClick, String verificationString, String... additionalChecks)
    {
        selenium.openWindow("", "prot");
        clickLinkContainingText(linkToClick,false);
        selenium.selectWindow("prot");
        waitForPageToLoad();
        waitForText(verificationString);

        checkForITRAQNormalization();
        checkForITRAQQuantitation();

        assertTextPresent(additionalChecks);
        selenium.close();
        selenium.selectWindow(null);

    }
    private void specificProteinTest()
    {

        newWindowTest("gi|2144275|JC5226_ubiquitin_", "Protein Sequence");
        //TODO:  single cell check
    }

    private void groupTest()
    {
        newWindowTest("4", "Scan", "gi|28189228|similar_to_polyub");
    }

    private void checkForITRAQNormalization()
    {
        checkForNormalizationCountofSomething("iTRAQ Quantitation Normalized ");
    }

    private void checkForITRAQQuantitation()
    {
        checkForNormalizationCountofSomething(iTRAQ_QUANTITATION_RATIO);
    }

    protected void checkForNormalizationCountofSomething(String toCheck)
    {
        for(int i = 1; i <= normalizationCount; i++)
        {
            assertTextPresent(toCheck + i);
        }

    }

    private void addNormalizationCount()
    {
        for(int i = 1; i <= normalizationCount; i++)
        {
            CustomizeViewsHelper.addCustomizeViewColumn(this, "iTRAQQuantitation/Normalized" + i, "Normalized " + i);
        }
    }

    private void proteinProphetTest()
    {
        clickMenuButton("Views", "ProteinProphet");

        waitForElement(Locator.navButton("Views"), WAIT_FOR_JAVASCRIPT);
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        for(int i=1; i<=normalizationCount; i++)
        {
            CustomizeViewsHelper.addCustomizeViewColumn(this, "ProteinProphetData/ProteinGroupId/iTRAQQuantitation/Ratio" + i, "Ratio " + i);
        }

        addNormalizationCount();

        CustomizeViewsHelper.saveCustomView(this, proteinProphetView);
        checkForITRAQQuantitation();


        assertTableCellTextEquals("dataregion_MS2Peptides", 2, "iTRAQ Quantitation Ratio 1", "0.71");

        Locator img = Locator.xpath("//img[contains(@id,'MS2Peptides-Handle')]");
        click(img);
        checkForITRAQNormalization();
    }

    @Override
    protected void doCleanup()
    {
        deleteProject(getProjectName());
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4HelperWD;
import org.labkey.test.util.LogMethod;

/**
 * User: elvan
 * Date: 6/14/12
 * Time: 1:58 PM
 */
public abstract class TargetedMSTest extends BaseWebDriverTest
{
    protected static final String SKY_FILE = "MRMer.zip";
    protected static final String SKY_FILE2 = "MRMer_renamed_protein.zip";

    public enum FolderType {
        Experiment, Library, LibraryProtein, Undefined
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMS" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void setupAndImportData(FolderType folderType)
    {
        _containerHelper.createProject(getProjectName(), "Panorama");
        selectFolderType(folderType);
        setPipelineRoot(getSampledataPath() + "/TargetedMS");

        importData(SKY_FILE);
        importData(SKY_FILE2);
    }

    protected void importData(String file)
    {
        goToModule("Pipeline");
        clickButton("Process and Import Data");
        waitForText(file, 5*defaultWaitForPage);
        selectPipelineFileAndImportAction(file, "Import Skyline Results");
        waitForText("Skyline document import");
        waitForPipelineJobsToFinish(1);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyRunSummaryCounts()
    {
        log("Verifying expected summary counts");
        assertElementPresent(Locator.xpath("//tr[td[text()='Protein Count']][td[text()='24']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Peptide Count']][td[text()='44']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Precursor Count']][td[text()='88']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Transition Count']][td[text()='296']]"));
        assertTextPresent("CDC19 SGDID:S000000036, Chr I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate, the input for aerobic (TCA cycle) or anaerobic (glucose fermentation) respiration");
        // Verify expected peptides/proteins in the nested view
        //Verify that amino acids from peptides are highlighted in blue as expected.
    }


    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyPeptide()
    {
        // Click on a peptide.
        String targetProtein  = "LTSLNVVAGSDLR";
        clickAndWait(Locator.linkContainingText(targetProtein));
        //Verify it’s associated with the right protein and other values from details view.
        //protein name, portien, neutral mass, avg. RT , precursor
        assertTextPresent(targetProtein, "YAL038W", "1343.7408", "27.9232", "677.8818++ (heavy)");

        //Verify the spectrum shows up correctly.

        //Verify we get the expected number of chromatogram graphs.
        assertElementPresent(Locator.xpath("//img[contains(@src, 'Chromatogram')]"),3);
        assertElementPresent(Locator.xpath("//img[contains(@alt, 'Chromatogram')]"), 5);

        //Click on a precursor icon link.
        clickAndWait(Locator.linkWithHref("precursorAllChromatogramsChart.view?"));
        //Verify expected values in detail view. Verify chromatogram.
        assertTextPresentInThisOrder("Precursor Chromatograms", "YAL038W",  "LTSLNVVAGSDLR", "672.8777");
        assertElementPresent(Locator.xpath("//img[contains(@src, 'Chromatogram')]"));

        goBack();
        clickAndWait(Locator.linkContainingText("YAL038W"));
        //Verify summary info
        assertTextPresent("CDC19 SGDID:S000000036, Chr I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate,");

        assertTextPresent("Sequence Coverage", "Peptides", "LTSLNVVAGSDLR", "TNNPETLVALR", "GVNLPGTDVDLPALSEK", "TANDVLTIR",
                "GDLGIEIPAPEVLAVQK", "EPVSDWTDDVEAR",
                "Peak Areas");

        goBack();
        clickAndWait(Locator.linkContainingText(SKY_FILE));
        //Toggle to Transition view (click on down arrow in Precursor List webpart header)
        click(Locator.xpath("//th[span[contains(text(), 'Precursor List')]]/span/a/img"));
        clickAndWait(Locator.tagContainingText("span","Transition List"));
        waitForText("Transition List");
        DataRegionTable drt = new DataRegionTable("transitions_view", this);
        drt.getDataAsText(5, "Label");
        Assert.assertEquals("heavy", drt.getDataAsText(5, "Label"));
        Assert.assertEquals("1353.7491", drt.getDataAsText(5, "Precursor Neutral Mass"));
        Assert.assertEquals("677.8818", drt.getDataAsText(5, "Q1 m/z"));
        Assert.assertEquals("y7", drt.getDataAsText(5, "Fragment"));
        Assert.assertEquals("727.3973", drt.getDataAsText(5, "Q3 m/z"));
        // We don't find these values based on their column headers because DataRegionTable gets confused with the
        // nested data regions having the same id in the HTML. The checks above happen to work because
        // they correspond to columns that aren't in the parent table, so the XPath flips to the second table with
        // that id, which has enough columns to satisfy the Locator
        assertTextPresent("1343.7408", "1226.6619", "1001.5505");

        //Click down arrow next to protein name. Click "Search for other references to this protein"
        Locator l = Locator.xpath("//span[a[text()='YAL038W']]/span/img");
        waitForElement(l);
        mouseOver(l);
        waitForText("Search for other references to this protein");
        clickAndWait(Locator.linkContainingText("Search for other references to this protein"));

        //Verify Targeted MS Peptides section of page.
        //Click on Details link.
        //Spot check some values.
        assertTextPresent("Protein Search Results", "Targeted MS Peptides","LTSLNVVAGSDLR",
               "TNNPETLVALR",  "GVNLPGTDVDLPALSEK",  "TANDVLTIR",
                "GDLGIEIPAPEVLAVQK", "EPVSDWTDDVEAR");
        click(Locator.imageWithSrc("plus.gif", true));
        assertTextPresent("I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate, the input for aerobic (TCA cyc...");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyModificationSearch()
    {
        // add modificaiton search webpart and do an initial search by AminoAcid and DeltaMass
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        assertTextPresent("Mass Spec Search");
        _ext4Helper.clickExt4Tab("Modification Search");
        waitForElement(Locator.name("aminoAcids"));
        setFormElement(Locator.name("aminoAcids"), "R");
        setFormElement(Locator.name("deltaMass"), "10");
        waitAndClickAndWait(Locator.ext4Button("Search"));
        waitForText("Modification Search Results");
        //waitForText("1 - 13 of 13");
        assertTextPresentInThisOrder("Targeted MS Modification Search", "Targeted MS Peptides");
        assertTextPresent("Amino Acids:", "Delta Mass:");
        Assert.assertEquals(13, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'R[+10]')]")));
        Assert.assertEquals(0, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'K[+8]')]")));

        // search for K[+8] modification
        setFormElement(Locator.name("aminoAcids"), "k R, N"); // should be split into just chars
        setFormElement(Locator.name("deltaMass"), "8.01"); // should be rounded to a whole number
        waitAndClickAndWait(Locator.ext4Button("Search"));
        //waitForText("1 - 31 of 31");
        Assert.assertEquals(0, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'R[+10]')]")));
        Assert.assertEquals(31, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'K[+8]')]")));

        // test custom name search type
        _ext4Helper.selectRadioButton("Search By:", "Modification Name");
        assertElementNotVisible(Locator.name("aminoAcids"));
        assertElementNotVisible(Locator.name("deltaMass"));
        assertElementNotVisible(Locator.name("unimodName"));
        assertElementVisible(Locator.name("customName"));
        _ext4Helper.selectRadioButton("Type:", "Names used in imported experiments");
        _ext4Helper.selectComboBoxItem("Custom Name:", "Label:13C(6)15N(4) (C-term R)");
        waitAndClickAndWait(Locator.ext4Button("Search"));
        //waitForText("1 - 13 of 13");
        Assert.assertEquals(13, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'R[+10]')]")));
        Assert.assertEquals(0, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'K[+8]')]")));
        _ext4Helper.selectComboBoxItem("Custom Name:", "Label:13C(6)15N(2) (C-term K)");
        waitAndClickAndWait(Locator.ext4Button("Search"));
        //waitForText("1 - 31 of 31");
        Assert.assertEquals(0, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'R[+10]')]")));
        Assert.assertEquals(31, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'K[+8]')]")));

        // test unimod name search type
        _ext4Helper.selectRadioButton("Type:", "All Unimod modifications");
        assertElementNotVisible(Locator.name("aminoAcids"));
        assertElementNotVisible(Locator.name("deltaMass"));
        assertElementNotVisible(Locator.name("customName"));
        assertElementVisible(Locator.name("unimodName"));
        _ext4Helper.selectComboBoxItem(Ext4HelperWD.Locators.formItemWithLabelContaining("Unimod Name:"), "Label:13C(6)15N(4) (C-term R)");
        waitAndClickAndWait(Locator.ext4Button("Search"));
        //waitForText("1 - 13 of 13");
        Assert.assertEquals(13, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'R[+10]')]")));
        Assert.assertEquals(0, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'K[+8]')]")));

        // test C-term search using special character (i.e. ] )
        _ext4Helper.selectRadioButton("Search By:", "Delta Mass");
        setFormElement(Locator.name("aminoAcids"), "]");
        setFormElement(Locator.name("deltaMass"), "8");
        waitAndClickAndWait(Locator.ext4Button("Search"));
        //waitForText("1 - 31 of 31");
        Assert.assertEquals(0, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'R[+10]')]")));
        Assert.assertEquals(31, getElementCount( Locator.xpath("//td/a/span[contains(@title, 'K[+8]')]")));
    }

    @LogMethod
    protected void selectFolderType(FolderType folderType) {
        log("Select Folder Type: " + folderType);
        switch(folderType)
        {
            case Experiment:
                click(Locator.radioButtonById("experimentalData")); // click the first radio button - Experimental Data
                break;
            case Library:
                click(Locator.radioButtonById("chromatogramLibrary")); // click the 2nd radio button - Library
                break;
            case LibraryProtein:
                click(Locator.radioButtonById("chromatogramLibrary")); // click the 2nd radio button - Library
                click(Locator.checkboxById("precursorNormalized")); // check the normalization checkbox.
                break;
        }
        clickButton("Finish");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/targetedms";
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}

package org.labkey.test.tests;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 6/14/12
 * Time: 1:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class TargetedMSTest extends BaseSeleniumWebTest
{

    private final String SKY_FILE = "MRMer.sky";
    @Override
    protected String getProjectName()
    {
        return "TargetedMS";
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        createProject(getProjectName(), "Targeted MS");
        setPipelineRoot(getSampledataPath() + "\\TargetedMS");
        goToProjectHome();
        addWebPart("Data Pipeline");
        clickNavButton("Process and Import Data");
        sleep(5000); //pipeline browser takes forever to load in linux
        selectPipelineFileAndImportAction("MRMer/" + SKY_FILE, "Import Skyline Results");
        waitForText("Targeted MS Runs ");
        waitForTextWithRefresh(SKY_FILE, defaultWaitForPage);

        assertTextPresent("Transitions");
        clickLinkContainingText(SKY_FILE);

        log("Verifying expected summary counts ");
        assertElementPresent(Locator.xpath("//tr[td[text()='Peptide Group Count']][td[text()='24']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Peptide Count']][td[text()='44']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Precursor Count']][td[text()='88']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Transition Count']][td[text()='296']]"));
        assertTextPresent("CDC19 SGDID:S000000036, Chr I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate, the input for aerobic (TCA cycle) or anaerobic (glucose fermentation) respiration");
        // Verify expected peptides/proteins in the nested view
        //Verify that amino acids from peptides are highlighted in blue as expected.


//        Click on a peptide.
        String targetProtein  = "LTSLNVVAGSDLR";
        clickLinkContainingText(targetProtein);
        //Verify it’s associated with the right protein and other values from details view.
        //protein name, portien, neutral mass, avg. RT , precursor
        assertTextPresent(targetProtein, "YAL038W", "1343.7408", "27.9232", "677.8818++ (heavy)");

        //Verify the spectrum shows up correctly.

        //Verify we get the expected number of chromatogram graphs.
        assertElementPresent(Locator.xpath("//img[contains(@src, 'Chromatogram')]"),3);
        assertElementPresent(Locator.xpath("//img[contains(@alt, 'Chromatogram')]"), 5);

        //Click on a precursor icon link.
        clickLink(Locator.linkWithHref("precursorAllChromatogramsChart.view?"));
        //Verify expected values in detail view. Verify chromatogram.
        assertTextPresentInThisOrder("Precursor Chromatograms", "YAL038W",  "LTSLNVVAGSDLR", "672.8777");
        assertElementPresent(Locator.xpath("//img[contains(@src, 'Chromatogram')]"));

        goBack();
        clickLinkContainingText("YAL038W");
        //Verify summary info
        assertTextPresent("CDC19 SGDID:S000000036, Chr I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate,");

        assertTextPresent("Sequence Coverage", "Peptides", "LTSLNVVAGSDLR", "TNNPETLVALR", "GVNLPGTDVDLPALSEK", "TANDVLTIR",
                 "GDLGIEIPAPEVLAVQK", "EPVSDWTDDVEAR",
                "Peak Areas");

        goBack();
        clickLinkContainingText(SKY_FILE);
        //Toggle to Transition view (click on down arrow in Precursor List webpart header)
        click(Locator.xpath("//th[span[contains(text(), 'Precursor List')]]/span/a/img"));
        clickLink(Locator.tagContainingText("span","Transition List"));
        waitForText("Transition List");
        DataRegionTable drt = new DataRegionTable("transitions_view", this);
        drt.getDataAsText(5, "Precursor");
        assertEquals("LTSLNVVAGSDLR", drt.getDataAsText(5, "Precursor"));
        assertEquals("heavy", drt.getDataAsText(5, "Label"));
        assertEquals("1343.7408", drt.getDataAsText(5, "Neutral Mass"));
        assertEquals("677.8818", drt.getDataAsText(5, "Q1 m/z"));
        assertEquals("727.3973", drt.getDataAsText(5, "Q3 m/z"));

        //Click down arrow next to protein name. Click "Search for other references to this protein"

        String xpath = Locator.xpath("//span[a[text()='YAL038W']]/span/img").toXpath();
        selenium.mouseOver(xpath);
        selenium.mouseMoveAt(xpath, "1,1");
        waitForText("Search for other references to this protein");
        clickLinkContainingText("Search for other references to this protein");

        //Verify TargetedMS Peptides section of page.
        //Click on Details link.
        //Spot check some values.
        assertTextPresent("Protein Search Results", "TargetedMS Peptides ","LTSLNVVAGSDLR",
               "TNNPETLVALR",  "GVNLPGTDVDLPALSEK",  "TANDVLTIR",
                "GDLGIEIPAPEVLAVQK", "EPVSDWTDDVEAR");
        click(Locator.imageWithSrc("plus.gif", true, 2));
        assertTextPresent("I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate, the input for aerobic (TCA cyc...");


    }

    @Override
    protected void doCleanup() throws Exception
    {
        deleteProject(getProjectName());
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

package org.labkey.test.tests;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ExtHelper;
import sun.security.krb5.Checksum;

import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 8/19/11
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
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

        clickButtonContainingText("Process and Import Data");
        sleep(5000);
        Locator l = Locator.xpath("//span[contains(text(),'Libra')]");
        click(l);
        ExtHelper.clickFileBrowserFileCheckbox(this, "iTRAQ.search.xar.xml");
        selectImportDataAction("Import Experiment");
        waitForTextToDisappear("LOADING");
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
        clickButtonContainingText("Go");
        waitForText("Peptide");
        sleep(200);
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        addNormalizationCount();

        CustomizeViewsHelper.saveCustomView(this, standardView);
        System.out.print("foo");

        checkForITRAQNormalization();

        proteinProphetTest();
        //TODO:  activate these                                                      g
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

        CustomizeViewsHelper.openCustomizeViewPanel(this);
        for(int i=1; i<=normalizationCount; i++)
        {
            CustomizeViewsHelper.addCustomizeViewColumn(this, "ProteinProphetData/ProteinGroupId/iTRAQQuantitation/Ratio" + i, "Ratio " + i);
        }

        addNormalizationCount();

        CustomizeViewsHelper.saveCustomView(this, proteinProphetView);
        checkForITRAQQuantitation();


        int columnHeader = getColumnIndex("dataregion_MS2Peptides", "iTRAQ Quantitation Ratio 1");
        String text = getProteinProphetTableCell(2, columnHeader);
        assertEquals("0.71", text);

        Locator img = Locator.xpath("//img[contains(@id,'MS2Peptides-Handle')]");
        click(img);
        checkForITRAQNormalization();


        //To change body of created methods use File | Settings | File Templates.
    }

    //I have no idea why this works when getTableCellText doesn't.
    protected String getProteinProphetTableCell(int row, int column)
    {
        Locator l = Locator.xpath("//table[@id='dataregion_MS2Peptides']/tbody/tr[" + (row+1) + "]/td["+ (column+1) + "]");
        String text = getText(l);

        return text;
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

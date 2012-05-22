package org.labkey.test.tests;

import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 5/10/12
 * Time: 6:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class NabMigrationTest extends NabOldTest
{

    private static final String NEW_ASSAY_NAME = "New Assay";
    private static final String NEW_ASSAY_NAME2 = "Second Assay";
    private static String assayRowId = "";
    private static String newField = "new Field" + TRICKY_CHARACTERS;
    private static String newFieldVal = "42";

    protected void runUITests()
    {
        createRuns();
        String url = getCurrentRelativeURL();
        String rowId = url.split("=")[1];
        assayRowId = rowId.substring(0, rowId.indexOf("&"));

        createAssay(NEW_ASSAY_NAME);
        createAssay(NEW_ASSAY_NAME2);
        copyAssay();
        verifyMigration();
    }

    private void verifyMigration()
    {
        log("Verify that the data didn't transfer to the first assay");
        clickLinkWithText(FOLDER_NAME);
        clickTab("Nab");
        clickLinkWithText(NEW_ASSAY_NAME);
        assertEquals("Data present in NAb assay that should be unused", 0, new DataRegionTable(NEW_ASSAY_NAME + " Runs", this).getDataRowCount());
        goBack();

        log("verify data present in second assay");
        clickLinkWithText(NEW_ASSAY_NAME2);
        assertEquals("Newly added value incorrect.", newFieldVal, new DataRegionTable(NEW_ASSAY_NAME2 + " Runs", this).getDataAsText(1, newField));
        assertEquals("No present in NAb assay", 2, new DataRegionTable(NEW_ASSAY_NAME2 + " Runs", this).getDataRowCount());DataRegionTable drt = new  DataRegionTable(NEW_ASSAY_NAME2+ " Runs", this);
        assertTrue("LegacyID not present in new assay run list", drt.getIndexWhereDataAppears(assayRowId, "LegacyID") > -1);
//        assertEquals("RowID from original second nab run not copied into legacyID field", assayRowId, first);
        clickLinkWithText("run details");
        assertElementPresent(Locator.xpath("//img[@alt='Neutralization Graph']"));
        assertEquals("Specimen 1 Dilution Factor incorrect", "3.0", getText(Locator.xpath("//tr[td[text()='Specimen 1']]//td[2]")));
        assertEquals("Specimen 4 Initial Dilution incorrect", "20.0", getText(Locator.xpath("//tr[td[text()='Specimen 4']]//td[3]")));
        assertEquals("Incorrect 80% Curve Based for Specimen 3", "456", getText(Locator.xpath("//tbody[tr/td[text()='Curve Based']]/tr[6]/td[4]")));
    }

    private void copyAssay()
    {
        clickLinkContainingText("eprecated");
        clickLinkContainingText("Migrate");
        int index=-1;
        if(isPresentInThisOrder(NEW_ASSAY_NAME, NEW_ASSAY_NAME2)==null)
            index=1;
        else
            index=0;

        setFormElement(Locator.name("newField1", index), newFieldVal);
        clickNavButtonByIndex("Migrate", index);
        waitForPipelineJobsToFinish(1);
    }

    private void createAssay(String assayName)
    {
        clickLinkWithText(FOLDER_NAME);
        clickTab("Nab");
        startCreateNabAssay(assayName);
        addRunField("LegacyID", "LegacyID", ListHelper.ListColumnType.Integer);
        addRunField(newField, newField, ListHelper.ListColumnType.Integer);

        clickButtonContainingText("Save & Close");

    }

}

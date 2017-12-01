package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyC;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyC.class})
public class MultiplePKUploadAssayTest extends BaseWebDriverTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
            super.doCleanup(afterTest);
            _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
                MultiplePKUploadAssayTest init = (MultiplePKUploadAssayTest)getCurrentTest();
                init.doSetup();
    }

    private void doSetup()
    {
            _containerHelper.createProject(getProjectName(), "Assay");
    }

    @Test
    public void verifyMultiplePKListUpload() throws Exception
    {
        goToProjectHome();
        log("Create GPAT assay");
        String assayName = "ThawList Assay";
        AssayDesignerPage assayDesigner = _assayHelper.createAssayAndEdit("General", assayName);

        log("Creating individual lists");
        File List1 = TestFileUtils.getSampleData("ThawLists/List1.xlsx");
        File List2 = TestFileUtils.getSampleData("ThawLists/List2.xlsx");
        _listHelper.createListFromFile(getProjectName(),"ThawList1",List1);
        _listHelper.createListFromFile(getProjectName(),"ThawList2",List2);

        log("Joining the query");
        String joinedList = "JoinedThawList";
        String sql = "SELECT ThawList1.participantID,ThawList1.specimenID,ThawList2.visitId,ThawList2.Date\n" +
                    "FROM    ThawList1,ThawList2\n" +
                    "Where   ThawList1.participantID=ThawList2.participantID\n" +
                    "ORDER BY ThawList1.participantID";
        goToSchemaBrowser();
        createNewQuery("lists");
        setFormElement(Locator.name("ff_newQueryName"), joinedList);
        clickAndWait(Locator.lkButton("Create and Edit Source"));
        setCodeEditorValue("queryText", sql);
        clickButton("Save & Finish");

        log("Importing the data into assay");
        goToProjectHome();
        PortalHelper portalHelper = new PortalHelper(this);
        log("Now import the data from the list into the assay.");
        portalHelper.clickAndWait(Locator.linkWithText(assayName));
        portalHelper.clickButton(("Import Data"), "Batch Properties");
        portalHelper.checkRadioButton(Locator.radioButtonById("RadioBtn-Lookup"));
        portalHelper.waitForText("Use an existing sample list");
        portalHelper.checkRadioButton(Locator.radioButtonById("RadioBtn-ThawListType-List"));
        portalHelper.waitForText("Schema:");

        log("Selecting the joined list");
        _ext4Helper.selectComboBoxItem(Locator.id("thawListSchemaName"), "lists");
        waitForElement(Locator.css(".query-loaded-marker"));
        _ext4Helper.selectComboBoxItem(Locator.id("thawListQueryName"), joinedList);

        portalHelper.clickButton("Next", "Run Properties");

        log("Specifying the specimenID");
        assertTitleContains("Data Import: Run Properties and Data File");
        setFormElement(Locator.name("name"), "First run");
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), "SpecimenID\nS17");
        clickButton("Save and Finish");

        log("Verifying the visit ID");
        clickAndWait(Locator.linkWithText("First run"));
        DataRegionTable table = new DataRegionTable("Data",getDriver());
        List<String> visitID = table.getColumnDataAsText("VisitID");
        assertEquals("Missing visitID","17.0",visitID.get(0));

    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "Multiple PK Thaw list assay project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }

}
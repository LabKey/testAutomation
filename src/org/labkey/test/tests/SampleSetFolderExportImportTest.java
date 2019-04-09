package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.pages.admin.FolderManagementPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleSetHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({DailyA.class})
public class SampleSetFolderExportImportTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleSetExportFolderTest";
    private static final String IMPORT_PROJECT_NAME = "SampleSetImportFolderTest";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void setupProject()
    {
        SampleSetFolderExportImportTest init = (SampleSetFolderExportImportTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        // Delete the import project if it exists.
        _containerHelper.deleteProject(IMPORT_PROJECT_NAME, false);

        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME);

        projectMenu().navigateToProject(PROJECT_NAME);
        portalHelper.addWebPart("Sample Sets");
        portalHelper.addWebPart("Experiment Runs");

    }

    protected boolean areDataListEqual(List<Map<String, String>> list01, List<Map<String, String>> list02)
    {
        return areDataListEqual(list01, list02, true);
    }

    protected boolean areDataListEqual(List<Map<String, String>> list01, List<Map<String, String>> list02, boolean logMismatch)
    {
        if( list01.size() != list02.size())
            return false;

        // Order the two lists so compare can be done by index and not by searching the two lists.
        Collections.sort(list01, (Map<String, String> o1, Map<String, String> o2)->
                {
                    return o1.get("Name").compareTo(o2.get("Name"));
                }
        );

        Collections.sort(list02, (Map<String, String> o1, Map<String, String> o2)->
                {
                    return o1.get("Name").compareTo(o2.get("Name"));
                }
        );

        boolean areEqual = true;

        for(int i = 0; i < list01.size(); i++)
        {
            if(!list01.get(i).equals(list02.get(i)))
            {
                if(logMismatch)
                {
                    StringBuilder errorMsg = new StringBuilder();
                    errorMsg.append("\n*************** ERROR ***************");
                    errorMsg.append("\nFound a mismatch in the lists.");
                    errorMsg.append("\nlist01(" + i + "): " + list01.get(i));
                    errorMsg.append("\nlist02(" + i + "): " + list02.get(i));
                    errorMsg.append("\n*************** ERROR ***************");
                    log(errorMsg.toString());
                }
                areEqual = false;
            }
        }

        return areEqual;
    }

    protected List<Map<String, String>> getSampleDataFromDB(String folderPath, String sampleSetName, List<String> fields)
    {
        List<Map<String, String>> results = new ArrayList<>(6);
        Map<String, String> tempRow;

        Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        SelectRowsCommand cmd = new SelectRowsCommand("samples", sampleSetName);
        cmd.setColumns(fields);

        try
        {
            SelectRowsResponse response = cmd.execute(cn, folderPath);

            for (Map<String, Object> row : response.getRows())
            {

                tempRow = new HashMap<>();

                for(String key : row.keySet())
                {

                    if (fields.contains(key))
                    {

                        String tmpFlag = key;

                        if(key.equalsIgnoreCase("Flag/Comment"))
                            tmpFlag = "Flag";

                        if (null == row.get(key))
                        {
                            tempRow.put(tmpFlag, "");
                        }
                        else
                        {
                            tempRow.put(tmpFlag, row.get(key).toString());
                        }

                    }

                }

                results.add(tempRow);

            }

        }
        catch(CommandException | IOException excp)
        {
            Assert.fail(excp.getMessage());
        }

        return results;
    }

    @Test
    public void testExportAndImportWithMissingAndRequiredFields()
    {
        final String SAMPLE_SET_NAME = "ExportMissingValues";

        final String REQUIRED_FIELD_NAME = "field01";
        final String REQUIRED_FIELD_DISPLAY_NAME = "Field01";
        final String MISSING_FIELD_NAME = "field02";
        final String MISSING_FIELD_DISPLAY_NAME = "Field02";
        String INDICATOR_FIELD_NAME;

        // Unfortunately Postgres and MSSQL case the missing indicator field differently. This causes issues when
        // getting the data by a db query and validating it against expected values.
        if(WebTestHelper.getDatabaseType().equals(WebTestHelper.DatabaseType.MicrosoftSQLServer))
            INDICATOR_FIELD_NAME = MISSING_FIELD_NAME + "_MVIndicator";
        else
            INDICATOR_FIELD_NAME = MISSING_FIELD_NAME + "_mvindicator";

        StringBuilder errorLog = new StringBuilder();

        log("Create a SampleSet that has missing values.");

        log("Create expected missing value indicators.");
        clickProject(PROJECT_NAME);

        final String MV_INDICATOR_01 = "Q";
        final String MV_DESCRIPTION_01 = "Data currently under quality control review.";
        final String MV_INDICATOR_02 = "N";
        final String MV_DESCRIPTION_02 = "Required field marked by site as 'data not available'.";
        final String MV_INDICATOR_03 = "X";
        final String MV_DESCRIPTION_03 = "Here is a non system one.";

        List<Map<String, String>> missingValueIndicators = new ArrayList<>();
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_01, "description", MV_DESCRIPTION_01));
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_02, "description", MV_DESCRIPTION_02));
        missingValueIndicators.add(Map.of("indicator", MV_INDICATOR_03, "description", MV_DESCRIPTION_03));

        setupMVIndicators(missingValueIndicators);

        clickProject(PROJECT_NAME);

        List<Map<String, String>> sampleData = new ArrayList<>();
        List<Map<String, String>> expectedValuesInDB = new ArrayList<>();

        String[] sampleNames = {"mv01", "mv02", "mv03", "mv04", "mv05", "mv06", "mv07", "mv08", "DerivedSample01"};

        // Later the test will query the DB to validate that the imported data is as expected. This becomes a little
        // tricky because the missing value fields only have a value in the _mvindicator field, any value in the filed is removed.
        // To work around this I used a second list (expectedValuesInDB) to check against the DB.
        sampleData.add(Map.of("Name", sampleNames[0], REQUIRED_FIELD_NAME, "aa_mv01", MISSING_FIELD_NAME, "This value is here.", INDICATOR_FIELD_NAME, ""));
        expectedValuesInDB.add(Map.of("Name", sampleNames[0], REQUIRED_FIELD_NAME, "aa_mv01", MISSING_FIELD_NAME, "This value is here.", INDICATOR_FIELD_NAME, ""));

        sampleData.add(Map.of("Name", sampleNames[1], REQUIRED_FIELD_NAME, "bb_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, "Q"));
        expectedValuesInDB.add(Map.of("Name", sampleNames[1], REQUIRED_FIELD_NAME, "bb_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, "Q"));

        sampleData.add(Map.of("Name", sampleNames[2], REQUIRED_FIELD_NAME, "cc_mv01", MISSING_FIELD_NAME, "Just to break things up.", INDICATOR_FIELD_NAME, ""));
        expectedValuesInDB.add(Map.of("Name", sampleNames[2], REQUIRED_FIELD_NAME, "cc_mv01", MISSING_FIELD_NAME, "Just to break things up.", INDICATOR_FIELD_NAME, ""));

        sampleData.add(Map.of("Name", sampleNames[3], REQUIRED_FIELD_NAME, "ee_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, ""));
        expectedValuesInDB.add(Map.of("Name", sampleNames[3], REQUIRED_FIELD_NAME, "ee_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, ""));

        sampleData.add(Map.of("Name", sampleNames[4], REQUIRED_FIELD_NAME, "dd_mv01", MISSING_FIELD_NAME, "X", INDICATOR_FIELD_NAME, ""));
        expectedValuesInDB.add(Map.of("Name", sampleNames[4], REQUIRED_FIELD_NAME, "dd_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, ""));

        sampleData.add(Map.of("Name", sampleNames[5], REQUIRED_FIELD_NAME, "ff_mv01", MISSING_FIELD_NAME, "N", INDICATOR_FIELD_NAME, "N"));
        expectedValuesInDB.add(Map.of("Name", sampleNames[5], REQUIRED_FIELD_NAME, "ff_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, "N"));

        sampleData.add(Map.of("Name", sampleNames[6], REQUIRED_FIELD_NAME, "gg_mv01", MISSING_FIELD_NAME, "Here is a valid string value.", INDICATOR_FIELD_NAME, "Q"));
        expectedValuesInDB.add(Map.of("Name", sampleNames[6], REQUIRED_FIELD_NAME, "gg_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, "Q"));

        sampleData.add(Map.of("Name", sampleNames[7], REQUIRED_FIELD_NAME, "hh_mv01", MISSING_FIELD_NAME, "X", INDICATOR_FIELD_NAME, "X"));
        expectedValuesInDB.add(Map.of("Name", sampleNames[7], REQUIRED_FIELD_NAME, "hh_mv01", MISSING_FIELD_NAME, "", INDICATOR_FIELD_NAME, "X"));

        log("Create the sample set named '" + SAMPLE_SET_NAME + "' and add the fields.");
        SampleSetHelper sampleHelper = new SampleSetHelper(this);
        sampleHelper.createSampleSet(SAMPLE_SET_NAME, null);
        List<FieldDefinition> fields = new ArrayList<>();
        fields.add(new FieldDefinition(REQUIRED_FIELD_NAME)
                .setType(FieldDefinition.ColumnType.String)
                .setMvEnabled(false)
                .setRequired(true));
        fields.add(new FieldDefinition(MISSING_FIELD_NAME)
                .setType(FieldDefinition.ColumnType.String)
                .setMvEnabled(true)
                .setRequired(false));

        sampleHelper.addFields(fields);

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));
        sampleHelper = new SampleSetHelper(this);

        log("Bulk import the samples.");
        sampleHelper.bulkImport(sampleData);

        log("Change the view so the missing value indicator is there and for the screen shot is useful on failure.");
        sampleHelper = new SampleSetHelper(this);
        DataRegionTable drtSamples = sampleHelper.getSamplesDataRegionTable();
        CustomizeView cv = drtSamples.openCustomizeGrid();
        cv.showHiddenItems();
        cv.addColumn(INDICATOR_FIELD_NAME);
        cv.saveCustomView();

        log("Derive a sample from the given samples, this will create an experiment run which is needed for export.");

        drtSamples.checkAllOnPage();
        clickAndWait(Locator.lkButtonContainingText("Derive Sample"));

        selectOptionByText(Locator.name("targetSampleSetId"), SAMPLE_SET_NAME + " in /" + PROJECT_NAME);
        clickButtonContainingText("Next");

        // TODO: Should validate that the Derive Samples action shows the various fields as expected. That is the required and missing value fields should have the correct input type. Will be fixed in 19.2.
        setFormElement(Locator.tagWithName("input", "outputSample1_Name"), sampleNames[8]);
        setFormElement(Locator.tagWithName("input", "outputSample1_" + REQUIRED_FIELD_NAME), "Required text for this field.");
        setFormElement(Locator.tagWithName("input", "outputSample1_" + MISSING_FIELD_NAME), "Q");
        clickButtonContainingText("Submit");

        // TODO: There is a bug where derived values do not honor missing value fields (treat them as a text field). So the indicator field for this sample will be empty. Will be fixed in 19.2.
        expectedValuesInDB.add(Map.of("Name", sampleNames[8], REQUIRED_FIELD_NAME, "Required text for this field.", MISSING_FIELD_NAME, "Q", INDICATOR_FIELD_NAME, ""));

        // Wait for the header to show up, and view this as success.
        waitForElementToBeVisible(Locator.tagWithText("h3", "Sample DerivedSample01"));

        goToProjectHome();

        log("Export folder. Select 'Experiments and runs' as part of the export.");
        FolderManagementPage folderManagementPage = goToFolderManagement();
        folderManagementPage.goToExportTab();

        new Checkbox(Locator.tagWithText("label", "Experiments and runs").precedingSibling("input")
                .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT)).check();


        File exportedFolderFile = doAndWaitForDownload(()->findButton("Export").click());

        log("Folder should have been exported!");

        log("Create a new folder and import the previously exported folder.");

        _containerHelper.createProject(IMPORT_PROJECT_NAME);

        goToProjectHome(IMPORT_PROJECT_NAME);

        importFolderFromZip(exportedFolderFile, false, 1);

        log("Folder should now have been imported.");

        goToProjectHome(IMPORT_PROJECT_NAME);

        log("Validate that the number of Sample Sets in the imported folder is the expected value. If not fail test.");
        Assert.assertTrue("Does not look like the Sample Set has been imported.", isElementVisible(Locator.linkWithText(SAMPLE_SET_NAME)));

        DataRegionTable sampleSetsDataRegion = new DataRegionTable("SampleSet", getWrappedDriver());

        Assert.assertEquals("Number of Sample Sets not as expected.", 1, sampleSetsDataRegion.getDataRowCount());

        String sampleCount = sampleSetsDataRegion.getDataAsText(0, "SampleCount" );

        log("Check some of the other expected value. If they are not just log an error but continue testing.");
        String errorMsg;
        if(!sampleCount.trim().equalsIgnoreCase("9"))
        {
            errorMsg = "Number of samples imported not as expected.\nExpected '9', found '" + sampleCount.trim() +"'.";
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            errorLog.append(errorMsg);
            errorLog.append("\n");
        }

        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));

        sampleHelper = new SampleSetHelper(this);
        DataRegionTable samplesDataRegion = sampleHelper.getSamplesDataRegionTable();

        log("Validated that all of the fields are present in the UI.");

        List<String> columnLabels = samplesDataRegion.getColumnLabels();

        log("Here are the fields visible: " + columnLabels);

        errorLog.append(checkDisplayFields("Name", columnLabels));
        errorLog.append(checkDisplayFields("Flag", columnLabels));
        errorLog.append(checkDisplayFields(REQUIRED_FIELD_DISPLAY_NAME, columnLabels));
        errorLog.append(checkDisplayFields(MISSING_FIELD_DISPLAY_NAME, columnLabels));

        log("Validate that there is a link to each of the samples.");

        log("Validated that the imported data is as expected by looking at the database");

        for(String sampleName : sampleNames)
        {

            if(!isElementVisible(Locator.linkWithText(sampleName)))
            {
                errorMsg = "Did not find a link for sample named '" + sampleName + "'.";
                log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

                errorLog.append(errorMsg);
                errorLog.append("\n");
            }

        }

        // TODO: Checking the field values in the DB will fail because export/import for SampleSets doesn't honor missing value fields. This will be fixed in 19.2.
        /*
        List<Map<String, String>> resultsFromDB = getSampleDataFromDB("/" + IMPORT_PROJECT_NAME, SAMPLE_SET_NAME, Arrays.asList("Name", REQUIRED_FIELD_NAME, MISSING_FIELD_NAME, INDICATOR_FIELD_NAME));

        Assert.assertTrue("Imported Sample Set data not as expected.", areDataListEqual(resultsFromDB, expectedValuesInDB));
        */

        if(errorLog.length() > 0)
            Assert.fail(errorLog.toString());

        log("All done.");
    }

    private StringBuilder checkDisplayFields(String displayField, List<String> columnLabels)
    {
        StringBuilder tmpString = new StringBuilder();

        if(!columnLabels.contains("Name"))
        {
            String errorMsg = "Did not find the 'Name' column.";
            log("\n*************** ERROR ***************\n" + errorMsg + "\n*************** ERROR ***************");

            tmpString.append(errorMsg);
            tmpString.append("\n");
        }

        return tmpString;
    }

    @LogMethod
    private void setupMVIndicators(List<Map<String, String>> missingValueIndicators)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Missing Values"));
        uncheckCheckbox(Locator.checkboxById("inherit"));

        // Delete all site-level settings
        for (WebElement deleteButton : Locator.tagWithAttribute("img", "alt", "delete").findElements(getDriver()))
        {
            deleteButton.click();
            shortWait().until(ExpectedConditions.stalenessOf(deleteButton));
        }

        for(int index = 0; index < missingValueIndicators.size(); index++)
        {
            clickButton("Add", 0);
            WebElement mvInd = Locator.css("#mvIndicatorsDiv input[name=mvIndicators]").index(index).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
            setFormElement(mvInd, missingValueIndicators.get(index).get("indicator"));
            WebElement mvLabel = Locator.css("#mvIndicatorsDiv input[name=mvLabels]").index(index).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
            setFormElement(mvLabel, missingValueIndicators.get(index).get("description"));
        }
        clickButton("Save");
    }

}

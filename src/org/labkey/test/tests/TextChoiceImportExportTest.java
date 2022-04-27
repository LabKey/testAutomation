package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
public class TextChoiceImportExportTest extends TextChoiceTest
{

    private static final String ORG_PROJ_NAME = "TextChoice_Import_Export_Test";
    private static final String IMPORT_PROJ_NAME = "Imported_TextChoice_Import_Export_Test";

    protected static Map<String, String> assayResultRowData = new HashMap<>();

    @Override
    protected String getProjectName()
    {
        return ORG_PROJ_NAME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(ORG_PROJ_NAME, afterTest);
        _containerHelper.deleteProject(IMPORT_PROJ_NAME, afterTest);
    }

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        TextChoiceImportExportTest init = (TextChoiceImportExportTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {

        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), null);

        log("Add some web parts to make it easier to debug etc...");
        goToProjectHome();
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        portalHelper.addWebPart("Assay List");
        portalHelper.addWebPart("Lists");
        portalHelper.exitAdminMode();

        createSampleType();

        createList();

        createAssayDesign();

        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        createAssayRun();

    }

    /**
     * Create a run for the assay.
     */
    protected void createAssayRun()
    {

        DataRegionTable runTable = new DataRegionTable("Runs", getDriver());
        runTable.clickHeaderButtonAndWait("Import Data");

        Locator batchLocator = Locator.name(getSelectControlName(BATCH_TC_FIELD));

        log(String.format("Set the batch field '%s' to '%s'.", BATCH_TC_FIELD, BATCH_VALUE));
        WebElement select = batchLocator.findElement(getDriver());
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(BATCH_VALUE));

        clickButton("Next");

        log(String.format("Set the Assay ID to '%s'.", ASSAY_RUN_ID));

        setFormElement(Locator.tagWithName("input", "name"), ASSAY_RUN_ID);

        Locator runLocator = Locator.name(getSelectControlName(RUN_TC_FIELD));

        log(String.format("Set the run field '%s' to '%s'.", RUN_TC_FIELD, RUN_VALUE));
        select = runLocator.findElement(getDriver());
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(RUN_VALUE));

        StringBuilder resultsPasteText = new StringBuilder();
        resultsPasteText.append(String.format("%s\t%s\n", RESULT_SAMPLE_FIELD, RESULT_TC_FIELD));

        int valueIndex = 0;
        int count = 1;
        for (String sample : SAMPLES)
        {

            String resultsValue = RESULT_FIELD_VALUES.get(valueIndex);

            if(count%2 == 0)
                valueIndex++;

            resultsPasteText.append(String.format("%s\t%s\n", sample, resultsValue));

            assayResultRowData.put(sample, resultsValue);

            count++;
        }

        log("Paste in the results and save.");

        setFormElement(Locator.id("TextAreaDataCollector.textArea"), resultsPasteText.toString());

        clickButton("Save and Finish");

    }

    /**
     * <p>
     *     Test exporting then importing a file that has a sample type, assay and list with TextChoice fields.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a folder/project with a sample type, assay design with run/result and a list that have TextChoice fields.</li>
     *         <li>Export the folder as a zip file and validate no errors are generated.</li>
     *         <li>Import the zip file into a new folder.</li>
     *         <li>Validate that the data for the various domains is as expected.</li>
     *     </ul>
     * </p>
     *
     * @throws IOException Can be thrown by the creat api.
     * @throws CommandException Can be thrown by the creat api.
     */
    @Test
    public void testExportImport() throws IOException, CommandException
    {
        goToProjectHome();
        log("Export the project.");

        File exportFile = exportFolderToBrowserAsZip();

        log(String.format("Project was exported to file '%s'.", exportFile));

        Assert.assertEquals("Export caused server side errors.", 0, getServerErrorCount());

        log("Import the file that was just exported.");

        _containerHelper.createProject(IMPORT_PROJ_NAME, null);

        importFolderFromZip(exportFile);

        Assert.assertEquals("Import caused server side errors.", 0, getServerErrorCount());

        log("Validate the data in the imported file.");
        goToProjectHome(IMPORT_PROJ_NAME);

        log("Validate the list data.");

        // Go to the list so if there is a failure the screenshot is meaningful.
        waitAndClickAndWait(Locator.linkWithText(LIST_NAME));

        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("lists", LIST_NAME);
        cmd.setColumns(Arrays.asList(LIST_TC_FIELD, LIST_TEXT_FIELD));

        SelectRowsResponse response = cmd.execute(cn, getCurrentContainerPath());

        List<Map<String, String>> importedListData = new ArrayList<>();
        for (Map<String, Object> row : response.getRows())
        {
            importedListData.add(
                    Map.of(LIST_TC_FIELD, row.get(LIST_TC_FIELD).toString(),
                            LIST_TEXT_FIELD, row.get(LIST_TEXT_FIELD).toString()));
        }

        checker().withScreenshot("Imported_List_Error").verifyEquals("Imported data for the list not as expected.", LIST_DATA, importedListData);

        log("Validate the assay data.");
        goToProjectHome(IMPORT_PROJ_NAME);
        waitAndClickAndWait(Locator.linkWithText(ASSAY_NAME));

        DataRegionTable dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        Map<String, String> rowData = dataRegionTable.getRowDataAsMap("Assay ID", ASSAY_RUN_ID);

        checker().verifyEquals(String.format("Value for the run TextChoice field '%s' is not as expected.", RUN_TC_FIELD),
                        RUN_VALUE, rowData.get(RUN_TC_FIELD));

        checker().verifyEquals(String.format("Value for the batch TextChoice field '%s' is not as expected.", BATCH_TC_FIELD),
                        BATCH_VALUE, rowData.get(String.format("Batch/%s", BATCH_TC_FIELD)));

        checker().screenShotIfNewError("Import_Runs_Table_Error");

        clickAndWait(Locator.linkWithText(ASSAY_RUN_ID));

        verifyRunResultsTable(assayResultRowData, BATCH_VALUE, RUN_VALUE);

        checker().screenShotIfNewError("Import_Results_Table_Error");

        log("Validate the sample type data.");
        goToProjectHome(IMPORT_PROJ_NAME);
        waitAndClickAndWait(Locator.linkWithText(ST_NAME));
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        // Let the helper verify the values. If a TextChoice value is not as expected this will assert and stop the test.
        sampleTypeHelper.verifyDataValues(ST_DATA);

    }

}

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
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.issues.IssueListDefDataRegion;
import org.labkey.test.pages.issues.IssuesAdminPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
public class TextChoiceImportExportAndOtherDomainsTest extends TextChoiceTest
{

    private static final String ORIGINAL_PROJ_NAME = "TextChoice_Import_Export_Test";
    private static final String IMPORTED_PROJ_NAME = "Imported_TextChoice_Import_Export_Test";

    // List field name, values etc...
    private static final String LIST_NAME = "Simple_TC_List";
    private static final String LIST_TC_FIELD = "LTC_Field";
    private static final String LIST_TEXT_FIELD = "Str";
    private static final List<String> LIST_VALUES = Arrays.asList("L1", "L2", "L3", "L4");
    private static List<Map<String, String>> listData = new ArrayList<>();

    // Issue names, etc...
    private static final String ISSUE_DESIGN_NAME = "Simple_TC_Issue";
    private static final String ISSUE_TC_FIELD = "Issue_TC_Field";
    private static final List<String> ISSUE_FIELD_VALUES = List.of("I1", "I2", "I3", "I4");
    private static final String ISSUE_TITLE = "Issue to validate TextChoice fields";
    private static final String ISSUE_VALUE = ISSUE_FIELD_VALUES.get(2);

    @Override
    protected String getProjectName()
    {
        return ORIGINAL_PROJ_NAME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(ORIGINAL_PROJ_NAME, afterTest);
        _containerHelper.deleteProject(IMPORTED_PROJ_NAME, afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        TextChoiceImportExportAndOtherDomainsTest init = (TextChoiceImportExportAndOtherDomainsTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {

        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule("Issues");

        log("Add some web parts to make it easier to debug etc...");
        goToProjectHome();
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        portalHelper.addWebPart("Assay List");
        portalHelper.addWebPart("Lists");
        portalHelper.addWebPart("Issue Definitions");
        portalHelper.addWebPart("Issues Summary");
        portalHelper.exitAdminMode();

    }

    /**
     * <p>Very basic test to validate that a list domain will work with a TextChoice field.</p>
     * <p>
     *     If something happens during creation it will be a 'hard' failure and will stop the test. The list data will
     *     be validated after the import.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a list design with a TextChoice field.</li>
     *         <li>Import list data in bulk.</li>
     *         <li>Add a new list item using the UI.</li>
     *     </ul>
     * </p>
     */
    private void verifyTextChoiceInList()
    {

        FieldDefinition tcField = new FieldDefinition(LIST_TC_FIELD, FieldDefinition.ColumnType.TextChoice);
        tcField.setTextChoiceValues(LIST_VALUES);

        FieldDefinition txtField = new FieldDefinition(LIST_TEXT_FIELD, FieldDefinition.ColumnType.String);

        log(String.format("Create a list named '%s' with a string field '%s' and a TextChoice field '%s'.",
                LIST_NAME, LIST_TEXT_FIELD, LIST_TC_FIELD));

        _listHelper.createList(getCurrentContainerPath(), LIST_NAME, "Key", tcField, txtField);

        log("Bulk upload data into the list.");

        // Save the expected list data. Will be used after import.
        listData.add(Map.of(LIST_TC_FIELD, LIST_VALUES.get(0), LIST_TEXT_FIELD, "My"));
        listData.add(Map.of(LIST_TC_FIELD, LIST_VALUES.get(1), LIST_TEXT_FIELD, "Name"));
        listData.add(Map.of(LIST_TC_FIELD, LIST_VALUES.get(2), LIST_TEXT_FIELD, "Is"));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s\t%s\n", LIST_TC_FIELD, LIST_TEXT_FIELD));
        for(Map<String, String> row : listData)
        {
            sb.append(String.format("%s\t%s\n", row.get(LIST_TC_FIELD), row.get(LIST_TEXT_FIELD)));
        }

        _listHelper.uploadData(sb.toString());

        Map<String, String> newRow = Map.of(LIST_TC_FIELD, LIST_VALUES.get(1), LIST_TEXT_FIELD, "Who?");

        // Add the new row to the expected data.
        listData.add(newRow);

        log("Add a new row to the list using the UI.");
        _listHelper.insertNewRow(newRow);

    }

    /**
     * <p>
     *     Very basic test to validate a issue design can be created with a TextChoice.
     * </p>
     * <p>
     *     This just validates that no errors happen during design creation or insertion of an issue with the TextChoice
     *     field set. Issues designs are not exported so no validation of this data will happen after import.
     * </p>
     */
    private void verifyTextChoiceInIssueDesign()
    {

        goToProjectHome();

        log(String.format("Create an issue design named '%s' with a TextChoice field '%s'.", ISSUE_DESIGN_NAME, ISSUE_TC_FIELD));

        IssuesAdminPage issuesAdminPage = IssueListDefDataRegion.fromWebPart(getDriver()).createIssuesListDefinition(ISSUE_DESIGN_NAME);

        DomainFormPanel formPanel = issuesAdminPage.getFieldsPanel();
        formPanel.addField(new FieldDefinition(ISSUE_TC_FIELD, FieldDefinition.ColumnType.TextChoice).setTextChoiceValues(ISSUE_FIELD_VALUES));

        issuesAdminPage.clickSave();

        log("Validate that a new issue can be inserted that uses the TextChoice field.");
        IssuesHelper issuesHelper = new IssuesHelper(getDriver());

        String tcFieldName;

        // Looks like field name is cased differently in MSSQL.
        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL)
        {
            tcFieldName = ISSUE_TC_FIELD.toLowerCase();
        }
        else
        {
            tcFieldName = getSelectControlName(ISSUE_TC_FIELD);
        }

        Map<String, String> issueDetails = Map.of("title", ISSUE_TITLE, "assignedTo", getDisplayName(), tcFieldName, ISSUE_VALUE);

        issuesHelper.addIssue(issueDetails);

        goToProjectHome();
        waitAndClickAndWait(Locator.linkWithText(ISSUE_DESIGN_NAME));

        DataRegionTable issuesTable = new DataRegionTable.DataRegionFinder(getDriver())
                .withName(String.format("issues-%s", ISSUE_DESIGN_NAME.toLowerCase())).find();

        Map<String, String> rowData = issuesTable.getRowDataAsMap("Title", ISSUE_TITLE);

        checker().withScreenshot("Issue_TextChoice_Field_Error")
                .verifyEquals(String.format("Value in TextChoice field '%s' not as expected.", ISSUE_TC_FIELD),
                        ISSUE_VALUE, rowData.get(ISSUE_TC_FIELD.toLowerCase()));

    }

    /**
     * <p>
     *     Test using TextChoice in some other domain, exporting and then importing the project.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Create a folder/project with a sample type, assay design with run/result</li>
     *         <li>Validate that a TextChoice field can be used in a list.</li>
     *         <li>Validate that a TextChoice field can be used in an issue design.</li>
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
    public void testOtherDomainsExportAndImport() throws IOException, CommandException
    {
        goToProjectHome();

        log("Create a sample type, assay design and an assay run. These will be used to validate export/import.");
        createDefaultSampleTypeWithTextChoice();

        createDefaultAssayDesignWithTextChoice();

        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        Map<String, String> assayResultRowData = createAssayRun();

        log("Create a list with a TextChoice field. The list will also be validated after import.");
        verifyTextChoiceInList();

        log("Create an issue design with a TextChoice field and create an issue that uses it. Issue designs are not exported.");
        verifyTextChoiceInIssueDesign();

        log("Export the project.");

        File exportFile = exportFolderToBrowserAsZip();

        log(String.format("Project was exported to file '%s'.", exportFile));

        // Not sure if I need this.
        Assert.assertEquals("Export caused server side errors.", 0, getServerErrorCount());

        log("Import the file that was just exported.");

        _containerHelper.createProject(IMPORTED_PROJ_NAME, null);

        importFolderFromZip(exportFile);

        // Not sure if I need this either.
        Assert.assertEquals("Import caused server side errors.", 0, getServerErrorCount());

        log("Validate the data in the imported file.");
        goToProjectHome(IMPORTED_PROJ_NAME);

        log("Validate the list data.");

        // Will validate the list by looking at the data returned by SelectRowsCommand.
        // However, will navigate to the list in the UI so if there is a failure the screenshot is meaningful.
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

        checker().withScreenshot("Imported_List_Error")
                .verifyEquals("Imported data for the list not as expected.", listData, importedListData);

        log("Validate the assay data.");
        goToProjectHome(IMPORTED_PROJ_NAME);
        waitAndClickAndWait(Locator.linkWithText(ASSAY_NAME));

        DataRegionTable dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        Map<String, String> rowData = dataRegionTable.getRowDataAsMap("Assay ID", ASSAY_RUN_ID);

        checker().verifyEquals(String.format("Value for the run TextChoice field '%s' is not as expected.", RUN_TC_FIELD),
                        RUN_VALUE, rowData.get(RUN_TC_FIELD));

        checker().verifyEquals(String.format("Value for the batch TextChoice field '%s' is not as expected.", BATCH_TC_FIELD),
                        BATCH_VALUE, rowData.get(String.format("Batch/%s", BATCH_TC_FIELD)));

        checker().screenShotIfNewError("Import_Runs_Table_Error");

        clickAndWait(Locator.linkWithText(ASSAY_RUN_ID));

        verifyRunResultsTable(assayResultRowData, RUN_VALUE);

        checker().screenShotIfNewError("Import_Results_Table_Error");

        log("Validate the sample type data.");
        goToProjectHome(IMPORTED_PROJ_NAME);
        waitAndClickAndWait(Locator.linkWithText(ST_NAME));
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        // Let the helper verify the values. If a TextChoice value is not as expected this will assert and stop the test.
        sampleTypeHelper.verifyDataValues(ST_DATA);

    }

}

/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.ImportDataCommand;
import org.labkey.remoteapi.query.ImportDataResponse;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.BaseRowsCommand;
import org.labkey.remoteapi.query.MoveRowsCommand;
import org.labkey.remoteapi.query.RowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.params.list.VarListDefinition;
import org.labkey.test.util.data.TestDataUtils;
import org.labkey.test.util.query.QueryApiHelper;
import org.labkey.remoteapi.query.TruncateTableCommand;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.labkey.test.util.search.SearchAdminAPIHelper;
import org.openqa.selenium.Alert;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test trigger script matrix, expands on {@link ScriptValidationTest} which covers custom schemas (Vehicles)
 */
@Category({Daily.class, Data.class})
@BaseWebDriverTest.ClassTimeout(minutes = 8)
public class TriggerScriptTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "Test Trigger Script Project";
    private static final String SUBFOLDER_NAME = "SubfolderA";
    private static final String SUBFOLDER_PATH = "/" + PROJECT_NAME + "/" + SUBFOLDER_NAME;

    //List constants
    private static final String TRIGGER_MODULE = "triggerTestModule";
    private static final String SIMPLE_MODULE = "simpletest";
    private static final String LIST_NAME = "Employees";
    private static final String LIST_SCHEMA = "lists";
    private static final String AFTER_INSERT_ERROR = "This is the After Insert Error";
    private static final String AFTER_UPDATE_ERROR = "This is the After Update Error";
    private static final String BEFORE_UPDATE_COMPANY = "Before Update changed me";
    private static final String BEFORE_DELETE_ERROR = "This is the Before Delete Error";
    private static final String AFTER_DELETE_ERROR = "This is the After Delete Error";

    private static final String MANAGED_STRUCT_ADD_ERROR = "attempted to add";
    private static final String MANAGED_STRUCT_REMOVE_ERROR = "attempted to remove";

    //Dataset constants
    private static final String STUDY_SCHEMA = "study";
    private static final String DATASET_NAME = "Demographics";
    private static final String INDIVIDUAL_TEST = "Individual Test";
    private static final String API_TEST = "API Test";
    private static final String IMPORT_TEST = "Import Test";

    private static final String DATA_CLASSES_SCHEMA = "exp.data";
    private static final String DATA_CLASSES_NAME = "DataClassTest";

    private static final String SAMPLE_TYPE_SCHEMA = "samples";
    private static final String SAMPLE_TYPE_NAME = "SampleTypeTest";

    private static final String COMMENTS_FIELD = "Comments";
    private static final String COUNTRY_FIELD = "Country";
    public static final String PEOPLE_LIST_NAME = "People";

    protected final PortalHelper _portalHelper = new PortalHelper(this);
    private static ListDefinition EMPLOYEE_LIST;

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("Query", STUDY_SCHEMA, SIMPLE_MODULE, TRIGGER_MODULE);
    }

    @Nullable
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

    public static class EmployeeRecord
    {
        public String name, ssn, company, employeeId;

        public EmployeeRecord(String name, String ssn, String company)
        {
            this.name = name;
            this.ssn = ssn;
            this.company = company;
        }

        public Map<String, Object> toMap()
        {
            return Maps.of("Name", name, "SSN", ssn, "Company", company, "employeeId", employeeId);
        }

        public Map<String, String> toStringMap()
        {
            return Maps.of("name", name, "ssn", ssn, "company", company);
        }

        public String toDelimitedString(String delimiter)
        {
            return name + delimiter + ssn + delimiter + company + delimiter + (employeeId != null ? employeeId : "") + "\n";
        }

        public static EmployeeRecord fromMap(Map<String, Object> map)
        {
            EmployeeRecord newbie = new EmployeeRecord((String)map.get("Name"), (String)map.get("ssn"), (String)map.get("Company"));
            if (map.containsKey("employeeId"))
                newbie.employeeId = (String)map.get("employeeId");

            return newbie;
        }

        public static String getTsvHeaders()
        {
            return "Name\tSSN\tCompany\tEmployeeId\n";
        }
    }

    /**
     * Delegate interface to move test to the appropriate Data UI
     */
    private interface GoToDataUI
    {
        void goToDataUIPage();
    }

    @BeforeClass
    public static void projectSetup() throws Exception
    {
        TriggerScriptTest init = getCurrentTest();
        init.doSetup();
    }

    protected void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), SUBFOLDER_NAME);
        _containerHelper.enableModules(getProjectName(), List.of("Query", SIMPLE_MODULE, TRIGGER_MODULE));

        // Create a data class
        new DataClassDefinition(DATA_CLASSES_NAME)
                .setFields(List.of(new FieldDefinition(COMMENTS_FIELD), new FieldDefinition(COUNTRY_FIELD)))
                .create(createDefaultConnection(), getProjectName());

        // Create a sample type
        SampleTypeAPIHelper.createEmptySampleType(getProjectName(), new SampleTypeDefinition(SAMPLE_TYPE_NAME)
                .setFields(List.of(new FieldDefinition(COMMENTS_FIELD), new FieldDefinition(COUNTRY_FIELD))));

        // Create lists
        {
            List<FieldDefinition> fields = List.of(
                new FieldDefinition("name").setLabel("Name"),
                new FieldDefinition("ssn").setLabel("SSN"),
                new FieldDefinition("company").setLabel("Company"),
                new FieldDefinition("employeeId").setLabel("Employee ID")
                        .setHidden(true)
                        .setShownInInsertView(false)
                        .setShownInUpdateView(false)
            );

            EMPLOYEE_LIST = new VarListDefinition(LIST_NAME).setKeyName("name").setFields(fields);
            EMPLOYEE_LIST.create(createDefaultConnection(), getProjectName());

            fields = List.of(
                new FieldDefinition("Name").setDescription("Name"),
                new FieldDefinition("Age", ColumnType.Integer).setDescription("Age"),
                new FieldDefinition("FavoriteDateTime", ColumnType.DateAndTime).setDescription("Favorite date time. Who doesn't have one?"),
                new FieldDefinition("Crazy", ColumnType.Boolean).setDescription("Crazy?")
            );

            new IntListDefinition(PEOPLE_LIST_NAME, "Key")
                    .setFields(fields)
                    .create(createDefaultConnection(), getProjectName());
        }

        // Create dataset via import
        importFolderFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"));

        goToProjectHome();

        _portalHelper.addWebPart("Datasets");
        _portalHelper.addWebPart("Data Classes");
        _portalHelper.addWebPart("Sample Types");
    }

    @Before
    public void goToProjectStart()
    {
        clickProject(getProjectName());
    }

    @Test
    public void testListIndividualTriggers()
    {
        cleanUpListRows();
        var employeeOne = new EmployeeRecord("Emp 1", "1112223333", "Test");
        var employeeTwo = new EmployeeRecord("Emp 2", "2223334444", "Some Other");

        // Insert a row into a list
        insertSingleRowViaUI(employeeOne);
        String testName = INDIVIDUAL_TEST;
        String step = "AfterInsert";

        // Check AfterInsert event
        log("** " + testName + " " + step + " Event");
        waitForText(AFTER_INSERT_ERROR, 1, 1_000);
        clickButton("Cancel");

        // Check BeforeInsert event
        step = "BeforeInsert";
        log("** " + testName + " " + step + " Event");
        insertSingleRowViaUI(employeeTwo);
        assertElementNotPresent("Transaction was committed after error", Locator.tagWithText("td", "Emp 1"));
        waitForElement(Locator.tagWithText("td","Inserting Single"));

        // Check BeforeDelete event
        step = "BeforeDelete";
        log("** " + testName + " " + step + " Event");
        deleteSingleRowViaUI("Company", "Inserting Single", "query", "Confirm Delete", true);
        waitForText(BEFORE_DELETE_ERROR);
        clickButton("Back");

        // Check AfterUpdate event
        step = "AfterUpdate";
        log("** " + testName + " " + step + " Event");
        employeeTwo.company = "Company After Update Error";
        _listHelper.updateRow(1, employeeTwo.toStringMap());
        waitForText(AFTER_UPDATE_ERROR);
        clickButton("Cancel");

        // Check BeforeUpdate event
        step = "BeforeUpdate";
        log("** " + testName + " " + step + " Event");
        employeeTwo.company = "Company Up";
        _listHelper.updateRow(1, employeeTwo.toStringMap());
        waitForText(BEFORE_UPDATE_COMPANY);

        // Check AfterDelete event
        step = "AfterDelete";
        log("** " + testName + " " + step + " Event");
        deleteSingleRowViaUI("Company", BEFORE_UPDATE_COMPANY, "query", "Confirm Delete", true);
        waitForText(AFTER_DELETE_ERROR);
        clickButton("Back");

        // Verify validation error prevented delete
        waitForElement(Locator.tagWithText("td", BEFORE_UPDATE_COMPANY));
    }

    @Test
    public void testListImportTriggers()
    {
        cleanUpListRows();

        EmployeeRecord caughtAfter = new EmployeeRecord("Emp 1", "1112223333", "Test"),
                changedBefore = new EmployeeRecord("Emp 5", "2223334444", "Some Other");

        String testName = IMPORT_TEST;

        // Check AfterInsert event
        String step = "AfterInsert";
        log("** " + testName + " " + step + " Event");
        String tsvData = EmployeeRecord.getTsvHeaders();
        String delimiter = "\t";
        tsvData += caughtAfter.toDelimitedString(delimiter);
        tsvData += changedBefore.toDelimitedString(delimiter);

        _listHelper.goToList(LIST_NAME);
        _listHelper.bulkImportDataExpectingError(tsvData, AFTER_INSERT_ERROR);

        // Check BeforeInsert event
        step = "BeforeInsert";
        log("** " + testName + " " + step + " Event");
        tsvData = EmployeeRecord.getTsvHeaders();
        tsvData += changedBefore.toDelimitedString(delimiter);

        _listHelper.goToList(LIST_NAME);
        _listHelper.bulkImportData(tsvData);

        waitForElement(Locator.tagWithText("td","Importing TSV"));
    }

    @Test
    public void testListMoveTriggers() throws Exception
    {
        cleanUpListRows();

        RowsResponse response = EMPLOYEE_LIST.getTestDataGenerator(getProjectName())
            .addCustomRow(Map.of("name", "Emp 11", "ssn", "123-45-6789", "company", "LK"))
            .insertRows();

        List<EmployeeRecord> records = response.getRows().stream().map(EmployeeRecord::fromMap).toList();

        openServerJavaScriptConsole();

        MoveRowsCommand command = new MoveRowsCommand(SUBFOLDER_PATH, LIST_SCHEMA, LIST_NAME);
        command.setRows(List.of(Map.of("Name", records.getFirst().name)));
        command.execute(createDefaultConnection(), getProjectName());
        waitForConsole("init got triggered with event: move", "complete got triggered with event: move");

        closeServerJavaScriptConsole();
    }

    @Test
    public void testListManagedColumnsTriggers() throws Exception
    {
        cleanUpListRows();
        Connection cn = WebTestHelper.getRemoteApiConnection();

        // Insert: declared a managed column not set by trigger
        // "boomerang" is absent from the payload; trigger has no handler for this name and never sets it
        InsertRowsCommand insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        insCmd.addRow(Map.of("Name", "Unhandled Name", "SSN", "-123", "Company", "Test Co"));
        assertAPIErrorMessage(insCmd, "declared the managed column 'boomerang'", cn);

        // Insert: trigger sets the declared managed column "employeeId"
        insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        insCmd.addRow(Map.of("Name", "Managed Insert", "SSN", "111222334", "Company", "Test Co"));
        RowsResponse resp = insCmd.execute(cn, getProjectName());
        EmployeeRecord inserted = EmployeeRecord.fromMap(resp.getRows().getFirst());
        Assert.assertEquals("Trigger should have set employeeId", "EMP-INS", inserted.employeeId);

        // Insert: structural add error — trigger adds a column not declared as managed
        insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        insCmd.addRow(Map.of("Name", "Managed Struct", "SSN", "111222335", "Company", "Test Co"));
        assertAPIErrorMessage(insCmd, MANAGED_STRUCT_ADD_ERROR, cn);

        // Insert: structural remove error — trigger deletes a column not declared as managed
        insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        insCmd.addRow(Map.of("Name", "Managed Struct Remove", "SSN", "111222336", "Company", "Test Co"));
        assertAPIErrorMessage(insCmd, MANAGED_STRUCT_REMOVE_ERROR, cn);

        // Setup: insert rows for update tests; include "employeeId" in payload so insert validation passes
        insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        insCmd.addRow(Map.of("Name", "Managed Update", "SSN", "111222340", "Company", "Setup Co", "employeeId", "OLD-ID"));
        insCmd.addRow(Map.of("Name", "MC Struct Setup", "SSN", "111222341", "Company", "Setup Co", "employeeId", "OLD-ID-2"));
        insCmd.addRow(Map.of("Name", "MC Struct Remove Setup", "SSN", "111222342", "Company", "Setup Co", "employeeId", "OLD-ID-3"));
        insCmd.execute(cn, getProjectName());

        // Update: trigger sets both declared managed columns "company" and "employeeId"
        UpdateRowsCommand updCmd = new UpdateRowsCommand(LIST_SCHEMA, LIST_NAME);
        updCmd.addRow(Map.of("Name", "Managed Update", "SSN", "111222340-1"));
        resp = updCmd.execute(cn, getProjectName());
        EmployeeRecord updated = EmployeeRecord.fromMap(resp.getRows().getFirst());
        Assert.assertEquals("Trigger should have set company", "Managed Co", updated.company);
        Assert.assertEquals("Trigger should have set employeeId", "EMP-UPD", updated.employeeId);

        // Update via DIB: declared a managed column not set by trigger
        // "boomerang" is absent from the payload; SSN="-123" causes the trigger to skip setting it
        // Name is explicitly provided so the trigger does not accidentally match a named handler
        var row = Map.of("Name", "Managed Update", "SSN", "-123", "Company", "Test Co");
        _listHelper.goToList(LIST_NAME);
        _listHelper.bulkUpdateExpectingError(TestDataUtils.tsvStringFromRowMaps(List.of(row), List.of("Name", "SSN", "Company"), true), "declared the managed column 'boomerang'");

        // Update: structural add error — trigger adds a column not declared as managed
        row = Map.of("Name", "Managed Struct", "SSN", "111222341");
        _listHelper.goToList(LIST_NAME);
        _listHelper.bulkUpdateExpectingError(TestDataUtils.tsvStringFromRowMaps(List.of(row), List.of("Name", "SSN"), true), "attempted to add: 'undeclaredCol'");

        // Update: structural remove error — trigger deletes a column not declared as managed
        row = Map.of("Name", "Managed Struct Remove", "SSN", "111222342");
        _listHelper.goToList(LIST_NAME);
        _listHelper.bulkUpdateExpectingError(TestDataUtils.tsvStringFromRowMaps(List.of(row), List.of("Name", "SSN"), true), "attempted to remove: 'SSN'");
    }

    /** Issue 52098 - ensure trigger scripts have a chance to do custom type conversion with the incoming row */
    @Test
    public void testListAPITriggerTypeConversion() throws Exception
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();

        // Insert a row with a value that can only be handled by the trigger script to make sure it gets a chance
        // to do the conversion. People.js should strip the "RemoveMe" prefix from Age and FavoriteDateTime
        InsertRowsCommand insCmd = new InsertRowsCommand(LIST_SCHEMA, PEOPLE_LIST_NAME);
        insCmd.addRow(Map.of("Name", "Jimbo", "Age", "RemoveMe25", "FavoriteDateTime", "RemoveMe2025-06-11 11:42", "Crazy", "true"));
        RowsResponse insResp = insCmd.execute(cn, getProjectName());
        List<Map<String, Object>> insertedRows = insResp.getRows();
        Assert.assertEquals(1, insertedRows.size());

        Map<String, Object> insertedRow = insertedRows.getFirst();
        Assert.assertEquals("Jimbo", insertedRow.get("Name"));
        Assert.assertEquals(25, insertedRow.get("Age"));
        Assert.assertEquals("2025-06-11 11:42:00.000", insertedRow.get("FavoriteDateTime"));

        // Validate update too
        UpdateRowsCommand upCmd = new UpdateRowsCommand(LIST_SCHEMA, PEOPLE_LIST_NAME);
        insertedRow.put("Age", "RemoveMe26");
        upCmd.addRow(insertedRow);
        RowsResponse upResp = upCmd.execute(cn, getProjectName());
        List<Map<String, Object>> updatedRows = upResp.getRows();
        Assert.assertEquals(1, updatedRows.size());

        Map<String, Object> updatedRow = updatedRows.getFirst();
        Assert.assertEquals(26, updatedRow.get("Age"));
    }

    @Test
    public void testListAPITriggers() throws Exception
    {
        cleanUpListRows();
        String ssn1 = "111111112";
        String ssn2 = "222211111";

        EmployeeRecord row1 = new EmployeeRecord("Emp 1", ssn1, "LK"),
                       row2 = new EmployeeRecord("Emp 6", ssn2, "KL");

        Connection cn = WebTestHelper.getRemoteApiConnection();

        String testName = API_TEST;
        String step = "AfterInsert";
        log("** " + testName + " " + step + " Event");

        // Check After Insert Event
        InsertRowsCommand insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        insCmd.addRow(row1.toMap()); //can add multiple rows to insert many at once
        insCmd.addRow(row2.toMap());
        assertAPIErrorMessage(insCmd, AFTER_INSERT_ERROR, cn);

        // Check Before Insert Event
        step = "BeforeInsert";
        log("** " + testName + " " + step + " Event");
        insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        EmployeeRecord row3 = new EmployeeRecord("Emp 7","123","DeleteMe");

        insCmd.addRow(row2.toMap());
        insCmd.addRow(row3.toMap());
        RowsResponse resp = insCmd.execute(cn, getProjectName());
        row2 = EmployeeRecord.fromMap(resp.getRows().getFirst());
        Assert.assertEquals("API BeforeInsert", row2.company);

        row3 = EmployeeRecord.fromMap(resp.getRows().get(1));

        // Check After Update Event
        step = "AfterUpdate";
        log("** " + testName + " " + step + " Event");
        UpdateRowsCommand updCmd = new UpdateRowsCommand(LIST_SCHEMA, LIST_NAME);
        row2.ssn = ssn1;
        row2.company = "Company After Update Error";
        updCmd.addRow(row2.toMap());
        updCmd.addRow(row3.toMap());
        assertAPIErrorMessage(updCmd, AFTER_UPDATE_ERROR, cn);

        // Check Before Update Event
        step = "BeforeUpdate";
        log("** " + testName + " " + step + " Event");
        updCmd = new UpdateRowsCommand(LIST_SCHEMA, LIST_NAME);
        row2.company = "Company Up";
        updCmd.addRow(row2.toMap());
        updCmd.addRow(row3.toMap());
        resp = updCmd.execute(cn, getProjectName());
        EmployeeRecord updateCo = EmployeeRecord.fromMap(resp.getRows().getFirst());
        Assert.assertEquals(BEFORE_UPDATE_COMPANY, updateCo.company);
        // Check update persisted
        Assert.assertEquals(ssn1, updateCo.ssn);

        // Check After Delete Event
        step = "AfterDelete";
        log("** " + testName + " " + step + " Event");
        DeleteRowsCommand delCmd = new DeleteRowsCommand(LIST_SCHEMA, LIST_NAME);
        delCmd.addRow(row2.toMap());
        assertAPIErrorMessage(delCmd, AFTER_DELETE_ERROR, cn);

        // Check Before Delete Event
        step = "BeforeDelete";
        log("** " + testName + " " + step + " Event");
        delCmd = new DeleteRowsCommand(LIST_SCHEMA, LIST_NAME);
        delCmd.addRow(row3.toMap());
        assertAPIErrorMessage(delCmd, BEFORE_DELETE_ERROR, cn);
    }

    @Test
    public void testDatasetIndividualTriggers()
    {
        GoToDataUI goToDataset = () -> goToDataset(DATASET_NAME);

        doIndividualTriggerTest("Dataset", goToDataset, "ParticipantId", true, "Confirm Delete", true);

        //For some reason these only get logged for datasets...
        checkExpectedErrors(4);
    }

    @Test
    public void testDatasetImportTriggers()
    {
        String flagField = COMMENTS_FIELD; //Field to watch in trigger script
        String testName = IMPORT_TEST;

        String delimiter = "\t";

        Map<String,String> caughtAfter = new HashMap<>();
        String badParticipant = "101";
        Date date1 = new Date();
        caughtAfter.put("ParticipantId", badParticipant);
        caughtAfter.put("date", date1.toString());
        caughtAfter.put("Gender", "f");
        caughtAfter.put(flagField, "AfterInsert");

        Map<String,String> changedBefore = new HashMap<>();
        String key2 = "102";
        Date date2 = new Date();
        changedBefore.put("ParticipantId", key2);
        changedBefore.put("date", date2.toString());
        changedBefore.put("Gender", "f");
        changedBefore.put(flagField, testName);

        // Check AfterInsert event
        String step = "AfterInsert";
        log("** " + testName + " " + step + " Event");
        String importHeaders = joinMapKeys(caughtAfter, delimiter);
        String row1 = joinMapValues(caughtAfter, delimiter);
        String row2 = joinMapValues(changedBefore, delimiter);
        String tsvData = importHeaders + "\n";
        tsvData += row1 + "\n";
        tsvData += row2 + "\n";

        goToDataset(DATASET_NAME);
        new DataRegionTable("Dataset", getDriver()).clickImportBulkData();
        new ImportDataPage(getDriver())
                .setText(tsvData).submitExpectingError("row 1: " + AFTER_INSERT_ERROR);
    }

    @Test
    public void testDatasetAPITriggers() throws Exception
    {
        doAPITriggerTest(STUDY_SCHEMA, DATASET_NAME, "ParticipantId", true);
    }

    @Test
    public void testDataClassIndividualTriggers() throws Exception
    {
        cleanUpTableRows(DATA_CLASSES_SCHEMA, DATA_CLASSES_NAME);

        //Generate delegate to move to data class UI
        GoToDataUI goToDataClass = () -> goTo("Data Classes", DATA_CLASSES_NAME);

        openServerJavaScriptConsole();

        doIndividualTriggerTest("query", goToDataClass, "Name", false, "Yes, Delete", false);

        waitForConsole("init got triggered with event: delete", "complete got triggered with event: delete");
        closeServerJavaScriptConsole();
    }

    @Test
    public void testDataClassAPITriggers() throws Exception
    {
        cleanUpTableRows(DATA_CLASSES_SCHEMA, DATA_CLASSES_NAME);
        doAPITriggerTest(DATA_CLASSES_SCHEMA, DATA_CLASSES_NAME, "Name", false);
    }

    @Test
    public void testSampleTypeIndividualTriggers() throws Exception
    {
        cleanUpTableRows(SAMPLE_TYPE_SCHEMA, SAMPLE_TYPE_NAME);

        //Generate delegate to move to sample type UI
        GoToDataUI goToSampleType = () -> goTo("Sample Types", SAMPLE_TYPE_NAME);

        doIndividualTriggerTest("Material", goToSampleType, "Name", false, "Yes, Delete", false);
    }

    @Test
    public void testSampleTypeAPITriggers() throws Exception
    {
        cleanUpTableRows(SAMPLE_TYPE_SCHEMA, SAMPLE_TYPE_NAME);
        doAPITriggerTest(SAMPLE_TYPE_SCHEMA, SAMPLE_TYPE_NAME, "Name", false);
    }

    @Test
    public void testDataClassManagedColumnsTriggers() throws Exception
    {
        cleanUpTableRows(DATA_CLASSES_SCHEMA, DATA_CLASSES_NAME);
        doManagedColumnsTriggerTest(DATA_CLASSES_SCHEMA, DATA_CLASSES_NAME, "Name", false, "Name");
    }

    @Test
    public void testSampleTypeManagedColumnsTriggers() throws Exception
    {
        cleanUpTableRows(SAMPLE_TYPE_SCHEMA, SAMPLE_TYPE_NAME);
        doManagedColumnsTriggerTest(SAMPLE_TYPE_SCHEMA, SAMPLE_TYPE_NAME, "Name", false, "Name");
    }

    @Test
    public void testDatasetManagedColumnsTriggers() throws Exception
    {
        doManagedColumnsTriggerTest(STUDY_SCHEMA, DATASET_NAME, "ParticipantId", true, COMMENTS_FIELD);
    }

    /**
     * Shared test of managed columns trigger behavior across data types.
     * Verifies that the framework enforces managed column declarations and detects structural violations.
     *
     * @param schemaName          Schema containing the query
     * @param queryName           Query/table name
     * @param keyColumnName       Primary key column ("Name" for DC/ST, "ParticipantId" for Dataset)
     * @param requiresDate        Whether rows require a "Date" field (datasets)
     * @param insertDispatchField Field that routes managed-column behavior in beforeInsert;
     *                            equals keyColumnName for DC/ST, COMMENTS_FIELD for Dataset
     */
    private void doManagedColumnsTriggerTest(String schemaName, String queryName, String keyColumnName,
            boolean requiresDate, String insertDispatchField) throws Exception
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        boolean dispatchIsKey = insertDispatchField.equals(keyColumnName);

        // Insert: declared managed column not set by trigger → error
        InsertRowsCommand insCmd = new InsertRowsCommand(schemaName, queryName);
        insCmd.addRow(makeInsertRow(keyColumnName, requiresDate, dispatchIsKey, insertDispatchField,
                "Managed Unhandled", "MC-UN-001"));
        assertAPIErrorMessage(insCmd, "declared the managed column '" + COUNTRY_FIELD + "'", cn);

        // Insert: trigger sets managed column → success
        insCmd = new InsertRowsCommand(schemaName, queryName);
        insCmd.addRow(makeInsertRow(keyColumnName, requiresDate, dispatchIsKey, insertDispatchField,
                "Managed Insert", "MC-IN-001"));
        RowsResponse resp = insCmd.execute(cn, getProjectName());
        Assert.assertEquals("Trigger should have set " + COUNTRY_FIELD, "MANAGED-INS",
                resp.getRows().getFirst().get(COUNTRY_FIELD));

        // Insert: structural add error — trigger adds undeclared column → error
        insCmd = new InsertRowsCommand(schemaName, queryName);
        insCmd.addRow(makeInsertRow(keyColumnName, requiresDate, dispatchIsKey, insertDispatchField,
                "Managed Struct", "MC-ST-001"));
        assertAPIErrorMessage(insCmd, MANAGED_STRUCT_ADD_ERROR, cn);

        // Insert: structural remove error — trigger deletes column → error
        insCmd = new InsertRowsCommand(schemaName, queryName);
        insCmd.addRow(makeInsertRow(keyColumnName, requiresDate, dispatchIsKey, insertDispatchField,
                "Managed Struct Remove", "MC-SR-001"));
        assertAPIErrorMessage(insCmd, MANAGED_STRUCT_REMOVE_ERROR, cn);

        // Setup: insert rows for update tests with neutral Comments so the trigger fallback fires.
        // Dataset requires a fixed date so the composite key can be matched by the MERGE import below.
        String updKey = dispatchIsKey ? "MC Update Setup" : "MC-UPD-001";
        String strKey = dispatchIsKey ? "MC Struct Setup" : "MC-STR-001";
        String srrKey = dispatchIsKey ? "MC Struct Remove Setup" : "MC-SRR-001";

        InsertRowsCommand setupCmd = new InsertRowsCommand(schemaName, queryName);
        setupCmd.addRow(makeSetupRow(keyColumnName, requiresDate, dispatchIsKey, "MC Update Setup", "MC-UPD-001"));
        setupCmd.addRow(makeSetupRow(keyColumnName, requiresDate, dispatchIsKey, "MC Struct Setup", "MC-STR-001"));
        setupCmd.addRow(makeSetupRow(keyColumnName, requiresDate, dispatchIsKey, "MC Struct Remove Setup", "MC-SRR-001"));
        RowsResponse setupResp = setupCmd.execute(cn, getProjectName());

        // For datasets, derive the Date from the insert response so the MERGE key matches exactly
        // what the server stored rather than relying on a string literal that may round-trip differently.
        String tsvHeader = keyColumnName + (requiresDate ? "\tDate" : "") + "\t" + COMMENTS_FIELD + "\n";
        String updDateVal = requiresDate ? "\t" + setupResp.getRows().get(0).get("Date") : "";
        String strDateVal = requiresDate ? "\t" + setupResp.getRows().get(1).get("Date") : "";
        String srrDateVal = requiresDate ? "\t" + setupResp.getRows().get(2).get("Date") : "";

        // Merge via DIB: trigger sets managed column → success
        importData(cn, schemaName, queryName, tsvHeader + updKey + updDateVal + "\tManaged Merge\n", ImportDataCommand.InsertOption.MERGE);
        SelectRowsResponse selResp = new QueryApiHelper(cn, getProjectName(), schemaName, queryName)
                .selectRows(null, List.of(new Filter(keyColumnName, updKey)));
        Assert.assertEquals("Trigger should have set " + COUNTRY_FIELD, "MANAGED-MERGE",
                selResp.getRows().getFirst().get(COUNTRY_FIELD));

        // Merge via DIB: declared managed column not set by trigger → error
        assertImportError(cn, schemaName, queryName, tsvHeader + updKey + updDateVal + "\tManaged Unhandled\n",
                ImportDataCommand.InsertOption.MERGE, "declared the managed column");

        // Merge via DIB: structural add error — trigger adds undeclared column → error
        assertImportError(cn, schemaName, queryName, tsvHeader + strKey + strDateVal + "\tManaged Struct\n",
                ImportDataCommand.InsertOption.MERGE, "attempted to add: 'undeclaredInsertCol'");

        // Merge via DIB: structural remove error — trigger deletes column → error
        assertImportError(cn, schemaName, queryName, tsvHeader + srrKey + srrDateVal + "\tManaged Struct Remove\n",
                ImportDataCommand.InsertOption.MERGE, "attempted to remove: '" + COMMENTS_FIELD + "'");
    }

    /**
     * Builds an insert row for the managed column trigger test.
     * When dispatchIsKey, the dispatch value goes in the key column (DC/ST); otherwise it goes in
     * the separate dispatch field (Dataset) and a generated unique value is used for the key.
     */
    private Map<String, Object> makeInsertRow(String keyColumnName, boolean requiresDate, boolean dispatchIsKey, String dispatchField, String dispatchValue, String uniqueKey)
    {
        Map<String, Object> row = new HashMap<>();
        if (dispatchIsKey)
        {
            row.put(keyColumnName, dispatchValue);
            row.put(COMMENTS_FIELD, "managed-test");  // ensure Comments exists for struct-remove test
        }
        else
        {
            row.put(keyColumnName, uniqueKey);
            row.put(dispatchField, dispatchValue);
        }
        if (requiresDate)
            row.put("Date", new Date());
        return row;
    }

    /**
     * Builds a neutral setup row for update-phase tests; trigger fallback fires for these rows.
     */
    private Map<String, Object> makeSetupRow(String keyColumnName, boolean requiresDate, boolean dispatchIsKey, String nameValue, String uniqueKey)
    {
        Map<String, Object> row = new HashMap<>();
        row.put(keyColumnName, dispatchIsKey ? nameValue : uniqueKey);
        row.put(COMMENTS_FIELD, "setup");
        if (requiresDate)
            row.put("Date", new Date());
        return row;
    }

    private void cleanUpTableRows(String schemaName, String queryName) throws Exception
    {
        new TruncateTableCommand(schemaName, queryName).execute(createDefaultConnection(), getProjectName());
    }

    /**
     * Run an api test against a schema and query based on preset trigger script
     * @param keyColumnName Name of key column
     * @param requiresDate param to add a date column to inserted items
     */
    private void doAPITriggerTest(String schemaName, String queryName, String keyColumnName, boolean requiresDate) throws Exception
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        String flagField = COMMENTS_FIELD; //Field to watch in trigger script
        String updateField = COUNTRY_FIELD; //field that is set by trigger scripts and Updates

        Map<String,Object> row1 = new HashMap<>();
        String text1 = "123";
        row1.put(keyColumnName, text1);
        if (requiresDate)
            row1.put("Date", new Date());

        row1.put(flagField, "AfterInsert");

        Map<String,Object> row2 = new HashMap<>();
        String text2 = "321";
        row2.put(keyColumnName, text2);
        if (requiresDate)
            row2.put("Date", new Date());

        String testName = API_TEST;
        String step = "AfterInsert";
        log("** " + testName + " " + step + " Event");

        // Check After Insert Event
        InsertRowsCommand insCmd = new InsertRowsCommand(schemaName, queryName);

        insCmd.addRow(row1); //can add multiple rows to insert many at once
        insCmd.addRow(row2);
        assertAPIErrorMessage(insCmd, AFTER_INSERT_ERROR, cn);

        // Check Before Insert Event
        step = "BeforeInsert";
        log("** " + testName + " " + step + " Event");
        insCmd = new InsertRowsCommand(schemaName, queryName);

        Map<String,Object> row3 = new HashMap<>();
        row3.put(keyColumnName, "213");
        if (requiresDate)
            row3.put("Date", new Date());

        row2.put(flagField, testName);

        insCmd.addRow(row2);
        insCmd.addRow(row3);
        RowsResponse resp = insCmd.execute(cn, getProjectName());
        row2 = resp.getRows().getFirst();
        Assert.assertEquals("API BeforeInsert", row2.get(COUNTRY_FIELD));

        SearchAdminAPIHelper.waitForIndexer();

        row3 = resp.getRows().get(1);

        // Check Before Update Event
        step = "BeforeUpdate";
        log("** " + testName + " " + step + " Event");
        UpdateRowsCommand updCmd = new UpdateRowsCommand(schemaName,queryName);
        row2.put(flagField, "BeforeUpdate");
        row2.put(updateField, "Labkey");
        row3.put(flagField, "BeforeDelete");  //For later.
        updCmd.addRow(row2);
        updCmd.addRow(row3);
        resp = updCmd.execute(cn, getProjectName());
        Map<String, Object> updateCo = resp.getRows().getFirst();
        Assert.assertEquals(BEFORE_UPDATE_COMPANY, updateCo.get(updateField));
        // Check update persisted
        Assert.assertEquals("BeforeUpdate", updateCo.get(flagField));

        SearchAdminAPIHelper.waitForIndexer();

        // Check After Update Event
        step = "AfterUpdate";
        log("** " + testName + " " + step + " Event");
        updCmd = new UpdateRowsCommand(schemaName, queryName);
        row2.put(flagField, "AfterUpdate");
        updCmd.addRow(row2);
        updCmd.addRow(row3);
        assertAPIErrorMessage(updCmd, AFTER_UPDATE_ERROR, cn);

        // Check After Delete Event
        step = "After Delete";
        log("** " + testName + " " + step + " Event");
        DeleteRowsCommand delCmd = new DeleteRowsCommand(schemaName, queryName);
        delCmd.addRow(row2);
        assertAPIErrorMessage(delCmd, AFTER_DELETE_ERROR, cn);

        // Check Before Delete Event
        step = "BeforeDelete";
        log("** " + testName + " " + step + " Event");
        delCmd = new DeleteRowsCommand(schemaName, queryName);
        delCmd.addRow(row3);
        assertAPIErrorMessage(delCmd, BEFORE_DELETE_ERROR, cn);
    }

    /**
     * Execute a set of tests against a datatype and preset trigger script
     */
    private void doIndividualTriggerTest(String dataRegionName, GoToDataUI goToData, String keyColumnName, boolean requiresDate, String deleteButtonText, boolean expectPageLoad)
    {
        String flagField = COMMENTS_FIELD; //Field to watch in trigger script
        String testName = INDIVIDUAL_TEST;

        Map<String,String> caughtAfter = new HashMap<>();
        String badParticipant = "101345";
        Date date1 = new Date();
        caughtAfter.put(keyColumnName, badParticipant);
        if (requiresDate)
            caughtAfter.put("date", date1.toString());
        caughtAfter.put(flagField, "AfterInsert");

        Map<String,String> changedBefore = new HashMap<>();
        String key2 = "103506";
        Date date2 = new Date();
        changedBefore.put(keyColumnName, key2);
        if (requiresDate)
            changedBefore.put("date", date2.toString());
        changedBefore.put(flagField, testName);

        // Check AfterInsert event
        String step = "AfterInsert";
        log("** " + testName + " " + step + " Event");
        //  Insert row into List
        goToData.goToDataUIPage();
        insertSingleRowViaUI(dataRegionName, caughtAfter);
        waitForText(AFTER_INSERT_ERROR, 1, 1_000);
        clickButton("Cancel");

        // Check BeforeInsert event
        step = "BeforeInsert";
        log("** " + testName + " " + step + " Event");
        goToData.goToDataUIPage();
        insertSingleRowViaUI(dataRegionName, changedBefore);
        assertElementNotPresent("Transaction was committed after error", Locator.tagWithText("td", badParticipant));
        waitForElement(Locator.tagWithText("td","Inserting Single"));

        // Check BeforeDelete Event
        step = "BeforeDelete";
        log("** " + testName + " " + step + " Event");

        // Check that the previous step prepared row for delete
        pushLocation();
        waitForElement(Locator.tagWithText("td", "BeforeDelete"));
        deleteSingleRowViaUI(flagField, step, dataRegionName, deleteButtonText, expectPageLoad);
        waitForText(1_000, BEFORE_DELETE_ERROR);
        popLocation();
        // Verify validation error prevented delete
        waitForElement(Locator.tagWithText("td", "BeforeDelete"));

        // Check AfterUpdate Event
        step = "AfterUpdate";
        log("** " + testName + " " + step + " Event");
        changedBefore.put(flagField, step);
        updateDataSetRow(1, dataRegionName, changedBefore);
        waitForText(1_000, AFTER_UPDATE_ERROR);
        clickButton("Cancel");

        // Check BeforeUpdate Event
        step = "BeforeUpdate";
        log("** " + testName + " " + step + " Event");
        changedBefore.put(flagField, step);
        updateDataSetRow(1, dataRegionName, changedBefore);
        waitForText(1_000, BEFORE_UPDATE_COMPANY);
        waitForText(1_000, "BeforeUpdate");  // Check change was retained

        // Check AfterDelete Event
        step = "AfterDelete";
        log("** " + testName + " " + step + " Event");
        pushLocation();
        deleteSingleRowViaUI(COUNTRY_FIELD, BEFORE_UPDATE_COMPANY, dataRegionName, deleteButtonText, expectPageLoad);
        waitForText(1_000, AFTER_DELETE_ERROR);
        popLocation();
        // Verify validation error prevented delete
        waitForElement(Locator.tagWithText("td", "BeforeUpdate"));
    }

    /**
     * Verify the error message received from an API call matches the expected error
     * @param cmd command to run
     * @param expected error message to check
     * @param cn connection object to run against
     */
    private void assertAPIErrorMessage(BaseRowsCommand cmd, String expected, Connection cn) throws IOException
    {
        try
        {
            cmd.execute(cn, getProjectName());
            Assert.fail("No error triggered. Expected: " + expected);
        }
        catch (CommandException e)
        {
            Assertions.assertThat(e.getMessage())
                    .as("Trigger script error should contain expected text")
                    .contains(expected);
        }
    }

    /**
     * Assert that an {@link #importData} call throws a {@link CommandException} whose message contains {@code expected}.
     */
    private void assertImportError(Connection cn, String schemaName, String queryName, String text,
            @Nullable ImportDataCommand.InsertOption insertOption, String expected) throws IOException
    {
        try
        {
            importData(cn, schemaName, queryName, text, insertOption);
            Assert.fail("No error triggered. Expected: " + expected);
        }
        catch (CommandException e)
        {
            Assertions.assertThat(e.getMessage())
                    .as("Trigger script error should contain expected text")
                    .contains(expected);
        }
    }

    /**
     * Import TSV text into a schema/query via the Data Iterator pipeline.
     * Pass {@code null} for {@code insertOption} to use the server default (IMPORT/insert).
     */
    private ImportDataResponse importData(Connection cn, String schemaName, String queryName, String text,
                                          @Nullable ImportDataCommand.InsertOption insertOption) throws IOException, CommandException
    {
        ImportDataCommand cmd = new ImportDataCommand(schemaName, queryName);
        cmd.setText(text);
        if (insertOption != null)
            cmd.setInsertOption(insertOption);
        cmd.setTimeout(180_000);
        return cmd.execute(cn, getProjectName());
    }

    /**
     * Insert a single record into list
     */
    private void insertSingleRowViaUI(EmployeeRecord record)
    {
        _listHelper.goToList(LIST_NAME);
        _listHelper.insertNewRow(record.toStringMap());
    }

    /**
     * insert single record into dataset
     */
    private void insertSingleRowViaUI(String dataRegionName, Map<String,String> record)
    {
        DataRegionTable.DataRegion(getDriver()).withName(dataRegionName).find().clickInsertNewRow();
        record.forEach((key, value) -> setFormElement(Locator.xpath("//*[@name='quf_" + key + "']"), value));
        clickButton("Submit");
    }

    /**
     * delete single record via the table UI
     *
     * @param columnName       Column to look at
     * @param columnValue      value to look for
     * @param tableName        DataRegionTable name
     * @param deleteButtonText text that appears in delete confirmation when not in an alert.
     * @param expectPageLoad indicates whether confirming deletion will result in a page load or not
     */
    private void deleteSingleRowViaUI(String columnName, String columnValue, String tableName, String deleteButtonText, boolean expectPageLoad)
    {
        DataRegionTable drt = new DataRegionTable(tableName, this);
        int rowId = drt.getRowIndex(columnName, columnValue);
        drt.checkCheckbox(rowId);
        doAndMaybeWaitForPageToLoad(defaultWaitForPage, () ->
        {
            drt.clickHeaderButton("Delete");
            Alert alert = getAlertIfPresent();
            if (alert != null)
                alert.accept();
            else
                clickButton(deleteButtonText, expectPageLoad ? defaultWaitForPage : 0);
            return expectPageLoad;
        });
    }

    /**
     * Edit a dataset fields base on map
     * @param id dataset entry to edit
     * @param data Field/value map
     */
    public void updateDataSetRow(int id, String tableName, Map<String, String> data)
    {
        DataRegionTable dr = new DataRegionTable(tableName, this);
        this.clickAndWait(dr.updateLink(id - 1));
        data.forEach((key, value) -> setFormElement(Locator.xpath("//*[@name='quf_" + key + "']"), value));
        clickButton("Submit");
    }

    /**
     * Delete list rows added by test
     */
    private void cleanUpListRows()
    {
        var listsPage = goToManageLists();
        var grid = listsPage.getGrid();
        grid.uncheckAllOnPage();
        grid.selectLists(List.of(LIST_NAME));
        grid.deleteAllDataFromSelectedLists();
    }

    /**
     * Navigate to particular Dataset
     */
    private void goToDataset(String datasetName)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(datasetName));
    }

    /**
     * Navigate to particular dataclass/sampletype in the given webpart
     */
    private void goTo(String webPartName, String tableName)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(webPartName));
        clickAndWait(Locator.linkWithText(tableName));
    }

    /**
     * Generate delimited string of keys from a map
     */
    private String joinMapValues(Map<String,String> data, String delimiter)
    {
        StringBuilder sb = new StringBuilder();
        data.values().forEach(val -> sb.append(val).append(delimiter));
        return sb.toString().trim();
    }

    /**
     * Generate delimited string of keys from a map
     */
    private String joinMapKeys(Map<String,String> data, String delimiter)
    {
        StringBuilder sb = new StringBuilder();
        data.keySet().forEach(val -> sb.append(val).append(delimiter));
        return sb.toString().trim();
    }

    private void closeServerJavaScriptConsole()
    {
        switchToWindow(1);
        getDriver().close();
        switchToMainWindow();
    }

    private void openServerJavaScriptConsole()
    {
        // Go to the log view to start capturing messages
        new SiteNavBar(getDriver()).clickAdminMenuItem(false, "Developer Links", "Server JavaScript Console");
        switchToWindow(1);
        waitForText("Message");
        switchToMainWindow();
    }

    private void waitForConsole(String... text)
    {
        switchToWindow(1);
        waitForText(text);
        switchToMainWindow();
    }
}

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
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.BaseRowsCommand;
import org.labkey.remoteapi.query.MoveRowsCommand;
import org.labkey.remoteapi.query.RowsResponse;
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
 * Test trigger script matrix, expands on ScriptValidationTest which covers custom schemas (Vehicles)
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
        public String name, ssn, company;
        public Integer key;

        public EmployeeRecord(String name, String ssn, String company)
        {
            this(name, ssn, company, null);
        }

        public EmployeeRecord(String name, String ssn, String company, Integer key)
        {
            this.name = name;
            this.ssn = ssn;
            this.company = company;
            this.key = key;
        }

        public Map<String, Object> toMap()
        {
            return Maps.of("Name", name, "SSN", ssn, "Company", company, "Key", key);
        }

        public Map<String, String> toStringMap()
        {
            return Maps.of("name", name, "ssn", ssn, "company", company);
        }

        public String toDelimitedString(String delimiter)
        {
            return name + delimiter + ssn + delimiter + company + "\n";
        }

        public static EmployeeRecord fromMap(Map<String, Object> map)
        {
            EmployeeRecord newbie = new EmployeeRecord((String)map.get("Name"), (String)map.get("ssn"), (String)map.get("Company"));
            if (map.containsKey("Key"))
                newbie.key = (Integer)map.get("Key");

            return newbie;
        }

        public static String getTsvHeaders()
        {
            return "Name\tSSN\tCompany\n";
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
        _containerHelper.enableModule(getProjectName(), "Query");
        _containerHelper.enableModule(getProjectName(), SIMPLE_MODULE);
        _containerHelper.enableModule(getProjectName(), TRIGGER_MODULE);

        // Create lists
        {
            List<FieldDefinition> fields = List.of(
                new FieldDefinition("name", ColumnType.String).setLabel("Name"),
                new FieldDefinition("ssn", ColumnType.String).setLabel("SSN"),
                new FieldDefinition("company", ColumnType.String).setLabel("Company")
            );

            EMPLOYEE_LIST = new IntListDefinition(LIST_NAME, "Key").setFields(fields);
            EMPLOYEE_LIST.create(createDefaultConnection(), getProjectName());

            fields = List.of(
                new FieldDefinition("Name", ColumnType.String).setDescription("Name"),
                new FieldDefinition("Age", ColumnType.Integer).setDescription("Age"),
                new FieldDefinition("FavoriteDateTime", ColumnType.DateAndTime).setDescription("Favorite date time. Who doesn't have one?"),
                new FieldDefinition("Crazy", ColumnType.Boolean).setDescription("Crazy?")
            );

            new IntListDefinition(PEOPLE_LIST_NAME, "Key")
                    .setFields(fields)
                    .create(createDefaultConnection(), getProjectName());
        }

        importFolderFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"));

        //Add webparts for dataset, data class, sample type setup
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
        EmployeeRecord caughtAfter = new EmployeeRecord("Emp 1", "1112223333", "Test"),
                changedBefore = new EmployeeRecord("Emp 2", "2223334444", "Some Other");

        //Insert row into List
        insertSingleRowViaUI(caughtAfter);
        String testName = INDIVIDUAL_TEST;
        String step = "AfterInsert";

        //Check AfterInsert event
        log("** " + testName + " " + step + " Event");
        waitForText(AFTER_INSERT_ERROR, 1, 1_000);
        clickButton("Cancel");

        //Check BeforeInsert event
        step = "BeforeInsert";
        log("** " + testName + " " + step + " Event");
        insertSingleRowViaUI(changedBefore);
        assertElementNotPresent("Transaction was committed after error", Locator.tagWithText("td", "Emp 1"));
        waitForElement(Locator.tagWithText("td","Inserting Single"));

        //Check BeforeDelete Event
        step = "BeforeDelete";
        log("** " + testName + " " + step + " Event");
        deleteSingleRowViaUI("Company", "Inserting Single", "query", "Confirm Delete", true);
        waitForText(BEFORE_DELETE_ERROR);
        clickButton("Back");

        //Check AfterUpdate Event
        step = "AfterUpdate";
        log("** " + testName + " " + step + " Event");
        new DataRegionTable("query", getDriver()).clickEditRow(0);
        clickButton("Submit");
        waitForText(AFTER_UPDATE_ERROR);
        clickButton("Cancel");

        //Check BeforeUpdate Event
        step = "BeforeUpdate";
        log("** " + testName + " " + step + " Event");
        changedBefore.name = "Emp 3";
        _listHelper.updateRow(1, changedBefore.toStringMap());
        waitForText(BEFORE_UPDATE_COMPANY);

        //Check AfterDelete Event
        step = "AfterDelete";
        log("** " + testName + " " + step + " Event");
        deleteSingleRowViaUI("Company", BEFORE_UPDATE_COMPANY, "query", "Confirm Delete", true);
        waitForText(AFTER_DELETE_ERROR);
        clickButton("Back");
        //Verify validation error prevented delete
        waitForElement(Locator.tagWithText("td", "Emp 3"));
    }

    @Test
    public void testListImportTriggers()
    {
        cleanUpListRows();
        goToManagedList(LIST_NAME);
        _listHelper.clickImportData();

        EmployeeRecord caughtAfter = new EmployeeRecord("Emp 1", "1112223333", "Test"),
                changedBefore = new EmployeeRecord("Emp 5", "2223334444", "Some Other");

        String testName = IMPORT_TEST;
        String step = "AfterInsert";

        //Check AfterInsert event
        log("** " + testName + " " + step + " Event");
        String tsvData = EmployeeRecord.getTsvHeaders();
        String delimiter = "\t";
        tsvData += caughtAfter.toDelimitedString(delimiter);
        tsvData += changedBefore.toDelimitedString(delimiter);

        ImportDataPage importDataPage = new ImportDataPage(getDriver());
        importDataPage.setText(tsvData);
        importDataPage.submitExpectingErrorContaining(AFTER_INSERT_ERROR);

        //Check BeforeInsert event
        step = "BeforeInsert";
        log("** " + testName + " " + step + " Event");
        tsvData = EmployeeRecord.getTsvHeaders();
        tsvData += changedBefore.toDelimitedString(delimiter);
        importDataPage.setText(tsvData);
        importDataPage.submit();

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
        command.setRows(List.of(Map.of("Key", records.get(0).key)));
        command.execute(createDefaultConnection(), getProjectName());
        waitForConsole("init got triggered with event: move", "complete got triggered with event: move");

        closeServerJavaScriptConsole();
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

        Map<String, Object> insertedRow = insertedRows.get(0);
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

        Map<String, Object> updatedRow = updatedRows.get(0);
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

        //Check After Insert Event
        InsertRowsCommand insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        RowsResponse resp;

        insCmd.addRow(row1.toMap()); //can add multiple rows to insert many at once
        insCmd.addRow(row2.toMap());
        assertAPIErrorMessage(insCmd, AFTER_INSERT_ERROR, cn);

        //Check Before Insert Event
        step = "BeforeInsert";
        log("** " + testName + " " + step + " Event");
        insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        EmployeeRecord row3 = new EmployeeRecord("Emp 7","123","DeleteMe");

        insCmd.addRow(row2.toMap());
        insCmd.addRow(row3.toMap());
        resp = insCmd.execute(cn, getProjectName());
        row2 = EmployeeRecord.fromMap(resp.getRows().get(0));
        Assert.assertEquals("API BeforeInsert", row2.company);

        row3 = EmployeeRecord.fromMap(resp.getRows().get(1));

        //Check After Update Event
        step = "AfterUpdate";
        log("** " + testName + " " + step + " Event");
        UpdateRowsCommand updCmd = new UpdateRowsCommand(LIST_SCHEMA, LIST_NAME);
        row2.ssn = ssn1;
        updCmd.addRow(row2.toMap());
        updCmd.addRow(row3.toMap());
        assertAPIErrorMessage(updCmd, AFTER_UPDATE_ERROR, cn);

        //Check Before Update Event
        step = "BeforeUpdate";
        log("** " + testName + " " + step + " Event");
        updCmd = new UpdateRowsCommand(LIST_SCHEMA,LIST_NAME);
        row2.name = "Emp 8";
        updCmd.addRow(row2.toMap());
        updCmd.addRow(row3.toMap());
        resp = updCmd.execute(cn, getProjectName());
        EmployeeRecord updateCo = EmployeeRecord.fromMap(resp.getRows().get(0));
        Assert.assertEquals(BEFORE_UPDATE_COMPANY, updateCo.company);
        //Check update persisted
        Assert.assertEquals(ssn1, updateCo.ssn);

        //Check After Delete Event
        step = "AfterDelete";
        log("** " + testName + " " + step + " Event");
        DeleteRowsCommand delCmd = new DeleteRowsCommand(LIST_SCHEMA, LIST_NAME);
        delCmd.addRow(row2.toMap());
        assertAPIErrorMessage(delCmd, AFTER_DELETE_ERROR, cn);

        //Check Before Delete Event
        step = "BeforeDelete";
        log("** " + testName + " " + step + " Event");
        delCmd = new DeleteRowsCommand(LIST_SCHEMA, LIST_NAME);
        delCmd.addRow(row3.toMap());
        assertAPIErrorMessage(delCmd, BEFORE_DELETE_ERROR, cn);
    }

    /********************************
     * Dataset Trigger Script Tests
     ********************************/

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

        //Check AfterInsert event
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

    /********************************
     * Data Class Trigger Script Tests
     ********************************/

    @Test
    public void testDataClassIndividualTriggers() throws Exception
    {
        //Generate delegate to move to data class UI
        GoToDataUI goToDataClass = () -> goTo("Data Classes", DATA_CLASSES_NAME);

        setupDataClass();
        openServerJavaScriptConsole();

        doIndividualTriggerTest("query", goToDataClass, "Name", false, "Yes, Delete", false);

        waitForConsole("init got triggered with event: delete",
                "exp.data: this is from the shared function",
                "complete got triggered with event: delete");
        closeServerJavaScriptConsole();
    }


    @Test
    public void testDataClassAPITriggers() throws Exception
    {
        setupDataClass();
        doAPITriggerTest(DATA_CLASSES_SCHEMA, DATA_CLASSES_NAME, "Name", false);
    }

    /********************************
     * Sample Type Trigger Script Tests
     ********************************/

    @Test
    public void testSampleTypeIndividualTriggers() throws Exception
    {
        //Generate delegate to move to sample type UI
        GoToDataUI goToSampleType = () -> goTo("Sample Types", SAMPLE_TYPE_NAME);

        setupSampleType();
        doIndividualTriggerTest("Material", goToSampleType, "Name", false, "Yes, Delete", false);
    }


    @Test
    public void testSampleTypeAPITriggers() throws Exception
    {
        setupSampleType();
        doAPITriggerTest(SAMPLE_TYPE_SCHEMA, SAMPLE_TYPE_NAME, "Name", false);
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

        //Check After Insert Event
        InsertRowsCommand insCmd = new InsertRowsCommand(schemaName, queryName);

        insCmd.addRow(row1); //can add multiple rows to insert many at once
        insCmd.addRow(row2);
        assertAPIErrorMessage(insCmd, AFTER_INSERT_ERROR, cn);

        //Check Before Insert Event
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
        row2 = resp.getRows().get(0);
        Assert.assertEquals("API BeforeInsert", row2.get(COUNTRY_FIELD));

        SearchAdminAPIHelper.waitForIndexer();

        row3 = resp.getRows().get(1);

        //Check Before Update Event
        step = "BeforeUpdate";
        log("** " + testName + " " + step + " Event");
        UpdateRowsCommand updCmd = new UpdateRowsCommand(schemaName,queryName);
        row2.put(flagField, "BeforeUpdate");
        row2.put(updateField, "Labkey");
        row3.put(flagField, "BeforeDelete");  //For later.
        updCmd.addRow(row2);
        updCmd.addRow(row3);
        resp = updCmd.execute(cn, getProjectName());
        Map<String, Object> updateCo = resp.getRows().get(0);
        Assert.assertEquals(BEFORE_UPDATE_COMPANY, updateCo.get(updateField));
        //Check update persisted
        Assert.assertEquals("BeforeUpdate", updateCo.get(flagField));

        SearchAdminAPIHelper.waitForIndexer();

        //Check After Update Event
        step = "AfterUpdate";
        log("** " + testName + " " + step + " Event");
        updCmd = new UpdateRowsCommand(schemaName, queryName);
        row2.put(flagField, "AfterUpdate");
        updCmd.addRow(row2);
        updCmd.addRow(row3);
        assertAPIErrorMessage(updCmd, AFTER_UPDATE_ERROR, cn);

        //Check After Delete Event
        step = "After Delete";
        log("** " + testName + " " + step + " Event");
        DeleteRowsCommand delCmd = new DeleteRowsCommand(schemaName, queryName);
        delCmd.addRow(row2);
        assertAPIErrorMessage(delCmd, AFTER_DELETE_ERROR, cn);

        //Check Before Delete Event
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
        String updateField = COUNTRY_FIELD; //Field updated by trigger script
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

        //Check AfterInsert event
        String step = "AfterInsert";
        log("** " + testName + " " + step + " Event");
        //  Insert row into List
        goToData.goToDataUIPage();
        insertSingleRowViaUI(dataRegionName, caughtAfter);
        waitForText(AFTER_INSERT_ERROR, 1, 1_000);
        clickButton("Cancel");

        //Check BeforeInsert event
        step = "BeforeInsert";
        log("** " + testName + " " + step + " Event");
        goToData.goToDataUIPage();
        insertSingleRowViaUI(dataRegionName, changedBefore);
        assertElementNotPresent("Transaction was committed after error", Locator.tagWithText("td", badParticipant));
        waitForElement(Locator.tagWithText("td","Inserting Single"));

        //Check BeforeDelete Event
        step = "BeforeDelete";
        log("** " + testName + " " + step + " Event");

        //Check previous step prepared row for delete
        pushLocation();
        waitForElement(Locator.tagWithText("td", "BeforeDelete"));
        deleteSingleRowViaUI(flagField, step, dataRegionName, deleteButtonText, expectPageLoad);
        waitForText(1_000, BEFORE_DELETE_ERROR);
        popLocation();
        //Verify validation error prevented delete
        waitForElement(Locator.tagWithText("td", "BeforeDelete"));

        //Check AfterUpdate Event
        step = "AfterUpdate";
        log("** " + testName + " " + step + " Event");
        changedBefore.put(flagField, step);
        updateDataSetRow(1, dataRegionName, changedBefore);
        waitForText(1_000, AFTER_UPDATE_ERROR);
        clickButton("Cancel");

        //Check BeforeUpdate Event
        step = "BeforeUpdate";
        log("** " + testName + " " + step + " Event");
        changedBefore.put(flagField, step);
        updateDataSetRow(1, dataRegionName, changedBefore);
        waitForText(1_000, BEFORE_UPDATE_COMPANY);
        waitForText(1_000, "BeforeUpdate");  //Check change was retained

        //Check AfterDelete Event
        step = "AfterDelete";
        log("** " + testName + " " + step + " Event");
        pushLocation();
        deleteSingleRowViaUI(updateField, BEFORE_UPDATE_COMPANY, dataRegionName, deleteButtonText, expectPageLoad);
        waitForText(1_000, AFTER_DELETE_ERROR);
        popLocation();
        //Verify validation error prevented delete
        waitForElement(Locator.tagWithText("td", "BeforeUpdate"));
    }

    /**
     * Verify error message received from api call matches the expected error
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
     * Insert a single record into list
     */
    private void insertSingleRowViaUI(EmployeeRecord record)
    {
        goToManagedList(LIST_NAME);
        _listHelper.insertNewRow(record.toStringMap());
    }

    /**
     * insert single record into dataset
     */
    private void insertSingleRowViaUI(String dataRegionName, Map<String,String> record)
    {
        DataRegionTable.DataRegion(getDriver()).withName(dataRegionName).find().clickInsertNewRow();
        record.entrySet().forEach((entry) -> setFormElement( Locator.xpath("//*[@name='quf_"+ entry.getKey() + "']"), entry.getValue()));
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
        data.entrySet().forEach((entry) -> setFormElement( Locator.xpath("//*[@name='quf_"+ entry.getKey() + "']"), entry.getValue()));
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
     * Navigate to a particular test
     */
    private void goToManagedList(String listName)
    {
        goToManageLists();
        clickAndWait(Locator.linkWithText(listName));
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

    /**
     * Setup the data class
     */
    private void setupDataClass() throws CommandException, IOException
    {
        //Setup Data Class
        goToProjectHome();
        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, "Data Classes");
        int rowId = drt.getRowIndex("Name", DATA_CLASSES_NAME);
        if (rowId >= 0)
        {
            drt.checkCheckbox(rowId);
            drt.clickHeaderButtonAndWait("Delete");
            clickButton("Confirm Delete");
        }

        DataClassDefinition dataClass = new DataClassDefinition(DATA_CLASSES_NAME)
                .setFields(List.of(
                        new FieldDefinition(COMMENTS_FIELD, ColumnType.String),
                        new FieldDefinition(COUNTRY_FIELD, ColumnType.String)));
        dataClass.create(createDefaultConnection(), getProjectName());
    }

    /**
     * Setup the sample type
     */
    private void setupSampleType()
    {
        SampleTypeDefinition sampleType = new SampleTypeDefinition(SAMPLE_TYPE_NAME)
                .setFields(List.of(
                        new FieldDefinition(COMMENTS_FIELD, ColumnType.String),
                        new FieldDefinition(COUNTRY_FIELD, ColumnType.String)));
        SampleTypeAPIHelper.createEmptySampleType(getProjectName(), sampleType);
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

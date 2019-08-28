package org.labkey.test.tests;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.GetDomainCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.DomainFieldRow;
import org.labkey.test.components.DomainFormPanel;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
public class DomainDesignerTest extends BaseWebDriverTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        DomainDesignerTest init = (DomainDesignerTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(getDriver()).addBodyWebPart("Sample Sets");
        new PortalHelper(getDriver()).addBodyWebPart("Lists");
    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
    }

    @Test
    public void testListNumericFormatting() throws Exception
    {
        String listName = "NumericFieldsList";
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);

        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)     // just make the list
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("Number", FieldDefinition.ColumnType.Integer)
                ));
        dgen.createDomain(createDefaultConnection(true), "IntList", Map.of("keyName", "id"));

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);

        DomainFormPanel panel = domainDesignerPage.fieldProperties();
        DomainFieldRow integerRow = panel
                .addField("integerField")
                .setType("Integer")
                .setNumberFormat("###,###,###")
                .setScaleType(PropertiesEditor.ScaleType.LINEAR)
                .setDescription("field for an Integer")
                .setLabel("IntegerFieldLabel");

        DomainFieldRow decimalRow = panel
                .addField("decimalField")
                .setType("Decimal")
                .setNumberFormat("###,###,###.000")
                .setScaleType(PropertiesEditor.ScaleType.LOG)
                .setDescription("field for a decimal")
                .setLabel("DecimalField");
        domainDesignerPage.clickSaveAndFinish();

        dgen = new TestDataGenerator(lookupInfo)        // now put some test data in the new fields
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("Key", FieldDefinition.ColumnType.Integer),
                        TestDataGenerator.simpleFieldDef("integerField", FieldDefinition.ColumnType.Integer)
                ));
        dgen.addCustomRow(Map.of("Number", 1, "integerField", 10000000, "decimalField", 6.022));
        dgen.addCustomRow(Map.of("Number", 2, "integerField", 8675309, "decimalField", 3.1415926));
        dgen.addCustomRow(Map.of("Number", 3, "integerField", 123456789, "decimalField", 12345.678));
        dgen.addCustomRow(Map.of("Number", 4, "integerField", 98765432, "decimalField", 1234.56789));
        dgen.addCustomRow(Map.of("Number", 5, "integerField", 4, "decimalField", 5.654));
        SaveRowsResponse response = dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        DataRegionTable listQuery = DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        listQuery.setSort("Number", SortDirection.ASC);

        Map row1 = listQuery.getRowDataAsMap("Number", "1");
        assertEquals("10,000,000", row1.get("integerField"));
        assertEquals("6.022", row1.get("decimalField"));

        List<String> intColData = listQuery.getColumnDataAsText("integerField");
        List<String> expectedIntColData = Arrays.asList("10,000,000", "8,675,309", "123,456,789", "98,765,432", "4");
        assertEquals("expect formatted integer numbers", expectedIntColData, intColData);

        List<String> decimalColData = listQuery.getColumnDataAsText("decimalField");
        List<String> expectedDecimalColData = Arrays.asList("6.022", "3.142", "12,345.678", "1,234.568", "5.654");
        assertEquals("expect formatted decimal numbers", expectedDecimalColData, decimalColData);
    }

    @Test
    public void testSampleStringFields() throws Exception
    {
        String sampleSet = "StringSampleSet";
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);

        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
            .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("stringField", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("multilineField", FieldDefinition.ColumnType.MultiLine)
                ));
        dgen.createDomain(createDefaultConnection(true), "SampleSet");

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);

        domainFormPanel.getField("stringField")
                .expand()
                .setDescription("basic string field")
                .setLabel("StringField")
                .setCharCount(200);
        domainFormPanel.getField("multilineField")
                .expand()
                .setDescription("basic multiline field")
                .setLabel("MultiLineField")
                .allowMaxChar();
        domainDesignerPage.clickSaveAndFinish();

        dgen.addCustomRow(Map.of("name", "first", "stringField", "baaaaaasic string heeeeeeeeeeere", "multiLineField", "multi\nline\nfield"));
        dgen.addCustomRow(Map.of("name", "second", "stringField", "basic string heeeeere", "multiLineField", "multi\nline\nfield with extra"));
        dgen.addCustomRow(Map.of("name", "third", "stringField", "basic string here", "multiLineField", "multi\nline\nfield and so much more"));
        dgen.addCustomRow(Map.of("name", "fourth", "stringField", "basic string here", "multiLineField", "multi\nline\nfield this is silly"));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        goToProjectHome();
        clickAndWait(Locator.linkWithText(sampleSet));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        Map<String, String> firstMap = samplesTable.getRowDataAsMap("Name", "first");
        assertEquals("multi\nline\nfield", firstMap.get("multiLinefield"));
        assertEquals("baaaaaasic string heeeeeeeeeeere", firstMap.get("stringField"));
    }

    @Test
    public void testDeleteDomainField() throws Exception
    {
        String sampleSet = "deleteColumnSampleSet";
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);

        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("deleteMe", FieldDefinition.ColumnType.String)
                ));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");
        List<Map<String, Object>> createdFields = createResponse.getColumns();
        assertTrue(createdFields.get(0).get("name").equals("deleteMe"));

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);

        domainFormPanel.getField("deleteMe")
                .clickRemoveField()
                .dismiss("Yes");
        domainDesignerPage.clickSaveAndFinish();

        GetDomainCommand domainCommand = new GetDomainCommand("exp.materials", sampleSet);
        DomainResponse afterResponse = domainCommand.execute(createDefaultConnection(true), getProjectName());
        List<Map<String, Object>> remainingFields = afterResponse.getColumns();
        assertEquals("expect only field in the domain to have been deleted", 0, remainingFields.size());

        // double-check to ensure the column has been deleted
        SelectRowsResponse rowsResponse = dgen.getRowsFromServer(createDefaultConnection(true));
        List<String> columnsAfterDelete = rowsResponse.getColumnModel().stream().map(col -> (String) col.get("dataIndex")).collect(Collectors.toList());
        Assert.assertThat("Columns after delete", columnsAfterDelete, CoreMatchers.allOf(CoreMatchers.hasItem("Name"), CoreMatchers.not(CoreMatchers.hasItem("deleteMe"))));

        // this column should no longer exist
        List<Map<String, Object>> deleteMe = rowsResponse.getColumnModel().stream().filter(a -> a.get("dataIndex").equals("deleteMe")).collect(Collectors.toList());
        assertEquals(0, deleteMe.size());
        // make sure the name field is still there
        List<Map<String, Object>> name = rowsResponse.getColumnModel().stream().filter(a -> a.get("dataIndex").equals("Name")).collect(Collectors.toList());
        assertEquals(1, name.size());
    }

    @Test
    public void testAddDomainField() throws Exception
    {
        String sampleSet = "addColumnSampleSet";
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);

        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);

        domainFormPanel.addField("addedField")
                .setType("Date Time")
                .expand()
                .setDateFormat("yyyy-MM-dd HH:mm")
                .setDateShift(false)
                .setDescription("simplest date format of all")
                .setLabel("DateTime");
        domainDesignerPage.clickSaveAndFinish();

        // insert sample dates, confirm expected formats

        dgen.addCustomRow(Map.of("name", "jeff", "addedField", "05-15-2007 11:25"));
        dgen.addCustomRow(Map.of("name", "billy", "addedField", "05-15-2017 23:25"));
        dgen.addCustomRow(Map.of("name", "william", "addedField", "07-25-2007 11:25"));
        dgen.addCustomRow(Map.of("name", "charlie", "addedField", "11-15-1997 13:25"));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        goToProjectHome();
        clickAndWait(Locator.linkWithText(sampleSet));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        Map<String, String> charlieMap = samplesTable.getRowDataAsMap("Name", "charlie");
        assertEquals("1997-11-15 13:25", charlieMap.get("addedField"));
        Map<String, String> williamMap = samplesTable.getRowDataAsMap("Name", "william");
        assertEquals("2007-07-25 11:25", williamMap.get("addedField"));
        Map<String, String> billyMap = samplesTable.getRowDataAsMap("Name", "billy");
        assertEquals("2017-05-15 23:25", billyMap.get("addedField"));
        Map<String, String> jeffMap = samplesTable.getRowDataAsMap("Name", "jeff");
        assertEquals("2007-05-15 11:25", jeffMap.get("addedField"));
    }

    @Test
    public void testBlankNameFieldOnAddedField() throws Exception
    {
        String sampleSet = "errorColumnSampleSet";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("firstCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);

        // add a new field, but leave the name field blank
        DomainFieldRow noNameRow = domainFormPanel.addField("");
        domainDesignerPage.clickSave();

        domainDesignerPage.waitForError();
        assertTrue ("field should report error if saved without a name", noNameRow.hasFieldError());
        assertEquals("New field. Error: Please provide a name for each field.", noNameRow.detailsMessage());
        WebElement errorDiv = domainDesignerPage.errorAlert();
        assertNotNull(errorDiv);
        String hasNoNameError = domainDesignerPage.waitForError();
        assertTrue("expect error to contain [Please provide a name for each field.] but was[" +hasNoNameError+ "]",
                hasNoNameError.contains("Please provide a name for each field."));

        // now give the field a name with characters that will get a warning
        noNameRow.setName("&foolishness!");
        String warning = domainDesignerPage.waitForWarning();
        String warningfieldMessage = noNameRow.detailsMessage();
        String expectedWarning = "SQL queries, R scripts, and other code are easiest to write when field names only contain combination of letters, numbers, and underscores, and start with a letter or underscore";
        assertTrue("expect error to contain ["+expectedWarning+"] but was[" +warning+ "]",
                warning.contains(expectedWarning));
        assertTrue("expect field-level warning to contain ["+expectedWarning+"] but was[" +warningfieldMessage+ "]",
                warning.contains(expectedWarning));
    }

    @Test
    public void testDuplicateFieldName() throws Exception
    {
        String sampleSet = "errorDuplicateFieldSampleset";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("firstCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);

        // add a new field, but leave the name field blank
        DomainFieldRow firstcol = domainFormPanel.getField("firstCol");
        DomainFieldRow dupeNameRow = domainFormPanel.addField("firstCol");
        domainDesignerPage.clickSave();

        // confirm the page shows the summary error
        String dupeError = domainDesignerPage.waitForError();
        String expectedError = "The field name 'firstCol' is already taken. Please provide a unique name for each field.";
        assertTrue("expect field-level warning to contain ["+expectedError+"] but was[" +dupeError+ "]",
                dupeError.contains(expectedError));

        // ensure the duplicate fields show as error fields, with explanations
        assertTrue("expect duplicate field name rows to show errors", firstcol.hasFieldError());
        String firstColErrorStatus = firstcol.detailsMessage();
        assertTrue("expect field-level warning to contain ["+expectedError+"] but was[" +firstColErrorStatus+ "]",
                firstColErrorStatus.contains(expectedError));
        assertTrue("expect duplicate field name to show as error", dupeNameRow.hasFieldError());
        String dupeColErrorStatus = dupeNameRow.detailsMessage();
        assertTrue("expect field-level warning to contain ["+expectedError+"] but was[" +dupeColErrorStatus+ "]",
                dupeColErrorStatus.contains(expectedError));
    }

    @Test
    public void testUserCannotEditListKeyFields() throws Exception
    {
        String list = "testUserCannotEditKeyFieldsList";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", list);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("firstCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "IntList", Map.of("keyName", "id"));
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", list);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(list);

        // confirm the field name/data type edits are disabled
        DomainFieldRow keyRow = domainFormPanel.getField("id");
        assertFalse("expect field name edit to be disabled", keyRow.nameInput().getComponentElement().isEnabled());
        assertNotNull(keyRow.nameInput().getComponentElement().getAttribute("disabled"));
        assertFalse("expect field type select to be disabled", keyRow.typeInput().isEnabled());
        assertNotNull(keyRow.typeInput().getAttribute("disabled"));
    }

    @Test
    public void testDeleteFieldInListWithData() throws Exception
    {
        String list = "testDeleteFieldList";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", list);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("color", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "IntList", Map.of("keyName", "id"));
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        dgen.addCustomRow(Map.of("name", "first", "color", "orange"));
        dgen.addCustomRow(Map.of("name", "second", "color", "green"));
        dgen.addCustomRow(Map.of("name", "third", "color", "blue"));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", list);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(list);

        DomainFieldRow nameRow = domainFormPanel.getField("name");
        // first, delete 'name' column
        nameRow.clickRemoveField()
                .dismiss("Yes");
        domainDesignerPage.clickSave();
        String saveMsg = domainDesignerPage.waitForInfo();
        String expectedMsg = "Save Successful";
        assertTrue("expect field-level warning to contain ["+expectedMsg+"] but was[" +saveMsg+ "]",
                saveMsg.contains(expectedMsg));

        DomainFieldRow colorRow = domainFormPanel.getField("color");
        colorRow.clickRemoveField()
                .dismiss("Yes");
        domainDesignerPage.clickSave();
        domainDesignerPage.waitForInfo();

        // there should just be the key field (id) now:
        domainFormPanel = domainDesignerPage.fieldProperties(list); // re-find the panel to work around field caching silliness
        assertNotNull(domainFormPanel.getField("id"));
        assertNull(domainFormPanel.getField("color"));
        assertNull(domainFormPanel.getField("name"));
    }

    @Test
    public void testDeleteRequiredField() throws Exception
    {
        String list = "testDeleteRequiredFieldList";

        // create the list
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", list);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String)
                            .setRequired(true),                                                                 // <-- marked 'required'
                        TestDataGenerator.simpleFieldDef("color", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "IntList", Map.of("keyName", "id"));
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        dgen.addCustomRow(Map.of("name", "first", "color", "orange"));
        dgen.addCustomRow(Map.of("name", "second", "color", "green"));
        dgen.addCustomRow(Map.of("name", "third", "color", "blue"));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", list);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(list);

        DomainFieldRow nameRow = domainFormPanel.getField("name");
        // confirm the UI shows its expected 'required' status
        assertEquals (true, nameRow.getRequiredField());

        nameRow.clickRemoveField()
                .dismiss("Yes");
        domainDesignerPage.clickSave();
        domainDesignerPage.waitForInfo();       // expect the 'save successful' sprite to appear
    }

    /**
     * confirms that the key field (called 'name') in a sampleset is not shown in the domain editor
     * @throws Exception
     */
    @Test
    public void testConfirmNameFieldFromSamplesetNotShown() throws Exception
    {
        String sampleSet = "hiddenNameFieldSampleset";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("firstCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);

        DomainFieldRow nameField = domainFormPanel.getField("name");
        assertNull("expect the 'name' field not to be shown in the domain editor", nameField);
        assertNotNull("confirm that the 'firstCol' field is found", domainFormPanel.getField("firstCol"));
    }

    @Test
    public void testAddFieldsWithReservedNames() throws Exception
    {
        String sampleSet = "fieldsWithReservedNamesSampleSet";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("firstCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);

        DomainFieldRow modifiedRow = domainFormPanel.addField("modified");
        DomainFieldRow blarg1 = domainFormPanel.addField("blarg");
        DomainFieldRow blarg2 = domainFormPanel.addField("blarg");
        DomainFieldRow clientFieldWarning = domainFormPanel.addField("select * from table");

        domainDesignerPage.clickSave();
        String clientWarning = domainDesignerPage.waitForWarning();
        String multipleIssuesError = domainDesignerPage.waitForError();
        String expectedErrMsg = "Multiple fields contain issues that need to be fixed. Review the red highlighted fields below for more information.";
        String expectedWarningMsg = " SQL queries, R scripts, and other code are easiest to write when field names only contain combination of letters, numbers, and underscores, and start with a letter or underscore.";
        assertTrue( "expect error message to contain ["+expectedErrMsg+"] but was ["+multipleIssuesError+"]",
                multipleIssuesError.contains(expectedErrMsg));
        assertTrue( "expect warning message to contain ["+expectedWarningMsg+"] but was ["+clientWarning+"]",
                clientWarning.contains(expectedWarningMsg));

        assertTrue("expect field error when using reserved field names", modifiedRow.hasFieldError());
        assertTrue("expect error for duplicate field names", blarg1.hasFieldError());
        assertTrue("expect error for duplicate field names", blarg2.hasFieldError());
        assertTrue("expect warning for field name with spaces or special characters", clientFieldWarning.hasFieldWarning());
    }

    /**
     * provides regression coverage for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=38314
     * @throws Exception
     */
    @Test
    public void verifySavedFieldCannotBeRenamedReservedName() throws Exception
    {
        String sampleSet = "renameColToReservedNameTest";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("extraField", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);

        DomainFieldRow testCol = domainFormPanel.getField("testCol");
        testCol.setName("modified");
        domainDesignerPage.clickSave();

        // confirm expected error warning
        String expectedError = "'modified' is a reserved field name";
        String error = domainDesignerPage.waitForError();
        assertTrue("expect error containing ["+expectedError+"] but it was ["+error+"]",
                error.contains(expectedError));

        // double-check to ensure the column has not been altered on the server side
        SelectRowsResponse rowsResponse = dgen.getRowsFromServer(createDefaultConnection(true));
        List<String> columnsAfterSaveAttempt = rowsResponse.getColumnModel().stream().map(col -> (String) col.get("dataIndex")).collect(Collectors.toList());
        Assert.assertThat("Columns after delete", columnsAfterSaveAttempt,
                CoreMatchers.allOf(CoreMatchers.hasItems("Name", "testCol", "extraField"),
                        CoreMatchers.not(CoreMatchers.hasItem("modified"))));
    }

    @Test
public void showHideFieldOnDefaultGridView() throws Exception
{
    String sampleSet = "showFieldOnDefaultGridViewSampleSet";

    FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);
    TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
            .withColumnSet(List.of(
                    TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                    TestDataGenerator.simpleFieldDef("extraField", FieldDefinition.ColumnType.String),
                    TestDataGenerator.simpleFieldDef("testCol", FieldDefinition.ColumnType.String)));
    DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");
    List<Map<String, Object>> createdFields = createResponse.getColumns();

    DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
    DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);
    DomainFieldRow defaultViewRow = domainFormPanel.addField("defaultViewField");
    defaultViewRow.showFieldOnDefaultView(false);

    domainDesignerPage.clickSaveAndFinish();

    // expect to arrive at project home, with a list of 'sampleSets'
    clickAndWait(Locator.linkWithText(sampleSet));

    DataRegionTable sampleSetTable = DataRegionTable.findDataRegionWithinWebpart(this,"Sample Set Contents");
    List<String> columnsInDefaultView = sampleSetTable.getColumnNames();
    Assert.assertThat("Columns after delete", columnsInDefaultView,
            CoreMatchers.allOf(CoreMatchers.hasItems("Name", "Flag", "extraField", "testCol"),
                    CoreMatchers.not(CoreMatchers.hasItem("defaultViewField"))));
}

    @Test
    public void showHideFieldOnInsertGridView() throws Exception
    {
        String sampleSet = "showFieldOnInsertGridViewSampleSet";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("extraField", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);
        DomainFieldRow hiddenRow = domainFormPanel.addField("hiddenField");
        hiddenRow.showFieldOnInsertView(false);
        DomainFieldRow shownRow = domainFormPanel.addField("shownField");
        shownRow.showFieldOnInsertView(true);

        domainDesignerPage.clickSaveAndFinish();

        // expect to arrive at project home, with a list of 'sampleSets'
        clickAndWait(Locator.linkWithText(sampleSet));

        DataRegionTable sampleSetTable = DataRegionTable.findDataRegionWithinWebpart(this,"Sample Set Contents");
        sampleSetTable.clickInsertNewRow();

        waitForElement(Locator.input("quf_shownField"));
        assertElementNotPresent(Locator.input("quf_hiddenField"));
    }

    @Test
    public void showHideFieldOnUpdateForm() throws Exception
    {
        String sampleSet = "showFieldOnUpdateForm";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("extraField", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");
        List<Map<String, Object>> createdFields = createResponse.getColumns();

        dgen.addCustomRow(Map.of("name", "first", "extraField", "eleven", "testCol", "test", "hiddenField", "hidden", "shownField", "shown"));

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);
        DomainFieldRow hiddenRow = domainFormPanel.addField("hiddenField");
        hiddenRow.showFieldOnUpdateView(false);
        DomainFieldRow shownRow = domainFormPanel.addField("shownField");
        shownRow.showFieldOnUpdateView(true);

        domainDesignerPage.clickSaveAndFinish();
        // expect to arrive at project home, with a list of 'sampleSets'
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());
        clickAndWait(Locator.linkWithText(sampleSet));

        DataRegionTable sampleSetTable = DataRegionTable.findDataRegionWithinWebpart(this,"Sample Set Contents");
        sampleSetTable.clickEditRow(0);

        waitForElement(Locator.input("quf_shownField"));
        assertElementNotPresent(Locator.input("quf_hiddenField"));
    }

    @Test
    public void setPhiLevel() throws Exception
    {
        String sampleSet = "phiLevelSampleSet";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleSet);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumnSet(List.of(
                        TestDataGenerator.simpleFieldDef("name", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("extraField", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "SampleSet");

        // create a little test data
        dgen.addCustomRow(Map.of("name", "first", "extraField", "eleven", "testCol", "test",
                "notPHI", "notPHI", "limitedPHI", "limitedPHI", "fullPHI", "fullPHI", "restrictedPHI", "restrictedPHI"));
        dgen.addCustomRow(Map.of("name", "second", "extraField", "twelve", "testCol", "blah",
                "notPHI", "notPHI", "limitedPHI", "limitedPHI", "fullPHI", "fullPHI", "restrictedPHI", "restrictedPHI"));

        List<Map<String, Object>> createdFields = createResponse.getColumns();

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleSet);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldProperties(sampleSet);
        DomainFieldRow notPhi = domainFormPanel.addField("notPHI");
        notPhi.setPHILevel("Not PHI");
        DomainFieldRow limitedPHI = domainFormPanel.addField("limitedPHI");
        limitedPHI.setPHILevel("Limited PHI");
        DomainFieldRow fullPHI = domainFormPanel.addField("fullPHI");
        fullPHI.setPHILevel("Full PHI");
        DomainFieldRow restrictedPHI = domainFormPanel.addField("restrictedPHI");
        restrictedPHI.setPHILevel("Restricted PHI");

        domainDesignerPage.clickSaveAndFinish();
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());
        clickAndWait(Locator.linkWithText(sampleSet));

        DataRegionTable sampleSetTable = DataRegionTable.findDataRegionWithinWebpart(this,"Sample Set Contents");
        DomainResponse domainResponse = dgen.getDomain(createDefaultConnection(true));

        Map<String, Object> notPHiField = domainResponse.getColumns().stream().filter(a-> a.get("name").equals("notPHI")).findFirst().orElse(null);
        assertEquals("NotPHI", notPHiField.get("PHI"));
        Map<String, Object> limitedPHIField = domainResponse.getColumns().stream().filter(a-> a.get("name").equals("limitedPHI")).findFirst().orElse(null);
        assertEquals("Limited", limitedPHIField.get("PHI"));
        Map<String, Object> fullPhiField = domainResponse.getColumns().stream().filter(a-> a.get("name").equals("fullPHI")).findFirst().orElse(null);
        assertEquals("PHI", fullPhiField.get("PHI"));
        Map<String, Object> restrictedPHIField = domainResponse.getColumns().stream().filter(a-> a.get("name").equals("restrictedPHI")).findFirst().orElse(null);
        assertEquals("Restricted", restrictedPHIField.get("PHI"));
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "DomainDesignerTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}

package org.labkey.test.tests;

import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.json.JSONArray;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.domain.ConditionalFormat;
import org.labkey.remoteapi.domain.ConditionalFormatFilter;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.DomainDetailsResponse;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.GetDomainDetailsCommand;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.serverapi.collections.ArrayListMap;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.ConditionalFormatDialog;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.domain.RangeValidatorDialog;
import org.labkey.test.components.domain.RegexValidatorDialog;
import org.labkey.test.components.domain.RegexValidatorPanel;
import org.labkey.test.pages.core.admin.BaseSettingsPage.DATE_FORMAT;
import org.labkey.test.pages.core.admin.BaseSettingsPage.TIME_FORMAT;
import org.labkey.test.pages.experiment.CreateSampleTypePage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND;

@Category({BVT.class})
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
        new PortalHelper(getDriver()).addBodyWebPart("Sample Types");
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
                .withColumns(List.of(
                        new FieldDefinition("Number", FieldDefinition.ColumnType.Integer)
                ));
        dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);

        DomainFormPanel panel = domainDesignerPage.fieldsPanel();
        DomainFieldRow integerRow = panel
                .addField("integerField")
                .setType(FieldDefinition.ColumnType.Integer)
                .setNumberFormat("###,###,###")
                .setScaleType(FieldDefinition.ScaleType.LINEAR)
                .setDescription("field for an Integer")
                .setLabel("IntegerFieldLabel");

        DomainFieldRow decimalRow = panel
                .addField("decimalField")
                .setType(FieldDefinition.ColumnType.Decimal)
                .setNumberFormat("###,###,###.000")
                .setScaleType(FieldDefinition.ScaleType.LOG)
                .setDescription("field for a decimal")
                .setLabel("DecimalField");
        domainDesignerPage.clickFinish();

        dgen = new TestDataGenerator(lookupInfo)        // now put some test data in the new fields
                .withColumns(List.of(
                        new FieldDefinition("Key", FieldDefinition.ColumnType.Integer),
                        new FieldDefinition("integerField", FieldDefinition.ColumnType.Integer)
                ));
        dgen.addCustomRow(Map.of("Number", 1, "integerField", 10000000, "decimalField", 6.022));
        dgen.addCustomRow(Map.of("Number", 2, "integerField", 8675309, "decimalField", 3.1415926));
        dgen.addCustomRow(Map.of("Number", 3, "integerField", 123456789, "decimalField", 12345.678));
        dgen.addCustomRow(Map.of("Number", 4, "integerField", 98765432, "decimalField", 1234.56789));
        dgen.addCustomRow(Map.of("Number", 5, "integerField", 4, "decimalField", 5.654));
        SaveRowsResponse response = dgen.insertRows(createDefaultConnection(), dgen.getRows());

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
        String sampleType = "StringSampleType";
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);

        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("stringField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("multilineField", FieldDefinition.ColumnType.MultiLine)
                ));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

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
        domainDesignerPage.clickFinish();

        dgen.addCustomRow(Map.of("name", "first", "stringField", "baaaaaasic string heeeeeeeeeeere", "multiLineField", "multi\nline\nfield"));
        dgen.addCustomRow(Map.of("name", "second", "stringField", "basic string heeeeere", "multiLineField", "multi\nline\nfield with extra"));
        dgen.addCustomRow(Map.of("name", "third", "stringField", "basic string here", "multiLineField", "multi\nline\nfield and so much more"));
        dgen.addCustomRow(Map.of("name", "fourth", "stringField", "basic string here", "multiLineField", "multi\nline\nfield this is silly"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        goToProjectHome();
        clickAndWait(Locator.linkWithText(sampleType));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();

        Map<String, String> firstMap = samplesTable.getRowDataAsMap("Name", "first");
        assertEquals("multi\nline\nfield", firstMap.get("multiLinefield"));
        assertEquals("baaaaaasic string heeeeeeeeeeere", firstMap.get("stringField"));
    }

    @Test
    public void testDeleteDomainField() throws Exception
    {
        String sampleType = "deleteColumnSampleType";
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);

        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("deleteMe", FieldDefinition.ColumnType.String)
                ));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        List<PropertyDescriptor> createdFields = createResponse.getDomain().getFields();
        assertTrue(createdFields.get(0).getName().equals("deleteMe"));

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        domainFormPanel.getField("deleteMe")
                .clickRemoveField(true);
        domainDesignerPage.clickFinish();

        GetDomainDetailsCommand domainCommand = new GetDomainDetailsCommand("exp.materials", sampleType);
        DomainDetailsResponse afterResponse = domainCommand.execute(createDefaultConnection(), getProjectName());
        Domain domain = afterResponse.getDomain();
        assertEquals("expect only field in the domain to have been deleted", 0, domain.getFields().size());

        // double-check to ensure the column has been deleted
        SelectRowsResponse rowsResponse = dgen.getRowsFromServer(createDefaultConnection());
        List<String> columnsAfterDelete = rowsResponse.getColumnModel().stream().map(col -> (String) col.get("dataIndex")).collect(Collectors.toList());
        assertThat("Columns after delete", columnsAfterDelete, CoreMatchers.allOf(CoreMatchers.hasItem("Name"), CoreMatchers.not(CoreMatchers.hasItem("deleteMe"))));

        // this column should no longer exist
        List<Map<String, Object>> deleteMe = rowsResponse.getColumnModel().stream().filter(a -> a.get("dataIndex").equals("deleteMe")).collect(Collectors.toList());
        assertEquals(0, deleteMe.size());
        // make sure the name field is still there
        List<Map<String, Object>> name = rowsResponse.getColumnModel().stream().filter(a -> a.get("dataIndex").equals("Name")).collect(Collectors.toList());
        assertEquals(1, name.size());
    }

    @Test
    public void testInvalidLookupDomainField() throws IOException, CommandException
    {
        String listName = "InvalidLookUpNameList";
        String editedListName = listName + "_edited";
        String sampleType = "TestSampleForInvalidLookupField";

        log("Creating a list used for look up");
        _listHelper.createList(getProjectName(), listName, "Id");

        log("Creating a sample type with look up field to above list");
        FieldDefinition.LookupInfo lookupInfo1 = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo1)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        domainDesignerPage.fieldsPanel().addField("lookUpField")
                .setType(FieldDefinition.ColumnType.Lookup)
                .expand()
                .setFromFolder("Current Folder")
                .setFromSchema("lists")
                .setFromTargetTable(listName + " (Integer)")
                .collapse();
        domainDesignerPage.clickFinish();

        log("Editing the list name");
        _listHelper.goToEditDesign(listName);
        EditListDefinitionPage editListDefinitionPage = new EditListDefinitionPage(getDriver());
        editListDefinitionPage.setName(editedListName)
                .clickSave();

        log("Verifying the error message");
        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        domainDesignerPage.fieldsPanel().getField("lookUpField").detailsMessage();
        Assert.assertEquals("Missing invalid look up message", "Current Folder > lists > " + listName + " . Error: Lookup target table does not exist.",
                domainDesignerPage.fieldsPanel().getField("lookUpField").detailsMessage());

        log("Updating valid value for lookup field value ");
        domainDesignerPage.fieldsPanel().getField("lookUpField")
                .expand()
                .setFromTargetTable(editedListName + " (Integer)")
                .collapse();
        domainDesignerPage.clickFinish();
    }

    @Test
    public void testAddDomainField() throws Exception
    {
        String sampleType = "addColumnSampleType";
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);

        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        domainFormPanel.addField("addedField")
                .setType(FieldDefinition.ColumnType.DateAndTime)
                .expand()
                .setDateTimeFormat(DATE_FORMAT.yyyy_MM_dd, TIME_FORMAT.HH_mm)
                .setExcludeFromDateShifting(false)
                .setDescription("simplest date format of all")
                .setLabel("DateTime");
        domainDesignerPage.clickFinish();

        // insert sample dates, confirm expected formats

        dgen.addCustomRow(Map.of("name", "jeff", "addedField", "05-15-2007 11:25"));
        dgen.addCustomRow(Map.of("name", "billy", "addedField", "05-15-2017 23:25"));
        dgen.addCustomRow(Map.of("name", "william", "addedField", "07-25-2007 11:25"));
        dgen.addCustomRow(Map.of("name", "charlie", "addedField", "11-15-1997 13:25"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        goToProjectHome();
        clickAndWait(Locator.linkWithText(sampleType));
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
        String sampleType = "errorColumnSampleType";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("firstCol", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        // add a new field, but leave the name field blank
        DomainFieldRow noNameRow = domainFormPanel.addField("");
        domainDesignerPage.clickSaveExpectingErrors();

        assertTrue("field should report error if saved without a name", noNameRow.hasFieldError());
        assertTrue(noNameRow.detailsMessage().contains("New Field. Error: Please provide a name for each field."));
        WebElement errorDiv = domainDesignerPage.errorAlert();
        assertNotNull(errorDiv);
        String hasNoNameError = domainDesignerPage.waitForError();
        assertTrue("expect error to contain [Please provide a name for each field.] but was[" + hasNoNameError + "]",
                hasNoNameError.contains("Please provide a name for each field."));

        String warningFieldMessage = noNameRow.setName("&has weird characters that make scripts hard to write")
                .waitForWarning()
                .detailsMessage();
        String expectedWarning = "Warning: Field name contains special characters.";
        assertThat("expected new field message", warningFieldMessage, containsString("New Field."));
        assertThat("expected error", warningFieldMessage, containsString(expectedWarning));

        domainDesignerPage.clickCancelWithUnsavedChanges().discardChanges();
    }

    @Test
    public void testDuplicateFieldName() throws Exception
    {
        String sampleType = "errorDuplicateFieldSampleType";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("firstCol", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        // add a new field, but leave the name field blank
        DomainFieldRow firstcol = domainFormPanel.getField("firstCol");
        DomainFieldRow dupeNameRow = domainFormPanel.addField("firstCol");
        domainDesignerPage.clickSaveExpectingErrors();

        // confirm the page shows the summary error
        String dupeError = domainDesignerPage.waitForError();
        String expectedError = "The field name 'firstCol' is already taken. Please provide a unique name for each field.";
        assertTrue("expect field-level warning to contain [" + expectedError + "] but was[" + dupeError + "]",
                dupeError.contains(expectedError));

        // ensure the duplicate fields show as error fields, with explanations
        assertTrue("expect duplicate field name rows to show errors", firstcol.hasFieldError());
        String firstColErrorStatus = firstcol.detailsMessage();
        assertTrue("expect field-level warning to contain [" + expectedError + "] but was[" + firstColErrorStatus + "]",
                firstColErrorStatus.contains(expectedError));
        assertTrue("expect duplicate field name to show as error", dupeNameRow.hasFieldError());
        String dupeColErrorStatus = dupeNameRow.detailsMessage();
        assertTrue("expect field-level warning to contain [" + expectedError + "] but was[" + dupeColErrorStatus + "]",
                dupeColErrorStatus.contains(expectedError));

        domainDesignerPage.clickCancelWithUnsavedChanges().discardChanges();
    }

    @Test
    public void testUserCannotEditListKeyFields() throws Exception
    {
        String list = "testUserCannotEditKeyFieldsList";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", list);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("firstCol", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", list);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

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
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("color", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        dgen.addCustomRow(Map.of("name", "first", "color", "orange"));
        dgen.addCustomRow(Map.of("name", "second", "color", "green"));
        dgen.addCustomRow(Map.of("name", "third", "color", "blue"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", list);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        // first, delete 'name' column and 'color'
        DomainFieldRow nameRow = domainFormPanel.getField("name");
        nameRow.clickRemoveField(true);
        DomainFieldRow colorRow = domainFormPanel.getField("color");
        colorRow.clickRemoveField(true);
        domainDesignerPage.clickFinish();

        // there should just be the key field (id) now:
        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", list);
        domainFormPanel = domainDesignerPage.fieldsPanel(); // re-find the panel to work around field caching silliness
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
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String)
                                .setRequired(true),                                                                 // <-- marked 'required'
                        new FieldDefinition("color", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        dgen.addCustomRow(Map.of("name", "first", "color", "orange"));
        dgen.addCustomRow(Map.of("name", "second", "color", "green"));
        dgen.addCustomRow(Map.of("name", "third", "color", "blue"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", list);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        DomainFieldRow nameRow = domainFormPanel.getField("name");
        // confirm the UI shows its expected 'required' status
        assertEquals(true, nameRow.getRequiredField());

        nameRow.clickRemoveField(true);
        domainDesignerPage.clickFinish();
    }

    @Test
    public void testDeleteNewField() throws Exception
    {
        String list = "testDeleteNewFieldList";

        // create the list with no fields to start
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", list);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo);
        dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", list);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        // add a new field and remove it, new fields should not have Confirm Remove Field dialog
        domainFormPanel.addField("deleteMe")
                .setLabel("field label test")
                .setDescription("field description test")
                .clickRemoveField(false);

        domainDesignerPage.clickFinish();
    }

    /**
     * confirms that the key field (called 'name') in a sampleset is not shown in the domain editor
     *
     * @throws Exception
     */
    @Test
    public void testConfirmNameFieldFromSampleTypeNotShown() throws Exception
    {
        String sampleType = "hiddenNameFieldSampleType";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("firstCol", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        DomainFieldRow nameField = domainFormPanel.getField("name");
        assertNull("expect the 'name' field not to be shown in the domain editor", nameField);
        assertNotNull("confirm that the 'firstCol' field is found", domainFormPanel.getField("firstCol"));
    }

    @Test
    public void testFieldNameErrors() throws Exception
    {
        String sampleType = "fieldsWithReservedNamesSampleType";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("firstCol", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        DomainFieldRow modifiedRow = domainFormPanel.addField("modified");
        DomainFieldRow blarg1 = domainFormPanel.addField("blarg");
        DomainFieldRow blarg2 = domainFormPanel.addField("blarg");
        DomainFieldRow clientFieldWarning = domainFormPanel.addField("select * from table");

        domainDesignerPage.clickSaveExpectingErrors();
        String expectedWarnMsg = "Warning: Field name contains special characters.";
        String blargErrMsg = "New Field. Error: The field name 'blarg' is already taken. Please provide a unique name for each field.";
        String reservedErrMsg = "New Field. Error: 'modified' is a reserved field name in 'fieldsWithReservedNamesSampleType'.";
        String modRowDetailsMsg = modifiedRow.waitForError()
                .detailsMessage();
        String blarg1DetailsMsg = blarg1.waitForError().detailsMessage();
        String blarg2DetailsMsg = blarg2.waitForError().detailsMessage();
        String clientFieldWarningMsg = clientFieldWarning.waitForWarning().detailsMessage();

        assertThat("expected warning", clientFieldWarningMsg, containsString(expectedWarnMsg));
        assertThat("expected error", blarg1DetailsMsg, containsString(blargErrMsg));
        assertThat("expected error", blarg2DetailsMsg, containsString(blargErrMsg));
        assertThat("expected error", modRowDetailsMsg, containsString(reservedErrMsg));

        assertTrue("expect field error when using reserved field names", modifiedRow.hasFieldError());
        assertTrue("expect error for duplicate field names", blarg1.hasFieldError());
        assertTrue("expect error for duplicate field names", blarg2.hasFieldError());
        assertTrue("expect warning for field name with spaces or special characters", clientFieldWarning.hasFieldWarning());

        domainDesignerPage.clickCancelWithUnsavedChanges().discardChanges();
    }

    /**
     * provides regression coverage for https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=38314
     *
     * @throws Exception
     */
    @Test
    public void verifySavedFieldCannotBeRenamedReservedName() throws Exception
    {
        String sampleType = "renameColToReservedNameTest";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        // go to the new domain designer and do some work here
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        DomainFieldRow testCol = domainFormPanel.getField("testCol");
        testCol.setName("modified");
        domainDesignerPage.clickSaveExpectingErrors();

        // confirm expected error warning
        String expectedError = "'modified' is a reserved field name";
        String error = domainDesignerPage.waitForError();
        assertTrue("expect error containing [" + expectedError + "] but it was [" + error + "]",
                error.contains(expectedError));

        // double-check to ensure the column has not been altered on the server side
        SelectRowsResponse rowsResponse = dgen.getRowsFromServer(createDefaultConnection());
        List<String> columnsAfterSaveAttempt = rowsResponse.getColumnModel().stream().map(col -> (String) col.get("dataIndex")).collect(Collectors.toList());
        assertThat("Columns after delete", columnsAfterSaveAttempt,
                CoreMatchers.allOf(hasItems("Name", "testCol", "extraField"),
                        CoreMatchers.not(CoreMatchers.hasItem("modified"))));

        domainDesignerPage.clickCancelWithUnsavedChanges().discardChanges();
    }

    /**
     * regresses issue https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=38341
     *
     * @throws Exception
     */
    @Test
    public void showHideFieldOnDefaultGridView() throws Exception
    {
        String sampleType = "showFieldOnDefaultGridViewSampleType";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow defaultViewRow = domainFormPanel.addField("defaultViewField");
        defaultViewRow.showFieldOnDefaultView(false);
        DomainFieldRow extraFieldRow = domainFormPanel.getField("extraField");
        extraFieldRow.showFieldOnDefaultView(true); // true is default behavior

        domainDesignerPage.clickFinish();

        // expect to arrive at project home, with a list of 'sample types'
        clickAndWait(Locator.linkWithText(sampleType));

        DataRegionTable sampleTypeTable = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        List<String> columnsInDefaultView = sampleTypeTable.getColumnNames();
        assertThat("Columns after delete", columnsInDefaultView,
                CoreMatchers.allOf(hasItems("Name", "Flag", "extraField", "testCol"),
                        CoreMatchers.not(CoreMatchers.hasItem("defaultViewField"))));
    }

    @Test
    public void showHideFieldOnInsertGridView() throws Exception
    {
        String sampleType = "showFieldOnInsertGridViewSampleType";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow hiddenRow = domainFormPanel.addField("hiddenField");
        hiddenRow.showFieldOnInsertView(false);
        DomainFieldRow shownRow = domainFormPanel.addField("shownField");
        shownRow.showFieldOnInsertView(true);

        domainDesignerPage.clickFinish();

        // expect to arrive at project home, with a list of 'sample types'
        waitForElement(Locator.linkWithText(sampleType));
        clickAndWait(Locator.linkWithText(sampleType));

        DataRegionTable sampleTypeTable = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        sampleTypeTable.clickInsertNewRow();

        waitForElement(Locator.input("quf_shownField"));
        assertElementNotPresent(Locator.input("quf_hiddenField"));
    }

    @Test
    public void showHideFieldOnUpdateForm() throws Exception
    {
        String sampleType = "showFieldOnUpdateForm";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        dgen.addCustomRow(Map.of("name", "first", "extraField", "eleven", "testCol", "test", "hiddenField", "hidden", "shownField", "shown"));

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow hiddenRow = domainFormPanel.addField("hiddenField");
        hiddenRow.showFieldOnUpdateView(false);
        DomainFieldRow shownRow = domainFormPanel.addField("shownField");
        shownRow.showFieldOnUpdateView(true);

        domainDesignerPage.clickFinish();
        // expect to arrive at project home, with a list of 'sample types'
        dgen.insertRows(createDefaultConnection(), dgen.getRows());
        clickAndWait(Locator.linkWithText(sampleType));

        DataRegionTable sampleTypeTable = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        sampleTypeTable.clickEditRow(0);

        waitForElement(Locator.input("quf_shownField"));
        assertElementNotPresent(Locator.input("quf_hiddenField"));
    }

    @Test
    public void setPhiLevel() throws Exception
    {
        String sampleType = "phiLevelSampleType";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        // create a little test data
        dgen.addCustomRow(Map.of("name", "first", "extraField", "eleven", "testCol", "test",
                "notPHI", "notPHI", "limitedPHI", "limitedPHI", "fullPHI", "fullPHI", "restrictedPHI", "restrictedPHI"));
        dgen.addCustomRow(Map.of("name", "second", "extraField", "twelve", "testCol", "blah",
                "notPHI", "notPHI", "limitedPHI", "limitedPHI", "fullPHI", "fullPHI", "restrictedPHI", "restrictedPHI"));

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow notPhi = domainFormPanel.addField("notPHI");
        notPhi.setPHILevel(FieldDefinition.PhiSelectType.NotPHI);
        DomainFieldRow limitedPHI = domainFormPanel.addField("limitedPHI");
        limitedPHI.setPHILevel(FieldDefinition.PhiSelectType.Limited);
        DomainFieldRow fullPHI = domainFormPanel.addField("fullPHI");
        fullPHI.setPHILevel(FieldDefinition.PhiSelectType.PHI);
        DomainFieldRow restrictedPHI = domainFormPanel.addField("restrictedPHI");
        restrictedPHI.setPHILevel(FieldDefinition.PhiSelectType.Restricted);

        domainDesignerPage.clickFinish();
        dgen.insertRows(createDefaultConnection(), dgen.getRows());
        clickAndWait(Locator.linkWithText(sampleType));

        DataRegionTable sampleTypeTable = DataRegionTable.findDataRegionWithinWebpart(this, "Sample Type Contents");
        Connection cn = createDefaultConnection();
        DomainDetailsResponse domainResponse = dgen.getQueryHelper(cn).getDomainDetails();

        assertEquals("NotPHI", getColumn(domainResponse.getDomain(), "notPHI").getPHI());
        assertEquals("Limited", getColumn(domainResponse.getDomain(), "limitedPHI").getPHI());
        assertEquals("PHI", getColumn(domainResponse.getDomain(), "fullPHI").getPHI());
        assertEquals("Restricted", getColumn(domainResponse.getDomain(), "restrictedPHI").getPHI());
    }

    @Test
    public void setMissingValue() throws Exception
    {
        String sampleType = "setMissingValueTest";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        dgen.addCustomRow(Map.of("name", "first", "extraField", "eleven", "testCol", "test", "hiddenField", "hidden", "shownField", "shown"));

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow missingValueRow = domainFormPanel.addField("missingValue");
        missingValueRow.setMissingValuesEnabled(true);
        // explicitly set missingValue false on extraField
        DomainFieldRow extraFieldRow = domainFormPanel.getField("extraField");
        extraFieldRow.setMissingValuesEnabled(false);

        domainDesignerPage.clickFinish();
        Connection cn = createDefaultConnection();
        DomainDetailsResponse domainResponse = dgen.getQueryHelper(cn).getDomainDetails();
        assertEquals("expect column to have MissingValue enabled", true, getColumn(domainResponse.getDomain(), "missingValue").getAllProperties().get("mvEnabled"));
        assertEquals("expect column not to have MissingValue enabled", false, getColumn(domainResponse.getDomain(), "extraField").getAllProperties().get("mvEnabled"));
    }

    @Test
    public void setFieldAsDimension() throws Exception
    {
        String sampleType = "setFieldAsDimension";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow dimensionField = domainFormPanel.addField("dimensionField");
        dimensionField.setDimension(true);
        // explicitly set dimension false on extraField
        DomainFieldRow extraFieldRow = domainFormPanel.getField("extraField");
        extraFieldRow.setDimension(false);

        domainDesignerPage.clickFinish();

        Connection cn = createDefaultConnection();
        DomainDetailsResponse domainResponse = dgen.getQueryHelper(cn).getDomainDetails();
        assertEquals("dimensionField should have dimension marked true", true, getColumn(domainResponse.getDomain(), "dimensionField").getDimension());
        assertEquals("extraField should not have dimension marked true", false, getColumn(domainResponse.getDomain(), "extraField").getDimension());
    }

    @Test
    public void setFieldAsMeasure() throws Exception
    {
        String sampleType = "setFieldAsMeasure";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow measureField = domainFormPanel.addField("measureField");
        measureField.setMeasure(true);
        // explicitly set measure false on extraField
        DomainFieldRow extraFieldRow = domainFormPanel.getField("extraField");
        extraFieldRow.setMeasure(false);

        domainDesignerPage.clickFinish();

        Connection cn = createDefaultConnection();
        DomainDetailsResponse domainResponse = dgen.getQueryHelper(cn).getDomainDetails();

        assertEquals("measureField should have dimension marked true", true, getColumn(domainResponse.getDomain(), "measureField").getMeasure());
        assertEquals("extraField should not have dimension marked true", false, getColumn(domainResponse.getDomain(), "extraField").getMeasure());
    }

    @Test
    public void setFieldAsVariable() throws Exception
    {
        String sampleType = "setFieldAsVariable";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow variableField = domainFormPanel.addField("variableField");
        variableField.setRecommendedVariable(true);
        // explicitly set recommended variable false on extraField
        DomainFieldRow extraFieldRow = domainFormPanel.getField("extraField");
        extraFieldRow.setRecommendedVariable(false);

        domainDesignerPage.clickFinish();

        Connection cn = createDefaultConnection();
        DomainDetailsResponse domainResponse = dgen.getQueryHelper(cn).getDomainDetails();
        assertEquals("variableField should have recommendedVariable marked true", true, getColumn(domainResponse.getDomain(), "variableField").getAllProperties().get("recommendedVariable"));
        assertEquals("extraField should not have recommendedVariable marked true", false, getColumn(domainResponse.getDomain(), "extraField").getAllProperties().get("recommendedVariable"));
    }

    @Test
    public void testLookUpFieldSampleType() throws IOException, CommandException
    {
        String sampleType = "setFieldAsLookup";
        String listName = "lookUpList1";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen1 = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("color", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen1.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        FieldDefinition.LookupInfo lookupInfo1 = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo1)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("extraField", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        DomainFieldRow lookUpRow = domainFormPanel.addField("lookUpField")
                .setType(FieldDefinition.ColumnType.Lookup)
                .expand()
                .setFromFolder("Current Folder")
                .setFromSchema("lists")
                .setFromTargetTable("lookUpList1 (Integer)")
                .setDescription("LookUp in same container")
                .collapse();

        assertEquals("Incorrect detail message", "New Field. Current Folder > lists > lookUpList1", lookUpRow.detailsMessage());

        domainDesignerPage.clickFinish();

        Connection cn = createDefaultConnection();
        DomainDetailsResponse domainResponse = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor lookupFieldDescriptor = getColumn(domainResponse.getDomain(), "lookUpField");
        assertEquals("lookUpField from folder is incorrect", null, lookupFieldDescriptor.getAllProperties().get("lookupContainer"));
        assertEquals("lookUpField schema name is incorrect", "lists", lookupFieldDescriptor.getAllProperties().get("lookupSchema"));
        assertEquals("lookUpField target table is incorrect", listName, lookupFieldDescriptor.getAllProperties().get("lookupQuery"));

    }

    @Test
    public void testLookUpFieldList() throws IOException, CommandException
    {
        String mainListName = "setFieldAsLookupinList";
        String lookUplistName = "lookUpList";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", lookUplistName);
        TestDataGenerator dgen1 = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("color", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen1.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        FieldDefinition.LookupInfo lookupInfo1 = new FieldDefinition.LookupInfo(getProjectName(), "lists", mainListName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo1)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testCol", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse1 = dgen.createDomain(createDefaultConnection(), "VarList", Map.of("keyName", "id"));

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", mainListName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        DomainFieldRow lookUpRow = domainFormPanel.addField("lookUpField")
                .setType(FieldDefinition.ColumnType.Lookup)
                .expand()
                .setFromFolder("Current Folder")
                .setFromSchema("lists")
                .setFromTargetTable("lookUpList (Integer)")
                .setDescription("LookUp in same container")
                .collapse();

        assertEquals("Incorrect detail message", "New Field. Current Folder > lists > lookUpList", lookUpRow.detailsMessage());
        domainDesignerPage.clickFinish();

        Connection cn = createDefaultConnection();
        DomainDetailsResponse domainResponse = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor lookupColumn = getColumn(domainResponse.getDomain(), "lookUpField"); // getColumn asserts the column is non-null

        assertEquals("lookUpField schema name should be present", "lists", lookupColumn.getAllProperties().get("lookupSchema"));
        assertEquals("lookUpField target table should be present", "lookUpList", lookupColumn.getAllProperties().get("lookupQuery"));
        assertNull("lookUpField target table should be null for current container", lookupColumn.getAllProperties().get("lookupContainer"));
    }

    @Test
    public void testLookupPropertyValidator() throws Exception
    {
        String listName = "lookupValidatorTestList";            // this is the main list
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("color", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));
        String listName1 = "lookupValidatorLookupList";         // this list will contain lookup values
        String lookupList1Item = listName1 + " (Integer)";
        FieldDefinition.LookupInfo lookupInfo1 = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName1);
        TestDataGenerator dgen1 = new TestDataGenerator(lookupInfo1)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("color", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse1 = dgen1.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));
        dgen1.addCustomRow(Map.of("name", "joey", "color", "green"));
        dgen1.addCustomRow(Map.of("name", "billy", "color", "red"));
        dgen1.addCustomRow(Map.of("name", "eddie", "color", "blue"));
        dgen1.insertRows(createDefaultConnection(), dgen1.getRows());

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        // add a lookup field to the test list
        DomainFieldRow row = domainFormPanel.addField("looky")
                .setType(FieldDefinition.ColumnType.Lookup)
                .setFromFolder("Current Folder")
                .setFromSchema("lists")
                .setFromTargetTable(lookupList1Item)
                .setLookupValidatorEnabled(true)
                .setDescription("should validate lookup value contents");
        domainDesignerPage.clickFinish();

        // now make sure the validator is set
        Connection cn = createDefaultConnection();
        DomainDetailsResponse domainResponse = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor lookupColumn = getColumn(domainResponse.getDomain(), "looky");
        Map<String, Object> propertyValidator = getPropertyValidator(lookupColumn, "Lookup Validator");
    }

    @Test
    public void testDefaultValues() throws Exception
    {
        String listName = "defaultValuesTestList";
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("color", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        DomainFieldRow testRow = domainFormPanel.getField("color");
        testRow.clickAdvancedSettings()
                .setDefaultValueType(FieldDefinition.DefaultType.FIXED_EDITABLE)  //"Editable default"
                .apply();
        domainDesignerPage.clickFinish();

        // now make sure the validator is not yet set
        Connection cn1 = createDefaultConnection();
        DomainDetailsResponse domainResponse = dgen.getQueryHelper(cn1).getDomainDetails();
        PropertyDescriptor colorCol = getColumn(domainResponse.getDomain(), "color");
        assertNull("expect default value to not be set yet", colorCol.getAllProperties().get("defaultValue"));

        // now re-open the domain designer, go set the default values
        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel newPanel = domainDesignerPage.fieldsPanel();
        newPanel.getField("color")
                .clickAdvancedSettings()
                .clickDefaultValuesLink();  // should land us in
        setFormElement(Locator.input("color"), "green");
        clickButton("Save Defaults");

        Connection cn = createDefaultConnection();
        DomainDetailsResponse updatedResponse = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor updatedColor = getColumn(updatedResponse.getDomain(), "color");
        assertEquals("expect default value to be green", "green", updatedColor.getAllProperties().get("defaultValue"));
    }

    @Test
    @Ignore("Issue 38785: dom.disable_beforeunload FireFox preference is causing numerous test failures")
    public void verifyExpectedWarningOnNavigateWithUncomittedChanges() throws Exception
    {
        goToProjectHome();
        String homeUrl = getDriver().getCurrentUrl();
        String listName = "dirtyListNavigationTest";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteIceCream", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteSnack", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        // edit an existing field
        domainFormPanel.getField("favoriteSnack")
                .setDescription("Exactly what it sounds like. Someone's fave frozen dairy goodness")
                .collapse();
        // create a new field
        domainFormPanel.addField("newField")
                .setDescription("Exactly what it sounds like. A new dang field");

        // capture the current url; ensure we don't navigate anywhere when we refresh or browse
        String currentUrl = getDriver().getCurrentUrl();

        // now refresh view, without saving
        getDriver().navigate().refresh();
        // dismiss the alert
        shortWait().until(ExpectedConditions.alertIsPresent());     // currently alert does not appear in FF; this is a known issue
        getDriver().switchTo().alert().dismiss();

        // make sure we are still here
        assertEquals(currentUrl, getDriver().getCurrentUrl());

        // now navigate away
        getDriver().navigate().to(homeUrl);
        // dismiss the alert
        shortWait().until(ExpectedConditions.alertIsPresent());
        getDriver().switchTo().alert().dismiss();
        // ensure current location
        assertEquals(currentUrl, getDriver().getCurrentUrl());

        domainDesignerPage.clickCancelWithUnsavedChanges().saveChanges(); // this should save the changes

        Connection cn = createDefaultConnection();
        DomainDetailsResponse response = dgen.getQueryHelper(cn).getDomainDetails();

        PropertyDescriptor faveSnackRow = getColumn(response.getDomain(), "favoriteSnack");
        PropertyDescriptor newFieldRow = getColumn(response.getDomain(), "newField");
        assertEquals("Exactly what it sounds like. Someone's fave frozen dairy goodness", faveSnackRow.getDescription());
        assertEquals("Exactly what it sounds like. A new dang field", newFieldRow.getDescription());
    }

    /**
     * verifies that when a user marks a field 'required' (and that field already has empty values in it) they are warned
     *
     * @throws Exception
     */
    @Test
    public void testUserWarningOnRequiredFieldWithEmptyValues() throws Exception
    {
        String sampleTypeName = "hasRowsWithBlankValuesWarnSampleType";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleTypeName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("color", FieldDefinition.ColumnType.String),
                        new FieldDefinition("manufacturer", FieldDefinition.ColumnType.String),
                        new FieldDefinition("volume", FieldDefinition.ColumnType.Decimal)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        dgen.addCustomRow(Map.of("name", "agar", "color", "green", "manufacturer", "glaxo", "volume", 2.34));
        dgen.addCustomRow(Map.of("name", "stuff", "color", "clear", "manufacturer", "glaxo", "volume", 2.34));
        dgen.addCustomRow(Map.of("name", "icecream", "color", "blue", "manufacturer", "glaxo", "volume", 2.34));
        dgen.addCustomRow(Map.of("name", "pbs", "color", "yellow", "manufacturer", "glaxo", "volume", 2.34));
        dgen.addCustomRow(Map.of("name", "slurm", "color", "orange", "volume", 2.34));  //<-- no value for manufacturer
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleTypeName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        // now attempt to mark 'manufacturer' field 'required'
        domainFormPanel.getField("manufacturer")
                .setRequiredField(true);
        // expect error warning here; this should warn the user
        domainDesignerPage.clickSaveExpectingErrors();
        String expectedWarning = domainDesignerPage.waitForAnyAlert();
        assertTrue(expectedWarning.contains("cannot be required when it contains rows with blank values."));
        domainDesignerPage.clickCancelWithUnsavedChanges().discardChanges();  // discard the changes to free the browser to go on to the next page
    }

    /**
     * verifies that a field with data (and no blank values) can be marked as 'required'
     *
     * @throws Exception
     */
    @Test
    public void testMarkFieldRequired() throws Exception
    {
        String sampleTypeName = "testSampleTypeWithRequiredField";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleTypeName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("color", FieldDefinition.ColumnType.String),
                        new FieldDefinition("manufacturer", FieldDefinition.ColumnType.String),
                        new FieldDefinition("volume", FieldDefinition.ColumnType.Decimal)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), SAMPLE_TYPE_DOMAIN_KIND);

        dgen.addCustomRow(Map.of("name", "agar", "color", "green", "manufacturer", "glaxo", "volume", 2.34));
        dgen.addCustomRow(Map.of("name", "stuff", "color", "clear", "manufacturer", "glaxo", "volume", 2.34));
        dgen.addCustomRow(Map.of("name", "icecream", "color", "blue", "manufacturer", "glaxo", "volume", 2.34));
        dgen.addCustomRow(Map.of("name", "pbs", "color", "yellow", "manufacturer", "glaxo", "volume", 2.34));
        dgen.addCustomRow(Map.of("name", "slurm", "color", "orange", "manufacturer", "slurmCo", "volume", 2.34));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleTypeName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        // now attempt to mark 'manufacturer' field 'required'
        domainFormPanel.getField("manufacturer")
                .setRequiredField(true);
        domainDesignerPage.clickFinish();

        Connection cn = createDefaultConnection();
        DomainDetailsResponse response = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor manufacturerRow = getColumn(response.getDomain(), "manufacturer");
        assertEquals("expect row to be marked 'required'", true, manufacturerRow.getRequired());
    }

    /**
     * confirms that clicking the name field does not expand the field row
     *
     * @throws Exception
     */
    @Test
    public void verifyNameFieldClickExpandsRow() throws Exception
    {
        String listName = "sillyListJustHereForTestPurposes";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen1 = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteIceCream", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteSnack", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen1.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        DomainFieldRow snackRow = domainFormPanel.getField("favoriteSnack");
        snackRow.nameInput()
                .getComponentElement().click();

        sleep(500); // wait just to be sure we are checking for expanded state correctly
        assertFalse("clicking the name field should not expand the field row", snackRow.isExpanded());
    }

    @Test
    public void testRangeValidator() throws Exception
    {
        String listName = "listForRangeValidator";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteIceCream", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteSnack", FieldDefinition.ColumnType.String),
                        new FieldDefinition("size", FieldDefinition.ColumnType.Integer)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "Key"));
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        DomainFieldRow sizeRow = domainFormPanel.getField("size");
        FieldDefinition.RangeValidator midsizeValidator = new FieldDefinition.RangeValidator("midsize", "falls between 2 and 3", "value must be 2 or 3",
                FieldDefinition.RangeType.GTE, "2",
                FieldDefinition.RangeType.LTE, "3");
        sizeRow.setRangeValidators(Arrays.asList(midsizeValidator));
        domainDesignerPage.clickFinish();

        // now verify the expected validator is formed and added to the field's validator array
        Connection cn = createDefaultConnection();
        DomainDetailsResponse response = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor sizeCol = getColumn(response.getDomain(), "size");
        Map<String, Object> validator = getPropertyValidator(sizeCol, "midsize");
        assertEquals("expect expression to be ", "~gte=2&~lte=3", validator.get("expression"));
        assertEquals("validator we just created should be new", true, validator.get("new"));
        assertEquals("expected description should be on the field",
                "falls between 2 and 3", validator.get("description"));
        assertEquals("expected error message should be on the field",
                "value must be 2 or 3", validator.get("errorMessage"));
    }

    @Test
    public void testConditionalFormat() throws Exception
    {
        String listName = "conditionalFormatList";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteIceCream", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteSnack", FieldDefinition.ColumnType.String),
                        new FieldDefinition("size", FieldDefinition.ColumnType.Integer)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "Key"));
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        dgen.addCustomRow(Map.of("name", "billy", "favoriteIceCream", "vanilla", "favoriteSnack", "apple", "size", 12));
        dgen.addCustomRow(Map.of("name", "jeffy", "favoriteIceCream", "chocolate", "favoriteSnack", "almond brittle", "size", 12));
        dgen.addCustomRow(Map.of("name", "alex", "favoriteIceCream", "strawberry", "favoriteSnack", "peanuts", "size", 12));
        dgen.insertRows(createDefaultConnection(), dgen.getRows()); // insert test data into the list

        DomainFieldRow favoriteSnack = domainFormPanel.getField("favoriteSnack");
        ConditionalFormatDialog formatDlg = favoriteSnack.clickConditionalFormatButton();
        formatDlg.getOpenFormatPanel()
                .setFirstCondition(Filter.Operator.DOES_NOT_CONTAIN)
                .setFirstValue("almond")
                .setItalicsCheckbox(true);
        formatDlg.clickApply();
        domainDesignerPage.clickFinish();

        // now verify the expected validator is formed and added to the field's validator array
        Connection cn = createDefaultConnection();
        DomainDetailsResponse response = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor faveSnackCol = getColumn(response.getDomain(), "favoriteSnack");
        Map<String, Object> validator = getConditionalFormats(faveSnackCol, "format.column~doesnotcontain=almond");
        assertEquals("expect italics to be set", true, validator.get("italic"));
        assertEquals("expect bold not to be set", false, validator.get("bold"));
        assertEquals("expect strikethrough not to be set", false, validator.get("strikethrough"));
    }

    @Test
    public void testRegexValidator() throws Exception
    {
        String listName = "regexValidatorList";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteIceCream", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteSnack", FieldDefinition.ColumnType.String),
                        new FieldDefinition("size", FieldDefinition.ColumnType.Integer)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "Key"));
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow favoriteSnack = domainFormPanel.getField("favoriteSnack");

        RegexValidatorDialog validatorDialog = favoriteSnack.clickRegexButton();
        RegexValidatorPanel panel = validatorDialog.getValidationPanel();
        String expression = "twizzler";
        panel.setExpression(expression)
                .setDescription("twizzler is not a snack")
                .setErrorMessage("favorite snack cannot be twizzlers, yo")
                .setFailOnMatch(true)
                .setName("neverTwizzlers");
        validatorDialog.clickApply();
        domainDesignerPage.clickFinish();

        // now verify the expected validator is formed and added to the field's validator array
        Connection cn = createDefaultConnection();
        DomainDetailsResponse response = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor faveSnackCol = getColumn(response.getDomain(), "favoriteSnack");
        Map<String, Object> specialCharsValidator = getPropertyValidator(faveSnackCol, "neverTwizzlers");
        assertEquals("validator we just created should be new", true, specialCharsValidator.get("new"));
        assertEquals("expected expression should be on the field", expression, specialCharsValidator.get("expression"));
        assertEquals("expected description should be on the field",
                "twizzler is not a snack", specialCharsValidator.get("description"));
        assertEquals("expected error message should be on the field",
                "favorite snack cannot be twizzlers, yo", specialCharsValidator.get("errorMessage"));

        // this test does not verify that attempts to insert values that match will get an error
    }

    @Test
    public void addUpdateRemoveRegexValidator() throws Exception
    {
        String listName = "regexCrudList";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("favoriteSnack", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "Key"));
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow favoriteSnack = domainFormPanel.getField("favoriteSnack");

        RegexValidatorDialog validatorDialog = favoriteSnack.clickRegexButton();
        RegexValidatorPanel panel1 = validatorDialog.getValidationPanel();
        String expression1 = ".*[twizzler]+.*";
        panel1.setExpression(expression1)
                .setDescription("twizzler is not a snack")
                .setErrorMessage("favorite snack cannot be twizzlers, yo")
                .setFailOnMatch(true)
                .setName("neverTwizzlers");
        String expression2 = ".*[!@#$%^]+.*";
        RegexValidatorPanel panel2 = validatorDialog.addValidationPanel("specialChars")
                .setDescription("matches on any special character substrings in the input")
                .setExpression(expression2)
                .setFailOnMatch(false)
                .setErrorMessage("no special characters in this field, please");
        String expression3 = ".*[<>]+.*";
        RegexValidatorPanel panel3 = validatorDialog.addValidationPanel("angleBrackets")
                .setDescription("matches on any angle bracket substrings in the input")
                .setExpression(expression3)
                .setFailOnMatch(false)
                .setName("angleBrackets")
                .setErrorMessage("no angle brackets in this field, please");
        validatorDialog.clickApply();
        domainDesignerPage.clickFinish();

        // now verify the expected validator is formed and added to the field's validator array
        Connection cn1 = createDefaultConnection();
        DomainDetailsResponse response = dgen.getQueryHelper(cn1).getDomainDetails();
        PropertyDescriptor faveSnackCol = getColumn(response.getDomain(), "favoriteSnack");
        Map<String, Object> specialCharsValidator = getPropertyValidator(faveSnackCol, "specialChars");
        assertEquals("validator we just created should be new", true, specialCharsValidator.get("new"));
        assertEquals("expected expression should be on the field", expression2, specialCharsValidator.get("expression"));
        assertEquals("expected description should be on the field",
                "matches on any special character substrings in the input", specialCharsValidator.get("description"));
        assertEquals("expected error message should be on the field",
                "no special characters in this field, please", specialCharsValidator.get("errorMessage"));

        // reopen, edit one, remove another
        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        RegexValidatorDialog valDialog = domainDesignerPage.fieldsPanel()
                .getField("favoriteSnack")
                .clickRegexButton();
        valDialog.getValidationPanel(2) // will get 'angleBrackets' field, it's 3rd
                .clickRemove();
        RegexValidatorPanel specialValidatorPanel = valDialog
                .getValidationPanel(1)  // will get the 'specialChars' field, it's 2nd
                .setFailOnMatch(true);

        valDialog.clickApply();
        domainDesignerPage.clickFinish();
        waitAndClickAndWait(Locator.linkWithText(listName));    // give it time by navigating to the list
        DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();

        // now confirm 2 validators on the field
        Connection cn = createDefaultConnection();
        DomainDetailsResponse newResponse = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor snackField = getColumn(newResponse.getDomain(), "favoriteSnack");
        List<Map<String, Object>> validators = (List<Map<String, Object>>) snackField.getAllProperties().get("propertyValidators");

        // Domain designer UI only handles Range, Regex and Lookup validators
        validators = validators.stream().filter(val ->
                (val.get("type").equals("RegEx") || val.get("type").equals("Range") || val.get("type").equals("Lookup"))).collect(Collectors.toList());

        Map<String, Object> twiz = getPropertyValidator(snackField, "neverTwizzlers");
        Map<String, Object> spec = getPropertyValidator(snackField, "specialChars");

        // no validator with name 'angleBrackets' exists now
        assertEquals(0, validators.stream().filter(a -> a.get("name").equals("angleBrackets")).collect(Collectors.toList()).size());
        // why not just verify just 2 validators on the field now?  ...because apparently when removed, it becomes a filter on text size is less-than-or-equal-to field cap
        // ...but that looks like a regression of issue 38598, we're tracking it as 38662
        assertEquals("issue 38662", 2, validators.size());
    }

    @Test
    public void addUpdateRemoveRangeValidator() throws Exception
    {
        String listName = "rangeValidatorCrudList";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("size", FieldDefinition.ColumnType.Integer)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "Key"));
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow size = domainFormPanel.getField("size");

        RangeValidatorDialog sizeDialog = size.clickRangeButton();
        sizeDialog.getValidationPanel(0)
                .setFirstCondition(Filter.Operator.LTE)
                .setFirstValue("2")
                .setName("lte2");
        sizeDialog.addValidationPanel("equals3")
                .setFirstCondition(Filter.Operator.EQUAL)
                .setFirstValue("3");
        sizeDialog.addValidationPanel("gte4")
                .setFirstCondition(Filter.Operator.GTE)
                .setFirstValue("4");
        sizeDialog.clickApply();
        domainDesignerPage.clickFinish();

        // now verify we have 3 formats on the size field
        Connection cn1 = createDefaultConnection();
        DomainDetailsResponse response = dgen.getQueryHelper(cn1).getDomainDetails();
        PropertyDescriptor sizeCol = getColumn(response.getDomain(), "size");
        Map<String, Object> lte2 = getPropertyValidator(sizeCol, "lte2");
        Map<String, Object> equals3 = getPropertyValidator(sizeCol, "equals3");
        Map<String, Object> gte4 = getPropertyValidator(sizeCol, "gte4");

        // now reopen the page, edit 2 delete another
        // get back to the domain designer
        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        RangeValidatorDialog dlg = domainDesignerPage.fieldsPanel().getField("size")
                .clickRangeButton();
        dlg.getValidationPanel(0)       //lte2
                .setDescription("2 or less");
        dlg.getValidationPanel(1)       //equals3
                .setDescription("equals 3");
        dlg.getValidationPanel(2)       //gte4
                .clickRemove();
        dlg.clickApply();
        domainDesignerPage.clickFinish();
        waitAndClickAndWait(Locator.linkWithText(listName));        // wait for navigation and page load before using the API call to get the domain
        DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();

        // now verify we have 2 formats on the size field
        Connection cn = createDefaultConnection();
        DomainDetailsResponse updatedResponse = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor updatedSizeCol = getColumn(updatedResponse.getDomain(), "size");
        List<Map<String, Object>> validators = (List<Map<String, Object>>) updatedSizeCol.getAllProperties().get("propertyValidators");
        assertEquals("expect only 2 validators on the field", 2, validators.size());
        Map<String, Object> editedLte2 = getPropertyValidator(updatedSizeCol, "lte2");
        Map<String, Object> editedEquals3 = getPropertyValidator(updatedSizeCol, "equals3");

        assertEquals("expect description edit to take", "2 or less", editedLte2.get("description"));
        assertEquals("expect description edit to take", "equals 3", editedEquals3.get("description"));
    }

    @Test
    public void addUpdateRemoveConditionalFormat() throws Exception
    {
        String listName = "conditionalFormatCrudList";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("superHero", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "Key"));
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        DomainFieldRow superHero = domainFormPanel.getField("superHero");

        ConditionalFormatDialog formatDialog = superHero.clickConditionalFormatButton();
        formatDialog.getOpenFormatPanel()
                .setFirstCondition(Filter.Operator.EQUAL)
                .setFirstValue("Thor")
                .setBoldCheckbox(true);
        formatDialog.addFormatPanel()
                .setFirstCondition(Filter.Operator.EQUAL)
                .setFirstValue("Aquaman")
                .setItalicsCheckbox(true);
        formatDialog.addFormatPanel()
                .setFirstCondition(Filter.Operator.EQUAL)
                .setFirstValue("IronMan")
                .setStrikethroughCheckbox(true);
        formatDialog.clickApply();
        domainDesignerPage.clickFinish();

        // now verify we have 3
        Connection cn1 = createDefaultConnection();
        DomainDetailsResponse response = dgen.getQueryHelper(cn1).getDomainDetails();
        PropertyDescriptor heroCol = getColumn(response.getDomain(), "superHero");
        Map<String, Object> thorMap = getConditionalFormats(heroCol, "format.column~eq=Thor");
        Map<String, Object> aquaMap = getConditionalFormats(heroCol, "format.column~eq=Aquaman");
        Map<String, Object> ironMap = getConditionalFormats(heroCol, "format.column~eq=IronMan");

        // get back to the domain designer
        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        ConditionalFormatDialog dlg = domainDesignerPage.fieldsPanel().getField("superHero")
                .clickConditionalFormatButton();
        dlg.getPanelByIndex(2)  // ironman
                .clickRemove();
        dlg.getPanelByIndex(0) // thor
                .setItalicsCheckbox(true);
        dlg.getPanelByIndex(1)  // aquaman
                .setBoldCheckbox(true);
        dlg.clickApply();
        domainDesignerPage.clickFinish();

        Connection cn = createDefaultConnection();
        DomainDetailsResponse validationResponse = dgen.getQueryHelper(cn).getDomainDetails();
        PropertyDescriptor editedHeroCol = getColumn(validationResponse.getDomain(), "superHero");

        List<Map<String, Object>> formats = (List<Map<String, Object>>) editedHeroCol.getAllProperties().get("conditionalFormats");
        assertEquals(2, formats.size());

        Map<String, Object> editedThor = getConditionalFormats(editedHeroCol, "format.column~eq=Thor");
        assertEquals(true, editedThor.get("bold"));
        assertEquals(true, editedThor.get("italic"));
        Map<String, Object> editedAquamap = getConditionalFormats(editedHeroCol, "format.column~eq=Aquaman");
        assertEquals(true, editedAquamap.get("bold"));
        assertEquals(true, editedAquamap.get("bold"));
    }

    @Test
    public void testExportImportListFields() throws Exception
    {
        String listName = "exportFieldsTestList";

        // create a list
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        List<PropertyDescriptor> fields = importExportTestFields();
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(fields);
        // collect the fields from the server response
        List<PropertyDescriptor> createdFields = dgen.createList(createDefaultConnection(), "Key")
                .getDomain().getFields();
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        // export the fields via the UI
        File exportFile = domainFormPanel.clickExportFields();
        List<String> uiFields = domainFormPanel.fieldNames();
        Map<String, PropertyDescriptor> exportedFields = getFieldsFromExportFile(exportFile);

        // confirm that the ones exported match the ones on the server
        for (PropertyDescriptor createdField : createdFields)  // created fields will include Key, which wasn't passed in but we do want to validate
        {
            String fieldName = createdField.getName();

            if (!fieldName.equals("Key"))
            {
                // compare the field definition passed in to the one that came back
                PropertyDescriptor intendedField = fields.stream().filter(a -> a.getName().equals(fieldName)).findFirst().orElse(null);
                assertNotNull("a field that wasn't specified [" + fieldName + "] was created", intendedField);
                verifyExpectedFieldProperties(intendedField, createdField);
            }
            // compare the created field properties to the exported field properties
            PropertyDescriptor exportedField = exportedFields.get(fieldName);
            assertNotNull("expected field [" + fieldName + "] was not exported", exportedField);
            verifyExpectedFieldProperties(createdField, exportedField);
        }

        // now create a list with the set of created Keys
        String roundTripList = "roundTripListNameCreatedFromJsonFields";
        EditListDefinitionPage listDefPage = _listHelper.beginCreateList(getProjectName(), roundTripList);
        listDefPage.getFieldsPanel()
                .setInferFieldFile(exportFile);
        listDefPage.selectKeyField("Key");
        listDefPage.clickSave();

        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", roundTripList);
        assertThat("expect list created from export to have the same fields",
                domainDesignerPage.fieldsPanel().fieldNames(), is(uiFields));
    }

    @Test
    public void testFileImportFieldsToNewSampleType() throws Exception
    {
        String sampleType = "importFieldsTestSampleType";

        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "exp.materials", sampleType);
        List<PropertyDescriptor> fields = importExportTestFields();
        fields.add(new FieldDefinition("name", FieldDefinition.ColumnType.String)); // need to add a 'name' field to a sampletype
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(fields);
        // get the fields from the server after creating the sampletype on the server
        List<PropertyDescriptor> createdFields = dgen.createDomain(createDefaultConnection(), "SampleSet")
                .getDomain().getFields();
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleType);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        String roundTripSampleType = "testSampleTypeWithImportedFields";
        File exportFile = domainFormPanel.clickExportFields();
        List uiFields = domainFormPanel.fieldNames();       // capture the order of the fields in the UI; we will use this to validate ordering for import/export
        ArrayListMap<String, PropertyDescriptor> exportedFields = getFieldsFromExportFile(exportFile);
        CreateSampleTypePage sampleTypeCreatePage = CreateSampleTypePage.beginAt(this, getProjectName());
        sampleTypeCreatePage.setName(roundTripSampleType);
        sampleTypeCreatePage.getFieldsPanel()
                .setInferFieldFile(exportFile);
        sampleTypeCreatePage.clickSave();

        DomainDesignerPage sampleTypeDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", roundTripSampleType);
        File roundTripFile = sampleTypeDesignerPage.fieldsPanel().clickExportFields();
        ArrayListMap<String, PropertyDescriptor> roundTrippedFields = getFieldsFromExportFile(roundTripFile);

        for (PropertyDescriptor intendedField : fields) // fields are the ones we first sent in when creating the domain
        {
            String fieldName = intendedField.getName();

            // first check that properties that came back from domain creation match what we sent in
            if (!fieldName.equals("name")) // name is a magic column that must be specified when creating a sampleType
            {                        // but which is never on the domain, cannot be added or imported or exported
                PropertyDescriptor createdField = createdFields.stream().filter(a -> a.getName().equals(fieldName)).findFirst().orElse(null);
                assertNotNull("expected field [" + fieldName + "] was not created", createdField);
                verifyExpectedFieldProperties(intendedField, createdField);

                // now confirm that properties come back from export still matching
                PropertyDescriptor exportedField = roundTrippedFields.get(fieldName);
                assertNotNull("expected field [" + intendedField.getName() + "] was not exported", exportedField);
                verifyExpectedFieldProperties(intendedField, exportedField);
            }
        }

        // ensure exported fields order the same as they appear on the page, and that this also applies when imported

        for (int i = 0; i < uiFields.size(); i++)
        {
            assertThat("Expect exported fields to be in the same order as their original UI",
                    exportedFields.get(i).getName(), is(uiFields.get(i)));
            assertThat("Expect round-trip fields to retain their original order",
                    roundTrippedFields.get(i).getName(), is(uiFields.get(i)));
        }
        assertThat(exportedFields.keySet(), is(roundTrippedFields.keySet()));
    }

    @Test
    public void testImportListFieldsToNonList() throws Exception
    {
        String listName = "listForFieldImportKeyTest";

        // create a list
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        List<PropertyDescriptor> fields = importExportTestFields();
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(fields);
        // collect the fields from the server response
        List<PropertyDescriptor> createdFields = dgen.createList(createDefaultConnection(), "Key")
                .getDomain().getFields();
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        File exportFile = domainFormPanel.clickExportFields();

        String sampleTypeName = "sampleTypeWithNonKeyKeyField";
        CreateSampleTypePage createSampleTypePage = CreateSampleTypePage.beginAt(this, getProjectName());
        createSampleTypePage.setName(sampleTypeName)
                .setDescription("created by exporting fields from a list with PK 'Key' field")
                .getFieldsPanel()
                .setInferFieldFile(exportFile);
        createSampleTypePage.clickSave();

        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "exp.materials", sampleTypeName);
        File sampleTypeFields = domainDesignerPage.fieldsPanel().clickExportFields();
        ArrayListMap<String, PropertyDescriptor> sampleFields = getFieldsFromExportFile(sampleTypeFields);
        PropertyDescriptor keyField = sampleFields.get("Key");
        assertFalse("Expect field import to disregard PK on fields imported to SampleType",
                (Boolean) keyField.getAllProperties().get("isPrimaryKey"));
    }

    private List<PropertyDescriptor> importExportTestFields()
    {
        ConditionalFormatFilter gtFive = new ConditionalFormatFilter("5", Filter.Operator.GT);
        FieldDefinition.RangeValidator soonValidator = new FieldDefinition.RangeValidator("soonValidator", "between now and 2 days from now", "aaaa",
                FieldDefinition.RangeType.GTE, "2020-11-04",
                FieldDefinition.RangeType.LTE, "2020-11-06");

        List<PropertyDescriptor> fields = new ArrayList<>();
        fields.add(new FieldDefinition("textField", FieldDefinition.ColumnType.String)
                .setDescription("is texty").setLabel("text field"));
        fields.add(new FieldDefinition("booleanField", FieldDefinition.ColumnType.Boolean)
                .setLabel("Boolean Field")
                .setRequired(true)
                .setURL("fake/list-def.url"));
        fields.add(new FieldDefinition("attachmentField", FieldDefinition.ColumnType.Attachment)
                .setHidden(true));
        fields.add(new FieldDefinition("decimalField", FieldDefinition.ColumnType.Decimal).
                setScale(1234).setFormat("0.####"));
        fields.add(new FieldDefinition("intField", FieldDefinition.ColumnType.Integer)
                .setMvEnabled(true)
                .setConditionalFormats(List.of(new ConditionalFormat(List.of(gtFive), true, true, false))));
        fields.add(new FieldDefinition("dateTimeField", FieldDefinition.ColumnType.DateAndTime)
                .setValidators(List.of(soonValidator)));
        return fields;
    }

    private void verifyExpectedFieldProperties(PropertyDescriptor intendedField, PropertyDescriptor actualField)
    {
        log("verifying properties for field [" + intendedField.getName() + "]");
        checker().verifyEquals("Description does not match expected",
                intendedField.getDescription(), actualField.getDescription());
        checker().verifyEquals("Label does not match intended label",
                intendedField.getLabel(), actualField.getLabel());
        checker().verifyEquals("Required bit does not match expected",
                intendedField.getRequired(), actualField.getRequired());
        checker().verifyEquals("Hidden value does not match expected",
                intendedField.getHidden(), actualField.getHidden());
        checker().verifyEquals("MvEnabled bit does not match expected",
                nullToFalse(intendedField.getMvEnabled()), actualField.getMvEnabled());
        checker().verifyEquals("Dimension does not match expected",
                nullToFalse(intendedField.getDimension()), actualField.getDimension());
        checker().verifyEquals("PHI does not match specified value",
                (intendedField.getPHI() == null ? FieldDefinition.PhiSelectType.NotPHI.name() : intendedField.getPHI()),
                actualField.getPHI());
        checker().verifyEquals("Expect measure bit to match intended measure",
                nullToFalse(intendedField.getMeasure()), actualField.getMeasure());
        checker().verifyEquals("Format property should export",
                intendedField.getFormat(), actualField.getFormat());
        checker().verifyEquals("Key field property should export",
                nullToFalse(intendedField.getAllProperties().get("isPrimaryKey")),
                nullToFalse(actualField.getAllProperties().get("isPrimaryKey")));

        Assertions.assertThat(actualField.getConditionalFormats().stream().map(f -> f.toJSON().toMap()))
                .as("Exported conditional fields.")
                .containsExactlyElementsOf(intendedField.getConditionalFormats().stream().map(f -> f.toJSON().toMap()).toList());

        // would like to do validators, but this part of the remoteAPI is incomplete; validators are settable
        // on FieldDefinition, but not gettable on PropertyDescriptor
    }

    private Object nullToFalse(Object val)
    {
        return val == null ? false : val;
    }

    private ArrayListMap<String, PropertyDescriptor> getFieldsFromExportFile(File exportFile) throws IOException
    {
        try (Reader reader = new FileReader(exportFile, StandardCharsets.UTF_8))
        {
            JSONArray jsonArray = new JSONArray(new JSONTokener(reader));
            ArrayListMap<String, PropertyDescriptor> exportFields = new ArrayListMap<>();

            for (int i = 0; i < jsonArray.length(); i++)
            {
                PropertyDescriptor field = new PropertyDescriptor(jsonArray.getJSONObject(i));
                exportFields.put(field.getName(), field);
            }

            return exportFields;
        }
    }

    public PropertyDescriptor getColumn(Domain domain, String columnName)
    {
        PropertyDescriptor descriptor = domain.getFields().stream().filter(desc -> desc.getName().equals(columnName)).findFirst().orElse(null);
        assertNotNull("Didn't find expected columns", descriptor);

        return descriptor;
    }


    public Map<String, Object> getPropertyValidator(PropertyDescriptor column, String name)
    {
        List<Map<String, Object>> validators = (List<Map<String, Object>>) column.getAllProperties().get("propertyValidators");
        Map<String, Object> validator = validators.stream()
                .filter(a -> a.get("name").equals(name))
                .findFirst().orElse(null);
        assertNotNull("did not find property validator [" + name + "] on column. Column properties: " + column.getAllProperties().toString(), validator);
        return validator;
    }

    public Map<String, Object> getConditionalFormats(PropertyDescriptor column, String filterExpression)
    {
        List<Map<String, Object>> formats = (List<Map<String, Object>>) column.getAllProperties().get("conditionalFormats");
        Map<String, Object> conditionalFormat = formats.stream()
                .filter(a -> a.get("filter").equals(filterExpression))
                .findFirst().orElse(null);
        assertNotNull("did not find conditionalFormat [" + filterExpression + "] on column. Column properties: " + column.getAllProperties().toString(), conditionalFormat);
        return conditionalFormat;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "DomainDesignerTest Project" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}

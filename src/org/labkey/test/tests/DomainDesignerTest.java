package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.GetDomainCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.DomainFieldRow;
import org.labkey.test.components.DomainFormPanel;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class})
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
    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
        new PortalHelper(getDriver()).addBodyWebPart("Sample Sets");
        new PortalHelper(getDriver()).addBodyWebPart("Lists");
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

        DomainFieldRow integerRow = domainDesignerPage
                .fieldProperties(listName)
                .addField("integerField")
                .setType("Integer")
                .expand()
                .setNumberFormat("###,###,###")
                .setScaleType(PropertiesEditor.ScaleType.LINEAR)
                .setDescription("field for an Integer")
                .setLabel("IntegerFieldLabel");

        DomainFieldRow decimalRow = domainDesignerPage
                .fieldProperties(listName)
                .addField("decimalField")
                .setType("Decimal")
                .expand()
                .setNumberFormat("###,###,###.000")
                .setScaleType(PropertiesEditor.ScaleType.LOG)
                .setDescription("field for a decimal")
                .setLabel("DecimalField");
        domainDesignerPage.clickSaveChanges();

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
        domainDesignerPage.clickSaveChanges();

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
        domainDesignerPage.clickSaveChanges();

        GetDomainCommand domainCommand = new GetDomainCommand("exp.materials", sampleSet);
        DomainResponse afterResponse = domainCommand.execute(createDefaultConnection(true), getProjectName());
        List<Map<String, Object>> remainingFields = afterResponse.getColumns();
        assertEquals("expect only field in the domain to have been deleted", 0, remainingFields.size());
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
        domainDesignerPage.clickSaveChanges();

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

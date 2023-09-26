package org.labkey.test.tests.experiment;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.query.SourceQueryPage;
import org.labkey.test.pages.query.UpdateQueryRowPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@Category({Daily.class})
public class SampleTypeLookupDisplayColumnTest extends BaseWebDriverTest
{
    private final String TEST_LOOKUP_SAMPLETYPE = "lookupsampletype";
    private final String TEST_INGREDIENT_LIST = "ingredientList";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        SampleTypeLookupDisplayColumnTest init = (SampleTypeLookupDisplayColumnTest) getCurrentTest();

        init.doSetup();

        // create a list with an integer PK
        List<FieldDefinition> listColumns = Arrays.asList(
                new FieldDefinition("intColumn", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("textColumn", FieldDefinition.ColumnType.String),
                new FieldDefinition("titleColumn", FieldDefinition.ColumnType.String));
        TestDataGenerator dgen = new TestDataGenerator("lists", init.TEST_INGREDIENT_LIST, init.getProjectName())
                .withColumns(listColumns);
        dgen.addCustomRow(Map.of("intColumn", 1,
                "textColumn", "heavy heavy gas",
                "titleColumn", "sodium hexafluoride"));
        dgen.addCustomRow(Map.of("intColumn", 2,
                "textColumn", "inert neutral gas",
                "titleColumn", "molecular nitrogen"));
        dgen.addCustomRow(Map.of("intColumn", 3,
                "textColumn", "inert light gas",
                "titleColumn", "helium"));
        dgen.createList(init.createDefaultConnection(), "key");
        dgen.insertRows();

        // create a sampleType with a lookup column to the issue tracker
        List<FieldDefinition> testColumns = Arrays.asList(
                new FieldDefinition("comment", FieldDefinition.ColumnType.String),
                new FieldDefinition("ingredient", new FieldDefinition.LookupInfo(init.getProjectName(), "lists", init.TEST_INGREDIENT_LIST)
                        .setTableType(FieldDefinition.ColumnType.Integer)));
        SampleTypeDefinition sampleTypeDef = new SampleTypeDefinition(init.TEST_LOOKUP_SAMPLETYPE)
                .setFields(testColumns)
                .setNameExpression("S-${genId}");
        SampleTypeAPIHelper.createEmptySampleType(init.getProjectName(), sampleTypeDef);

        new PortalHelper(init.getDriver()).addBodyWebPart("Sample Types");
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testLookupByIntColumn()
    {
        // specify fkDisplayColumnName in metadata in the list
        setFKDisplayColumnName("ingredient", "intColumn");
        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        sampleTypeList.clickInsertNewRow();

        String comment = "inserted with lookup by ingredient.intcolumn";
        setFieldValues("intSample", comment, "3.5", "2");

        sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        var row = sampleTypeList.getRowDataAsMap("Comment", comment);
        assertEquals("Expect Ingredient display to respect fKDisplayCol setting of intColumn",
                "2", row.get("Ingredient"));
    }

    @Test
    public void testLookupByTextColumn()
    {
        // specify fkDisplayColumnName in metadata in the list
        setFKDisplayColumnName("ingredient", "titleColumn");
        DataRegionTable sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        sampleTypeList.clickInsertNewRow();

        String comment = "inserted with lookup by ingredient.titleColumn";
        setFieldValues("nameSample", comment, "3.5", "sodium hexafluoride");

        sampleTypeList =  DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        var row = sampleTypeList.getRowDataAsMap("Comment", comment);
        assertEquals("Expect Ingredient display to respect fKDisplayCol setting of titleColumn",
                "sodium hexafluoride", row.get("Ingredient"));

        sampleTypeList.clickEditRow(sampleTypeList.getRowIndex("Name", "nameSample"))
                .setField("comment", "Switching to Helium to prove edit respects fkDisplay")
                .setField("ingredient", "helium")
                .submit();

        var updatedRow = sampleTypeList.getRowDataAsMap("Name", "nameSample");
        assertEquals("Expect updated ingredient displayed as titleColumn",
                "helium", updatedRow.get("Ingredient"));

    }

    private void setFieldValues(String name, String comment, String amount, String ingredient)
    {
        var insertPage = new UpdateQueryRowPage(getDriver());
        if (name != null)   // for update, name field is disabled
            insertPage.setField("Name", name);
        insertPage.setField("comment", comment);
        insertPage.setField("StoredAmount", amount);
        insertPage.setField("ingredient", ingredient);
        insertPage.submit();
    }

    private void setFKDisplayColumnName(String lookupColumn, String fkDisplayColumnName)
    {
        String xmlMetaData = "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
                "  <table tableName=\""+ TEST_LOOKUP_SAMPLETYPE + "\" tableDbType=\"NOT_IN_DB\">\n" +
                "    <columns>\n" +
                "      <column columnName=\""+lookupColumn+"\">\n" +
                "        <fk>\n" +
                "          <fkDbSchema>lists</fkDbSchema>\n" +
                "          <fkTable>"+TEST_INGREDIENT_LIST+"</fkTable>\n" +
                "          <fkDisplayColumnName>"+fkDisplayColumnName+"</fkDisplayColumnName>\n" +
                "        </fk>\n" +
                "      </column>\n" +
                "    </columns>\n" +
                "  </table>\n" +
                "</tables>";
        var sourceQueryPage = SourceQueryPage.beginAt(this, getProjectName(), "samples", TEST_LOOKUP_SAMPLETYPE);
        sourceQueryPage.waitForElement(Locator.id("query-editor-panel"));
        sourceQueryPage
                .setMetadataXml(xmlMetaData)
                .clickSaveAndFinish();
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "SampleTypeLookupDisplayColumnTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}

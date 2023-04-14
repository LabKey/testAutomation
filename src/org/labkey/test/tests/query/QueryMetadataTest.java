package org.labkey.test.tests.query;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.pages.query.EditMetadataPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.params.list.IntListDefinition;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@Category({})
public class QueryMetadataTest extends BaseWebDriverTest
{
    static public String TEST_LIST = "queryMetadataTestList";
    static public String TEST_SAMPLES = "queryMetadataSamples";

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        QueryMetadataTest init = (QueryMetadataTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Collaboration");

        // create a sampleType to look up to
        List<FieldDefinition> fields = List.of(
                new FieldDefinition("stringField", FieldDefinition.ColumnType.String),
        new FieldDefinition("boolField", FieldDefinition.ColumnType.Boolean),
        new FieldDefinition("decimalField", FieldDefinition.ColumnType.Decimal));
        var samplesDgen = new SampleTypeDefinition(TEST_SAMPLES)
                .setFields(fields)
                .create(createDefaultConnection(), getProjectName())
                .withGeneratedRows(10);
        samplesDgen.insertRows();
        // create a list

        List<FieldDefinition> listColumns = Arrays.asList(
                new FieldDefinition("name", FieldDefinition.ColumnType.String),
                new FieldDefinition("value", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("users", FieldDefinition.ColumnType.User),
                new FieldDefinition("date", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("samples", new FieldDefinition.IntLookup(getProjectName(),"exp.materials", TEST_SAMPLES)),
                new FieldDefinition("selfLookup", new FieldDefinition.IntLookup(getProjectName(),"lists", TEST_LIST)));
        var dgen = new IntListDefinition(TEST_LIST, "Key").setFields(listColumns)
                .create(createDefaultConnection(), getProjectName());
    }

    @Test
    public void testUpdateLookupFields()
    {
        var editPage = EditMetadataPage.beginAt(this, getProjectName(), "lists", TEST_LIST);
        editPage.fieldsPanel()
                .getField("selfLookup")
                .setLabel("SelfLookup")
                .setLookupValidatorEnabled(true)     // is a change, but does not appear in metadata view
                .setLookup(new FieldDefinition.IntLookup(null, "lists", TEST_LIST)) // non-change
                .setDescription("has new description");
        editPage.fieldsPanel()
                .getField("samples")
                .setLabel("SamplesLookup")
                .setLookup(new FieldDefinition.IntLookup(getProjectName(), "samples", TEST_SAMPLES))
                .setLookupValidatorEnabled(true);   // is a change but does not appear in metadata view
        editPage.clickSave();

        String expectedXml = "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
                "  <table tableName=\"queryMetadataTestList\" tableDbType=\"NOT_IN_DB\">\n" +
                "    <columns>\n" +
                "      <column columnName=\"samples\">\n" +
                "        <columnTitle>SamplesLookup</columnTitle>\n" +
                "        <fk>\n" +
                "          <fkDbSchema>samples</fkDbSchema>\n" +
                "          <fkTable>queryMetadataSamples</fkTable>\n" +
                "          <fkColumnName>RowId</fkColumnName>\n" +
                "          <fkFolderPath>/QueryMetadataTest Project</fkFolderPath>\n" +
                "        </fk>\n" +
                "      </column>\n" +
                "      <column columnName=\"selfLookup\">\n" +
                "        <description>has new description</description>\n" +
                "        <columnTitle>SelfLookup</columnTitle>\n" +
                "      </column>\n" +
                "    </columns>\n" +
                "  </table>\n" +
                "</tables>";
        var queryPage = editPage.clickEditSource();
        checker().withScreenshot("xml_mismatch")
                .verifyEquals("expect xml to show only the delta for value description",
                expectedXml, queryPage.getMetadataXml());

        // clean up after
        EditMetadataPage.beginAt(this, getProjectName(), "lists", TEST_LIST).resetToDefault();
    }

    @Test
    public void testSaveDescriptionUpdate()
    {
        var editPage = EditMetadataPage.beginAt(this, getProjectName(), "lists", TEST_LIST);
        editPage.fieldsPanel()
                .getField("value")
                .setDescription("has new description");
        editPage.clickSave();

        String expectedXml = "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
                "  <table tableName=\"queryMetadataTestList\" tableDbType=\"NOT_IN_DB\">\n" +
                "    <columns>\n" +
                "      <column columnName=\"value\">\n" +
                "        <description>has new description</description>\n" +
                "      </column>\n" +
                "    </columns>\n" +
                "  </table>\n" +
                "</tables>";

        var queryPage = editPage.clickEditSource();
        checker().withScreenshot("xml_mismatch")
                .verifyEquals("expect xml to show only the delta for value description",
                expectedXml, queryPage.getMetadataXml());

        // clean up after
        EditMetadataPage.beginAt(this, getProjectName(), "lists", TEST_LIST).resetToDefault();
    }

    /*
        EditMetadataPage allows you to edit multiple field properties
        ensure that 'reset to default' from the editMetadataPage
     */
    @Test
    public void testResetToDefault()
    {
        var editPage = EditMetadataPage.beginAt(this, getProjectName(), "lists", TEST_LIST);
        editPage.fieldsPanel()
                .getField("date")
                .setDescription("has new description");
        editPage.fieldsPanel()
                .getField("value")
                .setLabel("ValueLabel");
        editPage.clickSave();
        // verify/confirm success
        editPage.waitForSuccess();
        var queryPage = editPage.clickEditSource();
        String expectedXml = "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
                "  <table tableName=\"queryMetadataTestList\" tableDbType=\"NOT_IN_DB\">\n" +
                "    <columns>\n" +
                "      <column columnName=\"value\">\n" +
                "        <columnTitle>ValueLabel</columnTitle>\n" +
                "      </column>\n" +
                "      <column columnName=\"date\">\n" +
                "        <description>has new description</description>\n" +
                "      </column>\n" +
                "    </columns>\n" +
                "  </table>\n" +
                "</tables>";
        var actualXml = queryPage.getMetadataXml();
        checker().withScreenshot("initial_edits")
            .wrapAssertion(()-> assertThat(actualXml)
            .as("expect xml to show only the delta for value description")
            .isEqualTo(expectedXml));
        queryPage.clickSave();
        queryPage.goBack();

        editPage = new EditMetadataPage(getDriver());

        // now ensure that 'reset to default' clears the xml delta
        editPage.resetToDefault();
        queryPage = editPage.clickEditSource();

        String expectedAfterXml =
                "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
                        "  <table tableName=\"queryMetadataTestList\" tableDbType=\"NOT_IN_DB\">\n" +
                        "    <columns>\n" +
                        "    </columns>\n" +
                        "  </table>\n" +
                        "</tables>\n";
        var actualAfterXml = queryPage.getMetadataXml();
        checker().withScreenshot("after_edits")
            .wrapAssertion(()-> assertThat(actualAfterXml)
            .as("expect xml to show only the delta for value description")
            .isEqualTo(expectedAfterXml));
    }



    @Override
    protected String getProjectName()
    {
        return "QueryMetadataTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}

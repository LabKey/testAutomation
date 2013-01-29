package org.labkey.test.tests;

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelperWD;
import org.labkey.test.util.LogMethod;

/**
 * User: kevink
 * Date: 1/28/13
 */
public class LinkedSchemaTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = LinkedSchemaTest.class.getSimpleName() + "Project";
    private static final String SOURCE_FOLDER = "SourceFolder";
    private static final String TARGET_FOLDER = "TargetFolder";

    public static final String LIST_NAME = "People";
    public static final String LIST_DATA = "Name\tAge\tCrazy\n" +
            "Dave\t39\tTrue\n" +
            "Adam\t65\tTrue\n" +
            "Britt\t30\tFalse\n" +
            "Josh\t30\tTrue";

    public static final String A_PEOPLE_METADATA =
            "        <dat:tables xmlns:dat=\"http://labkey.org/data/xml\" xmlns:cv=\"http://labkey.org/data/xml/queryCustomView\">\n" +
            "            <dat:table tableName=\"People\" tableDbType=\"NOT_IN_DB\">\n" +
            "                <dat:tableUrl>/simpletest/other.view</dat:tableUrl>\n" +
            "                <dat:filters>\n" +
            "                  <cv:where>Name LIKE 'A%'</cv:where>\n" +
            "                </dat:filters>\n" +
            "            </dat:table>\n" +
            "        </dat:tables>";


    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/query";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupProject();
        createList();

        createLinkedSchema();
        verifyLinkedSchema();

        createLinkedSchemaUsingTemplate();
        verifyLinkedSchemaUsingTemplate();
    }

    @LogMethod
    void setupProject()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), SOURCE_FOLDER, null);
        // Enable simpletest in source folder so the "BPeopleTemplate" is visible.
        enableModule(SOURCE_FOLDER, "simpletest");

        _containerHelper.createSubfolder(getProjectName(), TARGET_FOLDER, null);
    }

    @LogMethod
    void createList()
    {
        log("Importing some data...");
        _listHelper.createList(SOURCE_FOLDER, LIST_NAME,
                ListHelperWD.ListColumnType.AutoInteger, "Key",
                new ListHelperWD.ListColumn("Name", "Name", ListHelperWD.ListColumnType.String, "Name"),
                new ListHelperWD.ListColumn("Age", "Age", ListHelperWD.ListColumnType.Integer, "Age"),
                new ListHelperWD.ListColumn("Crazy", "Crazy", ListHelperWD.ListColumnType.Boolean, "Crazy?"));

        log("Importing some data...");
        clickButton("Import Data");
        _listHelper.submitTsvData(LIST_DATA);
    }

    @LogMethod
    void createLinkedSchema()
    {
        createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, "A_People", SOURCE_FOLDER, null, "lists", "People", A_PEOPLE_METADATA);
    }

    @LogMethod
    void verifyLinkedSchema()
    {
        goToSchemaBrowser();
        selectQuery("A_People", "People");
        waitAndClick(Locator.linkWithText("view data"));

        waitForElement(Locator.id("dataregion_query"));
        DataRegionTable table = new DataRegionTable("query", this);
        Assert.assertEquals("Unexpected number of rows", 1, table.getDataRowCount());
        Assert.assertEquals("Expected to filter table to only Adam", "Adam", table.getDataAsText(0, "Name"));

        // Check generic details page is available
        clickAndWait(table.detailsLink(0));
        assertTextPresent("Details", "Adam");
    }

    @LogMethod
    void createLinkedSchemaUsingTemplate()
    {
        createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, "B_People", SOURCE_FOLDER, "BPeople", null, null, null);
    }

    @LogMethod
    void verifyLinkedSchemaUsingTemplate()
    {
        goToSchemaBrowser();
        selectQuery("B_People", "People");
        waitAndClick(Locator.linkWithText("view data"));

        waitForElement(Locator.id("dataregion_query"));
        DataRegionTable table = new DataRegionTable("query", this);
        Assert.assertEquals("Unexpected number of rows", 1, table.getDataRowCount());
        Assert.assertEquals("Expected to filter table to only Britt", "Britt", table.getDataAsText(0, "Name"));

        // Check generic details page is available
        clickAndWait(table.detailsLink(0));
        assertTextPresent("Details", "Britt");
    }



    @LogMethod
    void createLinkedSchema(String containerPath, String name, String sourceContainer, String schemaTemplate, String sourceSchema, String tables, String metadata)
    {
        beginAt("/query/" + containerPath + "/admin.view");
        assertTextNotPresent(name);

        clickAndWait(Locator.linkWithText("new linked schema"));
        setFormElement(Locator.name("userSchemaName"), name);
        _extHelper.selectComboBoxItem("Source Container:", sourceContainer);

        if (schemaTemplate != null)
        {
            // UNDONE: Can't seem to get the timing right -- so just set the schemaTemplate on the form element
            _extHelper.selectComboBoxItem("Schema Template:", schemaTemplate);
        }
        else
        {
            _extHelper.selectComboBoxItem("LabKey Schema Name:", sourceSchema);

            // UNDONE
            //if (tables != null)
            //    setFormElement(Locator.name("tables"), tables);

            if (metadata != null)
                setFormElement(Locator.name("metaData"), metadata);
        }

        clickButton("Create");

        // On success, we go back to admin.view
        assertTitleContains("Schema Administration");
        assertTextPresent(name);
    }

}

/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.ListHelperWD;
import org.labkey.test.util.LogMethod;

import java.io.File;

/**
 * User: kevink
 * Date: 1/28/13
 */
public class LinkedSchemaTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = LinkedSchemaTest.class.getSimpleName() + "Project";
    private static final String SOURCE_FOLDER = "SourceFolder";
    private static final String TARGET_FOLDER = "TargetFolder";
    private static final String OTHER_FOLDER = "OtherFolder";
    private static final int MOTHER_ID = 3;


    public static final String LIST_NAME = "People";
    public static final String LIST_DATA = "Name\tAge\tCrazy\n" +
            "Dave\t39\tTrue\n" +
            "Adam\t65\tTrue\n" +
            "Britt\t30\tFalse\n" +
            "Josh\t30\tTrue";

    public static final String A_PEOPLE_SCHEMA_NAME = "A_People";
    public static final String A_PEOPLE_METADATA_TITLE = "A_PEOPLE_METADATA Name title";
    public static final String A_PEOPLE_METADATA =
            "<dat:tables xmlns:dat=\"http://labkey.org/data/xml\" xmlns:cv=\"http://labkey.org/data/xml/queryCustomView\">\n" +
            "    <dat:table tableName=\"People\" tableDbType=\"NOT_IN_DB\">\n" +
            "        <dat:tableUrl>/query/recordDetails.view?schemaName=" + A_PEOPLE_SCHEMA_NAME + "&amp;queryName=People&amp;keyField=Key&amp;key=${Key}</dat:tableUrl>\n" +
            "        <dat:filters>\n" +
            "          <cv:where>Name LIKE 'A%'</cv:where>\n" +
            "        </dat:filters>\n" +
            "        <dat:columns>\n" +
            "          <dat:column columnName=\"Name\">\n" +
            "            <dat:columnTitle>" + A_PEOPLE_METADATA_TITLE + "</dat:columnTitle>\n" +
            "          </dat:column>\n" +
            "        </dat:columns>\n" +
            "    </dat:table>\n" +
            "</dat:tables>\n";

    public static final String B_PEOPLE_SCHEMA_NAME = "B_People";
    public static final String B_PEOPLE_TEMPLATE_METADATA_TITLE = "BPeopleTemplate Name title";

    public static final String D_PEOPLE_SCHEMA_NAME = "D_People";
    public static final String D_PEOPLE_METADATA_TITLE = "D_PEOPLE_METADATA Name title";
    public static final String D_PEOPLE_METADATA =
            "<dat:tables xmlns:dat=\"http://labkey.org/data/xml\" xmlns:cv=\"http://labkey.org/data/xml/queryCustomView\">\n" +
            "    <dat:table tableName=\"People\" tableDbType=\"NOT_IN_DB\">\n" +
            "        <!-- disable details url -->\n" +
            "        <dat:tableUrl></dat:tableUrl>\n" +
            "        <dat:filters>\n" +
            "          <cv:where>Name LIKE 'D%'</cv:where>\n" +
            "        </dat:filters>\n" +
            "        <dat:columns>\n" +
            "          <dat:column columnName=\"Name\">\n" +
            "            <dat:columnTitle>" + D_PEOPLE_METADATA_TITLE + "</dat:columnTitle>\n" +
            "          </dat:column>\n" +
            "        </dat:columns>\n" +
            "    </dat:table>\n" +
            "    <dat:table tableName=\"TestQuery\" tableDbType=\"NOT_IN_DB\">\n" +
            "        <dat:columns>\n" +
            "          <dat:column columnName=\"Name\">\n" +
            "            <dat:columnTitle>Crazy " + D_PEOPLE_METADATA_TITLE + "</dat:columnTitle>\n" +
            "          </dat:column>\n" +
            "        </dat:columns>\n" +
            "    </dat:table>\n" +
            "</dat:tables>\n";


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
        importListData();

        goToProject("LinkedSchemaTestProject");

        createLinkedSchema();
        verifyLinkedSchema();

        String sourceContainerPath = "/" + getProjectName() + "/" + SOURCE_FOLDER;
        createLinkedSchema(TARGET_FOLDER, "BasicLinkedSchema", sourceContainerPath, null, "lists", "NIMHDemographics,NIMHPortions", null);

        //Ensure that all the columns we would expect to come through are coming through
        assertColumnsPresent(TARGET_FOLDER, "BasicLinkedSchema", "NIMHDemographics", "SubjectID", "Name", "Family", "Mother", "Father", "Species", "Occupation",
                "MaritalStatus", "CurrentStatus", "Gender", "BirthDate", "Image");
        //Make sure the columns in the source that should be hidden are hidden, then check them in the linked schema
        assertColumnsNotPresent(SOURCE_FOLDER, "lists", "NIMHDemographics", "EntityId", "LastIndexed");
        assertColumnsNotPresent(TARGET_FOLDER, "BasicLinkedSchema", "NIMHDemographics", "EntityId", "LastIndexed");

        //Make sure that the lookup columns propogated properly into the linked schema
        assertLookupsWorking(TARGET_FOLDER, "BasicLinkedSchema", "NIMHDemographics", true, "Mother", "Father");

        // Linked schemas disallow lookups to other folders outside of the current folder.
        //Change the Mother column lookup to point to the other folder, then ensure that the mother lookup is no longer propogating
        changelistLookup(SOURCE_FOLDER, "NIMHDemographics", MOTHER_ID, new ListHelper.LookupInfo("/" + PROJECT_NAME + "/" + OTHER_FOLDER, "lists", "NIMHDemographics"));
        assertLookupsWorking(TARGET_FOLDER, "BasicLinkedSchema", "NIMHDemographics", true, "Father");
        assertLookupsWorking(TARGET_FOLDER, "BasicLinkedSchema", "NIMHDemographics", false, "Mother");

        //Create a query over the table with lookups
        createLinkedSchemaQuery(SOURCE_FOLDER, "lists", "QueryOverLookup", "NIMHDemographics");

        //Create a new linked schema that includes that query, and ensure that it is propogating lookups in the expected manner
        createLinkedSchema(TARGET_FOLDER, "QueryLinkedSchema", sourceContainerPath, null, "lists", "NIMHDemographics,NIMHPortions,QueryOverLookup", null);
        assertLookupsWorking(TARGET_FOLDER, "QueryLinkedSchema", "QueryOverLookup", true, "Father");
        assertLookupsWorking(TARGET_FOLDER, "QueryLinkedSchema", "QueryOverLookup", false, "Mother");

        //Change the Mother column lookup to point to the query, and then make sure that the table has lookups appropriately.
        changelistLookup(SOURCE_FOLDER, "NIMHDemographics", MOTHER_ID, new ListHelper.LookupInfo("/" + PROJECT_NAME + "/" + SOURCE_FOLDER, "lists", "QueryOverLookup"));
        assertLookupsWorking(TARGET_FOLDER, "QueryLinkedSchema", "NIMHDemographics", true, "Mother", "Father");


        createLinkedSchemaUsingTemplate();
        verifyLinkedSchemaUsingTemplate();

        createLinkedSchemaTemplateOverride();
        verifyLinkedSchemaTemplateOverride();

    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    void setupProject()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), SOURCE_FOLDER, null);
        // Enable simpletest in source folder so the "BPeopleTemplate" is visible.
        enableModule(SOURCE_FOLDER, "simpletest");

        _containerHelper.createSubfolder(getProjectName(), TARGET_FOLDER, null);
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
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

    @LogMethod(category = LogMethod.MethodType.SETUP)
    void importListData()
    {
        File lists = new File(getLabKeyRoot() + "/sampledata/lists/ListDemo.lists.zip");
        _listHelper.importListArchive(SOURCE_FOLDER, lists);

        //Create second folder that should be not visible to linked schemas
        _containerHelper.createSubfolder(getProjectName(), OTHER_FOLDER, null);
        _listHelper.importListArchive(OTHER_FOLDER, lists);
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    void createLinkedSchema()
    {
        log("** Creating linked schema APeople without template");
        String sourceContainerPath = "/" + getProjectName() + "/" + SOURCE_FOLDER;
        createLinkedSchema(TARGET_FOLDER, A_PEOPLE_SCHEMA_NAME, sourceContainerPath, null, "lists", "People", A_PEOPLE_METADATA);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    void verifyLinkedSchema()
    {
        goToSchemaBrowser();
        selectQuery(A_PEOPLE_SCHEMA_NAME, "People");
        waitAndClick(Locator.linkWithText("view data"));

        waitForElement(Locator.id("dataregion_query"));
        DataRegionTable table = new DataRegionTable("query", this);
        Assert.assertEquals("Unexpected number of rows", 1, table.getDataRowCount());
        Assert.assertEquals("Expected to filter table to only Adam", "Adam", table.getDataAsText(0, A_PEOPLE_METADATA_TITLE));

        // Check the custom details url is used
        clickAndWait(table.detailsLink(0));
        assertTitleContains("Record Details:");
        assertTextPresent("Adam");
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    void createLinkedSchemaUsingTemplate()
    {
        log("** Creating linked schema BPeople using BPeopleTemplate");
        String sourceContainerPath = "/" + getProjectName() + "/" + SOURCE_FOLDER;
        createLinkedSchema(TARGET_FOLDER, B_PEOPLE_SCHEMA_NAME, sourceContainerPath, "BPeopleTemplate", null, null, null);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    void verifyLinkedSchemaUsingTemplate()
    {
        goToSchemaBrowser();
        selectQuery(B_PEOPLE_SCHEMA_NAME, "People");
        waitAndClick(Locator.linkWithText("view data"));

        waitForElement(Locator.id("dataregion_query"));
        DataRegionTable table = new DataRegionTable("query", this);
        Assert.assertEquals("Unexpected number of rows", 1, table.getDataRowCount());
        // Check Name column is renamed and 'Britt' is the only value
        Assert.assertEquals("Expected to filter table to only Britt", "Britt", table.getDataAsText(0, B_PEOPLE_TEMPLATE_METADATA_TITLE));

        // Check the simpletest/other.view details url is used
        clickAndWait(table.detailsLink(0));
        assertTextPresent("This is another view in the simple test module.");
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    void createLinkedSchemaTemplateOverride()
    {
        log("** Creating linked schema BPeople using BPeopleTemplate with metadata override to only show 'D' people");
        String sourceContainerPath = "/" + getProjectName() + "/" + SOURCE_FOLDER;
        createLinkedSchema(TARGET_FOLDER, D_PEOPLE_SCHEMA_NAME, sourceContainerPath, "BPeopleTemplate", null, "People,TestQuery", D_PEOPLE_METADATA);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    void verifyLinkedSchemaTemplateOverride()
    {
        goToSchemaBrowser();
        viewQueryData(D_PEOPLE_SCHEMA_NAME, "People");

        waitForElement(Locator.id("dataregion_query"));
        DataRegionTable table = new DataRegionTable("query", this, false);
        Assert.assertEquals("Unexpected number of rows", 1, table.getDataRowCount());
        // Check Name column is renamed and 'Dave' is the only value
        Assert.assertEquals("Expected to filter table to only Dave", "Dave", table.getDataAsText(0, D_PEOPLE_METADATA_TITLE));

        // Check the details url has been disabled
        assertElementNotPresent(Locator.linkWithText("details"));

        // Check TestQuery is available and metadata is overridden
        goToSchemaBrowser();
        viewQueryData(D_PEOPLE_SCHEMA_NAME, "TestQuery");

        waitForElement(Locator.id("dataregion_query"));
        table = new DataRegionTable("query", this, false);
        // TestQuery is executed over the 'D_People' filtered People table -- so only 'Dave' is available.
        Assert.assertEquals("Unexpected number of rows", 1, table.getDataRowCount());
        Assert.assertEquals("Expected to filter query to only Dave", "Dave", table.getDataAsText(0, "Crazy " + D_PEOPLE_METADATA_TITLE));
    }

    protected void goToSchemaBrowserTable(String schemaName, String tableName)
    {
        goToSchemaBrowser();
        waitForElement(Locator.xpath("//span[text()='"+schemaName+"']"));
        click(Locator.xpath("//span[text()='" + schemaName + "']"));
        waitForElement(Locator.xpath("//span[text()='"+ tableName +"']"));
        click(Locator.xpath("//span[text()='" + tableName + "']"));
    }

    protected void goToProject(String projectName)
    {
        goToHome();
        waitForElement(Locator.xpath("//a[text()='"+ projectName +"']"));
        click(Locator.xpath("//a[text()='" + projectName + "']"));
    }

    protected void changeListName(String oldName, String newName)
    {
        goToSchemaBrowserTable("lists", oldName);
        waitForElement(Locator.xpath("//a[text()='edit definition']"));
        click(Locator.xpath("//a[text()='edit definition']"));

        _listHelper.clickEditDesign();
        waitForElement(Locator.xpath("//input[@name='ff_name']"));
        setFormElement(Locator.xpath("//input[@name='ff_name']"), newName);

        _listHelper.clickSave();
    }

    protected void assertColumnsPresent(String sourceFolder, String schemaName, String tableName, String... columnNames)
    {
        waitForElement(Locator.xpath("//a[text()='"+ sourceFolder +"']"));
        click(Locator.xpath("//a[text()='"+ sourceFolder +"']"));

        goToSchemaBrowserTable(schemaName, tableName);
        waitForElement(Locator.xpath("//a[text()='view data']"));
        click(Locator.xpath("//a[text()='view data']"));
        waitForText(tableName);

        for (String name : columnNames)
        {
            waitForElement(Locator.xpath("//td[@id='query:" + name + ":header']"));
        }

        click(Locator.xpath("//a[text()='TargetFolder']"));

    }

    protected void assertColumnsNotPresent(String sourceFolder, String schemaName, String tableName, String... columnNames)
    {
        waitForElement(Locator.xpath("//a[text()='"+ sourceFolder +"']"));
        click(Locator.xpath("//a[text()='"+ sourceFolder +"']"));

        goToSchemaBrowserTable(schemaName, tableName);
        waitForElement(Locator.xpath("//a[text()='view data']"));
        click(Locator.xpath("//a[text()='view data']"));
        waitForText(tableName);

        for (String name : columnNames)
        {
            assertElementNotPresent(Locator.xpath("//td[@id='query:" + name + ":header']"));
        }

        click(Locator.xpath("//a[text()='TargetFolder']"));
    }

    protected void assertLookupsWorking(String sourceFolder, String schemaName, String listName, boolean present, String... lookupColumns)
    {
        waitForElement(Locator.xpath("//a[text()='"+ sourceFolder +"']"));
        click(Locator.xpath("//a[text()='"+ sourceFolder +"']"));

        goToSchemaBrowserTable(schemaName, listName);
        waitForElement(Locator.xpath("//a[text()='view data']"));
        click(Locator.xpath("//a[text()='view data']"));

        _customizeViewsHelper.openCustomizeViewPanel();

        for (String column : lookupColumns)
        {
            Assert.assertEquals("Expected lookup column '" + column + "' to be " + (present ? "present" : "not present"), present, _customizeViewsHelper.isLookupColumn(column));
        }
    }

    protected void changelistLookup(String sourceFolder, String tableName, int index, ListHelper.LookupInfo info)
    {
        waitForElement(Locator.xpath("//a[text()='"+ sourceFolder +"']"));
        click(Locator.xpath("//a[text()='"+ sourceFolder +"']"));

        goToSchemaBrowserTable("lists", tableName);
        waitForElement(Locator.xpath("//a[text()='edit definition']"));
        click(Locator.xpath("//a[text()='edit definition']"));

        _listHelper.clickEditDesign();
        _listHelper.setColumnType(index, info);
        _listHelper.clickSave();

    }

    protected void createLinkedSchemaQuery(String sourceFolder, String schemaName, String queryName, String tableName)
    {
        waitForElement(Locator.xpath("//a[text()='"+ sourceFolder +"']"));
        click(Locator.xpath("//a[text()='"+ sourceFolder +"']"));

        goToSchemaBrowser();
        waitForElement(Locator.xpath("//span[text()='"+schemaName+"']"));
        click(Locator.xpath("//span[text()='"+schemaName+"']"));

        clickButton("Create New Query");

        waitForElement(Locator.xpath("//input[@name='ff_newQueryName']"));
        setFormElement(Locator.xpath("//input[@name='ff_newQueryName']"), queryName);
        click(Locator.xpath("//select[@name='ff_baseTableName']"));
        click(Locator.xpath("//option[@name='"+tableName+"']"));

        clickButton("Create and Edit Source");
        waitForElement(Locator.xpath("//button[text()='Save & Finish']"));
        clickButton("Save & Finish");
    }


    @LogMethod(category = LogMethod.MethodType.SETUP)
    void createLinkedSchema(String targetFolder, String name, String sourceContainerPath, String schemaTemplate, String sourceSchemaName, String tables, String metadata)
    {
        _editLinkedSchema(true, targetFolder, name, sourceContainerPath, schemaTemplate, sourceSchemaName, tables, metadata);
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    void updateLinkedSchema(String targetFolder, String name, String sourceContainerPath, String schemaTemplate, String sourceSchemaName, String tables, String metadata)
    {
        _editLinkedSchema(false, targetFolder, name, sourceContainerPath, schemaTemplate, sourceSchemaName, tables, metadata);
    }

    void _editLinkedSchema(boolean create, String targetFolder, String name, String sourceContainerPath, String schemaTemplate, String sourceSchemaName, String tables, String metadata)
    {
        beginAt("/query/" + getProjectName() + "/" + targetFolder + "/admin.view");

        // Click the create new or edit existing link.
        Locator link;
        if (create)
            link = Locator.xpath("//a[text()='new linked schema']");
        else
            link = Locator.xpath("//td[text()='" + name + "']/..//a[text()='edit']");
        waitForElement(link);
        click(link);

        waitForElement(Locator.xpath("//input[@name='userSchemaName']"));
        setFormElement(Locator.xpath("//input[@name='userSchemaName']"), name);
        setFormElement(Locator.xpath("//input[@name='dataSource']"), sourceContainerPath);
        waitForElement(Locator.xpath("//li[text()='" + sourceContainerPath + "']"));
        click(Locator.xpath("//li[text()='" + sourceContainerPath + "']"));

        if (schemaTemplate != null)
        {
            _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//input[@name='schemaTemplate']")));
            setFormElement(Locator.xpath("//input[@name='schemaTemplate']"), schemaTemplate);
            waitForElement(Locator.xpath("//li[text()='" + schemaTemplate + "']"));
            click(Locator.xpath("//li[text()='" + schemaTemplate + "']"));
        }

        if (sourceSchemaName != null)
        {
            if (schemaTemplate != null)
            {
                // click "Override template value" widget
                click(Locator.xpath("id('sourceSchemaOverride')/span[text()='Override template value']"));
            }
            _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//input[@name='sourceSchemaName']")));

            setFormElement(Locator.xpath("//input[@name='sourceSchemaName']"), sourceSchemaName);
            waitForElement(Locator.xpath("//li[text()='"+ sourceSchemaName + "']"));
            click(Locator.xpath("//li[text()='"+ sourceSchemaName + "']"));
        }

        if (tables != null)
        {
            if (schemaTemplate != null)
            {
                // click "Override template value" widget
                click(Locator.xpath("id('tablesOverride')/span[text()='Override template value']"));
            }
            _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//input[@name='tables']")));

            clickAt(Locator.xpath("//input[@name='tables']"), 1, 1);
            sleep(200);
            for (String table : tables.split(","))
            {
                click(Locator.xpath("//li[text() = '" + table + "']"));
            }
        }

        if (metadata != null)
        {
            if (schemaTemplate != null)
            {
                // click "Override template value" widget
                click(Locator.xpath("id('metadataOverride')/span[text()='Override template value']"));
            }
            _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//textarea[@name='metaData']")));

            setFormElement(Locator.xpath("//textarea[@name='metaData']"), metadata);
        }

        if (create)
            clickButton("Create");
        else
            clickButton("Update");

        // Back on schema admin page, check the linked schema was created/updated.
        waitForElement(Locator.xpath("//td[text()='" + name + "']"));
    }

}

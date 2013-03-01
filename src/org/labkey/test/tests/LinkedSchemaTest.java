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

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.poi.util.SystemOutLogger;
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.testpicker.TestHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.ListHelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: kevink
 * Date: 1/28/13
 */
public class LinkedSchemaTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = LinkedSchemaTest.class.getSimpleName() + "Project";
    private static final String SOURCE_FOLDER = "SourceFolder";
    private static final String TARGET_FOLDER = "TargetFolder";
    private static final int MOTHER_ID = 3;


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

    private String _sourceContainerId;
    private String _targetContainerId;


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
        _containerHelper.deleteProject("InvisibleProject", false, 90000);
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupProject();
        createList();
        File lists = new File(getLabKeyRoot() + "/sampledata/lists/ListDemo.lists.zip");
        _listHelper.importListArchive("SourceFolder", lists);

        //Create second project that should be invisible to linked schemas
        _containerHelper.createProject("InvisibleProject", null);
        _containerHelper.createSubfolder("InvisibleProject", "InvisibleFolder", null);
        _listHelper.importListArchive("InvisibleFolder", lists);
        goToProject("LinkedSchemaTestProject");

        createLinkedSchema();
        createLinkedSchemaWithTabels("TargetFolder", "BasicLinkedSchema", "lists", "NIMHDemographics", "NIMHPortions");
        //Ensure that all the columns we would expect to come through are coming through
        assertColumnsPresent("TargetFolder", "BasicLinkedSchema", "NIMHDemographics", "SubjectID", "Name", "Family", "Mother", "Father", "Species", "Occupation",
                            "MaritalStatus", "CurrentStatus", "Gender", "BirthDate", "Image");
        //Make sure the columns in the source that should be hidden are hidden, then check them in the linked schema
        assertColumnsNotPresent("SourceFolder", "lists", "NIMHDemographics", "EntityId", "LastIndexed", "Created", "CreatedBy", "Modified", "ModifiedBy");
        //TODO:  unccoment with fixing of issue 17316
        //assertColumnsNotPresent("TargetFolder", "BasicLinkedSchema", "People", "Key", "EntityId", "LastIndexed", "Created", "CreatedBy", "Modified", "ModifiedBy");

        //Make sure that the lookup columns propogated properly into the linked schema
        assertLookupsWorking("TargetFolder", "BasicLinkedSchema", "NIMHDemographics", true, "Mother", "Father");
        //Change the Mother column lookup to point to the invisible folder, then ensure that the mother lookup is no longer propogating
        changelistLookup("SourceFolder", "NIMHDemographics", MOTHER_ID, new ListHelper.LookupInfo("/InvisibleProject/InvisibleFolder", "lists", "NIMHDemographics"));
        assertLookupsWorking("TargetFolder", "BasicLinkedSchema", "NIMHDemographics", true, "Father");
        assertLookupsWorking("TargetFolder", "BasicLinkedSchema", "NIMHDemographics", false, "Mother");

        //Create a query over the table with lookups
        createLinkedSchemaQuery("SourceFolder", "lists", "QueryOverLookup", "NIMHDemographics");

        //Create a new linked schema that includes that query, and ensure that it is propogating lookups in the expected manner
        createLinkedSchemaWithTabels("TargetFolder", "QueryLinkedSchema", "lists", "NIMHDemographics", "NIMHPortions", "QueryOverLookup");
        assertLookupsWorking("TargetFolder", "QueryLinkedSchema", "QueryOverLookup", true, "Father");
        assertLookupsWorking("TargetFolder", "QueryLinkedSchema", "QueryOverLookup", false, "Mother");

        //Change the Mother column lookup to point to the query, and then make sure that the table has lookups appropriately.
        changelistLookup("SourceFolder", "NIMHDemographics", MOTHER_ID, new ListHelper.LookupInfo("/LinkedSchemaTestProject/SourceFolder", "lists", "QueryOverLookup"));
        //TODO:  Uncomment with fixing of issue 17298
        //assertLookupsWorking("TargetFolder", "QueryLinkedSchema", "NIMHDemographics", true, "Mother", "Father");

        //Validate
        //TODO:  Uncomment with fixing of issue 17317
        //verifyLinkedSchema();
        createLinkedSchemaUsingTemplate();
        //TODO:  Uncomment with fixing of issue 17317
        //verifyLinkedSchemaUsingTemplate();

    }

    @LogMethod
    void setupProject()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), SOURCE_FOLDER, null);
        // Enable simpletest in source folder so the "BPeopleTemplate" is visible.
        enableModule(SOURCE_FOLDER, "simpletest");
        _sourceContainerId = getContainerId();

        _containerHelper.createSubfolder(getProjectName(), TARGET_FOLDER, null);
        _targetContainerId = getContainerId();
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
        if (_sourceContainerId == null)
            _sourceContainerId = getContainerId(getBaseURL() + "/project/" + getProjectName() + "/" + SOURCE_FOLDER + "/begin.view");
        createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, "A_People", _sourceContainerId, null, "lists", "People", A_PEOPLE_METADATA);
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
        createLinkedSchema(getProjectName() + "/" + TARGET_FOLDER, "B_People", _sourceContainerId, "BPeopleTemplate", null, null, null);
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

    //TODO:  DELETE THIS METHOD with resolution of issue 17317
    @Override
    public void validateQueries(boolean validateSubfolders)
    {

    }

    protected void goToSchemaBrowserTable(String schemaName, String tableName)
    {
        goToSchemaBrowser();
        waitForElement(Locator.xpath("//span[text()='"+schemaName+"']"));
        click(Locator.xpath("//span[text()='"+schemaName+"']"));
        waitForElement(Locator.xpath("//span[text()='"+ tableName +"']"));
        click(Locator.xpath("//span[text()='" + tableName + "']"));
    }

    protected void goToProject(String projectName)
    {
        goToHome();
        waitForElement(Locator.xpath("//a[text()='"+ projectName +"']"));
        click(Locator.xpath("//a[text()='"+ projectName +"']"));
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

    protected void createLinkedSchemaWithTabels(String targetFolder, String linkedSchemaName, String sourceSchema, String... tables)
    {
        waitForElement(Locator.xpath("//a[text()='"+ targetFolder +"']"));
        click(Locator.xpath("//a[text()='"+ targetFolder +"']"));

        goToSchemaBrowser();
        waitForText("Schema Administration");
        clickButton("Schema Administration");
        waitForElement(Locator.xpath("//a[text()='new linked schema']"));
        click(Locator.xpath("//a[text()='new linked schema']"));

        waitForElement(Locator.xpath("//input[@name='userSchemaName']"));
        setFormElement(Locator.xpath("//input[@name='userSchemaName']"), linkedSchemaName);
        setFormElement(Locator.xpath("//input[@name='dataSource']"), "/LinkedSchemaTestProject/SourceFolder");
        waitForElement(Locator.xpath("//li[text()='/LinkedSchemaTestProject/SourceFolder']"));
        click(Locator.xpath("//li[text()='/LinkedSchemaTestProject/SourceFolder']"));
        setFormElement(Locator.xpath("//input[@name='sourceSchemaName']"), sourceSchema);
        click(Locator.xpath("//li[text()='"+ sourceSchema +"']"));

        click(Locator.xpath("//input[@name='tables']"));
        for(String table : tables)
        {
            click(Locator.xpath("//li[text() = '" + table + "']"));
        }

        clickButton("Create");
        waitForElement(Locator.xpath("//a[text()='TargetFolder']"));
        click(Locator.xpath("//a[text()='TargetFolder']"));
    }

    protected void assertColumnsPresent(String sourceFolder, String schemaName, String tableName, String... columnNames)
    {
        waitForElement(Locator.xpath("//a[text()='"+ sourceFolder +"']"));
        click(Locator.xpath("//a[text()='"+ sourceFolder +"']"));

        goToSchemaBrowserTable(schemaName, tableName);
        waitForElement(Locator.xpath("//a[text()='view data']"));
        click(Locator.xpath("//a[text()='view data']"));
        waitForText(tableName);

        for(String name : columnNames){
            waitForElement(Locator.xpath("//td[@id='query:" + name + ":header']"));
        }

        click(Locator.xpath("//a[text()='TargetFolder']"));
        System.out.print("Hey! Good job.");

    }

    protected void assertColumnsNotPresent(String sourceFolder, String schemaName, String tableName, String... columnNames)
    {
        waitForElement(Locator.xpath("//a[text()='"+ sourceFolder +"']"));
        click(Locator.xpath("//a[text()='"+ sourceFolder +"']"));

        goToSchemaBrowserTable(schemaName, tableName);
        waitForElement(Locator.xpath("//a[text()='view data']"));
        click(Locator.xpath("//a[text()='view data']"));
        waitForText(tableName);

        for(String name : columnNames){
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

        click(Locator.xpath("//span[text()='Views']"));
        click(Locator.xpath("//a[@id='query:Views:Customize View']"));

        if(present)
        {
            for(String column : lookupColumns)
            {
                waitForElement(Locator.xpath("//div[@fieldkey='"+column+"']/img[@class='x-tree-ec-icon x-tree-elbow-plus']"));
            }
        }
        else
        {
            for(String column : lookupColumns)
            {
                assertElementNotPresent(Locator.xpath("//div[@fieldkey='"+column+"']/img[@class='x-tree-ec-icon x-tree-elbow-plus']"));
            }
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



    @LogMethod
    void createLinkedSchema(String containerPath, String name, String sourceContainerId, String schemaTemplate, String sourceSchemaName, String tables, String metadata)
    {
        beginAt("/query/" + containerPath + "/admin.view");
        assertTextNotPresent(name);

        // UNDONE: Use web ui to insert the linked schema ...
        /*
        clickAndWait(Locator.linkWithText("new linked schema"));
        _extHelper.setExtFormElementByLabel("Schema Name:", name);
        //setFormElement(Locator.name("userSchemaName"), name);
        _extHelper.selectComboBoxItem("Source Container:", sourceContainer);

        if (schemaTemplate != null)
        {
            // UNDONE: Can't seem to get the timing right -- so just set the schemaTemplate on the form element
            _extHelper.selectComboBoxItem("Schema Template:", schemaTemplate);
        }
        else
        {
            _extHelper.selectComboBoxItem("Source Schema:", sourceSchema);

            // UNDONE
            //if (tables != null)
            //    setFormElement(Locator.name("tables"), tables);

            if (metadata != null)
                setFormElement(Locator.name("metaData"), metadata);
        }

        clickButton("Create");
        */

        HttpClient client = WebTestHelper.getHttpClient(PasswordUtil.getUsername(), PasswordUtil.getPassword());
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpPost method = null;
        HttpResponse response = null;
        try
        {
            method = new HttpPost(getBaseURL() + "/query/" + containerPath + "/insertLinkedSchema.post");
            List<NameValuePair> args = new ArrayList<NameValuePair>();
            args.add(new BasicNameValuePair("schemaType", "linked"));
            args.add(new BasicNameValuePair("userSchemaName", name));
            args.add(new BasicNameValuePair("dataSource", sourceContainerId));
            args.add(new BasicNameValuePair("schemaTemplate", schemaTemplate));
            args.add(new BasicNameValuePair("sourceSchemaName", sourceSchemaName));
            args.add(new BasicNameValuePair("tables", tables));
            args.add(new BasicNameValuePair("metaData", metadata));
            method.setEntity(new UrlEncodedFormEntity(args));

            log("** Inserting linked schema by POST to " + method.getURI());
            response = client.execute(method, context);

            StatusLine statusLine = response.getStatusLine();
            log("  " + statusLine);
            Assert.assertTrue("Expected to success code 200 or 302: " + statusLine,
                    HttpStatus.SC_OK == statusLine.getStatusCode() || HttpStatus.SC_MOVED_TEMPORARILY == statusLine.getStatusCode());
            String html = EntityUtils.toString(response.getEntity());
            int err = html.indexOf("<div class=\"labkey-error\"");
            if (err > -1)
            {
                String msg = "ERROR inserting linked schema";
                int end = html.indexOf("</div>", err+1);
                if (end > -1)
                    msg = html.substring(err, end);
                Assert.fail(msg);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (client != null)
                client.getConnectionManager().shutdown();
        }

        // On success, we are returned to admin.view (XXX: well, in the web ui version we will be...)
        //assertTitleContains("Schema Administration");
        beginAt("/query/" + containerPath + "/admin.view");
        assertTextPresent(name);
    }

}

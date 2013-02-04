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
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;

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

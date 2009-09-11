/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.Maps;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.Connection;

import java.util.Map;
import java.util.Arrays;

/*
* User: Dave
* Date: Mar 25, 2009
* Time: 10:50:18 AM
*
* Tests the simple module and file-based resources introduced in version 9.1
*/
public class SimpleModuleTest extends BaseSeleniumWebTest
{
    public static final String PROJECT_NAME = "Simple Module Verfiy Project";
    public static final String MODULE_NAME = "simpletest";
    public static final String VEHICLE_SCHEMA = "vehicle";
    public static final String LIST_NAME = "People";
    public static final String LIST_DATA = "Name\tAge\tCrazy\n" +
            "Dave\t39\tTrue\n" +
            "Adam\t65\tTrue\n" +
            "Britt\t30\tFalse\n" +
            "Josh\t30\tTrue";

    protected void doTestSteps() throws Exception
    {
        assertModuleDeployed(MODULE_NAME);
        createProject(PROJECT_NAME);
        enableModule(PROJECT_NAME, MODULE_NAME);
        enableModule(PROJECT_NAME, "Query");

        clickLinkWithText(PROJECT_NAME);
        doTestSchemas();
        doTestViews();
        doTestWebParts();
        createList();
        doTestQueries();
        doTestQueryViews();
        doTestReports();
    }

    private void doTestSchemas() throws Exception
    {
        log("Testing schemas in modules...");
        beginAt("/query/" + PROJECT_NAME + "/begin.view?schemaName=" + VEHICLE_SCHEMA);

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        log("Inserting new Manufacturers via java client api...");
        InsertRowsCommand insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Manufacturers");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.<String, Object>of("Name", "Ford"),
                Maps.<String, Object>of("Name", "Toyota"),
                Maps.<String, Object>of("Name", "Honda")
        ));
        SaveRowsResponse insertResp = insertCmd.execute(cn, PROJECT_NAME);
        assertEquals("Expected to insert 3 rows.", 3, insertResp.getRowsAffected().intValue());

        Long fordId = null;
        Long toyotaId = null;
        Long hondaId = null;

        for (Map<String, Object> row : insertResp.getRows())
        {
            Long rowId = (Long)row.get("RowId");
            String name = (String)row.get("Name");
            assertNotNull("Expected response row to have a Name column", name);
            assertNotNull("Expected response row to have a RowId column", rowId);
            if (name.equalsIgnoreCase("Ford"))
                fordId = rowId;
            else if (name.equalsIgnoreCase("Toyota"))
                toyotaId = rowId;
            else if (name.equalsIgnoreCase("Honda"))
                hondaId = rowId;
        }
        assertTrue("Expected rowids for all Manufacturers", fordId != null && toyotaId != null && hondaId != null);

        log("Inserting new Models via javas client api...");
        insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Models");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.<String, Object>of("ManufacturerId", toyotaId,
                                        "Name", "Prius"),
                Maps.<String, Object>of("ManufacturerId", toyotaId,
                                        "Name", "Camry"),
                Maps.<String, Object>of("ManufacturerId", fordId,
                                        "Name", "Focus"),
                Maps.<String, Object>of("ManufacturerId", fordId,
                                        "Name", "F150")
        ));
        insertResp = insertCmd.execute(cn, PROJECT_NAME);
        assertEquals("Expected to insert 4 rows.", 4, insertResp.getRowsAffected().intValue());
    }

    private void doTestViews()
    {
        log("Testing views in modules...");
        //begin.view should display when clicking on the module's tab
        clickTab(MODULE_NAME);
        assertTextPresent("This is the begin view from the test module");

        //navigate to other view
        clickLinkWithText("other view");
        assertTextPresent("This is another view in the simple test module");

        log("Testing vehicle.Model url link...");
        beginAt("/query/" + PROJECT_NAME + "/begin.view?schemaName=" + VEHICLE_SCHEMA);
        selectQuery(VEHICLE_SCHEMA, "Models");
        waitForElement(Locator.linkWithText("view data"), 5000); //on Ext panel
        clickLinkWithText("view data");
        clickLinkWithText("Prius");
        assertTextPresent("Hooray!");
        String rowidStr = getText(Locator.id("model.rowid"));
        int rowid = Integer.parseInt(rowidStr);
        assertTrue("Expected rowid on model.html page", rowid > 0);
    }

    private void doTestWebParts()
    {
        log("Testing web parts in modules...");
        //go to project portal
        clickLinkWithText(PROJECT_NAME);

        //add Simple Module Web Part
        addWebPart("Simple Module Web Part");
        assertTextPresent("This is a web part view in the simple test module");
    }

    private void createList()
    {
        //create a list for our query
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Lists");

        log("Creating list for query/view/report test...");
        ListHelper.createList(this, PROJECT_NAME, LIST_NAME,
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name"),
                new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "Age"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"));

        log("Importing some data...");
        clickLinkWithText("import data");
        setFormElement("ff_data", LIST_DATA);
        submit();
    }

    private void doTestQueries()
    {
        log("Testing queries in modules...");

        //go to query module portal
        clickLinkWithText(PROJECT_NAME);
        clickTab("Query");
        selectQuery("lists", "TestQuery");
        waitForElement(Locator.linkWithText("view data"), 5000); //on Ext panel
        clickLinkWithText("view data");

        assertTextPresent("Adam");
        assertTextPresent("Dave");
        assertTextPresent("Josh");
        assertTextNotPresent("Britt");

        log("Testing query of vehicle schema...");
        beginAt("/query/" + PROJECT_NAME + "/schema.view?schemaName=" + VEHICLE_SCHEMA);
        selectQuery(VEHICLE_SCHEMA, "Toyotas");
        waitForElement(Locator.linkWithText("view data"), 5000); //on Ext panel
        clickLinkWithText("view data");

        assertTextPresent("Prius");
        assertTextPresent("Camry");
    }

    private void doTestQueryViews()
    {
        log("Testing module-based custom query views...");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(LIST_NAME);

        clickMenuButton("Views", "Views:Crazy People");
        assertTextPresent("Adam");
        assertTextPresent("Dave");
        assertTextPresent("Josh");
        assertTextNotPresent("Britt");

        //custom view has a sort by age descending
        assertTextBefore("Adam", "Josh");
    }

    private void doTestReports()
    {
        //check that R script engine is insalled, otherwise skip
        clickLinkWithText("Admin Console");
        clickLinkWithText("views and scripting");
        if(!isREngineConfigured())
        {
            log("R scripting engine is not configured--skipping R report test.");
            return;
        }

        log("Testing module-based reports...");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(LIST_NAME);
        clickMenuButton("Views", "Views:Super Cool R Report");
        assertTextPresent("\"name\"");
        assertTextPresent("\"age\"");
        assertTextPresent("\"crazy\"");
    }

    private void assertModuleDeployed(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is deployed");
        clickLinkWithText("Admin Console");
        assertTextPresent(moduleName);
    }

    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch(Throwable ignore) {}
        log("Cleaned up SimpleModuleTest project.");
    }

    public String getAssociatedModuleDirectory()
    {
        //return "none" to skip verification of module directory
        //as it won't exist until after the test starts running the first time
        return "none";
    }
}

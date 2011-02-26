/*
 * Copyright (c) 2009-2010 LabKey Corporation
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
import org.labkey.remoteapi.query.*;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.CommandException;

import java.io.IOException;
import java.util.Map;
import java.util.Arrays;
import java.util.Date;

/*
* User: Dave
* Date: Mar 25, 2009
* Time: 10:50:18 AM
*
* Tests the simple module and file-based resources introduced in version 9.1
*/
public class SimpleModuleTest extends BaseSeleniumWebTest
{
    public static final String FOLDER_TYPE = "My XML-defined Folder Type"; // Folder type defined in customFolder.foldertype.xml
    public static final String MODULE_NAME = "simpletest";
    public static final String FOLDER_NAME = "subfolder";
    public static final String VEHICLE_SCHEMA = "vehicle";
    public static final String LIST_NAME = "People";
    public static final String LIST_DATA = "Name\tAge\tCrazy\n" +
            "Dave\t39\tTrue\n" +
            "Adam\t65\tTrue\n" +
            "Britt\t30\tFalse\n" +
            "Josh\t30\tTrue";

    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    protected void doTestSteps() throws Exception
    {
        assertModuleDeployed(MODULE_NAME);
        createProject(getProjectName(), FOLDER_TYPE);
        assertModuleEnabledByDefault("Portal");
        assertModuleEnabledByDefault("simpletest");
        assertModuleEnabledByDefault("Query");

        createSubfolder(getProjectName(), FOLDER_NAME, null);

        // Modules enabled in file based folder definition
        //enableModule(getProjectName(), MODULE_NAME);
        //enableModule(getProjectName(), "Query");

        clickLinkWithText(getProjectName());
        doTestCustomFolder();
        doTestSchemas();
        doTestViews();
        doTestWebParts();
        createList();
        doTestQueries();
        doTestQueryViews();
        doTestReports();
    }
    
    private void doTestCustomFolder()
    {
        assertTextPresent("A customized web part");
        assertTextPresent("Data Pipeline");
        assertTextPresent("Experiment Runs");
        assertTextPresent("Sample Sets");
        assertTextPresent("Run Groups");
        assertLinkNotPresentWithText("Create Run Group"); // Not in small Run Groups web-part.
    }

    private void doTestSchemas() throws Exception
    {
        log("** Testing schemas in modules...");
        beginAt("/query/" + getProjectName() + "/begin.view?schemaName=" + VEHICLE_SCHEMA);

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        log("** Inserting new Manufacturers via java client api...");
        InsertRowsCommand insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Manufacturers");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.<String, Object>of("Name", "Ford"),
                Maps.<String, Object>of("Name", "Toyota"),
                Maps.<String, Object>of("Name", "Honda")
        ));
        SaveRowsResponse insertResp = insertCmd.execute(cn, getProjectName());
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

        log("** Inserting new Models via javas client api...");
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
        insertResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Expected to insert 4 rows.", 4, insertResp.getRowsAffected().intValue());

        Long priusId = null;
        Long f150Id = null;

        for (Map<String, Object> row : insertResp.getRows())
        {
            Long rowId = (Long)row.get("RowId");
            String name = (String)row.get("Name");
            if (name.equalsIgnoreCase("Prius"))
                priusId = rowId;
            else if (name.equalsIgnoreCase("F150"))
                f150Id = rowId;
        }
        assertNotNull(priusId);
        assertNotNull(f150Id);

        log("** Testing vehicle.Model RowId url link...");
        beginAt("/query/" + getProjectName() + "/begin.view?schemaName=" + VEHICLE_SCHEMA);
        viewQueryData(VEHICLE_SCHEMA, "Models");
        clickLinkWithText("Prius");
        assertTextPresent("Hooray!");
        String rowidStr = getText(Locator.id("model.rowid"));
        int rowid = Integer.parseInt(rowidStr);
        assertTrue("Expected rowid on model.html page", rowid > 0);

        log("** Testing query of vehicle schema...");
        beginAt("/query/" + getProjectName() + "/schema.view?schemaName=" + VEHICLE_SCHEMA);
        viewQueryData(VEHICLE_SCHEMA, "Toyotas");

        assertTextPresent("Prius");
        assertTextPresent("Camry");

        log("** Inserting colors...");
        insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Colors");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.<String, Object>of("Name", "Red", "Hex", "#FF0000"),
                Maps.<String, Object>of("Name", "Green", "Hex", "#00FF00"),
                Maps.<String, Object>of("Name", "Blue", "Hex", "#0000FF")
        ));
        insertResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Expected to insert 3 rows.", 3, insertResp.getRowsAffected().intValue());

        log("** Inserting vechicles...");
        insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.<String, Object>of(
                        "ModelId", priusId,
                        "Color", "Green!",
                        "ModelYear", Integer.valueOf(2000),
                        "Milage", Integer.valueOf(3),
                        "LastService", new Date(2009, 9, 9)
                ),
                Maps.<String, Object>of(
                        "ModelId", f150Id,
                        "Color", "Red!",
                        "ModelYear", Integer.valueOf(2001),
                        "Milage", Integer.valueOf(4),
                        "LastService", new Date(2009, 11, 9)
                )
        ));
        insertResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Expected to insert 2 rows.", 2, insertResp.getRowsAffected().intValue());

        Long[] vehicleIds = new Long[2];
        vehicleIds[0] = (Long)(insertResp.getRows().get(0).get("RowId"));
        vehicleIds[1] = (Long)(insertResp.getRows().get(1).get("RowId"));

        log("** Trying to update Vehicle from wrong container...");
        UpdateRowsCommand updateCmd = new UpdateRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        updateCmd.getRows().addAll(Arrays.asList(
                Maps.<String, Object>of(
                        "RowId", vehicleIds[1],
                        "Milage", Integer.valueOf(4),
                        "LastService", new Date(2009, 9, 10)
                )
        ));
        try
        {
            SaveRowsResponse updateRows = updateCmd.execute(cn, getProjectName() + "/" + FOLDER_NAME);
            fail("Expected to throw CommandException");
        }
        catch (CommandException ex)
        {
            assertEquals("The row is from the wrong container.", ex.getMessage());
        }

        // Make sure that the schema isn't resolved if the module is not enabled in the container
        try
        {
            SaveRowsResponse updateRows = updateCmd.execute(cn, "Home");
            fail("Expected to throw CommandException");
        }
        catch (CommandException ex)
        {
            assertEquals("The schema 'vehicle' does not exist.", ex.getMessage());
        }

        log("** Updating vehicles...");
        SaveRowsResponse updateRows = updateCmd.execute(cn, getProjectName());
        assertEquals("Expected to update 1 row.", 1, updateRows.getRowsAffected().intValue());
        assertEquals(4, ((Number)(updateRows.getRows().get(0).get("Milage"))).intValue());


        log("** Testing vehicle.Vehicles details url link...");
        beginAt("/query/" + getProjectName() + "/schema.view?schemaName=" + VEHICLE_SCHEMA);
        viewQueryData(VEHICLE_SCHEMA, "Vehicles");
        clickLinkWithText("details");
        assertTextPresent("Hooray!");
        rowidStr = getText(Locator.id("vehicle.rowid"));
        rowid = Integer.parseInt(rowidStr);
        assertTrue("Expected rowid on vehicle.html page", rowid > 0);


        SelectRowsCommand selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());
        assertTrue("Expected to select >0 rows.", selectResp.getRowCount().intValue() > 0);

        DeleteRowsCommand deleteCmd = new DeleteRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        deleteCmd.setRows(selectResp.getRows());
        try
        {
            log("** Trying to delete Vehicles from a different container");
            SaveRowsResponse deleteResp = deleteCmd.execute(cn, getProjectName() + "/" + FOLDER_NAME);
            fail("Expected to throw CommandException");
        }
        catch (CommandException ex)
        {
            assertEquals("The row is from the wrong container.", ex.getMessage());
        }
    }

    private void cleanupSchema(Connection cn) throws IOException
    {
        cleanupTable(cn, "Vehicles", getProjectName());
        cleanupTable(cn, "Models", null);
        cleanupTable(cn, "Manufacturers", null);
        cleanupTable(cn, "Colors", null);
    }

    private void cleanupTable(Connection cn, String tableName, String project) throws IOException
    {
        log("** Deleting all " + tableName + " from '" + (project == null ? "root" : project) + "'");
        try
        {
            SelectRowsCommand selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, tableName);
            selectCmd.setMaxRows(-1);
            SelectRowsResponse selectResp = selectCmd.execute(cn, project);

            if (selectResp.getRowCount().intValue() > 0)
            {
                DeleteRowsCommand deleteCmd = new DeleteRowsCommand(VEHICLE_SCHEMA, tableName);
                deleteCmd.setRows(selectResp.getRows());
                deleteCmd.execute(cn, project);
                assertEquals("Expected no rows remaining", 0, selectCmd.execute(cn, project).getRowCount().intValue());
            }
        }
        catch (CommandException e)
        {
            // Don't log project not found error
            if (e.getStatusCode() != 404)
                e.printStackTrace();
        }
    }

    private void doTestViews()
    {
        log("Testing views in modules...");
        //begin.view should display when clicking on the module's tab
        goToModule(MODULE_NAME);
        assertTextPresent("This is the begin view from the test module");

        //navigate to other view
        clickLinkWithText("other view");
        assertTextPresent("This is another view in the simple test module");
    }

    private void doTestWebParts()
    {
        log("Testing web parts in modules...");
        //go to project portal
        clickLinkWithText(getProjectName());

        //add Simple Module Web Part
        addWebPart("Simple Module Web Part");
        assertTextPresent("This is a web part view in the simple test module");
    }

    private void createList()
    {
        //create a list for our query
        clickLinkWithText(getProjectName());
        addWebPart("Lists");

        log("Creating list for query/view/report test...");
        ListHelper.createList(this, getProjectName(), LIST_NAME,
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name"),
                new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "Age"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"));

        log("Importing some data...");
        clickNavButton("Import Data");
        setFormElement("ff_data", LIST_DATA);
        submit();
    }

    private void doTestQueries()
    {
        log("Testing queries in modules...");

        //go to query module portal
        clickLinkWithText(getProjectName());
        goToModule("Query");
        viewQueryData("lists", "TestQuery");

        assertTextPresent("Adam");
        assertTextPresent("Dave");
        assertTextPresent("Josh");
        assertTextNotPresent("Britt");
    }

    private void doTestQueryViews()
    {
        log("Testing module-based custom query views...");
        clickLinkWithText(getProjectName());
        clickLinkWithText(LIST_NAME);

        clickMenuButton("Views", "Crazy People");
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
        clickLinkWithText(getProjectName());
        clickLinkWithText(LIST_NAME);
        clickMenuButton("Views", "Super Cool R Report");
        assertTextPresent("\"name\"");
        assertTextPresent("\"age\"");
        assertTextPresent("\"crazy\"");
    }

    protected void assertModuleDeployed(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is deployed");
        clickLinkWithText("Admin Console");
        assertTextPresent(moduleName);
    }

    protected void assertModuleEnabledByDefault(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is enabled");
        clickLinkWithText("Folder Settings");
        assertElementPresent(Locator.xpath("//input[@type='checkbox' and @checked and @disabled and @title='" + moduleName + "']"));
    }

    protected void doCleanup() throws Exception
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        cleanupSchema(cn);

        try
        {
            deleteProject(getProjectName());
        }
        catch(Throwable ignore) {}
        log("Cleaned up SimpleModuleTest project.");
    }

    public String getAssociatedModuleDirectory()
    {
        //return "null" to skip verification of module directory
        //as it won't exist until after the test starts running the first time
        return null;
    }

}

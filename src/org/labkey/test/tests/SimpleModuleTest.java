/*
 * Copyright (c) 2009-2013 LabKey Corporation
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

import org.json.simple.JSONObject;
import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.Row;
import org.labkey.remoteapi.query.RowMap;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.RReportHelperWD;
import org.labkey.test.util.SecurityHelperWD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
* User: Dave
* Date: Mar 25, 2009
* Time: 10:50:18 AM
*
* Tests the simple module and file-based resources introduced in version 9.1
*/
public class SimpleModuleTest extends BaseWebDriverTest
{
    public static final String FOLDER_TYPE = "My XML-defined Folder Type"; // Folder type defined in customFolder.foldertype.xml
    public static final String TABBED_FOLDER_TYPE = "My XML-defined Tabbed Folder Type";
    public static final String MODULE_NAME = "simpletest";
    public static final String FOLDER_NAME = "subfolder";
    public static final String VEHICLE_SCHEMA = "vehicle";
    public static final String CORE_SCHEMA = "core";
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
        _containerHelper.createProject(getProjectName(), FOLDER_TYPE);
        assertModuleEnabledByDefault("Portal");
        assertModuleEnabledByDefault("simpletest");
        assertModuleEnabledByDefault("Query");

        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME, TABBED_FOLDER_TYPE);
        assertModuleEnabledByDefault("Portal");
        assertModuleEnabledByDefault("simpletest");
        assertModuleEnabledByDefault("Query");
        assertModuleEnabledByDefault("Study");

        doTestTabbedFolder();
        doTestContainerTabConversion();

        clickFolder(getProjectName());
        doTestCustomFolder();
        doTestSchemas();
        doTestTableAudit();
        doTestViews();
        doTestWebParts();
        createList();
        doTestModuleProperties();
        doTestQueries();
        doTestQueryViews();
        doTestReports();
        doTestInsertUpdateViews();
        doTestParameterizedQueries();
        doTestContainerColumns();
        doTestFilterSort();
        doTestImportTemplates();
    }

    @LogMethod
    private void doTestCustomFolder()
    {
        SecurityHelperWD securityHelper = new SecurityHelperWD(this);

        assertTextPresentInThisOrder("A customized web part", "Data Pipeline", "Experiment Runs", "Sample Sets", "Assay List");
        assertTextPresent("Run Groups");
        assertLinkNotPresentWithText("Create Run Group"); // Not in small Run Groups web-part.
        securityHelper.checkWebpartPermission("A customized web part", "Read", null);
        securityHelper.checkWebpartPermission("Data Pipeline", "Read", null);
    }

    @LogMethod
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
        Assert.assertEquals("Expected to insert 3 rows.", 3, insertResp.getRowsAffected().intValue());

        Long fordId = null;
        Long toyotaId = null;
        Long hondaId = null;

        for (Map<String, Object> row : insertResp.getRows())
        {
            Long rowId = (Long)row.get("RowId");
            String name = (String)row.get("Name");
            Assert.assertNotNull("Expected response row to have a Name column", name);
            Assert.assertNotNull("Expected response row to have a RowId column", rowId);
            if (name.equalsIgnoreCase("Ford"))
                fordId = rowId;
            else if (name.equalsIgnoreCase("Toyota"))
                toyotaId = rowId;
            else if (name.equalsIgnoreCase("Honda"))
                hondaId = rowId;
        }
        Assert.assertTrue("Expected rowids for all Manufacturers", fordId != null && toyotaId != null && hondaId != null);

        log("** Inserting new Models via javas client api...");
        insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Models");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.<String, Object>of("ManufacturerId", toyotaId,
                                        "Name", "Prius C"),
                Maps.<String, Object>of("ManufacturerId", toyotaId,
                                        "Name", "Camry"),
                Maps.<String, Object>of("ManufacturerId", fordId,
                                        "Name", "Focus"),
                Maps.<String, Object>of("ManufacturerId", fordId,
                                        "Name", "F150"),
                Maps.<String, Object>of("ManufacturerId", fordId,
                                        "Name", "Pinto")
        ));
        insertResp = insertCmd.execute(cn, getProjectName());
        Assert.assertEquals("Expected to insert 5 rows.", 5, insertResp.getRowsAffected().intValue());

        Long priusId = null;
        Long f150Id = null;

        for (Map<String, Object> row : insertResp.getRows())
        {
            Long rowId = (Long)row.get("RowId");
            String name = (String)row.get("Name");
            if (name.equalsIgnoreCase("Prius C"))
                priusId = rowId;
            else if (name.equalsIgnoreCase("F150"))
                f150Id = rowId;
        }
        Assert.assertNotNull(priusId);
        Assert.assertNotNull(f150Id);

        // update a row in models
        UpdateRowsCommand updateCmd = new UpdateRowsCommand(VEHICLE_SCHEMA, "Models");
        updateCmd.getRows().addAll(Arrays.asList(
                Maps.<String, Object>of(
                        "RowId", priusId,
                        "Name", "Prius"
                )
        ));
        updateCmd.execute(cn, getProjectName());

        log("** Testing vehicle.Manufacturers default queryDetailsRow.view url link...");
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=" + VEHICLE_SCHEMA + "&query.queryName=Manufacturers&query.Name~eq=Toyota");
        clickAndWait(Locator.linkWithText("details"));
        assertTextPresent("Name");
        assertTextPresent("Toyota");

        log("** Testing vehicle.Model RowId url link...");
        beginAt("/query/" + getProjectName() + "/begin.view?schemaName=" + VEHICLE_SCHEMA);
        viewQueryData(VEHICLE_SCHEMA, "Models");
        clickAndWait(Locator.linkWithText("Prius"));
        assertTextPresent("Hooray!");
        String rowidStr = getText(Locator.id("model.rowid"));
        int rowid = Integer.parseInt(rowidStr);
        Assert.assertTrue("Expected rowid on model.html page", rowid > 0);

        log("** Testing query of vehicle schema...");
        beginAt("/query/" + getProjectName() + "/schema.view?schemaName=" + VEHICLE_SCHEMA);
        viewQueryData(VEHICLE_SCHEMA, "Toyotas", "simpletest");
        
        assertTextPresent("Prius");
        assertTextPresent("Camry");

        // Issue 15595: Generic query details links for tables and queries
        // reenable this check once default details links are provided for queries.
        //log(".. generic details link should include _RowId as pk");
        //clickAndWait(Locator.linkWithText("details"));
        //assertTextPresent("Name");
        //assertTextPresent("Toyota");

        log("** Inserting colors...");
        insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Colors");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.<String, Object>of("Name", "Red", "Hex", "#FF0000"),
                Maps.<String, Object>of("Name", "Green", "Hex", "#00FF00"),
                Maps.<String, Object>of("Name", "Blue", "Hex", "#0000FF")
        ));
        insertResp = insertCmd.execute(cn, getProjectName());
        Assert.assertEquals("Expected to insert 3 rows.", 3, insertResp.getRowsAffected().intValue());

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
        Assert.assertEquals("Expected to insert 2 rows.", 2, insertResp.getRowsAffected().intValue());

        Long[] vehicleIds = new Long[2];
        vehicleIds[0] = (Long)(insertResp.getRows().get(0).get("RowId"));
        vehicleIds[1] = (Long)(insertResp.getRows().get(1).get("RowId"));

        log("** Trying to update Vehicle from wrong container...");
        updateCmd = new UpdateRowsCommand(VEHICLE_SCHEMA, "Vehicles");
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
            Assert.fail("Expected to throw CommandException");
        }
        catch (CommandException ex)
        {
            Assert.assertEquals(401, ex.getStatusCode());
    //            Assert.assertEquals("The row is from the wrong container.", ex.getMessage());
        }

        // Make sure that the schema isn't resolved if the module is not enabled in the container
        try
        {
            SaveRowsResponse updateRows = updateCmd.execute(cn, "Shared");
            Assert.fail("Expected to throw CommandException");
        }
        catch (CommandException ex)
        {
            Assert.assertEquals("The schema 'vehicle' does not exist.", ex.getMessage());
        }

        log("** Updating vehicles...");
        SaveRowsResponse updateRows = updateCmd.execute(cn, getProjectName());
        Assert.assertEquals("Expected to update 1 row.", 1, updateRows.getRowsAffected().intValue());
        Assert.assertEquals(4, ((Number)(updateRows.getRows().get(0).get("Milage"))).intValue());


        log("** Testing vehicle.Vehicles details url link...");
        beginAt("/query/" + getProjectName() + "/schema.view?schemaName=" + VEHICLE_SCHEMA);
        viewQueryData(VEHICLE_SCHEMA, "Vehicles");
        clickAndWait(Locator.linkWithText("details"));
        assertTextPresent("Hooray!");
        rowidStr = getText(Locator.id("vehicle.rowid"));
        rowid = Integer.parseInt(rowidStr);
        Assert.assertTrue("Expected rowid on vehicle.html page", rowid > 0);


        log("** Insert vehicle into subfolder...");
        insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.<String, Object>of(
                        "ModelId", priusId,
                        "Color", "Red!",
                        "ModelYear", Integer.valueOf(3000),
                        "Milage", Integer.valueOf(3000),
                        "LastService", new Date(2011, 1, 1)
                )
        ));
        insertResp = insertCmd.execute(cn, getProjectName() + "/" + FOLDER_NAME);
        Assert.assertEquals("Expected to insert 1 row.", 1, insertResp.getRowsAffected().intValue());
        
        log("** Select with url containerFilter");
        SelectRowsCommand selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(ContainerFilter.CurrentAndSubfolders);
        SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());
        Assert.assertEquals("Expected to select 3 rows.", 3, selectResp.getRowCount().intValue());

        log("** Select with customView with containerFilter");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setViewName("VehiclesInCurrentAndSubfolders");
        selectResp = selectCmd.execute(cn, getProjectName());
        Assert.assertEquals("Expected to select 3 rows.", 3, selectResp.getRowCount().intValue());

        log("** Select with customView with containerFilter, override with url containerFilter");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(ContainerFilter.Current);
        selectCmd.setViewName("VehiclesInCurrentAndSubfolders");
        selectResp = selectCmd.execute(cn, getProjectName());
        Assert.assertEquals("Expected to select 2 rows.", 2, selectResp.getRowCount().intValue());

        log("** Select with no container filter");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectResp = selectCmd.execute(cn, getProjectName());
        Assert.assertEquals("Expected to select 2 rows.", 2, selectResp.getRowCount().intValue());

        DeleteRowsCommand deleteCmd = new DeleteRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        deleteCmd.setRows(selectResp.getRows());
        try
        {
            log("** Trying to delete Vehicles from a different container");
            SaveRowsResponse deleteResp = deleteCmd.execute(cn, getProjectName() + "/" + FOLDER_NAME);
            Assert.fail("Expected to throw CommandException");
        }
        catch (CommandException ex)
        {
            Assert.assertEquals(401, ex.getStatusCode());
//            Assert.assertEquals("The row is from the wrong container.", ex.getMessage());
        }
    }

    @LogMethod
    private void doTestTableAudit() throws Exception
    {
        goToSchemaBrowser();
        pushLocation();

        // manufacturers should have an audit level of summary
        selectSchema(VEHICLE_SCHEMA);
        selectQuery(VEHICLE_SCHEMA, "Manufacturers");

        assertLinkPresentWithText("view history");
        clickAndWait(Locator.linkContainingText("view history"));

        DataRegionTable table = new DataRegionTable("audit", this, false, true);
        Assert.assertEquals("3 row(s) were inserted.", table.getDataAsText(0, "Comment"));

        // models should have an audit level of detailed
        popLocation();
        selectSchema(VEHICLE_SCHEMA);
        selectQuery(VEHICLE_SCHEMA, "Models");
        pushLocation();

        assertLinkPresentWithText("view history");
        clickAndWait(Locator.linkContainingText("view history"));

        table = new DataRegionTable("audit", this, false, true);
        Assert.assertEquals("Row was updated.", table.getDataAsText(0, "Comment"));
        Assert.assertEquals("A row was inserted.", table.getDataAsText(1, "Comment"));

        // click the details link
        pushLocation();
        table.clickLink(1,0);
        assertElementPresent(Locator.xpath("//span[@class='labkey-nav-page-header' and text() = 'Audit Details']"));
        //assertElementPresent(Locator.xpath("//td[text() = 'Pinto']"));

        popLocation();
        table.clickLink(5,0);
        assertElementPresent(Locator.xpath("//span[@class='labkey-nav-page-header' and text() = 'Audit Details']"));
        assertElementPresent(Locator.xpath("//i[text() = 'A row was inserted.']"));

        // check the row level audit details
        popLocation();
        selectSchema(VEHICLE_SCHEMA);
        selectQuery(VEHICLE_SCHEMA, "Models");
        assertLinkPresentWithText("view data");
        clickAndWait(Locator.linkContainingText("view data"));

        table = new DataRegionTable("query", this, true, true);
        table.clickLink(0,1);

        assertElementPresent(Locator.xpath("//span[@class='labkey-nav-page-header' and text() = 'Details']"));
        table = new DataRegionTable("audit", this, false, true);
        Assert.assertEquals("Row was updated.", table.getDataAsText(0, "Comment"));
        Assert.assertEquals("A row was inserted.", table.getDataAsText(1, "Comment"));

        // click the details link
        table.clickLink(0,0);
        assertElementPresent(Locator.xpath("//span[@class='labkey-nav-page-header' and text() = 'Audit Details']"));
        assertElementPresent(Locator.xpath("//td[contains(text(), 'Prius C') and contains(text(), 'Prius')]"));

        goToSchemaBrowser();
    }

    @LogMethod
    private void cleanupSchema(Connection cn) throws IOException
    {
        // enable simpletest module in Home so we can delete from all containers
        enableModule("Home", "simpletest");

        cleanupTable(cn, "Vehicles");
        cleanupTable(cn, "Models");
        cleanupTable(cn, "Manufacturers");
        cleanupTable(cn, "Colors");
    }

    @LogMethod
    private void cleanupTable(Connection cn, String tableName) throws IOException
    {
        log("** Deleting all " + tableName + " in all containers");
        try
        {
            SelectRowsCommand selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, tableName);
            selectCmd.setMaxRows(-1);
            selectCmd.setContainerFilter(ContainerFilter.AllFolders);
            selectCmd.setColumns(Arrays.asList("*"));
            SelectRowsResponse selectResp = selectCmd.execute(cn, "Home");

            if (selectResp.getRowCount().intValue() > 0)
            {
                String keyField = selectResp.getMetaData().get("id").toString();

                Map<String, List<Map<String, Object>>> rowsByContainer = new LinkedHashMap<String, List<Map<String, Object>>>();
                for (Map<String, Object> row : selectResp.getRows())
                {
                    log("  ... found row: " + row);
                    Row convertedRow = new RowMap(row);

                    String container = null;
                    if (convertedRow.getValue("Container") != null)
                        container = convertedRow.getValue("Container").toString();

                    Map<String, Object> newRow = new HashMap<String, Object>();
                    Object value = convertedRow.getValue(keyField);
                    newRow.put(keyField, value);

                    List<Map<String, Object>> rows = rowsByContainer.get(container);
                    if (rows == null)
                        rowsByContainer.put(container, rows = new ArrayList<Map<String, Object>>());
                    rows.add(newRow);
                }

                for (String container : rowsByContainer.keySet())
                {
                    String c = container == null ? "Home" : container;
                    log("  ... deleting all " + tableName + " from container '" + c + "'");
                    DeleteRowsCommand deleteCmd = new DeleteRowsCommand(VEHICLE_SCHEMA, tableName);
                    List<Map<String, Object>> rows = rowsByContainer.get(container);
                    deleteCmd.setRows(rows);
                    deleteCmd.execute(cn, c);
                }
                
                Assert.assertEquals("Expected no rows remaining", 0, selectCmd.execute(cn, "Home").getRowCount().intValue());
            }
        }
        catch (CommandException e)
        {
            // Don't log project not found error
            if (e.getStatusCode() != 404)
            {
                log("** Error during cleanupTable:");
                e.printStackTrace(System.out);
            }
        }
    }

    @LogMethod
    private void doTestViews()
    {
        log("Testing views in modules...");
        //begin.view should display when clicking on the module's tab
        goToModule(MODULE_NAME);
        assertTextPresent("This is the begin view from the test module");

        //navigate to other view
        clickAndWait(Locator.linkWithText("other view"));
        assertTextPresent("This is another view in the simple test module");
    }

    @LogMethod
    private void doTestWebParts()
    {
        log("Testing web parts in modules...");
        //go to project portal
        clickFolder(getProjectName());

        //add Simple Module Web Part
        addWebPart("Simple Module Web Part");
        assertTextPresent("This is a web part view in the simple test module");

        Boolean value = (Boolean)executeScript("return LABKEY.moduleContext.simpletest.scriptLoaded");
        Assert.assertTrue("Module context not being loaded propertly", value);
    }

    @LogMethod
    private void createList()
    {
        //create a list for our query
        clickFolder(getProjectName());
        addWebPart("Lists");

        log("Creating list for query/view/report test...");
        _listHelper.createList(getProjectName(), LIST_NAME,
                ListHelperWD.ListColumnType.AutoInteger, "Key",
                new ListHelperWD.ListColumn("Name", "Name", ListHelperWD.ListColumnType.String, "Name"),
                new ListHelperWD.ListColumn("Age", "Age", ListHelperWD.ListColumnType.Integer, "Age"),
                new ListHelperWD.ListColumn("Crazy", "Crazy", ListHelperWD.ListColumnType.Boolean, "Crazy?"));

        log("Importing some data...");
        clickButton("Import Data");
        _listHelper.submitTsvData(LIST_DATA);

        log("Create list in subfolder to prevent query validation failure");
        _listHelper.createList(FOLDER_NAME, LIST_NAME,
                ListHelperWD.ListColumnType.AutoInteger, "Key",
                new ListHelperWD.ListColumn("Name", "Name", ListHelperWD.ListColumnType.String, "Name"),
                new ListHelperWD.ListColumn("Age", "Age", ListHelperWD.ListColumnType.Integer, "Age"),
                new ListHelperWD.ListColumn("Crazy", "Crazy", ListHelperWD.ListColumnType.Boolean, "Crazy?"));

        log("Create list in container tab containers to prevent query validation failure");
        _listHelper.createListFromTab("Study Container", LIST_NAME,
                ListHelperWD.ListColumnType.AutoInteger, "Key",
                new ListHelperWD.ListColumn("Name", "Name", ListHelperWD.ListColumnType.String, "Name"),
                new ListHelperWD.ListColumn("Age", "Age", ListHelperWD.ListColumnType.Integer, "Age"),
                new ListHelperWD.ListColumn("Crazy", "Crazy", ListHelperWD.ListColumnType.Boolean, "Crazy?"));

        _listHelper.createListFromTab("Assay Container", LIST_NAME,
                ListHelperWD.ListColumnType.AutoInteger, "Key",
                new ListHelperWD.ListColumn("Name", "Name", ListHelperWD.ListColumnType.String, "Name"),
                new ListHelperWD.ListColumn("Age", "Age", ListHelperWD.ListColumnType.Integer, "Age"),
                new ListHelperWD.ListColumn("Crazy", "Crazy", ListHelperWD.ListColumnType.Boolean, "Crazy?"));
    }

    @LogMethod
    private void doTestQueries()
    {
        log("Testing queries in modules...");

        //go to query module portal
        clickFolder(getProjectName());
        goToModule("Query");
        viewQueryData("lists", "TestQuery");

        assertTextPresent("Adam");
        assertTextPresent("Dave");
        assertTextPresent("Josh");
        assertTextNotPresent("Britt");
    }

    @LogMethod
    private void doTestQueryViews()
    {
        log("Testing module-based custom query views...");
        clickFolder(getProjectName());
        clickAndWait(Locator.linkWithText(LIST_NAME));

        clickMenuButton("Views", "Crazy People");
        assertTextPresent("Adam");
        assertTextPresent("Dave");
        assertTextPresent("Josh");
        assertTextNotPresent("Britt");

        //custom view has a sort by age descending
        assertTextBefore("Adam", "Josh");

        //Issue 11307: Inconsistencies saving session view over file-based view
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("CreatedBy");
        _customizeViewsHelper.applyCustomView();
        assertTextPresent("is unsaved");
        assertTextPresent("Created By");
        _customizeViewsHelper.saveUnsavedViewGridClosed(null);
        waitForText("Crazy People Copy");

    }

    @LogMethod
    private void doTestReports()
    {
        RReportHelperWD _rReportHelper = new RReportHelperWD(this);
        _rReportHelper.ensureRConfig();

        log("Testing module-based JS reports...");
        clickFolder(getProjectName());
        clickAndWait(Locator.linkWithText(LIST_NAME));
        clickMenuButton("Views", "Want To Be Cool");
        waitForText("Less cool than expected. Loaded dependent scripts.", WAIT_FOR_JAVASCRIPT);

        clickFolder(getProjectName());
        addWebPart("Report");
        setFormElement("title", "Report Tester Part");
        selectOptionByValue("reportId", "module:simpletest/reports/schemas/lists/People/Less Cool JS Report.js");
        clickButton("Submit");
        waitForText("Less cool than expected. Loaded dependent scripts.", WAIT_FOR_JAVASCRIPT);

        String WikiName = "JS Report Wiki";
        addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement("name", WikiName);
        setFormElement("title", WikiName);
        setWikiBody("placeholder text");
        saveWikiPage();
        setSourceFromFile("jsReportTest.html", WikiName);
        waitForText("Console output", WAIT_FOR_JAVASCRIPT);
        waitForText("Less cool than expected. Loaded dependent scripts.", 2, WAIT_FOR_JAVASCRIPT);
        assertTextPresent("JS Module Report");
        assertTextPresent("Hello, Bob!");

        log("Testing module-based reports...");
        clickAndWait(Locator.linkWithText(LIST_NAME));
        clickMenuButton("Views", "Super Cool R Report");
        waitForText("Console output", WAIT_FOR_JAVASCRIPT);
        assertTextPresent("\"name\"");
        assertTextPresent("\"age\"");
        assertTextPresent("\"crazy\"");
    }

    @LogMethod
    private void doTestImportTemplates() throws Exception
    {
        log("Testing import templates...");

        //go to query module portal
        clickFolder(getProjectName());
        goToModule("Query");
        viewQueryData(VEHICLE_SCHEMA, "Vehicles");
        clickButton("Import Data");
        Assert.assertTrue("Import message not present", isTextPresent("Please read this before you import data"));

        Locator l = Locator.xpath("//select[@id='importTemplate']//option");
        Assert.assertTrue("Wrong number of templates found", getXpathCount((Locator.XPathLocator)l) == 2);
    }

    @LogMethod
    private void doTestContainerColumns() throws Exception
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        log("** Testing container columns");
        SelectRowsCommand selectCmd = new SelectRowsCommand(CORE_SCHEMA, "Containers");
        selectCmd.setMaxRows(-1);
        List<String> columns = new ArrayList<String>();
        columns.add("*");
        selectCmd.setColumns(columns);
        selectCmd.setRequiredVersion(9.1);
        SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());
        Assert.assertEquals("Expected to select 1 rows.", 1, selectResp.getRowCount().intValue());

        Map<String,Object> row = selectResp.getRows().get(0);
        String entityId = (String)((JSONObject)row.get("EntityId")).get("value");
        Assert.assertEquals("Expected core.containers path column to return the string: /" + getProjectName(), "/" + getProjectName(), ((JSONObject)row.get("Path")).get("value"));

        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setColumns(columns);
        selectCmd.setRequiredVersion(9.1);
        selectResp = selectCmd.execute(cn, getProjectName());
        JSONObject vehicleRow = (JSONObject)(selectResp.getRows().get(0)).get("container");

        Assert.assertEquals("Expected vehicles.container to return the value: " + entityId, entityId, vehicleRow.get("value"));
        Assert.assertEquals("Expected vehicles.container to return the displayValue: " + getProjectName(), getProjectName(), vehicleRow.get("displayValue"));

    }

    @LogMethod
    private void doTestInsertUpdateViews() throws IOException, CommandException
    {
        log("Testings custom views for insert/update/details");

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        log("** using selectRows to test details view, which has been customized in schema XML only");
        SelectRowsCommand selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setViewName("~~DETAILS~~");
        selectCmd.setMaxRows(-1);
        SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());

        String[] expectedCols = new String[]{"ModelId", "ModelYear", "Milage", "LastService", "RowId"};
        Assert.assertEquals("Expected to return " + expectedCols.length + " columns, based on the saved view", expectedCols.length, selectResp.getColumnModel().size());
        for (String col : expectedCols)
        {
            Assert.assertNotNull("Details view does not contain column: " + col, selectResp.getColumnModel(col));
        }

        log("** using selectRows to test insert view");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setViewName("~~INSERT~~");
        selectCmd.setMaxRows(-1);
        selectResp = selectCmd.execute(cn, getProjectName());

        expectedCols = new String[]{"RowId", "ModelYear", "Milage", "ModelId/ManufacturerId/Name"};
        Assert.assertEquals("Expected to return " + expectedCols.length + " columns, based on the saved view", expectedCols.length, selectResp.getColumnModel().size());

        for (String col : expectedCols)
        {
            Assert.assertNotNull("Insert view does not contain column: " + col, selectResp.getColumnModel(col));
        }

        log("** using selectRows to test update view");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setViewName("~~UPDATE~~");
        selectCmd.setMaxRows(-1);
        selectResp = selectCmd.execute(cn, getProjectName());

        expectedCols = new String[]{"RowId", "ModelYear", "Milage", "LastService", "ModelId/ManufacturerId/Name"};
        Assert.assertEquals("Expected to return " + expectedCols.length + " columns, based on the saved view", expectedCols.length, selectResp.getColumnModel().size());

        for (String col : expectedCols)
        {
            Assert.assertNotNull("Update view does not contain column: " + col, selectResp.getColumnModel(col));
        }
    }

    @LogMethod
    private void doTestParameterizedQueries()
    {
        log("Create embedded QWP to test parameterized query.");
        clickAndWait(Locator.linkWithText(FOLDER_NAME));
        goToModule("Wiki");
        createNewWikiPage();
        setFormElement("wiki-input-name", "Parameterized QWP");
        setWikiBody(getFileContents("/server/test/modules/simpletest/views/parameterizedQWP.html"));
        clickButton("Save & Close");

        log("Check that parameterized query doesn't cause page load.");
        setFormElement(Locator.id("search-input"), MODULE_NAME);
        waitForElement(Locator.xpath("//input[contains(@name, 'param.STARTS_WITH')]"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[contains(@name, 'param.STARTS_WITH')]"), "P");
        clickButton("Submit", 0);
        waitForText("Manufacturer");
        Assert.assertEquals("Unexpected page refresh.", MODULE_NAME, getFormElement(Locator.id("headerSearchInput-inputEl")));
        assertTextPresent("Pinto");
        assertTextNotPresent("Prius");
    }

    @LogMethod
    private void doTestFilterSort() throws Exception
    {
        log("** Testing filtering and sorting via java API...");

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        log("** Select using selectRows and a view with a filter in it");
        SelectRowsCommand selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setViewName("Filter On Letter P");
        SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());
        Assert.assertEquals("Expected to select 1 rows.", 1, selectResp.getRowCount().intValue());
        Assert.assertEquals("Expected to return 3 columns, based on the saved view", 3, selectResp.getColumnModel().size());

        log("** Select using selectRows and a view with a sort in it");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setViewName("SortOnModelYear");
        selectResp = selectCmd.execute(cn, getProjectName());
        Assert.assertEquals("Expected first row to be 2001.", 2001, selectResp.getRows().get(0).get("ModelYear"));
        Assert.assertEquals("Expected first row to be 2000.", 2000, selectResp.getRows().get(1).get("ModelYear"));
        Assert.assertTrue("Expected the column 'ModelId/ManufacturerId/Name' to be included based on the default view", selectResp.getColumnModel("ModelId/ManufacturerId/Name") != null);
        Assert.assertEquals("Expected to return 6 columns, based on the default view", 6, selectResp.getColumnModel().size());

    }

    @LogMethod
    private void doTestModuleProperties() throws Exception
    {
        String prop1 = "TestProp1";
        String prop1Value = "Prop1Value";
        String prop2 = "TestProp2";

        beginAt("/project/" + getProjectName() + "/" + FOLDER_NAME +"/begin.view?");
        addWebPart("Simple Module Web Part");
        waitForText("This is a web part view in the simple test module");

        Assert.assertEquals("Module context not set propertly", "DefaultValue", executeScript("return LABKEY.getModuleContext('simpletest')." + prop2));

        Map<String, List<String[]>> props = new HashMap<String, List<String[]>>();
        List<ModulePropertyValue> propList = new ArrayList<ModulePropertyValue>();
        propList.add(new ModulePropertyValue("simpletest", "/", prop1, prop1Value));
        propList.add(new ModulePropertyValue("simpletest", "/" + getProjectName() + "/" + FOLDER_NAME, prop2 , "FolderValue"));

        setModuleProperties(propList);

        beginAt("/project/" + getProjectName() + "/" + FOLDER_NAME +"/begin.view?");

        Assert.assertEquals("Module context not set propertly", prop1Value, executeScript("return LABKEY.getModuleContext('simpletest')." + prop1));
        Assert.assertEquals("Module context not set propertly", "FolderValue", executeScript("return LABKEY.getModuleContext('simpletest')." + prop2));

        goToProjectHome();
        Assert.assertEquals("Module context not set propertly", "DefaultValue", executeScript("return LABKEY.getModuleContext('simpletest')." + prop2));
    }

    @LogMethod
    private void doTestTabbedFolder()
    {
        clickFolder(FOLDER_NAME);
        waitForPageToLoad();

        //it should start on tab 2
        verifyTabSelected("Tab 2");
        log("verifying webparts present in correct order");
        assertTextPresentInThisOrder("A customized web part", "Experiment Runs", "Assay List");

        //verify Tab 1
        clickTab("Tab 1");
        waitForPageToLoad();
        assertTextPresentInThisOrder("A customized web part", "Data Pipeline", "Experiment Runs", "Run Groups", "Sample Sets", "Assay List");
        addWebPart("Messages");

        clickTab("Tab 2");
        waitForPageToLoad();

        //verify added webpart is persisted
        clickTab("Tab 1");
        waitForPageToLoad();
        assertTextPresentInThisOrder("A customized web part", "Data Pipeline", "Experiment Runs", "Run Groups", "Sample Sets", "Assay List", "Messages");

        //there is a selector for the assay controller and tab2
        clickAndWait(Locator.linkWithText("New Assay Design"));
        waitForPageToLoad();
        verifyTabSelected("Tab 2");

        //this is a controller selector
        beginAt("/query/" + getProjectName() + "/" + FOLDER_NAME + "/begin.view?");
        waitForPageToLoad();
        verifyTabSelected("Tab 1");

        //this is a view selector
        beginAt("/pipeline-status/" + getProjectName() + "/" + FOLDER_NAME + "/showList.view?");
        waitForPageToLoad();
        verifyTabSelected("Tab 2");

        //this is a regex selector
        clickFolder(FOLDER_NAME);
        waitForPageToLoad();
        addWebPart("Sample Sets");
        clickAndWait(Locator.linkWithText("Import Sample Set"));
        waitForPageToLoad();
        verifyTabSelected("Tab 1");

        // Test Container tabs
        clickTab("Assay Container");
        waitForPageToLoad();
        assertTextPresent("Assay List");
        clickTab("Study Container");
        assertTextPresent("Study Overview");
        clickAndWait(Locator.linkWithText("Create Study"));
        waitForPageToLoad();
        clickAndWait(Locator.linkWithText("Create Study"));
        waitForPageToLoad();
        assertTextPresent("Manage Study", "Study Container", "Overview", "Specimen Data");
        clickAndWait(Locator.linkWithText("Specimen Data"));
        assertTextPresent("Vial Search", "Specimens");
    }

    @LogMethod
    private void doTestContainerTabConversion()
    {
        // Set up a Collaboration folder with study and assay subfolders
        final String COLLAB_FOLDER = "collab1";
        final String STUDYCONTAINER_NAME = "Study Container Tab";
        final String STUDYTAB_NAME = "Study Container";
        final String ASSAYCONTAINER_NAME = "Assay Container Tab 2";
        final String ASSAYTAB_NAME = "Assay Container";
        final String COLLABFOLDER_PATH = getProjectName() + "/" + COLLAB_FOLDER;
        final String EXTRA_ASSAY_WEBPART = "Run Groups";
        clickFolder(getProjectName());
        _containerHelper.createSubfolder(getProjectName(), COLLAB_FOLDER, "Collaboration");
        _containerHelper.createSubfolder(COLLABFOLDER_PATH, STUDYCONTAINER_NAME, "Study");
        _containerHelper.createSubfolder(COLLABFOLDER_PATH, ASSAYCONTAINER_NAME, "Assay");
        clickAndWait(Locator.linkWithText(COLLAB_FOLDER));
        clickAndWait(Locator.linkWithText(STUDYCONTAINER_NAME));
        assertTextPresent("Study Overview");
        clickAndWait(Locator.linkWithText("Create Study"));
        clickAndWait(Locator.linkWithText("Create Study"));
        clickAndWait(Locator.linkWithText(COLLAB_FOLDER));
        clickAndWait(Locator.linkWithText(ASSAYCONTAINER_NAME));
        addWebPart(EXTRA_ASSAY_WEBPART);

        // Change folder type to XML Tabbed
        clickAndWait(Locator.linkWithText(COLLAB_FOLDER));
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton("folderType", TABBED_FOLDER_TYPE);
        clickAndWait(Locator.linkWithText("Update Folder"));

        // Verify that subfolders got moved into tabs
        assertTextPresent(STUDYTAB_NAME, ASSAYTAB_NAME);
        clickAndWait(Locator.linkWithText(STUDYTAB_NAME));
        assertTextPresent("Study Overview");
        clickAndWait(Locator.linkWithText("Specimen Data"));
        assertTextPresent("Vial Search", "Import Specimens");
        clickAndWait(Locator.linkWithText(ASSAYTAB_NAME));
        assertTextPresent("Assay List");
        assertTextPresent(EXTRA_ASSAY_WEBPART);

        // Change back to Collab
        clickAndWait(Locator.linkWithText(COLLAB_FOLDER));
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton("folderType", "Collaboration");
        clickAndWait(Locator.linkWithText("Update Folder"));

        // Study and Assay should be hidden now
        assertTextNotPresent(STUDYTAB_NAME, ASSAYTAB_NAME, STUDYCONTAINER_NAME, ASSAYCONTAINER_NAME);

        // Now change back to TABBED
        clickAndWait(Locator.linkWithText(COLLAB_FOLDER));
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton("folderType", TABBED_FOLDER_TYPE);
        clickAndWait(Locator.linkWithText("Update Folder"));

        // Verify that folder tabs are back
        assertTextPresent(STUDYTAB_NAME, ASSAYTAB_NAME);
        clickAndWait(Locator.linkWithText(STUDYTAB_NAME));
        assertTextPresent("Study Overview");
        clickAndWait(Locator.linkWithText("Specimen Data"));
        assertTextPresent("Vial Search", "Import Specimens");
        clickAndWait(Locator.linkWithText(ASSAYTAB_NAME));
        assertTextPresent("Assay List");
        assertTextPresent(EXTRA_ASSAY_WEBPART);

        deleteFolder(getProjectName(), COLLAB_FOLDER);
    }

    protected void assertModuleDeployed(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is deployed");
        goToAdminConsole();
        assertTextPresent(moduleName);
    }

    protected void assertModuleEnabledByDefault(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is enabled");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        assertElementPresent(Locator.xpath("//input[@type='checkbox' and @checked and @disabled and @title='" + moduleName + "']"));
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        try
        {
            cleanupSchema(cn);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        super.doCleanup(afterTest);
        log("Cleaned up SimpleModuleTest project.");
    }

    public String getAssociatedModuleDirectory()
    {
        //return "null" to skip verification of module directory
        //as it won't exist until after the test starts running the first time
        return null;
    }

}

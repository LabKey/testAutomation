/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.EditDatasetDefinitionPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
* Tests the simple module and file-based resources introduced in version 9.1
*/
@Category({DailyA.class})
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

    public static final String STUDY_FOLDER_TAB_NAME = "Study Container Tab";
    public static final String ASSAY_FOLDER_TAB_NAME = "Assay Container Tab 2";
    public static final String STUDY_FOLDER_TAB_LABEL = "Study Container";
    public static final String ASSAY_FOLDER_TAB_LABEL = "Assay Container";

    public static final String RESTRICTED_MODULE_NAME = "restrictedModule";
    public static final String RESTRICTED_FOLDER_NAME = "Restricted Folder";
    public static final String RESTRICTED_FOLDER_TYPE = "Folder With Restricted Module";
    public static final String NEW_FOLDER_NAME = "New Folder";
    public static final String RESTRICTED_FOLDER_IMPORT_NAME =
            "/sampledata/SimpleAndRestrictedModule/FolderWithRestricted.folder.zip";

    private static final String THUMBNAIL_FOLDER = "thumbnails/";
    private static final String THUMBNAIL_FILENAME = "/Thumbnail.png";
    private static final String ICON_FILENAME = "/SmallThumbnail.png";

    private static final String KNITR_PEOPLE = "Knitr People";
    private static final String SUPER_COOL_R_REPORT = "Super Cool R Report";
    private static final String WANT_TO_BE_COOL = "Want To Be Cool";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final PortalHelper portalHelper = new PortalHelper(this);

    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    @LogMethod
    public static void initTest()
    {
        SimpleModuleTest init = (SimpleModuleTest) getCurrentTest();
        init.doSetup();
    }
    
    protected void doSetup()
    {
        assertModuleDeployed(MODULE_NAME);
        _containerHelper.createProject(getProjectName(), FOLDER_TYPE);
        assertModuleEnabledByDefault("Portal");
        assertModuleEnabledByDefault(MODULE_NAME);
        assertModuleEnabledByDefault("Query");

        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME, TABBED_FOLDER_TYPE);
        assertModuleEnabledByDefault("Portal");
        assertModuleEnabledByDefault(MODULE_NAME);
        assertModuleEnabledByDefault("Query");
        assertModuleEnabledByDefault("Study");

        goToProjectHome();
        portalHelper.addWebPart("Data Views");
        for (int i = 0; i < 5; i ++)
            portalHelper.moveWebPart("Data Views", PortalHelper.Direction.UP);

        goToProjectSettings();
        setFormElement(Locator.name("defaultDateFormat"), DATE_FORMAT);
        clickAndWait(Locator.lkButton("Save"));

        goToProjectHome();
    }

    @Test
    public void testSteps() throws Exception
    {
        createList();
        doVerifySteps();
    }

    @LogMethod
    protected void doVerifySteps() throws Exception
    {
        doTestRestrictedModule();
        doTestCustomFolder();
        doTestSchemas();
        doTestTableAudit();
        doTestViews();
        doTestWebParts();
        doTestQueries();
        doTestQueryViews();
        doTestReports();
        doTestInsertUpdateViews();
        doTestParameterizedQueries();
        doTestContainerColumns();
        doTestFilterSort();
        doTestImportTemplates();
        doTestDatasetsAndFileBasedQueries();
        doTestViewEditing();
    }

    @LogMethod
    private void doTestCustomFolder()
    {
        clickProject(getProjectName());
        PortalHelper portalHelper = new PortalHelper(this);

        assertTextPresentInThisOrder("A customized web part", "Data Pipeline", "Experiment Runs", "Sample Sets", "Assay List");
        assertTextPresent("Run Groups");
        assertElementNotPresent(Locator.linkWithText("Create Run Group")); // Not in small Run Groups web-part.
        portalHelper.checkWebpartPermission("A customized web part", "Read", null);
        portalHelper.checkWebpartPermission("Data Pipeline", "Read", null);
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
                Maps.of("Name", "Ford"),
                Maps.of("Name", "Toyota"),
                Maps.of("Name", "Honda")
        ));
        SaveRowsResponse insertResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Expected to insert 3 rows.", 3, insertResp.getRowsAffected().intValue());

        Long fordId = null;
        Long toyotaId = null;
        Long hondaId = null;

        for (Map<String, Object> row : insertResp.getRows())
        {
            Long rowId = (Long) row.get("RowId");
            String name = (String) row.get("Name");
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
                Maps.of("ManufacturerId", toyotaId,
                        "Name", "Prius C",
                        "InitialReleaseYear", null),
                Maps.of("ManufacturerId", toyotaId,
                        "Name", "Camry",
                        "InitialReleaseYear", 1982),
                Maps.of("ManufacturerId", fordId,
                        "Name", "Focus"),
                Maps.of("ManufacturerId", fordId,
                        "Name", "F150"),
                Maps.of("ManufacturerId", fordId,
                        "Name", "Pinto")
        ));
        insertResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Expected to insert 5 rows.", 5, insertResp.getRowsAffected().intValue());

        Long priusId = null;
        Long f150Id = null;

        for (Map<String, Object> row : insertResp.getRows())
        {
            Long rowId = (Long) row.get("RowId");
            String name = (String) row.get("Name");
            if (name.equalsIgnoreCase("Prius C"))
                priusId = rowId;
            else if (name.equalsIgnoreCase("F150"))
                f150Id = rowId;
        }
        assertNotNull(priusId);
        assertNotNull(f150Id);

        // update a row in models
        UpdateRowsCommand updateCmd = new UpdateRowsCommand(VEHICLE_SCHEMA, "Models");
        updateCmd.getRows().addAll(Arrays.asList(
                Maps.of(
                        "RowId", priusId,
                        "Name", "Prius"
                )
        ));
        updateCmd.execute(cn, getProjectName());

        log("** Testing vehicle.Manufacturers default queryDetailsRow.view url link...");
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=" + VEHICLE_SCHEMA + "&query.queryName=Manufacturers&query.Name~eq=Toyota");
        DataRegionTable table = new DataRegionTable("query", getDriver());
        clickAndWait(table.detailsLink(0));
        assertTextPresent("Name", "Toyota");

        log("** Testing vehicle.Model RowId url link...");
        beginAt("/query/" + getProjectName() + "/begin.view?");
        viewQueryData(VEHICLE_SCHEMA, "Models");
        clickAndWait(Locator.linkWithText("Prius"));
        assertTextPresent("Hooray!");
        String rowidStr = getText(Locator.id("model.rowid"));
        int rowid = Integer.parseInt(rowidStr);
        assertTrue("Expected rowid on model.html page", rowid > 0);


        log("** Testing url expression null behavior for null InitialReleaseYear...");
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=vehicle&query.queryName=Models&query.InitialReleaseYear~isblank=");
        DataRegionTable modelsGrid = new DataRegionTable("query", this);

        // URLs with a bad (missing) column should result in no URL being rendered
        assertFalse("Expected to not find URL on 'urlBadColumnWithDefaultBehavior' column", modelsGrid.hasHref(0, "urlBadColumnWithDefaultBehavior"));
        assertFalse("Expected to not find URL on 'urlBadColumnWithNullResult' column", modelsGrid.hasHref(0, "urlBadColumnWithNullResult"));
        assertFalse("Expected to not find URL on 'urlBadColumnWithNullValue' column", modelsGrid.hasHref(0, "urlBadColumnWithNullValue"));
        assertFalse("Expected to not find URL on 'urlBadColumnWithBlankValue' column", modelsGrid.hasHref(0, "urlBadColumnWithBlankValue"));

        // ... except if the missing column has a defaultValue
        String href = modelsGrid.getHref(0, "urlBadColumnWithDefaultValue");
        assertTrue("Expected 'urlBadColumnWithDefaultValue' to replace missing token with default 'fred' string: " + href, href.endsWith("&doesNotExist=fred"));


        // URLs with column that is present (not missing) but is null should result in a URL with blanks
        href = modelsGrid.getHref(0, "urlNullableColumnWithDefaultBehavior");
        assertTrue("Expected 'urlNullableColumnWithDefaultBehavior' to replace missing token with blank: " + href, href.endsWith("&nullableColumn="));

        // ... except if we override the NullValueBehavior to return a null result
        assertFalse("Expected 'urlNullableColumnWithNullResult' to replace null value with null result", modelsGrid.hasHref(0, "urlNullableColumnWithNullResult"));

        href = modelsGrid.getHref(0, "urlNullableColumnWithNullValue");
        assertTrue("Expected 'urlNullableColumnWithNullValue' to replace missing token with 'null': " + href, href.endsWith("&nullableColumn=null"));

        href = modelsGrid.getHref(0, "urlNullableColumnWithBlankValue");
        assertTrue("Expected 'urlNullableColumnWithBlankValue' to replace missing token with blank: " + href, href.endsWith("&nullableColumn="));

        href = modelsGrid.getHref(0, "urlNullableColumnWithDefaultValue");
        assertTrue("Expected 'urlNullableColumnWithDefaultValue' to replace missing token with default 'fred' string: " + href, href.endsWith("&nullableColumn=fred"));


        log("** Testing url expression null behavior for non-null InitialReleaseYear...");
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=vehicle&query.queryName=Models&query.InitialReleaseYear~isnonblank=");
        modelsGrid = new DataRegionTable("query", this);

        href = modelsGrid.getHref(0, "urlNullableColumnWithDefaultBehavior");
        assertTrue("Expected 'urlNullableColumnWithDefaultBehavior' to replace missing token with value: " + href, href.endsWith("&nullableColumn=1982"));

        href = modelsGrid.getHref(0, "urlNullableColumnWithNullResult");
        assertTrue("Expected 'urlNullableColumnWithNullResult' to replace missing token with value: " + href, href.endsWith("&nullableColumn=1982"));

        href = modelsGrid.getHref(0, "urlNullableColumnWithNullValue");
        assertTrue("Expected 'urlNullableColumnWithNullValue' to replace missing token with value: " + href, href.endsWith("&nullableColumn=1982"));

        href = modelsGrid.getHref(0, "urlNullableColumnWithBlankValue");
        assertTrue("Expected 'urlNullableColumnWithBlankValue' to replace missing token with value: " + href, href.endsWith("&nullableColumn=1982"));

        href = modelsGrid.getHref(0, "urlNullableColumnWithDefaultValue");
        assertTrue("Expected 'urlNullableColumnWithDefaultValue' to replace missing token with default 'fred' string: " + href, href.endsWith("&nullableColumn=1982"));


        log("** Testing query of vehicle schema...");
        goToModule("Query");
        viewQueryData(VEHICLE_SCHEMA, "Toyotas", MODULE_NAME);

        assertTextPresent("Prius", "Camry");

        // Issue 15595: Generic query details links for tables and queries
        // reenable this check once default details links are provided for queries.
        //log(".. generic details link should include _RowId as pk");
        //clickAndWait(Locator.linkWithText("details"));
        //assertTextPresent("Name");
        //assertTextPresent("Toyota");

        log("** Inserting colors...");
        insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Colors");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.of("Name", "Red", "Hex", "#FF0000"),
                Maps.of("Name", "Green", "Hex", "#00FF00"),
                Maps.of("Name", "Blue", "Hex", "#0000FF")
        ));
        insertResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Expected to insert 3 rows.", 3, insertResp.getRowsAffected().intValue());

        log("** Inserting vechicles...");
        insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.of(
                        "ModelId", priusId,
                        "Color", "Green!",
                        "ModelYear", Integer.valueOf(2000),
                        "Milage", Integer.valueOf(3),
                        "LastService", new Date(2009, 9, 9)
                ),
                Maps.of(
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
        vehicleIds[0] = (Long) (insertResp.getRows().get(0).get("RowId"));
        vehicleIds[1] = (Long) (insertResp.getRows().get(1).get("RowId"));

        log("** Trying to update Vehicle from wrong container...");
        updateCmd = new UpdateRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        updateCmd.getRows().addAll(Arrays.asList(
                Maps.of(
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
            assertEquals(403, ex.getStatusCode());
            //assertEquals("The row is from the wrong container.", ex.getMessage());
        }

        // Make sure that the schema isn't resolved if the module is not enabled in the container
        try
        {
            SaveRowsResponse updateRows = updateCmd.execute(cn, "Shared");
            fail("Expected to throw CommandException");
        }
        catch (CommandException ex)
        {
            assertEquals("The schema 'vehicle' does not exist.", ex.getMessage());
        }

        log("** Updating vehicles...");
        SaveRowsResponse updateRows = updateCmd.execute(cn, getProjectName());
        assertEquals("Expected to update 1 row.", 1, updateRows.getRowsAffected().intValue());
        assertEquals(4, ((Number) (updateRows.getRows().get(0).get("Milage"))).intValue());


        log("** Testing vehicle.Vehicles details url link...");
        beginAt("/query/" + getProjectName() + "/schema.view?schemaName=" + VEHICLE_SCHEMA);
        viewQueryData(VEHICLE_SCHEMA, "Vehicles");
        DataRegionTable vehicles = new DataRegionTable("query", getDriver());
        clickAndWait(vehicles.detailsLink(0));
        assertTextPresent("Hooray!");
        rowidStr = getText(Locator.id("vehicle.rowid"));
        rowid = Integer.parseInt(rowidStr);
        assertTrue("Expected rowid on vehicle.html page", rowid > 0);


        log("** Insert vehicle into subfolder...");
        insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.of(
                        "ModelId", priusId,
                        "Color", "Red!",
                        "ModelYear", Integer.valueOf(3000),
                        "Milage", Integer.valueOf(3000),
                        "LastService", new Date(2011, 1, 1)
                )
        ));
        insertResp = insertCmd.execute(cn, getProjectName() + "/" + FOLDER_NAME);
        assertEquals("Expected to insert 1 row.", 1, insertResp.getRowsAffected().intValue());

        log("** Select with url containerFilter");
        SelectRowsCommand selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(ContainerFilter.CurrentAndSubfolders);
        SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());
        assertEquals("Expected to select 3 rows.", 3, selectResp.getRowCount().intValue());

        log("** Select with customView with containerFilter");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setViewName("VehiclesInCurrentAndSubfolders");
        selectResp = selectCmd.execute(cn, getProjectName());
        assertEquals("Expected to select 3 rows.", 3, selectResp.getRowCount().intValue());

        log("** Select with customView with containerFilter, override with url containerFilter");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(ContainerFilter.Current);
        selectCmd.setViewName("VehiclesInCurrentAndSubfolders");
        selectResp = selectCmd.execute(cn, getProjectName());
        assertEquals("Expected to select 2 rows.", 2, selectResp.getRowCount().intValue());

        log("** Select with no container filter");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectResp = selectCmd.execute(cn, getProjectName());
        assertEquals("Expected to select 2 rows.", 2, selectResp.getRowCount().intValue());

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
            assertEquals(403, ex.getStatusCode());
//            assertEquals("The row is from the wrong container.", ex.getMessage());
        }
    }

    @LogMethod
    private void doTestViewEditing() throws Exception
    {
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=" + VEHICLE_SCHEMA + "&query.queryName=Vehicles");

        DataRegionTable dr = new DataRegionTable("query", this);

        log("** Try to edit file-based default view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn("Color");
        CustomizeView.SaveWindow saveWindow = _customizeViewsHelper.clickSave();
        assertFalse("should not be able to select default view", saveWindow.defaultViewRadio.isEnabled());
        saveWindow.cancel();

        dr.goToView("EditableFileBasedView");

        log("** Try to edit overridable file-based view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("Color");
        saveWindow = _customizeViewsHelper.clickSave();

        assertTrue("should be able to select default view", saveWindow.defaultViewRadio.isEnabled());
        saveWindow.defaultViewRadio.check();
        Window saveError = saveWindow.saveError();
        saveError.clickButton("OK", 0);
        saveError.waitForClose();

        saveWindow.namedViewRadio.check();
        saveWindow.save();

        assertEquals("column not found", 2, dr.getColumnIndex("Color"));
    }

    @LogMethod
    private void doTestTableAudit() throws Exception
    {
        goToSchemaBrowser();
        pushLocation();

        // manufacturers should have an audit level of summary
        selectSchema(VEHICLE_SCHEMA);
        selectQuery(VEHICLE_SCHEMA, "Manufacturers");

        assertElementPresent(Locator.linkWithText("view history"));
        clickAndWait(Locator.linkContainingText("view history"));

        DataRegionTable table = new DataRegionTable("query", this);
        assertEquals("3 row(s) were inserted.", table.getDataAsText(0, "Comment"));

        // models should have an audit level of detailed
        popLocation();
        selectSchema(VEHICLE_SCHEMA);
        selectQuery(VEHICLE_SCHEMA, "Models");
        pushLocation();

        assertElementPresent(Locator.linkWithText("view history"));
        clickAndWait(Locator.linkContainingText("view history"));

        table = new DataRegionTable("query", this);
        assertEquals("Row was updated.", table.getDataAsText(0, "Comment"));
        assertEquals("A row was inserted.", table.getDataAsText(1, "Comment"));

        // click the details link
        pushLocation();
        clickAndWait(table.detailsLink(1));
        assertElementPresent(Locator.tagWithClass("div", "lk-body-title")
                .child(Locator.tagWithText("*", "Audit Details")));

        popLocation();
        table = new DataRegionTable("query", this);
        clickAndWait(table.detailsLink(5));
        assertElementPresent(Locator.tagWithClass("div", "lk-body-title")
                .child(Locator.tagWithText("*", "Audit Details")));
        assertElementPresent(Locator.xpath("//i[text() = 'A row was inserted.']"));

        // check the row level audit details
        popLocation();
        selectSchema(VEHICLE_SCHEMA);
        selectQuery(VEHICLE_SCHEMA, "Models");
        assertElementPresent(Locator.linkWithText("view data"));
        clickAndWait(Locator.linkContainingText("view data"));

        table = new DataRegionTable("query", this);
        clickAndWait(table.detailsLink(0));

        assertElementPresent(Locator.tagWithClass("div", "lk-body-title")
                .child(Locator.tagWithText("*", "Details")));
        table = new DataRegionTable("query", this);
        assertEquals("Row was updated.", table.getDataAsText(0, "Comment"));
        assertEquals("A row was inserted.", table.getDataAsText(1, "Comment"));

        // click the details link
        clickAndWait(table.detailsLink(0));
        assertElementPresent(Locator.tagWithClass("div", "lk-body-title")
                .child(Locator.tagWithText("*", "Audit Details")));
        assertElementPresent(Locator.xpath("//td[contains(text(), 'Prius C') and contains(text(), 'Prius')]"));

        goToSchemaBrowser();
    }

    @LogMethod
    private void cleanupSchema(Connection cn) throws IOException
    {
        // enable simpletest module in Home so we can delete from all containers
        _containerHelper.enableModule("Home", MODULE_NAME);

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

                Map<String, List<Map<String, Object>>> rowsByContainer = new LinkedHashMap<>();
                for (Map<String, Object> row : selectResp.getRows())
                {
                    log("  ... found row: " + row);
                    Row convertedRow = new RowMap(row);

                    String container = null;
                    if (convertedRow.getValue("Container") != null)
                        container = convertedRow.getValue("Container").toString();

                    Map<String, Object> newRow = new HashMap<>();
                    Object value = convertedRow.getValue(keyField);
                    newRow.put(keyField, value);

                    List<Map<String, Object>> rows = rowsByContainer.get(container);
                    if (rows == null)
                        rowsByContainer.put(container, rows = new ArrayList<>());
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
                
                assertEquals("Expected no rows remaining", 0, selectCmd.execute(cn, "Home").getRowCount().intValue());
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
        clickProject(getProjectName());

        //add Simple Module Web Part
        new PortalHelper(this).addWebPart("Simple Module Web Part");
        assertTextPresent("This is a web part view in the simple test module");

        Boolean value = (Boolean)executeScript("return LABKEY.moduleContext.simpletest.scriptLoaded");
        assertTrue("Module context not being loaded propertly", value);
    }

    @LogMethod
    protected void createList()
    {
        //create a list for our query
        clickProject(getProjectName());
        new PortalHelper(this).addWebPart("Lists");

        log("Creating list for query/view/report test...");
        createPeopleListInFolder(getProjectName());

        log("Importing some data...");
        clickButton("Import Data");
        _listHelper.submitTsvData(LIST_DATA);

        log("Create list in subfolder to prevent query validation failure");
        createPeopleListInFolder(FOLDER_NAME);

        log("Create list in container tab containers to prevent query validation failure");
        createPeopleListInTab(STUDY_FOLDER_TAB_LABEL);
        createPeopleListInTab(ASSAY_FOLDER_TAB_LABEL);
    }

    private void createPeopleListInFolder(String folderName)
    {
        String containerPath = folderName.equals(getProjectName()) ? getProjectName() : getProjectName() + "/" + folderName;
        _listHelper.createList(containerPath, LIST_NAME,
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name"),
                new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "Age"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"));
    }

    private void createPeopleListInTab(String tabLabel)
    {
        _listHelper.createListFromTab(tabLabel, LIST_NAME,
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name"),
                new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "Age"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"));
    }

    @LogMethod
    private void doTestQueries()
    {
        log("Testing queries in modules...");

        //go to query module portal
        clickProject(getProjectName());
        goToModule("Query");
        viewQueryData("lists", "TestQuery");

        assertTextPresent("Adam", "Dave", "Josh");
        assertTextNotPresent("Britt");
    }

    @LogMethod
    private void doTestQueryViews()
    {
        log("Testing module-based custom query views...");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(LIST_NAME));

        DataRegionTable table = new DataRegionTable("query", getDriver());
        table.goToView("Crazy People");
        assertTextPresent("Adam", "Dave", "Josh");
        assertTextNotPresent("Britt");

        //custom view has a sort by age descending
        assertTextBefore("Adam", "Josh");

        //Issue 11307: Inconsistencies saving session view over file-based view
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("CreatedBy");
        _customizeViewsHelper.applyCustomView();
        // Wait for the save button to appear
        waitForElement(Locator.xpath("//div[contains(@class, 'lk-region-context-bar')]//span[contains(@class, 'unsavedview-save')]"));
                    //todo: move this to dataregion or customizeViewsHelper, perhaps
        _customizeViewsHelper.saveUnsavedViewGridClosed(null);
        waitForText("Crazy People Copy");
    }

    @LogMethod
    private void doTestReports()
    {
        RReportHelper _rReportHelper = new RReportHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);

        _rReportHelper.ensureRConfig();

        log("Testing module-based JS reports...");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(LIST_NAME));
        DataRegionTable table = new DataRegionTable("query", getDriver());
        table.goToReport("Want To Be Cool");
        waitForText(WAIT_FOR_JAVASCRIPT, "Less cool than expected. Loaded dependent scripts.");

        clickProject(getProjectName());
        portalHelper.addWebPart("Report");
        setFormElement(Locator.name("title"), "Report Tester Part");
        selectOptionByValue(Locator.name("reportId"), "module:simpletest/reports/schemas/lists/People/Less Cool JS Report.js");
        clickButton("Submit");
        waitForText(WAIT_FOR_JAVASCRIPT, "Less cool than expected. Loaded dependent scripts.");

        String WikiName = "JS Report Wiki";
        portalHelper.addWebPart("Wiki");
        wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), WikiName);
        setFormElement(Locator.name("title"), WikiName);
        wikiHelper.setWikiBody("placeholder text");
        wikiHelper.saveWikiPage();
        wikiHelper.setSourceFromFile("jsReportTest.html", WikiName);
        waitForText(WAIT_FOR_JAVASCRIPT, "Console output");
        waitForText("Less cool than expected. Loaded dependent scripts.", 2, WAIT_FOR_JAVASCRIPT);
        assertTextPresent("JS Module Report", "Hello, Bob!");

        log("Testing module-based reports...");
        clickAndWait(Locator.linkWithText(LIST_NAME));
        table.goToReport( SUPER_COOL_R_REPORT);
        waitForText(WAIT_FOR_JAVASCRIPT, "Console output");
        assertTextNotPresent("Error executing command");
        log("Verify comment based output file substitution syntax");
        assertElementPresent(Locator.xpath("//img[starts-with(@id,'resultImage')]"));
        click(Locator.tagWithAttributeContaining("img", "src", "minus.gif"));
        log("Verify comment based output regex substitution syntax");
        assertElementPresent(Locator.tagWithText("a", "Text output file (click to download)"));
        log("Verify comment based input file substitution syntax");
        assertTextPresent("Adam", "Britt", "Dave");

        doTestReportThumbnails();
        doTestReportIcon();
        doTestReportCreatedDate();
    }

    @LogMethod
    private void doTestReportThumbnails()
    {
        goToProjectHome();
        log("Verify custom module report thumbnail images");
        verifyReportThumbnail(KNITR_PEOPLE);
        verifyReportThumbnail(SUPER_COOL_R_REPORT);
        verifyReportThumbnail(WANT_TO_BE_COOL);
    }

    @LogMethod
    private void doTestReportIcon()
    {

        log("Verify custom module report icon image");
        setFormElement(Locator.xpath("//table[contains(@class, 'dataset-search')]//input"), KNITR_PEOPLE);
        waitForElementToDisappear(Locator.tag("tr").withClass("x4-grid-row").containing(WANT_TO_BE_COOL).notHidden());

        File expectedIconFile = TestFileUtils.getSampleData(THUMBNAIL_FOLDER + KNITR_PEOPLE + ICON_FILENAME);
        String expectedIcon = TestFileUtils.getFileContents(expectedIconFile);

        String iconStyle = waitForElement(Locator.tag("img").withClass("dataview-icon").withoutClass("x4-tree-icon-parent").notHidden()).getAttribute("style");
        assertTrue("Module report icon style is not as expected", iconStyle.indexOf("background-image") == 0);
        String iconSrc = iconStyle.replace("background-image:url(\"", "").replace("background-image: url(\"", "").replace("\");", "");

        String portPortion = 80 == WebTestHelper.getWebPort() ? "" : ":" + WebTestHelper.getWebPort();
        String protocol = WebTestHelper.getTargetServer() + portPortion;
        String iconData = WebTestHelper.getHttpResponse(protocol + iconSrc).getResponseBody();

        int lengthToCompare = 3000;
        int diff = StringUtils.getLevenshteinDistance(expectedIcon.substring(0, lengthToCompare), iconData.substring(0, lengthToCompare));
        assertTrue("Module report icon is not as expected, diff is " + diff, expectedIcon.equals(iconData) ||
                diff  <= lengthToCompare * 0.03); // Might be slightly different due to indentations, etc
    }

    @LogMethod
    private void doTestReportCreatedDate()
    {
        log("Verify module report \"created\" date");
        click(Locator.tag("span").withClass("fa-list-ul").notHidden());
        waitForText("2015-08-01");
    }

    @LogMethod
    private void verifyReportThumbnail(String reportTitle)
    {
        File expectedThumbnailFile = TestFileUtils.getSampleData(THUMBNAIL_FOLDER + reportTitle + THUMBNAIL_FILENAME);
        String expectedThumbnail = TestFileUtils.getFileContents(expectedThumbnailFile);

        waitForElement(Locator.xpath("//a[text()='"+reportTitle+"']"));
        mouseOver(Locator.xpath("//a[text()='"+reportTitle+"']"));
        Locator.XPathLocator thumbnail = Locator.xpath("//div[@class='thumbnail']/img").notHidden();
        waitForElement(thumbnail);
        String thumbnailData;
        thumbnailData = WebTestHelper.getHttpResponse(getAttribute(thumbnail, "src")).getResponseBody();

        int lengthToCompare = 5000;
        int diff = StringUtils.getLevenshteinDistance(expectedThumbnail.substring(0, lengthToCompare), thumbnailData.substring(0, lengthToCompare));
        assertTrue("Module report thumbnail is not as expected, diff is " + diff, expectedThumbnail.equals(thumbnailData) ||
                diff  <= lengthToCompare * 0.03); // Might be slightly different due to indentations, etc
    }

    @LogMethod
    private void doTestImportTemplates() throws Exception
    {
        log("Testing import templates...");

        //go to query module portal
        clickProject(getProjectName());
        goToModule("Query");
        viewQueryData(VEHICLE_SCHEMA, "Vehicles");
        DataRegionTable table = new DataRegionTable("query", getDriver());
        table.clickImportBulkData();
        assertTrue("Import message not present", isTextPresent("Please read this before you import data"));

        Locator l = Locator.xpath("//select[@id='importTemplate']//option");
        assertTrue("Wrong number of templates found", getElementCount(l) == 2);
    }

    @LogMethod
    private void doTestContainerColumns() throws Exception
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        log("** Testing container columns");
        SelectRowsCommand selectCmd = new SelectRowsCommand(CORE_SCHEMA, "Containers");
        selectCmd.setMaxRows(-1);
        List<String> columns = new ArrayList<>();
        columns.add("*");
        selectCmd.setColumns(columns);
        selectCmd.setRequiredVersion(9.1);
        SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());
        assertEquals("Expected to select 1 rows.", 1, selectResp.getRowCount().intValue());

        Map<String,Object> row = selectResp.getRows().get(0);
        String entityId = (String)((JSONObject)row.get("EntityId")).get("value");
        assertEquals("Expected core.containers path column to return the string: /" + getProjectName(), "/" + getProjectName(), ((JSONObject)row.get("Path")).get("value"));

        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setColumns(columns);
        selectCmd.setRequiredVersion(9.1);
        selectResp = selectCmd.execute(cn, getProjectName());
        JSONObject vehicleRow = (JSONObject)(selectResp.getRows().get(0)).get("container");

        assertEquals("Expected vehicles.container to return the value: " + entityId, entityId, vehicleRow.get("value"));
        assertEquals("Expected vehicles.container to return the displayValue: " + getProjectName(), getProjectName(), vehicleRow.get("displayValue"));

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
        assertEquals("Expected to return " + expectedCols.length + " columns, based on the saved view", expectedCols.length, selectResp.getColumnModel().size());
        for (String col : expectedCols)
        {
            assertNotNull("Details view does not contain column: " + col, selectResp.getColumnModel(col));
        }

        log("** using selectRows to test insert view");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setViewName("~~INSERT~~");
        selectCmd.setMaxRows(-1);
        selectResp = selectCmd.execute(cn, getProjectName());

        expectedCols = new String[]{"RowId", "ModelYear", "Milage", "ModelId/ManufacturerId/Name"};
        assertEquals("Expected to return " + expectedCols.length + " columns, based on the saved view", expectedCols.length, selectResp.getColumnModel().size());

        for (String col : expectedCols)
        {
            assertNotNull("Insert view does not contain column: " + col, selectResp.getColumnModel(col));
        }

        log("** using selectRows to test update view");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setViewName("~~UPDATE~~");
        selectCmd.setMaxRows(-1);
        selectResp = selectCmd.execute(cn, getProjectName());

        expectedCols = new String[]{"RowId", "ModelYear", "Milage", "LastService", "ModelId/ManufacturerId/Name"};
        assertEquals("Expected to return " + expectedCols.length + " columns, based on the saved view", expectedCols.length, selectResp.getColumnModel().size());

        for (String col : expectedCols)
        {
            assertNotNull("Update view does not contain column: " + col, selectResp.getColumnModel(col));
        }
    }

    @LogMethod
    private void doTestParameterizedQueries()
    {
        WikiHelper wikiHelper = new WikiHelper(this);

        log("Create embedded QWP to test parameterized query.");
        clickFolder(FOLDER_NAME);
        goToModule("Wiki");
        wikiHelper.createNewWikiPage();
        setFormElement(Locator.id("wiki-input-name"), "Parameterized QWP");
        wikiHelper.setWikiBody(TestFileUtils.getFileContents("/server/test/modules/simpletest/resources/views/parameterizedQWP.html"));
        clickButton("Save & Close");

        log("Check that parameterized query doesn't cause page load.");
        SiteNavBar bar = new SiteNavBar(getDriver());
        WebElement searchInput = bar.expandSearchBar();
        setFormElement(searchInput, MODULE_NAME);
        waitForElement(Locator.xpath("//input[contains(@name, 'param.STARTS_WITH')]"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[contains(@name, 'param.STARTS_WITH')]"), "P");
        clickButton("Submit", 0);
        waitForText("Manufacturer");
        assertEquals("Unexpected page refresh.", MODULE_NAME, getFormElement(searchInput));
        waitForText(WAIT_FOR_JAVASCRIPT, "Pinto");
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
        assertEquals("Expected to select 1 rows.", 1, selectResp.getRowCount().intValue());
        assertEquals("Expected to return 3 columns, based on the saved view", 3, selectResp.getColumnModel().size());

        log("** Select using selectRows and a view with a sort in it");
        selectCmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Vehicles");
        selectCmd.setMaxRows(-1);
        selectCmd.setViewName("SortOnModelYear");
        selectResp = selectCmd.execute(cn, getProjectName());
        assertEquals("Expected first row to be 2001.", 2001, selectResp.getRows().get(0).get("ModelYear"));
        assertEquals("Expected first row to be 2000.", 2000, selectResp.getRows().get(1).get("ModelYear"));
        assertTrue("Expected the column 'ModelId/ManufacturerId/Name' to be included based on the default view", selectResp.getColumnModel("ModelId/ManufacturerId/Name") != null);
        assertEquals("Expected to return 6 columns, based on the default view", 6, selectResp.getColumnModel().size());

    }

    private final String subfolderPath = "/project/" + getProjectName() + "/" + FOLDER_NAME +"/begin.view?";
    @LogMethod
    @Test
    public void testModuleProperties() throws Exception
    {
        String prop1 = "TestProp1";
        String prop1Value = "Prop1Value";
        String prop2 = "TestProp2";

        beginAt(subfolderPath);
        portalHelper.addWebPart("Simple Module Web Part");
        waitForText("This is a web part view in the simple test module");

        assertEquals("Module context not set properly", "DefaultValue", executeScript("return LABKEY.getModuleContext('simpletest')." + prop2));

        Map<String, List<String[]>> props = new HashMap<>();
        List<ModulePropertyValue> propList = new ArrayList<>();
        propList.add(new ModulePropertyValue(MODULE_NAME, "/", prop1, prop1Value));
        propList.add(new ModulePropertyValue(MODULE_NAME, "/" + getProjectName() + "/" + FOLDER_NAME, prop2 , "FolderValue"));
        propList.add(new ModulePropertyValue(MODULE_NAME, "/", "TestCheckbox", "true", ModulePropertyValue.InputType.checkbox));
        propList.add(new ModulePropertyValue(MODULE_NAME, "/", "TestSelect", "value1", ModulePropertyValue.InputType.select));
        propList.add(new ModulePropertyValue(MODULE_NAME, "/", "TestCombo", "comboValue1", ModulePropertyValue.InputType.combo));

        validateInputTypes(propList);
        setModuleProperties(propList);
        // Validate values in folder in which they were set
        validateValues(propList);
        // Now check value at parent level
        goToProjectHome();
        assertEquals("Module context not set properly", "DefaultValue", executeScript("return LABKEY.getModuleContext('simpletest')." + prop2));
    }

    private void validateInputTypes(List<ModulePropertyValue> propList)
    {
        List<String> errors = new ArrayList<>();
        beginAt(subfolderPath);
        goToModuleProperties();
        propList.forEach(property -> {
            Ext4FieldRef ref = getModulePropertyFieldRef(property);
            String error = "Module property " + property.getPropertyName();
            if (null == ref)
                errors.add(error + " was not present");
            else if (!property.getInputType().getXtype().equals(ref.getEval("xtype")))
                errors.add(error + " was wrong xtype. Expected " + property.getInputType().getXtype() + " but was " + ref.getEval("xtype"));
            else if (!property.getInputType().isValid(ref))
                errors.add(error + " was invalid for its input type " + property.getInputType().toString());
        });

        if (!errors.isEmpty())
        {
            StringJoiner sj = new StringJoiner("\n", "\n", "");
            errors.forEach(sj::add);
            fail(sj.toString());
        }
    }

    private void validateValues(List<ModulePropertyValue> propList)
    {
        List<String> errors = new ArrayList<>();
        beginAt(subfolderPath);
        propList.forEach(property ->
        {
            Object actualValue = executeScript("return LABKEY.getModuleContext('simpletest')." + property.getPropertyName());
            if (!property.getValue().equals(actualValue))
                errors.add(property.getPropertyName() + " has incorrect value. Expected " + property.getValue() + " but was " + actualValue);
        });
        if (!errors.isEmpty())
        {
            StringJoiner sj = new StringJoiner("\n", "\n", "");
            errors.forEach(sj::add);
            fail(sj.toString());
        }
    }

    private static final String DATASET_NAME = "Data Name";
    private static final String DATASET_LABEL = "Data Label";
    private static final String DATASET_FIELDS = "Property\nFirst\nLast";

    @LogMethod
    private void doTestDatasetsAndFileBasedQueries()
    {
        navigateToFolder(getProjectName(), FOLDER_NAME);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Study Overview");
        waitForText("Create Study");
        clickAndWait(Locator.linkWithText("Create Study"));
        clickAndWait(Locator.linkWithText("Create Study"));
        EditDatasetDefinitionPage editDatasetPage = _studyHelper.goToManageDatasets()
                .clickCreateNewDataset()
                .setName(DATASET_NAME)
                .submit()
                .setDatasetLabel(DATASET_LABEL);
        clickButton("Import Fields", "Paste tab-delimited");
        setFormElement(Locator.name("tsv"), DATASET_FIELDS);
        clickButton("Import", 0);
        waitForText("First");
        editDatasetPage
                .save()
                .clickViewData();
        assertTextPresent("My Custom View", "Hello Dataset", "Visit");
        assertTextNotPresent("Participant Identifier");
    }

    @LogMethod
    private void doTestRestrictedModule()
    {
        log("Create folder with restricted");
        clickProject(getProjectName());
        _containerHelper.createSubfolder(getProjectName(), RESTRICTED_FOLDER_NAME, RESTRICTED_FOLDER_TYPE);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Restricted Module Web Part");
        assertTextPresent("This is a web part view in the restricted module.");
        assertModuleEnabledByDefault(RESTRICTED_MODULE_NAME);
        createPeopleListInFolder(RESTRICTED_FOLDER_NAME);

        log("folder admin without restricted permission can still see existing restricted folder, web parts");
        navigateToFolder(getProjectName(), RESTRICTED_FOLDER_NAME);
        impersonateRole("Reader");
        assertTextPresent("This is a web part view in the restricted module.");     // Can still see web part
        stopImpersonating();
        clickProject(getProjectName());
        navigateToFolder(getProjectName(), RESTRICTED_FOLDER_NAME);
        impersonateRole("Folder Administrator");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        // Shouldn't see folder type, module name
        assertElementNotPresent(Locator.xpath("//input[@type='checkbox' and @title='" + RESTRICTED_MODULE_NAME + "']"));
        assertElementNotPresent(Locator.xpath("//input[@type='radio' and @value='" + RESTRICTED_FOLDER_TYPE + "']"));

        log("folder admin without restricted permission cannot import restricted folder");
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), NEW_FOLDER_NAME, "Collaboration", null);
        createPeopleListInFolder(NEW_FOLDER_NAME);
        navigateToFolder(getProjectName(), NEW_FOLDER_NAME);
        importFolderFromZip(new File(TestFileUtils.getLabKeyRoot(), RESTRICTED_FOLDER_IMPORT_NAME), false, 1, true);
        clickAndWait(Locator.linkWithText("ERROR"));
        assertTextPresent(
                "Folder type 'Folder With Restricted Module' not set because it requires a restricted module for which you do not have permission.",
                "Modules not enabled because module 'restrictedModule' is restricted and you do not have the necessary permission to enable it."
        );
        stopImpersonating();
        checkExpectedErrors(2);
    }

    protected void assertModuleDeployed(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is deployed");
        goToAdminConsole().goToModuleInformationSection();
        assertTextPresent(moduleName);
    }

    protected void assertModuleEnabledByDefault(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is enabled");
        goToFolderManagement().goToFolderTypeTab()
                .assertModuleEnabled(moduleName);
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

    @Override
    public void validateQueries(boolean validateSubfolders)
    {
        super.validateQueries(false); // Container tabs fail query validation
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList(MODULE_NAME);
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}

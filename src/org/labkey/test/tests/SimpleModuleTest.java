/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.hc.core5.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Command;
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
import org.labkey.test.Locators;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.core.admin.LookAndFeelSettingsPage;
import org.labkey.test.pages.study.DatasetDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

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
import static org.labkey.test.TestFileUtils.getLabKeyRoot;

/**
* Tests the simple module and file-based resources introduced in version 9.1
*/
@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 15)
public class SimpleModuleTest extends BaseWebDriverTest
{
    public static final String FOLDER_TYPE = "My XML-defined Folder Type"; // Folder type defined in customFolder.foldertype.xml
    public static final String TABBED_FOLDER_TYPE = "My XML-defined Tabbed Folder Type";
    public static final String MODULE_NAME = "simpletest";
    public static final String FOLDER_NAME = "subfolder";
    public static final String FOLDER_NAME_2 = "subfolder2";
    public static final String FOLDER_NAME_3 = "subfolder3";
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
    public static final File RESTRICTED_FOLDER_IMPORT_NAME =
            TestFileUtils.getSampleData("SimpleAndRestrictedModule/FolderWithRestricted.folder.zip");

    private static final String THUMBNAIL_FOLDER = "thumbnails/";
    private static final String THUMBNAIL_FILENAME = "/Thumbnail.png";
    private static final String ICON_FILENAME = "/SmallThumbnail.png";

    private static final String KNITR_PEOPLE = "Knitr People";
    private static final String SUPER_COOL_R_REPORT = "Super Cool R Report";
    private static final String WANT_TO_BE_COOL = "Want To Be Cool";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final File DEFAULT_IMAGE =  TestFileUtils.getSampleData("thumbnails/default.jpg");
    private static final File PRIUS_THUMBNAIL =  TestFileUtils.getSampleData("thumbnails/prius.jpg");
    private static final File PRIUS_POPUP =  TestFileUtils.getSampleData("thumbnails/priusPopup.jpg");
    private static final File CAMRY_THUMBNAIL =  TestFileUtils.getSampleData("thumbnails/camry.jpg");
    private static final File FOCUS_POPUP =  TestFileUtils.getSampleData("thumbnails/focusPopup.jpg");

    private static final String XML_METADATA = "<tables xmlns=\"http://labkey.org/data/xml\"> \n" +
            "  <table tableName=\"Models\" tableDbType=\"TABLE\">\n" +
            "    <columns>\n" +
            "      <column columnName=\"Image\">\n" +
            "        <datatype>varchar</datatype>\n" +
            "        <displayColumnFactory>\n" +
            "          <className>org.labkey.api.data.URLDisplayColumn$Factory</className>\n" +
            "          <properties>\n" +
            "            <property name=\"thumbnailImageUrl\">/_webdav/SimpleModuleTest%20Project/%40files/${thumbnailImage}</property>\n" +
            "            <property name=\"popupImageUrl\">/_webdav/SimpleModuleTest%20Project/%40files/${popupImage}</property>\n" +
            "            <property name=\"popupImageWidth\">150px</property>\n" +
            "          </properties>\n" +
            "        </displayColumnFactory>\n" +
            "        <url>/_webdav/SimpleModuleTest%20Project/%40files/${Image}</url>\n" +
            "      </column>\n" +
            "    </columns>\n" +
            "  </table>\n" +
            "</tables>\n";

    private static final String XML_METADATA_NO_POPUP = "<tables xmlns=\"http://labkey.org/data/xml\"> \n" +
            "  <table tableName=\"Models\" tableDbType=\"TABLE\">\n" +
            "    <columns>\n" +
            "      <column columnName=\"Image\">\n" +
            "        <datatype>varchar</datatype>\n" +
            "        <displayColumnFactory>\n" +
            "          <className>org.labkey.api.data.URLDisplayColumn$Factory</className>\n" +
            "          <properties>\n" +
            "            <property name=\"thumbnailImageUrl\">/_webdav/SimpleModuleTest%20Project/%40files/${thumbnailImage}</property>\n" +
            "            <property name=\"popupImageUrl\"></property>\n" +
            "          </properties>\n" +
            "        </displayColumnFactory>\n" +
            "        <url>/_webdav/SimpleModuleTest%20Project/%40files/${Image}</url>\n" +
            "      </column>\n" +
            "    </columns>\n" +
            "  </table>\n" +
            "</tables>\n";

    private static final String XML_METADATA_CUSTOM_QUERY = "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
            "  <table tableName=\"SelectOnColors\" tableDbType=\"NOT_IN_DB\">\n" +
            "    <pkColumnName>Name</pkColumnName>\n" +
            "     <insertUrl>/query/insertQueryRow.view?schemaName=vehicle&amp;queryName=Colors</insertUrl> \n" +
            "     <updateUrl>/query/updateQueryRow.view?schemaName=vehicle&amp;queryName=Colors&amp;Name=${Name}</updateUrl> \n" +
            "     <importUrl>/query/import.view?schemaName=vehicle&amp;queryName=Colors</importUrl> \n" +
            "     <deleteUrl>/query/deleteQueryRows.view?schemaName=vehicle&amp;queryName=Colors</deleteUrl> \n" +
            "    <columns>\n" +
            "      \t<column columnName=\"Name\">\n" +
            "        \t<isKeyField>true</isKeyField>\n" +
            "        </column>\n" +
            "    </columns>\n" +
            "  </table>\n" +
            "</tables>";

    private final PortalHelper portalHelper = new PortalHelper(this);

    @Override
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
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME_2);

        goToProjectHome();
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME_3);

        goToProjectHome();
        portalHelper.addWebPart("Data Views");
        for (int i = 0; i < 5; i ++)
            portalHelper.moveWebPart("Data Views", PortalHelper.Direction.UP);

        goToProjectSettings();
        setFormElement(Locator.name("defaultDateFormat"), DATE_FORMAT);
        clickAndWait(Locator.lkButton("Save"));

        // images for thumbnails
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(DEFAULT_IMAGE);
        _fileBrowserHelper.uploadFile(PRIUS_THUMBNAIL);
        _fileBrowserHelper.uploadFile(PRIUS_POPUP);
        _fileBrowserHelper.uploadFile(CAMRY_THUMBNAIL);
        _fileBrowserHelper.uploadFile(FOCUS_POPUP);

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
        doTestColumnValidators();
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
        doTestRowLevelContainerPath();
        doTestCustomLogin();
        doTestFkLookupFilter();
        doTestMetadataOverrideForCustomQuery();
    }

    @LogMethod
    private void doTestMetadataOverrideForCustomQuery()
    {
        goToProjectHome();
        String subFolder = "Metadata override for custom query";
        String customQueryName = "SelectOnColors";
        _containerHelper.createSubfolder(getProjectName(), subFolder);

        goToSchemaBrowser();
        createNewQuery(VEHICLE_SCHEMA);
        setFormElement(Locator.name("ff_newQueryName"), customQueryName);
        selectOptionByText(Locator.name("ff_baseTableName"), "Colors");
        clickButton("Create and Edit Source", 0);

        clickButton("Save & Finish");
        assertElementNotPresent(Locator.tagWithAttribute("a", "data-original-title", "Insert data"));
        assertElementNotPresent(Locator.tagWithAttribute("a", "data-original-title", "Delete"));

        goToSchemaBrowser();
        selectQuery(VEHICLE_SCHEMA, customQueryName);
        waitForText("edit metadata");
        clickAndWait(Locator.linkWithText("edit metadata"));
        // wait for the domain editor to appear:
        clickButton("Edit Source", defaultWaitForPage);
        _ext4Helper.clickExt4Tab("XML Metadata");
        setCodeEditorValue("metadataText", XML_METADATA_CUSTOM_QUERY);
        clickButton("Save & Finish");

        assertElementPresent(Locator.tagWithAttribute("a", "data-original-title", "Insert data"));
        assertElementPresent(Locator.tagWithAttribute("a", "data-original-title", "Delete"));
        DataRegionTable customQuery = new DataRegionTable("query", getDriver());
        customQuery.clickInsertNewRow();

        setFormElement(Locator.name("quf_Name"), "Teal");
        setFormElement(Locator.name("quf_Hex"), "#008080");
        setFormElement(Locator.name("quf_TriggerScriptProperty"), "#008080");
        clickButton("Submit");

        assertEquals("After insert : Mismatch in row between custom query and hard table", getRowCount(VEHICLE_SCHEMA, customQueryName), getRowCount(VEHICLE_SCHEMA, "Colors"));
        assertEquals("Comparing colors in both table", getValuesOfColumn(customQueryName, "Name"), getValuesOfColumn("Colors", "Name"));

        customQuery = new DataRegionTable("query", getDriver());
        customQuery.clickEditRow(customQuery.getRowIndex("Name", "Teal!"));
        setFormElement(Locator.name("quf_Name"), "Teal!!!!");
        clickButton("Submit");

        assertEquals("Comparing colors in both table", getValuesOfColumn(customQueryName, "Name"), getValuesOfColumn("Colors", "Name"));

        customQuery = new DataRegionTable("query", getDriver());
        customQuery.checkCheckbox(customQuery.getRowIndex("Name", "Silver!"));
        customQuery.deleteSelectedRows();

        assertEquals("After Delete : Mismatch in row between custom query and hard table", getRowCount(VEHICLE_SCHEMA, customQueryName), getRowCount(VEHICLE_SCHEMA, "Colors"));

    }

    private int getRowCount(String schemaName, String tableName)
    {
        pushLocation();
        goToSchemaBrowser();
        viewQueryData(schemaName, tableName);
        DataRegionTable customQuery = new DataRegionTable("query", getDriver());
        int returnValue = customQuery.getDataRowCount();
        popLocation();
        return returnValue;
    }

    private List<String> getValuesOfColumn(String tableName, String colunmName)
    {
        pushLocation();
        goToSchemaBrowser();
        viewQueryData(VEHICLE_SCHEMA, tableName);
        DataRegionTable customQuery = new DataRegionTable("query", getDriver());
        List<String> retVal = customQuery.getColumnDataAsText(colunmName);
        popLocation();
        return retVal;

    }

    @LogMethod
    private void doTestColumnValidators() throws Exception
    {
        //first create a maufacturer:
        InsertRowsCommand insertCmdM = new InsertRowsCommand("vehicle", "Manufacturers");
        Map<String, Object> rowMapM = new HashMap<>();
        rowMapM.put("Name", "TestManufacturer");
        insertCmdM.addRow(rowMapM);
        SaveRowsResponse respM = insertCmdM.execute(createDefaultConnection(), getProjectName());
        Object manufacturerId = respM.getRows().get(0).get("RowId");

        //This table has one validator defined in the schema XML and one in query XML.  First do insert that should fail schema validator:
        InsertRowsCommand insertCmd = new InsertRowsCommand("vehicle", "Models");
        Map<String, Object> rowMap = new HashMap<>();
        rowMap.put("Name", "ShouldFail");
        rowMap.put("ManufacturerId", manufacturerId);
        rowMap.put("InitialReleaseYear", 5);
        insertCmd.addRow(rowMap);
        submitAndTestExpectedFailure(insertCmd, "Value '5' for field 'InitialReleaseYear' is invalid. Failed Schema Layer RegEx");

        //Now fail query-layer validator
        insertCmd = new InsertRowsCommand("vehicle", "Models");
        rowMap = new HashMap<>();
        rowMap.put("Name", "123456789012345678901");  //21 characters
        rowMap.put("ManufacturerId", manufacturerId);
        rowMap.put("InitialReleaseYear", 2000);
        insertCmd.addRow(rowMap);
        submitAndTestExpectedFailure(insertCmd, "Value '123456789012345678901' for field 'Name' is invalid. Failed Query Layer RegEx");

        //now succeed:
        insertCmd = new InsertRowsCommand("vehicle", "Models");
        rowMap = new HashMap<>();
        rowMap.put("Name", "Model1");
        rowMap.put("ManufacturerId", manufacturerId);
        rowMap.put("InitialReleaseYear", 2000);
        insertCmd.addRow(rowMap);
        SaveRowsResponse resp = insertCmd.execute(createDefaultConnection(), getProjectName());
        Object rowId = resp.getRows().get(0).get("RowId");

        //now try to update it:
        UpdateRowsCommand updateCmd = new UpdateRowsCommand("vehicle", "Models");
        rowMap = new HashMap<>();
        rowMap.put("RowId", rowId);
        rowMap.put("Name", "123456789012345678901");  //back to failure
        updateCmd.addRow(rowMap);
        submitAndTestExpectedFailure(updateCmd, "Value '123456789012345678901' for field 'Name' is invalid. Failed Query Layer RegEx");

        //also fail
        updateCmd = new UpdateRowsCommand("vehicle", "Models");
        rowMap = new HashMap<>();
        rowMap.put("RowId", rowId);
        rowMap.put("InitialReleaseYear", 5);  //back to failure
        updateCmd.addRow(rowMap);
        submitAndTestExpectedFailure(updateCmd, "Value '5' for field 'InitialReleaseYear' is invalid. Failed Schema Layer RegEx");

        //now succeed:
        updateCmd = new UpdateRowsCommand("vehicle", "Models");
        rowMap = new HashMap<>();
        rowMap.put("RowId", rowId);
        rowMap.put("Name", "Model2");
        rowMap.put("InitialReleaseYear", 2000);
        updateCmd.addRow(rowMap);
        updateCmd.execute(createDefaultConnection(), getProjectName());

        //clean:
        DeleteRowsCommand deleteCmd = new DeleteRowsCommand("vehicle", "Models");
        rowMap = new HashMap<>();
        rowMap.put("RowId", rowId);
        deleteCmd.addRow(rowMap);
        deleteCmd.execute(createDefaultConnection(), getProjectName());

        deleteCmd = new DeleteRowsCommand("vehicle", "Manufacturers");
        rowMap = new HashMap<>();
        rowMap.put("RowId", manufacturerId);
        deleteCmd.addRow(rowMap);
        deleteCmd.execute(createDefaultConnection(), getProjectName());
    }

    private void submitAndTestExpectedFailure(Command cmd, String expectedError) throws Exception
    {
        try
        {
            cmd.execute(createDefaultConnection(), getProjectName());

            throw new Exception("This should have failed");
        }
        catch (CommandException e)
        {
            Map<String, Object> responseJson = e.getProperties();
            if (!responseJson.containsKey("errors"))
            {
                throw new Exception("Response lacks errors");
            }

            List<Map<String, Object>> errors = (List<Map<String, Object>>) responseJson.get("errors");
            String msg = errors.get(0).get("exception").toString();
            assertEquals("Incorrect exception", expectedError, msg);
        }
    }

    @LogMethod
    private void doTestCustomFolder()
    {
        clickProject(getProjectName());
        PortalHelper portalHelper = new PortalHelper(this);

        assertTextPresentInThisOrder("A customized web part", "Data Pipeline", "Experiment Runs", "Sample Type", "Assay List");
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

        Connection cn = WebTestHelper.getRemoteApiConnection();

        log("** Inserting new Manufacturers via java client api...");
        InsertRowsCommand insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Manufacturers");
        insertCmd.getRows().addAll(Arrays.asList(
                Maps.of("Name", "Ford"),
                Maps.of("Name", "Toyota"),
                Maps.of("Name", "Honda")
        ));
        SaveRowsResponse insertResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Expected to insert 3 rows.", 3, insertResp.getRowsAffected().intValue());

        Integer fordId = null;
        Integer toyotaId = null;
        Integer hondaId = null;

        for (Map<String, Object> row : insertResp.getRows())
        {
            Integer rowId = (Integer) row.get("RowId");
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
                        "Image", DEFAULT_IMAGE.getName(),
                        "ThumbnailImage", PRIUS_THUMBNAIL.getName(),
                        "PopupImage", PRIUS_POPUP.getName()),
                Maps.of("ManufacturerId", toyotaId,
                        "Name", "Camry",
                        "Image", DEFAULT_IMAGE.getName(),
                        "ThumbnailImage", CAMRY_THUMBNAIL.getName(),
                        "InitialReleaseYear", 1982),
                Maps.of("ManufacturerId", fordId,
                        "Name", "Focus",
                        "Image", DEFAULT_IMAGE.getName(),
                        "PopupImage", FOCUS_POPUP.getName()),
                Maps.of("ManufacturerId", fordId,
                        "Name", "F150"),
                Maps.of("ManufacturerId", fordId,
                        "Name", "Pinto")
        ));
        insertResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Expected to insert 5 rows.", 5, insertResp.getRowsAffected().intValue());

        // test thumbnail images
        log("testing custom thumbnail and popup images");
        goToSchemaBrowser();
        selectQuery("vehicle", "Models");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));

        // default thumbnail
        validateThumbnails(DEFAULT_IMAGE.getName(), "50px", true, FOCUS_POPUP.getName(), null);
        // both thumbnail and popup
        validateThumbnails(PRIUS_THUMBNAIL.getName(), "50px", true, PRIUS_POPUP.getName(), null);
        // should have default popup
        validateThumbnails(CAMRY_THUMBNAIL.getName(), "50px", true, DEFAULT_IMAGE.getName(), null);

        // override the metadata to set the thumbnail and popup widths
        setMetadataXML("vehicle", "Models", XML_METADATA);

        // revalidate with the updated sizes
        validateThumbnails(DEFAULT_IMAGE.getName(), null, true, FOCUS_POPUP.getName(), "150px");
        validateThumbnails(PRIUS_THUMBNAIL.getName(), null, true, PRIUS_POPUP.getName(), "150px");
        validateThumbnails(CAMRY_THUMBNAIL.getName(), null, true, DEFAULT_IMAGE.getName(), "150px");

        // override the metadata to show thumbnails but no popups
        setMetadataXML("vehicle", "Models", XML_METADATA_NO_POPUP);

        // revalidate with the updated sizes
        validateThumbnails(DEFAULT_IMAGE.getName(), null, false, FOCUS_POPUP.getName(), "150px");
        validateThumbnails(PRIUS_THUMBNAIL.getName(), null, false, PRIUS_POPUP.getName(), "150px");
        validateThumbnails(CAMRY_THUMBNAIL.getName(), null, false, DEFAULT_IMAGE.getName(), "150px");

        log("finished testing custom thumbnail and popup images");

        Integer priusId = null;
        Integer f150Id = null;

        for (Map<String, Object> row : insertResp.getRows())
        {
            Integer rowId = (Integer) row.get("RowId");
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
                Maps.of("Name", "Blue", "Hex", "#0000FF"),
                Maps.of("Name", "Black", "Hex", "#000000"),
                Maps.of("Name", "White", "Hex", "#FFFFFF"),
                Maps.of("Name", "Purple", "Hex", "#4A235A"),
                Maps.of("Name", "Brown", "Hex", "#6E2C00"),
                Maps.of("Name", "Gold", "Hex", "#FAD7A0"),
                Maps.of("Name", "Silver", "Hex", "#E5E8E8")
        ));
        insertResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Expected to insert 9 rows.", 9, insertResp.getRowsAffected().intValue());

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

        Integer[] vehicleIds = new Integer[2];
        vehicleIds[0] = (Integer) (insertResp.getRows().get(0).get("RowId"));
        vehicleIds[1] = (Integer) (insertResp.getRows().get(1).get("RowId"));

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

    private void setMetadataXML(String schemaName, String queryName, String metadata)
    {
        goToSchemaBrowser();
        selectQuery(schemaName, queryName);
        waitForText("edit metadata");
        clickAndWait(Locator.linkWithText("edit metadata"));
        // wait for the domain editor to appear:
        clickButton("Edit Source", defaultWaitForPage);
        _ext4Helper.clickExt4Tab("XML Metadata");
        setCodeEditorValue("metadataText", metadata);
        clickButton("Save & Finish");
    }

    private void validateThumbnails(String thumbnailImage, @Nullable String thumbnailWidth, boolean hasPopup, String popupImage, @Nullable String popupWidth)
    {
        Locator thumbnail = Locator.tag("img").withAttributeContaining("src", thumbnailImage).
                withAttributeContaining("style", thumbnailWidth != null ? "width:" + thumbnailWidth : "max-width:32px");
        assertElementPresent(thumbnail);

        log("Hover over the thumbnail and make sure the pop-up is as expected.");
        fireEvent(thumbnail, SeleniumEvent.mouseover);

        if (hasPopup)
        {
            Locator popup = Locator.tag("div").withAttribute("id", "helpDiv").descendant("img").withAttributeContaining("src", popupImage).
                    withAttributeContaining("style", popupWidth != null ? "width:" + popupWidth : "max-width:300px");
            shortWait().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#helpDiv")));
            String src = popup.findElement(getDriver()).getAttribute("src");

            fireEvent(thumbnail, SeleniumEvent.mouseout);
            Locator.tagWithClass("th", "labkey-selectors").findElement(getDriver()).click(); // safe out-click in the first header cell
            shortWait().until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("#helpDiv")));

            assertTrue("Wrong image in popup: " + src, src.contains(popupImage));
            assertEquals("Bad response from image pop-up", HttpStatus.SC_OK, WebTestHelper.getHttpResponse(src).getResponseCode());
        }
        else
        {
            Locator popup = Locator.tag("div").withAttribute("id", "helpDiv").descendant("img").withAttributeContaining("src", popupImage);
            assertTrue("Popup should not be present", !popup.existsIn(getDriver()));
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
        assertEquals("A row was updated.", table.getDataAsText(0, "Comment"));
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
        assertEquals("A row was updated.", table.getDataAsText(0, "Comment"));
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
    protected void createList() throws Exception
    {
        //create a list for our query
        clickProject(getProjectName());
        new PortalHelper(this).addWebPart("Lists");

        log("Creating list for query/view/report test...");
        createPeopleListInFolder(getProjectName());

        log("Importing some data...");
        _listHelper.goToList(LIST_NAME);
        _listHelper.clickImportData()
                .setText(LIST_DATA)
                .submit();

        log("Create list in subfolder to prevent query validation failure");
        createPeopleListInFolder(FOLDER_NAME);
        projectMenu().navigateToFolder(getProjectName(), "subfolder");
        log("Create list in container tab containers to prevent query validation failure");
        createPeopleListInTab(STUDY_FOLDER_TAB_LABEL, getProjectName() + "/subfolder/Study Container Tab");
        createPeopleListInTab(ASSAY_FOLDER_TAB_LABEL, getProjectName() + "/subfolder/Assay Container Tab 2");
    }

    private void createPeopleListInFolder(String folderName) throws Exception
    {
        String containerPath = folderName.equals(getProjectName()) ? getProjectName() : getProjectName() + "/" + folderName;
        TestDataGenerator dgen = new TestDataGenerator(new FieldDefinition.LookupInfo(containerPath, "lists", LIST_NAME))
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("Name", FieldDefinition.ColumnType.String).setDescription("Name"),
                        TestDataGenerator.simpleFieldDef("Age", FieldDefinition.ColumnType.Integer).setDescription("Age"),
                        TestDataGenerator.simpleFieldDef("Crazy", FieldDefinition.ColumnType.Boolean).setDescription("Crazy?")
                ));
        dgen.createList(createDefaultConnection(), "Key");
        goToManageLists();
        _listHelper.goToList(LIST_NAME);
    }

    private void createPeopleListInTab(String tabLabel, String containerPath) throws Exception  // todo: post-20.3, change this to go through the UI
    {
        clickTab(tabLabel.replace(" ", ""));
        if (!isElementPresent(Locator.linkWithText("Lists")))
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());
            portalHelper.addWebPart("Lists");
        }

        waitAndClickAndWait(Locator.linkWithText("manage lists"));
        TestDataGenerator dgen = new TestDataGenerator(new FieldDefinition.LookupInfo(containerPath, "lists", LIST_NAME))
                .withColumns(List.of(
                        TestDataGenerator.simpleFieldDef("Name", FieldDefinition.ColumnType.String).setDescription("Name"),
                        TestDataGenerator.simpleFieldDef("Age", FieldDefinition.ColumnType.Integer).setDescription("Age"),
                        TestDataGenerator.simpleFieldDef("Crazy", FieldDefinition.ColumnType.Boolean).setDescription("Crazy?")
                ));
        dgen.createList(createDefaultConnection(), "Key");
        refresh();
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
        int diff = new LevenshteinDistance().apply(expectedIcon.substring(0, lengthToCompare), iconData.substring(0, lengthToCompare));
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
    private void verifyReportThumbnail(@LoggedParam String reportTitle)
    {
        File expectedThumbnailFile = TestFileUtils.getSampleData(THUMBNAIL_FOLDER + reportTitle + THUMBNAIL_FILENAME);
        String expectedThumbnail = TestFileUtils.getFileContents(expectedThumbnailFile);

        WebElement reportLink = waitForElement(Locator.xpath("//a[text()='" + reportTitle + "']"));
        mouseOver(reportLink);
        WebElement thumbnail = waitForElement(Locator.xpath("//div[@class='thumbnail']/img").notHidden());
        String thumbnailData = WebTestHelper.getHttpResponse(thumbnail.getAttribute("src")).getResponseBody();

        int lengthToCompare = 5000;
        int diff = new LevenshteinDistance().apply(expectedThumbnail.substring(0, lengthToCompare), thumbnailData.substring(0, lengthToCompare));
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
        Connection cn = WebTestHelper.getRemoteApiConnection();

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
    private void doTestRowLevelContainerPath()
    {
        beginAt("/simpletest/" + getProjectName() + "/workbookTest.view");

        clickButton("RunContainerPathTest", 0);

        waitForElement(Locator.id("containerPathDiv").withText());

        List<WebElement> errors = Locators.labkeyError.findElements(getDriver());

        if (!errors.isEmpty())
            fail("Error(s) running workbook test. First error: " + errors.get(0).getText());
    }

    @LogMethod
    private void doTestInsertUpdateViews() throws IOException, CommandException
    {
        log("Testings custom views for insert/update/details");

        Connection cn = WebTestHelper.getRemoteApiConnection();

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
        wikiHelper.setWikiBody(TestFileUtils.getFileContents(new File(TestFileUtils.getTestRoot(), "modules/simpletest/resources/views/parameterizedQWP.html.wiki")));
        clickButton("Save & Close");

        log("Check that parameterized query doesn't cause page load.");
        WebElement rootEl = Locators.documentRoot.findElement(getDriver());
        WebElement parameterInput = waitForElement(Locator.xpath("//input[contains(@name, 'param.STARTS_WITH')]"), WAIT_FOR_JAVASCRIPT);
        setFormElement(parameterInput, "P");
        clickButton("Submit", 0);
        waitForText("Manufacturer", "Pinto");
        assertTextNotPresent("Prius");
        try
        {
            rootEl.isEnabled();
        }
        catch (StaleElementReferenceException stale)
        {
            fail("Unwanted page load from parameterized query webpart");
        }
    }

    @LogMethod
    private void doTestFilterSort() throws Exception
    {
        log("** Testing filtering and sorting via java API...");

        Connection cn = WebTestHelper.getRemoteApiConnection();

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

    private final static String GET_MODULEP_PROPS_SCRIPT = "library('Rlabkey')\n" +
            "baseUrl = labkey.url.base\n" +
            "folderPath = \"SimpleModuleTest Project/subfolder\"\n" +
            "moduleName = \"simpletest\"\n" +
            "labkey.getModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestProp1\")\n" +
            "labkey.getModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestProp2\")\n" +
            "labkey.getModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestTextArea\")\n" +
            "labkey.getModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestCheckbox\")\n" +
            "labkey.getModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestSelect\")\n" +
            "labkey.getModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestCombo\")";

    private static final String SET_MODULE_PROPS_SCRIPT = "library('Rlabkey')\n" +
            "baseUrl = labkey.url.base\n" +
            "moduleName = \"simpletest\"\n" +
            "\n" +
            "## set site wide properties\n" +
            "folderPath = \"/\"\n" +
            "labkey.setModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestProp1\", propValue = \"Prop1apiValue\")\n" +
            "labkey.setModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestCheckbox\", propValue = \"false\")\n" +
            "labkey.setModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestSelect\", propValue = \"value2\")\n" +
            "labkey.setModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestCombo\", propValue = \"comboValue2\")\n" +
            "\n" +
            "## set folder level properties\n" +
            "folderPath = \"SimpleModuleTest Project/subfolder\"\n" +
            "labkey.setModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestProp2\", propValue = \"Prop2apiValue\")\n" +
            "labkey.setModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestTextArea\", propValue = \"$$folder1value$$\")\n" +
            "\n" +
            "## set folder level property for another folder\n" +
            "folderPath = \"SimpleModuleTest Project/subfolder2\"\n" +
            "labkey.setModuleProperty(baseUrl, folderPath, moduleName, propName = \"TestTextArea\", propValue = \"$$folder2value$$\")";


    private static final String ENSURE_RLIBPATHS_SOURCE = "library('Rlabkey')\n" +
            "baseUrl = labkey.url.base\n" +
            "moduleName = \"simpletest\"\n" +
            "propName = \"TestTextArea\"\n" +
            "\n" +
            "labkey.ensureRLibPath <- function(append=FALSE)\n" +
            "{\n" +
            "  propValue <- labkey.getModuleProperty(baseUrl, folderPath, moduleName, propName)\n" +
            "  \n" +
            "  splits <- strsplit(propValue, '\\r\\n|\\n|\\r')\n" +
            "  paths <- splits[[1]]\n" +
            "  \n" +
            "  if (append == TRUE)\n" +
            "  \t.libPaths(c(paths, .libPaths()))\n" +
            "  else\n" +
            "  \t.libPaths(c(paths[1], paths[2]))\n" +
            "  \n" +
            "  .libPaths()\n" +
            "}\n" +
            "     \n" +
            "folderPath = \"SimpleModuleTest Project/subfolder\"\n" +
            "print(\"BEGIN-FIRST-CALL\")\n" +
            "labkey.ensureRLibPath()  \n" +
            "print(\"END-FIRST-CALL\")\n" +
            "     \n" +
            "folderPath = \"SimpleModuleTest Project/subfolder2\"  \n" +
            "print(\"BEGIN-SECOND-CALL\")\n" +
            "labkey.ensureRLibPath()\n" +
            "print(\"END-SECOND-CALL\")     \n" +
            "\n" +
            "folderPath = \"SimpleModuleTest Project/subfolder\"  \n" +
            "print(\"BEGIN-THIRD-CALL\")\n" +
            "labkey.ensureRLibPath(append=TRUE)\n" +
            "print(\"END-THIRD-CALL\")\n" +
            "  ";

    @LogMethod
    @Test
    public void testModuleProperties() throws Exception
    {
        RReportHelper rReportHelper = new RReportHelper(this);
        rReportHelper.ensureRConfig(false);

        String prop1 = "TestProp1";
        String prop1Value = "Prop1Value";
        String prop2 = "TestProp2";
        String propTextArea = "TestTextArea";

        beginAt(subfolderPath);
        portalHelper.addWebPart("Simple Module Web Part");
        waitForText("This is a web part view in the simple test module");

        assertEquals("Module context not set properly for text type module property", "DefaultValue", executeScript("return LABKEY.getModuleContext('simpletest')." + prop2));
        assertEquals("Module context not set properly for textArea type module property", "line1\nline2\nline3", executeScript("return LABKEY.getModuleContext('simpletest')." + propTextArea));

        List<ModulePropertyValue> propList = new ArrayList<>();
        propList.add(new ModulePropertyValue(MODULE_NAME, "/", prop1, prop1Value));
        propList.add(new ModulePropertyValue(MODULE_NAME, "/" + getProjectName() + "/" + FOLDER_NAME, prop2 , "FolderValue"));
        propList.add(new ModulePropertyValue(MODULE_NAME, "/" + getProjectName() + "/" + FOLDER_NAME, propTextArea , "updated1\nupdated2", ModulePropertyValue.InputType.textarea));
        propList.add(new ModulePropertyValue(MODULE_NAME, "/", "TestCheckbox", true));
        propList.add(new ModulePropertyValue(MODULE_NAME, "/", "TestSelect", "value1", ModulePropertyValue.InputType.select));
        propList.add(new ModulePropertyValue(MODULE_NAME, "/", "TestCombo", "comboValue1", ModulePropertyValue.InputType.combo));

        validateInputTypes(propList);
        setModuleProperties(propList);
        // Validate values in folder in which they were set
        validateValues(propList);

        log("Verify get module properties using Rlabkey api");
        String apiModulePropResults = rReportHelper.createAndRunRReport("getModuleProps", GET_MODULEP_PROPS_SCRIPT, false);
        List<String> expectedProps = Arrays.asList("[1] \"Prop1Value\"\n",
                "[1] \"FolderValue\"\n",
                "[1] \"updated1\\nupdated2\"\n",
                "[1] \"true\"\n",
                "[1] \"value1\"\n",
                "[1] \"comboValue1\"");
        for (String expected: expectedProps)
            assertTrue("R api labkey.getModuleProperty is not returning module properties as expected", apiModulePropResults.contains(expected));

        log("Set site and folder level module properties using Rlabkey api");
        String fileRootPath1 = getContainerRoot(getProjectName() + "/" + FOLDER_NAME);
        String fileRootPath2 = getContainerRoot(getProjectName() + "/" + FOLDER_NAME_2);
        String fileRootPath3 = getContainerRoot(getProjectName() + "/" + FOLDER_NAME_3);
        String fileRootFolder1 = getRStr(fileRootPath1);
        String fileRootFolder2 = getRStr(fileRootPath2);
        String fileRootFolder3 = getRStr(fileRootPath3);
        String rlibPathsFolder1 = fileRootFolder1 + "\n" + fileRootFolder3;

        String setModulePropsScript = SET_MODULE_PROPS_SCRIPT
                .replace("$$folder1value$$", rlibPathsFolder1)
                .replace("$$folder2value$$", fileRootFolder2);
        rReportHelper.createAndRunRReport("setModuleProps", setModulePropsScript, false);

        log("Verify R api setModuleProperty correctly sets property values");
        goToManageViews();
        waitAndClickAndWait(Locator.linkWithText("getModuleProps"));
        Locator reportOutput = Locator.tagWithClass("table", "labkey-output");
        waitForElement(reportOutput);
        apiModulePropResults = getText(reportOutput);
        expectedProps = Arrays.asList("[1] \"Prop1apiValue\"\n",
                "[1] \"Prop2apiValue\"\n",
                "[1] \"" + rlibPathsFolder1.replace("\n", "\\n") + "\"\n",
                "[1] \"false\"\n",
                "[1] \"value2\"\n",
                "[1] \"comboValue2\"");
        for (String expected: expectedProps)
            assertTrue("R api labkey.getModuleProperty followed by labkey.setModuleProperty is not returning module properties as expected",
                apiModulePropResults.contains(expected));

        log("Test R api getModuleProperty in example of using it to set rLibPaths");
        apiModulePropResults = rReportHelper.createAndRunRReport("ensureRLibPaths", ENSURE_RLIBPATHS_SOURCE, false);
        String folderOneResult = "[1] \"BEGIN-FIRST-CALL\"\n" +
                "[1] \"" + fileRootPath1.replaceAll("\\\\", "/") + "\" \n" +
                "[2] \"" + fileRootPath3.replaceAll("\\\\", "/") + "\"";
        String folderTwoResult = "[1] \"BEGIN-SECOND-CALL\"\n" +
                "[1] \"" + fileRootPath2.replaceAll("\\\\", "/") + "\"";
        String setWithAppend = "[1] \"BEGIN-THIRD-CALL\"\n" +
                "[1] \"" + fileRootPath1.replaceAll("\\\\", "/") + "\" \n" +
                "[2] \"" + fileRootPath3.replaceAll("\\\\", "/") + "\"\n" +
                "[3] \"" + fileRootPath2.replaceAll("\\\\", "/") + "\"";
        assertTrue("ensureRLibPaths script result is not as expected", apiModulePropResults.contains(folderOneResult));
        assertTrue("ensureRLibPaths script result is not as expected", apiModulePropResults.contains(folderTwoResult));
        assertTrue("ensureRLibPaths script result is not as expected", apiModulePropResults.contains(setWithAppend));

        goToProjectHome();
        assertEquals("Module context not set properly", "DefaultValue", executeScript("return LABKEY.getModuleContext('simpletest')." + prop2));

    }

    private String getContainerRoot(String containerPath)
    {
        File containerRoot = new File(getLabKeyRoot(), "build/deploy/files/" + containerPath);
        return containerRoot.getPath();
    }

    private String getRStr(String rawString)
    {
        StringBuilder rStr = new StringBuilder();
        int len = rawString.length();
        for (int i = 0 ; i<len ; i++)
        {
            char c = rawString.charAt(i);
            switch (c)
            {
                case '\\':
                    rStr.append("\\\\");
                    break;
                case '\'':
                    rStr.append("\\'");
                    break;
                case '\"':
                    rStr.append("\\\"");
                    break;
                default:
                    rStr.append(c);
                    break;
            }
        }
        return rStr.toString();
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
            Object expectedValue = String.valueOf(property.getValue());
            Object actualValue = executeScript("return LABKEY.getModuleContext('simpletest')." + property.getPropertyName());
            if (!expectedValue.equals(actualValue))
                errors.add(property.getPropertyName() + " has incorrect value. Expected " + expectedValue + " but was " + actualValue);
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

    @LogMethod
    private void doTestDatasetsAndFileBasedQueries()
    {
        navigateToFolder(getProjectName(), FOLDER_NAME);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Study Overview");
        waitForText("Create Study");
        clickAndWait(Locator.linkWithText("Create Study"));
        clickAndWait(Locator.linkWithText("Create Study"));
        DatasetDesignerPage editDatasetPage = _studyHelper.goToManageDatasets()
                .clickCreateNewDataset()
                .setName(DATASET_NAME)
                .setDatasetLabel(DATASET_LABEL);

        DomainFormPanel fieldsPanel = editDatasetPage.getFieldsPanel();
        fieldsPanel.manuallyDefineFields("First");
        fieldsPanel.addField("Last");

        editDatasetPage.clickSave()
                .clickViewData();
        assertTextPresent("My Custom View", "Hello Dataset", "Visit");
        assertTextNotPresent("Participant Identifier");
    }

    private void editMetadata(String schemaName, String tableName, String operations, String colunmName, String value, String operator)
    {
        goToSchemaBrowser();
        selectQuery(schemaName, tableName);
        waitForText("edit metadata");
        clickAndWait(Locator.linkWithText("edit metadata"));
        // wait for the domain editor to appear:
        clickButton("Edit Source", defaultWaitForPage);
        _ext4Helper.clickExt4Tab("XML Metadata");

        String XML_METADATA = "<tables xmlns=\"http://labkey.org/data/xml\"> \n" +
                "  <table tableName=\"" + tableName + "\" tableDbType=\"TABLE\">\n" +
                "    <columns>\n" +
                "      <column columnName=\"" + colunmName + "\">\n" +
                "        <datatype>varchar</datatype>\n" +
                "        <fk> \n" +
                "           <fkColumnName >Name</fkColumnName > \n" +
                "            <fkTable >Colors</fkTable > \n" +
                "            <fkDbSchema >vehicle</fkDbSchema > \n" +
                "            <filters > \n" +
                "               <filterGroup operation=\"" + operations + "\" > \n" +
                "               <filter column=\"Name\" value=\"" + value + "\" operator=\"" + operator + "\" /> \n" +
                "               </filterGroup > \n" +
                "            </filters > \n" +
                "          </fk > \n" +
                "      </column>\n" +
                "    </columns>\n" +
                "  </table>\n" +
                "</tables>\n";

        setCodeEditorValue("metadataText", XML_METADATA);
        clickButton("Save & Finish");
    }

    private static final String vehicleMetadataJsQuery = "function onFailure(errorInfo, options, responseObj)\n" +
            "{\n" +
            "    if (errorInfo && errorInfo.exception)\n" +
            "        callback(\"Failure: \" + errorInfo.exception);\n" +
            "    else\n" +
            "        callback(\"Failure: \" + responseObj.statusText);\n" +
            "}\n" +
            "\n" +
            "function onSuccess(data)\n" +
            "{\n" +
            "    if(data)\n" +
            "        callback(data);\n" +
            "    else\n" +
            "        callback(\"No data returned!\");\n" +
            "}\n" +
            "\n" +
            "LABKEY.Query.selectRows({\n" +
            "            schemaName: 'vehicle',\n" +
            "            queryName: 'Vehicles',\n" +
            "            columns: ['Color'],\n" +
            "            success: onSuccess,\n" +
            "            failure: onFailure\n" +
            "        });";

    @LogMethod
    private void doTestFkLookupFilter()
    {
        String schemaName = "vehicle";
        String tableName = "Vehicles";
        goToSchemaBrowser();
        viewQueryData("vehicle","Vehicles");

        log("Editing metadata xml to add the filtered foreign key for insert operation");
        editMetadata(schemaName,tableName,"insert", "Color","Green!","eq");

        log("Adding a valid row");
        DataRegionTable table = new DataRegionTable("query",getDriver());
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_ModelId"),"Camry");
        setFormElement(Locator.name("quf_Color"),"Green!");
        setFormElement(Locator.name("quf_ModelYear"),"2019");
        setFormElement(Locator.name("quf_Milage"),"20");
        setFormElement(Locator.name("quf_LastService"),"01/01/2019");
        clickButton("Submit");

        log("Adding a invalid row");
        try
        {
            table.clickInsertNewRow();
            setFormElement(Locator.name("quf_Color"), "Black!");
        }
        catch (NoSuchElementException e)
        {
            String[] str = e.getMessage().split("\\r?\\n");
            assertEquals("Cannot locate option with value: Black!".trim(),str[0].trim());
            clickButton("Cancel");
        }

        log("Check metadata for insert");
        Map insertFilterGroup = getColorColumnFilterGroup();
        Map insertFilterMap = (Map)((List)insertFilterGroup.get("filters")).get(0);
        assertEquals("Filter column 'Name' for vehicle.Vehicles query not found", "Name", insertFilterMap.get("column"));
        assertEquals("Filter operator 'eq' for vehicle.Vehicles query not found", "eq", insertFilterMap.get("operator"));
        assertEquals("Filter value 'Green!' for vehicle.Vehicles query not found!", "Green!", insertFilterMap.get("value"));
        assertEquals("Filter operation 'insert' for vehicle.Vehicles query not found!", "insert", insertFilterGroup.get("operation"));

        log("Editing metadata xml to add the filtered foreign key for update operation");
        editMetadata(schemaName,tableName,"update", "Color","Bl", "contains");
        table.clickEditRow(0);
        setFormElement(Locator.name("quf_Color"), "Blue!");
        clickButton("Submit");
        assertEquals("Blue!", table.getDataAsText(0,"Color"));

        log("Check metadata for update");
        Map updateFilterGroup = getColorColumnFilterGroup();
        Map updateFilterMap = (Map)((List)updateFilterGroup.get("filters")).get(0);
        assertEquals("Filter column 'Name' for vehicle.Vehicles query not found", "Name", updateFilterMap.get("column"));
        assertEquals("Filter operator 'contains' for vehicle.Vehicles query not found", "contains", updateFilterMap.get("operator"));
        assertEquals("Filter value 'Blue!' for vehicle.Vehicles query not found!", "Bl", updateFilterMap.get("value"));
        assertEquals("Filter operation 'update' for vehicle.Vehicles query not found!", "update", updateFilterGroup.get("operation"));
    }

    private Map getColorColumnFilterGroup()
    {
        Map result = (Map)executeAsyncScript(vehicleMetadataJsQuery);
        Map colorColumnFields = ((List<Map>)(((Map)result.get("metaData")).get("fields"))).get(0);

        assertEquals("Column fields for column 'Color' in vehicle.Vehicles query not found!", colorColumnFields.get("name"), "Color");

        return (Map)((List)((Map)colorColumnFields.get("lookup")).get("filterGroups")).get(0);
    }

    @LogMethod
    private void doTestCustomLogin()
    {
        log("Test basic override of login page");
        goToAdminConsole().goToSettingsSection().clickLookAndFeelSettings();
        LookAndFeelSettingsPage lookAndFeelSettingsPage = new LookAndFeelSettingsPage(getDriver());
        lookAndFeelSettingsPage.setAltLoginPage("simpletest-testCustomLogin");
        lookAndFeelSettingsPage.save();
        signOut();
        ensureSignedOut();

        beginAt(WebTestHelper.buildURL("login", "login"));
        waitForAnyElement("Should be on login or Home portal", Locator.id("email"), SiteNavBar.Locators.userMenu);
        assertElementPresent(Locator.tagWithText("p", "SimpleTest Module Custom Sign In"));
        signIn();
        assertEquals("This should redirect to /home", "/home", getCurrentContainerPath());

        log("Test override of login page using a hard-coded returnUrl");
        goToAdminConsole().goToSettingsSection().clickLookAndFeelSettings();
        lookAndFeelSettingsPage = new LookAndFeelSettingsPage(getDriver());
        lookAndFeelSettingsPage.setAltLoginPage("simpletest-testCustomLoginWithReturnUrl");
        lookAndFeelSettingsPage.save();
        signOut();
        ensureSignedOut();

        beginAt(WebTestHelper.buildURL("login", "login"));
        waitForAnyElement("Should be on login or Home portal", Locator.id("email"), SiteNavBar.Locators.userMenu);
        assertElementPresent(Locator.tagWithText("p", "SimpleTest Module Custom Sign In With Custom ReturnUrl"));
        signIn();
        assertEquals("This should redirect to /Shared", "/Shared", getCurrentContainerPath());

        log("ensure we dont get an IllegalArgumentException if an empty string is saved as login page");
        goToAdminConsole().goToSettingsSection().clickLookAndFeelSettings();
        lookAndFeelSettingsPage = new LookAndFeelSettingsPage(getDriver());
        lookAndFeelSettingsPage.setAltLoginPage("");
        lookAndFeelSettingsPage.save();
        signOut();
        ensureSignedOut();
        signIn();

        log("restore original login page");
        goToAdminConsole().goToSettingsSection().clickLookAndFeelSettings();
        lookAndFeelSettingsPage = new LookAndFeelSettingsPage(getDriver());
        lookAndFeelSettingsPage.setAltLoginPage("login-login");
        lookAndFeelSettingsPage.save();
        goToProjectHome();
    }


    @LogMethod
    private void doTestRestrictedModule() throws Exception
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
        importFolderFromZip(RESTRICTED_FOLDER_IMPORT_NAME, false, 1, true);
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

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
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

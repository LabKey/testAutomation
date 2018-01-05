/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Data;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.WorkbookHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class, Data.class})
public class ContainerContextTest extends BaseWebDriverTest
{
    private static final String SUB_FOLDER_A = "A";
    private static final String SUB_FOLDER_B = "B";

    protected static final String TEST_ASSAY_A = "Test Assay A";
    protected static final String TEST_ASSAY_DESC_A = "Description for assay A";

    protected static final String TEST_ASSAY_B = "Test Assay B";
    protected static final String TEST_ASSAY_DESC_B = "Description for assay B";

    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    private final static String LIST_KEY_NAME = "Key";

    private static final String COLOR = "Red";
    private static final String MANUFACTURER = "Toyota";
    private static final String MODEL = "Prius C";
    private RReportHelper _RReportHelper = new RReportHelper(this);
    private PortalHelper _portalHelper = new PortalHelper(this);

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query", "viscstudies");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        if (afterTest)
        {
            try
            {
                deleteVehicleRecords();
            }
            catch (IOException | CommandException rethrow)
            {
                throw new RuntimeException(rethrow);
            }
        }

        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setup() throws Exception
    {
        ContainerContextTest init = (ContainerContextTest)getCurrentTest();
        init.doSetup();
    }

    protected void doSetup() throws Exception
    {
        _RReportHelper.ensureRConfig();

        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModules(Arrays.asList("simpletest", "ViscStudies"));
        _portalHelper.addWebPart("Workbooks");

        _containerHelper.createSubfolder(getProjectName(), SUB_FOLDER_A, new String[]{"List", "Study", "ViscStudies"});
        _containerHelper.createSubfolder(getProjectName(), SUB_FOLDER_B, new String[]{"List", "Study", "ViscStudies"});
    }

    @Test
    public void testListLookupURL()
    {
        log("** Creating lookup target list in sub-folder");
        goToProjectHome();
        ListHelper.ListColumn[] lookupTargetCols = {
            new ListHelper.ListColumn("LookupName", "LookupName", ListHelper.ListColumnType.String, "Lookup Name"),
            new ListHelper.ListColumn("LookupAge", "LookupAge", ListHelper.ListColumnType.Integer, "Lookup Age", null, null, null, "fake/action.view?key=${Key}")
        };
        String lookupTargetListName = SUB_FOLDER_A + "-LookupTarget-List";
        _listHelper.createList(getProjectName() + "/" + SUB_FOLDER_A, lookupTargetListName, LIST_KEY_TYPE, LIST_KEY_NAME, lookupTargetCols);

        log("** Insert row into lookup target list");
        goToProjectHome();
        clickFolder(SUB_FOLDER_A);
        clickAndWait(Locator.linkWithText(lookupTargetListName));
        _listHelper.insertNewRow(Maps.of(
                "LookupName", "MyLookupItem1",
                "LookupAge", "100"
        ));
        _listHelper.insertNewRow(Maps.of(
                "LookupName", "MyLookupItem2",
                "LookupAge", "200"
        ));

        log("** Creating list with lookup to list in sub-folder");
        goToProjectHome();
        ListHelper.ListColumn[] cols = {
            new ListHelper.ListColumn("MyName", "MyName", ListHelper.ListColumnType.String, "My Name"),
            new ListHelper.ListColumn("ListLookup", "ListLookup", ListHelper.ListColumnType.String, "List Lookup", new ListHelper.LookupInfo(getProjectName() + "/" + SUB_FOLDER_A, "lists", lookupTargetListName)),
        };
        String lookupSourceListName = "Project-LookupSource-List";
        _listHelper.createList(getProjectName(), lookupSourceListName, LIST_KEY_TYPE, LIST_KEY_NAME, cols);
        clickButton("Done");

        log("** Insert row into list");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(lookupSourceListName));
        DataRegionTable table = new DataRegionTable("query", this);
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_MyName"), "MyName");
        selectOptionByText(Locator.name("quf_ListLookup"), "MyLookupItem2");
        clickButton("Submit");

        log("** Adding in lookup list columns to grid");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn(new String[] { "ListLookup", "LookupAge" });
        _customizeViewsHelper.saveCustomView();

        log("** Checking URLs go to correct container...");
        String href = getAttribute(DataRegionTable.updateLinkLocator(), "href");
        assertTrue("Expected [edit] link to go to " + getProjectName() + " container, href=" + href,
                href.contains(getProjectName()) && href.contains("query") && href.contains("updateQueryRow.view"));

        href = getAttribute(DataRegionTable.detailsLinkLocator(), "href");
        assertTrue("Expected [details] link to go to " + getProjectName() + " container, href=" + href,
                href.contains(getProjectName()) && href.contains("list") && href.contains("details.view"));

        href = getAttribute(Locator.linkWithText("MyName"), "href");
        assertTrue("Expected MyName link to go to " + getProjectName() + " container, href=" + href,
                href.contains(getProjectName()) && href.contains("list") && href.contains("details.view"));

        href = getAttribute(Locator.linkWithText("MyLookupItem2"), "href");
        assertTrue("Expected ListLookup link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container, href=" + href,
                href.contains(getProjectName() + "/" + SUB_FOLDER_A) && href.contains("list") && href.contains("details.view"));

        href = getAttribute(Locator.linkWithText("200"), "href");
        assertTrue("Expected ListLookup/LookupAge link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container, href=" + href,
                href.contains(getProjectName() + "/" + SUB_FOLDER_A) && href.contains("fake") && href.contains("action.view?key=2"));
    }

    // Issue 15610: viscstudieslist - URLs generated from lookups are broken
    @Test
    public void testIssue15610()
    {
        log("** Creating study in " + SUB_FOLDER_A);
        goToProjectHome();
        clickFolder(SUB_FOLDER_A);
        goToManageStudy();
        clickButton("Create Study");
        setFormElement(Locator.name("label"), SUB_FOLDER_A + "-Study");
        clickButton("Create Study");

        log("** Creating study in " + SUB_FOLDER_B);
        goToProjectHome();
        clickFolder(SUB_FOLDER_B);
        goToManageStudy();
        clickButton("Create Study");
        setFormElement(Locator.name("label"), SUB_FOLDER_B + "-Study");
        clickButton("Create Study");

        log("** Creating list with lookup to viscstudies.studies");
        ListHelper.ListColumn[] cols = {
            new ListHelper.ListColumn("StudyLookup", "StudyLookup", ListHelper.ListColumnType.String, "Study Lookup", new ListHelper.LookupInfo(null, "viscstudies", "studies")),
        };
        _listHelper.createList(getProjectName(), "Issue15610-List", LIST_KEY_TYPE, LIST_KEY_NAME, cols);
        clickButton("Done");

        log("** Inserting row into list");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Issue15610-List"));
        DataRegionTable.findDataRegion(this).clickInsertNewRow();
        selectOptionByText(Locator.name("quf_StudyLookup"), SUB_FOLDER_A + "-Study");
        clickButton("Submit");

        DataRegionTable.findDataRegion(this).clickInsertNewRow();
        selectOptionByText(Locator.name("quf_StudyLookup"), SUB_FOLDER_B + "-Study");
        clickButton("Submit");

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.linkWithText(SUB_FOLDER_A + "-Study"), "href");
        assertTrue("Expected 'MyStudy' link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container: " + href,
                href.contains("/study/" + getProjectName() + "/" + SUB_FOLDER_A + "/studySchedule.view") ||
                href.contains("/" + getProjectName() + "/" + SUB_FOLDER_A + "/study-studySchedule.view"));

        href = getAttribute(Locator.linkWithText(SUB_FOLDER_B + "-Study"), "href");
        assertTrue("Expected 'MyStudy' link to go to " + getProjectName() + "/" + SUB_FOLDER_B + " container: " + href,
                href.contains("/study/" + getProjectName() + "/" + SUB_FOLDER_B + "/studySchedule.view") ||
                href.contains("/" + getProjectName() + "/" + SUB_FOLDER_B + "/study-studySchedule.view"));
    }

    // Issue 15751: Pipeline job list generates URLs without correct container
    @Test
    public void testIssue15751()
    {
        log("** Create pipeline jobs");
        insertJobIntoSubFolder(SUB_FOLDER_A);
        insertJobIntoSubFolder(SUB_FOLDER_B);

        log("** Viewing pipeline status from project container. Sort by Description (report name) and include sub-folders");
        beginAt("/pipeline-status/" + getProjectName() + "/showList.view?StatusFiles.sort=Description&StatusFiles.containerFilterName=CurrentAndSubfolders");

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.tagWithText("a", "COMPLETE").index(0), "href");
        assertTrue("Expected 'COMPLETE' link 0 to go to current A container: " + href,
                href.contains("/pipeline-status/" + getProjectName() + "/" + SUB_FOLDER_A + "/details.view") ||
                href.contains("/" + getProjectName() + "/" + SUB_FOLDER_A + "/pipeline-status-details.view"));

        href = getAttribute(Locator.tagWithText("a", "COMPLETE").index(1), "href");
        assertTrue("Expected 'COMPLETE' link 1 to go to current B container: " + href,
                href.contains("/pipeline-status/" + getProjectName() + "/" + SUB_FOLDER_B + "/details.view") ||
                href.contains("/" + getProjectName() + "/" + SUB_FOLDER_B + "/pipeline-status-details.view"));
    }

    @LogMethod
    public void insertJobIntoSubFolder(String folder)
    {
        goToProjectHome();

        log("** Creating list in folder '" + folder + "'");
        ListHelper.ListColumn[] cols = {
            new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name")
        };
        String listName = folder + "-Issue15751-List";
        _listHelper.createList(getProjectName() + "/" + folder, listName, LIST_KEY_TYPE, LIST_KEY_NAME, cols);
        clickButton("Done");

        log("** Creating background R script");
        goToProjectHome();
        clickFolder(folder);
        clickAndWait(Locator.linkWithText(listName));

        String reportName = folder + "-BackgroundReport";
        DataRegionTable table = new DataRegionTable("query", this);
        table.goToReport("Create R Report");
        _RReportHelper.selectOption(RReportHelper.ReportOption.runInPipeline);
        _RReportHelper.saveReport(reportName);

        log("** Executing background R script");
        waitForElement(Locator.lkButton("Start Job"), WAIT_FOR_JAVASCRIPT);
        clickButton("Start Job", 0);
        waitForElementToDisappear(Ext4Helper.Locators.window("Start Pipeline Job"));
        goToModule("Pipeline");
        waitForPipelineJobsToFinish(1);
    }

    // Issue 20375: DetailsURL link has no container in certain cases
    @Test
    public void testIssue20375()
    {
        log("** Create wiki pages in subfolders");
        goToProjectHome();
        WikiHelper wikiHelper = new WikiHelper(this);

        clickFolder(SUB_FOLDER_A);
        clickTab("Wiki");
        wikiHelper.createWikiPage("subfolder-a", null, "title-a", "content-a", false, null, false);

        clickFolder(SUB_FOLDER_B);
        clickTab("Wiki");
        wikiHelper.createWikiPage("subfolder-b", null, "title-b", "content-b", false, null, false);

        // Bug would originally only repro when all columns with URLs have been removed so we only include the 'Created' column.
        goToProjectHome();
        navigateToQuery("wiki", "CurrentWikiVersions");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.clearColumns();
        _customizeViewsHelper.addColumn("Created");
        _customizeViewsHelper.addSort("Created", SortDirection.ASC);
        _customizeViewsHelper.saveCustomView("CreatedOnly");

        DataRegionTable table = new DataRegionTable("query", this);
        table.setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_AND_SUBFOLDERS);
        assertEquals(2, table.getDataRowCount());

        log("** Validate detailsURL goes to " + SUB_FOLDER_A);
        String detailsURL = table.getDetailsHref(0);
        log("  detailsURL = " + detailsURL);
        assertTrue("Expected details URL to contain subfolder A:" + detailsURL,
                detailsURL.contains("/wiki/" + getProjectName() + "/" + SUB_FOLDER_A + "/page.view?name=subfolder-a") ||
                        detailsURL.contains("/" + getProjectName() + "/" + SUB_FOLDER_A + "/wiki-page.view?name=subfolder-a"));

        log("** Validate detailsURL goes to " + SUB_FOLDER_B);
        detailsURL = table.getDetailsHref(1);
        log("  detailsURL = " + detailsURL);
        assertTrue("Expected details URL to contain subfolder B: " + detailsURL,
                detailsURL.contains("/wiki/" + getProjectName() + "/" + SUB_FOLDER_B + "/page.view?name=subfolder-b") ||
                detailsURL.contains("/" + getProjectName() + "/" + SUB_FOLDER_B + "/wiki-page.view?name=subfolder-b"));
    }

    @Test
    public void testSimpleModuleTables() throws Exception
    {
        log("** Creating required vehicle schema records...");
        int vehicleId = createRequiredRecords();

        log("** Inserting data into vehicle.emissiontest table...");
        String[] workbookIds = new String[3];

        String[] emissionIds = new String[3];
        String[] parentRowIds = new String[3];
        Map<String, String> rowIdToWorkbookId = new HashMap<>();
        WorkbookHelper workbookHelper = new WorkbookHelper(this);
        int max = 3;
        for (int i = 0; i < max; i++)
        {
            String workbookName = "Workbook" + i;
            goToProjectHome();

            String id = String.valueOf(workbookHelper.createWorkbook(getProjectName(), workbookName, "Description", WorkbookHelper.WorkbookFolderType.DEFAULT_WORKBOOK));
            workbookIds[i] = id;
            parentRowIds[i] = i > 0 ? emissionIds[i-1] : null;
            try
            {
                emissionIds[i] = insertEmissionTest(workbookIds[i], String.valueOf(i), vehicleId, parentRowIds[i]);
            }
            catch (IOException | CommandException fail) {throw new RuntimeException(fail);}
            rowIdToWorkbookId.put(emissionIds[i], workbookIds[i]);
        }

        log("** Checking default case, which includes the container column...");
        verifySimpleModuleTables("EmissionTest", "detailsQueryRow.view", "detailsQueryRow.view", max, workbookIds, emissionIds, parentRowIds, rowIdToWorkbookId, true, true, vehicleId);

        // Verify Issue 16243: Details URL creating URLs with null container unless the container column is actually added to current view
        log("** Removing container column and rechecking lookup URLs...");
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=vehicle&query.queryName=EmissionTest&query.sort=RowId");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.removeColumn("Container");
        _customizeViewsHelper.applyCustomView();

        verifySimpleModuleTables("EmissionTest", "detailsQueryRow.view", "detailsQueryRow.view", max, workbookIds, emissionIds, parentRowIds, rowIdToWorkbookId, false, true, vehicleId);

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.revertUnsavedView();


        log("** Override detailsURL in metadata...");
        String customMetadata =
                "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
                "  <table tableName=\"EmissionTest\" tableDbType=\"TABLE\" useColumnOrder=\"true\">\n" +
                "    <tableTitle>Custom Query</tableTitle>\n" +
                "    <!--<javaCustomizer class=\"org.labkey.ldk.query.BuiltInColumnsCustomizer\" />-->\n" +
                "    <titleColumn>rowid</titleColumn>\n" +
                "    <updateUrl>/query/updateQueryRow.view?schemaName=vehicle&amp;query.queryName=EmissionTest&amp;RowId=${rowid}</updateUrl>\n" +
                "    <tableUrl>/query/XXX.view?schemaName=vehicle&amp;query.queryName=EmissionTest&amp;RowId=${rowid}</tableUrl>\n" +
                "    <insertUrl></insertUrl>\n" +
                "    <importUrl>/query/importData.view?schemaName=vehicle&amp;query.queryName=EmissionTest&amp;RowId=${rowid}&amp;query.columns=*</importUrl>\n" +
                "  </table>\n" +
                "</tables>";

        overrideMetadata(getProjectName(), "vehicle", "EmissionTest", customMetadata);
        verifySimpleModuleTables("EmissionTest", "XXX.view", "XXX.view", max, workbookIds, emissionIds, parentRowIds, rowIdToWorkbookId, false, true, vehicleId);
        removeMetadata(getProjectName(), "vehicle", "EmissionTest");
        
        log("** Create custom query with custom metadata over vehicle.emissiontest table WITH container");
        String customQueryWithContainer =
                "SELECT emissiontest.rowid,\n" +
                    "emissiontest.Name,\n" +
                    "emissiontest.VehicleId,\n" +
                    "emissiontest.Result,\n" +
                    "emissiontest.ParentTest,\n" +
                    "emissiontest.Container\n" +
                "FROM emissiontest";

        createQuery(getProjectName(), "EmissionTests With Container", "vehicle", customQueryWithContainer, customMetadata, false);
        verifySimpleModuleTables("EmissionTests With Container", "XXX.view", "detailsQueryRow.view", max, workbookIds, emissionIds, parentRowIds, rowIdToWorkbookId, true, false, vehicleId);


        log("** Create custom query with custom metadata over vehicle.emissiontest table WITH container AS folder");
        String customQueryFolderContainer =
                "SELECT emissiontest.Rowid,\n" +
                    "emissiontest.Name,\n" +
                    "emissiontest.VehicleId,\n" +
                    "emissiontest.Result,\n" +
                    "emissiontest.ParentTest,\n" +
                    "emissiontest.Container AS Folder\n" +
                "FROM emissiontest";

        createQuery(getProjectName(), "EmissionTests With Folder", "vehicle", customQueryFolderContainer, customMetadata, false);
        verifySimpleModuleTables("EmissionTests With Folder", "XXX.view", "detailsQueryRow.view", max, workbookIds, emissionIds, parentRowIds, rowIdToWorkbookId, false, false, vehicleId);

        log("** Create custom query with custom metadata over vehicle.emissiontest table WITHOUT container.");
        log("** The container column should be added as a suggested column.");
        String customQueryWithoutContainer =
                "SELECT emissiontest.Rowid,\n" +
                    "emissiontest.Name,\n" +
                    "emissiontest.VehicleId,\n" +
                    "emissiontest.Result,\n" +
                    "emissiontest.ParentTest,\n" +
                    "--emissiontest.Container\n" +
                "FROM emissiontest";

        createQuery(getProjectName(), "EmissionTests Without Container", "vehicle", customQueryWithoutContainer, customMetadata, false);
        verifySimpleModuleTables("EmissionTests Without Container", "XXX.view", "detailsQueryRow.view", max, workbookIds, emissionIds, parentRowIds, rowIdToWorkbookId, false, false, vehicleId);
    }

    @Test
    public void testAssayList()
    {
        goToProjectHome();
        clickFolder(SUB_FOLDER_A);
        log("Defining a test assay at subfolder A");
        _portalHelper.addWebPart("Assay List");
        //clickButton("Manage Assays");
        clickButton("New Assay Design");
        assertElementNotPresent(Locator.radioButtonByNameAndValue("providerName", "Flow"));
        checkRadioButton(Locator.radioButtonByNameAndValue("providerName", "General"));
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), TEST_ASSAY_A);
        setFormElement(Locator.xpath("//textarea[@id='AssayDesignerDescription']"), TEST_ASSAY_DESC_A);
        clickButton("Save", 0);
        waitForText("Save successful");

        goToProjectHome();
        clickFolder(SUB_FOLDER_B);
        log("Defining a test assay at subfolder B");
        _portalHelper.addWebPart("Assay List");
        //clickButton("Manage Assays");
        clickButton("New Assay Design");
        assertElementNotPresent(Locator.radioButtonByNameAndValue("providerName", "Flow"));
        checkRadioButton(Locator.radioButtonByNameAndValue("providerName", "General"));
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), TEST_ASSAY_B);
        setFormElement(Locator.xpath("//textarea[@id='AssayDesignerDescription']"), TEST_ASSAY_DESC_B);
        clickButton("Save", 0);
        waitForText("Save successful");

        goToProjectHome();
        _portalHelper.addWebPart("Assay List");

        //Details URL should point to assays begin view in current container, not container where it was defined
        String detailsURL_A = getAttribute(Locator.tagWithText("a", TEST_ASSAY_A), "href");
        String detailsURL_B = getAttribute(Locator.tagWithText("a", TEST_ASSAY_B), "href");
        log("  detailsURL_A = " + detailsURL_A);
        assertTrue("Expected details URL to point to project home " + detailsURL_A,
                detailsURL_A.contains(WebTestHelper.buildRelativeUrl("assay", getProjectName(), "assayBegin")));
        log("  detailsURL_B = " + detailsURL_B);
        assertTrue("Expected details URL to point to project home " + detailsURL_B,
                detailsURL_B.contains(WebTestHelper.buildRelativeUrl("assay", getProjectName(), "assayBegin")));
        clickFolder(SUB_FOLDER_A);
        detailsURL_A = getAttribute(Locator.tagWithText("a", TEST_ASSAY_A), "href");
        detailsURL_B = getAttribute(Locator.tagWithText("a", TEST_ASSAY_B), "href");
        log("  detailsURL_A = " + detailsURL_A);
        assertTrue("Expected details URL to point to subfolder " + detailsURL_A,
                detailsURL_A.contains(WebTestHelper.buildRelativeUrl("assay", getProjectName() + "/" + SUB_FOLDER_A, "assayBegin")));
        log("  detailsURL_B = " + detailsURL_B);
        assertTrue("Expected details URL to point to subfolder " + detailsURL_B,
                detailsURL_B.contains(WebTestHelper.buildRelativeUrl("assay", getProjectName() + "/" + SUB_FOLDER_A, "assayBegin")));
        clickFolder(SUB_FOLDER_B);
        detailsURL_A = getAttribute(Locator.tagWithText("a", TEST_ASSAY_A), "href");
        detailsURL_B = getAttribute(Locator.tagWithText("a", TEST_ASSAY_B), "href");
        log("  detailsURL_A = " + detailsURL_A);
        assertTrue("Expected details URL to point to subfolder " + detailsURL_A,
                detailsURL_A.contains(WebTestHelper.buildRelativeUrl("assay", getProjectName() + "/" + SUB_FOLDER_B, "assayBegin")));
        log("  detailsURL_B = " + detailsURL_B);
        assertTrue("Expected details URL to point to subfolder " + detailsURL_B,
                detailsURL_B.contains(WebTestHelper.buildRelativeUrl("assay", getProjectName() + "/" + SUB_FOLDER_B, "assayBegin")));
    }

    protected void overrideMetadata(String container, String schemaName, String queryName, String xml)
    {
        beginAt(WebTestHelper.buildURL("query", container, "schema", Maps.of("schemaName", schemaName, "queryName", queryName)));
        waitForText(10000, "edit metadata");
        clickAndWait(Locator.linkWithText("edit metadata"));
        waitForText(10000, "Label");
        clickButton("Edit Source", WAIT_FOR_PAGE);
        _ext4Helper.clickExt4Tab("XML Metadata");
        setCodeEditorValue("metadataText", xml);
        clickButton("Save", 0);
        waitForElement(Locator.id("status").withText("Saved"), WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    protected void removeMetadata(String container, String schemaName, String queryName)
    {
        overrideMetadata(container, schemaName, queryName, "");
    }

    @LogMethod
    private void verifySimpleModuleTables(
            String queryName,
            String detailsAction,
            String parentDetailsAction,
            int max,
            String[] workbookIds,
            String[] emissionIds,
            String[] parentRowIds,
            Map<String, String> rowIdToWorkbookId,
            boolean hasContainer,
            boolean hasUpdate,
            int vehicleId)
    {
        log("** Checking containers on lookup URLs for '" + queryName + "'");
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=vehicle&query.queryName=" + queryName + "&query.sort=RowId");

        DataRegionTable dr = new DataRegionTable("query", this);

        for (int i = 0; i < max; i++)
        {
            String workbookContainer = EscapeUtil.encode(getProjectName()) + "/" + workbookIds[i];
            String href;
            String expectedHref;
            String expectedContainerRelativeHref;

            // update link
            if (hasUpdate)
            {
                href = dr.getUpdateHref(i);
                log("  [edit] column href = " + href);

                String rest = "?schemaName=vehicle&query.queryName=EmissionTest&RowId=" + emissionIds[i];
                expectedHref = "/query/" + workbookContainer + "/updateQueryRow.view" + rest;
                expectedContainerRelativeHref = "/" + workbookContainer + "/query-updateQueryRow.view" + rest;

                assertTrue("Expected and actual [edit] links differ:\n" +
                        "Expected: " + expectedHref + "\n" +
                        "Actual  : " + href,
                        href != null && (href.contains(expectedHref) || href.contains(expectedContainerRelativeHref)));
            }

            // details link
            if (detailsAction != null)
            {
                href = dr.getDetailsHref(i);
                log("  [details] column href = " + href);

                String rest = "?schemaName=vehicle&query.queryName=EmissionTest&RowId=" + emissionIds[i];
                expectedHref = "/query/" + workbookContainer + "/" + detailsAction + rest;
                expectedContainerRelativeHref = "/" + workbookContainer + "/query-" + detailsAction + rest;

                assertTrue("Expected and actual [details] links differ:\n" +
                        "Expected: " + expectedHref + "\n" +
                        "Actual:   " + href,
                        href != null && (href.contains(expectedHref) || href.contains(expectedContainerRelativeHref)));
            }

            // vehicle link
            href = dr.getHref(i, "Vehicle Id");
            log("  Vehicle column href = " + href);

            expectedHref = "/simpletest/" + getProjectName() + "/vehicle.view?rowid=" + vehicleId;
            expectedContainerRelativeHref = "/" + getProjectName() + "/simpletest-vehicle.view?rowid=" + vehicleId;

            assertTrue("Expected and actual Vehicle column URL differ:\n" +
                    "Expected: " + expectedHref + "\n" +
                    "Actual:   " + href,
                    href != null && (href.contains(expectedHref) || href.contains(expectedContainerRelativeHref)));

            // parent sample ID link (table has a container so URL should go to lookup's container)
            if (parentRowIds[i] != null && !parentRowIds[i].equals("") && parentDetailsAction != null)
            {
                String parentTestWorkbookId = rowIdToWorkbookId.get(parentRowIds[i]);
                String parentTestContainer = EscapeUtil.encode(getProjectName()) + "/" + parentTestWorkbookId;
                String rest = "?schemaName=vehicle&query.queryName=EmissionTest&RowId=" + parentRowIds[i];
                expectedHref = "/query/" + parentTestContainer + "/" + parentDetailsAction + rest;
                expectedContainerRelativeHref = "/" + parentTestContainer + "/query-" + parentDetailsAction + rest;

                href = dr.getHref(i, "Parent Test");
                if (href != null)
                {
                    log("  Parent test column href = " + href);
                    assertTrue("Expected and actual parent test column URL differ:\n" +
                            "Expected: " + expectedHref + "\n" +
                            "Actual:   " + href,
                            (href.contains(expectedHref) || href.contains(expectedContainerRelativeHref)));
                }
            }

            // container column
            if (hasContainer)
            {
                href = dr.getHref(i, "Folder");

                log("  Folder column href = " + href);
                expectedHref = "/project/" + workbookContainer + "/begin.view?";
                expectedContainerRelativeHref = "/" + workbookContainer + "/project-begin.view?";

                assertTrue("Expected and actual container column URL differ:\n" +
                        "Expected container: " + workbookContainer + "\n" +
                        "Actual URL        : " + href,
                        href != null && (href.contains(expectedHref) || href.contains(expectedContainerRelativeHref)));
            }

            log("");
        }

        log("** Checked containers on lookup URLs for query '" + queryName + "'\n");
    }

    @LogMethod
    private String insertEmissionTest(String workbookId, String suffix, int vehicleId, String parentRowId) throws IOException, CommandException
    {
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

            InsertRowsCommand insertCmd = new InsertRowsCommand("vehicle", "EmissionTest");
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("name", "EmissionTest" + suffix);
            rowMap.put("vehicleId", vehicleId);

            if (parentRowId != null)
                rowMap.put("ParentTest", parentRowId);

            rowMap.put("result", false);

            insertCmd.addRow(rowMap);
            SaveRowsResponse response = insertCmd.execute(cn, getProjectName() + "/" + workbookId);
            Map<String, Object> row = response.getRows().get(0);
            Long rowId = (Long)row.get("RowId");
            return rowId.toString();
    }

    @LogMethod
    private void deleteVehicleRecords() throws IOException, CommandException
    {
            log("deleting records from vehicle schema that may have been created by this test");
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

            SelectRowsCommand sr0 = new SelectRowsCommand("vehicle", "EmissionTest");
            SelectRowsResponse resp0 = sr0.execute(cn, getProjectName());

            if (resp0.getRowCount().intValue() > 0)
            {
                DeleteRowsCommand del = new DeleteRowsCommand("vehicle", "EmissionTest");
                for (Map<String, Object> row : resp0.getRows())
                {
                    del.addRow(row);
                }
                del.execute(cn, getProjectName());
            }

            SelectRowsCommand sr1 = new SelectRowsCommand("vehicle", "vehicles");
            SelectRowsResponse resp1 = sr1.execute(cn, getProjectName());

            if (resp1.getRowCount().intValue() > 0)
            {
                DeleteRowsCommand del = new DeleteRowsCommand("vehicle", "vehicles");
                for (Map<String, Object> row : resp1.getRows())
                {
                    del.addRow(row);
                }
                del.execute(cn, getProjectName());
            }

            SelectRowsCommand sr2 = new SelectRowsCommand("vehicle", "models");
            sr2.addFilter(new Filter("name", MODEL));
            SelectRowsResponse resp2 = sr2.execute(cn, getProjectName());

            if (resp2.getRowCount().intValue() > 0)
            {
                DeleteRowsCommand del2 = new DeleteRowsCommand("vehicle", "models");
                del2.addRow(Maps.of("rowid", resp2.getRows().get(0).get("rowid")));
                del2.execute(cn, getProjectName());
            }

            SelectRowsCommand sr = new SelectRowsCommand("vehicle", "manufacturers");
            sr.addFilter(new Filter("name", MANUFACTURER));
            SelectRowsResponse resp = sr.execute(cn, getProjectName());

            if (resp.getRowCount().intValue() > 0)
            {
                DeleteRowsCommand del1 = new DeleteRowsCommand("vehicle", "manufacturers");
                del1.addRow(Maps.of("rowid", resp.getRows().get(0).get("rowid")));
                del1.execute(cn, getProjectName());
            }

            DeleteRowsCommand del2 = new DeleteRowsCommand("vehicle", "colors");
            del2.addRow(Maps.of("name", COLOR + "!"));
            del2.execute(cn, getProjectName());
    }

    @LogMethod
    private int createRequiredRecords() throws IOException, CommandException
    {
            deleteVehicleRecords();  //schema should be enabled, so dont ignore exceptions

            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

            //look like we need to create this too
            InsertRowsCommand insertCmd0 = new InsertRowsCommand("vehicle", "colors");
            insertCmd0.addRow(Maps.of("Name", COLOR, "Hex", "#FF0000"));
            insertCmd0.execute(cn, getProjectName());

            //then create manufacturer
            InsertRowsCommand insertCmd = new InsertRowsCommand("vehicle", "manufacturers");
            Map<String,Object> rowMap = new HashMap<>();
            rowMap.put("name", MANUFACTURER);
            insertCmd.addRow(rowMap);
            SaveRowsResponse resp1 = insertCmd.execute(cn, getProjectName());

            //then create model
            InsertRowsCommand insertCmd2 = new InsertRowsCommand("vehicle", "models");
            rowMap = new HashMap<>();
            rowMap.put("manufacturerId",  resp1.getRows().get(0).get("rowid"));
            rowMap.put("name", MODEL);
            insertCmd2.addRow(rowMap);
            SaveRowsResponse resp2 = insertCmd2.execute(cn, getProjectName());

            InsertRowsCommand insertCmd3 = new InsertRowsCommand("vehicle", "vehicles");
            rowMap = new HashMap<>();
            rowMap.put("Color", COLOR + "!");
            rowMap.put("ModelId", resp2.getRows().get(0).get("rowid"));
            rowMap.put("ModelYear", 2050);
            rowMap.put("Milage", 2);
            rowMap.put("LastService", new Date());

            insertCmd3.addRow(rowMap);
            SaveRowsResponse response = insertCmd3.execute(cn, getProjectName());

            Map<String, Object> row = response.getRows().get(0);
            Long rowId = (Long)row.get("RowId");
            return rowId.intValue();
    }

    @Override
    protected void checkQueries()
    {
        //simplemodule has queries for list we didnt import
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}

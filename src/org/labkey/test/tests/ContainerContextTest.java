/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.RReportHelperWD;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class ContainerContextTest extends BaseWebDriverTest
{
    private static final String SUB_FOLDER_A = "A";
    private static final String SUB_FOLDER_B = "B";

    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    private final static String LIST_KEY_NAME = "Key";

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName();
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/query";
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }


    @Override
    protected void doTestSteps() throws Exception
    {
        doSetup();

        doTestListLookupURL();
        doTestIssue15610();
        doTestIssue15751();
        doTestSimpleModuleTables();
    }

    protected void doSetup() throws Exception
    {
        RReportHelperWD rReportHelperWD = new RReportHelperWD(this);
        rReportHelperWD.ensureRConfig();

        _containerHelper.createProject(getProjectName(), null);
        enableModule(getProjectName(), "Laboratory");
        addWebPart("Workbooks");

        createSubfolder(getProjectName(), SUB_FOLDER_A, new String[] {"List", "Study", "ViscStudies"});
        createSubfolder(getProjectName(), SUB_FOLDER_B, new String[]{"List", "Study", "ViscStudies"});
    }

    @LogMethod
    protected void doTestListLookupURL()
    {
        log("** Creating lookup target list in sub-folder");
        goToProjectHome();
        ListHelper.ListColumn[] lookupTargetCols = {
            new ListHelper.ListColumn("LookupName", "LookupName", ListHelper.ListColumnType.String, "Lookup Name"),
            new ListHelper.ListColumn("LookupAge", "LookupAge", ListHelper.ListColumnType.Integer, "Lookup Age", null, null, null, "fake/action.view?key=${Key}")
        };
        String lookupTargetListName = SUB_FOLDER_A + "-LookupTarget-List";
        _listHelper.createList(SUB_FOLDER_A, lookupTargetListName, LIST_KEY_TYPE, LIST_KEY_NAME, lookupTargetCols);

        log("** Insert row into lookup target list");
        goToProjectHome();
        clickFolder(SUB_FOLDER_A);
        clickAndWait(Locator.linkWithText(lookupTargetListName));
        _listHelper.insertNewRow(Maps.<String, String>of(
                "LookupName", "MyLookupItem1",
                "LookupAge", "100"
        ));
        _listHelper.insertNewRow(Maps.<String, String>of(
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
        clickButton("Insert New");
        setFormElement(Locator.name("quf_MyName"), "MyName");
        selectOptionByText(Locator.name("quf_ListLookup"), "MyLookupItem2");
        clickButton("Submit");

        log("** Adding in lookup list columns to grid");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn(new String[] { "ListLookup", "LookupAge" });
        _customizeViewsHelper.saveCustomView();

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.linkWithText("EDIT"), "href");
        Assert.assertTrue("Expected [edit] link to go to " + getProjectName() + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/update.view?"));

        href = getAttribute(Locator.linkWithText("DETAILS"), "href");
        Assert.assertTrue("Expected [details] link to go to " + getProjectName() + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/details.view?"));

        href = getAttribute(Locator.linkWithText("MyName"), "href");
        Assert.assertTrue("Expected MyName link to go to " + getProjectName() + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/details.view?"));

        href = getAttribute(Locator.linkWithText("MyLookupItem2"), "href");
        Assert.assertTrue("Expected ListLookup link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/" + SUB_FOLDER_A + "/details.view?"));

        href = getAttribute(Locator.linkWithText("200"), "href");
        Assert.assertTrue("Expected ListLookup/LookupAge link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container, href=" + href,
                href.contains("/fake/" + getProjectName() + "/" + SUB_FOLDER_A + "/action.view?key=2"));

    }

    // Issue 15610: viscstudieslist - URLs generated from lookups are broken
    @LogMethod
    protected void doTestIssue15610()
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
        clickAndWait(Locator.linkWithText(SUB_FOLDER_B));
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

        log("** Insering row into list");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Issue15610-List"));
        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_StudyLookup"), SUB_FOLDER_A + "-Study");
        clickButton("Submit");

        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_StudyLookup"), SUB_FOLDER_B + "-Study");
        clickButton("Submit");

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.linkWithText(SUB_FOLDER_A + "-Study"), "href");
        Assert.assertTrue("Expected 'MyStudy' link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container: " + href,
                href.contains("/study/" + getProjectName() + "/" + SUB_FOLDER_A + "/studySchedule.view"));

        href = getAttribute(Locator.linkWithText(SUB_FOLDER_B + "-Study"), "href");
        Assert.assertTrue("Expected 'MyStudy' link to go to " + getProjectName() + "/" + SUB_FOLDER_B + " container: " + href,
                href.contains("/study/" + getProjectName() + "/" + SUB_FOLDER_B + "/studySchedule.view"));
    }

    // Issue 15751: Pipeline job list generates URLs without correct container
    @LogMethod
    protected void doTestIssue15751()
    {
        log("** Create pipeline jobs");
        insertJobIntoSubFolder(SUB_FOLDER_A);
        insertJobIntoSubFolder(SUB_FOLDER_B);

        log("** Viewing pipeline status from project container. Sort by Description (report name) and include sub-folders");
        beginAt("/pipeline-status/" + getProjectName() + "/showList.view?StatusFiles.sort=Description&StatusFiles.containerFilterName=CurrentAndSubfolders");

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.linkWithText("COMPLETE", 0), "href");
        Assert.assertTrue("Expected 'COMPLETE' link 0 to go to current A container: " + href,
                href.contains("/pipeline-status/" + getProjectName() + "/" + SUB_FOLDER_A + "/details.view"));

        href = getAttribute(Locator.linkWithText("COMPLETE", 1), "href");
        Assert.assertTrue("Expected 'COMPLETE' link 1 to go to current B container: " + href,
                href.contains("/pipeline-status/" + getProjectName() + "/" + SUB_FOLDER_B + "/details.view"));
    }

    @LogMethod
    protected void insertJobIntoSubFolder(String folder)
    {
        goToProjectHome();

        log("** Creating list in folder '" + folder + "'");
        ListHelper.ListColumn[] cols = {
            new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name")
        };
        String listName = folder + "-Issue15751-List";
        _listHelper.createList(folder, listName, LIST_KEY_TYPE, LIST_KEY_NAME, cols);
        clickButton("Done");

        log("** Creating background R script");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(folder));
        clickAndWait(Locator.linkWithText(listName));
        clickMenuButton("Views", "Create", "R View");
        clickCheckboxById("runInBackground");
        clickButton("Save", 0);
        setFormElement(Locator.xpath("//input[@class='ext-mb-input']"), folder + "-BackgroundReport");
        _extHelper.clickExtButton("Save", 0);
        waitForElement(Locator.id("query"));

        log("** Executing background R script");
        clickMenuButton("Views", folder + "-BackgroundReport");
        waitForElement(Locator.navButton("Start Job"), WAIT_FOR_JAVASCRIPT);
        clickButton("Start Job", 0);
        waitForText("COMPLETE", WAIT_FOR_PAGE);
    }

    @LogMethod
    protected void doTestSimpleModuleTables() throws Exception
    {
        log("** Inserting data into labratory.samples table...");
        int max = 3;

        String[] workbookIds = new String[3];
        String[] sampleIds = new String[3];
        String[] parentSampleIds = new String[3];
        Map<String, String> sampleIdToWorkbookId = new HashMap<String, String>();
        for (int i = 0; i < max; i++)
        {
            String workbookName = "Workbook" + i;
//            LabModuleHelper labModuleHelper = new LabModuleHelper(this);
//            String id = labModuleHelper.createWorkbook(workbookName, "Description");
            String id = createWorkbook(workbookName, "Description");
            workbookIds[i] = id;
            parentSampleIds[i] = i > 0 ? sampleIds[i-1] : null;
            sampleIds[i] = insertLabSample(id, String.valueOf(i), parentSampleIds[i]);
            sampleIdToWorkbookId.put(sampleIds[i], workbookIds[i]);
        }
//        log(" ** For running against this data repeatedly without having to re-rcreate the folders: ");
//        log("String[] workbookIds = new String[] { \"" + StringUtils.join(workbookIds, "\", \"") + "\" };");
//        log("String[] sampleIds = new String[] { \"" + StringUtils.join(sampleIds, "\", \"") + "\" };");
//        log("String[] parentSampleIds = new String[] { \"" + StringUtils.join(parentSampleIds, "\", \"") + "\" };");
//        log("Map<String, String> sampleIdToWorkbookId = new HashMap<String, String>();");
//        for (int i = 0; i < max; i++)
//            log("sampleIdToWorkbookId.put(\"" + sampleIds[i] + "\", \"" + workbookIds[i] + "\");");


        verifySimpleModuleTables("Samples", "detailsQueryRow.view", "detailsQueryRow.view", max, workbookIds, sampleIds, parentSampleIds, sampleIdToWorkbookId, true, true);


        // Verify Issue 16243: Details URL creating URLs with null container unless the container column is actually added to current view
        log("** Removing container column and rehecking lookup URLs...");
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=laboratory&query.queryName=samples&query.sort=RowId");
        CustomizeViewsHelper cv = new CustomizeViewsHelper(this);
        cv.openCustomizeViewPanel();
        cv.removeCustomizeViewColumn("container");
        cv.saveCustomView();

        verifySimpleModuleTables("Samples", "detailsQueryRow.view", "detailsQueryRow.view", max, workbookIds, sampleIds, parentSampleIds, sampleIdToWorkbookId, false, true);


        log("** Override detailsURL in metadata...");
        String customMetadata =
                "<ns:tables xmlns:ns=\"http://labkey.org/data/xml\">\n" +
                "  <ns:table tableName=\"CustomSamples\" tableDbType=\"TABLE\" useColumnOrder=\"true\">\n" +
                "    <ns:tableTitle>Custom Samples</ns:tableTitle>\n" +
                "    <!--<ns:javaCustomizer>org.labkey.ldk.query.BuiltInColumnsCustomizer</ns:javaCustomizer>-->\n" +
                "    <ns:titleColumn>rowid</ns:titleColumn>\n" +
                "    <ns:updateUrl>/query/manageRecord.view?schemaName=laboratory&amp;query.queryName=samples&amp;keyField=rowid&amp;key=${rowid}</ns:updateUrl>\n" +
                "    <ns:tableUrl>/query/XXX.view?schemaName=laboratory&amp;query.queryName=samples&amp;rowid=${rowid}</ns:tableUrl>\n" +
                "    <ns:insertUrl></ns:insertUrl>\n" +
                "    <ns:importUrl>/query/importData.view?schemaName=laboratory&amp;query.queryName=samples&amp;keyField=rowid&amp;key=${rowid}&amp;query.columns=*</ns:importUrl>\n" +
                "  </ns:table>\n" +
                "</ns:tables>";

        overrideMetadata(getProjectName(), "laboratory", "Samples", customMetadata);
        verifySimpleModuleTables("Samples", "XXX.view", "XXX.view", max, workbookIds, sampleIds, parentSampleIds, sampleIdToWorkbookId, false, true);
        removeMetadata(getProjectName(), "laboratory", "Samples");

        
        log("** Create custom query over laboratory.samples table WITH container");
        String customQueryWithContainer =
                "SELECT samples.rowid,\n" +
                "samples.samplename,\n" +
                "samples.subjectid,\n" +
                "samples.sampletype,\n" +
                "samples.samplesource,\n" +
                "samples.parentsample,\n" +
                "samples.container\n" +
                "FROM samples";

        createQuery(getProjectName(), "Samples With Container", "laboratory", customQueryWithContainer, customMetadata, false);
        verifySimpleModuleTables("Samples With Container", "XXX.view", "detailsQueryRow.view", max, workbookIds, sampleIds, parentSampleIds, sampleIdToWorkbookId, true, false);


        log("** Create custom query over laboratory.samples table WITH container AS folder");
        String customQueryFolderContainer =
                "SELECT samples.rowid,\n" +
                "samples.samplename,\n" +
                "samples.subjectid,\n" +
                "samples.sampletype,\n" +
                "samples.samplesource,\n" +
                "samples.parentsample,\n" +
                "samples.container AS folder\n" +
                "FROM samples";

        createQuery(getProjectName(), "Samples With Folder", "laboratory", customQueryFolderContainer, customMetadata, false);
        verifySimpleModuleTables("Samples With Folder", "XXX.view", "detailsQueryRow.view", max, workbookIds, sampleIds, parentSampleIds, sampleIdToWorkbookId, false, false);


        // Container context won't work if the container column is named something other than container or folder.
        /*
        log("** Create custom query over laboratory.samples table WITH RENAMED container");
        String customQueryXXXContainer =
                "SELECT samples.rowid,\n" +
                "samples.samplename,\n" +
                "samples.subjectid,\n" +
                "samples.sampletype,\n" +
                "samples.samplesource,\n" +
                "samples.parentsample,\n" +
                "samples.container AS XXX\n" +
                "FROM samples";

        createQuery(getProjectName(), "Samples XXX Container", "laboratory", customQueryXXXContainer, customMetadata, false);
        verifySimpleModuleTables("Samples XXX Container", "XXX.view", "detailsQueryRow.view", max, workbookIds, sampleIds, parentSampleIds, sampleIdToWorkbookId, false, false);
        */


        log("** Create custom query over laboratory.samples table WITHOUT container.");
        log("** The container column should be added as a suggested column.");
        String customQueryWithoutContainer =
                "SELECT samples.rowid,\n" +
                "samples.samplename,\n" +
                "samples.subjectid,\n" +
                "samples.sampletype,\n" +
                "samples.samplesource,\n" +
                "samples.parentsample\n" +
                "--samples.container\n" +
                "FROM samples";

        createQuery(getProjectName(), "Samples Without Container", "laboratory", customQueryWithoutContainer, customMetadata, false);
        verifySimpleModuleTables("Samples Without Container", "XXX.view", "detailsQueryRow.view", max, workbookIds, sampleIds, parentSampleIds, sampleIdToWorkbookId, false, false);
    }

    private void deleteQuery(String container, String schemaName, String queryName)
    {
        String deleteQueryURL = "query/" + container + "/deleteQuery.view?schemaName=" + schemaName + "&query.queryName=" + queryName;
        beginAt(deleteQueryURL);
        clickButton("OK");
    }

    @Override
    protected void createQuery(String container, String name, String schemaName, String sql, String xml, boolean inheritable)
    {
        deleteQuery(container, schemaName, name);
        super.createQuery(container, name, schemaName, sql, xml, inheritable);
    }

    protected void overrideMetadata(String container, String schemaName, String queryName, String xml)
    {
        beginAt("/query/" + container + "/schema.view?schemaName=" + schemaName + "&queryName=" + queryName);
        waitForText("edit metadata", 10000);
        clickAndWait(Locator.linkWithText("edit metadata"));
        waitForText("Label", 10000);
        clickButton("Edit Source");
        _extHelper.clickExtTab("XML Metadata");
        setQueryEditorValue("metadataText", xml);
        clickButton("Save", 0);
        waitForText("Saved", WAIT_FOR_JAVASCRIPT);
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
            String[] sampleIds,
            String[] parentSampleIds,
            Map<String, String> sampleIdToWorkbookId,
            boolean hasContainer,
            boolean hasUpdate)
    {
        log("** Checking containers on lookup URLs for '" + queryName + "'");
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=laboratory&query.queryName=" + queryName + "&query.sort=RowId");

        DataRegionTable dr = new DataRegionTable("query", this);

        for (int i = 0; i < max; i++)
        {
            String workbookContainer = EscapeUtil.encode(getProjectName()) + "/workbook-" + workbookIds[i];
            String href;
            String expectedHref;

            // update link
            if (hasUpdate)
            {
                href = dr.getUpdateHref(i);
                log("  [edit] column href = " + href);
                expectedHref = "/query/" + workbookContainer + "/manageRecord.view?schemaName=laboratory&query.queryName=samples&keyField=rowid&key=" + sampleIds[i];
                Assert.assertTrue("Expected [edit] link to go to " + expectedHref + ", got href=" + href,
                        href.contains(expectedHref));
            }

            // details link
            href = dr.getDetailsHref(i);
            log("  [details] column href = " + href);
            expectedHref = "/query/" + workbookContainer + "/" + detailsAction;
            Assert.assertTrue("Expected [details] link to go to " + expectedHref + ", got href=" + href,
                    href.contains(expectedHref));

            // sample ID link
            href = dr.getHref(i, "Sample Id");
            log("  Sample Id column href = " + href);
            expectedHref = "/query/" + workbookContainer + "/" + detailsAction + "?schemaName=laboratory&query.queryName=samples&rowid=" + sampleIds[i];
            Assert.assertTrue("Expected Sample Id column URL to go to " + expectedHref + ", got href=" + href,
                    href.contains(expectedHref));

            // parent sample ID link (table has a container so URL should go to lookup's container)
            if (parentSampleIds[i] != null && !parentSampleIds[i].equals(""))
            {
                String parentSampleWorkbookId = sampleIdToWorkbookId.get(parentSampleIds[i]);
                String parentSampleContainer = EscapeUtil.encode(getProjectName()) + "/workbook-" + parentSampleWorkbookId;
                expectedHref = "/query/" + parentSampleContainer + "/" + parentDetailsAction + "?schemaName=laboratory&query.queryName=samples&rowid=" + parentSampleIds[i];

                href = dr.getHref(i, "Parent Sample");
                log("  Parent Sample column href = " + href);
                Assert.assertTrue("Expected parent sample column URL to go to " + expectedHref + ", got href=" + href,
                        href.contains(expectedHref));
            }

            // sample source lookup (table has no container so URL should go to current container)
            href = dr.getHref(i, "Sample Source");
            log("  Sample Source column href = " + href);
            expectedHref = "/query/" + getProjectName() + "/detailsQueryRow.view?schemaName=laboratory&query.queryName=sample_type";
            Assert.assertTrue("Expected sample source column URL to go to " + getProjectName() + " container, got href=" + href,
                    href.contains(expectedHref));

            // sample type lookup (table has no container so URL should go to current container)
            href = dr.getHref(i, "Sample Type");
            log("  Sample Type column href = " + href);
            expectedHref = "/query/" + getProjectName() + "/detailsQueryRow.view?schemaName=laboratory&query.queryName=sample_type";
            Assert.assertTrue("Expected sample type column URL to go to " + getProjectName() + " container, got href=" + href,
                    href.contains(expectedHref));

            // container column
            if (hasContainer)
            {
                href = dr.getHref(i, "Folder");
                log("  Folder column href = " + href);
                expectedHref = "/project/" + workbookContainer + "/begin.view?";
                Assert.assertTrue("Expected container column to go to " + workbookContainer + " container, got href=" + href,
                        href.contains(expectedHref));
            }

            log("");
        }

        log("** Checked containers on lookup URLs for query '" + queryName + "'\n");
    }

    @LogMethod
    private String insertLabSample(String workbookId, String suffix, String parentSampleId)
    {
        try
        {
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

            InsertRowsCommand insertCmd = new InsertRowsCommand("laboratory", "samples");
            Map<String,Object> rowMap = new HashMap<String,Object>();
            rowMap.put("samplename", "Sample" + suffix);
            rowMap.put("freezer", "Freezer" + suffix);
            rowMap.put("sampletype", "DNA");
            rowMap.put("samplesource", "PBMC");
            if (parentSampleId != null)
                rowMap.put("parentsample", parentSampleId);

            insertCmd.addRow(rowMap);
            SaveRowsResponse response = insertCmd.execute(cn, getProjectName() + "/workbook-" + workbookId);
            Map<String, Object> row = response.getRows().get(0);
            Long rowId = (Long)row.get("RowId");
            return rowId.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    // TODO: Use LabModuleHelper.createWorkbook() after merging to trunk
    public String createWorkbook(String workbookTitle, String workbookDescription)
    {
        goToProjectHome();
        clickButton("Create New Workbook", 0);
        waitForElement(Ext4Helper.ext4Window("Create Workbook"));
        setFormElement(Locator.name("title"), workbookTitle);
        setFormElement(Locator.name("description"), workbookDescription);
        clickButton("Submit");
        waitForElement(Locator.css("span.wb-name + span").withText(workbookTitle));

        try
        {
            String path = getURL().toURI().getPath();
            path = path.replaceAll(".*/workbook-", "");
            path = path.replaceAll("/begin.view", "");
            return path;
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }
}

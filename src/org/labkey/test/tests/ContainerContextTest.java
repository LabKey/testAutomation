/*
 * Copyright (c) 2012 LabKey Corporation
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
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class ContainerContextTest extends BaseSeleniumWebTest
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
    protected void doCleanup() throws Exception
    {
        try { deleteProject(getProjectName()) ; } catch (Throwable t) { }
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
        _containerHelper.createProject(getProjectName(), null);
        enableModule(getProjectName(), "Laboratory");
        addWebPart("Workbooks");

        createSubfolder(getProjectName(), SUB_FOLDER_A, new String[] {"List", "Study", "ViscStudies"});
        createSubfolder(getProjectName(), SUB_FOLDER_B, new String[]{"List", "Study", "ViscStudies"});
    }

    protected void doTestListLookupURL()
    {
        log("** Creating lookup target list in sub-folder");
        goToProjectHome();
        ListHelper.ListColumn[] lookupTargetCols = {
            new ListHelper.ListColumn("LookupName", "LookupName", ListHelper.ListColumnType.String, "Lookup Name"),
            new ListHelper.ListColumn("LookupAge", "LookupAge", ListHelper.ListColumnType.Integer, "Lookup Age", null, null, null, "fake/action.view?key=${Key}")
        };
        String lookupTargetListName = SUB_FOLDER_A + "-LookupTarget-List";
        ListHelper.createList(this, SUB_FOLDER_A, lookupTargetListName, LIST_KEY_TYPE, LIST_KEY_NAME, lookupTargetCols);
        clickButton("Done");

        log("** Insert row into lookup target list");
        goToProjectHome();
        clickLinkWithText(SUB_FOLDER_A);
        clickLinkWithText(lookupTargetListName);
        ListHelper.insertNewRow(this, Maps.<String, String>of(
                "LookupName", "MyLookupItem1",
                "LookupAge", "100"
        ));
        ListHelper.insertNewRow(this, Maps.<String, String>of(
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
        ListHelper.createList(this, getProjectName(), lookupSourceListName, LIST_KEY_TYPE, LIST_KEY_NAME, cols);
        clickButton("Done");

        log("** Insert row into list");
        goToProjectHome();
        clickLinkWithText(lookupSourceListName);
        clickButton("Insert New");
        setFormElement("quf_MyName", "MyName");
        selectOptionByText(Locator.name("quf_ListLookup"), "MyLookupItem2");
        clickButton("Submit");

        log("** Adding in lookup list columns to grid");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, new String[] { "ListLookup", "LookupAge" });
        CustomizeViewsHelper.saveCustomView(this);

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.linkWithText("edit"), "href");
        assertTrue("Expected [edit] link to go to " + getProjectName() + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/update.view?"));

        href = getAttribute(Locator.linkWithText("details"), "href");
        assertTrue("Expected [details] link to go to " + getProjectName() + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/details.view?"));

        href = getAttribute(Locator.linkWithText("MyName"), "href");
        assertTrue("Expected MyName link to go to " + getProjectName() + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/details.view?"));

        href = getAttribute(Locator.linkWithText("MyLookupItem2"), "href");
        assertTrue("Expected ListLookup link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/" + SUB_FOLDER_A + "/details.view?"));

        href = getAttribute(Locator.linkWithText("200"), "href");
        assertTrue("Expected ListLookup/LookupAge link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container, href=" + href,
                href.contains("/fake/" + getProjectName() + "/" + SUB_FOLDER_A + "/action.view?key=2"));

    }

    // Issue 15610: viscstudieslist - URLs generated from lookups are broken
    protected void doTestIssue15610()
    {
        log("** Creating study in " + SUB_FOLDER_A);
        goToProjectHome();
        clickLinkWithText(SUB_FOLDER_A);
        goToManageStudy();
        clickButton("Create Study");
        setFormElement(Locator.name("label"), SUB_FOLDER_A + "-Study");
        clickButton("Create Study");

        log("** Creating study in " + SUB_FOLDER_B);
        goToProjectHome();
        clickLinkWithText(SUB_FOLDER_B);
        goToManageStudy();
        clickButton("Create Study");
        setFormElement(Locator.name("label"), SUB_FOLDER_B + "-Study");
        clickButton("Create Study");

        log("** Creating list with lookup to viscstudies.studies");
        ListHelper.ListColumn[] cols = {
            new ListHelper.ListColumn("StudyLookup", "StudyLookup", ListHelper.ListColumnType.String, "Study Lookup", new ListHelper.LookupInfo(null, "viscstudies", "studies")),
        };
        ListHelper.createList(this, getProjectName(), "Issue15610-List", LIST_KEY_TYPE, LIST_KEY_NAME, cols);
        clickButton("Done");

        log("** Insering row into list");
        goToProjectHome();
        clickLinkWithText("Issue15610-List");
        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_StudyLookup"), SUB_FOLDER_A + "-Study");
        clickButton("Submit");

        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_StudyLookup"), SUB_FOLDER_B + "-Study");
        clickButton("Submit");

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.linkWithText(SUB_FOLDER_A + "-Study"), "href");
        assertTrue("Expected 'MyStudy' link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container: " + href,
                href.contains("/study/" + getProjectName() + "/" + SUB_FOLDER_A + "/studySchedule.view"));

        href = getAttribute(Locator.linkWithText(SUB_FOLDER_B + "-Study"), "href");
        assertTrue("Expected 'MyStudy' link to go to " + getProjectName() + "/" + SUB_FOLDER_B + " container: " + href,
                href.contains("/study/" + getProjectName() + "/" + SUB_FOLDER_B + "/studySchedule.view"));
    }

    // Issue 15751: Pipeline job list generates URLs without correct container
    protected void doTestIssue15751()
    {
        log("** Create pipeline jobs");
        insertJobIntoSubFolder(SUB_FOLDER_A);
        insertJobIntoSubFolder(SUB_FOLDER_B);

        log("** Viewing pipeline status from project container. Sort by Description (report name) and include sub-folders");
        beginAt("/pipeline-status/" + getProjectName() + "/showList.view?StatusFiles.sort=Description&StatusFiles.containerFilterName=CurrentAndSubfolders");

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.linkWithText("COMPLETE", 0), "href");
        assertTrue("Expected 'COMPLETE' link 0 to go to current A container: " + href,
                href.contains("/pipeline-status/" + getProjectName() + "/" + SUB_FOLDER_A + "/details.view"));

        href = getAttribute(Locator.linkWithText("COMPLETE", 1), "href");
        assertTrue("Expected 'COMPLETE' link 1 to go to current B container: " + href,
                href.contains("/pipeline-status/" + getProjectName() + "/" + SUB_FOLDER_B + "/details.view"));
    }

    protected void insertJobIntoSubFolder(String folder)
    {
        goToProjectHome();

        log("** Creating list in folder '" + folder + "'");
        ListHelper.ListColumn[] cols = {
            new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name")
        };
        String listName = folder + "-Issue15751-List";
        ListHelper.createList(this, folder, listName, LIST_KEY_TYPE, LIST_KEY_NAME, cols);
        clickButton("Done");

        log("** Creating background R script");
        goToProjectHome();
        clickLinkWithText(folder);
        clickLinkWithText(listName);
        clickMenuButton("Views", "Create", "R View");
        clickCheckboxById("runInBackground");
        clickButton("Save", 0);
        setFormElement(Locator.xpath("//input[@class='ext-mb-input']"), folder + "-BackgroundReport");
        ExtHelper.clickExtButton(this, "Save");

        log("** Executing background R script");
        clickMenuButton("Views", folder + "-BackgroundReport");
        waitForElement(Locator.navButton("Start Job"), WAIT_FOR_JAVASCRIPT);
        clickButton("Start Job", 0);
        waitForText("COMPLETE", WAIT_FOR_PAGE);
    }

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
            String id = createWorkbook(workbookName, "Description");
            workbookIds[i] = id;
            parentSampleIds[i] = i > 0 ? sampleIds[i-1] : null;
            sampleIds[i] = insertLabSample(id, String.valueOf(i), parentSampleIds[i]);
            sampleIdToWorkbookId.put(sampleIds[i], workbookIds[i]);
        }

        log("** Checking containers on lookup URLs...");
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=laboratory&query.queryName=samples&query.sort=RowId");
        waitForPageToLoad();
        DataRegionTable dr = new DataRegionTable("query", this);

        for (int i = 0; i < max; i++)
        {
            String workbookContainer = EscapeUtil.encode(getProjectName()) + "/workbook-" + workbookIds[i];

            // update link
            String href = dr.getUpdateHref(i);
            log("  [edit] column href = " + href);
            Assert.assertTrue("Expected [edit] link to go to " + workbookContainer + " container, got href=" + href,
                    href.contains("/query/" + workbookContainer + "/manageRecord.view?"));

            // details link
            href = dr.getDetailsHref(i);
            log("  [details] column href = " + href);
            Assert.assertTrue("Expected [details] link to go to " + workbookContainer + " container, got href=" + href,
                    href.contains("/query/" + workbookContainer + "/recordDetails.view?"));

            // sample ID link
            href = dr.getHref(i, "Sample Id");
            log("  Sample Id column href = " + href);
            String expectedHref = "/query/" + workbookContainer + "/recordDetails.view?schemaName=laboratory&query.queryName=Samples&keyField=rowid&key=" + sampleIds[i];
            Assert.assertTrue("Expected Sample Id column URL to go to " + expectedHref + ", got href=" + href,
                    href.contains(expectedHref));

            // parent sample ID link (table has a container so URL should go to lookup's container)
            if (parentSampleIds[i] != null)
            {
                String parentSampleWorkbookId = sampleIdToWorkbookId.get(parentSampleIds[i]);
                String parentSampleContainer = EscapeUtil.encode(getProjectName()) + "/workbook-" + parentSampleWorkbookId;
                expectedHref = "/query/" + parentSampleContainer + "/recordDetails.view?schemaName=laboratory&query.queryName=Samples&keyField=rowid&key=" + parentSampleIds[i];

                href = dr.getHref(i, "Parent Sample");
                log("  Parent Sample column href = " + href);
                Assert.assertTrue("Expected parent sample column URL to go to " + expectedHref + ", got href=" + href,
                        href.contains(expectedHref));
            }

            // sample source lookup (table has no container so URL should go to current container)
            href = dr.getHref(i, "Sample Source");
            log("  Sample Source column href = " + href);
            Assert.assertTrue("Expected sample source column URL to go to " + getProjectName() + " container, got href=" + href,
                    href.contains("/query/" + getProjectName() + "/detailsQueryRow.view?schemaName=laboratory&query.queryName=sample_source"));

            // sample type lookup (table has no container so URL should go to current container)
            href = dr.getHref(i, "Sample Type");
            log("  Sample Type column href = " + href);
            Assert.assertTrue("Expected sample type column URL to go to " + getProjectName() + " container, got href=" + href,
                    href.contains("/query/" + getProjectName() + "/detailsQueryRow.view?schemaName=laboratory&query.queryName=sample_type"));

            // container column
            href = dr.getHref(i, "Folder");
            log("  Folder column href = " + href);
            Assert.assertTrue("Expected container column to go to " + workbookContainer + " container, got href=" + href,
                    href.contains("/project/" + workbookContainer + "/begin.view?"));

            log("");
        }

        log("** Checked containers on lookup URLs.");
    }

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
        setText("title", workbookTitle);
        setText("description", workbookDescription);
        clickButton("Submit");
        waitForPageToLoad();

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

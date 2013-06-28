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
package org.labkey.test.module;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.join;


public class OConnorExperimentTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "OConnor Experiment Project";
    private static final String MODULE_NAME = "OConnorExperiments";
    private static final String SCHEMA_NAME = MODULE_NAME;
    private static final String QUERY_NAME = "Experiments";
    private static final String TABLE_NAME = "Experiments";
    private PortalHelper portalHelper = new PortalHelper(this);
    private ArrayList<String> workbookids = new ArrayList<String>();
    private ArrayList<String> pkeys = new ArrayList<>();

    @Nullable
    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        enableModule(PROJECT_NAME, MODULE_NAME);

        // Customize the default view for the Experiments table
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=" + SCHEMA_NAME + "&query.queryName=" + QUERY_NAME);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("Created");
        _customizeViewsHelper.addCustomizeViewColumn("Modified");
        _customizeViewsHelper.addCustomizeViewColumn("FolderType");
        _customizeViewsHelper.addCustomizeViewColumn(new String[] { "Container", "EntityId" });
        _customizeViewsHelper.saveDefaultView();

        // Customize the default view for the Workbooks table
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=core&query.queryName=Workbooks");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("FolderType");
        _customizeViewsHelper.addCustomizeViewColumn("EntityId");
        _customizeViewsHelper.saveDefaultView();

        // Add the webparts to the portal page
        goToProjectHome();
        portalHelper.addQueryWebPart("Query", SCHEMA_NAME, QUERY_NAME, null);
        addWebPart("Workbooks");

        log("setup complete");
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        doSetup();

        String entityId3 = insertViaGenericQueryForm();
        // update the thrid Experiment (Experiment Number is a container scoped sequence in the current project)
        updateViaGenericQueryForm(entityId3, "3");
        deleteViaQueryWebPart();

        insertViaQueryWebPart();
        //uploadFileUpdatesModified();
        //editWikiUpdatesModified();

        //TODO: enable this test once bug 17931 is fixed
        //insertViaWorkbook();
        updateViaWorkbook();
        deleteViaWorkbook();
        insertViaJavaApi();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected String insertViaGenericQueryForm()
    {
        log("starting insertViaQueryWebPart test");

        beginAt("/query/" + getProjectName() + "/insertQueryRow.view?schemaName=" + SCHEMA_NAME + "&query.queryName=" + QUERY_NAME);
        waitForElement(Locator.name("quf_Description"));
        setFormElement(Locator.name("quf_Description"), "description1");
        setFormElement(Locator.name("quf_ExperimentType"), "type1");
        clickButton("Submit");

        beginAt("/query/" + getProjectName() + "/insertQueryRow.view?schemaName=" + SCHEMA_NAME + "&query.queryName=" + QUERY_NAME);
        waitForElement(Locator.name("quf_Description"));
        setFormElement(Locator.name("quf_Description"), "description2");
        setFormElement(Locator.name("quf_ExperimentType"), "type2");
        selectOptionByText(Locator.name("quf_ParentExperiments"), "1");
        clickButton("Submit");

        beginAt("/query/" + getProjectName() + "/insertQueryRow.view?schemaName=" + SCHEMA_NAME + "&query.queryName=" + QUERY_NAME);
        waitForElement(Locator.name("quf_Description"));
        setFormElement(Locator.name("quf_Description"), "description3");
        setFormElement(Locator.name("quf_ExperimentType"), "type3");
        selectOptionByText(Locator.name("quf_ParentExperiments"), "1");
        selectOptionByText(Locator.name("quf_ParentExperiments"), "2");
        clickButton("Submit");

        goToProjectHome();

        // Verify the Experiment is inserted
        DataRegionTable q_table = new DataRegionTable("qwp1", this);
        int row3 = q_table.getRow("Description", "description3");
        Assert.assertTrue("Expected to find row for 'description3'", row3 != -1);
        Assert.assertEquals("3", q_table.getDataAsText(row3, "ExperimentNumber"));
        Assert.assertEquals("type3", q_table.getDataAsText(row3, "ExperimentType"));
        Assert.assertEquals("OConnorExperiment", q_table.getDataAsText(row3, "FolderType"));
        String parentExperiments = q_table.getDataAsText(row3, "ParentExperiments");
        Assert.assertEquals("Expected Parent Experiments to be '1, 2'; got '" + parentExperiments + "'", "1, 2", parentExperiments);

        String entityId = q_table.getDataAsText(row3, "EntityId");

        // Make sure each component of the ParentExperiments column is rendered with a link to the begin page for that experiment
        Locator.XPathLocator l = q_table.xpath(row3, q_table.getColumn("ParentExperiments"));
        for (int i = 1; i < 3; i++)
        {
            Locator.XPathLocator link = l.child("a[" + i + "]");
            String parentExpText = getText(link);
            String parentExpHref = getAttribute(link, "href");
            Assert.assertTrue("Expected link to go to project begin for " + parentExpText + ", got: " + parentExpHref,
                    parentExpHref.contains("/" + parentExpText + "/begin.view"));
        }

        checkQueryAndWorkbook();

        return entityId;
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void updateViaGenericQueryForm(String entityId, String experimentNumber)
    {


        beginAt("/query/" + getProjectName() + "/" + experimentNumber + "/updateQueryRow.view?" +
                "schemaName=" + SCHEMA_NAME + "&query.queryName=" + QUERY_NAME + "&Container=" + entityId);

        waitForElement(Locator.name("quf_Description"));
        String description = getFormElement(Locator.name("quf_Description"));
        setFormElement(Locator.name("quf_Description"), description + " edited");

        // UNDONE: verify ParentExperiments are selected correctly
        // Issue 17985: MultiValueFK update form doesn't select/highlight junction key values
        //List<String> selectedValues = getSelectedOptionValues(Locator.name("quf_ParentExperiments"));
        //Assert.assertEquals(2, selectedValues.size());

        selectOptionByText(Locator.name("quf_ParentExperiments"), "1");
        selectOptionByText(Locator.name("quf_ParentExperiments"), "3");

        clickButton("Submit");

        // Verify the Experiment is updated
        DataRegionTable q_table = new DataRegionTable("query", this);
        int row = q_table.getRow("Description", description + " edited");
        Assert.assertTrue("Expected to find row for '" + description + " edited'", row != -1);
        String parentExperiments = q_table.getDataAsText(row, "ParentExperiments");
        Assert.assertEquals("Expected Parent Experiments to be '1, 3'; got '" + parentExperiments + "'", "1, 3", parentExperiments);

        goToProjectHome();
        checkQueryAndWorkbook();
    }

    // Pre-condition: ExperimentNumber 1 and 3 have already been inserted.
    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void insertViaQueryWebPart()
    {
        log("starting insertViaQueryWebPart test");
        goToProjectHome();

        clickButtonByIndex("Insert New", 0);
        setEditInPlaceContent(Locator.id("Description"), "description4");
        Assert.assertEquals("description4", getText(Locator.id("Description")));
        setEditInPlaceContent(Locator.id("ExperimentType"), "type4");
        Assert.assertEquals("type4", getText(Locator.id("ExperimentType")));

        setEditInPlaceContent(Locator.id("ParentExperiments"), "1,100");
        assertTextPresent("100 is not a valid Experiment Number");
        setEditInPlaceContent(Locator.id("ParentExperiments"), "1,3");
        Assert.assertEquals("1,3", getText(Locator.id("ParentExperiments")));

        goToProjectHome();

        DataRegionTable q_table = new DataRegionTable("qwp1", this);
        int row = q_table.getRow("Description", "description4");
        Assert.assertEquals("type4", q_table.getDataAsText(row, "ExperimentType"));
        Assert.assertEquals("1, 3", q_table.getDataAsText(row, "ParentExperiments"));
    }

    protected void setEditInPlaceContent(Locator.XPathLocator l, String text)
    {
        waitForElement(l);
        click(l);

        Locator.XPathLocator input = l.parent().child("textarea");
        if (!isElementPresent(input))
            input = l.parent().child("input");

        WebElement el = input.findElement(getDriver());
        el.sendKeys(text);
        fireEvent(el, SeleniumEvent.blur);
        waitForElement(l);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void deleteViaQueryWebPart()
    {
        goToProjectHome();

        DataRegionTable q_table = new DataRegionTable("qwp1", this);
        q_table.checkCheckbox(1);
        prepForPageLoad();
        q_table.clickHeaderButtonByText("Delete");
        assertAlertContains("Are you sure you want to delete the selected row?");
        newWaitForPageToLoad();
        checkQueryAndWorkbook();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void insertViaWorkbook()
    {
        log("starting insertViaWorkbook test") ;
        goToProjectHome();
        clickButtonByIndex("Insert New", 1);
        waitForElement(Locator.id("workbookTitle"));
        setFormElement(Locator.id("workbookTitle"), "title");
        setFormElement(Locator.id("workbookDescription"),"description");
        selectOptionByText(Locator.id("workbookFolderType"), "Default Workbook");
        clickButton("Create Workbook");
        goToProjectHome();
        checkQueryAndWorkbook();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void updateViaWorkbook()
    {
        goToProjectHome();
        click(Locator.xpath("//table[@id='dataregion_query']/tbody/tr/td/a[contains(text(), \"edit\")]"));
        waitForElement(Locator.name("quf_Description"));
        setFormElement(Locator.name("quf_Description"), "description1 edited again");
        clickButton("Submit");
        goToProjectHome();
        checkQueryAndWorkbook();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void deleteViaWorkbook()
    {
        goToProjectHome();
        DataRegionTable w_table = new DataRegionTable("query", this);
        w_table.checkCheckbox(0);
        prepForPageLoad();
        w_table.clickHeaderButtonByText("Delete");
        assertAlertContains("Are you sure you want to delete the selected row?");
        newWaitForPageToLoad();
        checkQueryAndWorkbook();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void insertViaJavaApi()
    {
        log("** Inserting via api...");
        try
        {
            Map<String,Object> rowMap;
            rowMap = new HashMap<>();
            rowMap.put("Description", "API Description");
            rowMap.put("ExperimentType", "API Type");
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
            InsertRowsCommand insertCmd = new InsertRowsCommand(SCHEMA_NAME, TABLE_NAME);
            insertCmd.addRow(rowMap);;
            SaveRowsResponse resp = insertCmd.execute(cn, getProjectName());
            String[] pks = new String[insertCmd.getRows().size()];
            for (int i = 0; i < insertCmd.getRows().size(); i++)
            {
                Map<String, Object> row = resp.getRows().get(i);
                Assert.assertTrue(row.containsKey("container"));
                pks[i] = ((String)row.get("container")).toString();
            }
            for (int i = 0; i < pks.length; i++)
            {
                pkeys.add(0, pks[i]);
            }
        }
        catch (CommandException e)
        {
            log("CommandException: " + e.getMessage());
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            log("IOException: " + e.getMessage());
            throw new RuntimeException(e);
        }
        goToProjectHome();
        checkQueryAndWorkbook();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void updateViaJavaApi()
    {
        log("** Updating via api...");
        try
        {
            UpdateRowsCommand cmd = new UpdateRowsCommand(SCHEMA_NAME, TABLE_NAME);
            Map<String,Object> rowMap;
            rowMap = new HashMap<>();
            rowMap.put("Description", "API Description Edited");
            rowMap.put("ExperimentType", "API Type Edited");
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
            SaveRowsResponse resp = cmd.execute(cn, getProjectName());
            Assert.assertEquals("Expected to update " + rowMap.size() + " rows", rowMap.size(), resp.getRowsAffected().intValue());
            goToProjectHome();
            checkQueryAndWorkbook();
        }
        catch (CommandException e)
        {
            log("CommandException: " + e.getMessage());
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            log("IOException: " + e.getMessage());
            throw new RuntimeException(e);
        }
        goToProjectHome();
        checkQueryAndWorkbook();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void deleteViaJavaApi()
    {
        log("** Deleting via api: pks=" + join(",", pkeys) + "...");
        try
        {
            DeleteRowsCommand cmd = new DeleteRowsCommand(SCHEMA_NAME, TABLE_NAME);
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
            for (String pk : pkeys)
                cmd.addRow(Collections.singletonMap("container", (Object) pk));

            SaveRowsResponse resp = cmd.execute(cn, getProjectName());
            Assert.assertEquals("Expected to delete " + pkeys.size() + " rows", pkeys.size(), resp.getRowsAffected().intValue());

            SelectRowsCommand selectCmd = new SelectRowsCommand(SCHEMA_NAME, TABLE_NAME);
            selectCmd.addFilter("RowId", join(";", pkeys), Filter.Operator.IN);
            SelectRowsResponse selectResp = selectCmd.execute(cn, getProjectName());
            Assert.assertEquals("Expected to select 0 rows", 0, selectResp.getRowCount().intValue());
        }
        catch (CommandException e)
        {
            log("CommandException: " + e.getMessage());
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            log("IOException: " + e.getMessage());
            throw new RuntimeException(e);
        }
        goToProjectHome();
        checkQueryAndWorkbook();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void checkQueryAndWorkbook()
    {
        //since we have started with a clean o'connor project, both the workbook and query webparts should remain synchronized
        List<List<String>> wb_vals = getColumnValues("query", "Description", "Created", "Created By");
        List<String> wb_descriptions = wb_vals.get(0);
        List<String> wb_createds = wb_vals.get(1);
        List<String> wb_createdbys = wb_vals.get(2);

        List<List<String>> q_vals = getColumnValues("qwp1", "Description", "Created", "Created By");
        List<String> q_descriptions = q_vals.get(0);
        List<String> q_createds = q_vals.get(1);
        List<String> q_createdbys = q_vals.get(2);

        assert(wb_descriptions.size() == q_descriptions.size());
        assert(wb_createdbys.size() == q_createdbys.size());
        assert(wb_createds.size() == q_createds.size());

        for(int i=0; i < wb_descriptions.size(); i++)
        {
            assert(wb_descriptions.contains(q_descriptions.get(i)));
            assert(wb_createdbys.contains(q_descriptions.get(i)));
            assert(wb_createds.contains(q_createds.get(i)));
        }
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/OConnorExperiments";
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}


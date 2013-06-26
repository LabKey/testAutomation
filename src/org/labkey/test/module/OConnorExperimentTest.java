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
    private static final String QUERY = "Experiments";
    private static final String TABLE_NAME = "Experiments";
    private PortalHelper portalHelper = new PortalHelper(this);
    private ArrayList<String> workbookids = new ArrayList<String>();
    private ArrayList<String> pkeys = new ArrayList<>();
    DataRegionTable q_table;
    DataRegionTable w_table;

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

        portalHelper.addQueryWebPart("Query", SCHEMA_NAME, QUERY, null);
        //click(Locator.xpath("//table[@id='dataregion_qwp2']//a[@class='labkey-menu-button']/span[contains(text(), 'Views')]"));
        //click(Locator.xpath("//a[@id='qwp2:Views:Customize View']"));
        //waitForElement(Locator.xpath("//div[@fieldkey='Created']/a/span[contains(text(), 'Created')]"));
        //click(Locator.xpath("//div[@fieldkey='Created']/a/span[contains(text(), 'Created')]"));
        //click(Locator.xpath("//button[contains(text(), 'Save')]"));
        //waitForAlert("Save Custom View", 5000);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Created");
        //_customizeViewsHelper.applyCustomView();
        _customizeViewsHelper.saveDefaultView();
        addWebPart("Workbooks");
        q_table = new DataRegionTable("qwp1", this);
        w_table = new DataRegionTable("query", this);
        log("setup complete");
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        doSetup();
        insertViaQueryWebPart();
        updateViaQueryWebPart();
        deleteViaQueryWebPart();
        //TODO: enable this test once bug 17931 is fixed
        //insertViaWorkbook();
        updateViaWorkbook();
        deleteViaWorkbook();
        insertViaJavaApi();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void insertViaQueryWebPart()
    {
        log("starting insertViaQueryWebPart test");
        goToProjectHome();

        clickButtonByIndex("Insert New", 0);
        waitForElement(Locator.name("quf_Description"));
        setFormElement(Locator.name("quf_Description"), "description1");
        setFormElement(Locator.name("quf_ExperimentType"), "type1");
        clickButton("Submit");
        goToProjectHome();
        clickButtonByIndex("Insert New", 0);
        waitForElement(Locator.name("quf_Description"));
        setFormElement(Locator.name("quf_Description"), "description2");
        setFormElement(Locator.name("quf_ExperimentType"), "type2");
        selectOptionByText(Locator.name("quf_ParentExperiments"), "1");
        clickButton("Submit");
        checkQueryAndWorkbook();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void updateViaQueryWebPart()
    {
        goToProjectHome();
        click(Locator.xpath("//table[@id='dataregion_qwp1']/tbody/tr/td/a[contains(text(), \"edit\")]"));
        waitForElement(Locator.name("quf_Description"));
        setFormElement(Locator.name("quf_Description"), "description1 edited");
        clickButton("Submit");
        goToProjectHome();
        checkQueryAndWorkbook();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void deleteViaQueryWebPart()
    {
        goToProjectHome();
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}


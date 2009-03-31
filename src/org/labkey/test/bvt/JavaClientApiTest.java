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

import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.*;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.ListHelper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;

/*
* User: Dave
* Date: Mar 30, 2009
* Time: 1:53:45 PM
*/

/**
 * Test for the Java Client API library. This test is written in
 * Selenium because we don't yet have a way to create a list via
 * the API, so this test will setup a list and then use the Java
 * client API library to insert, read, update, and delete from that list
 */
public class JavaClientApiTest extends BaseSeleniumWebTest
{
    public static final String PROJECT_NAME = "~Java Client Api Verify Project~";
    public static final String LIST_NAME = "People";

    protected void doTestSteps() throws Exception
    {
        log("Starting Java client api library test...");
        createProject(PROJECT_NAME);
        doQueryTest();

        log("Finished Java client api library test.");
    }

    protected void doQueryTest() throws Exception
    {
        log("Starting query portion of test...");
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Lists");

        log("Creating list for Query test...");

        ListHelper.createList(this, PROJECT_NAME, LIST_NAME,
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("FirstName", "First Name", ListHelper.ListColumnType.String, "First Name"),
                new ListHelper.ListColumn("LastName", "Last Name", ListHelper.ListColumnType.String, "Last Name"),
                new ListHelper.ListColumn("Birthdate", "Birthdate", ListHelper.ListColumnType.DateTime, "Birthdate"),
                new ListHelper.ListColumn("GooAmount", "Goo Amount", ListHelper.ListColumnType.Double, "Amount of Goo"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"));

        log("Setting permissions...");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Permissions");
        setPermissions("Guests", "Editor");

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(LIST_NAME);
        doCRUDtTest();

        log("Finished query portion of test.");
    }

    protected void doCRUDtTest() throws Exception
    {
        log("Starting CRUD test...");
        log("Inserting a new record into that list...");
        Connection cn = new Connection(getBaseURL());

        //insert a row
        Date now = new Date();
        InsertRowsCommand insertCmd = new InsertRowsCommand("lists", LIST_NAME);
        Map<String,Object> rowMap = new HashMap<String,Object>();
        rowMap.put("FirstName", "first to be inserted");
        rowMap.put("LastName", "last to be inserted");
        rowMap.put("Birthdate", now);
        rowMap.put("GooAmount", 4.2);
        insertCmd.addRow(rowMap);
        SaveRowsResponse saveResp = insertCmd.execute(cn, PROJECT_NAME);
        assertTrue(1 == saveResp.getRowsAffected().intValue());

        //get new key value
        Number newKey = (Number)saveResp.getRows().get(0).get("Key");
        assertTrue(null != newKey);
        int key = newKey.intValue();

        //verify row was inserted and data comes back the same
        SelectRowsCommand selectCmd = new SelectRowsCommand("lists", LIST_NAME);
        SelectRowsResponse selResp = selectCmd.execute(cn, PROJECT_NAME);
        assertTrue(1 == selResp.getRowCount().intValue());
        rowMap = selResp.getRows().get(0);
        assertTrue("first to be inserted".equals(rowMap.get("FirstName")));
        assertTrue("last to be inserted".equals(rowMap.get("LastName")));
        assertTrue(rowMap.get("Birthdate") instanceof Date);
        assertTrue(new Double(4.2).equals(rowMap.get("GooAmount")));

        //refresh the list in the browser and make sure it appears there too
        refresh();
        assertTextPresent("first to be inserted");
        assertTextPresent("last to be inserted");

        //update the record
        log("Updating the record...");
        UpdateRowsCommand updateCmd = new UpdateRowsCommand("lists", LIST_NAME);
        rowMap = new HashMap<String,Object>();
        rowMap.put("Key", key);
        rowMap.put("firstname", "UPDATED first name"); //testing for case-insensitivity
        rowMap.put("gooamount", 5.5);
        updateCmd.addRow(rowMap);
        saveResp = updateCmd.execute(cn, PROJECT_NAME);
        assertTrue(1 == saveResp.getRowsAffected().intValue());

        //verify that row was updated
        selectCmd.addFilter("Key", key, Filter.Operator.EQUAL);
        selResp = selectCmd.execute(cn, PROJECT_NAME);
        rowMap = selResp.getRows().get(0);
        assertTrue("UPDATED first name".equals(rowMap.get("FirstName")));
        assertTrue(new Double(5.5).equals(rowMap.get("GooAmount")));

        //verify that it's updated in the browser as well
        refresh();
        assertTextPresent("UPDATED first name");

        //delete the record
        log("Deleting the record...");
        DeleteRowsCommand deleteCmd = new DeleteRowsCommand("lists", LIST_NAME);
        rowMap = new HashMap<String,Object>();
        rowMap.put("Key", key);
        deleteCmd.addRow(rowMap);
        deleteCmd.execute(cn, PROJECT_NAME);

        //verify it was deleted
        selResp = selectCmd.execute(cn, PROJECT_NAME);
        assertTrue(0 == selResp.getRowCount().intValue());

        //verify in browser as well
        refresh();
        assertTextNotPresent("UPDATED first name");

        log("Completed CRUD test...");
    }

    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch(Throwable ignore) {}
    }

    public String getAssociatedModuleDirectory()
    {
        return "experiment";
    }
}
/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.PasswordUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category({DailyA.class})
public class SecondaryAuthenticationTest extends BaseWebDriverTest
{
    @Test
    /* This test assumes that the Duo 2-Factor is Disabled */
    public void testSecondaryAuthentication()
    {
        Date currentDate = new Date(); //get today's date

        //Enable 'Test Secondary Authentication Provider'
        enableSecondaryAuthentication();

        /** Test audit log **/

        //get all the rows that are greater than or equal to today's date
        SelectRowsResponse selectRowsResponse = getLatestAuditEntries();

        Map<String, Object> row = selectRowsResponse.getRows().get(0);

        String commentColVal = (String) row.get("Comment"); //get a value from Comment column
        Date auditDate = (Date)row.get("Created"); //get a value from Created ('Date' in the UI) column

        //compare time stamp of the audit log
        assertFalse("No audit entry for enabled Secondary Authentication", auditDate.before(DateUtils.truncate(currentDate, Calendar.SECOND)));

        //compare 'Comment' value of the last/latest audit log
        assertEquals("Latest audit log for Authentication provider should read: Test Secondary Authentication provider was enabled",
                "Test Secondary Authentication provider was enabled", commentColVal);

        //Sign Out
        signOut();

        currentDate= new Date();

        //URL before User Signs In
        String relativeURLBeforeSignIn = getCurrentRelativeURL();

            //Sign In - Primary Authentication before Secondary Authentication
            attemptSignIn(PasswordUtil.getUsername(), PasswordUtil.getPassword());

            /* Secondary Authentication */

            //'Sign In' link shouldn't be present
            waitForElementToDisappear(Locator.linkContainingText("Sign In"));

            //User should be still recognized as guest until secondary authentication is successful.
            assertTextPresent("Is " + PasswordUtil.getUsername() +" really you?");

            //Select Radio button No
            checkRadioButton(Locator.radioButtonByNameAndValue("valid", "0"));
            click(Locator.input("TestSecondary"));

            //should stay on Secondary Authentication page until user selects Yes radio
            assertTextPresent("Secondary Authentication");

            //Select radio Yes
            checkRadioButton(Locator.radioButtonByNameAndValue("valid", "1"));

            //Click on button 'TestSecondary'
            clickAndWait(Locator.input("TestSecondary"));

            //get current relative URL after Sign In
            String relativeURLAfterSignIn = getCurrentRelativeURL();

            //user should be redirected to the same URL they were on, before Sign In.
            assertEquals("After successful secondary authentication, user should be redirected to the same URL they were on before Sign In",
                    relativeURLBeforeSignIn, relativeURLAfterSignIn);

        //Disable 'Test Secondary Authentication Provider'
        disableSecondaryAuthentication();

         /** Test audit log **/

        selectRowsResponse = getLatestAuditEntries();
        row = selectRowsResponse.getRows().get(0);

        commentColVal = (String) row.get("Comment"); //get a value from Comment column
        auditDate = (Date)row.get("Created"); //get a value from Created ('Date' in the UI) column

        //compare time stamp of the audit log
        assertFalse("No audit entry for disabled Secondary Authentication", auditDate.before(DateUtils.truncate(currentDate, Calendar.SECOND)));

        //compare 'Comment' value of the last/latest audit log
        assertEquals("Latest audit log for Authentication provider should read: Test Secondary Authentication provider was disabled",
                "Test Secondary Authentication provider was disabled", commentColVal);

    }

    protected SelectRowsResponse getLatestAuditEntries()
    {
        Connection cn = createDefaultConnection(true);
        SelectRowsCommand selectCmd = new SelectRowsCommand("auditLog", "AuthenticationProviderConfiguration");
        selectCmd.setSorts(Arrays.asList(new Sort("Created", Sort.Direction.DESCENDING)));
        selectCmd.setMaxRows(1);
        selectCmd.setColumns(Arrays.asList("*"));

        SelectRowsResponse selectResp = null;
        try
        {
            selectResp = selectCmd.execute(cn, "/");
        }
        catch (CommandException | IOException e)
        {
            throw new RuntimeException(e);
        }

        return selectResp;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
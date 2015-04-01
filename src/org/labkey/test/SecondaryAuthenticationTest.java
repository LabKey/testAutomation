package org.labkey.test;

/**
 * Created by Binal Patel on 3/18/15.
 */

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.PasswordUtil;

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

        //Sign In - Primary Authentication
        attemptSignIn(PasswordUtil.getUsername(), PasswordUtil.getPassword());

        //Secondary Authentication

            //'Sign In' link shouldn't be present
            assertElementNotPresent(Locator.linkContainingText("Sign In"));

            //User should be still recognized as guest until secondary authentication is successful.
            assertTextPresent("Is guest really you?");

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

        /* Disable Test Secondary Authentication */

        //Disable 'Test Secondary Authentication Provider'
        disableSecondaryAuthentication();

        /** Test audit log - same as above **/

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
        catch (Exception e)
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
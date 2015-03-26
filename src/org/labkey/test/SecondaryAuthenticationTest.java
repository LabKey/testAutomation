package org.labkey.test;

/**
 * Created by Binal Patel on 3/18/15.
 */

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.Row;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.PasswordUtil;
import org.testng.Assert;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


@Category({InDevelopment.class})
public class SecondaryAuthenticationTest extends BaseWebDriverTest
{

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        SecondaryAuthenticationTest init = (SecondaryAuthenticationTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "Secondary Authentication");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    /* This test assumes that the Duo 2-Factor is Disabled */
    public void testSecondaryAuthentication()
    {
        Date date = new Date(); //get today's date

        //Sign In
        signIn();

        //Go to Admin
        goToAdminConsole();

        //Go to Authenticate
        clickAndWait(Locator.linkWithText("authentication"));

        //Enable 'Test Secondary Authentication Provider'
        click(Locator.linkWithHref("/labkey/login/enable.view?name=Test%20Secondary%20Authentication"));

        //Click Done
        clickAndWait(Locator.linkWithSpan("Done"));

            /** Test audit log **/

            //create a filter for dates that are greater than equal to today's date
            Filter dateFilter = new Filter("Date", date, Filter.Operator.DATE_GTE);
            List<Filter> dateFilterList = new LinkedList<>();
            dateFilterList.add(dateFilter);

            //get all the rows that are greater than or equal to today's date
            SelectRowsResponse selectRowsResponse = executeSelectRowCommand("auditLog", "AuthenticationProviderConfiguration", null, null, dateFilterList);

            for (Row row : selectRowsResponse.getRowset())
            {
                String commentColVal = (String) row.getValue("Comment"); //get a value from Comment column
                Date dateTime = (Date)row.getValue("Created"); //get a value from Created ('Date' in the UI) column

                //compare time stamp of the audit log
                if(dateTime.after(date))
                {
                    //compare 'Comment' value of the last/latest audit log
                    Assert.assertEquals("Test Secondary Authentication provider was enabled", commentColVal,
                            "Latest audit log for Authentication provider should read: Test Secondary Authentication provider was enabled");
                    break;
                }
            }

        //Sign Out
        signOut();

        date = new Date();

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
            click(Locator.input("TestSecondary"));

            //get current relative URL after Sign In
            String relativeURLAfterSignIn = getCurrentRelativeURL();

            //user should be redirected to the same URL they were on, before Sign In.
            Assert.assertEquals(relativeURLBeforeSignIn, relativeURLAfterSignIn,
                    "After successful secondary authentication, user should be redirected to the same URL they were on before Sign In");

        /* Disable Test Secondary Authentication */

        goToAdminConsole();

        //Go to Authenticate
        clickAndWait(Locator.linkWithText("authentication"));

        //Disable 'Test Secondary Authentication Provider'
        click(Locator.linkWithHref("/labkey/login/disable.view?name=Test%20Secondary%20Authentication"));

            /** Test audit log - same as above **/

            dateFilter = new Filter("Date", date, Filter.Operator.DATE_GTE);
            dateFilterList = new LinkedList<>();
            dateFilterList.add(dateFilter);

            selectRowsResponse = executeSelectRowCommand("auditLog", "AuthenticationProviderConfiguration", null, null, dateFilterList);

            for (Row row : selectRowsResponse.getRowset())
            {
                String commentColVal = (String) row.getValue("Comment");
                Date dateTime = (Date)row.getValue("Created");

                if(dateTime.after(date))
                {
                    Assert.assertEquals("Test Secondary Authentication provider was disabled", commentColVal,
                            "Latest audit log for Authentication provider should read: Test Secondary Authentication provider was disabled");
                    break;
                }
            }

        signOut();
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "SecondaryAuthenticationTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
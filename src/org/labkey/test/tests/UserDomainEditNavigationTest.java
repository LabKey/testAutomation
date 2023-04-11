package org.labkey.test.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


    /*
        adds regression coverage for Issue 46738
        explicitly navigate between admin console, site users and user domain editor
    */
@Category({Daily.class})
public class UserDomainEditNavigationTest extends BaseWebDriverTest
{

    // explicitly override cleanup to prevent base class implementation from trying to clean up a project this test never creates
    @Override
    public void cleanup(){}

    @Test
    public void testNavigateFromAdminConsole()
    {
        var console = goToAdminConsole();
        var userEditPage = console.clickChangeUserProperties();
        var adminConsoleUrl = getURL();
        checker().wrapAssertion(()-> assertThat(userEditPage.fieldsPanel().fieldNames())
                .as("expect any standard user fields to be present")
                .contains("FirstName", "LastName", "Description"));
        userEditPage.clickCancel();
        checker().verifyEquals("expect redirect back to admin console",
                adminConsoleUrl, getURL());
    }

    @Test
    public void testNavigateFromSiteUsers()
    {
        var siteUsers = goToSiteUsers();
        var siteUsersURL = getURL();
        var usersPropertyPage = siteUsers.clickChangeUserProperties();
        checker().wrapAssertion(()-> assertThat(usersPropertyPage.fieldsPanel().fieldNames())
                .as("expect any standard user fields to be present")
                .contains("FirstName", "LastName", "Description"));
        usersPropertyPage.clickCancel();
        checker().verifyEquals("expect redirect back to site users",
                siteUsersURL, getURL());
    }

    @Override
    protected String getProjectName()
    {
        return "UserDomainEditNavigationTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}

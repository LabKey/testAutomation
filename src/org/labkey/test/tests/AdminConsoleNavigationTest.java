package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Git;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({Git.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class AdminConsoleNavigationTest extends BaseWebDriverTest
{
    private static final String TROUBLESHOOTER = "troubleshooter@adminconsolelinks.test";
    private static final String NON_ADMIN = "nonadmin@adminconsolelinks.test";

    public ApiPermissionsHelper _apiPermissionsHelper = new ApiPermissionsHelper(this);

    @BeforeClass
    public static void setupProject()
    {
        AdminConsoleNavigationTest init = (AdminConsoleNavigationTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _userHelper.createUser(TROUBLESHOOTER);
        _apiPermissionsHelper.addMemberToRole(TROUBLESHOOTER, "Troubleshooter", PermissionsHelper.MemberType.user, "/");

        _userHelper.createUser(NON_ADMIN);
        _apiPermissionsHelper.setUserPermissions(NON_ADMIN, "Reader");
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _userHelper.deleteUsers(false, TROUBLESHOOTER, NON_ADMIN);
    }

    @Test
    public void testAdminNavTrails()
    {
        Set<String> ignoredLinks = Collections.newSetFromMap(new CaseInsensitiveHashMap<>());
        ignoredLinks.addAll(List.of(
                "LDAP Sync Admin",                  // An HTML view -- difficult to customize navtrail
                "Authentication",                   // Slow to load
                "Change User Properties",           // Generic domain action -- difficult to customize navtrail
                "Puppeteer Service",                // An HTML view -- difficult to customize navtrail
                "Dump Heap",                        // Undesired consequences
                "Reset Site Errors",                // Undesired consequences
                "Memory Usage",                     // Slow to load
                "View All Site Errors",             // No nav trail
                "View All Site Errors Since Reset", // No nav trail
                "View Primary Site Log File"        // No nav trail
        ));
        List<WebElement> adminLinks = ShowAdminPage.beginAt(this).getAllAdminConsoleLinks();
        assertTrue(String.format("Failed sanity check. Only found %s admin links. There should be more.", adminLinks.size()), adminLinks.size() > 10);
        Map<String, String> linkHrefs = new HashMap<>();

        for (WebElement link : adminLinks)
        {
            linkHrefs.put(link.getText(), link.getAttribute("href"));
        }

        List<String> pagesMissingNavTrail = new ArrayList<>();

        for (Map.Entry<String, String> link : linkHrefs.entrySet())
        {
            if (ignoredLinks.contains(link.getKey()))
            {
                log("Skipping admin link: " + link.getKey());
            }
            else
            {
                TestLogger.log("Checking admin link: " + link.getKey());
                beginAt(link.getValue());
                if (!verifyNavTrail(false, "Admin Console"))
                    pagesMissingNavTrail.add(link.getKey() + ": " + link.getValue());
            }
        }

        pagesMissingNavTrail.sort(Comparator.naturalOrder());
        assertTrue("The following " + pagesMissingNavTrail.size() + " pages are missing Admin Console navtrails:\n" + String.join("\n",
                pagesMissingNavTrail), pagesMissingNavTrail.isEmpty());
    }

    @Test
    public void testTroubleshooterLinkAccess()
    {
        Set<String> ignoredLinks = Collections.newSetFromMap(new CaseInsensitiveHashMap<>());
        ignoredLinks.addAll(List.of(
                "SignUp",                   //link shows up to the troubleshooter but throws 403 while accessing it.
                "Notebook Settings",        //link shows up to the troubleshooter but throws 403 while accessing it.
                "Puppeteer Service",        //link shows up to the troubleshooter but throws 403 while accessing it.
                "Dump Heap",                // Undesired consequences
                "Profiler"                  //Profiler can be edited by the troubleshooter
        ));
        ShowAdminPage adminConsole = goToAdminConsole();
        impersonate(TROUBLESHOOTER);
        Map<String, String> linkHrefs = new LinkedHashMap<>();
        List<WebElement> troubleshooterLinks = adminConsole.getAllAdminConsoleLinks();
        for (WebElement link : troubleshooterLinks)
            linkHrefs.put(link.getText(), link.getAttribute("href"));

        for (Map.Entry<String, String> link : linkHrefs.entrySet())
        {
            if (ignoredLinks.contains(link.getKey()))
                TestLogger.log("Skipping admin link: " + link.getKey());
            else
            {
                log("Verifying link " + link.getKey() + " with URL " + link.getValue());
                verifyReadOnlyLink(link.getKey(), link.getValue());
                verifyPostCommandFails(link.getValue());
            }
        }
        goToHome();
        stopImpersonating();
    }

    @Test
    public void testAdminConsoleLinksForAdminAndNonAdmin()
    {
        Set<String> ignoredLinksNonAdmin = Collections.newSetFromMap(new CaseInsensitiveHashMap<>());
        ignoredLinksNonAdmin.addAll(List.of(
                "Merge sync admin",     //Can be accessed by non admin
                "Dump Heap",            // Undesired consequences
                "Credits"               //Can be accessed by non admin
        ));

        Set<String> ignoredLinksAdmin = Collections.newSetFromMap(new CaseInsensitiveHashMap<>());
        ignoredLinksAdmin.addAll(List.of(
                "Dump Heap"           // Undesired consequences
        ));
        ShowAdminPage adminConsole = goToAdminConsole();
        List<WebElement> adminLinks = adminConsole.getAllAdminConsoleLinks();
        Map<String, String> linkHrefs = new LinkedHashMap<>();
        for (WebElement link : adminLinks)
            linkHrefs.put(link.getText(), link.getAttribute("href"));

        log("Verifying links can be accessed by admin");
        for (Map.Entry<String, String> link : linkHrefs.entrySet())
        {
            if (ignoredLinksAdmin.contains(link.getKey()))
                TestLogger.log("Skipping admin link: " + link.getKey());
            else
            {
                log("Verifying link " + link.getKey() + " with URL " + link.getValue());
                verifyLink(link.getKey(), link.getValue(), 200);
            }
        }

        log("Verifying links cannot be accessed by non admin");
        goToHome();
        impersonate(NON_ADMIN);
        for (Map.Entry<String, String> link : linkHrefs.entrySet())
        {
            if (ignoredLinksNonAdmin.contains(link.getKey()))
                TestLogger.log("Skipping admin link: " + link.getKey());
            else
            {
                log("Verifying link " + link.getKey() + " with URL " + link.getValue());
                verifyLink(link.getKey(), link.getValue(), 403);
            }
        }
        goToHome();
        stopImpersonating();
    }

    private void verifyReadOnlyLink(String text, String url)
    {
        beginAt(url);
        Assert.assertEquals("URL " + url + " is broken for " + text, 200, getResponseCode());
        Locator.XPathLocator updateButtonLoc = Locator.XPathLocator.union(
                Locator.linkWithText("Submit"), Locator.linkWithText("Save"), Locator.linkWithText("Update")).notHidden();
        assertFalse("Either Save/Submit/Update button exists in the page " + text, isElementPresent(updateButtonLoc));
    }

    private void verifyPostCommandFails(String url)
    {
        Crawler.ControllerActionId controllerActionId = new Crawler.ControllerActionId(url);
        Connection connection = WebTestHelper.getRemoteApiConnection();
        SimplePostCommand command = new SimplePostCommand(controllerActionId.getController(), controllerActionId.getAction());
        try
        {
            command.execute(connection, "/");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Post command should not go through", 403, e.getStatusCode());
        }
    }

    private void verifyLink(String text, String url, int expectedResponseCode)
    {
        beginAt(url);
        Assert.assertEquals("URL " + url + " is broken for " + text, expectedResponseCode, getResponseCode());
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}

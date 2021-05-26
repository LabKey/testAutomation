package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Git;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Category({Git.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class AdminConsoleNavigationTest extends BaseWebDriverTest
{
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
            "Memory Usage",                     // Slow to load
            "Reset Site Errors",                // Undesired consequences
            "Running Threads",                  // Undesired consequences
            "View All Site Errors",             // No nav trail
            "View All Site Errors Since Reset", // No nav trail
            "View Primary Site Log File"        // No nav trail
        ));
        ShowAdminPage.beginAt(this);
        WebElement adminLinksContainer = Locator.id("links").findElement(getDriver()); // Maybe put this in 'ShowAdminPage'
        List<WebElement> adminLinks = Locator.tag("a").findElements(adminLinksContainer);
        Assert.assertTrue(String.format("Failed sanity check. Only found %s admin links. There should be more.", adminLinks.size()), adminLinks.size() > 10);
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
                TestLogger.log("Skipping admin link: " + link.getKey());
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
        Assert.assertTrue("The following " + pagesMissingNavTrail.size() + " pages are missing Admin Console navtrails:\n" + String.join("\n", pagesMissingNavTrail), pagesMissingNavTrail.isEmpty());
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

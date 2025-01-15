package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Git;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;

@Category({Git.class})
public class ServerUpgradeTest extends BaseWebDriverTest
{

    @Test
    public void testErrorLog()
    {
        String serverErrors = getServerErrors().trim();

        checker().verifyTrue("There should be no server errors after upgrade. Found: " + serverErrors,
                serverErrors.isEmpty());
    }

    @Test
    public void testMissingModules()
    {
        beginAt(WebTestHelper.buildURL("admin", "modules"));
        List<WebElement> panels = Locator.tagWithClass("div", "panel-portal").findElements(getDriver());

        String panelText = panels.get(1).getText();
        checker().verifyTrue(String.format("It looks like there are unknown modules after upgrade: %s", panelText),
                panelText.contains("This server has no unknown modules."));

    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "Server Upgrade Test";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("Platform");
    }

}

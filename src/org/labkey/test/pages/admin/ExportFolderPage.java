package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class ExportFolderPage extends LabKeyPage<ExportFolderPage.ElementCache>
{
    public ExportFolderPage(WebDriver driver)
    {
        super(driver);
    }

    public static ExportFolderPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ExportFolderPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath, "exportFolder", Maps.of("tabId", "export")));
        return new ExportFolderPage(driver.getDriver());
    }

    public ExportFolderPage selectExperimentsAndRuns(boolean checked)
    {
        elementCache().experimentsAndRunsCheckbox.set(checked);

        return this;
    }

    public ExportFolderPage selectRoleAssignments(boolean checked)
    {
        elementCache().selectRoleAssighmentsCheckbox.set(checked);

        return this;
    }

    public ExportFolderPage selectFiles(boolean checked)
    {
        elementCache().selectFilesCheckbox.set(checked);

        return this;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        public Checkbox experimentsAndRunsCheckbox = new Checkbox(Locator.tagWithText("label", "Experiments and runs")
                .precedingSibling("input").findWhenNeeded(getDriver()));

        public Checkbox selectRoleAssighmentsCheckbox = new Checkbox(Locator.tagWithText("label", "Role assignments for users and groups")
                .precedingSibling("input").findWhenNeeded(getDriver()));

        public Checkbox selectFilesCheckbox = new Checkbox(Locator.tagWithText("label", "Files")
                .precedingSibling("input").findWhenNeeded(getDriver()));

    }
}

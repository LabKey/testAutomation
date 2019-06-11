package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

import java.io.File;

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

    public ExportFolderPage exportRoleAssignments(boolean checked)
    {
        elementCache().roleAssighmentsCheckbox.set(checked);
        return this;
    }

    public ExportFolderPage includeFiles(boolean checked)
    {
        elementCache().selectFilesCheckbox.set(checked);
        return this;
    }

    public ExportFolderPage selectETLDefintions(boolean checked)
    {
        elementCache().etlDefinitionsCheckbox.set(checked);
        return this;
    }

    public ExportFolderPage exportSecurityGroups(boolean checked)
    {
        elementCache().exportSecurityGroupsCheckbox.set(checked);
        return this;
    }

    public ExportFolderPage includeSubfolders(boolean checked)
    {
        elementCache().includeSubfoldersCheckBox.set(checked);
        return this;
    }

    public File exportToBrowserAsZipFile()
    {
        elementCache().browserAsZipFileToggle.check();
        return clickAndWaitForDownload(Locator.linkWithSpan("Export"));
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        public Checkbox experimentsAndRunsCheckbox = new Checkbox(Locator.tagWithText("label", "Experiments and runs")
                .precedingSibling("input").findWhenNeeded(getDriver()));

        public Checkbox roleAssighmentsCheckbox = new Checkbox(Locator.tagWithText("label", "Role assignments for users and groups")
                .precedingSibling("input").findWhenNeeded(getDriver()));

        public Checkbox selectFilesCheckbox = new Checkbox(Locator.tagWithText("label", "Files")
                .precedingSibling("input").findWhenNeeded(getDriver()));

        public Checkbox etlDefinitionsCheckbox = new Checkbox(Locator.tagWithText("label", "ETL Definitions")
                .precedingSibling("input").findWhenNeeded(getDriver()));
        public Checkbox exportSecurityGroupsCheckbox = new Checkbox(Locator.tagWithText("label", "Project-level groups and members")
                .precedingSibling("input").findWhenNeeded(getDriver()));
        public Checkbox includeSubfoldersCheckBox = new Checkbox(Locator.tagContainingText("label", "Include Subfolders")
                .precedingSibling("input").findWhenNeeded(getDriver()));

        RadioButton browserAsZipFileToggle = new RadioButton(Locator.tagContainingText("label", "Browser as zip file")
            .precedingSibling("input").findWhenNeeded(getDriver()));

    }
}

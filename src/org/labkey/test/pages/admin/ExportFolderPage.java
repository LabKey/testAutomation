package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;

public class ExportFolderPage extends LabKeyPage<ExportFolderPage.ElementCache>
{
    public static final String FOLDER_TYPE_AND_ACTIVE_MODULES = "Folder type and active modules";
    public static final String FULL_TEXT_SEARCH_SETTINGS = "Full-text search settings";
    public static final String WEBPART_PROPERTIES_AND_LAYOUT = "Webpart properties and layout";
    public static final String CONTAINER_SPECIFIC_MODULE_PROPERTIES = "Container specific module properties";
    public static final String EXPERIMENTS_AND_RUNS = "Experiments, Protocols, and Runs";
    public static final String LISTS = "Lists";
    public static final String QUERIES = "Queries";
    public static final String GRID_VIEWS = "Grid Views";
    public static final String REPORTS_AND_CHARTS = "Reports and Charts";
    public static final String EXTERNAL_SCHEMA_DEFINITIONS = "External schema definitions";
    public static final String WIKIS_AND_THEIR_ATTACHMENTS = "Wikis and their attachments";
    public static final String NOTIFICATIONS_SETTINGS = "Notification settings";
    public static final String MISSING_VALUE_INDICATORS = "Missing value indicators";
    public static final String SECURITY_GROUPS = "Project-level groups and members";
    public static final String ROLE_ASSIGNMENTS = "Role assignments for users and groups";
    public static final String STUDY = "Study";
    public static final String FILES = "Files";
    public static final String ETLS = "ETL Definitions";
    public static final String SAMPLE_TYPES_AND_DATA_CLASSES = "Sample Types and Data Classes";
    public static final String QC_STATE_SETTINGS = "QC State Settings";

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
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath, "exportFolder"));
        return new ExportFolderPage(driver.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        shortWait().until(ExpectedConditions.visibilityOf(elementCache().exportBtn));
    }

    public ExportFolderPage includeExperimentsAndRuns(boolean checked)
    {
        elementCache().experimentsAndRunsCheckbox.set(checked);
        return this;
    }

    public ExportFolderPage includeQCStateSettings(boolean checked)     // note: this checkbox will only be present for assay and study folder types
    {
        elementCache().qcStateSettingsCheckbox.set(checked);
        return this;
    }

    public ExportFolderPage includeRoleAssignments(boolean checked)
    {
        elementCache().roleAssighmentsCheckbox.set(checked);
        return this;
    }

    public ExportFolderPage includeFiles(boolean checked)
    {
        elementCache().includeFilesCheckbox.set(checked);
        return this;
    }

    public ExportFolderPage includeETLDefintions(boolean checked)
    {
        elementCache().etlDefinitionsCheckbox.set(checked);
        return this;
    }

    public ExportFolderPage includeSampleDatasetData(boolean checked)
    {
        elementCache().SampleDatasetData.set(checked);
        return this;
    }

    public ExportFolderPage includeSampleDatasetDefinitions(boolean checked)
    {
        elementCache().SampleDatasetDefinitions.set(checked);
        return this;
    }

    public ExportFolderPage includeSecurityGroups(boolean checked)
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
        return clickAndWaitForDownload(elementCache().exportBtn);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        public Checkbox experimentsAndRunsCheckbox = new Checkbox(Locator.tagWithText("label", EXPERIMENTS_AND_RUNS)
                .precedingSibling("input").findWhenNeeded(getDriver()));
        public Checkbox qcStateSettingsCheckbox = new Checkbox(Locator.tagContainingText("label", QC_STATE_SETTINGS)
                .precedingSibling("input").findWhenNeeded(getDriver()));

        public Checkbox roleAssighmentsCheckbox = new Checkbox(Locator.tagWithText("label", ROLE_ASSIGNMENTS)
                .precedingSibling("input").findWhenNeeded(getDriver()));

        public Checkbox includeFilesCheckbox = new Checkbox(Locator.tagWithText("label", FILES)
                .precedingSibling("input").findWhenNeeded(getDriver()));

        public Checkbox etlDefinitionsCheckbox = new Checkbox(Locator.tagWithText("label", ETLS)
                .precedingSibling("input").findWhenNeeded(getDriver()));
        public Checkbox exportSecurityGroupsCheckbox = new Checkbox(Locator.tagWithText("label", SECURITY_GROUPS)
                .precedingSibling("input").findWhenNeeded(getDriver()));
        public Checkbox includeSubfoldersCheckBox = new Checkbox(Locator.tagContainingText("label", "Include Subfolders")
                .precedingSibling("input").findWhenNeeded(getDriver()));
        public Checkbox SampleDatasetData = new Checkbox(Locator.tagContainingText("label","Datasets: Sample Dataset Data")
                .precedingSibling("input").findWhenNeeded(getDriver()));
        public Checkbox SampleDatasetDefinitions = new Checkbox(Locator.tagContainingText("label","Datasets: Sample Dataset Definitions")
                .precedingSibling("input").findWhenNeeded(getDriver()));

        RadioButton browserAsZipFileToggle = new RadioButton(Locator.tagContainingText("label", "Browser as zip file")
            .precedingSibling("input").findWhenNeeded(getDriver()));

        WebElement exportBtn = Locator.linkWithSpan("Export").findWhenNeeded(getDriver());
    }
}

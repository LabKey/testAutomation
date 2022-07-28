package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.FileBrowserHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Map;

import static org.labkey.test.params.FieldDefinition.PhiSelectType.NotPHI;

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
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().exportBtn));
        shortWait().until(ExpectedConditions.visibilityOf(elementCache()
                .exportLocationRadioButtons.get(ExportLocation.browserAsZip).getComponentElement()));
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

    public ExportFolderPage includeSampleTypeAndDataClasses(boolean checked)
    {
        elementCache().sampleTypeAndDataClasses.set(checked);
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
        elementCache().sampleDatasetData.set(checked);
        return this;
    }

    public ExportFolderPage includeSampleDatasetDefinitions(boolean checked)
    {
        elementCache().sampleDatasetDefinitions.set(checked);
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

    public ExportFolderPage includeObject(String label, boolean include)
    {
        elementCache().exportItemCheckbox(label).set(include);
        return this;
    }

    public void includePhiColumns(FieldDefinition.PhiSelectType exportPhiLevel)
    {
        if (NotPHI != exportPhiLevel)
        {
            elementCache().phiColumnCheckbox.check();
            switch (exportPhiLevel)
            {
                case Limited -> selectPhiCombo("Limited PHI");
                case PHI -> selectPhiCombo("Full and Limited PHI");
                case Restricted -> selectPhiCombo("Restricted, Full and Limited PHI");
            }
        }
        else
        {
            elementCache().phiColumnCheckbox.uncheck();
        }
    }

    private void selectPhiCombo(String selection)
    {
        elementCache().phiColumnComboBox.selectComboBoxItem(selection);
    }

    public void exportToPipelineAsIndividualFiles()
    {
        selectExportLocation(ExportLocation.pipelineAsFiles);
        clickAndWait(elementCache().exportBtn);
        new FileBrowserHelper(this).waitForFileGridReady();
    }

    public void exportToPipelineAsZip()
    {
        selectExportLocation(ExportLocation.pipelineAsZip);
        clickAndWait(elementCache().exportBtn);
        new FileBrowserHelper(this).waitForFileGridReady();
    }

    public File exportToBrowserAsZipFile()
    {
        selectExportLocation(ExportLocation.browserAsZip);
        return clickAndWaitForDownload(elementCache().exportBtn);
    }

    private void selectExportLocation(ExportLocation location)
    {
        elementCache().exportLocationRadioButtons.get(location).check();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        public Checkbox exportItemCheckbox(String label)
        {
            return new Checkbox.CheckboxFinder().withLabel(label).findWhenNeeded(this);
        }

        public final Checkbox experimentsAndRunsCheckbox = exportItemCheckbox(EXPERIMENTS_AND_RUNS);

        // Use partial label for QC State. Actual label varies depending on enabled modules
        public final Checkbox qcStateSettingsCheckbox = new Checkbox.CheckboxFinder().withLabelContaining(QC_STATE_SETTINGS)
                .findWhenNeeded(getDriver());

        public final Checkbox roleAssighmentsCheckbox = exportItemCheckbox(ROLE_ASSIGNMENTS);

        public final Checkbox sampleTypeAndDataClasses = exportItemCheckbox(SAMPLE_TYPES_AND_DATA_CLASSES);

        public final Checkbox includeFilesCheckbox = exportItemCheckbox(FILES);

        public final Checkbox etlDefinitionsCheckbox = exportItemCheckbox(ETLS);
        public final Checkbox exportSecurityGroupsCheckbox = exportItemCheckbox(SECURITY_GROUPS);
        public final Checkbox includeSubfoldersCheckBox = new Checkbox.CheckboxFinder().withLabelContaining("Include Subfolders")
                .findWhenNeeded(getDriver());
        public final Checkbox sampleDatasetData = new Checkbox.CheckboxFinder().withLabelContaining("Datasets: Sample Dataset Data")
                .findWhenNeeded(getDriver());
        public final Checkbox sampleDatasetDefinitions = new Checkbox.CheckboxFinder().withLabelContaining("Datasets: Sample Dataset Definitions")
                .findWhenNeeded(getDriver());

        public final Checkbox phiColumnCheckbox = new Checkbox.CheckboxFinder().withLabelContaining("Include PHI Columns:")
                .findWhenNeeded(this);
        public final ComboBox phiColumnComboBox = new ComboBox.ComboBoxFinder(getDriver()).withInputNamed("exportPhiLevel")
                .findWhenNeeded(this);

        public final Map<ExportLocation, RadioButton> exportLocationRadioButtons = Map.of(
                ExportLocation.pipelineAsFiles, new RadioButton.RadioButtonFinder()
                        .withLabel("Pipeline root export directory, as individual files").findWhenNeeded(getDriver()),
                ExportLocation.pipelineAsZip, new RadioButton.RadioButtonFinder()
                        .withLabel("Pipeline root export directory, as zip file").findWhenNeeded(getDriver()),
                ExportLocation.browserAsZip, new RadioButton.RadioButtonFinder()
                        .withLabel("Browser as zip file").findWhenNeeded(getDriver())
        );

        public final WebElement exportBtn = Locator.linkWithSpan("Export").findWhenNeeded(getDriver());
    }

    public enum ExportLocation
    {
        pipelineAsFiles,
        pipelineAsZip,
        browserAsZip
    }
}

package org.labkey.test.tests.flow;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.FileBrowserHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Issue 47873: Folder archive with xar and samples fails to import
 */
@Category({Daily.class})
public class FlowFolderReimportTest extends BaseWebDriverTest
{
    private static final String OTHER_PROJECT = "FlowFolderReimportTest Target";
    private static final File FLOW_WORKSPACE = TestFileUtils.getSampleData("flow/versions/v10.8.wsp");

    File flowSamples = null;

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), false);
        _containerHelper.deleteProject(OTHER_PROJECT, false);
    }

    @Before
    public void preTest()
    {
        doCleanup(false); // Delete projects

        _containerHelper.createProject(OTHER_PROJECT); // Create target project
    }

    @Test
    public void testReimportProject()
    {
        _containerHelper.createProject(getProjectName(), "Flow");
        setupFlowWorkspace();
        verifyFlowDashboard(getProjectName());

        exportThenImport(false, false);
        verifyFlowDashboard(OTHER_PROJECT);
    }

    @Test
    public void testReimportSubfolder()
    {
        String subfolder = "Flow subfolder";

        _containerHelper.createProject(getProjectName(), "Flow");
        _containerHelper.createSubfolder(getProjectName(), subfolder, "Flow");
        setupFlowWorkspace();
        verifyFlowDashboard(getProjectName() + "/" + subfolder);

        exportThenImport(true, false);
        verifyFlowDashboard(OTHER_PROJECT + "/" + subfolder);
    }

    @Test
    public void testReimportNestedWorkspaceFolder()
    {
        final String subfolder = "Flow subfolder";

        _containerHelper.createProject(getProjectName(), "Flow");
        setupFlowWorkspace();
        _containerHelper.createSubfolder(getProjectName(), subfolder, "Flow");
        setupFlowWorkspace();

        exportThenImport(true, true);

        // TODO: enable once import is successful
        verifyFlowDashboard(OTHER_PROJECT);
        verifyFlowDashboard(OTHER_PROJECT + "/" + subfolder);
    }

    private void exportThenImport(boolean hasSubfolders, boolean expectError)
    {
        ExportFolderPage exportFolderPage = ExportFolderPage.beginAt(this, getProjectName());
        exportFolderPage.includeSubfolders(hasSubfolders);
        FileBrowserHelper fileBrowserHelper = exportFolderPage.exportToPipelineAsZip(600_000);

        File file;
        if (TestProperties.isServerRemote())
        {
            List<String> fileList = fileBrowserHelper.getFileList();
            fileBrowserHelper.selectFileBrowserItem(fileList.get(0));
            file = fileBrowserHelper.downloadSelectedFiles();
        }
        else
        {
            File exportDir = new File(TestFileUtils.getDefaultFileRoot(getProjectName()), "export");
            file = exportDir.listFiles()[0];
        }

        goToProjectHome(OTHER_PROJECT);
        importFolderFromZip(file, false, 1, expectError, 600_000);
    }

    private void setupFlowWorkspace()
    {
        clickAndWait(Locator.linkWithText("Import FlowJo Workspace Analysis"));
        // 1. Select Workspace
        setFormElement(Locator.id("workspace.file"), FLOW_WORKSPACE);
        clickAndWait(Locator.lkButton("Next"));
        // 2. Select FCS Files
        clickAndWait(Locator.lkButton("Next"));
        // 3. Review Samples
        clickAndWait(Locator.lkButton("Next"));
        // 4. Analysis Folder
        clickAndWait(Locator.lkButton("Next"));
        // 5. Confirm
        clickAndWait(Locator.lkButton("Finish"));
        // Wait for import
        new PipelineStatusDetailsPage(getDriver()).waitForComplete();

        if (flowSamples == null)
        {
            // Pipeline status should redirect to sample list.
            DataRegionTable drt = new DataRegionTable.DataRegionFinder(getDriver()).waitFor();
            drt.checkAllOnPage();
            flowSamples = drt.expandExportPanel()
                    .exportExcel(AbstractDataRegionExportOrSignHelper.ExcelFileType.XLSX);

            clickAndWait(Locators.folderTitle);
            clickAndWait(Locator.linkWithText("Upload Sample Descriptions"));
            waitAndClickAndWait(Locator.button("Save")); // Domain designer

            clickAndWait(Locators.folderTitle);
            clickAndWait(Locator.linkWithText("Upload More Samples"));
            new ImportDataPage(getDriver()).setFile(flowSamples).submit();
        }

        clickAndWait(Locators.folderTitle);
        clickAndWait(Locator.linkWithText("Create a new Analysis script"));
        setFormElement(Locator.name("ff_name"), "Unused Script");
        clickAndWait(Locator.lkButton("Create Analysis Script"));

        clickAndWait(Locators.folderTitle);
        clickAndWait(Locator.linkWithText("Define sample description join fields"));
        selectOptionByValue(Locator.name("ff_samplePropertyURI"), "Name");
        selectOptionByValue(Locator.name("ff_dataField"), "Name");
        clickAndWait(Locator.lkButton("update"));
    }

    private void verifyFlowDashboard(String containerPath)
    {
        goToProjectHome(containerPath);

        // Flow Summary Webpart
        //assertElementPresent(Locator.linkWithText("FCS Analyses (1 run)"));
        //assertElementPresent(Locator.linkWithText("Unused Script (0 runs)"));
        assertElementPresent(Locator.linkWithText("Samples (72)"));
        //assertElementPresent(Locator.linkWithText("Analysis (1 run)"));

        // Flow Experiment Webpart
        //assertElementPresent(Locator.linkWithText("72 FCS files"));
        assertElementPresent(Locator.linkWithText("72 sample descriptions"));
        assertElementPresent(Locator.linkWithText("Modify sample description join fields"));
    }

    @Override
    protected String getProjectName()
    {
        return "FlowFolderReimportTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("flow", "experiment");
    }
}

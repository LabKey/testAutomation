/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests;

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.UIContainerHelper;

import java.io.File;

/**
 * User: elvan
 * Date: 1/24/12
 * Time: 1:26 PM
 */
public class FolderExportTest extends BaseSeleniumWebTest
{

    String[] webParts = {"Study Overview", "Data Pipeline", "Datasets", "Specimens", "Views", "Study Data Tools", "List", "Report web part", "Workbooks"};
    File dataDir = new File(getSampledataPath(), "FolderExport");
    private String folderFromZip = "1 Folder From Zip"; // add numbers to folder names to keep ordering for created folders
    private String folderFromPipelineZip = "2 Folder From Pipeline Zip";
    private String folderFromPipelineExport = "3 Folder From Pipline Export";
    private String folderFromTemplate = "4 Folder From Template";
    String folderZip = "SampleWithSubfolders.folder.zip";


    public FolderExportTest()
    {
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected String getProjectName()
    {
        return "FolderExportTest";
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true; // for importFolderFromZip
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        // we are using the simpletest module to test Container Tab import/export
        goToAdminConsole();
        assertTextPresent("simpletest");

        RReportHelper _rReportHelper = new RReportHelper(this);
        _rReportHelper.ensureRConfig();
        _containerHelper.createProject(getProjectName(), null);
        
        verifyImportFromZip();
        verifyImportFromPipelineZip();
        //Issue 13881
        verifyImportFromPipelineExpanded();
        verifyCreateFolderFromTemplate();
    }

    private void verifyCreateFolderFromTemplate()
    {
        createSubFolderFromTemplate(getProjectName(), folderFromTemplate, "/" + getProjectName() + "/" + folderFromZip, null);
        verifyExpectedWebPartsPresent();
        verifySubfolderImport(3, true);
        verifyFolderExportAsExpected(folderFromTemplate);
    }

    private void verifyImportFromPipelineZip()
    {
        verifyImportFromPipeline(folderZip, folderFromPipelineZip, 1);
    }

    private void verifyImportFromPipelineExpanded()
    {
        // test importing the folder archive that we exported from the verifyFolderExportAsExpected method
        verifyImportFromPipeline("export/folder.xml", folderFromPipelineExport, 2);
    }

    private void verifyImportFromPipeline(String fileImport, String folderName, int subfolderIndex)
    {

        createSubfolder(getProjectName(), getProjectName(), folderName, "Collaboration", null);
        setPipelineRoot(dataDir.getAbsolutePath());
        importFolderFromPipeline( "" + fileImport);


        clickLinkWithText(folderName);
        verifyFolderImportAsExpected(subfolderIndex);
        verifyFolderExportAsExpected(folderName);
    }

    private void verifyImportFromZip()
    {
        _containerHelper.createSubfolder(getProjectName(), folderFromZip, null);
        // create one of the subfolders, to be imported, to test merge on import of subfolders
        _containerHelper.createSubfolder(folderFromZip, "Subfolder1", "Collaboration");

        clickLinkWithText(folderFromZip);
        importFolderFromZip(new File(dataDir, folderZip).getAbsolutePath());
        beginAt(getCurrentRelativeURL()); //work around linux issue
        waitForPipelineJobsToComplete(1, "Folder import", false);
        clickLinkWithText(folderFromZip);
        verifyFolderImportAsExpected(0);
        verifyFolderExportAsExpected(folderFromZip);
    }

    private void verifyFolderExportAsExpected(String folderName)
    {
        log("Exporting folder to pipeline as individual files");
        clickLinkWithText(folderName);
        goToFolderManagement();
        clickLinkWithText("Export");
        click(Locator.name("includeSubfolders"));
        click(Locator.name("location")); // first locator with this name is "Pipeline root export directory, as individual files
        clickButton("Export");

        // verify some of the folder export items by selecting them in the file browser
        _extHelper.selectFileBrowserItem("export/folder.xml");
        _extHelper.selectFileBrowserItem("export/subfolders/subfolders.xml");
        _extHelper.selectFileBrowserItem("export/subfolders/Subfolder1/folder.xml");
        _extHelper.selectFileBrowserItem("export/subfolders/Subfolder1/subfolders/_hidden/folder.xml");
        _extHelper.selectFileBrowserItem("export/subfolders/Subfolder2/folder.xml");
    }

    private void verifyExpectedWebPartsPresent()
    {
        assertTextPresentInThisOrder(webParts);
    }
    private void verifyFolderImportAsExpected(int subfolderIndex)
    {
        verifyExpectedWebPartsPresent();
        assertTextPresent("Demo Study tracks data in 12 datasets over 26 time points. Data is present for 6 Participants", "Test wikiTest wikiTest wiki");

        log("Verify import of list");
        String listName = "safe list";
        assertTextPresent(listName);
        clickLinkWithText(listName);
        assertTextPresent("persimmon");
        assertImagePresentWithSrc("/labkey/_images/mv_indicator.gif");
        assertTextNotPresent("grapefruit");//this has been filtered out.  if "grapefruit" is present, the filter wasn't preserved
        goBack();

        log("verify import of query web part");
        assertTextPresent("~!@#$%^&*()_+query web part", "Contains one row per announcement or reply");

        log("verify report present");
        assertTextPresent("pomegranate");

        log("verify search settings as expected");
        goToFolderManagement();
        clickLinkWithText("Search");
        Assert.assertFalse("Folder search settings not imported", isChecked(Locator.checkboxById("searchable")));

        log("verify folder type was overwritten on import");
        clickLinkContainingText("Folder Type");
        Assert.assertTrue("Folder type not overwritten on import", isChecked(Locator.radioButtonByNameAndValue("folderType", "None")));

        log("verify notification default settings as expected");
        clickLinkWithText("Notifications");
        waitForText("Email Notification Settings");
        click(Locator.navButton("Update Settings"));
        waitForElement(Locator.xpath("//li/a[text()='files']"), WAIT_FOR_JAVASCRIPT);
        isElementPresent(Locator.xpath("//div[text()='Daily digest' and contains(@class, 'x-combo-selected')]"));
        selenium.mouseDown("//li/a[text()='messages']");
        isElementPresent(Locator.xpath("//div[text()='All conversations' and contains(@class, 'x-combo-selected')]"));

        verifySubfolderImport(subfolderIndex, false);
    }

    private void verifySubfolderImport(int subfolderIndex, boolean fromTemplate)
    {
        log("verify child containers were imported");
        clickLinkWithText("Subfolder1", subfolderIndex);
        assertTextPresent("My Test Container Tab Query");
        clickLinkWithText("_hidden", subfolderIndex);
        assertTextPresentInThisOrder("Lists", "Hidden Folder List");
        clickLinkWithText("Subfolder2", subfolderIndex);
        if (fromTemplate)
            assertTextPresent("This folder does not contain a study.");
        else
            assertTextPresent("Study Label for Subfolder2 tracks data in 1 datasets over 1 visits. Data is present for 2 Monkeys.");

        log("verify container tabs were imported");
        clickLinkWithText("Subfolder1", subfolderIndex);
        assertLinkPresentWithText("Assay Container");
        assertLinkPresentWithText("Tab 2");
        assertLinkPresentWithText("Study Container");
        assertLinkNotPresentWithText("Tab 1");
        clickLinkWithText("Tab 2");
        assertTextPresentInThisOrder("A customized web part", "Experiment Runs", "Assay List");
        clickLinkWithText("Study Container");
        if (fromTemplate)
            assertTextPresent("This folder does not contain a study.");
        else
            assertTextPresent("Study Container Tab Study tracks data in 0 datasets over 0 visits. Data is present for 0 Participants.");
    }


    @Override
    protected void doCleanup(boolean afterTest) throws Exception
    {
        deleteProject(getProjectName() + TRICKY_CHARACTERS_FOR_PROJECT_NAMES, false);
        deleteProject(getProjectName(), false);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

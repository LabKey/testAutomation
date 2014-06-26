/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.UIContainerHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@Category({DailyB.class})
public class FolderExportTest extends BaseWebDriverTest
{

    String[] webParts = {"Study Overview", "Data Pipeline", "Datasets", "Specimens", "Views", "Test wiki", "Study Data Tools", "Lists", "~!@#$%^&*()_+query web part", "Report web part", "Workbooks"};
    File dataDir = new File(getSampledataPath(), "FolderExport");
    private final String folderFromZip = "1 Folder From Zip"; // add numbers to folder names to keep ordering for created folders
    private final String folderFromPipelineZip = "2 Folder From Pipeline Zip";
    private final String folderFromPipelineExport = "3 Folder From Pipeline Export";
    private final String folderFromTemplate = "4 Folder From Template";
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
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected Set<String> excludeFromViewCheck()
    {
        Set<String> folders = new HashSet<>();
        folders.add(folderFromTemplate);
        return folders;
    }

    @Test
    public void testSteps()
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

    @LogMethod
    private void verifyCreateFolderFromTemplate()
    {
        createSubFolderFromTemplate(getProjectName(), folderFromTemplate, "/" + getProjectName() + "/" + folderFromZip, new String[] {"Reports"});
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

    @LogMethod
    private void verifyImportFromPipeline(String fileImport, String folderName, int subfolderIndex)
    {

        createSubfolder(getProjectName(), getProjectName(), folderName, "Collaboration", null);
        setPipelineRoot(dataDir.getAbsolutePath());
        importFolderFromPipeline( "" + fileImport);


        clickFolder(folderName);
        verifyFolderImportAsExpected(subfolderIndex);
        verifyFolderExportAsExpected(folderName);
    }

    @LogMethod
    private void verifyImportFromZip()
    {
        _containerHelper.createSubfolder(getProjectName(), folderFromZip, null);
        // create one of the subfolders, to be imported, to test merge on import of subfolders
        _containerHelper.createSubfolder(getProjectName() + "/" + folderFromZip, "Subfolder1", "Collaboration");

        clickFolder(folderFromZip);
        importFolderFromZip(new File(dataDir, folderZip));
        beginAt(getCurrentRelativeURL()); //work around linux issue
        waitForPipelineJobsToComplete(1, "Folder import", false);
        clickFolder(folderFromZip);
        verifyFolderImportAsExpected(0);
        verifyFolderExportAsExpected(folderFromZip);
    }

    @LogMethod
    private void verifyFolderExportAsExpected(String folderName)
    {
        log("Exporting folder to pipeline as individual files");
        clickFolder(folderName);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Export"));
        click(Locator.name("includeSubfolders"));
        click(Locator.name("location")); // first locator with this name is "Pipeline root export directory, as individual files
        clickButton("Export");

        // verify some of the folder export items by selecting them in the file browser
        _fileBrowserHelper.selectFileBrowserItem("export/folder.xml");
        _fileBrowserHelper.selectFileBrowserItem("export/subfolders/subfolders.xml");
        _fileBrowserHelper.selectFileBrowserItem("export/subfolders/Subfolder1/folder.xml");
        _fileBrowserHelper.selectFileBrowserItem("export/subfolders/Subfolder1/subfolders/_hidden/folder.xml");
        _fileBrowserHelper.selectFileBrowserItem("export/subfolders/Subfolder2/folder.xml");
    }

    private void verifyExpectedWebPartsPresent()
    {
        Locator titleLoc = Locator.css(".labkey-wp-title-text");
        List<WebElement> titlesElements = titleLoc.findElements(getDriver());
        Iterator<WebElement> it = titlesElements.iterator();
        WebElement curEl = it.next();
        for (String expectedTitle : webParts)
        {
            while (!curEl.getText().equals(expectedTitle))
            {
                if (it.hasNext())
                    curEl = it.next();
                else
                {
                    assertElementPresent(titleLoc.withText(expectedTitle));
                    fail("Webpart found out of order: " + expectedTitle);
                }
            }
        }
    }

    @LogMethod
    private void verifyFolderImportAsExpected(int subfolderIndex)
    {
        verifyExpectedWebPartsPresent();
        assertElementPresent(Locator.css(".study-properties").withText("Demo Study tracks data in 12 datasets over 26 time points. Data is present for 6 Participants."));
        assertElementPresent(Locator.css(".labkey-wiki").withText("Test wikiTest wikiTest wiki"));

        log("Verify import of list");
        String listName = "safe list";
        assertTextPresent(listName);
        clickAndWait(Locator.linkWithText(listName));
        assertTextPresent("persimmon");
        assertElementPresent(Locator.imageWithSrc("/labkey/_images/mv_indicator.gif", false));
        assertTextNotPresent("grapefruit");//this has been filtered out.  if "grapefruit" is present, the filter wasn't preserved
        goBack();

        log("verify import of query web part");
        assertTextPresent("~!@#$%^&*()_+query web part", "Contains one row per announcement or reply");

        log("verify report present");
        assertTextPresent("pomegranate");

        log("verify search settings as expected");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Search"));
        assertFalse("Folder search settings not imported", isChecked(Locator.checkboxById("searchable")));

        log("verify folder type was overwritten on import");
        clickAndWait(Locator.linkContainingText("Folder Type"));
        assertTrue("Folder type not overwritten on import", isChecked(Locator.radioButtonByNameAndValue("folderType", "None")));

        log("verify notification default settings as expected");
        clickAndWait(Locator.linkWithText("Notifications"));
        waitForText("Email Notification Settings");
        click(Locator.lkButton("Update Settings"));
        waitForElement(Locator.xpath("//li/a[text()='files']"), WAIT_FOR_JAVASCRIPT);
        isElementPresent(Locator.xpath("//div[text()='Daily digest' and contains(@class, 'x-combo-selected')]"));
        click(Locator.css("#dataregion_Users li > a").withText("messages"));
        isElementPresent(Locator.xpath("//div[text()='All conversations' and contains(@class, 'x-combo-selected')]"));

        verifySubfolderImport(subfolderIndex, false);
    }

    @LogMethod
    private void verifySubfolderImport(int subfolderIndex, boolean fromTemplate)
    {
        log("verify child containers were imported");
        hoverFolderBar();
        expandFolderTree("Subfolder1"); // Will expand to all subfolders with this name
        clickAndWait(Locator.linkWithText("Subfolder1", subfolderIndex));
        assertTextPresent("My Test Container Tab Query");
        hoverFolderBar();
        expandFolderTree("_hidden");
        clickAndWait(Locator.linkWithText("_hidden").index(subfolderIndex));
        assertTextPresentInThisOrder("Lists", "Hidden Folder List");
        hoverFolderBar();
        clickAndWait(Locator.linkWithText("Subfolder2", subfolderIndex));
        if (fromTemplate)
            assertElementPresent(Locator.css("#bodypanel .labkey-wp-body p").withText("This folder does not contain a study."));
        else
            assertElementPresent(Locator.css(".study-properties").withText("Study Label for Subfolder2 tracks data in 1 dataset over 1 visit. Data is present for 2 Monkeys."));

        log("verify container tabs were imported");
        hoverFolderBar();
        clickAndWait(Locator.linkWithText("Subfolder1", subfolderIndex));
        assertElementPresent(Locator.linkWithText("Assay Container"));
        assertElementPresent(Locator.linkWithText("Tab 2"));
        assertElementPresent(Locator.linkWithText("Study Container"));
        assertElementNotPresent(Locator.linkWithText("Tab 1"));
        clickAndWait(Locator.linkWithText("Tab 2"));
        assertTextPresentInThisOrder("A customized web part", "Experiment Runs", "Assay List");
        clickAndWait(Locator.linkWithText("Study Container"));
        if (fromTemplate)
            assertElementPresent(Locator.css("#bodypanel .labkey-wp-body p").withText("This folder does not contain a study."));
        else
            assertElementPresent(Locator.css(".study-properties").withText("Study Container Tab Study tracks data in 0 datasets over 0 visits. Data is present for 0 Participants."));
    }


    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName() + TRICKY_CHARACTERS_FOR_PROJECT_NAMES, false);
        deleteProject(getProjectName(), false);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/core";
    }

    @Override
    public void validateQueries(boolean validateSubfolders)
    {
        super.validateQueries(false); // too may subfolders
    }
}

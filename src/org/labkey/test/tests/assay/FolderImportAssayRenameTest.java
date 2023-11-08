package org.labkey.test.tests.assay;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({Assays.class, Daily.class})
public class FolderImportAssayRenameTest extends BaseWebDriverTest
{

    private static final String ORIGINAL_PROJECT = "Folder_Import_Assay_Rename_Test";
    private static final String SECOND_PROJECT = "Folder_Import_Assay_Rename_Test_Copy";

    private static final File GPAT_ASSAY_XLS = TestFileUtils.getSampleData("GPAT/trial01.xls");

    @BeforeClass
    public static void doSetup()
    {
        FolderImportAssayRenameTest init = (FolderImportAssayRenameTest) getCurrentTest();

        init._containerHelper.createProject(ORIGINAL_PROJECT, "Assay");
        init._containerHelper.createProject(SECOND_PROJECT, "Assay");

        init.goToProjectHome();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }

    @Override
    protected String getProjectName()
    {
        return ORIGINAL_PROJECT;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(ORIGINAL_PROJECT, afterTest);
        _containerHelper.deleteProject(SECOND_PROJECT, afterTest);
    }

    /**
     * <p>
     *     This test adds some coverage for code that was changed as part of the assay rename work.
     * </p>
     * <p>
     *     This test will verify that an assay can be created successfully after importing from xar, with different names.
     *     This scenario is needed because the rename work changed how unique lsid is constructed. It will check that the
     *     DBsequence used for xar import doesn't collide with those created from project manually
     * </p>
     */
    @Test
    public void testProjectImport()
    {

        log("Create a gPat assay with some run data.");
        String originalAssayName = "This Assay Will Be Renamed";
        createGpatAssayAndRun(ORIGINAL_PROJECT, originalAssayName);

        String updatedAssayName = "A Brand New Name";
        log(String.format("Edit the assay design and rename it to '%s'.", updatedAssayName));

        ReactAssayDesignerPage assayDesignerPage = _assayHelper.clickEditAssayDesign(false);
        checker().fatal()
                .verifyTrue("The 'Name' field should be enabled and editable. Fatal error.",
                        assayDesignerPage.isNameEnabled());

        assayDesignerPage.setName(updatedAssayName);
        assayDesignerPage.clickFinish();

        log("Export the project folder.");
        File exportedProject = exportFolderAsZip(ORIGINAL_PROJECT, false, false, false, true);

        log("Import the folder archive into a new project.");
        goToProjectHome(SECOND_PROJECT);
        importFolderFromZip(exportedProject, false, 1);

        log("Validated that the renamed assay is as expected in the new project.");

        beginAt(WebTestHelper.buildURL("assay", SECOND_PROJECT, "begin"));

        if(checker().verifyTrue(String.format("Link to updated assay design name '%s' not present in second project.", updatedAssayName),
                Locator.linkWithText(updatedAssayName).isDisplayed(getDriver())))
        {
            clickAndWait(Locator.linkWithText(updatedAssayName));

            if(checker().verifyTrue(String.format("Link to updated assay design name '%s' not present in second project.", updatedAssayName),
                    Locator.linkWithText(GPAT_ASSAY_XLS.getName()).isDisplayed(getDriver())))
            {
                clickAndWait(Locator.linkWithText(GPAT_ASSAY_XLS.getName()));
                checker().verifyTrue(String.format("Run data for assay '%s' not as expected in the second folder.", updatedAssayName),
                        validateRunDataLoaded());
            }

        }

        checker().screenShotIfNewError("Imported_Project_Folder_Assay_Error");

        log("Validate that a new assay can be created in the second folder.");
        String secondAssay = "Assay Created In Copied Project";
        createGpatAssayAndRun(SECOND_PROJECT, secondAssay);

    }

    private void createGpatAssayAndRun(String projectName, String assayName)
    {
        new WebDavUploadHelper(projectName).uploadFile(GPAT_ASSAY_XLS);
        beginAt(WebTestHelper.buildURL("pipeline", projectName, "browse"));
        _fileBrowserHelper.importFile(GPAT_ASSAY_XLS.getName(), "Create New Standard Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");

        Locator assayNameTxtBox = Locator.tagWithId("input", "AssayDesignerName");
        setFormElement(assayNameTxtBox, assayName);
        fireEvent(assayNameTxtBox, SeleniumEvent.blur);

        clickButton("Begin import");
        waitAndClick(Locator.lkButton("Next"));
        waitAndClick(Locator.lkButton("Save and Finish"));
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLS.getName()));

        checker().fatal().verifyTrue("Run data not loaded. Fatal error.",
                validateRunDataLoaded());

    }

    private boolean validateRunDataLoaded()
    {
        // Waiting for record count will validate that the import was successful.
        return waitFor(()->Locator.css(".labkey-pagination")
                        .containing("1 - 100 of 201")
                        .isDisplayed(getDriver()),
                5_000);
    }

}

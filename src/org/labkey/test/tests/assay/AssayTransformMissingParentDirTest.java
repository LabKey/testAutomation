package org.labkey.test.tests.assay;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.assay.AssayConstants;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayImportPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.params.assay.GeneralAssayDesign;
import org.labkey.test.util.search.SearchAdminAPIHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Issue 54156: Regression test to ensure a reasonable error message is shown when an assay design references
 * a transform script whose parent directory has since been deleted, and that the assay design can be fixed by removing the script.
 */
@Category({Assays.class, Daily.class})
public class AssayTransformMissingParentDirTest extends AbstractAssayTransformTest
{
    @Test
    public void testMissingParentDirectoryRegression() throws Exception
    {
        // Create a nested directory and an R transform script within it
        String assayName = "missingParentDirAssay";
        Path parentDir = Files.createTempDirectory("assay-transform-parent-");
        Path nestedDir = FileUtil.createDirectories(parentDir.resolve("child"), false);
        String scriptName = "transformMissingParent.R";
        String transformContent = "library(Rlabkey);";
        File transformFile = nestedDir.resolve(scriptName).toFile();
        TestFileUtils.writeFile(transformFile, transformContent);

        // Create a General assay and add the transform by absolute path (not upload)
        var protocolResponse = new GeneralAssayDesign(assayName).createAssay(getProjectName(), createDefaultConnection());
        var assayDesignerPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId(),
                "general", getURL().toString());
        assayDesignerPage.goToBatchFields().removeAllFields(true);
        // add by path so the absolute path is stored; this allows reproducing the missing parent dir scenario
        assayDesignerPage.addTransformScript(transformFile);
        assayDesignerPage.clickSave();

        // Now delete the parent dir to ensure we handle it reasonably.
        // Sometimes on Windows the directory could be locked, maybe by an external process, or the child directory is
        // readonly. Use a retry mechanism to set the writeable flag and then try to delete the parent directory.
        for (int attempt = 1; attempt <= 10; attempt++) {
            try
            {
                parentDir.toFile().setWritable(true, false);

                // Wrap in a try to close the stream.
                try (Stream<Path> files = Files.walk(parentDir)) {
                    files.forEach(p -> p.toFile().setWritable(true, false));
                }

                FileUtils.deleteDirectory(parentDir.toFile());
                log(String.format("Deletion of directory %s was successful.", parentDir));
                break;
            } catch (AccessDeniedException deniedException) {
                // Yes I know AccessDeniedException is a subset of an IOException, but I wanted to log explicitly a
                // failure and retry because of an AccessDeniedException from some other IOException.
                log(String.format("Access denied trying to delete directory %s. Error: %s. Waiting 10s and retrying. Attempt %d of 10.",
                        parentDir, deniedException.getMessage(), attempt));
                if (attempt == 10) throw deniedException;
                sleep(10_000);
            }
            catch (IOException ioException) {
                log(String.format("IOException trying to delete directory %s. Error: %s. Waiting 10s and retrying. Attempt %d of 10.",
                        parentDir, ioException.getMessage(), attempt));
                if (attempt == 10)
                {
                    log("Dump the heap.");
                    dumpHeap();

                    if (SystemUtils.IS_OS_WINDOWS) {
                        try {
                            log("Lock diagnostic...");
                            ProcessBuilder pb = new ProcessBuilder("tasklist");
                            pb.redirectErrorStream(true);
                            Process p = pb.start();
                            String output = new String(p.getInputStream().readAllBytes(), StringUtilsLabKey.DEFAULT_CHARSET);
                            log("Running processes:\n" + output);
                        } catch (IOException diagnosticException) {
                            log("Failed to run lock diagnostic: " + diagnosticException.getMessage());
                        }
                    }
                    throw ioException;
                }
                sleep(10_000);
            }
        }

        // Attempt to import data and verify a reasonable error message is shown
        String importData = """
                VisitID\tParticipantID\tComment
                1\tP1\timport after parent deleted
                """;

        clickAndWait(Locator.linkWithText(assayName));
        new AssayRunsPage(getDriver()).getTable().clickHeaderButtonAndWait("Import Data");
        var importPage = new AssayImportPage(getDriver());
        importPage.setNamedInputText("Name", "missingParentImport");
        importPage.setNamedTextAreaValue(AssayConstants.TEXT_AREA_DATA_COLLECTOR_TEXT_AREA_NAME, importData);
        importPage.clickSaveAndFinish();

        // Expect an error page/message indicating the transform script path cannot be used
        // Be tolerant to platform-specific phrasing; assert any of these appear
        String expectedPath = transformFile.getAbsolutePath();
        checker().withScreenshot("missing-parent-error")
                .verifyTrue("Expect an error message about the transform script path not being found",
                        isTextPresent("transformMissingParent.R, configured for this assay does not exist."));

        // Fix the assay design by removing the transform script
        goToProjectHome();
        assayDesignerPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId(),
                "general", getURL().toString());
        assayDesignerPage.removeTransformScript(scriptName);
        assayDesignerPage.clickSave();

        // Retry the import and verify it succeeds without the transform
        clickAndWait(Locator.linkWithText(assayName));
        new AssayRunsPage(getDriver()).getTable().clickHeaderButtonAndWait("Import Data");
        importPage = new AssayImportPage(getDriver());
        importPage.setNamedInputText("Name", "fixedAssayImport");
        importPage.setNamedTextAreaValue(AssayConstants.TEXT_AREA_DATA_COLLECTOR_TEXT_AREA_NAME, importData);
        importPage.clickSaveAndFinish();

        // Verify we land on the run details page and can see the run name (no transform needed)
        new AssayRunsPage(getDriver()).clickAssayIdLink("fixedAssayImport");
    }

}

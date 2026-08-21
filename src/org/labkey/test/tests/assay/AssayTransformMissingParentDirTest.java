/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.tests.assay;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.assay.AssayConstants;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.AssayImportPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.pages.files.WebDavPage;
import org.labkey.test.params.assay.GeneralAssayDesign;

import java.io.File;
import java.util.List;

/**
 * Issue 54156: Regression test to ensure a reasonable error message is shown when an assay design references
 * a transform script whose parent directory has since been deleted, and that the assay design can be fixed by removing the script.
 */
@Category({Assays.class, Daily.class})
public class AssayTransformMissingParentDirTest extends AbstractAssayTransformTest
{
    private static final File RTRANSFORM_SCRIPT_FILE_NOOP = TestFileUtils.getSampleData("qc/noopTransform.R");

    @Test
    public void testMissingParentDirectoryRegression() throws Exception
    {
        // create a General assay and add the transform
        String assayName = "missingParentDirAssay";
        String transformFileName = RTRANSFORM_SCRIPT_FILE_NOOP.getName();
        var protocolResponse = new GeneralAssayDesign(assayName).setBatchFields(List.of(), false).createAssay(getProjectName(), createDefaultConnection());
        var assayDesignerPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId(),
                "general", "");
        assayDesignerPage.addTransformScript(RTRANSFORM_SCRIPT_FILE_NOOP);
        String transformScriptPath = assayDesignerPage.getTransformScriptPath(transformFileName);
        assayDesignerPage.clickSave();

        // move the script into a nested directory in the @scripts webdav path
        WebDavPage webDavPage = WebDavPage.beginAt(this, getProjectName() + "/@scripts");
        webDavPage.getFileBrowserHelper().createFolder("child");
        File transformFile = new File(transformScriptPath);
        File destinationFile = new File(transformFile.getParent() + "/child", transformFileName);
        FileUtils.moveFile(transformFile, destinationFile);

        // update the assay design with the new nested dir file path
        assayDesignerPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId(),
                "general", "");
        assayDesignerPage.removeTransformScript(transformFileName);
        assayDesignerPage.addTransformScript(destinationFile, false);
        assayDesignerPage.clickSave();

        // Now delete the nested dir to ensure we handle it reasonably
        File destinationDir = new File(transformFile.getParent() + "/child");
        FileUtils.deleteDirectory(destinationDir);

        // Attempt to import data and verify a reasonable error message is shown
        String importData = """
                VisitID\tParticipantID\tComment
                1\tP1\timport after parent deleted
                """;

        AssayRunsPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId())
            .getTable().clickHeaderButtonAndWait("Import Data");
        var importPage = new AssayImportPage(getDriver());
        importPage.setNamedInputText("Name", "missingParentImport");
        importPage.setNamedTextAreaValue(AssayConstants.TEXT_AREA_DATA_COLLECTOR_TEXT_AREA_NAME, importData);
        importPage.clickSaveAndFinish();

        // Expect an error page/message indicating the transform script path cannot be used
        // Be tolerant to platform-specific phrasing; assert any of these appear
        checker().withScreenshot("missing-parent-error")
                .verifyTrue("Expect an error message about the transform script path not being found",
                        isTextPresent(transformFileName + ", configured for this assay does not exist."));

        // Fix the assay design by removing the transform script
        goToProjectHome();
        assayDesignerPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId(),
                "general", getURL().toString());
        assayDesignerPage.removeTransformScript(transformFileName);
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

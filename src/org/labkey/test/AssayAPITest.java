/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.ImportRunResponse;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.labkey.test.util.UIAssayHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({Daily.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class AssayAPITest extends BaseWebDriverTest
{
    protected final static File CREST_FILE = TestFileUtils.getSampleData("InlineImages/crest.png");
    protected final static File SCREENSHOT_FILE = TestFileUtils.getSampleData("InlineImages/screenshot.png");
    protected final static File FOO_XLS_FILE = TestFileUtils.getSampleData("InlineImages/foo.xls");
    protected final static File HELP_ICON_FILE = TestFileUtils.getSampleData("InlineImages/help.jpg");
    protected final static File CREST_2_FILE = TestFileUtils.getSampleData("InlineImages/crest-2.png");
    protected final static String SUBFOLDER_1 = "Sub1";

    @Override
    protected String getProjectName()
    {
        return "Assay API TEST";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        AssayAPITest initTest = (AssayAPITest) getCurrentTest();
        initTest._containerHelper.createProject(initTest.getProjectName(), "Assay");
        initTest._containerHelper.createSubfolder(initTest.getProjectName(), SUBFOLDER_1, "Assay");
        initTest.goToProjectHome();;

        initTest.log("upload inline files to the pipeline root");
        initTest.goToModule("FileContent");
        initTest._fileBrowserHelper.uploadFile(CREST_FILE);
        initTest._fileBrowserHelper.uploadFile(SCREENSHOT_FILE);
        initTest._fileBrowserHelper.uploadFile(FOO_XLS_FILE);
    }

    @Test
    public void testImportRun() throws Exception
    {
        goToProjectHome();
        int pipelineCount = 0;
        String runName = "trial01.xls";
        importAssayAndRun(TestFileUtils.getSampleData("AssayAPI/XLS Assay.xar.xml"), ++pipelineCount, "/" + getProjectName(),
                "XLS Assay", TestFileUtils.getSampleData("GPAT/" + runName), runName, new String[]{"K770K3VY-19"});
        // verify images are resolved and rendered properly
        assertElementPresent("Did not find the expected number of icons for images for " + CREST_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + CREST_FILE.getName() + "')]"), 100);
        waitForElement(Locator.paginationText(1, 100, 201));

        // Upload from child folder to parent folder
        goToProjectFolder(getProjectName(), SUBFOLDER_1);

        //Issue 16073
        importAssayAndRun(TestFileUtils.getSampleData("AssayAPI/BatchPropRequired.xar"), ++pipelineCount, "/" + getProjectName(),
                "BatchPropRequired", TestFileUtils.getSampleData("GPAT/" + runName), "trial01-1.xls", new String[]{"K770K3VY-19"});
        waitForElement(Locator.paginationText(1, 100, 201));
    }

    protected void importAssayAndRun(File assayPath, int pipelineCount, String container, String assayName, File runPath,
                                     String runName, String[] textToCheck) throws IOException, CommandException
    {
        // Issue 42637: Verify that .xar.xml file can be imported through the UI
        UIAssayHelper _uiAssayHelper = new UIAssayHelper(this);
        _uiAssayHelper.uploadXarFileAsAssayDesign(assayPath, pipelineCount, container);

        APIAssayHelper _apiAssayHelper = new APIAssayHelper(this);
        _apiAssayHelper.importAssay(assayName, runPath, getProjectName(), Collections.singletonMap("ParticipantVisitResolver", "SampleInfo"));

        log("verify import worked");
        goToProjectHome();
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText(runName));
        assertTextPresent(textToCheck);
    }

    @Test
    public void testGpatAssayOverAPI() throws Exception
    {
        String assayName = "testGpatAssay";
        String assayDescription = "generated for test purposes over remoteAPI";

        Connection connection = createDefaultConnection();
        GetProtocolCommand getProtocolCommand = new GetProtocolCommand("General");                      // gets a template from the server
        ProtocolResponse getProtocolResponse = getProtocolCommand.execute(connection, getCurrentContainerPath());

        Protocol newAssayProtocol = getProtocolResponse.getProtocol();
        newAssayProtocol.setName(assayName)
                .setDescription(assayDescription)
                .setQCEnabled(true)
                .setEditableResults(true)
                .setEditableRuns(true);
        SaveProtocolCommand saveProtocolCommand = new SaveProtocolCommand(newAssayProtocol);
        ProtocolResponse saveProtocolResponse = saveProtocolCommand.execute(connection, getCurrentContainerPath());
        Integer protocolId = saveProtocolResponse.getProtocol().getProtocolId();

        assertEquals(assayDescription, saveProtocolResponse.getProtocol().getDescription());
        assertTrue(saveProtocolResponse.getProtocol().getQcEnabled());
        assertTrue(saveProtocolResponse.getProtocol().getEditableResults());
        assertTrue(saveProtocolResponse.getProtocol().getEditableRuns());

        GetProtocolCommand protocolCommand = new GetProtocolCommand(protocolId);
        ProtocolResponse doubleCheckProtocolResponse = protocolCommand.execute(connection, getCurrentContainerPath());

        assertEquals(assayDescription, doubleCheckProtocolResponse.getProtocol().getDescription());
        assertTrue(doubleCheckProtocolResponse.getProtocol().getQcEnabled());
        assertTrue(doubleCheckProtocolResponse.getProtocol().getEditableResults());
        assertTrue(doubleCheckProtocolResponse.getProtocol().getEditableRuns());
    }

    // Issue 30003: support importing assay data relative to pipeline root
    @Test
    public void testImportRun_serverFilePath() throws Exception
    {
        goToProjectHome();

        String assayName = "GPAT-ImportRunApi" + TRICKY_CHARACTERS;
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        int assayId = assayHelper.getIdFromAssayName(assayName, getProjectName(), false);
        if (assayId == 0)
            assayId = assayHelper.createAssayDesignWithDefaults(getProjectName(), "General", assayName).getProtocolId();

        // First, simulate file already being uploaded to the server by copying to the pipeline root
        List<String> lines1 = Arrays.asList(
                "ptid\tdate\n",
                "p01\t2017-05-10\n",
                "p02\t2017-05-10\n"
        );
        File fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        Path relativePath1 = Paths.get("testImportRunFilePath", "results1.tsv");
        Path pipelinePath1 = createDataFile(fileRoot, relativePath1, lines1);

        // import the file using a relative path
        ImportRunResponse resp = assayHelper.importAssay(assayId, relativePath1.toString(), getProjectName(), Collections.emptyMap());
        beginAt(resp.getSuccessURL());
        assertTextPresent("p01", "p02");

        goToProjectHome();

        List<String> lines2 = Arrays.asList(
                "ptid\tdate\n",
                "p03\t2017-05-10\n",
                "p04\t2017-05-10\n"
        );
        Path relativePath2 = Paths.get("testImportRunFilePath", "results2.tsv");
        Path pipelinePath2 = createDataFile(fileRoot, relativePath2, lines2);

        // import the file using an absolute path
        resp = assayHelper.importAssay(assayId, pipelinePath2.toString(), getProjectName(), Collections.emptyMap());
        beginAt(resp.getSuccessURL());
        assertTextPresent("p03", "p04");

        // attempt to import file outside of pipeline root
        try
        {
            File runFilePath = TestFileUtils.getSampleData("GPAT/trial01.xls");
            assayHelper.importAssay(assayId, runFilePath.toString(), getProjectName(), Collections.emptyMap());
            fail("Expected exception trying to read file outside of pipeline root");
        }
        catch (CommandException ex)
        {
            assertTrue("Expected 'File not found', got: " + ex.getMessage(), ex.getMessage().contains("File not found"));
            assertTrue("Expected 'trial01.xls', got: " + ex.getMessage(), ex.getMessage().contains("trial01.xls"));
        }
    }

    public static Path createDataFile(File fileRoot, Path relativePath, Iterable<String> lines) throws IOException
    {
        Path pipelinePath = fileRoot.toPath().resolve(relativePath);
        if (!Files.isRegularFile(pipelinePath))
        {
            Files.createDirectories(pipelinePath.getParent());
            Files.write(pipelinePath, lines);
            if (!Files.isRegularFile(pipelinePath))
                fail("Failed to create file " + pipelinePath);
        }
        return pipelinePath;
    }

    private void createAssayWithFileFields(String assayName)
    {
        ReactAssayDesignerPage assayDesigner = _assayHelper.createAssayDesign("General", assayName);

        log("Create a 'File' column for the assay run.");
        assayDesigner.goToRunFields()
                .addField("RunFileField")
                .setType(FieldDefinition.ColumnType.File)
                .setLabel("Run File Field");

        log("Create a 'File' column for the assay data.");
        assayDesigner.goToResultsFields()
                .addField("DataFileField")
                .setType(FieldDefinition.ColumnType.File)
                .setLabel("Data File Field");

        assayDesigner.clickFinish();
    }

    // Issue 22632: import runs into GPAT assay using LABKEY.Assay.importRun() API with data rows
    @Test
    public void testImportRun_dataRows() throws Exception
    {
        goToProjectHome();

        log("create GPAT assay");
        String assayName = "GPAT-ImportRunApi-dataRows" + TRICKY_CHARACTERS;
        createAssayWithFileFields(assayName);

        File fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        Path fullPath = fileRoot.toPath().resolve("screenshot.png");

        List<Map<String, Object>> dataRows = Arrays.asList(
                Maps.of("ptid", "p01", "date", "2017-05-10", "DataFileField", "crest.png"),
                Maps.of("ptid", "p02", "date", "2017-05-10", "DataFileField", fullPath.toString())
        );

        // import the file using a relative path
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        int assayId = assayHelper.getIdFromAssayName(assayName, getProjectName());
        ImportRunResponse resp = assayHelper.importAssay(assayId, "x", dataRows, getProjectName(), Collections.singletonMap("RunFileField", "foo.xls"), Collections.emptyMap());
        beginAt(resp.getSuccessURL());
        assertTextPresent("p01", "p02");

        // verify images are resolved and rendered properly
        assertElementPresent("Did not find the expected number of icons for images for " + CREST_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + CREST_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + SCREENSHOT_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + SCREENSHOT_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + FOO_XLS_FILE.getName() + " from the runs.", Locator.xpath("//a[contains(text(), '" + FOO_XLS_FILE.getName() + "')]"), 2);

        log("verify files can be resolved after the run is imported");
        String runName = "file resolution run";

        dataRows = Arrays.asList(
                Maps.of("ptid", "p03", "date", "2017-05-10", "DataFileField", "crest-2.png")
        );

        // import the file using a relative path
        resp = assayHelper.importAssay(assayId, runName, dataRows, getProjectName(), Collections.singletonMap("RunFileField", "crest-2.png"), Collections.emptyMap());
        beginAt(resp.getSuccessURL());
        assertElementNotPresent("File should not exist for " + CREST_2_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + CREST_2_FILE.getName() + "')]"));

        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(CREST_2_FILE);
        beginAt(resp.getSuccessURL());
        assertElementPresent("Did not find the expected number of icons for " + CREST_2_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + CREST_2_FILE.getName() + "')]"), 2);
    }


    // Issue 21247: Import runs into GPAT assay using LABKEY.Experiment.saveBatch() API
    @Test
    public void testGpatSaveBatch() throws Exception
    {
        goToProjectHome();

        log("create GPAT assay");
        String assayName = "GPAT-SaveBatch" + TRICKY_CHARACTERS;
        createAssayWithFileFields(assayName);

        log("create run via saveBatch");
        String runName = "created-via-saveBatch";
        List<Map<String, Object>> resultRows = new ArrayList<>();
        resultRows.add(Maps.of("ptid", "188438418", "SpecimenID", "K770K3VY-19", "DataFileField", "crest.png"));
        resultRows.add(Maps.of("ptid", "188487431", "SpecimenID", "A770K4W1-15", "DataFileField", "screenshot.png"));

        ((APIAssayHelper) _assayHelper).saveBatch(assayName, runName, Collections.singletonMap("RunFileField", "foo.xls"), resultRows, getProjectName());

        log("verify assay saveBatch worked");
        goToManageAssays();
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText(runName));
        DataRegionTable table = new DataRegionTable("Data", this);
        assertEquals(Arrays.asList("K770K3VY-19", "A770K4W1-15"), table.getColumnDataAsText("SpecimenID"));

        // verify images are resolved and rendered properly
        assertElementPresent("Did not find the expected number of icons for images for " + CREST_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + CREST_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + SCREENSHOT_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + SCREENSHOT_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find the expected number of icons for images for " + FOO_XLS_FILE.getName() + " from the runs.", Locator.xpath("//a[contains(text(), '" + FOO_XLS_FILE.getName() + "')]"), 2);

        log("verify files can be resolved after the run is imported");
        resultRows.clear();
        resultRows.add(Maps.of("ptid", "188438419", "SpecimenID", "K770K3VY-20", "DataFileField", "help.jpg"));

        runName = "file resolution run";
        ((APIAssayHelper) _assayHelper).saveBatch(assayName, runName, Collections.singletonMap("RunFileField", "help.jpg"), resultRows, getProjectName());
        goToManageAssays();
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText(runName));
        assertElementNotPresent("File should not exist for " + HELP_ICON_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + HELP_ICON_FILE.getName() + "')]"));

        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(HELP_ICON_FILE);
        goToManageAssays();
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText(runName));
        assertElementPresent("Did not find the expected number of icons for " + HELP_ICON_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + HELP_ICON_FILE.getName() + "')]"), 2);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }
}
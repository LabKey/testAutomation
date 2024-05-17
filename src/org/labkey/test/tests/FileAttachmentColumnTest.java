/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class FileAttachmentColumnTest extends BaseWebDriverTest
{
    private final String FOLDER_NAME = "TestFolder";
    private final String LIST_NAME = "TestList";
    private final String LIST_KEY = "TestListId";
    private final String SAMPLESET_NAME = "FileSamples";
    private final File DATAFILE_DIRECTORY = TestFileUtils.getSampleData("fileTypes");
    private final File SAMPLE_CSV = new File(DATAFILE_DIRECTORY, "csv_sample.csv");
    private final File SAMPLE_JPG = new File(DATAFILE_DIRECTORY, "jpg_sample.jpg");
    private final File SAMPLE_PDF = new File(DATAFILE_DIRECTORY, "pdf_sample.pdf");
    private final File SAMPLE_TIF = new File(DATAFILE_DIRECTORY, "tif_sample.tif");

    @Test
    public void verifyFileDownloadOnClick()
    {
        clickAndWait(Locator.linkWithText(LIST_NAME));
        DataRegionTable testListRegion = new DataRegionTable("query", getDriver()); // Just make sure the DRT is ready

        // verify file download behavior for csv, tif
        doAndWaitForDownload(()->click(Locator.linkContainingText(SAMPLE_CSV.getName())));
        doAndWaitForDownload(()->click(Locator.linkContainingText(SAMPLE_TIF.getName())));
        doAndWaitForDownload(()->click(Locator.linkContainingText(SAMPLE_PDF.getName())));

        // verify popup/sprite for jpeg
        mouseOver(Locator.tagWithAttribute("img", "title", SAMPLE_JPG.getName()));
        shortWait().until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div/span[contains(text(),'" + SAMPLE_JPG.getName() + "')]")));
        mouseOut();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        FileAttachmentColumnTest init = (FileAttachmentColumnTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "Custom");
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME);
        _containerHelper.enableModules(Arrays.asList("Experiment", "Pipeline", "Portal"));

        //create list with attachment columns
        createList();

        //create sample type with file columns
        createSampleType();
    }

    private void createList()
    {
        beginAt(getProjectName() + "/" + FOLDER_NAME + "/project-begin.view");
        clickTab("Portal");

        ListHelper listHelper = new ListHelper(getDriver());
        String containerPath = getProjectName() + "/" + FOLDER_NAME;
        listHelper.createList(containerPath, LIST_NAME, LIST_KEY, new FieldDefinition("Name", ColumnType.String), new FieldDefinition("File", ColumnType.Attachment));
        goToManageLists();
        listHelper.click(Locator.linkContainingText(LIST_NAME));
        // todo: import actual data here
        Map<String, String> csvRow = new HashMap<>();
        csvRow.put("Name", "csv file");
        csvRow.put("File", SAMPLE_CSV.getAbsolutePath());
        Map<String, String> jpgRow = new HashMap<>();
        jpgRow.put("Name", "jpeg file");
        jpgRow.put("File", SAMPLE_JPG.getAbsolutePath());
        Map<String, String> pdfRow = new HashMap<>();
        pdfRow.put("Name", "pdf file");
        pdfRow.put("File", SAMPLE_PDF.getAbsolutePath());
        Map<String, String> tifRow = new HashMap<>();
        tifRow.put("Name", "tif file");
        tifRow.put("File", SAMPLE_TIF.getAbsolutePath());
        listHelper.insertNewRow(csvRow, false);
        listHelper.insertNewRow(jpgRow, false);
        listHelper.insertNewRow(pdfRow, false);
        listHelper.insertNewRow(tifRow, false);
    }

    private void createSampleType()
    {
        beginAt(getProjectName() + "/" + FOLDER_NAME + "/project-begin.view");
        clickTab("Portal");

        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.addWebPart("Sample Types");

        log("adding sample type with file column");

        SampleTypeHelper sampleHelper = new SampleTypeHelper(this);
        sampleHelper.createSampleType(new SampleTypeDefinition(SAMPLESET_NAME).setFields(List.of(new FieldDefinition("color", ColumnType.String))), Collections.singletonList(Map.of("Name", "ed", "color", "green")));

        // add a 'file' column
        log("editing fields for sample type");
        clickFolder(FOLDER_NAME);
        UpdateSampleTypePage updatePage = sampleHelper.goToEditSampleType(SAMPLESET_NAME);
        updatePage.addFields(List.of(new FieldDefinition("File", ColumnType.File)));
        updatePage.clickSave();

        StringBuilder sb = new StringBuilder("Name\tcolor\tfile\n");
        for (File file : DATAFILE_DIRECTORY.listFiles())
        {
            String newRow = file.getName() + "\tred\t\"" + file.getPath() + "\"\n";
            sb.append(newRow);
        }
        sampleHelper.bulkImport(sb.toString());
    }

    @Before
    public void preTest()
    {
        beginAt(getProjectName() + "/" + FOLDER_NAME + "/project-begin.view");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "FileAndAttachmentColumns Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("Experiment", "Pipeline", "Portal");
    }
}

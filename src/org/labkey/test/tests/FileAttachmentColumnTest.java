/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
import org.labkey.test.categories.DailyC;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({DailyC.class})
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

        //create sampleset with file columns
        createSampleSet();
    }

    private void createList()
    {
        beginAt("/project/" + getProjectName() +"/"+ FOLDER_NAME + "/begin.view?");
        clickTab("Portal");
        ListHelper listHelper = new ListHelper(getDriver());

        listHelper.createList(getProjectName() + "/" + FOLDER_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, LIST_KEY,
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String),
                new ListHelper.ListColumn("File", "File", ListHelper.ListColumnType.Attachment));
        clickButton("Done");
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

    private void createSampleSet()
    {
        beginAt("/project/" + getProjectName() +"/"+ FOLDER_NAME + "/begin.view?");
        clickTab("Portal");

        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.addWebPart("Sample Sets");

        log("adding sample set with file column");
        String vialSetsData = "Name\tcolor\n" +
                "ed\tgreen\n";
        DataRegionTable sampleSetsTable = new DataRegionTable("SampleSet", getDriver());
        sampleSetsTable.clickHeaderButtonByText("Import Sample Set");
        setFormElement(Locator.xpath("//input[@id='name']"), SAMPLESET_NAME);
        setFormElement(Locator.xpath("//textarea[@id='textbox']"), vialSetsData);

        // set id row
        selectOptionByText(Locator.xpath("//select[@id='idCol1']"), "Name");
        // leave parent empty
        clickButton("Submit");

        // add a 'file' column
        log("editing fields in Vial Groups");
        ListHelper vialGroupsFieldEditHelper = new ListHelper(getDriver()).withEditorTitle("Field Properties");
        vialGroupsFieldEditHelper.clickEditFields();
        vialGroupsFieldEditHelper.addField(new ListHelper.ListColumn("File", ListHelper.ListColumnType.File));
        clickButton("Save");

        DataRegionTable samplesTable = new DataRegionTable("Material", getDriver());
        samplesTable.clickImportBulkData();

        StringBuilder sb = new StringBuilder("Name\tcolor\tfile\n");
        for (File file : DATAFILE_DIRECTORY.listFiles())
        {
            String newRow = file.getName() + "\tred\t\"" + file.getPath() + "\"\n";
            sb.append(newRow);
        }
        checkRadioButton(Locator.radioButtonById("insertIgnoreChoice"));
        setFormElement(Locator.xpath("//textarea[@id='textbox']"), sb.toString());
        clickButton("Submit");
    }

    @Before
    public void preTest()
    {
        beginAt("/project/" + getProjectName() +"/"+ FOLDER_NAME + "/begin.view?");
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

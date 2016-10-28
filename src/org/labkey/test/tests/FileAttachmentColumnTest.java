/*
 * Copyright (c) 2016 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FileAttachmentColumnTest extends BaseWebDriverTest
{
    private final String FOLDER_NAME = "TestFolder";
    private final String LIST_NAME = "TestList";
    private final String LIST_KEY = "TestListId";
    private final String SAMPLESET_NAME = "FileSamples";
    private final String DATAFILE_DIRECTORY = TestFileUtils.getLabKeyRoot() + "\\sampledata\\fileTypes\\";
    private List<Map<String, String>> LIST_CONTENTS = new ArrayList<>();

    @Test
    public void verifyFileDownloadOnClick()
    {
        clickAndWait(Locator.linkWithText(LIST_NAME));
        DataRegionTable testListRegion = new DataRegionTable("query", getDriver());

        // verify file download behavior for csv, tif
        doAndWaitForDownload(()->click(Locator.linkContainingText("csv_sample.csv")));
        doAndWaitForDownload(()->click(Locator.linkContainingText("tif_sample.tif")));

        // expected behavior for pdf: render as a web page.
        pushLocation();
        doAndWaitForPageToLoad(()->click(Locator.linkContainingText("pdf_sample.pdf")));
        // todo: verify file contents
        popLocation();

        // verify popup/sprite for jpeg
        mouseOver(Locator.xpath("//img[@title='jpg_sample.jpg']"));
        shortWait().until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div/span[contains(text(),'jpg_sample.jpg')]")));
        mouseOut();
        String foo="stop here";
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

        listHelper.createList(FOLDER_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, LIST_KEY,
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String),
                new ListHelper.ListColumn("File", "File", ListHelper.ListColumnType.Attachment));
        clickButton("Done");
        listHelper.click(Locator.linkContainingText(LIST_NAME));
        // todo: import actual data here
        Map<String, String> csvRow = new HashMap<>();
        csvRow.put("Name", "csv file");
        csvRow.put("File", DATAFILE_DIRECTORY+"csv_sample.csv");
        Map<String, String> jpgRow = new HashMap<>();
        jpgRow.put("Name", "jpeg file");
        jpgRow.put("File", DATAFILE_DIRECTORY+"jpg_sample.jpg");
        Map<String, String> pdfRow = new HashMap<>();
        pdfRow.put("Name", "pdf file");
        pdfRow.put("File", DATAFILE_DIRECTORY+"pdf_sample.pdf");
        Map<String, String> tifRow = new HashMap<>();
        tifRow.put("Name", "tif file");
        tifRow.put("File", DATAFILE_DIRECTORY+"tif_sample.tif");
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
        ListHelper vialGroupsFieldEditHelper = new ListHelper(getDriver());
        vialGroupsFieldEditHelper.clickEditFields();
        vialGroupsFieldEditHelper.addField(new ListHelper.ListColumn("File", ListHelper.ListColumnType.File));
        clickButton("Save");

        DataRegionTable samplesTable = new DataRegionTable("Material", getDriver());
        samplesTable.clickImportBulkDataDropdown();

        StringBuilder sb = new StringBuilder("Name\tcolor\tfile\n");
        for (File file : new File(DATAFILE_DIRECTORY).listFiles())
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

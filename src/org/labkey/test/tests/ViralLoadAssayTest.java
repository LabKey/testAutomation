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

import junit.framework.Assert;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;
import org.labkey.test.util.ext4cmp.Ext4GridRefWD;
import org.openqa.selenium.Alert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 11/6/12
 * Time: 5:43 PM
 */
public class ViralLoadAssayTest extends LabModulesTest
{
    private static final String ASSAY_NAME = "Viral Load Test";
    private String DETECTOR_NAME = "PIATAK SIVGAG";

    private static final String[][] TEMPLATE_DATA = new String[][]{
        {"Well", "Subject Id", "Sample Date", "Category", "Sample Volume (mL)"},
        {"A5", "LowQual1", "2012-02-01", "Unknown", "0.3"},
        {"A6", "LowQual1", "2012-02-01", "Unknown", "0.3"},
        {"A7", "LowQual2", "2012-06-01", "Unknown", "0.3"},
        {"A8", "LowQual2", "2012-06-01", "Unknown", "0.3"},
        {"A9", "Subject1", "2012-02-12", "Unknown", "0.3"},
        {"A10", "Subject1", "2012-02-12", "Unknown", "0.3"},
        {"A11", "Subject2", "2012-02-14", "Unknown", "0.3"},
        {"A12", "Subject2", "2012-02-14", "Unknown", "0.3"},
        {"B1", "Subject3", "2012-02-16", "Unknown", "0.3"},
        {"B2", "Subject3", "2012-02-16", "Unknown", "0.3"},
        {"B3", "Subject4", "2012-02-18", "Unknown", "0.3"},
        {"B4", "Subject4", "2012-02-18", "Unknown", "0.3"},
        {"B5", "Subject5", "2012-02-20", "Unknown", "0.3"},
        {"B6", "Subject5", "2012-02-20", "Unknown", "0.3"},
        {"B7", "Subject6", "2012-02-22", "Unknown", "0.3"},
        {"B8", "Subject6", "2012-02-22", "Unknown", "0.3"},
        {"B9", "Subject7", "2012-02-24", "Unknown", "0.3"},
        {"B10", "Subject7", "2012-02-24", "Unknown", "0.3"},
        {"B11", "Subject8", "2012-02-26", "Unknown", "0.3"},
        {"B12", "Subject8", "2012-02-26", "Unknown", "0.3"},
        {"C1", "Subject9", "2012-02-28", "Unknown", "0.3"},
        {"C2", "Subject9", "2012-02-28", "Unknown", "0.3"},
        {"C3", "Subject10", "2012-03-01", "Unknown", "0.3"},
        {"C4", "Subject10", "2012-03-01", "Unknown", "0.3"},
        {"C5", "Subject11", "2012-03-03", "Unknown", "0.3"},
        {"C6", "Subject11", "2012-03-03", "Unknown", "0.3"},
        {"C7", "Subject12", "2012-03-05", "Unknown", "0.3"},
        {"C8", "Subject12", "2012-03-05", "Unknown", "0.3"},
        {"C9", "Subject13", "2012-03-07", "Unknown", "0.3"},
        {"C10", "Subject13", "2012-03-07", "Unknown", "0.3"},
        {"C11", "Subject14", "2012-03-09", "Unknown", "0.3"},
        {"C12", "Subject14", "2012-03-09", "Unknown", "0.3"},
        {"D1", "Subject15", "2012-03-11", "Unknown", "0.3"},
        {"D2", "Subject15", "2012-03-11", "Unknown", "0.3"},
        {"D3", "Subject16", "2012-03-13", "Unknown", "0.3"},
        {"D4", "Subject16", "2012-03-13", "Unknown", "0.3"},
        {"D5", "Subject17", "2012-03-15", "Unknown", "0.3"},
        {"D6", "Subject17", "2012-03-15", "Unknown", "0.3"},
        {"D7", "Subject18", "2012-03-17", "Unknown", "0.3"},
        {"D8", "Subject18", "2012-03-17", "Unknown", "0.3"},
        {"D9", "Subject19", "2012-03-19", "Unknown", "0.3"},
        {"D10", "Subject19", "2012-03-19", "Unknown", "0.3"},
        {"D11", "Subject20", "2012-03-21", "Unknown", "0.3"},
        {"D12", "Subject20", "2012-03-21", "Unknown", "0.3"},
        {"E1", "Subject21", "2012-03-23", "Unknown", "0.3"},
        {"E2", "Subject21", "2012-03-23", "Unknown", "0.3"},
        {"E3", "Subject22", "2012-03-25", "Unknown", "0.3"},
        {"E4", "Subject22", "2012-03-25", "Unknown", "0.3"},
        {"E5", "Subject23", "2012-03-27", "Unknown", "0.3"},
        {"E6", "Subject23", "2012-03-27", "Unknown", "0.3"},
        {"E7", "Subject24", "2012-03-29", "Unknown", "0.3"},
        {"E8", "Subject24", "2012-03-29", "Unknown", "0.3"},
        {"E9", "Subject25", "2012-03-31", "Unknown", "0.3"},
        {"E10", "Subject25", "2012-03-31", "Unknown", "0.3"},
        {"E11", "Subject26", "2012-04-02", "Unknown", "0.3"},
        {"E12", "Subject26", "2012-04-02", "Unknown", "0.3"},
        {"F1", "Subject27", "2012-04-04", "Unknown", "0.3"},
        {"F2", "Subject27", "2012-04-04", "Unknown", "0.3"},
        {"F3", "Subject28", "2012-04-06", "Unknown", "0.3"},
        {"F4", "Subject28", "2012-04-06", "Unknown", "0.3"},
        {"F5", "Subject29", "2012-04-08", "Unknown", "0.3"},
        {"F6", "Subject29", "2012-04-08", "Unknown", "0.3"},
        {"F7", "Subject30", "2012-04-10", "Unknown", "0.3"},
        {"F8", "Subject30", "2012-04-10", "Unknown", "0.3"},
        {"F9", "Positive Control-1", "2012-04-12", "Pos Control", "0.3"},
        {"F10", "Positive Control-1", "2012-04-12", "Pos Control", "0.3"},
        {"F11", "Positive Control-2", "2012-04-14", "Pos Control", "0.3"},
        {"F12", "Positive Control-2", "2012-04-14", "Pos Control", "0.3"},
        {"G1", "NTC", "", "Neg Control", "0.3"},
        {"G2", "NTC", "", "Neg Control", "0.3"},
        {"G3", "STD_1000000", "", "Standard", "0.3"},
        {"G4", "STD_1000000", "", "Standard", "0.3"},
        {"G5", "STD_320000", "", "Standard", "0.3"},
        {"G6", "STD_320000", "", "Standard", "0.3"},
        {"G7", "STD_100000", "", "Standard", "0.3"},
        {"G8", "STD_100000", "", "Standard", "0.3"},
        {"G9", "STD_32000", "", "Standard", "0.3"},
        {"G10", "STD_32000", "", "Standard", "0.3"},
        {"G11", "STD_10000", "", "Standard", "0.3"},
        {"G12", "STD_10000", "", "Standard", "0.3"},
        {"H1", "STD_3200", "", "Standard", "0.3"},
        {"H2", "STD_3200", "", "Standard", "0.3"},
        {"H3", "STD_1000", "", "Standard", "0.3"},
        {"H4", "STD_1000", "", "Standard", "0.3"},
        {"H5", "STD_320", "", "Standard", "0.3"},
        {"H6", "STD_320", "", "Standard", "0.3"},
        {"H7", "STD_100", "", "Standard", "0.3"},
        {"H8", "STD_100", "", "Standard", "0.3"},
        {"H9", "STD_32", "", "Standard", "0.3"},
        {"H10", "STD_32", "", "Standard", "0.3"},
        {"H11", "STD_10", "", "Standard", "0.3"},
        {"H12", "STD_10", "", "Standard", "0.3"}
    };

    public ViralLoadAssayTest()
    {
        setContainerHelper(new UIContainerHelper(this));
        PROJECT_NAME = "VL_AssayVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUpTest();
        createPlateTemplate();
        importABI7500Results();
        importLightCyclerRun();
        importLC480Run();

        testDefaultImportMethod();
    }

    @Override
    protected void setUpTest() throws Exception
    {
        super.setUpTest();

        ensureABI7500Records();
    }

    private void ensureABI7500Records() throws Exception
    {
        String query = "abi7500_detectors";
        String schema = "viral_load_assay";
        String assayName = "SIVmac239-Gag";

        log("Inserting initial records into ABI7500 Detectors table");

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        SelectRowsCommand selectCmd = new SelectRowsCommand(schema, query);
        selectCmd.addFilter(new Filter("assayName", assayName));
        SelectRowsResponse resp = selectCmd.execute(cn, getProjectName());
        Long count = (Long)resp.getRowCount();
        if (count == 0)
        {
            log("Creating ABI7500 detector record");

            InsertRowsCommand insertCmd = new InsertRowsCommand(schema, query);
            Map<String,Object> rowMap = new HashMap<String,Object>();
            rowMap.put("assayName", assayName);
            rowMap.put("detector", DETECTOR_NAME);
            rowMap.put("reporter", "FAM");
            insertCmd.addRow(rowMap);
            SaveRowsResponse saveResp = insertCmd.execute(cn, getProjectName());
            Assert.assertEquals("Prolem creating record", saveResp.getRowsAffected(), (long)1);
        }
        else
        {
            log("ABI7500 already exists, no action needed");
            Map<String, Object> row = resp.getRows().get(0);
            DETECTOR_NAME = (String)row.get("detector");
        }
    }
    private void createPlateTemplate()
    {
        _helper.goToLabHome();
        _helper.clickNavPanelItem(ASSAY_NAME + ":", IMPORT_DATA_TEXT);
        _ext4Helper.clickExt4MenuItem("Prepare Run");
        waitForElement(Ext4Helper.ext4Window(IMPORT_DATA_TEXT));
        waitAndClick(Locator.ext4Button("Submit"));
        waitForPageToLoad();

        List<String> expectedCols = new ArrayList<String>();
        expectedCols.add("well");
        expectedCols.add("category");
        expectedCols.add("subjectId");
        expectedCols.add("date");
        expectedCols.add("sampleVol");
        expectedCols.add("comment");
        expectedCols.add("sampleId");

        waitForElement(Locator.xpath("//span[contains(text(), 'Freezer Id') and contains(@class, 'x4-column-header-text')]")); //ensure grid loaded
        _helper.addRecordsToAssayTemplate(TEMPLATE_DATA, expectedCols);

        waitAndClick(Locator.ext4Button("Plate Layout"));
        waitForElement(Ext4Helper.ext4Window("Configure Plate"));
        waitForText("Group By Category");
        Ext4FieldRefWD.getForLabel(this, "Group By Category").setChecked(true);
        waitForText("Below are the sample categories");
        Ext4FieldRefWD ctlField = Ext4FieldRefWD.getForLabel(this, "Neg Control (2)");
        ctlField.setValue(8); //A8
        waitAndClick(Locator.ext4Button("Submit"));
        assertAlert("Error: Neg Control conflicts with an existing sample in well: A8");

        ctlField.setValue(73); //corresponds to G1
        waitAndClick(Locator.ext4Button("Submit"));

        waitForElement(_helper.getAssayWell("G1", LabModuleHelper.NEG_COLOR));
        assertElementPresent(_helper.getAssayWell("G1", LabModuleHelper.NEG_COLOR));
        assertElementPresent(_helper.getAssayWell("F12", LabModuleHelper.POS_COLOR));
        assertElementPresent(_helper.getAssayWell("B5", LabModuleHelper.UNKNOWN_COLOR));
        assertElementPresent(_helper.getAssayWell("G3", LabModuleHelper.STD_COLOR));
        assertElementPresent(_helper.getAssayWell("H12", LabModuleHelper.STD_COLOR));

        Ext4FieldRefWD.getForLabel(this, "Run Name").setValue("TestRun");

        waitAndClick(Locator.ext4Button("Save and Close"));
        waitForText("Save Complete");
        waitAndClick(Locator.ext4Button("OK"));
        waitForPageToLoad();

        //verify template created
        _helper.clickNavPanelItem(ASSAY_NAME + ":", IMPORT_DATA_TEXT);
        _ext4Helper.clickExt4MenuItem("View Planned Runs");
        waitForPageToLoad();

        log("Reopening saved run plan");
        DataRegionTable dr = new DataRegionTable("query", this);
        dr.clickLink(0, 0);
        waitForPageToLoad();

        //ensure previous run loads correctly
        waitForElement(_helper.getAssayWell("G1", LabModuleHelper.NEG_COLOR), WAIT_FOR_PAGE);
        assertElementPresent(_helper.getAssayWell("G1", LabModuleHelper.NEG_COLOR));
        assertElementPresent(_helper.getAssayWell("B5", LabModuleHelper.UNKNOWN_COLOR));
        assertElementPresent(_helper.getAssayWell("G3", LabModuleHelper.STD_COLOR));
        assertElementPresent(_helper.getAssayWell("H12", LabModuleHelper.STD_COLOR));

        //verify duplicates not allowed
        Ext4GridRefWD grid = _ext4Helper.queryOne("grid", Ext4GridRefWD.class);
        grid.setGridCell(1, 1, "H11");
        click(Locator.ext4Button("Download"));
        assertAlert("There was an error downloading the template");
        assertTextPresent("another sample is already present in well: H11");
        //restore original contents
        grid.setGridCell(1, 1, "A5");

        //TODO: test other error messages including a run lacking any controls, required values, also verify well matching template, etc.
//        click(Locator.ext4Button("Download"));
//       grid.setGridCell(1, 2, "");
//        grid.setGridCell(1, 2, "Subject1");
//        assertAlert("There was an error downloading the template");
//        assertTextPresent("another sample is already present in well: H11");

        //save valid data
        click(Locator.ext4Button("Save"));
        waitAndClick(Locator.ext4Button("OK"));

        //test download
        String url = getCurrentRelativeURL();
        url += "&exportAsWebPage=1";
        beginAt(url);
        waitForElement(_helper.getAssayWell("G1", LabModuleHelper.NEG_COLOR), WAIT_FOR_PAGE);

        waitAndClick(Locator.ext4Button("Download"));
        waitForPageToLoad();

        int i = 1;

        Map<String, String> categoryMap = new HashMap<String, String>();
        categoryMap.put("Unknown", "UNKN");
        categoryMap.put("Standard", "STND");
        categoryMap.put("Pos Control", "UNKN");
        categoryMap.put("Neg Control", "NTC");
        String delim = "\t";

        while (i < TEMPLATE_DATA.length)
        {
            String[] row = TEMPLATE_DATA[i];
            StringBuilder sb = new StringBuilder();
            sb.append(row[1]);
            if (row[3].equals("Unknown") || row[3].equals("Pos Control"))
            {
                sb.append("_").append(row[2]);
            }
            sb.append(delim);
            sb.append(DETECTOR_NAME);
            sb.append(delim);
            sb.append(categoryMap.get(row[3]));

            if (row[3].equalsIgnoreCase("Standard"))
            {
                String[] parts = row[1].split("_");
                sb.append(delim).append(parts[1]);
            }

            assertTextPresent(sb.toString());

            i++;
        }

        i = 1;
        while (i < 5)
        {
            assertTextPresent(i + delim + "empty");
            i++;
        }

        beginAt(getProjectUrl());
    }

    /**
     * Imports results for the run created using createPlateTemplate().
     * Also verifies error messages and VL calculations
     */
    private void importABI7500Results()
    {
        log("Verifying ABI7500 Import");
        _helper.goToLabHome();
        _helper.clickNavPanelItem(ASSAY_NAME + ":", IMPORT_DATA_TEXT);
        _ext4Helper.clickExt4MenuItem("View Planned Runs");
        waitForPageToLoad();

        log("Entering results for saved run");
        DataRegionTable templates = new DataRegionTable("query", this);
        templates.clickLink(0, 1);
        waitForPageToLoad();
        
        //use the same data included with this assay
        Locator btn = Locator.xpath("//span[text() = 'Download Example Data']");
        waitForElement(btn);

        Assert.assertEquals("Incorrect value for field", "ABI 7500", Ext4FieldRefWD.getForLabel(this, "Instrument").getValue());
        Assert.assertEquals("Incorrect value for field", new Long(60), Ext4FieldRefWD.getForLabel(this, "Eluate Volume").getValue());
        Assert.assertEquals("Incorrect value for field", new Long(20), Ext4FieldRefWD.getForLabel(this, "Sample Vol Per Rxn").getValue());
        waitAndClick(btn);

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        String text = _helper.getExampleData();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("A1=231841", "");
        errorText = errorText.replaceAll("A3=432947\tDETECTOR1", "A3=432947\tDETECTOR2");
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Upload Failed"));
        click(Locator.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        assertTextPresent("Missing sample name for row: 9");
        assertTextPresent("Row 11: Unable to find detector information for detector: DETECTOR2");
        assertTextPresent("Row 12: Unable to find detector information for detector: DETECTOR2");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText("Import Samples");

        log("Verifying results");
        DataRegionTable dr = _helper.getDrForQueryWebpart("Experiment Runs (" + ASSAY_NAME + ")");
        dr.clickLink(0, 1);
        waitForPageToLoad();
        DataRegionTable results = new DataRegionTable("Data", this);

        int totalRows = 88;
        Map<String, String[]> expected = new LinkedHashMap<String, String[]>();
        expected.put("0", new String[]{"Subject1", "Unknown", "2012-02-11", "7187000"});
        expected.put("1", new String[]{"Subject1", "Unknown", "2012-02-11", "7999000"});
        expected.put("2", new String[]{"Subject2", "Unknown", "2012-02-13", "58730"});
        expected.put("3", new String[]{"Subject2", "Unknown", "2012-02-13", "86430"});
        expected.put("4", new String[]{"Subject3", "Unknown", "2012-02-15", "108300"});
        expected.put("5", new String[]{"Subject3", "Unknown", "2012-02-15", "79390"});
        expected.put("6", new String[]{"Subject4", "Unknown", "2012-02-17", "3376"});
        expected.put("7", new String[]{"Subject4", "Unknown", "2012-02-17", "3606"});
        expected.put("8", new String[]{"Subject5", "Unknown", "2012-02-19", "23760"});
        expected.put("9", new String[]{"Subject5", "Unknown", "2012-02-19", "25180"});
        expected.put("10", new String[]{"Subject6", "Unknown", "2012-02-21", "1066"});
        expected.put("11", new String[]{"Subject6", "Unknown", "2012-02-21", "1017"});
        expected.put("12", new String[]{"Subject7", "Unknown", "2012-02-23", "185300"});
        expected.put("13", new String[]{"Subject7", "Unknown", "2012-02-23", "143600"});
        expected.put("14", new String[]{"Subject8", "Unknown", "2012-02-25", "612.8"});
        expected.put("15", new String[]{"Subject8", "Unknown", "2012-02-25", "640.7"});
        expected.put("16", new String[]{"Subject9", "Unknown", "2012-02-27", "227.3"});
        expected.put("17", new String[]{"Subject9", "Unknown", "2012-02-27", "259.5"});
        expected.put("18", new String[]{"Subject10", "Unknown", "2012-02-29", "397300"});
        expected.put("19", new String[]{"Subject10", "Unknown", "2012-02-29", "396100"});
        expected.put("20", new String[]{"Subject11", "Unknown", "2012-03-02", "285100"});
        expected.put("21", new String[]{"Subject11", "Unknown", "2012-03-02", "312300"});
        expected.put("22", new String[]{"Subject12", "Unknown", "2012-03-04", "537900"});
        expected.put("23", new String[]{"Subject12", "Unknown", "2012-03-04", "616500"});
        expected.put("24", new String[]{"Subject13", "Unknown", "2012-03-06", "329.1"});
        expected.put("25", new String[]{"Subject13", "Unknown", "2012-03-06", "393.7"});
        expected.put("26", new String[]{"Subject14", "Unknown", "2012-03-08", "1253000"});
        expected.put("27", new String[]{"Subject14", "Unknown", "2012-03-08", "1568000"});
        expected.put("28", new String[]{"Subject15", "Unknown", "2012-03-10", "8382"});
        expected.put("29", new String[]{"Subject15", "Unknown", "2012-03-10", "11970"});
        expected.put("30", new String[]{"Subject16", "Unknown", "2012-03-12", "0"});
        expected.put("31", new String[]{"Subject16", "Unknown", "2012-03-12", "0"});
        expected.put("32", new String[]{"Subject17", "Unknown", "2012-03-14", "28890"});
        expected.put("33", new String[]{"Subject17", "Unknown", "2012-03-14", "36380"});
        expected.put("34", new String[]{"Subject18", "Unknown", "2012-03-16", "2291"});
        expected.put("35", new String[]{"Subject18", "Unknown", "2012-03-16", "2088"});
        expected.put("36", new String[]{"Subject19", "Unknown", "2012-03-18", "359200"});
        expected.put("37", new String[]{"Subject19", "Unknown", "2012-03-18", "301900"});
        expected.put("38", new String[]{"Subject20", "Unknown", "2012-03-20", "29820"});
        expected.put("39", new String[]{"Subject20", "Unknown", "2012-03-20", "29210"});
        expected.put("40", new String[]{"Subject21", "Unknown", "2012-03-22", "38790"});
        expected.put("41", new String[]{"Subject21", "Unknown", "2012-03-22", "30000"});
        expected.put("42", new String[]{"Subject22", "Unknown", "2012-03-24", "1852"});
        expected.put("43", new String[]{"Subject22", "Unknown", "2012-03-24", "2314"});
        expected.put("44", new String[]{"Subject23", "Unknown", "2012-03-26", "28090"});
        expected.put("45", new String[]{"Subject23", "Unknown", "2012-03-26", "27310"});
        expected.put("46", new String[]{"Subject24", "Unknown", "2012-03-28", "0"});
        expected.put("47", new String[]{"Subject24", "Unknown", "2012-03-28", "0"});
        expected.put("48", new String[]{"Subject25", "Unknown", "2012-03-30", "59640"});
        expected.put("49", new String[]{"Subject25", "Unknown", "2012-03-30", "58800"});
        expected.put("50", new String[]{"Subject26", "Unknown", "2012-04-01", "425100"});
        expected.put("51", new String[]{"Subject26", "Unknown", "2012-04-01", "740400"});
        expected.put("52", new String[]{"Subject27", "Unknown", "2012-04-03", "60650"});
        expected.put("53", new String[]{"Subject27", "Unknown", "2012-04-03", "56570"});
        expected.put("54", new String[]{"Subject28", "Unknown", "2012-04-05", "147100"});
        expected.put("55", new String[]{"Subject28", "Unknown", "2012-04-05", "107200"});
        expected.put("56", new String[]{"Subject29", "Unknown", "2012-04-07", "9362"});
        expected.put("57", new String[]{"Subject29", "Unknown", "2012-04-07", "10670"});
        expected.put("58", new String[]{"Subject30", "Unknown", "2012-04-09", "670300"});
        expected.put("59", new String[]{"Subject30", "Unknown", "2012-04-09", "569600"});
        expected.put("60", new String[]{"Positive Control-1", "Unknown", "2012-04-11", "117800"});
        expected.put("61", new String[]{"Positive Control-1", "Unknown", "2012-04-11", "140700"});
        expected.put("62", new String[]{"Positive Control-2", "Unknown", "2012-04-13", "90090"});
        expected.put("63", new String[]{"Positive Control-2", "Unknown", "2012-04-13", "128700"});
        expected.put("64", new String[]{"NTC", "Neg Control", "", "0"});
        expected.put("65", new String[]{"NTC", "Neg Control", "", "0"});
        expected.put("66", new String[]{"STD_1000000", "Standard", "", "8328000"});
        expected.put("67", new String[]{"STD_1000000", "Standard", "", "9216000"});
        expected.put("68", new String[]{"STD_320000", "Standard", "", "2529000"});
        expected.put("69", new String[]{"STD_320000", "Standard", "", "3637000"});
        expected.put("70", new String[]{"STD_100000", "Standard", "", "1077000"});
        expected.put("71", new String[]{"STD_100000", "Standard", "", "1250000"});
        expected.put("72", new String[]{"STD_32000", "Standard", "", "365300"});
        expected.put("73", new String[]{"STD_32000", "Standard", "", "301700"});
        expected.put("74", new String[]{"STD_10000", "Standard", "", "111300"});
        expected.put("75", new String[]{"STD_10000", "Standard", "", "120500"});
        expected.put("76", new String[]{"STD_3200", "Standard", "", "25930"});
        expected.put("77", new String[]{"STD_3200", "Standard", "", "30320"});
        expected.put("78", new String[]{"STD_1000", "Standard", "", "7519"});
        expected.put("79", new String[]{"STD_1000", "Standard", "", "11990"});
        expected.put("80", new String[]{"STD_320", "Standard", "", "3740"});
        expected.put("81", new String[]{"STD_320", "Standard", "", "4118"});
        expected.put("82", new String[]{"STD_100", "Standard", "", "847.9"});
        expected.put("83", new String[]{"STD_100", "Standard", "", "971.1"});
        expected.put("84", new String[]{"STD_32", "Standard", "", "349.5"});
        expected.put("85", new String[]{"STD_32", "Standard", "", "256"});
        expected.put("86", new String[]{"STD_10", "Standard", "", "111.5"});
        expected.put("87", new String[]{"STD_10", "Standard", "", "89.54"});

        verifyImportedVLs(totalRows, expected, results, null);

        log("verifying run plan marked as complete");
        _helper.goToLabHome();
        _helper.clickNavPanelItem(ASSAY_NAME + ":", IMPORT_DATA_TEXT);
        _ext4Helper.clickExt4MenuItem("View Planned Runs");
        waitForPageToLoad();
        DataRegionTable dr2 = new DataRegionTable("query", this);
        Assert.assertEquals("Run plan not marked completed", 0, dr2.getDataRowCount());
    }

    private void importLC480Run()
    {
        log("Verifying LC480 Import");
        _helper.goToAssayResultImport(ASSAY_NAME);

        //a proxy for page loading
        _helper.waitForField("Source Material");

        //switch import method
        Ext4FieldRefWD field = Ext4FieldRefWD.getForBoxLabel(this, "LC480");
        field.setChecked(true);
        Locator btn = Locator.xpath("//span[text() = 'Download Example Data']");
        waitForElement(btn);

        Ext4FieldRefWD.getForLabel(this, "Run Description").setValue("Description");

        Assert.assertEquals("Incorrect value for field", "LC480", Ext4FieldRefWD.getForLabel(this, "Instrument").getValue());
        Assert.assertEquals("Incorrect value for field", new Long(50), Ext4FieldRefWD.getForLabel(this, "Eluate Volume").getValue());
        Assert.assertEquals("Incorrect value for field", new Long(5), Ext4FieldRefWD.getForLabel(this, "Sample Vol Per Rxn").getValue());
        waitAndClick(btn);

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        String text = _helper.getExampleData();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("\t35.85\t3.01E1\t0", "");
        errorText = errorText.replaceAll("d56053_2010.04.21_1_JBS", "");
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Upload Failed"));
        click(Locator.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        assertTextPresent("Missing sample name for row: 17");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText("Import Samples");

        log("Verifying results");
        DataRegionTable dr = _helper.getDrForQueryWebpart("Experiment Runs (" + ASSAY_NAME + ")");
        dr.clickLink(0, 1);
        waitForPageToLoad();
        DataRegionTable results = new DataRegionTable("Data", this);

        int totalRows = 38;
        Map<String, String[]> expected = new HashMap<String, String[]>();
        expected.put("STD_15000000", new String[]{"STD_15000000", "Standard", "", "176000000"});
        expected.put("STD_5", new String[]{"STD_5", "Standard", "", "56.5"});
        expected.put("STD_3", new String[]{"STD_3", "Standard", "", "30"});
        expected.put("STD_0", new String[]{"STD_0", "Standard", "", "0"});
        expected.put("sd0159", new String[]{"sd0159", "Unknown", "2010-04-19", "793000"});
        expected.put("sd0160", new String[]{"sd0160", "Unknown", "2010-04-19", "0"});
        expected.put("sd0338", new String[]{"sd0338", "Unknown", "2010-04-19", "0"});
        expected.put("sd0339", new String[]{"sd0339", "Unknown", "2010-04-19", "0"});
        expected.put("sd0340", new String[]{"sd0340", "Unknown", "2010-04-19", "0"});
        expected.put("sd0341", new String[]{"sd0341", "Unknown", "2010-04-19", "0"});
        expected.put("sd0345", new String[]{"sd0345", "Unknown", "2010-04-19", "0"});
        expected.put("sd0346", new String[]{"sd0346", "Unknown", "2010-04-19", "0"});
        expected.put("deAJ11", new String[]{"deAJ11", "Unknown", "2010-04-21", "0"});
        expected.put("d90480", new String[]{"d90480", "Unknown", "2010-04-21", "0"});
        expected.put("d95149", new String[]{"d95149", "Unknown", "2010-04-21", "12000"});
        expected.put("d96061", new String[]{"d96061", "Unknown", "2010-04-21", "0"});
        expected.put("d56053", new String[]{"d56053", "Unknown", "2010-04-21", "560000"});
        expected.put("d28016", new String[]{"d28016", "Unknown", "2010-04-21", "4150"});
        expected.put("d98037", new String[]{"d98037", "Unknown", "2010-04-21", "17300"});
        expected.put("d96006", new String[]{"d96006", "Unknown", "2010-04-21", "301"});
        expected.put("d95019", new String[]{"d95019", "Unknown", "2010-04-21", "396000"});
        expected.put("d04032", new String[]{"d04032", "Unknown", "2010-04-21", "194000"});
        expected.put("d03019", new String[]{"d03019", "Unknown", "2010-04-21", "0"});
        expected.put("CTL_negative", new String[]{"CTL_negative", "Control", "", "0"});
        expected.put("CTL_negative", new String[]{"CTL_negative", "Control", "", "0"});
        expected.put("CTL_d02507", new String[]{"CTL_d02507", "Control", "", "10220"});
        expected.put("CTL_negative", new String[]{"CTL_negative", "Control", "", "0"});
        expected.put("CTL_r02007", new String[]{"CTL_r02007", "Control", "", "46600"});
        expected.put("d02056", new String[]{"d02056", "Unknown", "2010-04-19", "102000"});
        expected.put("d03830", new String[]{"d03830", "Unknown", "2010-04-19", "8450"});
        expected.put("d04291", new String[]{"d04291", "Unknown", "2010-04-19", "252000"});
        expected.put("d03504", new String[]{"d03504", "Unknown", "2010-04-19", "171000"});
        expected.put("dh6U10", new String[]{"dh6U10", "Unknown", "2010-04-20", "1300"});
        expected.put("d95067", new String[]{"d95067", "Unknown", "2010-04-20", "39.04"});
        expected.put("d02088", new String[]{"d02088", "Unknown", "2010-04-20", "1200000"});
        expected.put("d03037", new String[]{"d03037", "Unknown", "2010-04-20", "143.3"});
        expected.put("d04145", new String[]{"d04145", "Unknown", "2010-04-20", "0"});
        expected.put("d01599", new String[]{"d01599", "Unknown", "2010-04-20", "36.64"});

        verifyImportedVLs(totalRows, expected, results, new String[]{"Subject Id"});
    }

    private void importLightCyclerRun()
    {
        log("Verifying Light Cycle Import");

        _helper.goToAssayResultImport(ASSAY_NAME);

        //a proxy for page loading
        _helper.waitForField("Source Material");

        //switch import method
        Ext4FieldRefWD field = Ext4FieldRefWD.getForBoxLabel(this, "Light Cycler");
        field.setChecked(true);
        Locator btn = Locator.xpath("//span[text() = 'Download Example Data']");
        waitForElement(btn);

        //set other field values
        Ext4FieldRefWD.getForLabel(this, "Sample Type").setValue("Serum");
        Assert.assertEquals("Incorrect value for field", "Light Cycler", Ext4FieldRefWD.getForLabel(this, "Instrument").getValue());
        Assert.assertEquals("Incorrect value for field", new Long(50), Ext4FieldRefWD.getForLabel(this, "Eluate Volume").getValue());
        Assert.assertEquals("Incorrect value for field", new Long(5), Ext4FieldRefWD.getForLabel(this, "Sample Vol Per Rxn").getValue());
        waitAndClick(btn);

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        String text = _helper.getExampleData();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("de0114_2008.09.08_1_JG", "de0114_200.09.08_1_JG");
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Upload Failed"));
        click(Locator.ext4Button("OK"));

        errorText = text.replaceAll("CTL_negative", "");
        errorText = errorText.replaceAll("de0115_2008.09.08_1_JG\t\t\t0", "");
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Upload Failed"));
        click(Locator.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        assertTextPresent("Missing sample name for row: 23");
        assertTextPresent("Missing sample name for row: 27");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText("Import Samples");

        log("Verifying results");
        DataRegionTable dr = _helper.getDrForQueryWebpart("Experiment Runs (" + ASSAY_NAME + ")");
        dr.clickLink(0, 1);
        waitForPageToLoad();
        DataRegionTable results = new DataRegionTable("Data", this);

        int totalRows = 28;

        Map<String, String[]> expected = new HashMap<String, String[]>();
        expected.put("CTL_negative", new String[]{"CTL_negative", "Control", "", "0"});
        expected.put("CTL_r02007", new String[]{"CTL_r02007", "Control", "", "85000"});
        expected.put("d02321", new String[]{"d02321", "Unknown", "2008-09-08", "3370000"});
        expected.put("d02589", new String[]{"d02589", "Unknown", "2008-09-08", "0"});
        expected.put("d03103", new String[]{"d03103", "Unknown", "2008-09-08", "0"});
        expected.put("d04061", new String[]{"d04061", "Unknown", "2008-09-08", "0"});
        expected.put("d05099", new String[]{"d05099", "Unknown", "2008-09-08", "0"});
        expected.put("d05114", new String[]{"d05114", "Unknown", "2008-09-08", "0"});
        expected.put("d06012", new String[]{"d06012", "Unknown", "2008-09-08", "14640000"});
        expected.put("d07020", new String[]{"d07020", "Unknown", "2008-09-08", "0"});
        expected.put("d29063", new String[]{"d29063", "Unknown", "2008-09-08", "0"});
        expected.put("d29112", new String[]{"d29112", "Unknown", "2008-09-08", "0"});
        expected.put("d45069", new String[]{"d45069", "Unknown", "2008-09-08", "0"});
        expected.put("de0114", new String[]{"de0114", "Unknown", "2008-09-08", "9320"});
        expected.put("de0115", new String[]{"de0115", "Unknown", "2008-09-08", "0"});
        expected.put("de0150", new String[]{"de0150", "Unknown", "2008-09-08", "1860"});
        expected.put("de0152", new String[]{"de0152", "Unknown", "2008-09-08", "308000"});
        expected.put("de0166", new String[]{"de0166", "Unknown", "2008-09-08", "503000"});
        expected.put("STD_0", new String[]{"STD_0", "Standard", "", "0"});
        expected.put("STD_1.5", new String[]{"STD_1.5", "Standard", "", "0"});
        expected.put("STD_15", new String[]{"STD_15", "Standard", "", "150"});
        expected.put("STD_150", new String[]{"STD_150", "Standard", "", "1410"});
        expected.put("STD_1500", new String[]{"STD_1500", "Standard", "", "15600"});
        expected.put("STD_15000", new String[]{"STD_15000", "Standard", "", "157000"});
        expected.put("STD_150000", new String[]{"STD_150000", "Standard", "", "1530000"});
        expected.put("STD_1500000", new String[]{"STD_1500000", "Standard", "", "14700000"});
        expected.put("STD_15000000", new String[]{"STD_15000000", "Standard", "", "147000000"});
        expected.put("STD_3", new String[]{"STD_3", "Standard", "", "0"});

        String sampleType = results.getDataAsText(2, "Sample Type");
        Assert.assertEquals("Incorrect sample type", "Serum", sampleType);

        verifyImportedVLs(totalRows, expected, results, new String[]{"Subject Id"});
    }

    private void verifyImportedVLs(int totalRows, Map<String, String[]> expected, DataRegionTable results, @Nullable String[] keyFields)
    {
        Assert.assertEquals("Incorrect row count", totalRows, results.getDataRowCount());

        int i = 0;
        while (i < totalRows)
        {
            String subjectId = results.getDataAsText(i, "Subject Id");
            String vl = results.getDataAsText(i, "Viral Load");
            String date = results.getDataAsText(i, "Sample Date");
            String category = results.getDataAsText(i, "Category");
            String[] expectedVals;
            if (keyFields == null)
            {
                expectedVals = expected.get(String.valueOf(i));
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                String delim = "";
                for (String field : keyFields)
                {
                    sb.append(delim).append(results.getDataAsText(i, field));
                    delim = "_";
                }
                expectedVals = expected.get(sb.toString());
            }
            Assert.assertNotNull("Unable to find expected values", expectedVals);

            Assert.assertEquals("Incorrec subjectId", expectedVals[0], subjectId);
            Assert.assertEquals("Incorrect category", expectedVals[1], category);

            if (!("".equals(expectedVals[2]) && " ".equals(date)))
                Assert.assertEquals("Incorrect sample date", expectedVals[2], date);

            Double vl1 = Double.parseDouble(expectedVals[3]);
            Double vl2 = Double.parseDouble(vl);
            Assert.assertEquals("Incorrect VL", vl1, vl2);

            i++;
        }
    }

    private void testDefaultImportMethod()
    {
        log("verifying ability to set default import method");
        _helper.goToLabHome();
        click(Locator.xpath("//a//span[text() = 'Settings']"));
        waitForPageToLoad();
        waitForText("Set Assay Defaults");
        _helper.clickNavPanelItem("Set Assay Defaults");
        waitForPageToLoad();
        String defaultVal = "LC480";
        _helper.waitForField(ASSAY_NAME);
        Ext4FieldRefWD.getForLabel(this, ASSAY_NAME).setValue(defaultVal);
        waitAndClick(Locator.ext4Button("Submit"));

        waitForElement(Ext4Helper.ext4Window("Success"));
        waitAndClick(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText("Types of Data");
        _helper.goToAssayResultImport(ASSAY_NAME);
        _helper.waitForField("Source Material");
        Boolean state = (Boolean)Ext4FieldRefWD.getForBoxLabel(this, defaultVal).getValue();
        Assert.assertTrue("Default method not correct", state);
        beginAt(getProjectUrl());
        Alert alert = _driver.switchTo().alert();
        alert.accept();

    }

    @Override
    protected List<Pair<String, String>> getAssaysToCreate()
    {
        List<Pair<String, String>> assays = new ArrayList<Pair<String, String>>();
        assays.add(Pair.of("Viral Loads", ASSAY_NAME));

        return assays;
    }

    @Override
    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<String>();
        modules.add("Viral_Load_Assay");
        return modules;
    }

    @Override
    public boolean skipViewCheck()
    {
        //the module contains an R report tied to a specific assay name, so view check fails when an assay of that name isnt present
        //when module-based assays can supply reports this should be corrected
        return true;
    }
}

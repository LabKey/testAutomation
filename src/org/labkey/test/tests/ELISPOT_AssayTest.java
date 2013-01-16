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
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;
import org.labkey.test.util.ext4cmp.Ext4GridRefWD;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: bimber
 * Date: 11/19/12
 * Time: 5:06 PM
 */
public class ELISPOT_AssayTest extends AbstractLabModuleAssayTest
{
    private static final String ASSAY_NAME = "ELISPOT Test";
    private static final String[][] TEMPLATE_DATA = new String[][]{
        {"Well", "Subject Id", "Sample Date", "Category", "Peptide", "Cell Number (x10^5)"},
        {"A1", "", "2012-02-07", "Unknown", "3", "1"},
        {"B1", "Subject5", "2012-02-07", "Unknown", "3", "1"},
        {"C1", "Control", "", "Pos Control", "Con A", "1"},
        {"D1", "Control", "", "Neg Control", "No stim", "1"},
        {"E1", "Control", "", "Pos Control", "Con A", "1"},
        {"F1", "Control", "", "Neg Control", "No stim", "1"},
        {"G1", "Subject7", "2012-02-09", "Unknown", "5", "1"},
        {"H1", "Subject7", "2/9/2012", "Unknown", "5", "1"},
        {"A2", "Subject9", "2012-02-11", "Unknown", "7", "1"},
        {"B2", "Subject9", "02/11/2012", "Unknown", "7", "1"},
        {"C2", "Subject11", "2012-02-13", "Unknown", "9", "1"},
        {"D2", "Subject11", "2012-02-13", "Unknown", "9", "1"},
        {"E2", "Subject13", "2012-02-15", "Unknown", "11", "1"},
        {"F2", "Subject13", "2012-02-15", "Unknown", "11", "1"},
        {"G2", "Subject15", "2012-02-17", "Unknown", "13", "1"},
        {"H2", "Subject15", "2012-02-17", "Unknown", "13", "1"},
        {"A3", "Subject17", "2012-02-19", "Unknown", "15", "1"},
        {"B3", "Subject17", "2012-02-19", "Unknown", "15", "1"},
        {"C3", "Subject19", "2012-02-21", "Unknown", "17", "1"},
        {"D3", "Subject19", "2012-02-21", "Unknown", "17", "1"},
        {"E3", "Subject21", "2012-02-23", "Unknown", "19", "1"},
        {"F3", "Subject21", "2012-02-23", "Unknown", "19", "1"},
        {"G3", "Subject23", "2012-02-25", "Unknown", "21", "1"},
        {"H3", "Subject23", "2012-02-25", "Unknown", "21", "1"},
        {"A4", "Subject25", "2012-02-27", "Unknown", "23", "1"},
        {"B4", "Subject25", "2012-02-27", "Unknown", "23", "1"},
        {"C4", "Subject27", "2012-02-29", "Unknown", "25", "1"},
        {"D4", "Subject27", "2012-02-29", "Unknown", "25", "1"},
        {"E4", "Subject29", "2012-03-02", "Unknown", "27", "1"},
        {"F4", "Subject29", "2012-03-02", "Unknown", "27", "1"},
        {"G4", "Subject31", "2012-03-04", "Unknown", "29", "1"},
        {"H4", "Subject31", "2012-03-04", "Unknown", "29", "1"},
        {"A5", "Subject33", "2012-03-06", "Unknown", "31", "1"},
        {"B5", "Subject33", "2012-03-06", "Unknown", "31", "1"},
        {"C5", "Subject35", "2012-03-08", "Unknown", "33", "1"},
        {"D5", "Subject35", "2012-03-08", "Unknown", "33", "1"},
        {"E5", "Subject37", "2012-03-10", "Unknown", "35", "1"},
        {"F5", "Subject37", "2012-03-10", "Unknown", "35", "1"},
        {"G5", "Subject39", "2012-03-12", "Unknown", "37", "1"},
        {"H5", "Subject39", "2012-03-12", "Unknown", "37", "1"},
        {"A6", "Subject41", "2012-03-14", "Unknown", "39", "1"},
        {"B6", "Subject41", "2012-03-14", "Unknown", "39", "1"},
        {"C6", "Subject43", "2012-03-16", "Unknown", "41", "1"},
        {"D6", "Subject43", "2012-03-16", "Unknown", "41", "1"},
        {"E6", "Subject45", "2012-03-18", "Unknown", "43", "1"},
        {"F6", "Subject45", "2012-03-18", "Unknown", "43", "1"},
        {"G6", "Subject47", "2012-03-20", "Unknown", "45", "1"},
        {"H6", "Subject47", "2012-03-20", "Unknown", "45", "1"},
        {"A7", "Subject49", "2012-03-22", "Unknown", "47", "1"},
        {"B7", "Subject49", "2012-03-22", "Unknown", "47", "1"},
        {"C7", "Subject51", "2012-03-24", "Unknown", "49", "1"},
        {"D7", "Subject51", "2012-03-24", "Unknown", "49", "1"},
        {"E7", "Subject53", "2012-03-26", "Unknown", "51", "1"},
        {"F7", "Subject53", "2012-03-26", "Unknown", "51", "1"},
        {"G7", "Subject55", "2012-03-28", "Unknown", "53", "1"},
        {"H7", "Subject55", "2012-03-28", "Unknown", "53", "1"},
        {"A8", "Subject57", "2012-03-30", "Unknown", "55", "1"},
        {"B8", "Subject57", "2012-03-30", "Unknown", "55", "1"},
        {"C8", "Subject59", "2012-04-01", "Unknown", "57", "1"},
        {"D8", "Subject59", "2012-04-01", "Unknown", "57", "1"},
        {"E8", "Subject61", "2012-04-03", "Unknown", "59", "1"},
        {"F8", "Subject61", "2012-04-03", "Unknown", "59", "1"},
        {"G8", "Subject63", "2012-04-05", "Unknown", "61", "1"},
        {"H8", "Subject63", "2012-04-05", "Unknown", "61", "1"},
        {"A9", "Subject65", "2012-04-07", "Unknown", "63", "1"},
        {"B9", "Subject65", "2012-04-07", "Unknown", "63", "1"},
        {"C9", "Subject67", "2012-04-09", "Unknown", "65", "1"},
        {"D9", "Subject67", "2012-04-09", "Unknown", "65", "1"},
        {"E9", "Subject69", "2012-04-11", "Unknown", "67", "1"},
        {"F9", "Subject69", "2012-04-11", "Unknown", "67", "1"},
        {"G9", "Subject71", "2012-04-13", "Unknown", "69", "1"},
        {"H9", "Subject71", "2012-04-13", "Unknown", "69", "1"},
        {"A10", "Subject73", "2012-04-15", "Unknown", "71", "1"},
        {"B10", "Subject73", "2012-04-15", "Unknown", "71", "1"},
        {"C10", "Subject75", "2012-04-17", "Unknown", "73", "1"},
        {"D10", "Subject75", "2012-04-17", "Unknown", "73", "1"},
        {"E10", "Subject77", "2012-04-19", "Unknown", "75", "1"},
        {"F10", "Subject77", "2012-04-19", "Unknown", "75", "1"},
        {"G10", "Subject79", "2012-04-21", "Unknown", "77", "1"},
        {"H10", "Subject79", "2012-04-21", "Unknown", "77", "1"},
        {"A11", "Subject81", "2012-04-23", "Unknown", "79", "1"},
        {"B11", "Subject81", "2012-04-23", "Unknown", "79", "1"},
        {"C11", "Subject83", "2012-04-25", "Unknown", "81", "1"},
        {"D11", "Subject83", "2012-04-25", "Unknown", "81", "1"},
        {"E11", "Subject85", "2012-04-27", "Unknown", "83", "1"},
        {"F11", "Subject85", "2012-04-27", "Unknown", "83", "1"},
        {"G11", "Subject87", "2012-04-29", "Unknown", "85", "1"},
        {"H11", "Subject87", "2012-04-29", "Unknown", "85", "1"},
        {"A12", "Subject89", "2012-05-01", "Unknown", "87", "1"},
        {"B12", "Subject89", "2012-05-01", "Unknown", "87", "1"}
    };

    public ELISPOT_AssayTest()
    {
        setContainerHelper(new UIContainerHelper(this));
        PROJECT_NAME = "ELISPOT_AssayVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUpTest();
        createPlateTemplate();
        importResults();

        //TODO: verify peptide details, location calc

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
        expectedCols.add("peptide");
        expectedCols.add("cell_number");
        expectedCols.add("comment");
        expectedCols.add("sampleId");

        waitForElement(Locator.xpath("//span[contains(text(), 'Freezer Id') and contains(@class, 'x4-column-header-text')]")); //ensure grid loaded
        _helper.addRecordsToAssayTemplate(TEMPLATE_DATA, expectedCols);

        waitForElement(_helper.getAssayWell("A1", LabModuleHelper.UNKNOWN_COLOR), WAIT_FOR_PAGE);
        assertElementPresent(_helper.getAssayWell("C1", LabModuleHelper.POS_COLOR));
        assertElementPresent(_helper.getAssayWell("D1", LabModuleHelper.NEG_COLOR));
        assertElementPresent(_helper.getAssayWell("E1", LabModuleHelper.POS_COLOR));
        assertElementPresent(_helper.getAssayWell("F1", LabModuleHelper.NEG_COLOR));
        assertElementPresent(_helper.getAssayWell("H11", LabModuleHelper.UNKNOWN_COLOR));

        //The sample data is missing required values
        click(Locator.ext4Button("Save"));
        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent("One or more required fields are missing from the sample records");
        waitAndClick(Locator.ext4Button("OK"));

        //restore valid values
        Ext4GridRefWD grid = _ext4Helper.queryOne("grid", Ext4GridRefWD.class);
        grid.setGridCell(1, 4, "Subject5");
        waitAndClick(Locator.ext4Button("Save and Close"));
        waitForText("Save Complete");
        waitAndClick(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText(LabModuleHelper.LAB_HOME_TEXT);
    }

    private void importResults()
    {
        log("Verifying AID Plate Reader Import");
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

        Assert.assertEquals("Incorrect value for field", "AID Plate Reader", Ext4FieldRefWD.getForLabel(this, "Instrument").getValue());
        Assert.assertEquals("Incorrect value for field", new Double(0.05), Ext4FieldRefWD.getForLabel(this, "Positivity Threshold").getValue());
        waitAndClick(btn);

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        String text = _helper.getExampleData();

        //TODO: try to save invalid data

        log("Trying to save data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText("Import Samples");

        verifyExpectedValues();

        log("verifying run plan marked as complete");
        _helper.goToLabHome();
        _helper.clickNavPanelItem(ASSAY_NAME + ":", IMPORT_DATA_TEXT);
        _ext4Helper.clickExt4MenuItem("View Planned Runs");
        waitForPageToLoad();
        DataRegionTable dr2 = new DataRegionTable("query", this);
        Assert.assertEquals("Run plan not marked completed", 0, dr2.getDataRowCount());
    }

    private void verifyExpectedValues()
    {
        log("Verifying results");
        _helper.clickNavPanelItem(ASSAY_NAME + " Runs:", 1);
        waitAndClick(Locator.linkContainingText("view results"));

        DataRegionTable results = new DataRegionTable("Data", this);

        Map<String, String[]> expected = new LinkedHashMap<String, String[]>();
        expected.put("Subject7_56", new String[]{"Subject7", "<5>", "2012-02-09", "56", "NEG", "High CV: 0.30743773095067284", "PBMC"});
        expected.put("Subject7_36", new String[]{"Subject7", "<5>", "2012-02-09", "36", "NEG", "High CV: 0.30743773095067284", "PBMC"});
        expected.put("Subject79_11", new String[]{"Subject79", "<77>", "2012-04-21", "11", "POS", "High CV: 0.1697056274847714", "PBMC"});
        expected.put("Subject79_14", new String[]{"Subject79", "<77>", "2012-04-21", "14", "POS", "High CV: 0.1697056274847714", "PBMC"});
        expected.put("Subject27_35", new String[]{"Subject27", "<25>", "2012-02-29", "35", "NEG", "High CV: 0.128564869306645", "PBMC"});
        expected.put("Subject27_42", new String[]{"Subject27", "<25>", "2012-02-29", "42", "NEG", "High CV: 0.128564869306645", "PBMC"});
        expected.put("Subject15_47", new String[]{"Subject15", "<13>", "2012-02-17", "47", "POS", " ", "PBMC"});
        expected.put("Subject15_46", new String[]{"Subject15", "<13>", "2012-02-17", "46", "POS", " ", "PBMC"});
        expected.put("Subject41_51", new String[]{"Subject41", "<39>", "2012-03-14", "51", "NEG", "High CV: 0.15986762009434988", "PBMC"});
        expected.put("Subject41_64", new String[]{"Subject41", "<39>", "2012-03-14", "64", "NEG", "High CV: 0.15986762009434988", "PBMC"});
        expected.put("Subject39_67", new String[]{"Subject39", "<37>", "2012-03-12", "67", "NEG", "High CV: 0.308555686335948", "PBMC"});
        expected.put("Subject39_43", new String[]{"Subject39", "<37>", "2012-03-12", "43", "NEG", "High CV: 0.308555686335948", "PBMC"});
        expected.put("Subject77_14", new String[]{"Subject77", "<75>", "2012-04-19", "14", "NEG", " ", "PBMC"});
        expected.put("Subject77_16", new String[]{"Subject77", "<75>", "2012-04-19", "16", "NEG", " ", "PBMC"});
        expected.put("Subject69_13", new String[]{"Subject69", "<67>", "2012-04-11", "13", "NEG", "High CV: 0.29998469504883835", "PBMC"});
        expected.put("Subject69_20", new String[]{"Subject69", "<67>", "2012-04-11", "20", "NEG", "High CV: 0.29998469504883835", "PBMC"});
        expected.put("Subject83_47", new String[]{"Subject83", "<81>", "2012-04-25", "47", "NEG", "High CV: 0.13155474998819489", "PBMC"});
        expected.put("Subject83_39", new String[]{"Subject83", "<81>", "2012-04-25", "39", "NEG", "High CV: 0.13155474998819489", "PBMC"});
        expected.put("Control_1154", new String[]{"Control", "Con A", "", "1154", "POS", " ", "PBMC"});
        expected.put("Control_1124", new String[]{"Control", "Con A", "", "1124", "POS", " ", "PBMC"});
        expected.put("Subject43_46", new String[]{"Subject43", "<41>", "2012-03-16", "46", "NEG", " ", "PBMC"});
        expected.put("Subject43_40", new String[]{"Subject43", "<41>", "2012-03-16", "40", "NEG", " ", "PBMC"});
        expected.put("Subject71_15", new String[]{"Subject71", "<69>", "2012-04-13", "15", "NEG", " ", "PBMC"});
        expected.put("Subject71_14", new String[]{"Subject71", "<69>", "2012-04-13", "14", "NEG", " ", "PBMC"});
        expected.put("Subject11_53", new String[]{"Subject11", "<9>", "2012-02-13", "53", "NEG", "High CV: 0.11544600509168124", "PBMC"});
        expected.put("Subject11_45", new String[]{"Subject11", "<9>", "2012-02-13", "45", "NEG", "High CV: 0.11544600509168124", "PBMC"});
        expected.put("Subject55_15", new String[]{"Subject55", "<53>", "2012-03-28", "15", "NEG", "High CV: 0.51425947722658", "PBMC"});
        expected.put("Subject55_7", new String[]{"Subject55", "<53>", "2012-03-28", "7", "NEG", "High CV: 0.51425947722658", "PBMC"});
        expected.put("Subject65_37", new String[]{"Subject65", "<63>", "2012-04-07", "37", "NEG", "High CV: 0.5237828008789241", "PBMC"});
        expected.put("Subject65_17", new String[]{"Subject65", "<63>", "2012-04-07", "17", "NEG", "High CV: 0.5237828008789241", "PBMC"});
        expected.put("Subject47_63", new String[]{"Subject47", "<45>", "2012-03-20", "63", "NEG", "High CV: 0.16269713549424986", "PBMC"});
        expected.put("Subject47_50", new String[]{"Subject47", "<45>", "2012-03-20", "50", "NEG", "High CV: 0.16269713549424986", "PBMC"});
        expected.put("Subject73_36", new String[]{"Subject73", "<71>", "2012-04-15", "36", "NEG", "High CV: 0.6638145292771671", "PBMC"});
        expected.put("Subject73_13", new String[]{"Subject73", "<71>", "2012-04-15", "13", "NEG", "High CV: 0.6638145292771671", "PBMC"});
        expected.put("Subject5_1154", new String[]{"Subject5", "<3>", "2012-02-07", "1154", "NEG", "High CV: 1.3378984966126584", "PBMC"});
        expected.put("Subject5_32", new String[]{"Subject5", "<3>", "2012-02-07", "32", "NEG", "High CV: 1.3378984966126584", "PBMC"});
        expected.put("Subject19_47", new String[]{"Subject19", "<17>", "2012-02-21", "47", "POS", " ", "PBMC"});
        expected.put("Subject19_43", new String[]{"Subject19", "<17>", "2012-02-21", "43", "POS", " ", "PBMC"});
        expected.put("Subject51_36", new String[]{"Subject51", "<49>", "2012-03-24", "36", "NEG", "High CV: 0.10878565864408422", "PBMC"});
        expected.put("Subject51_42", new String[]{"Subject51", "<49>", "2012-03-24", "42", "NEG", "High CV: 0.10878565864408422", "PBMC"});
        expected.put("Subject53_42", new String[]{"Subject53", "<51>", "2012-03-26", "42", "NEG", "High CV: 0.30743773095067284", "PBMC"});
        expected.put("Subject53_27", new String[]{"Subject53", "<51>", "2012-03-26", "27", "NEG", "High CV: 0.30743773095067284", "PBMC"});
        expected.put("Subject13_33", new String[]{"Subject13", "<11>", "2012-02-15", "33", "NEG", "High CV: 0.388520209443158", "PBMC"});
        expected.put("Subject13_58", new String[]{"Subject13", "<11>", "2012-02-15", "58", "NEG", "High CV: 0.388520209443158", "PBMC"});
        expected.put("Subject57_48", new String[]{"Subject57", "<55>", "2012-03-30", "48", "POS", " ", "PBMC"});
        expected.put("Subject57_53", new String[]{"Subject57", "<55>", "2012-03-30", "53", "POS", " ", "PBMC"});
        expected.put("Subject61_44", new String[]{"Subject61", "<59>", "2012-04-03", "44", "NEG", "High CV: 0.1414213562373095", "PBMC"});
        expected.put("Subject61_36", new String[]{"Subject61", "<59>", "2012-04-03", "36", "NEG", "High CV: 0.1414213562373095", "PBMC"});
        expected.put("Subject21_33", new String[]{"Subject21", "<19>", "2012-02-23", "33", "NEG", "High CV: 0.27594410973133565", "PBMC"});
        expected.put("Subject21_49", new String[]{"Subject21", "<19>", "2012-02-23", "49", "NEG", "High CV: 0.27594410973133565", "PBMC"});
        expected.put("Subject33_71", new String[]{"Subject33", "<31>", "2012-03-06", "71", "NEG", "High CV: 0.411407581781264", "PBMC"});
        expected.put("Subject33_39", new String[]{"Subject33", "<31>", "2012-03-06", "39", "NEG", "High CV: 0.411407581781264", "PBMC"});
        expected.put("Subject49_66", new String[]{"Subject49", "<47>", "2012-03-22", "66", "NEG", "High CV: 0.5102832441552405", "PBMC"});
        expected.put("Subject49_31", new String[]{"Subject49", "<47>", "2012-03-22", "31", "NEG", "High CV: 0.5102832441552405", "PBMC"});
        expected.put("Subject37_65", new String[]{"Subject37", "<35>", "2012-03-10", "65", "NEG", "High CV: 0.1438183283769249", "PBMC"});
        expected.put("Subject37_53", new String[]{"Subject37", "<35>", "2012-03-10", "53", "NEG", "High CV: 0.1438183283769249", "PBMC"});
        expected.put("Subject81_12", new String[]{"Subject81", "<79>", "2012-04-23", "12", "NEG", "High CV: 0.37216146378239345", "PBMC"});
        expected.put("Subject81_7", new String[]{"Subject81", "<79>", "2012-04-23", "7", "NEG", "High CV: 0.37216146378239345", "PBMC"});
        expected.put("Subject17_53", new String[]{"Subject17", "<15>", "2012-02-19", "53", "NEG", "High CV: 0.215206411665471", "PBMC"});
        expected.put("Subject17_39", new String[]{"Subject17", "<15>", "2012-02-19", "39", "NEG", "High CV: 0.215206411665471", "PBMC"});
        expected.put("Subject45_48", new String[]{"Subject45", "<43>", "2012-03-18", "48", "NEG", "High CV: 0.22329687826943606", "PBMC"});
        expected.put("Subject45_66", new String[]{"Subject45", "<43>", "2012-03-18", "66", "NEG", "High CV: 0.22329687826943606", "PBMC"});
        expected.put("Subject75_16", new String[]{"Subject75", "<73>", "2012-04-17", "16", "NEG", " ", "PBMC"});
        expected.put("Subject75_15", new String[]{"Subject75", "<73>", "2012-04-17", "15", "NEG", " ", "PBMC"});
        expected.put("Subject29_61", new String[]{"Subject29", "<27>", "2012-03-02", "61", "NEG", "High CV: 0.16866767257660767", "PBMC"});
        expected.put("Subject29_48", new String[]{"Subject29", "<27>", "2012-03-02", "48", "NEG", "High CV: 0.16866767257660767", "PBMC"});
        expected.put("Subject35_41", new String[]{"Subject35", "<33>", "2012-03-08", "41", "NEG", "High CV: 0.2664460334905831", "PBMC"});
        expected.put("Subject35_28", new String[]{"Subject35", "<33>", "2012-03-08", "28", "NEG", "High CV: 0.2664460334905831", "PBMC"});
        expected.put("Control_25", new String[]{"Control", "No stim", "", "25", "NEG", " ", "PBMC"});
        expected.put("Control_22", new String[]{"Control", "No stim", "", "22", "NEG", " ", "PBMC"});
        expected.put("Subject63_12", new String[]{"Subject63", "<61>", "2012-04-05", "12", "NEG", " ", "PBMC"});
        expected.put("Subject63_12", new String[]{"Subject63", "<61>", "2012-04-05", "12", "NEG", " ", "PBMC"});
        expected.put("Subject85_23", new String[]{"Subject85", "<83>", "2012-04-27", "23", "NEG", "High CV: 0.41338550284752007", "PBMC"});
        expected.put("Subject85_42", new String[]{"Subject85", "<83>", "2012-04-27", "42", "NEG", "High CV: 0.41338550284752007", "PBMC"});
        expected.put("Subject23_31", new String[]{"Subject23", "<21>", "2012-02-25", "31", "NEG", "High CV: 0.12478354962115544", "PBMC"});
        expected.put("Subject23_37", new String[]{"Subject23", "<21>", "2012-02-25", "37", "NEG", "High CV: 0.12478354962115544", "PBMC"});
        expected.put("Subject87_46", new String[]{"Subject87", "<85>", "2012-04-29", "46", "NEG", "High CV: 0.4182885184483802", "PBMC"});
        expected.put("Subject87_25", new String[]{"Subject87", "<85>", "2012-04-29", "25", "NEG", "High CV: 0.4182885184483802", "PBMC"});
        expected.put("Subject31_54", new String[]{"Subject31", "<29>", "2012-03-04", "54", "POS", " ", "PBMC"});
        expected.put("Subject31_60", new String[]{"Subject31", "<29>", "2012-03-04", "60", "POS", " ", "PBMC"});
        expected.put("Subject25_78", new String[]{"Subject25", "<23>", "2012-02-27", "78", "NEG", "High CV: 0.282842712474619", "PBMC"});
        expected.put("Subject25_52", new String[]{"Subject25", "<23>", "2012-02-27", "52", "NEG", "High CV: 0.282842712474619", "PBMC"});
        expected.put("Subject67_25", new String[]{"Subject67", "<65>", "2012-04-09", "25", "NEG", " ", "PBMC"});
        expected.put("Subject67_23", new String[]{"Subject67", "<65>", "2012-04-09", "23", "NEG", " ", "PBMC"});
        expected.put("Subject59_43", new String[]{"Subject59", "<57>", "2012-04-01", "43", "NEG", "High CV: 0.22933192903347485", "PBMC"});
        expected.put("Subject59_31", new String[]{"Subject59", "<57>", "2012-04-01", "31", "NEG", "High CV: 0.22933192903347485", "PBMC"});
        expected.put("Subject89_13", new String[]{"Subject89", "<87>", "2012-05-01", "13", "NEG", " ", "PBMC"});
        expected.put("Subject89_12", new String[]{"Subject89", "<87>", "2012-05-01", "12", "NEG", " ", "PBMC"});
        expected.put("Subject9_1124", new String[]{"Subject9", "<7>", "2012-02-11", "1124", "NEG", "High CV: 1.2822682766555034", "PBMC"});
        expected.put("Subject9_55", new String[]{"Subject9", "<7>", "2012-02-11", "55", "NEG", "High CV: 1.2822682766555034", "PBMC"});

        int totalRows = 90;
        Assert.assertEquals("Incorrect row count", totalRows, results.getDataRowCount());

        int i = 0;
        while (i < totalRows)
        {
            String subjectId = results.getDataAsText(i, "Subject Id");
            String date = results.getDataAsText(i, "Sample Date");
            String peptide = results.getDataAsText(i, "Peptide/Pool");
            String spots = results.getDataAsText(i, "Spots");
            String result = results.getDataAsText(i, "Qualitative Result");
            String key = subjectId + "_" + spots;

            String[] expectedVals = expected.get(key);
            Assert.assertNotNull("Unable to find expected values", expectedVals);

            Assert.assertEquals("Incorrect subjectId for: " + key, expectedVals[0], subjectId);
            Assert.assertEquals("Incorrect peptide for: " + key, expectedVals[1], peptide);
            if (!"".equals(expectedVals[2]))
                Assert.assertEquals("Incorrect date for: " + key, expectedVals[2], date);
            Assert.assertEquals("Incorrect spots for: " + key, expectedVals[3], spots);
            Assert.assertEquals("Incorrect qual result for: " + key, expectedVals[4], result);

            i++;
        }
    }

    @Override
    protected void setUpTest() throws Exception
    {
        super.setUpTest();

        //TODO: insert peptides
    }

    @Override
    protected List<Pair<String, String>> getAssaysToCreate()
    {
        List<Pair<String, String>> assays = new ArrayList<Pair<String, String>>();
        assays.add(Pair.of("ELISPOT_Assay", ASSAY_NAME));

        return assays;
    }

    @Override
    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<String>();
        modules.add("ELISPOT_Assay");
        return modules;
    }
}

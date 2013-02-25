/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.test.util.Ext4HelperWD;
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
        //NOTE: this is deliberately missing a subject
            {"A1", "Subject1", "2012-02-09", "Unknown", "3", "1"},
            {"B1", "Subject1", "2012-02-09", "Unknown", "3", "1"},
            {"C1", "Subject1", "2012-02-09", "Pos Control", "Con A", "1"},
            {"D1", "Subject1", "2012-02-09", "Neg Control", "No stim", "1"},
            {"E1", "Subject1", "2012-02-09", "Pos Control", "Con A", "1"},
            {"F1", "Subject1", "2012-02-09", "Neg Control", "No stim", "1"},
            {"G1", "Subject1", "2012-02-09", "Unknown", "5", "1"},
            {"H1", "Subject1", "2/9/2012", "Unknown", "5", "1"},
            {"A2", "Subject1", "2012-02-09", "Unknown", "7", "1"},
            {"B2", "Subject1", "2012-02-09", "Unknown", "7", "1"},
            {"C2", "Subject1", "2012-02-09", "Unknown", "9", "1"},
            {"D2", "Subject1", "2012-02-09", "Unknown", "9", "1"},
            {"E2", "Subject1", "2012-02-09", "Unknown", "11", "1"},
            {"F2", "Subject1", "2012-02-09", "Unknown", "11", "1"},
            {"G2", "Subject2", "2012-02-19", "Unknown", "13", "1"},
            {"H2", "Subject2", "2012-02-19", "Unknown", "13", "1"},
            {"A3", "Subject2", "2012-02-19", "Neg Control", "15", "1"},
            {"B3", "Subject2", "2012-02-19", "Unknown", "15", "1"},
            {"C3", "Subject2", "2012-02-19", "Neg Control", "17", "1"},
            {"D3", "Subject2", "2012-02-19", "Unknown", "17", "1"},
            {"E3", "Subject2", "2012-02-19", "Unknown", "19", "1"},
            {"F3", "Subject2", "2012-02-19", "Unknown", "19", "1"},
            {"G3", "Subject2", "2012-02-19", "Unknown", "21", "1"},
            {"H3", "Subject2", "2012-02-19", "Unknown", "21", "1"},
            {"A4", "Subject2", "2012-02-19", "Unknown", "23", "1"},
            {"B4", "Subject2", "2012-02-19", "Unknown", "23", "1"},
            {"C4", "Subject2", "2012-02-19", "Unknown", "25", "1"},
            {"D4", "Subject2", "2012-02-19", "Unknown", "25", "1"},
            {"E4", "Subject2", "2012-02-19", "Unknown", "27", "1"},
            {"F4", "Subject2", "2012-02-19", "Unknown", "27", "1"},
            {"G4", "Subject3", "2012-03-04", "Unknown", "29", "1"},
            {"H4", "Subject3", "2012-03-04", "Unknown", "29", "1"},
            {"A5", "Subject3", "2012-03-04", "Neg Control", "31", "1"},
            {"B5", "Subject3", "2012-03-04", "Unknown", "31", "1"},
            {"C5", "Subject3", "2012-03-04", "Neg Control", "33", "1"},
            {"D5", "Subject3", "2012-03-04", "Unknown", "33", "1"},
            {"E5", "Subject3", "2012-03-04", "Unknown", "35", "1"},
            {"F5", "Subject3", "2012-03-04", "Unknown", "35", "1"},
            {"G5", "Subject3", "2012-03-04", "Unknown", "37", "1"},
            {"H5", "Subject3", "2012-03-04", "Unknown", "37", "1"},
            {"A6", "Subject3", "2012-03-04", "Unknown", "39", "1"},
            {"B6", "Subject3", "2012-03-04", "Unknown", "39", "1"},
            {"C6", "Subject3", "2012-03-04", "Unknown", "41", "1"},
            {"D6", "Subject3", "2012-03-04", "Unknown", "41", "1"},
            {"E6", "Subject3", "2012-03-04", "Unknown", "43", "1"},
            {"F6", "Subject3", "2012-03-04", "Unknown", "43", "1"},
            {"G6", "Subject3", "2012-03-04", "Unknown", "45", "1"},
            {"H6", "Subject3", "2012-03-04", "Unknown", "45", "1"},
            {"A7", "Subject3", "2012-03-04", "Unknown", "47", "1"},
            {"B7", "Subject3", "2012-03-04", "Unknown", "47", "1"},
            {"C7", "Subject3", "2012-03-04", "Unknown", "49", "1"},
            {"D7", "Subject3", "2012-03-04", "Unknown", "49", "1"},
            {"E7", "Subject3", "2012-03-04", "Unknown", "51", "1"},
            {"F7", "Subject3", "2012-03-04", "Unknown", "51", "1"},
            {"G7", "Subject3", "2012-03-04", "Unknown", "53", "1"},
            {"H7", "Subject3", "2012-03-04", "Unknown", "53", "1"},
            {"A8", "Subject3", "2012-03-04", "Unknown", "55", "1"},
            {"B8", "Subject3", "2012-03-04", "Unknown", "55", "1"},
            {"C8", "Subject3", "2012-03-04", "Unknown", "57", "1"},
            {"D8", "Subject3", "2012-03-04", "Unknown", "57", "1"},
            {"E8", "Subject3", "2012-03-04", "Unknown", "59", "1"},
            {"F8", "Subject3", "2012-03-04", "Unknown", "59", "1"},
            {"G8", "Subject3", "2012-03-04", "Unknown", "61", "1"},
            {"H8", "Subject3", "2012-03-04", "Unknown", "61", "1"},
            {"A9", "Subject3", "2012-03-04", "Unknown", "63", "1"},
            {"B9", "Subject3", "2012-03-04", "Unknown", "63", "1"},
            {"C9", "Subject3", "2012-03-04", "Unknown", "65", "1"},
            {"D9", "Subject3", "2012-03-04", "Unknown", "65", "1"},
            {"E9", "Subject3", "2012-03-04", "Unknown", "67", "1"},
            {"F9", "Subject3", "2012-03-04", "Unknown", "67", "1"},
            {"G9", "Subject3", "2012-03-04", "Unknown", "69", "1"},
            {"H9", "Subject3", "2012-03-04", "Unknown", "69", "1"},
            {"A10", "Subject3", "2012-03-04", "Unknown", "71", "1"},
            {"B10", "Subject3", "2012-03-04", "Unknown", "71", "1"},
            {"C10", "Subject3", "2012-03-04", "Unknown", "73", "1"},
            {"D10", "Subject3", "2012-03-04", "Unknown", "73", "1"},
            {"E10", "Subject3", "2012-03-04", "Unknown", "75", "1"},
            {"F10", "Subject3", "2012-03-04", "Unknown", "75", "1"},
            {"G10", "Subject3", "2012-03-04", "Unknown", "77", "1"},
            {"H10", "Subject3", "2012-03-04", "Unknown", "77", "1"},
            {"A11", "Subject3", "2012-03-04", "Unknown", "79", "1"},
            {"B11", "Subject3", "2012-03-04", "Unknown", "79", "1"},
            {"C11", "Subject3", "2012-03-04", "Unknown", "81", "1"},
            {"D11", "Subject3", "2012-03-04", "Unknown", "81", "1"},
            {"E11", "Subject3", "2012-03-04", "Unknown", "83", "1"},
            {"F11", "Subject3", "2012-03-04", "Unknown", "83", "1"},
            {"G11", "Subject3", "2012-03-04", "Unknown", "85", "1"},
            {"H11", "Subject3", "2012-03-04", "Unknown", "85", "1"},
            {"A12", "Subject3", "2012-03-04", "Unknown", "87", "1"},
            {"B12", "Subject3", "2012-03-04", "Unknown", "87", "1"}
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
        waitForElement(Ext4HelperWD.ext4Window(IMPORT_DATA_TEXT));
        waitAndClick(Locator.ext4Button("Submit"));

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
        String originalID = TEMPLATE_DATA[1][1];
        TEMPLATE_DATA[1][1] = "";

        _helper.addRecordsToAssayTemplate(TEMPLATE_DATA, expectedCols);

        waitForElement(_helper.getAssayWell("A1", LabModuleHelper.UNKNOWN_COLOR), WAIT_FOR_PAGE);
        assertElementPresent(_helper.getAssayWell("C1", LabModuleHelper.POS_COLOR));
        assertElementPresent(_helper.getAssayWell("D1", LabModuleHelper.NEG_COLOR));
        assertElementPresent(_helper.getAssayWell("E1", LabModuleHelper.POS_COLOR));
        assertElementPresent(_helper.getAssayWell("F1", LabModuleHelper.NEG_COLOR));
        assertElementPresent(_helper.getAssayWell("H11", LabModuleHelper.UNKNOWN_COLOR));

        //the data are missing an ID
        click(Locator.ext4Button("Save"));
        waitForElement(Ext4HelperWD.ext4Window("Error"));
        assertTextPresent("One or more required fields are missing from the sample records");
        waitAndClick(Locator.ext4Button("OK"));

        //save data using a fake ID
        Ext4GridRefWD grid = _ext4Helper.queryOne("grid", Ext4GridRefWD.class);
        grid.setGridCell(1, 4, "FakeId");

        click(Locator.ext4Button("Save"));
        waitForElement(Ext4HelperWD.ext4Window("Error"));
        assertTextPresent("Must provide at least 2 negative controls for each subjectId/date.");
        assertTextPresent("Missing for: FakeId / 2012-02-09");
        waitAndClick(Locator.ext4Button("OK"));

        //restore valid values
        grid.setGridCell(1, 4, originalID);
        waitAndClick(Locator.ext4Button("Save and Close"));
        waitForText("Save Complete");
        waitAndClick(Locator.ext4Button("OK"));
        waitForText(LabModuleHelper.LAB_HOME_TEXT);
    }

    private void importResults()
    {
        log("Verifying AID Plate Reader Import");
        _helper.goToLabHome();
        _helper.clickNavPanelItem(ASSAY_NAME + ":", IMPORT_DATA_TEXT);
        _ext4Helper.clickExt4MenuItem("View Planned Runs");

        log("Entering results for saved run");
        DataRegionTable templates = new DataRegionTable("query", this);
        templates.clickLink(0, 1);

        //use the same data included with this assay
        Locator btn = Locator.xpath("//span[text() = 'Download Example Data']");
        waitForElement(btn);

        Assert.assertEquals("Incorrect value for field", "AID Plate Reader", Ext4FieldRefWD.getForLabel(this, "Instrument").getValue());
        Assert.assertEquals("Incorrect value for field", new Double(0.05), Ext4FieldRefWD.getForLabel(this, "Positivity Threshold").getValue());
        waitAndClick(btn);

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        String text = _helper.getExampleData();

        log("Trying to save data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4HelperWD.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForText("Import Samples");

        verifyExpectedValues();

        log("verifying run plan marked as complete");
        _helper.goToLabHome();
        _helper.clickNavPanelItem(ASSAY_NAME + ":", IMPORT_DATA_TEXT);
        _ext4Helper.clickExt4MenuItem("View Planned Runs");
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
        expected.put("Subject3_<53>_15", new String[]{"Subject3", "<53>", "2012-03-04", "15", "NEG", "High CV: 0.51425947722658", "PBMC"});
        expected.put("Subject3_<53>_7", new String[]{"Subject3", "<53>", "2012-03-04", "7", "NEG", "High CV: 0.51425947722658", "PBMC"});
        expected.put("Subject3_<45>_63", new String[]{"Subject3", "<45>", "2012-03-04", "63", "NEG", "High CV: 0.16269713549424986", "PBMC"});
        expected.put("Subject3_<45>_50", new String[]{"Subject3", "<45>", "2012-03-04", "50", "NEG", "High CV: 0.16269713549424986", "PBMC"});
        expected.put("Subject3_<83>_23", new String[]{"Subject3", "<83>", "2012-03-04", "23", "NEG", "High CV: 0.41338550284752007", "PBMC"});
        expected.put("Subject3_<83>_42", new String[]{"Subject3", "<83>", "2012-03-04", "42", "NEG", "High CV: 0.41338550284752007", "PBMC"});
        expected.put("Subject3_<37>_67", new String[]{"Subject3", "<37>", "2012-03-04", "67", "NEG", "High CV: 0.308555686335948", "PBMC"});
        expected.put("Subject3_<37>_43", new String[]{"Subject3", "<37>", "2012-03-04", "43", "NEG", "High CV: 0.308555686335948", "PBMC"});
        expected.put("Subject3_<51>_42", new String[]{"Subject3", "<51>", "2012-03-04", "42", "NEG", "High CV: 0.30743773095067284", "PBMC"});
        expected.put("Subject3_<51>_27", new String[]{"Subject3", "<51>", "2012-03-04", "27", "NEG", "High CV: 0.30743773095067284", "PBMC"});
        expected.put("Subject3_<63>_37", new String[]{"Subject3", "<63>", "2012-03-04", "37", "NEG", "High CV: 0.5237828008789241", "PBMC"});
        expected.put("Subject3_<63>_17", new String[]{"Subject3", "<63>", "2012-03-04", "17", "NEG", "High CV: 0.5237828008789241", "PBMC"});
        expected.put("Subject3_<55>_48", new String[]{"Subject3", "<55>", "2012-03-04", "48", "NEG", "", "PBMC"});
        expected.put("Subject3_<55>_53", new String[]{"Subject3", "<55>", "2012-03-04", "53", "NEG", "", "PBMC"});
        expected.put("Subject3_<47>_66", new String[]{"Subject3", "<47>", "2012-03-04", "66", "NEG", "High CV: 0.5102832441552405", "PBMC"});
        expected.put("Subject3_<47>_31", new String[]{"Subject3", "<47>", "2012-03-04", "31", "NEG", "High CV: 0.5102832441552405", "PBMC"});
        expected.put("Subject3_<81>_47", new String[]{"Subject3", "<81>", "2012-03-04", "47", "NEG", "High CV: 0.13155474998819489", "PBMC"});
        expected.put("Subject3_<81>_39", new String[]{"Subject3", "<81>", "2012-03-04", "39", "NEG", "High CV: 0.13155474998819489", "PBMC"});
        expected.put("Subject3_<67>_13", new String[]{"Subject3", "<67>", "", "13", "NEG", "High CV: 0.29998469504883835", "PBMC"});
        expected.put("Subject3_<67>_20", new String[]{"Subject3", "<67>", "", "20", "NEG", "High CV: 0.29998469504883835", "PBMC"});
        expected.put("Subject3_<71>_36", new String[]{"Subject3", "<71>", "2012-03-04", "36", "NEG", "High CV: 0.6638145292771671", "PBMC"});
        expected.put("Subject3_<71>_13", new String[]{"Subject3", "<71>", "2012-03-04", "13", "NEG", "High CV: 0.6638145292771671", "PBMC"});
        expected.put("Subject3_<65>_25", new String[]{"Subject3", "<65>", "2012-03-04", "25", "NEG", "", "PBMC"});
        expected.put("Subject3_<65>_23", new String[]{"Subject3", "<65>", "2012-03-04", "23", "NEG", "", "PBMC"});
        expected.put("Subject1_No stim_25", new String[]{"Subject1", "No stim", "2012-02-09", "25", "NEG", "", "PBMC"});
        expected.put("Subject1_No stim_22", new String[]{"Subject1", "No stim", "2012-02-09", "22", "NEG", "", "PBMC"});
        expected.put("Subject3_<35>_65", new String[]{"Subject3", "<35>", "2012-03-04", "65", "NEG", "High CV: 0.1438183283769249", "PBMC"});
        expected.put("Subject3_<35>_53", new String[]{"Subject3", "<35>", "2012-03-04", "53", "NEG", "High CV: 0.1438183283769249", "PBMC"});
        expected.put("Subject2_<19>_33", new String[]{"Subject2", "<19>", "2012-02-19", "33", "NEG", "High CV: 0.27594410973133565", "PBMC"});
        expected.put("Subject2_<19>_49", new String[]{"Subject2", "<19>", "2012-02-19", "49", "NEG", "High CV: 0.27594410973133565", "PBMC"});
        expected.put("Subject3_<61>_12", new String[]{"Subject3", "<61>", "2012-03-04", "12", "NEG", "", "PBMC"});
        expected.put("Subject3_<61>_12", new String[]{"Subject3", "<61>", "2012-03-04", "12", "NEG", "", "PBMC"});
        expected.put("Subject3_<33>_41", new String[]{"Subject3", "<33>", "2012-03-04", "41", "NEG", "High CV: 0.2664460334905831", "PBMC"});
        expected.put("Subject3_<33>_28", new String[]{"Subject3", "<33>", "2012-03-04", "28", "NEG", "High CV: 0.2664460334905831", "PBMC"});
        expected.put("Subject3_<85>_46", new String[]{"Subject3", "<85>", "2012-03-04", "46", "NEG", "High CV: 0.4182885184483802", "PBMC"});
        expected.put("Subject3_<85>_25", new String[]{"Subject3", "<85>", "2012-03-04", "25", "NEG", "High CV: 0.4182885184483802", "PBMC"});
        expected.put("Subject3_<79>_12", new String[]{"Subject3", "<79>", "2012-03-04", "12", "NEG", "High CV: 0.37216146378239345", "PBMC"});
        expected.put("Subject3_<79>_7", new String[]{"Subject3", "<79>", "2012-03-04", "7", "NEG", "High CV: 0.37216146378239345", "PBMC"});
        expected.put("Subject1_<5>_56", new String[]{"Subject1", "<5>", "2012-02-09", "56", "NEG", "High CV: 0.30743773095067284", "PBMC"});
        expected.put("Subject1_<5>_36", new String[]{"Subject1", "<5>", "2012-02-09", "36", "NEG", "High CV: 0.30743773095067284", "PBMC"});
        expected.put("Subject3_<77>_11", new String[]{"Subject3", "<77>", "2012-03-04", "11", "NEG", "High CV: 0.1697056274847714", "PBMC"});
        expected.put("Subject3_<77>_14", new String[]{"Subject3", "<77>", "2012-03-04", "14", "NEG", "High CV: 0.1697056274847714", "PBMC"});
        expected.put("Subject3_<87>_13", new String[]{"Subject3", "<87>", "2012-03-04", "13", "NEG", "", "PBMC"});
        expected.put("Subject3_<87>_12", new String[]{"Subject3", "<87>", "2012-03-04", "12", "NEG", "", "PBMC"});
        expected.put("Subject1_<3>_1154", new String[]{"Subject1", "<3>", "2012-02-09", "1154", "NEG", "High CV: 1.3378984966126584", "PBMC"});
        expected.put("Subject1_<3>_32", new String[]{"Subject1", "<3>", "2012-02-09", "32", "NEG", "High CV: 1.3378984966126584", "PBMC"});
        expected.put("Subject3_<73>_16", new String[]{"Subject3", "<73>", "2012-03-04", "16", "NEG", "", "PBMC"});
        expected.put("Subject3_<73>_15", new String[]{"Subject3", "<73>", "2012-03-04", "15", "NEG", "", "PBMC"});
        expected.put("Subject3_<75>_14", new String[]{"Subject3", "<75>", "2012-03-04", "14", "NEG", "", "PBMC"});
        expected.put("Subject3_<75>_16", new String[]{"Subject3", "<75>", "2012-03-04", "16", "NEG", "", "PBMC"});
        expected.put("Subject3_<29>_54", new String[]{"Subject3", "<29>", "2012-03-04", "54", "NEG", "", "PBMC"});
        expected.put("Subject3_<29>_60", new String[]{"Subject3", "<29>", "2012-03-04", "60", "NEG", "", "PBMC"});
        expected.put("Subject1_<9>_53", new String[]{"Subject1", "<9>", "2012-02-09", "53", "NEG", "High CV: 0.11544600509168124", "PBMC"});
        expected.put("Subject1_<9>_45", new String[]{"Subject1", "<9>", "2012-02-09", "45", "NEG", "High CV: 0.11544600509168124", "PBMC"});
        expected.put("Subject1_<7>_1124", new String[]{"Subject1", "<7>", "2012-02-09", "1124", "NEG", "High CV: 1.2822682766555034", "PBMC"});
        expected.put("Subject1_<7>_55", new String[]{"Subject1", "<7>", "2012-02-09", "55", "NEG", "High CV: 1.2822682766555034", "PBMC"});
        expected.put("Subject1_<11>_33", new String[]{"Subject1", "<11>", "2012-02-09", "33", "NEG", "High CV: 0.388520209443158", "PBMC"});
        expected.put("Subject1_<11>_58", new String[]{"Subject1", "<11>", "2012-02-09", "58", "NEG", "High CV: 0.388520209443158", "PBMC"});
        expected.put("Subject1_Con A_1154", new String[]{"Subject1", "Con A", "2012-02-09", "1154", "POS", "", "PBMC"});
        expected.put("Subject1_Con A_1124", new String[]{"Subject1", "Con A", "2012-02-09", "1124", "POS", "", "PBMC"});
        expected.put("Subject2_<21>_31", new String[]{"Subject2", "<21>", "2012-02-19", "31", "NEG", "High CV: 0.12478354962115544", "PBMC"});
        expected.put("Subject2_<21>_37", new String[]{"Subject2", "<21>", "2012-02-19", "37", "NEG", "High CV: 0.12478354962115544", "PBMC"});
        expected.put("Subject3_<69>_15", new String[]{"Subject3", "<69>", "2012-03-04", "15", "NEG", "", "PBMC"});
        expected.put("Subject3_<69>_14", new String[]{"Subject3", "<69>", "2012-03-04", "14", "NEG", "", "PBMC"});
        expected.put("Subject2_<23>_78", new String[]{"Subject2", "<23>", "2012-02-19", "78", "NEG", "High CV: 0.282842712474619", "PBMC"});
        expected.put("Subject2_<23>_52", new String[]{"Subject2", "<23>", "2012-02-19", "52", "NEG", "High CV: 0.282842712474619", "PBMC"});
        expected.put("Subject2_<27>_61", new String[]{"Subject2", "<27>", "2012-02-19", "61", "NEG", "High CV: 0.16866767257660767", "PBMC"});
        expected.put("Subject2_<27>_48", new String[]{"Subject2", "<27>", "2012-02-19", "48", "NEG", "High CV: 0.16866767257660767", "PBMC"});
        expected.put("Subject3_<31>_71", new String[]{"Subject3", "<31>", "", "71", "NEG", "High CV: 0.411407581781264", "PBMC"});
        expected.put("Subject3_<31>_39", new String[]{"Subject3", "<31>", "", "39", "NEG", "High CV: 0.411407581781264", "PBMC"});
        expected.put("Subject2_<17>_47", new String[]{"Subject2", "<17>", "2012-02-19", "47", "NEG", "", "PBMC"});
        expected.put("Subject2_<17>_43", new String[]{"Subject2", "<17>", "2012-02-19", "43", "NEG", "", "PBMC"});
        expected.put("Subject3_<41>_46", new String[]{"Subject3", "<41>", "2012-03-04", "46", "NEG", "", "PBMC"});
        expected.put("Subject3_<41>_40", new String[]{"Subject3", "<41>", "2012-03-04", "40", "NEG", "", "PBMC"});
        expected.put("Subject3_<49>_36", new String[]{"Subject3", "<49>", "2012-03-04", "36", "NEG", "High CV: 0.10878565864408422", "PBMC"});
        expected.put("Subject3_<49>_42", new String[]{"Subject3", "<49>", "2012-03-04", "42", "NEG", "High CV: 0.10878565864408422", "PBMC"});
        expected.put("Subject2_<25>_35", new String[]{"Subject2", "<25>", "2012-02-19", "35", "NEG", "High CV: 0.128564869306645", "PBMC"});
        expected.put("Subject2_<25>_42", new String[]{"Subject2", "<25>", "2012-02-19", "42", "NEG", "High CV: 0.128564869306645", "PBMC"});
        expected.put("Subject3_<57>_43", new String[]{"Subject3", "<57>", "2012-03-04", "43", "NEG", "High CV: 0.22933192903347485", "PBMC"});
        expected.put("Subject3_<57>_31", new String[]{"Subject3", "<57>", "2012-03-04", "31", "NEG", "High CV: 0.22933192903347485", "PBMC"});
        expected.put("Subject2_<13>_47", new String[]{"Subject2", "<13>", "2012-02-19", "47", "NEG", "", "PBMC"});
        expected.put("Subject2_<13>_46", new String[]{"Subject2", "<13>", "2012-02-19", "46", "NEG", "", "PBMC"});
        expected.put("Subject3_<43>_48", new String[]{"Subject3", "<43>", "2012-03-04", "48", "NEG", "High CV: 0.22329687826943606", "PBMC"});
        expected.put("Subject3_<43>_66", new String[]{"Subject3", "<43>", "2012-03-04", "66", "NEG", "High CV: 0.22329687826943606", "PBMC"});
        expected.put("Subject3_<59>_44", new String[]{"Subject3", "<59>", "2012-03-04", "44", "NEG", "High CV: 0.1414213562373095", "PBMC"});
        expected.put("Subject3_<59>_36", new String[]{"Subject3", "<59>", "2012-03-04", "36", "NEG", "High CV: 0.1414213562373095", "PBMC"});
        expected.put("Subject3_<39>_51", new String[]{"Subject3", "<39>", "2012-03-04", "51", "NEG", "High CV: 0.15986762009434988", "PBMC"});
        expected.put("Subject3_<39>_64", new String[]{"Subject3", "<39>", "2012-03-04", "64", "NEG", "High CV: 0.15986762009434988", "PBMC"});
        expected.put("Subject2_<15>_53", new String[]{"Subject2", "<15>", "2012-02-19", "53", "NEG", "High CV: 0.215206411665471", "PBMC"});
        expected.put("Subject2_<15>_39", new String[]{"Subject2", "<15>", "2012-02-19", "39", "NEG", "High CV: 0.215206411665471", "PBMC"});

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
            String key = subjectId + "_" + peptide + "_" + spots;

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

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

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.External;
import org.labkey.test.categories.LabModule;
import org.labkey.test.categories.ONPRC;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.labkey.test.util.ext4cmp.Ext4GridRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@Category({External.class, ONPRC.class, LabModule.class})
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
        PROJECT_NAME = "ELISPOT_AssayVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Test
    public void testSteps() throws Exception
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

        List<String> expectedCols = new ArrayList<>();
        expectedCols.add("well");
        expectedCols.add("category");
        expectedCols.add("subjectId");
        expectedCols.add("date");
        expectedCols.add("peptide");
        expectedCols.add("cell_number");
        expectedCols.add("comment");
        expectedCols.add("sampleId");
        expectedCols.add("concentration");

        waitForElement(Locator.xpath("//span[contains(text(), 'Freezer Id') and contains(@class, 'x4-column-header-text')]"), WAIT_FOR_PAGE); //ensure grid loaded
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
        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent("One or more required fields are missing from the sample records");
        waitAndClick(Locator.ext4Button("OK"));

        //save data using a fake ID
        Ext4GridRef grid = _ext4Helper.queryOne("grid", Ext4GridRef.class);
        grid.setGridCell(1, "subjectId", "FakeId");

        click(Locator.ext4Button("Save"));
        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent("Must provide at least 2 negative controls for each subjectId/date.");
        assertTextPresent("Missing for: FakeId / 2012-02-09");
        waitAndClick(Locator.ext4Button("OK"));

        //restore valid values
        grid.setGridCell(1, "subjectId", originalID);
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
        Locator btn = Locator.linkContainingText("Download Example Data");
        waitForElement(btn);

        assertEquals("Incorrect value for field", "AID Plate Reader", Ext4FieldRef.getForLabel(this, "Instrument").getValue());
        assertEquals("Incorrect value for field", new Double(0.05), Ext4FieldRef.getForLabel(this, "Positivity Threshold").getValue());
        waitAndClick(btn);

        Ext4FieldRef textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRef.class);
        String text = _helper.getExampleData();

        log("Trying to save data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForText("Import Samples");

        verifyExpectedValues();

        log("verifying run plan marked as complete");
        _helper.goToLabHome();
        _helper.clickNavPanelItem(ASSAY_NAME + ":", IMPORT_DATA_TEXT);
        _ext4Helper.clickExt4MenuItem("View Planned Runs");
        DataRegionTable dr2 = new DataRegionTable("query", this);
        assertEquals("Run plan not marked completed", 0, dr2.getDataRowCount());
    }

    private void verifyExpectedValues()
    {
        log("Verifying results");
        _helper.clickNavPanelItemAndWait(ASSAY_NAME + " Runs:", 1);
        waitAndClickAndWait(Locator.linkContainingText("view results"));

        DataRegionTable results = new DataRegionTable("Data", this);

        Map<String, String[]> expected = new LinkedHashMap<>();
        expected.put("Subject1_<3>_1130.5", new String[]{"Subject1", "<3>", "2012-02-09", "1130.5", "NEG", "High CV: 1.338", "PBMC"});
        expected.put("Subject1_<7>_1100.5", new String[]{"Subject1", "<7>", "2012-02-09", "1100.5", "NEG", "High CV: 1.282", "PBMC"});
        expected.put("Subject2_<15>_3", new String[]{"Subject2", "<15>", "2012-02-19", "3", "NEG", "High CV: 0.215", "PBMC"});
        expected.put("Subject2_<23>_28", new String[]{"Subject2", "<23>", "2012-02-19", "28", "NEG", "High CV: 0.283", "PBMC"});
        expected.put("Subject3_<31>_15", new String[]{"Subject3", "<31>", "2012-03-04", "15", "NEG", "High CV: 0.411", "PBMC"});
        expected.put("Subject3_<39>_-5", new String[]{"Subject3", "<39>", "2012-03-04", "-5", "NEG", " ", "PBMC"});
        expected.put("Subject3_<47>_10", new String[]{"Subject3", "<47>", "2012-03-04", "10", "NEG", "High CV: 0.51", "PBMC"});
        expected.put("Subject3_<55>_-8", new String[]{"Subject3", "<55>", "2012-03-04", "-8", "NEG", " ", "PBMC"});
        expected.put("Subject3_<63>_-19", new String[]{"Subject3", "<63>", "2012-03-04", "-19", "NEG", "High CV: 0.524", "PBMC"});
        expected.put("Subject3_<71>_-20", new String[]{"Subject3", "<71>", "2012-03-04", "-20", "NEG", "High CV: 0.664", "PBMC"});
        expected.put("Subject3_<79>_-44", new String[]{"Subject3", "<79>", "2012-03-04", "-44", "NEG", " ", "PBMC"});
        expected.put("Subject3_<87>_-43", new String[]{"Subject3", "<87>", "2012-03-04", "-43", "NEG", " ", "PBMC"});
        expected.put("Subject1_<3>_8.5", new String[]{"Subject1", "<3>", "2012-02-09", "8.5", "NEG", "High CV: 1.338", "PBMC"});
        expected.put("Subject1_<7>_31.5", new String[]{"Subject1", "<7>", "2012-02-09", "31.5", "NEG", "High CV: 1.282", "PBMC"});
        expected.put("Subject2_<15>_-11", new String[]{"Subject2", "<15>", "2012-02-19", "-11", "NEG", "High CV: 0.215", "PBMC"});
        expected.put("Subject2_<23>_2", new String[]{"Subject2", "<23>", "2012-02-19", "2", "NEG", "High CV: 0.283", "PBMC"});
        expected.put("Subject3_<31>_-17", new String[]{"Subject3", "<31>", "2012-03-04", "-17", "NEG", "High CV: 0.411", "PBMC"});
        expected.put("Subject3_<39>_8", new String[]{"Subject3", "<39>", "2012-03-04", "8", "NEG", " ", "PBMC"});
        expected.put("Subject3_<47>_-25", new String[]{"Subject3", "<47>", "2012-03-04", "-25", "NEG", "High CV: 0.51", "PBMC"});
        expected.put("Subject3_<55>_-3", new String[]{"Subject3", "<55>", "2012-03-04", "-3", "NEG", " ", "PBMC"});
        expected.put("Subject3_<63>_-39", new String[]{"Subject3", "<63>", "2012-03-04", "-39", "NEG", "High CV: 0.524", "PBMC"});
        expected.put("Subject3_<71>_-43", new String[]{"Subject3", "<71>", "2012-03-04", "-43", "NEG", "High CV: 0.664", "PBMC"});
        expected.put("Subject3_<79>_-49", new String[]{"Subject3", "<79>", "2012-03-04", "-49", "NEG", " ", "PBMC"});
        expected.put("Subject3_<87>_-44", new String[]{"Subject3", "<87>", "2012-03-04", "-44", "NEG", " ", "PBMC"});
        expected.put("Subject1_Con A_1130.5", new String[]{"Subject1", "Con A", "2012-02-09", "1130.5", "POS", " ", "PBMC"});
        expected.put("Subject1_<9>_29.5", new String[]{"Subject1", "<9>", "2012-02-09", "29.5", "POS", " ", "PBMC"});
        expected.put("Subject2_<17>_-3", new String[]{"Subject2", "<17>", "2012-02-19", "-3", "NEG", " ", "PBMC"});
        expected.put("Subject2_<25>_-15", new String[]{"Subject2", "<25>", "2012-02-19", "-15", "NEG", " ", "PBMC"});
        expected.put("Subject3_<33>_-15", new String[]{"Subject3", "<33>", "2012-03-04", "-15", "NEG", "High CV: 0.266", "PBMC"});
        expected.put("Subject3_<41>_-10", new String[]{"Subject3", "<41>", "2012-03-04", "-10", "NEG", " ", "PBMC"});
        expected.put("Subject3_<49>_-20", new String[]{"Subject3", "<49>", "2012-03-04", "-20", "NEG", " ", "PBMC"});
        expected.put("Subject3_<57>_-13", new String[]{"Subject3", "<57>", "2012-03-04", "-13", "NEG", "High CV: 0.229", "PBMC"});
        expected.put("Subject3_<65>_-31", new String[]{"Subject3", "<65>", "2012-03-04", "-31", "NEG", " ", "PBMC"});
        expected.put("Subject3_<73>_-40", new String[]{"Subject3", "<73>", "2012-03-04", "-40", "NEG", " ", "PBMC"});
        expected.put("Subject3_<81>_-9", new String[]{"Subject3", "<81>", "2012-03-04", "-9", "NEG", " ", "PBMC"});
        expected.put("Subject1_No stim_1.5", new String[]{"Subject1", "No stim", "2012-02-09", "1.5", "NEG", " ", "PBMC"});
        expected.put("Subject1_<9>_21.5", new String[]{"Subject1", "<9>", "2012-02-09", "21.5", "POS", " ", "PBMC"});
        expected.put("Subject2_<17>_-7", new String[]{"Subject2", "<17>", "2012-02-19", "-7", "NEG", " ", "PBMC"});
        expected.put("Subject2_<25>_-8", new String[]{"Subject2", "<25>", "2012-02-19", "-8", "NEG", " ", "PBMC"});
        expected.put("Subject3_<33>_-28", new String[]{"Subject3", "<33>", "2012-03-04", "-28", "NEG", "High CV: 0.266", "PBMC"});
        expected.put("Subject3_<41>_-16", new String[]{"Subject3", "<41>", "2012-03-04", "-16", "NEG", " ", "PBMC"});
        expected.put("Subject3_<49>_-14", new String[]{"Subject3", "<49>", "2012-03-04", "-14", "NEG", " ", "PBMC"});
        expected.put("Subject3_<57>_-25", new String[]{"Subject3", "<57>", "2012-03-04", "-25", "NEG", "High CV: 0.229", "PBMC"});
        expected.put("Subject3_<65>_-33", new String[]{"Subject3", "<65>", "2012-03-04", "-33", "NEG", " ", "PBMC"});
        expected.put("Subject3_<73>_-41", new String[]{"Subject3", "<73>", "2012-03-04", "-41", "NEG", " ", "PBMC"});
        expected.put("Subject3_<81>_-17", new String[]{"Subject3", "<81>", "2012-03-04", "-17", "NEG", " ", "PBMC"});
        expected.put("Subject1_Con A_1100.5", new String[]{"Subject1", "Con A", "2012-02-09", "1100.5", "POS", " ", "PBMC"});
        expected.put("Subject1_<11>_9.5", new String[]{"Subject1", "<11>", "2012-02-09", "9.5", "NEG", "High CV: 0.389", "PBMC"});
        expected.put("Subject2_<19>_-17", new String[]{"Subject2", "<19>", "2012-02-19", "-17", "NEG", "High CV: 0.276", "PBMC"});
        expected.put("Subject2_<27>_11", new String[]{"Subject2", "<27>", "2012-02-19", "11", "NEG", " ", "PBMC"});
        expected.put("Subject3_<35>_9", new String[]{"Subject3", "<35>", "2012-03-04", "9", "NEG", " ", "PBMC"});
        expected.put("Subject3_<43>_-8", new String[]{"Subject3", "<43>", "2012-03-04", "-8", "NEG", "High CV: 0.223", "PBMC"});
        expected.put("Subject3_<51>_-14", new String[]{"Subject3", "<51>", "2012-03-04", "-14", "NEG", "High CV: 0.307", "PBMC"});
        expected.put("Subject3_<59>_-12", new String[]{"Subject3", "<59>", "2012-03-04", "-12", "NEG", " ", "PBMC"});
        expected.put("Subject3_<67>_-43", new String[]{"Subject3", "<67>", "2012-03-04", "-43", "NEG", "High CV: 0.3", "PBMC"});
        expected.put("Subject3_<75>_-42", new String[]{"Subject3", "<75>", "2012-03-04", "-42", "NEG", " ", "PBMC"});
        expected.put("Subject3_<83>_-33", new String[]{"Subject3", "<83>", "2012-03-04", "-33", "NEG", "High CV: 0.413", "PBMC"});
        expected.put("Subject1_No stim_-1.5", new String[]{"Subject1", "No stim", "2012-02-09", "-1.5", "NEG", " ", "PBMC"});
        expected.put("Subject1_<11>_34.5", new String[]{"Subject1", "<11>", "2012-02-09", "34.5", "NEG", "High CV: 0.389", "PBMC"});
        expected.put("Subject2_<19>_-1", new String[]{"Subject2", "<19>", "2012-02-19", "-1", "NEG", "High CV: 0.276", "PBMC"});
        expected.put("Subject2_<27>_-2", new String[]{"Subject2", "<27>", "2012-02-19", "-2", "NEG", " ", "PBMC"});
        expected.put("Subject3_<35>_-3", new String[]{"Subject3", "<35>", "2012-03-04", "-3", "NEG", " ", "PBMC"});
        expected.put("Subject3_<43>_10", new String[]{"Subject3", "<43>", "2012-03-04", "10", "NEG", "High CV: 0.223", "PBMC"});
        expected.put("Subject3_<51>_-29", new String[]{"Subject3", "<51>", "2012-03-04", "-29", "NEG", "High CV: 0.307", "PBMC"});
        expected.put("Subject3_<59>_-20", new String[]{"Subject3", "<59>", "2012-03-04", "-20", "NEG", " ", "PBMC"});
        expected.put("Subject3_<67>_-36", new String[]{"Subject3", "<67>", "2012-03-04", "-36", "NEG", "High CV: 0.3", "PBMC"});
        expected.put("Subject3_<75>_-40", new String[]{"Subject3", "<75>", "2012-03-04", "-40", "NEG", " ", "PBMC"});
        expected.put("Subject3_<83>_-14", new String[]{"Subject3", "<83>", "2012-03-04", "-14", "NEG", "High CV: 0.413", "PBMC"});
        expected.put("Subject1_<5>_32.5", new String[]{"Subject1", "<5>", "2012-02-09", "32.5", "NEG", "High CV: 0.307", "PBMC"});
        expected.put("Subject2_<13>_-3", new String[]{"Subject2", "<13>", "2012-02-19", "-3", "NEG", " ", "PBMC"});
        expected.put("Subject2_<21>_-19", new String[]{"Subject2", "<21>", "2012-02-19", "-19", "NEG", " ", "PBMC"});
        expected.put("Subject3_<29>_-2", new String[]{"Subject3", "<29>", "2012-03-04", "-2", "NEG", " ", "PBMC"});
        expected.put("Subject3_<37>_11", new String[]{"Subject3", "<37>", "2012-03-04", "11", "NEG", "High CV: 0.309", "PBMC"});
        expected.put("Subject3_<45>_7", new String[]{"Subject3", "<45>", "2012-03-04", "7", "NEG", " ", "PBMC"});
        expected.put("Subject3_<53>_-41", new String[]{"Subject3", "<53>", "2012-03-04", "-41", "NEG", "High CV: 0.514", "PBMC"});
        expected.put("Subject3_<61>_-44", new String[]{"Subject3", "<61>", "2012-03-04", "-44", "NEG", " ", "PBMC"});
        expected.put("Subject3_<69>_-41", new String[]{"Subject3", "<69>", "2012-03-04", "-41", "NEG", " ", "PBMC"});
        expected.put("Subject3_<77>_-45", new String[]{"Subject3", "<77>", "2012-03-04", "-45", "NEG", " ", "PBMC"});
        expected.put("Subject3_<85>_-10", new String[]{"Subject3", "<85>", "2012-03-04", "-10", "NEG", "High CV: 0.418", "PBMC"});
        expected.put("Subject1_<5>_12.5", new String[]{"Subject1", "<5>", "2012-02-09", "12.5", "NEG", "High CV: 0.307", "PBMC"});
        expected.put("Subject2_<13>_-4", new String[]{"Subject2", "<13>", "2012-02-19", "-4", "NEG", " ", "PBMC"});
        expected.put("Subject2_<21>_-13", new String[]{"Subject2", "<21>", "2012-02-19", "-13", "NEG", " ", "PBMC"});
        expected.put("Subject3_<29>_4", new String[]{"Subject3", "<29>", "2012-03-04", "4", "NEG", " ", "PBMC"});
        expected.put("Subject3_<37>_-13", new String[]{"Subject3", "<37>", "2012-03-04", "-13", "NEG", "High CV: 0.309", "PBMC"});
        expected.put("Subject3_<45>_-6", new String[]{"Subject3", "<45>", "2012-03-04", "-6", "NEG", " ", "PBMC"});
        expected.put("Subject3_<53>_-49", new String[]{"Subject3", "<53>", "2012-03-04", "-49", "NEG", "High CV: 0.514", "PBMC"});
        expected.put("Subject3_<61>_-44", new String[]{"Subject3", "<61>", "2012-03-04", "-44", "NEG", " ", "PBMC"});
        expected.put("Subject3_<69>_-42", new String[]{"Subject3", "<69>", "2012-03-04", "-42", "NEG", " ", "PBMC"});
        expected.put("Subject3_<77>_-42", new String[]{"Subject3", "<77>", "2012-03-04", "-42", "NEG", " ", "PBMC"});
        expected.put("Subject3_<85>_-31", new String[]{"Subject3", "<85>", "2012-03-04", "-31", "NEG", "High CV: 0.418", "PBMC"});

        int totalRows = 90;
        assertEquals("Incorrect row count", totalRows, results.getDataRowCount());
        log("DataRegion column count was: " + results.getColumnCount());

        //recreate the DR to see if this removes intermittent test failures
        results = new DataRegionTable(results.getTableName(), this);

        int i = 0;
        while (i < totalRows)
        {
            String subjectId = results.getDataAsText(i, "Subject Id");
            String date = results.getDataAsText(i, "Sample Date");
            String peptide = results.getDataAsText(i, "Peptide/Pool");
            String spots = results.getDataAsText(i, "Spots Above Background");
            String result = results.getDataAsText(i, "Result");
            String key = subjectId + "_" + peptide + "_" + spots;

            String[] expectedVals = expected.get(key);
            assertNotNull("Unable to find expected values: " + key, expectedVals);

            assertEquals("Incorrect subjectId for: " + key, expectedVals[0], subjectId);
            assertEquals("Incorrect peptide for: " + key, expectedVals[1], peptide);
            if (!"".equals(expectedVals[2]))
                assertEquals("Incorrect date for: " + key, expectedVals[2], date);
            assertEquals("Incorrect spots for: " + key, expectedVals[3], spots);
            assertEquals("Incorrect qual result for: " + key, expectedVals[4], result);

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
        List<Pair<String, String>> assays = new ArrayList<>();
        assays.add(Pair.of("ELISPOT_Assay", ASSAY_NAME));

        return assays;
    }

    @Override
    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<>();
        modules.add("ELISPOT_Assay");
        return modules;
    }
}

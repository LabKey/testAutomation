/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MiniTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PerlHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyA.class, MiniTest.class, Assays.class})
public class LuminexPositivityTest extends LuminexTest
{
    List<String> _analyteNames = new ArrayList<>();
    private int _expectedThresholdValue = 100;
    private int _newThresholdValue = 100;
    private Boolean _expectedNegativeControlValue = false;
    private Boolean _newNegativeControlValue = false;
    private String _negControlAnalyte = null;

    protected void ensureConfigured()
    {
        PerlHelper perlHelper = new PerlHelper(this);
        if(!perlHelper.ensurePerlConfig())
            fail("No Perl engine");

        setUseXarImport(true);
        super.ensureConfigured();
    }


    protected void runUITests()
    {
        String assayName = "Positivity";
        _analyteNames.add("MyAnalyte (1)");
        _analyteNames.add("Blank (3)");

        addTransformScriptsToAssayDesign();

        test3xFoldChange(assayName);
        test5xFoldChange(assayName);
        testWithoutBaselineVisitOrFoldChange(assayName);
        testWithNegativeControls(assayName);
        testBaselineVisitDataFromPreviousRun(assayName);
    }

    private void addTransformScriptsToAssayDesign()
    {
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + "/resources/transformscripts/description_parsing_example.pl"), 0);
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LABKEY), 1);
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LAB), 2);
        saveAssay();
    }

    private void testBaselineVisitDataFromPreviousRun(String assayName)
    {
        setPositivityThresholdParams(100, 100);
        setNegativeControlParams(true, false);
        uploadPositivityFile(assayName + " Baseline Visit Previous Run Error", TEST_ASSAY_LUM_FILE12, "1", "3", false, false);
        assertTextPresent("Error: Baseline visit data found in more than one prevoiusly uploaded run: Analyte=" + _analyteNames.get(0) + ", Participant=123400001, Visit=1.");
        clickButton("Cancel");

        // delete all but one run of data so we have the expected number of previous baseline visits rows
        checkDataRegionCheckbox("Runs", 0);
        checkDataRegionCheckbox("Runs", 1);
        checkDataRegionCheckbox("Runs", 2);
        checkDataRegionCheckbox("Runs", 3);
        clickButton("Delete");
        assertEquals(4, getElementCount(Locator.linkContainingText("Positivity ")));
        assertTextNotPresent("Positivity 3x Fold Change");
        clickButton("Confirm Delete");

        // now we exclude the analytes in the remaining run to test that version of the baseline visit query
        waitAndClickAndWait(Locator.linkWithText("Positivity 3x Fold Change"));
        excludeAnalyteForRun(_analyteNames.get(0), true, "");
        uploadPositivityFile(assayName + " Baseline Visit Previous Run 1", TEST_ASSAY_LUM_FILE12, "1", "3", false, false);
        checkPositivityValues("positive", 0, new String[0]);
        checkPositivityValues("negative", 0, new String[0]);
        clickAndWait(Locator.linkWithText("view runs"));
        waitAndClickAndWait(Locator.linkWithText("Positivity 3x Fold Change"));
        excludeAnalyteForRun(_analyteNames.get(0), false, "");

        // now we actual test the case of getting baseline visit data from a previously uploaded run
        _negControlAnalyte = _analyteNames.get(1);
        uploadPositivityFile(assayName + " Baseline Visit Previous Run 2", TEST_ASSAY_LUM_FILE12, "1", "3", false, true);
        String[] posWells = new String[] {"A2", "B2", "A6", "B6", "A9", "B9", "A10", "B10"};
        checkPositivityValues("positive", posWells.length, posWells);
        String[] negWells = new String[] {"A3", "B3", "A5", "B5"};
        checkPositivityValues("negative", negWells.length, negWells);
    }

    private void testWithNegativeControls(String assayName)
    {
        setPositivityThresholdParams(100, 100);
        setNegativeControlParams(false, true);
        uploadPositivityFile(assayName + " Negative Control", TEST_ASSAY_LUM_FILE11, "1", "5", false, true);
        checkPositivityValues("positive", 0, new String[0]);
        checkPositivityValues("negative", 0, new String[0]);
    }

    private void testWithoutBaselineVisitOrFoldChange(String assayName)
    {
        setPositivityThresholdParams(101, 101);
        uploadPositivityFile(assayName + " No Fold Change Error", TEST_ASSAY_LUM_FILE11, "1", "", false, true);
        assertTextPresent("Error: No value provided for 'Positivity Fold Change'.");
        clickButton("Cancel");

        // file contains the baseline visit data, which is not used in this case since we don't have a baseline visit run property set
        setPositivityThresholdParams(101, 100);
        uploadPositivityFile(assayName + " No Base Visit 1", TEST_ASSAY_LUM_FILE11, "", "", false, true);
        String[] posWells = new String[] {"A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A6", "B6", "A7", "B7", "A8", "B8", "A9", "B9", "A10", "B10"};
        checkPositivityValues("positive", posWells.length, posWells);
        String[] negWells = new String[] {"A5", "B5"};
        checkPositivityValues("negative", negWells.length, negWells);
        checkDescriptionParsing("123400001 1 2012-10-01", " ", "123400001", "1.0", "2012-10-01");
        checkDescriptionParsing("123400002,2,1/15/2012", " ", "123400002", "2.0", "2012-01-15");

        // file contains data that is only checked against thresholds (i.e. no baseline visit data)
        uploadPositivityFile(assayName + " No Base Visit 2", TEST_ASSAY_LUM_FILE13, "", "", false, false);
        posWells = new String[] {"C1", "D1"};
        checkPositivityValues("positive", posWells.length, posWells);
        negWells = new String[] {"C2", "D2", "C3", "D3", "C4", "D4", "C5", "D5"};
        checkPositivityValues("negative", negWells.length, negWells);
        checkDescriptionParsing("P562, Wk 48, 7-27-2011", " ", "P562", "48.0", "2011-07-27");
    }

    private void test5xFoldChange(String assayName)
    {
        // file contains the baseline visit data
        setPositivityThresholdParams(100, 101);
        uploadPositivityFile(assayName + " 5x Fold Change", TEST_ASSAY_LUM_FILE11, "1", "5", false, true);
        String[] posWells = new String[] {"A9", "B9", "A10", "B10"};
        checkPositivityValues("positive", posWells.length, posWells);
        String[] negWells = new String[] {"A2", "B2", "A3", "B3", "A5", "B5", "A6", "B6"};
        checkPositivityValues("negative", negWells.length, negWells);
        checkDescriptionParsing("123400001 1 2012-10-01", " ", "123400001", "1.0", "2012-10-01");
        checkDescriptionParsing("123400002,2,1/15/2012", " ", "123400002", "2.0", "2012-01-15");
    }

    private void test3xFoldChange(String assayName)
    {
        // file contains the baseline visit data
        setPositivityThresholdParams(100, 100);
        uploadPositivityFile(assayName + " 3x Fold Change", TEST_ASSAY_LUM_FILE11, "1", "3", false, false);
        String[] posWells = new String[] {"A2", "B2", "A6", "B6", "A9", "B9", "A10", "B10"};
        checkPositivityValues("positive", posWells.length, posWells);
        String[] negWells = new String[] {"A3", "B3", "A5", "B5"};
        checkPositivityValues("negative", negWells.length, negWells);

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Date");
        _customizeViewsHelper.saveCustomView();

        checkDescriptionParsing("123400001 1 2012-10-01", " ", "123400001", "1.0", "2012-10-01");
        checkDescriptionParsing("123400002,2,1/15/2012", " ", "123400002", "2.0", "2012-01-15");
    }

    private void setPositivityThresholdParams(int expectedValue, int newValue)
    {
        _expectedThresholdValue = expectedValue;
        _newThresholdValue = newValue;
    }

    private void setNegativeControlParams(boolean expectedValue, boolean newValue)
    {
        _expectedNegativeControlValue = expectedValue;
        _newNegativeControlValue = newValue;
    }

    @Override
    protected void setAnalytePropertyValues()
    {
        for (String analyteName : _analyteNames)
        {
            String inputName = "_analyte_" + analyteName + "_PositivityThreshold";
            Locator l = Locator.xpath("//input[@type='text' and @name='" + inputName + "'][1]");
            waitForElement(l);
            assertEquals(Integer.toString(_expectedThresholdValue), getFormElement(l));
            setFormElement(l, Integer.toString(_newThresholdValue));

            inputName = "_analyte_" + analyteName + "_NegativeControl";
            l = Locator.xpath("//input[@type='checkbox' and @name='" + inputName + "'][1]");
            waitForElement(l);
//            assertEquals(_expectedNegativeControlValue ? "1" : "0", getFormElement(l));
            if (!_expectedNegativeControlValue && _newNegativeControlValue)
                checkCheckbox(l);
            else if (_expectedNegativeControlValue && !_newNegativeControlValue)
                uncheckCheckbox(l);

            // special case for analyte that should always be considered negative control
            if (analyteName.equals(_negControlAnalyte))
                checkCheckbox(l);
        }

        if (_expectedThresholdValue != _newThresholdValue)
            _expectedThresholdValue = _newThresholdValue;

        if (!_expectedNegativeControlValue.equals(_newNegativeControlValue))
            _expectedNegativeControlValue = _newNegativeControlValue;
    }

    private void checkDescriptionParsing(String description, String specimenID, String participantID, String visitID, String date)
    {
        DataRegionTable drt = new DataRegionTable("Data", this);
        drt.ensureColumnsPresent("Description", "Specimen ID", "Participant ID", "Visit ID", "Date");
        int rowID = drt.getIndexWhereDataAppears(description, "Description");
        assertEquals(specimenID, drt.getDataAsText(rowID, "Specimen ID"));
        assertEquals(participantID, drt.getDataAsText(rowID, "Participant ID"));
        assertEquals(visitID, drt.getDataAsText(rowID, "Visit ID"));
        assertEquals(date, drt.getDataAsText(rowID, "Date"));
    }

    private void checkPositivityValues(String type, int numExpected, String[] positivityWells)
    {
        // verify that we are already on the Data results view
        assertElementPresent(Locator.tagWithText("span", "Exclude Analytes"));

        assertTextPresent(type, numExpected);

        DataRegionTable drt = new DataRegionTable("Data", this);
        List<String> posivitiy = drt.getColumnDataAsText("Positivity");
        List<String> wells = drt.getColumnDataAsText("Well");

        for(String well : positivityWells)
        {
            int i = wells.indexOf(well);
            assertEquals(type, posivitiy.get(i));
        }
    }
}

/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MiniTest;
import org.labkey.test.util.LogMethod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

@Category({DailyA.class, MiniTest.class, Assays.class})
public class LuminexMultipleCurvesTest extends LuminexTest
{
    @Override
    protected void ensureConfigured()
    {
        setUseXarImport(true);
        super.ensureConfigured();
    }

    protected void runUITests()
    {
        runMultipleCurveTest();
    }

    /**
     * Test our ability to upload multiple files and set multiple standards
     *
     */
    @LogMethod
    private void runMultipleCurveTest()
    {
        String name = startCreateMultipleCurveAssayRun();

        String[] standardsNames = {"Standard1", "Standard2"};
        checkStandardsCheckBoxesExist(standardsNames);

        String[] possibleAnalytes = getListOfAnalytesMultipleCurveData();
        String[] possibleStandards = new String[] {"Standard2", "Standard1"};

        Map<String, Set<String>> analytesAndStandardsConfig = generateAnalytesAndStandardsConfig(possibleAnalytes, possibleStandards);
        configureStandardsForAnalytes(analytesAndStandardsConfig, possibleStandards);



        clickButton("Save and Finish", 2*WAIT_FOR_PAGE);
        clickAndWait(Locator.linkWithText(name));

        //edit view to show Analyte Standard
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/Standard");
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/StdCurve");
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/FitProb");
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/ResVar");
        _customizeViewsHelper.applyCustomView();

        // We're OK with grabbing the footer curve fit from any of the files, under normal usage they should all share
        // the same curve fits
        assertTrue("BioPlex curve fit for ENV6 (97) in plate 1, 2, or 3",
                isTextPresent("FI = 0.465914 + (1.5417E+006 - 0.465914) / ((1 + (Conc / 122.733)^-0.173373))^7.64039") ||
                        isTextPresent("FI = 0.582906 + (167.081 - 0.582906) / ((1 + (Conc / 0.531813)^-5.30023))^0.1"));
        assertTrue("BioPlex FitProb for ENV6 (97) in plate 1, 2, or 3", isTextPresent("0.9667") || isTextPresent("0.4790"));
        assertTrue("BioPlex ResVar for ENV6 (97) in plate 1, 2, 3", isTextPresent("0.1895") || isTextPresent("0.8266"));

        compareColumnValuesAgainstExpected("Analyte", "Standard", analytesAndStandardsConfig);

        // Go to the schema browser to check out the parsed curve fits
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "CurveFit");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));

        // We're OK with grabbing the footer curve fit from any of the files, under normal usage they should all share
        // the same curve fits
        assertTrue("BioPlex curve fit parameter for ENV6 (97) in plate 1, 2, or 3", isTextPresent("0.465914") || isTextPresent("0.582906"));
        assertTrue("BioPlex curve fit parameter for ENV6 (97) in plate 1, 2, or 3", isTextPresent("7.64039") || isTextPresent("0.1"));
    }

    /**
     * Verify that the "set this  as standard" checkboxes exist and can be checked for the given standard names
     * preconditions:  at analyte properties page
     * postconditions:  unchanged
     * @param standardsNames
     */
    private void checkStandardsCheckBoxesExist(String[] standardsNames)
    {
        for(int i=0; i<standardsNames.length; i++)
        {
            String s = standardsNames[i];
            Locator l = Locator.checkboxByName("_titrationRole_standard_"+s);
            checkCheckbox(l);
            assertChecked(l);
        }
    }

    /**
     * using the list of analytes and standards, select one or more standards for each analyte.  Expects at least three
     * analytes and exactly two standards, and returns two analytes using different standards and the rest using both.
     * This is based on the test data we're currently using, can be changed to accomodate future changes
     *
     * preconditions: none, does not interact with server
     *
     * @param possibleAnalytes list of possible analytes
     * @param possibleStandards list of possible standards
     * @return map.  Key is the name of the analyte, value is a set of standards to be used for that analyte
     */
    private Map<String,Set<String>> generateAnalytesAndStandardsConfig(String[] possibleAnalytes, String[] possibleStandards)
    {
        Map<String, Set<String>> analytesAndStandardsConfig =  new HashMap<>();


        //based on the assumption that there are five analytes and two possible standards:  update this if you need to test for more
        Set<String> firstStandard = new HashSet<>(); firstStandard.add(possibleStandards[0]);
        Set<String> secondStandard = new HashSet<>(); secondStandard.add(possibleStandards[1]);
        Set<String> bothStandard = new HashSet<>();
        bothStandard.add(possibleStandards[0]);
        bothStandard.add(possibleStandards[1]);

        analytesAndStandardsConfig.put(possibleAnalytes[0], bothStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[1], firstStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[2], firstStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[3], secondStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[4], secondStandard);

        return analytesAndStandardsConfig;
    }

    /**
     * based on the instructions endcoded in the map, select the specified standards for each analyte
     *
     * preconditions:  on multiple curve data page.  analytes and standards must exist
     * postconditions: given check boxes checked and unchecked
     *
     * @param analytesAndTheirStandards map, where the keys are the analyte names and the values are sets of standard names,
     *      corresponding to the standards that should be used for the analyte.
     * @param standardsList list of all possible standards.  Important so that we know which boxes to uncheck when
     *      configuring which standard dto use
     */
    private void configureStandardsForAnalytes(Map<String, Set<String>> analytesAndTheirStandards, String[] standardsList)
    {
        Set<String> analytes = analytesAndTheirStandards.keySet();

        for(String analyte : analytes)
        {
            Set<String> analyteStandards = analytesAndTheirStandards.get(analyte);
            for(String standard: standardsList)
            {
                if(analyteStandards.contains(standard))
                    checkAnalyteAndStandardCheckBox(analyte, standard, true);
                else
                    checkAnalyteAndStandardCheckBox(analyte, standard, false);
            }
        }
    }

    /**
     * check or uncheck the checkbox for the given standard and analyte
     *
     * @param analyte
     * @param standard
     * @param checked
     */
    private void checkAnalyteAndStandardCheckBox(String analyte, String standard, boolean checked)
    {
        String checkboxName = "titration_" + analyte + "_" + standard;
        if(checked)
            checkCheckbox(Locator.checkboxByName(checkboxName));
        else
            uncheckCheckbox(Locator.checkboxByName(checkboxName));
    }
}

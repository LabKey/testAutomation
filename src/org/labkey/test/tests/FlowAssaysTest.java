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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.External;
import org.labkey.test.categories.ONPRC;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@Category({External.class, ONPRC.class})
public class FlowAssaysTest extends AbstractLabModuleAssayTest
{
    private static final String ICS_ASSAY_NAME = "ICS Assay Test";
    private static final String PHENOTYPE_ASSAY_NAME = "Immunophenotype Assay Test";
    private static final String[][] PHENOTYPE_TEST_DATA1 = new String[][]{
        {"Subject Id","Sample Date","Population","Result","Units","Freezer Id","Well","Parent Population","Comment"},
        {"Subj1","12/20/2012","CD4 T-cells","103","cells/uL","1","","","Comment"},
        {"Subj2","5/6/2011","CD8+ NK Cells","102","","2", ""},
        {"Subj3","4/5/2012","CD14 Mono","2","%","3","","Lymphocytes", ""},
        {"Subj3","4/6/2012","CD8 T-cells","342","","4", ""},
        {"Subj3","8/5/2012","CD8 Tcells","5321","","5", ""},
        {"Subj3","7/5/2012","CD8Tcells","4521","cells/uL","6","","","Comment4"}
    };

    private static final String[][] PHENOTYPE_PIVOT_DATA = new String[][]{
        {"Subject Id","Sample Date","Result","Units","Freezer Id","Well","Parent Population","Comment","CD4 T-Cells","CD8+ NK Cells","CD4 Tcells","CD8Tcells"},
        {"Subj1","2012-12-20","103","cells/uL","1","","","Comment","103","206","412","824"},
        {"Subj2","2011-05-06","102","","2","","","","102","204","408","816"},
        {"Subj3","2012-04-05","2","%","3","","Lymphocytes","","2","4","8","16"},
        {"Subj3","2012-04-06","342","","4","","","","342","684","1368","2736"},
        {"Subj3","2012-08-05","5321","","5","","","","5321","10642","21284","42568"},
        {"Subj3","2012-07-05","4521","cells/uL","6","","","Comment4","4521","9042","18084","36168"}
    };

    private static final String[][] ICS_DATA = new String[][]{
        {"Subject Id","Sample Date","Stimulation","Sample Category", "Population","Result","Units","Freezer Id","Well","Parent Population","Comment"},
        {"Subj1", "Gag Pool 1", "Unknown", "2012-12-20", "CD4 IFNg+", "103", "cells/uL", "1", "", "Comment"},
        {"Subj2", "Gag Pool 1", "Unknown", "2011-05-06", "CD4 IL-2+", "102", "", "2", "", ""},
        {"Subj3", "Gag Pool 2", "Unknown", "2012-04-05", "CD4 TNFa+", "2", "%", "3", "Lymphocytes", ""},
        {"Subj2", "Gag Pool 2", "Unknown", "2011-05-06", "CD8 All Respond", "2332", "%", "8", "Lymphocytes", ""},
        {"Subj3", "No Stim", "Neg Control", "2012-04-06", "CD4 All Respond", "342", "", "4", "", ""},
        {"Subj3", "No Stim", "Neg Control", "2012-08-05", "CD8 IL-2+", "5321", "", "5", "", ""},
        {"Subj3", "SEB", "Pos Control", "2012-07-05", "CD8 IFNg+", "4521", "cells/uL", "6", "", "Comment4"},
        {"Subj1", "SEB", "Pos Control", "2012-12-20", "CD8 TNFa+", "7654", "", "7", "", ""}
    };
    
    public FlowAssaysTest()
    {
        PROJECT_NAME = "FlowAssaysVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Test
    public void testSteps() throws Exception
    {
        setUpTest();

        ImmunophenotypeImportTest();
        ImmunophenotypePivotedImportTest();

//        ICSImportTest();
//        ICSPivotedImportTest();
    }

//    @Override
//    protected void doCleanup(boolean afterTest)
//    {
//    }

    private void ImmunophenotypeImportTest()
    {
        log("Verifying Basic Immunophenotype Import Test");
        _helper.goToAssayResultImport(PHENOTYPE_ASSAY_NAME, false);

        //a proxy for page loading
        _helper.waitForField("Sample Type", WAIT_FOR_JAVASCRIPT * 2);

        Ext4FieldRef.getForLabel(this, "Run Description").setValue("Description");
        Ext4FieldRef.getForLabel(this, "Assay Type").setValue(3);

        assertEquals("Incorrect value for field", "PBMC", Ext4FieldRef.getForLabel(this, "Sample Type").getValue());

        Ext4FieldRef textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRef.class);
        StringBuilder sb = new StringBuilder();
        for (String[] row : PHENOTYPE_TEST_DATA1)
        {
            sb.append(StringUtils.join(row, "\t")).append(System.getProperty("line.separator"));
        }
        String text = sb.toString();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("CD14 Mono", "NotRealPopulation");
        textarea.setValue(errorText);
        Ext4CmpRef btn = _ext4Helper.queryOne("#upload", Ext4CmpRef.class);
        btn.waitForEnabled();
        waitAndClick(Ext4Helper.Locators.ext4Button("Upload"));
        waitForElement(Ext4Helper.Locators.window("Upload Failed"));
        click(Ext4Helper.Locators.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        waitForText("Unknown value for population: NotRealPopulation");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Ext4Helper.Locators.ext4Button("Upload"));
        waitForElement(Ext4Helper.Locators.window("Success"));
        click(Ext4Helper.Locators.ext4Button("OK"));
        waitForText("Import Samples");

        log("Verifying results");
        _helper.clickNavPanelItemAndWait(PHENOTYPE_ASSAY_NAME + " Runs:", 1);
        waitAndClickAndWait(Locator.linkContainingText("view results"));

        List<String[]> expected = new ArrayList<>();
        expected.add(new String[]{"Subj1", "2012-12-20", "PBMC", "CD4 T-cells", "103", "cells/uL", "<1>", " ", "Comment"});
        expected.add(new String[]{"Subj2", "2011-05-06", "PBMC", "CD8+ NK Cells", "102", " ", "<2>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-04-05", "PBMC", "CD14 Mono", "2", "%", "<3>", "Lymphocytes", " "});
        expected.add(new String[]{"Subj3", "2012-04-06", "PBMC", "CD8 T-cells", "342", " ", "<4>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-08-05", "PBMC", "CD8 T-cells", "5321", " ", "<5>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-07-05", "PBMC", "CD8 T-cells", "4521", "cells/uL", "<6>", " ", "Comment4"});

        verifyResults(expected);
    }

    private void ImmunophenotypePivotedImportTest()
    {
        log("Verifying Pivoted Immunophenotype Import Test");
        _helper.goToAssayResultImport(PHENOTYPE_ASSAY_NAME, false);

        //a proxy for page loading
        _helper.waitForField("Sample Type", WAIT_FOR_JAVASCRIPT * 2);

        Ext4FieldRef field = Ext4FieldRef.getForBoxLabel(this, "Pivoted By Population");
        field.setChecked(true);
        _helper.waitForField("Sample Type");

        Ext4FieldRef.getForLabel(this, "Run Description").setValue("Description");
        Ext4FieldRef.getForLabel(this, "Assay Type").setValue(3);

        assertEquals("Incorrect value for field", "PBMC", Ext4FieldRef.getForLabel(this, "Sample Type").getValue());

        Ext4FieldRef textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRef.class);
        StringBuilder sb = new StringBuilder();
        for (String[] row : PHENOTYPE_PIVOT_DATA)
        {
            sb.append(StringUtils.join(row, "\t")).append(System.getProperty("line.separator"));
        }
        String text = sb.toString();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("CD4 T-Cells", "NotRealPopulation");
        textarea.setValue(errorText);

        Ext4CmpRef uploadBtn = _ext4Helper.queryOne("#upload", Ext4CmpRef.class);
        _helper.waitForDisabled(uploadBtn, false);

        waitAndClick(Ext4Helper.Locators.ext4Button("Upload"));
        waitForElement(Ext4Helper.Locators.window("Upload Failed"), WAIT_FOR_JAVASCRIPT * 2);
        click(Ext4Helper.Locators.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        waitForText("Unknown column: NotRealPopulation");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Ext4Helper.Locators.ext4Button("Upload"));
        waitForElement(Ext4Helper.Locators.window("Success"));
        click(Ext4Helper.Locators.ext4Button("OK"));
        waitForText("Import Samples");

        log("Verifying results");
        _helper.clickNavPanelItemAndWait(PHENOTYPE_ASSAY_NAME + " Runs:", 1);
        waitAndClickAndWait(Locator.linkContainingText("view results"));

        List<String[]> expected = new ArrayList<>();
        expected.add(new String[]{"Subj1", "2012-12-20", "PBMC", "CD4 T-cells", "103", "cells/uL", "<1>", " ", "Comment"});
        expected.add(new String[]{"Subj1", "2012-12-20", "PBMC", "CD8+ NK Cells", "206", "cells/uL", "<1>", " ", "Comment"});
        expected.add(new String[]{"Subj1", "2012-12-20", "PBMC", "CD4 T-cells", "412", "cells/uL", "<1>", " ", "Comment"});
        expected.add(new String[]{"Subj1", "2012-12-20", "PBMC", "CD8 T-cells", "824", "cells/uL", "<1>", " ", "Comment"});
        expected.add(new String[]{"Subj2", "2011-05-06", "PBMC", "CD4 T-cells", "102", " ", "<2>", " ", " "});
        expected.add(new String[]{"Subj2", "2011-05-06", "PBMC", "CD8+ NK Cells", "204", " ", "<2>", " ", " "});
        expected.add(new String[]{"Subj2", "2011-05-06", "PBMC", "CD4 T-cells", "408", " ", "<2>", " ", " "});
        expected.add(new String[]{"Subj2", "2011-05-06", "PBMC", "CD8 T-cells", "816", " ", "<2>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-04-05", "PBMC", "CD4 T-cells", "2", "%", "<3>", "Lymphocytes", " "});
        expected.add(new String[]{"Subj3", "2012-04-05", "PBMC", "CD8+ NK Cells", "4", "%", "<3>", "Lymphocytes", " "});
        expected.add(new String[]{"Subj3", "2012-04-05", "PBMC", "CD4 T-cells", "8", "%", "<3>", "Lymphocytes", " "});
        expected.add(new String[]{"Subj3", "2012-04-05", "PBMC", "CD8 T-cells", "16", "%", "<3>", "Lymphocytes", " "});
        expected.add(new String[]{"Subj3", "2012-04-06", "PBMC", "CD4 T-cells", "342", " ", "<4>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-04-06", "PBMC", "CD8+ NK Cells", "684", " ", "<4>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-04-06", "PBMC", "CD4 T-cells", "1368", " ", "<4>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-04-06", "PBMC", "CD8 T-cells", "2736", " ", "<4>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-08-05", "PBMC", "CD4 T-cells", "5321", " ", "<5>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-08-05", "PBMC", "CD8+ NK Cells", "10642", " ", "<5>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-08-05", "PBMC", "CD4 T-cells", "21284", " ", "<5>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-08-05", "PBMC", "CD8 T-cells", "42568", " ", "<5>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-07-05", "PBMC", "CD4 T-cells", "4521", "cells/uL", "<6>", " ", "Comment4"});
        expected.add(new String[]{"Subj3", "2012-07-05", "PBMC", "CD8+ NK Cells", "9042", "cells/uL", "<6>", " ", "Comment4"});
        expected.add(new String[]{"Subj3", "2012-07-05", "PBMC", "CD4 T-cells", "18084", "cells/uL", "<6>", " ", "Comment4"});
        expected.add(new String[]{"Subj3", "2012-07-05", "PBMC", "CD8 T-cells", "36168", "cells/uL", "<6>", " ", "Comment4"});

        verifyResults(expected);
    }

    private void ICSImportTest()
    {
        log("Verifying Basic ICS Import Test");
        _helper.goToAssayResultImport(ICS_ASSAY_NAME, false);

        //a proxy for page loading
        _helper.waitForField("Sample Type", WAIT_FOR_JAVASCRIPT * 2);

        Ext4FieldRef.getForLabel(this, "Run Description").setValue("Description");
        Ext4FieldRef.getForLabel(this, "Assay Type").setValue(3);

        assertEquals("Incorrect value for field", "PBMC", Ext4FieldRef.getForLabel(this, "Sample Type").getValue());

        Ext4FieldRef textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRef.class);
        StringBuilder sb = new StringBuilder();
        for (String[] row : ICS_DATA)
        {
            sb.append(StringUtils.join(row, "\t")).append(System.getProperty("line.separator"));
        }
        String text = sb.toString();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("CD14 Mono", "NotRealPopulation");
        textarea.setValue(errorText);
        waitAndClick(Ext4Helper.Locators.ext4Button("Upload"));
        waitForElement(Ext4Helper.Locators.window("Upload Failed"));
        click(Ext4Helper.Locators.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        waitForText("Unknown value for population: NotRealPopulation");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Ext4Helper.Locators.ext4Button("Upload"));
        waitForElement(Ext4Helper.Locators.window("Success"));
        click(Ext4Helper.Locators.ext4Button("OK"));
        waitForText("Import Samples");

        log("Verifying results");
        _helper.clickNavPanelItemAndWait(ICS_ASSAY_NAME + " Runs:", 1);
        waitAndClickAndWait(Locator.linkContainingText("view results"));

        List<String[]> expected = new ArrayList<>();

        verifyResults(expected);
    }




    private void verifyResults(List<String[]> expected)
    {
        DataRegionTable results = new DataRegionTable("Data", this);
        waitForText("cells/uL"); //proxy for DR loading
        assertEquals("Incorrect row count", expected.size(), results.getDataRowCount());

        log("DataRegion column count was: " + results.getColumnCount());

        //recreate the DR to see if this removes intermittent test failures
        results = new DataRegionTable(results.getTableName(), this);

        int i = 0;
        while (i < expected.size())
        {
            String[] expectedVals = expected.get(i);
            String subjectId = results.getDataAsText(i, "Subject Id");
            String date = results.getDataAsText(i, "Sample Date");
            String type = results.getDataAsText(i, "Sample Type");
            String population = results.getDataAsText(i, "Population");
            String result = results.getDataAsText(i, "Result");
            String units = results.getDataAsText(i, "Units");
            String freezer = results.getDataAsText(i, "Freezer Id");
            String parentPopulation = results.getDataAsText(i, "Parent Population");
            String comments = results.getDataAsText(i, "Comment");

            assertEquals("Incorrect subjectId on row " + i, expectedVals[0], subjectId);
            assertEquals("Incorrect date on row " + i, expectedVals[1], date);
            assertEquals("Incorrect sampleType on row " + i, expectedVals[2], type);
            assertEquals("Incorrect population on row " + i, expectedVals[3], population);
            assertEquals("Incorrect result on row " + i, expectedVals[4], result);
            assertEquals("Incorrect units on row " + i, expectedVals[5], units);
            assertEquals("Incorrect freezerId on row " + i, expectedVals[6], freezer);
            assertEquals("Incorrect parent population on row " + i, expectedVals[7], parentPopulation);
            assertEquals("Incorrect comments on row " + i, expectedVals[8], comments);
            i++;
        }
    }

    @Override
    protected List<Pair<String, String>> getAssaysToCreate()
    {
        List<Pair<String, String>> assays = new ArrayList<>();
        assays.add(Pair.of("ICS", ICS_ASSAY_NAME));
        assays.add(Pair.of("Immunophenotyping", PHENOTYPE_ASSAY_NAME));

        return assays;
    }

    @Override
    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<>();
        modules.add("FlowAssays");
        return modules;
    }
}

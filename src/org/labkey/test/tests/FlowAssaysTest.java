package org.labkey.test.tests;

import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 12/9/12
 * Time: 11:24 PM
 */
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
        setContainerHelper(new UIContainerHelper(this));
        PROJECT_NAME = "FlowAssaysVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    protected void doTestSteps() throws Exception
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
        _helper.waitForField("Sample Type");

        Ext4FieldRefWD.getForLabel(this, "Run Description").setValue("Description");
        Ext4FieldRefWD.getForLabel(this, "Assay Type").setValue(3);

        Assert.assertEquals("Incorrect value for field", "PBMC", Ext4FieldRefWD.getForLabel(this, "Sample Type").getValue());

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        StringBuilder sb = new StringBuilder();
        for (String[] row : PHENOTYPE_TEST_DATA1)
        {
            sb.append(StringUtils.join(row, "\t")).append(System.getProperty("line.separator"));
        }
        String text = sb.toString();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("CD14 Mono", "NotRealPopulation");
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Upload Failed"));
        click(Locator.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        waitForText("Unknown value for population: NotRealPopulation");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText("Import Samples");

        log("Verifying results");
        _helper.clickNavPanelItem(PHENOTYPE_ASSAY_NAME + " Runs:", 1);
        waitForPageToLoad();
        waitAndClick(Locator.linkContainingText("view results"));
        waitForPageToLoad();

        List<String[]> expected = new ArrayList<String[]>();
        expected.add(new String[]{"Subj1", "2012-12-20", "PBMC", "CD4 T-cells", "103.0", "cells/uL", "<1>", " ", "Comment"});
        expected.add(new String[]{"Subj2", "2011-05-06", "PBMC", "CD8+ NK Cells", "102.0", " ", "<2>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-04-05", "PBMC", "CD14 Mono", "2.0", "%", "<3>", "Lymphocytes", " "});
        expected.add(new String[]{"Subj3", "2012-04-06", "PBMC", "CD8 T-cells", "342.0", " ", "<4>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-08-05", "PBMC", "CD8 T-cells", "5321.0", " ", "<5>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-07-05", "PBMC", "CD8 T-cells", "4521.0", "cells/uL", "<6>", " ", "Comment4"});

        verifyResults(expected);
    }

    private void ImmunophenotypePivotedImportTest()
    {
        log("Verifying Pivoted Immunophenotype Import Test");
        _helper.goToAssayResultImport(PHENOTYPE_ASSAY_NAME, false);

        //a proxy for page loading
        _helper.waitForField("Sample Type");

        Ext4FieldRefWD field = Ext4FieldRefWD.getForBoxLabel(this, "Pivoted By Population");
        field.setChecked(true);
        _helper.waitForField("Sample Type");

        Ext4FieldRefWD.getForLabel(this, "Run Description").setValue("Description");
        Ext4FieldRefWD.getForLabel(this, "Assay Type").setValue(3);

        Assert.assertEquals("Incorrect value for field", "PBMC", Ext4FieldRefWD.getForLabel(this, "Sample Type").getValue());

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        StringBuilder sb = new StringBuilder();
        for (String[] row : PHENOTYPE_PIVOT_DATA)
        {
            sb.append(StringUtils.join(row, "\t")).append(System.getProperty("line.separator"));
        }
        String text = sb.toString();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("CD4 T-Cells", "NotRealPopulation");
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Upload Failed"));
        click(Locator.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        waitForText("Unknown column: NotRealPopulation");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText("Import Samples");

        log("Verifying results");
        _helper.clickNavPanelItem(PHENOTYPE_ASSAY_NAME + " Runs:", 1);
        waitForPageToLoad();
        waitAndClick(Locator.linkContainingText("view results"));
        waitForPageToLoad();

        List<String[]> expected = new ArrayList<String[]>();
        expected.add(new String[]{"Subj1", "2012-12-20", "PBMC", "CD4 T-cells", "103.0", "cells/uL", "<1>", " ", "Comment"});
        expected.add(new String[]{"Subj1", "2012-12-20", "PBMC", "CD8+ NK Cells", "206.0", "cells/uL", "<1>", " ", "Comment"});
        expected.add(new String[]{"Subj1", "2012-12-20", "PBMC", "CD4 T-cells", "412.0", "cells/uL", "<1>", " ", "Comment"});
        expected.add(new String[]{"Subj1", "2012-12-20", "PBMC", "CD8 T-cells", "824.0", "cells/uL", "<1>", " ", "Comment"});
        expected.add(new String[]{"Subj2", "2011-05-06", "PBMC", "CD4 T-cells", "102.0", " ", "<2>", " ", " "});
        expected.add(new String[]{"Subj2", "2011-05-06", "PBMC", "CD8+ NK Cells", "204.0", " ", "<2>", " ", " "});
        expected.add(new String[]{"Subj2", "2011-05-06", "PBMC", "CD4 T-cells", "408.0", " ", "<2>", " ", " "});
        expected.add(new String[]{"Subj2", "2011-05-06", "PBMC", "CD8 T-cells", "816.0", " ", "<2>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-04-05", "PBMC", "CD4 T-cells", "2.0", "%", "<3>", "Lymphocytes", " "});
        expected.add(new String[]{"Subj3", "2012-04-05", "PBMC", "CD8+ NK Cells", "4.0", "%", "<3>", "Lymphocytes", " "});
        expected.add(new String[]{"Subj3", "2012-04-05", "PBMC", "CD4 T-cells", "8.0", "%", "<3>", "Lymphocytes", " "});
        expected.add(new String[]{"Subj3", "2012-04-05", "PBMC", "CD8 T-cells", "16.0", "%", "<3>", "Lymphocytes", " "});
        expected.add(new String[]{"Subj3", "2012-04-06", "PBMC", "CD4 T-cells", "342.0", " ", "<4>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-04-06", "PBMC", "CD8+ NK Cells", "684.0", " ", "<4>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-04-06", "PBMC", "CD4 T-cells", "1368.0", " ", "<4>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-04-06", "PBMC", "CD8 T-cells", "2736.0", " ", "<4>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-08-05", "PBMC", "CD4 T-cells", "5321.0", " ", "<5>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-08-05", "PBMC", "CD8+ NK Cells", "10642.0", " ", "<5>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-08-05", "PBMC", "CD4 T-cells", "21284.0", " ", "<5>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-08-05", "PBMC", "CD8 T-cells", "42568.0", " ", "<5>", " ", " "});
        expected.add(new String[]{"Subj3", "2012-07-05", "PBMC", "CD4 T-cells", "4521.0", "cells/uL", "<6>", " ", "Comment4"});
        expected.add(new String[]{"Subj3", "2012-07-05", "PBMC", "CD8+ NK Cells", "9042.0", "cells/uL", "<6>", " ", "Comment4"});
        expected.add(new String[]{"Subj3", "2012-07-05", "PBMC", "CD4 T-cells", "18084.0", "cells/uL", "<6>", " ", "Comment4"});
        expected.add(new String[]{"Subj3", "2012-07-05", "PBMC", "CD8 T-cells", "36168.0", "cells/uL", "<6>", " ", "Comment4"});

        verifyResults(expected);
    }

    private void ICSImportTest()
    {
        log("Verifying Basic ICS Import Test");
        _helper.goToAssayResultImport(ICS_ASSAY_NAME, false);

        //a proxy for page loading
        _helper.waitForField("Sample Type");

        Ext4FieldRefWD.getForLabel(this, "Run Description").setValue("Description");
        Ext4FieldRefWD.getForLabel(this, "Assay Type").setValue(3);

        Assert.assertEquals("Incorrect value for field", "PBMC", Ext4FieldRefWD.getForLabel(this, "Sample Type").getValue());

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        StringBuilder sb = new StringBuilder();
        for (String[] row : ICS_DATA)
        {
            sb.append(StringUtils.join(row, "\t")).append(System.getProperty("line.separator"));
        }
        String text = sb.toString();

        log("Trying to save invalid data");
        String errorText = text.replaceAll("CD14 Mono", "NotRealPopulation");
        textarea.setValue(errorText);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Upload Failed"));
        click(Locator.ext4Button("OK"));
        assertTextPresent("There were errors in the upload");
        waitForText("Unknown value for population: NotRealPopulation");

        log("Saving valid data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText("Import Samples");

        log("Verifying results");
        _helper.clickNavPanelItem(ICS_ASSAY_NAME + " Runs:", 1);
        waitForPageToLoad();
        waitAndClick(Locator.linkContainingText("view results"));
        waitForPageToLoad();

        List<String[]> expected = new ArrayList<String[]>();

        verifyResults(expected);
    }




    private void verifyResults(List<String[]> expected)
    {
        DataRegionTable results = new DataRegionTable("Data", this);
        Assert.assertEquals("Incorrect row count", expected.size(), results.getDataRowCount());

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

            Assert.assertEquals("Incorrect subjectId", expectedVals[0], subjectId);
            Assert.assertEquals("Incorrect date", expectedVals[1], date);
            Assert.assertEquals("Incorrect sampleType", expectedVals[2], type);
            Assert.assertEquals("Incorrect population", expectedVals[3], population);
            Assert.assertEquals("Incorrect result", expectedVals[4], result);
            Assert.assertEquals("Incorrect units", expectedVals[5], units);
            Assert.assertEquals("Incorrect freezerId", expectedVals[6], freezer);
            Assert.assertEquals("Incorrect parent population", expectedVals[7], parentPopulation);
            Assert.assertEquals("Incorrect comments", expectedVals[8], comments);
            i++;
        }
    }

    @Override
    protected List<Pair<String, String>> getAssaysToCreate()
    {
        List<Pair<String, String>> assays = new ArrayList<Pair<String, String>>();
        assays.add(Pair.of("ICS", ICS_ASSAY_NAME));
        assays.add(Pair.of("Immunophenotyping", PHENOTYPE_ASSAY_NAME));

        return assays;
    }

    @Override
    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<String>();
        modules.add("FlowAssays");
        return modules;
    }
}

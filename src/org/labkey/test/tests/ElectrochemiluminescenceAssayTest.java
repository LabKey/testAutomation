package org.labkey.test.tests;

import junit.framework.Assert;
import org.apache.commons.lang3.tuple.Pair;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 11/19/12
 * Time: 5:08 PM
 */
public class ElectrochemiluminescenceAssayTest extends LabModulesTest
{
    private static final String ASSAY_NAME = "Electrochemiluminescence Test";
    private static final String[][] TEMPLATE_DATA = new String[][]{
        {"Well", "Category", "Subject Id", "Sample Date", "Diluent", "Dilution Factor"},
        {"104", "Blank", "DU", "", "DU", "1"},
        {"105", "Blank", "MD1", "", "MD1", "1"},
        {"106", "Blank", "MA", "", "MA", "1"},
        {"107", "Unknown", "Subject4", "2012-01-05", "DU", "2"},
        {"108", "Unknown", "Subject5", "2012-01-06", "MD1", "4"},
        {"109", "Unknown", "Subject6", "2012-01-07", "MA", "10"},
        {"110", "Unknown", "Subject7", "2012-01-08", "DU", "5"},
        {"111", "Unknown", "Subject8", "2012-01-09", "MD1", "2"},
        {"112", "Unknown", "Subject9", "2012-01-10", "MA", "2"},
        {"113", "Unknown", "Subject10", "2012-01-11", "", ""},
        {"114", "Unknown", "Subject11", "2012-01-12", "", ""},
        {"115", "Unknown", "Subject12", "2012-01-13", "", ""},
        {"116", "Unknown", "Subject13", "2012-01-14", "", ""},
        {"117", "Unknown", "Subject14", "2012-01-15", "", ""},
        {"118", "Unknown", "Subject15", "2012-01-16", "", ""},
        {"119", "Unknown", "Subject16", "2012-01-17", "", ""},
        {"120", "Unknown", "Subject17", "2012-01-18", "", ""},
        {"121", "Unknown", "Subject18", "2012-01-19", "", ""},
        {"122", "Unknown", "Subject19", "2012-01-20", "", ""},
        {"123", "Unknown", "Subject20", "2012-01-21", "", ""},
        {"124", "Unknown", "Subject21", "2012-01-22", "", ""},
        {"125", "Unknown", "Subject22", "2012-01-23", "", ""},
        {"126", "Unknown", "Subject23", "2012-01-24", "", ""},
        {"127", "Unknown", "Subject24", "2012-01-25", "", ""},
        {"128", "Unknown", "Subject25", "2012-01-26", "", ""},
        {"129", "Unknown", "Subject26", "2012-01-27", "", ""},
        {"130", "Unknown", "Subject27", "2012-01-28", "", ""},
        {"131", "Unknown", "Subject28", "2012-01-29", "", ""},
        {"132", "Unknown", "Subject29", "2012-01-30", "", ""},
        {"133", "Unknown", "Subject30", "2012-01-31", "", ""},
        {"134", "Unknown", "Subject31", "2012-02-01", "", ""},
        {"135", "Unknown", "Subject32", "2012-02-02", "", ""},
        {"136", "Unknown", "Subject33", "2012-02-03", "", ""},
        {"137", "Unknown", "Subject34", "2012-02-04", "", ""},
        {"138", "Unknown", "Subject35", "2012-02-05", "", ""},
        {"139", "Unknown", "Subject36", "2012-02-06", "", ""},
        {"140", "Unknown", "Subject37", "2012-02-07", "", ""},
        {"141", "Unknown", "Subject38", "2012-02-08", "", ""},
        {"142", "Unknown", "Subject39", "2012-02-09", "", ""},
        {"143", "Unknown", "Subject40", "2012-02-10", "", ""},
        {"144", "Unknown", "Subject41", "2012-02-11", "", ""},
        {"145", "Unknown", "Subject42", "2012-02-12", "", ""},
        {"146", "Unknown", "Subject43", "2012-02-13", "", ""},
        {"301", "Unknown", "Subject44", "2012-02-14", "", ""},
        {"302", "Unknown", "Subject45", "2012-02-15", "", ""},
        {"303", "Unknown", "Subject46", "2012-02-16", "", ""},
        {"304", "Unknown", "Subject47", "2012-02-17", "", ""},
        {"305", "Unknown", "Subject48", "2012-02-18", "", ""},
        {"306", "Unknown", "Subject49", "2012-02-19", "", ""},
        {"307", "Unknown", "Subject50", "2012-02-20", "", ""},
        {"308", "Unknown", "Subject51", "2012-02-21", "", ""},
        {"309", "Unknown", "Subject52", "2012-02-22", "", ""},
        {"310", "Unknown", "Subject53", "2012-02-23", "", ""},
        {"311", "Unknown", "Subject54", "2012-02-24", "", ""},
        {"312", "Unknown", "Subject55", "2012-02-25", "", ""},
        {"313", "Unknown", "Subject56", "2012-02-26", "", ""},
        {"314", "Unknown", "Subject57", "2012-02-27", "", ""},
        {"315", "Unknown", "Subject58", "2012-02-28", "", ""},
        {"316", "Unknown", "Subject59", "2012-02-29", "", ""},
        {"317", "Unknown", "Subject60", "2012-03-01", "", ""},
        {"318", "Unknown", "Subject61", "2012-03-02", "", ""},
        {"401", "Unknown", "Subject62", "2012-03-03", "", ""},
        {"402", "Unknown", "Subject63", "2012-03-04", "", ""},
        {"403", "Unknown", "Subject64", "2012-03-05", "", ""},
        {"404", "Unknown", "Subject65", "2012-03-06", "", ""},
        {"405", "Unknown", "Subject66", "2012-03-07", "", ""},
        {"406", "Unknown", "Subject67", "2012-03-08", "", ""},
        {"407", "Unknown", "Subject68", "2012-03-09", "", ""},
        {"408", "Unknown", "Subject69", "2012-03-10", "", ""},
        {"409", "Unknown", "Subject70", "2012-03-11", "", ""},
        {"410", "Unknown", "Subject71", "2012-03-12", "", ""},
        {"411", "Unknown", "Subject72", "2012-03-13", "", ""},
        {"412", "Unknown", "Subject73", "2012-03-14", "", ""},
        {"413", "Unknown", "Subject74", "2012-03-15", "", ""},
        {"414", "Unknown", "Subject75", "2012-03-16", "", ""},
        {"415", "Unknown", "Subject76", "2012-03-17", "", ""},
        {"416", "Unknown", "Subject77", "2012-03-18", "", ""},
        {"417", "Unknown", "Subject78", "2012-03-19", "", ""},
        {"418", "Unknown", "Subject79", "2012-03-20", "", ""},
        {"601", "Unknown", "Subject80", "2012-03-21", "", ""},
        {"602", "Unknown", "Subject81", "2012-03-22", "", ""},
        {"603", "Unknown", "Subject82", "2012-03-23", "", ""},
        {"604", "Unknown", "Subject83", "2012-03-24", "", ""},
        {"701", "Unknown", "Subject84", "2012-03-25", "", ""},
        {"702", "Unknown", "Subject85", "2012-03-26", "", ""},
        {"703", "Unknown", "Subject86", "2012-03-27", "", ""},
        {"704", "Unknown", "Subject87", "2012-03-28", "", ""},
        {"801", "Unknown", "Subject88", "2012-03-29", "", ""},
        {"802", "Unknown", "Subject89", "2012-03-30", "", ""},
        {"803", "Unknown", "Subject90", "2012-03-31", "", ""},
        {"804", "Unknown", "Subject91", "2012-04-01", "", ""},
        {"901", "Unknown", "Subject92", "2012-04-02", "", ""},
        {"902", "Unknown", "Subject93", "2012-04-03", "", ""},
        {"903", "Unknown", "Subject94", "2012-04-04", "", ""},
        {"904", "Unknown", "Subject95", "2012-04-05", "", ""}
    };

    public ElectrochemiluminescenceAssayTest()
    {
        setContainerHelper(new UIContainerHelper(this));
        PROJECT_NAME = "EC_AssayVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUpTest();
        createPlateTemplate();
        importResults();
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
        expectedCols.add("dilutionFactor");
        expectedCols.add("diluent");
        expectedCols.add("comment");
        expectedCols.add("sampleId");

        waitForElement(Locator.xpath("//span[contains(text(), 'Freezer Id') and contains(@class, 'x4-column-header-text')]")); //ensure grid loaded
        _helper.addRecordsToAssayTemplate(TEMPLATE_DATA, expectedCols);

        waitForText("904");  //this is the last sample

        waitAndClick(Locator.ext4Button("Save and Close"));
        waitForText("Save Complete");
        waitAndClick(Locator.ext4Button("OK"));
        waitForPageToLoad();
    }

    private void importResults()
    {
        log("Verifying Roche E411 Import");
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

        Assert.assertEquals("Incorrect value for field", "Roche E411", Ext4FieldRefWD.getForLabel(this, "Instrument").getValue());
        Assert.assertEquals("Incorrect value for field", "Electrochemiluminescence", Ext4FieldRefWD.getForLabel(this, "Method").getValue());
        Assert.assertEquals("Incorrect value for field", "Serum", Ext4FieldRefWD.getForLabel(this, "Sample Type").getValue());
        waitAndClick(btn);

        Ext4FieldRefWD textarea = _ext4Helper.queryOne("#fileContent", Ext4FieldRefWD.class);
        String text = _helper.getExampleData();

        //TODO
//        log("Trying to save invalid data");
//        String errorText = text.replaceAll("A1=231841", "");
//        errorText = errorText.replaceAll("A3=432947\tDETECTOR1", "A3=432947\tDETECTOR2");
//        textarea.setValue(errorText);
//        waitAndClick(Locator.ext4Button("Upload"));
//        waitForElement(Ext4Helper.ext4Window("Upload Failed"));
//        click(Locator.ext4Button("OK"));
//        assertTextPresent("There were errors in the upload");
//        assertTextPresent("Missing sample name for row: 9");
//        assertTextPresent("Row 11: Unable to find detector information for detector: DETECTOR2");

        log("Trying to save data");
        textarea.setValue(text);
        waitAndClick(Locator.ext4Button("Upload"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText("Import Samples");

        verifyResults();

        log("verifying run plan marked as complete");
        _helper.goToLabHome();
        _helper.clickNavPanelItem(ASSAY_NAME + ":", IMPORT_DATA_TEXT);
        _ext4Helper.clickExt4MenuItem("View Planned Runs");
        waitForPageToLoad();
        DataRegionTable dr2 = new DataRegionTable("query", this);
        Assert.assertEquals("Run plan not marked completed", 0, dr2.getDataRowCount());
    }

    private void verifyResults()
    {
        log("Verifying results");
        _helper.clickNavPanelItem(ASSAY_NAME + " Runs:", 1);
        waitForPageToLoad();
        waitAndClick(Locator.linkContainingText("view results"));
        waitForPageToLoad();

        DataRegionTable results = new DataRegionTable("Data", this);

        Map<String, String[]> expected = new LinkedHashMap<String, String[]>();
        expected.put("DU_DHEAS", new String[]{"DU", "", "DHEAS", "0.11", "ug/ml", "Blank", "DU", "1", " "});
        expected.put("MD1_DHEAS", new String[]{"MD1", "", "DHEAS", "0.079", "ug/ml", "Blank", "MD1", "1", " "});
        expected.put("MA_Progesterone", new String[]{"MA", "", "Progesterone", "1.56", "ng/ml", "Blank", "MA", "1", " "});
        expected.put("MA_Estradiol", new String[]{"MA", "", "Estradiol", "15.35", "pg/ml", "Blank", "MA", "1", " "});
        expected.put("Subject4_Progesterone", new String[]{"Subject4", "2012-01-05", "Progesterone", "0", "ng/ml", "Unknown", "DU", "2", " "});
        expected.put("Subject4_Estradiol", new String[]{"Subject4", "2012-01-05", "Estradiol", "189.58999999999997", "pg/ml", "Unknown", "DU", "2", " "});
        expected.put("Subject5_Progesterone", new String[]{"Subject5", "2012-01-06", "Progesterone", "-0.049", "ng/ml", "Unknown", "MD1", "4", "Test, Blank greater than result"});
        expected.put("Subject5_Estradiol", new String[]{"Subject5", "2012-01-06", "Estradiol", "4.921", "pg/ml", "Unknown", "MD1", "4", "Test"});
        expected.put("Subject6_Progesterone", new String[]{"Subject6", "2012-01-07", "Progesterone", "-8.366", "ng/ml", "Unknown", "MA", "10", "Blank greater than result"});
        expected.put("Subject6_Estradiol", new String[]{"Subject6", "2012-01-07", "Estradiol", "-3.455", "pg/ml", "Unknown", "MA", "10", "Test, Blank greater than result"});
        expected.put("Subject7_Progesterone", new String[]{"Subject7", "2012-01-08", "Progesterone", "5.37", "ng/ml", "Unknown", "DU", "5", " "});
        expected.put("Subject7_Estradiol", new String[]{"Subject7", "2012-01-08", "Estradiol", "39.34", "pg/ml", "Unknown", "DU", "5", " "});
        expected.put("Subject8_Progesterone", new String[]{"Subject8", "2012-01-09", "Progesterone", "-0.011999999999999997", "ng/ml", "Unknown", "MD1", "2", "Blank greater than result"});
        expected.put("Subject8_Estradiol", new String[]{"Subject8", "2012-01-09", "Estradiol", "24.781", "pg/ml", "Unknown", "MD1", "2", " "});
        expected.put("Subject9_Progesterone", new String[]{"Subject9", "2012-01-10", "Progesterone", "-8.316", "ng/ml", "Unknown", "MA", "2", "Blank greater than result"});
        expected.put("Subject9_Estradiol", new String[]{"Subject9", "2012-01-10", "Estradiol", "0.29499999999999993", "pg/ml", "Unknown", "MA", "2", " "});
        expected.put("Subject10_Progesterone", new String[]{"Subject10", "2012-01-11", "Progesterone", "0.163", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject10_Estradiol", new String[]{"Subject10", "2012-01-11", "Estradiol", "12.39", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject11_Progesterone", new String[]{"Subject11", "2012-01-12", "Progesterone", "0.232", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject11_Estradiol", new String[]{"Subject11", "2012-01-12", "Estradiol", "19.58", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject12_Progesterone", new String[]{"Subject12", "2012-01-13", "Progesterone", "0.375", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject12_Estradiol", new String[]{"Subject12", "2012-01-13", "Estradiol", "21.65", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject13_Progesterone", new String[]{"Subject13", "2012-01-14", "Progesterone", "0.186", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject13_Estradiol", new String[]{"Subject13", "2012-01-14", "Estradiol", "44.98", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject14_Progesterone", new String[]{"Subject14", "2012-01-15", "Progesterone", "0.265", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject14_Estradiol", new String[]{"Subject14", "2012-01-15", "Estradiol", "87.17", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject15_Progesterone", new String[]{"Subject15", "2012-01-16", "Progesterone", "0.301", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject15_Estradiol", new String[]{"Subject15", "2012-01-16", "Estradiol", "155.9", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject16_Progesterone", new String[]{"Subject16", "2012-01-17", "Progesterone", "0.741", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject16_Estradiol", new String[]{"Subject16", "2012-01-17", "Estradiol", "24.82", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject17_Progesterone", new String[]{"Subject17", "2012-01-18", "Progesterone", "2.72", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject17_Estradiol", new String[]{"Subject17", "2012-01-18", "Estradiol", "48.51", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject18_Progesterone", new String[]{"Subject18", "2012-01-19", "Progesterone", "0.275", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject18_Estradiol", new String[]{"Subject18", "2012-01-19", "Estradiol", "70.94", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject19_Progesterone", new String[]{"Subject19", "2012-01-20", "Progesterone", "0.068", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject19_Estradiol", new String[]{"Subject19", "2012-01-20", "Estradiol", "98.54", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject20_Progesterone", new String[]{"Subject20", "2012-01-21", "Progesterone", "2.13", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject20_Estradiol", new String[]{"Subject20", "2012-01-21", "Estradiol", "22.89", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject21_Progesterone", new String[]{"Subject21", "2012-01-22", "Progesterone", "4.83", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject21_Estradiol", new String[]{"Subject21", "2012-01-22", "Estradiol", "59.54", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject22_Progesterone", new String[]{"Subject22", "2012-01-23", "Progesterone", "0.269", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject22_Estradiol", new String[]{"Subject22", "2012-01-23", "Estradiol", "70.29", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject23_Progesterone", new String[]{"Subject23", "2012-01-24", "Progesterone", "0.056", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject23_Estradiol", new String[]{"Subject23", "2012-01-24", "Estradiol", "73.34", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject24_Progesterone", new String[]{"Subject24", "2012-01-25", "Progesterone", "3.38", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject24_Estradiol", new String[]{"Subject24", "2012-01-25", "Estradiol", "21.57", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject25_Progesterone", new String[]{"Subject25", "2012-01-26", "Progesterone", "4.7", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject25_Estradiol", new String[]{"Subject25", "2012-01-26", "Estradiol", "58.43", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject26_Progesterone", new String[]{"Subject26", "2012-01-27", "Progesterone", "0.26", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject26_Estradiol", new String[]{"Subject26", "2012-01-27", "Estradiol", "57.74", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject27_Progesterone", new String[]{"Subject27", "2012-01-28", "Progesterone", "0.08", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject27_Estradiol", new String[]{"Subject27", "2012-01-28", "Estradiol", "84.3", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject28_Progesterone", new String[]{"Subject28", "2012-01-29", "Progesterone", "7.6", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject28_Estradiol", new String[]{"Subject28", "2012-01-29", "Estradiol", "2706", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject29_Progesterone", new String[]{"Subject29", "2012-01-30", "Progesterone", "0.055", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject29_Estradiol", new String[]{"Subject29", "2012-01-30", "Estradiol", "48.59", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject30_Estradiol", new String[]{"Subject30", "2012-01-31", "Estradiol", "70.84", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject31_Progesterone", new String[]{"Subject31", "2012-02-01", "Progesterone", "0.064", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject31_Estradiol", new String[]{"Subject31", "2012-02-01", "Estradiol", "51.86", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject32_Progesterone", new String[]{"Subject32", "2012-02-02", "Progesterone", "0.066", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject32_Estradiol", new String[]{"Subject32", "2012-02-02", "Estradiol", "43.86", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject33_Estradiol", new String[]{"Subject33", "2012-02-03", "Estradiol", "77.97", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject34_Progesterone", new String[]{"Subject34", "2012-02-04", "Progesterone", "0.298", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject34_Estradiol", new String[]{"Subject34", "2012-02-04", "Estradiol", "2949", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject35_Progesterone", new String[]{"Subject35", "2012-02-05", "Progesterone", "0.053", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject35_Estradiol", new String[]{"Subject35", "2012-02-05", "Estradiol", "89.47", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject36_Estradiol", new String[]{"Subject36", "2012-02-06", "Estradiol", "127.7", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject37_Progesterone", new String[]{"Subject37", "2012-02-07", "Progesterone", "0.161", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject37_Estradiol", new String[]{"Subject37", "2012-02-07", "Estradiol", "128", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject38_Progesterone", new String[]{"Subject38", "2012-02-08", "Progesterone", "0.995", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject38_Estradiol", new String[]{"Subject38", "2012-02-08", "Estradiol", "1549", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject39_Progesterone", new String[]{"Subject39", "2012-02-09", "Progesterone", "0.049", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject39_Estradiol", new String[]{"Subject39", "2012-02-09", "Estradiol", "72.49", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject40_Estradiol", new String[]{"Subject40", "2012-02-10", "Estradiol", "328.1", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject41_Progesterone", new String[]{"Subject41", "2012-02-11", "Progesterone", "0.056", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject41_Estradiol", new String[]{"Subject41", "2012-02-11", "Estradiol", "124.8", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject42_Progesterone", new String[]{"Subject42", "2012-02-12", "Progesterone", "0.128", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject42_Estradiol", new String[]{"Subject42", "2012-02-12", "Estradiol", "145.8", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject43_Progesterone", new String[]{"Subject43", "2012-02-13", "Progesterone", "0.209", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject43_Estradiol", new String[]{"Subject43", "2012-02-13", "Estradiol", "155.1", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject44_Progesterone", new String[]{"Subject44", "2012-02-14", "Progesterone", "0.308", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject44_LH", new String[]{"Subject44", "2012-02-14", "LH", "3.66", "mIU/ml", "Unknown", " ", "1", " "});
        expected.put("Subject44_FSH", new String[]{"Subject44", "2012-02-14", "FSH", "7.06", "mIU/ml", "Unknown", " ", "1", " "});
        expected.put("Subject44_SHBG", new String[]{"Subject44", "2012-02-14", "SHBG", "2.75", "ug/ml", "Unknown", " ", "1", " "});
        expected.put("Subject44_Estradiol", new String[]{"Subject44", "2012-02-14", "Estradiol", "44.99", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject45_Progesterone", new String[]{"Subject45", "2012-02-15", "Progesterone", "0.28", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject45_LH", new String[]{"Subject45", "2012-02-15", "LH", "8.13", "mIU/ml", "Unknown", " ", "1", " "});
        expected.put("Subject45_Estradiol", new String[]{"Subject45", "2012-02-15", "Estradiol", "28.17", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject46_Progesterone", new String[]{"Subject46", "2012-02-16", "Progesterone", "0.351", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject46_LH", new String[]{"Subject46", "2012-02-16", "LH", "7.42", "mIU/ml", "Unknown", " ", "1", " "});
        expected.put("Subject46_Estradiol", new String[]{"Subject46", "2012-02-16", "Estradiol", "53.11", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject47_Progesterone", new String[]{"Subject47", "2012-02-17", "Progesterone", "0.261", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject47_LH", new String[]{"Subject47", "2012-02-17", "LH", "11.31", "mIU/ml", "Unknown", " ", "1", " "});
        expected.put("Subject47_Estradiol", new String[]{"Subject47", "2012-02-17", "Estradiol", "28.62", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject48_Progesterone", new String[]{"Subject48", "2012-02-18", "Progesterone", "0.314", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject48_LH", new String[]{"Subject48", "2012-02-18", "LH", "6.64", "mIU/ml", "Unknown", " ", "1", " "});
        expected.put("Subject48_Estradiol", new String[]{"Subject48", "2012-02-18", "Estradiol", "97.71", "pg/ml", "Unknown", " ", "1", " "});
        expected.put("Subject49_Progesterone", new String[]{"Subject49", "2012-02-19", "Progesterone", "0.287", "ng/ml", "Unknown", " ", "1", " "});
        expected.put("Subject49_LH", new String[]{"Subject49", "2012-02-19", "LH", "6.92", "mIU/ml", "Unknown", " ", "1", " "});
        expected.put("Subject49_Estradiol", new String[]{"Subject49", "2012-02-19", "Estradiol", "118.5", "pg/ml", "Unknown", " ", "1", " "});

        int totalRows = 100;
        Assert.assertEquals("Incorrect row count", totalRows, results.getDataRowCount());

        int i = 0;
        while (i < totalRows)
        {
            String subjectId = results.getDataAsText(i, "Subject Id");
            String date = results.getDataAsText(i, "Sample Date");
            String testName = results.getDataAsText(i, "Test Name");
            String result = results.getDataAsText(i, "Result");
            String units = results.getDataAsText(i, "Units");
            String diluent = results.getDataAsText(i, "Diluent");
            String df = results.getDataAsText(i, "Dilution Factor");
            String category = results.getDataAsText(i, "Sample Category");
            String qc = results.getDataAsText(i, "QC Flags");
            String key = subjectId + "_" + testName;

            String[] expectedVals = expected.get(key);
            Assert.assertNotNull("Unable to find expected values", expectedVals);

            Assert.assertEquals("Incorrect subjectId for: " + key, expectedVals[0], subjectId);
            if (!"".equals(expectedVals[1]))
                Assert.assertEquals("Incorrect date for: " + key, expectedVals[1], date);
            Assert.assertEquals("Incorrect result for: " + key, expectedVals[2], testName);
            if (!expectedVals[3].equals(result))
            {
                Double expectedResult = Double.parseDouble(expectedVals[3]);
                Double observedResult = Double.parseDouble(result);
                Assert.assertEquals("Incorrect result for: " + key, expectedResult, observedResult);
            }
            Assert.assertEquals("Incorrect units for: " + key, expectedVals[4], units);
            Assert.assertEquals("Incorrect category for: " + key, expectedVals[5], category);
            Assert.assertEquals("Incorrect diluent for: " + key, expectedVals[6], diluent);
            if (!expectedVals[7].equals(df))
            {
                Double expectedDf = Double.parseDouble(expectedVals[7]);
                Double observedDf = Double.parseDouble(df);
                Assert.assertEquals("Incorrect dilution factor for: " + key, expectedDf, observedDf);
            }

            Assert.assertEquals("Incorrect QC flag for: " + key, expectedVals[8], qc);

            i++;
        }
    }

    @Override
    protected void setUpTest() throws Exception
    {
        super.setUpTest();

    }

    @Override
    protected List<Pair<String, String>> getAssaysToCreate()
    {
        List<Pair<String, String>> assays = new ArrayList<Pair<String, String>>();
        assays.add(Pair.of("Electrochemiluminescence Assay", ASSAY_NAME));

        return assays;
    }

    @Override
    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<String>();
        modules.add("Electrochemiluminescence");
        return modules;
    }
}

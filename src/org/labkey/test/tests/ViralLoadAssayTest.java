package org.labkey.test.tests;

import org.apache.commons.lang3.tuple.Pair;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 11/6/12
 * Time: 5:43 PM
 */
public class ViralLoadAssayTest extends LabModulesTest
{
    private static final String ASSAY_NAME = "Viral Load";

    private static final String[][] TEMPLATE_DATA = new String[][]{
            {"Well", "Subject Id", "Sample Date", "Category", "Sample Volume (mL)"},
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
            {"F5", "Subject29", "2012-04-88", "Unknown", "0.3"},
            {"F6", "Subject29", "2012-04-08", "Unknown", "0.3"},
            {"F7", "Subject30", "2012-04-10", "Unknown", "0.3"},
            {"F8", "Subject30", "2012-04-10", "Unknown", "0.3"},
            {"F9", "Positive Control-1", "2012-04-12", "Unknown", "0.3"},
            {"F10", "Positive Control-1", "2012-04-12", "Unknown", "0.3"},
            {"F11", "Positive Control-2", "2012-04-14", "Unknown", "0.3"},
            {"F12", "Positive Control-2", "2012-04-14", "Unknown", "0.3"},
            {"A1", "NTC", "", "NTC", "0.3"},
            {"A2", "NTC", "", "NTC", "0.3"},
            {"G3", "STD_1000000", "", "STD", "0.3"},
            {"G4", "STD_1000000", "", "STD", "0.3"},
            {"G5", "STD_320000", "", "STD", "0.3"},
            {"G6", "STD_320000", "", "STD", "0.3"},
            {"G7", "STD_100000", "", "STD", "0.3"},
            {"G8", "STD_100000", "", "STD", "0.3"},
            {"G9", "STD_32000", "", "STD", "0.3"},
            {"G10", "STD_32000", "", "STD", "0.3"},
            {"G11", "STD_10000", "", "STD", "0.3"},
            {"G12", "STD_10000", "", "STD", "0.3"},
            {"H1", "STD_3200", "", "STD", "0.3"},
            {"H2", "STD_3200", "", "STD", "0.3"},
            {"H3", "STD_1000", "", "STD", "0.3"},
            {"H4", "STD_1000", "", "STD", "0.3"},
            {"H5", "STD_320", "", "STD", "0.3"},
            {"H6", "STD_320", "", "STD", "0.3"},
            {"H7", "STD_100", "", "STD", "0.3"},
            {"H8", "STD_100", "", "STD", "0.3"},
            {"H9", "STD_32", "", "STD", "0.3"},
            {"H10", "STD_32", "", "STD", "0.3"},
            {"H11", "STD_10", "", "STD", "0.3"},
            {"H12", "STD_10", "", "STD", "0.3"}
    };

    public ViralLoadAssayTest()
    {
        setContainerHelper(new UIContainerHelper(this));
        PROJECT_NAME = "VL_AssayVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
//        setUpTest();
        createRunTemplate();

    }

    private void createRunTemplate()
    {
        _helper.goToLabHome();
        _helper.clickNavPanelItem(ASSAY_NAME + ":", IMPORT_DATA_TEXT);
        _ext4Helper.clickExt4MenuItem("Prepare Run");
        waitForElement(Ext4Helper.ext4Window(IMPORT_DATA_TEXT));
        waitAndClick(Locator.ext4Button("Submit"));
        waitForPageToLoad();

        _helper.addRecordsToAssayTemplate(TEMPLATE_DATA);

        waitAndClick(Locator.ext4Button("Plate Layout"));
        waitForElement(Ext4Helper.ext4Window("Configure Plate"));
        waitForText("Group By Category");
        Ext4FieldRefWD.getForLabel(this, "Group By Category").setChecked(true);
        waitForText("Below are the sample categories");
        Ext4FieldRefWD ntcField = Ext4FieldRefWD.getForLabel(this, "NTC (2)");
        ntcField.setValue(8);
        waitAndClick(Locator.ext4Button("Submit"));
        assertAlert("Error: NTC conflicts with an existing sample in well: A9");

        ntcField.setValue(73); //corresponds to G1
        waitAndClick(Locator.ext4Button("Submit"));

        waitForElement(_helper.getAssayWell("G1", LabModuleHelper.NTC_COLOR));
        assertElementPresent(_helper.getAssayWell("G1", LabModuleHelper.NTC_COLOR));
        assertElementPresent(_helper.getAssayWell("B5", LabModuleHelper.UNKNOWN_COLOR));
        assertElementPresent(_helper.getAssayWell("G3", LabModuleHelper.STD_COLOR));
        assertElementPresent(_helper.getAssayWell("H12", LabModuleHelper.STD_COLOR));

        waitAndClick(Locator.ext4Button("Save"));
        _ext4Helper.waitForMaskToDisappear();
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
}

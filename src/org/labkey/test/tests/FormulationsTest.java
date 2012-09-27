/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.ListHelper.ListColumn;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by IntelliJ IDEA.
 * User: Nick
 * Date: Jan 21, 2011
 * Time: 11:36:22 AM
 */
public class FormulationsTest extends BaseSeleniumWebTest
{
    private static final String COMPOUNDS_NAME = "Compounds";
    private static final String FORMULATIONS_NAME = "Formulations";
    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.String;
    private final ListColumn LIST_COL_SORT = new ListColumn(
            "sort",
            "Sort Order",
            ListHelper.ListColumnType.Integer,
            "Used to sort ambigiously named timepoints based on day.");
    private static final String PROJECT_NAME = "FormulationsTest";
    private static final String RAWMATERIALS_SET_NAME = "Raw Materials";
    private static final String TEMPERATURE_LIST = "Temperatures";
    private static final String TIME_LIST = "Timepoints";
    private static final String TYPES_LIST = "FormulationTypes";
    private static final String MATERIAL_TYPES_LIST = "MaterialTypes";
        private final ListColumn MATERIAL_COL_TYPE = new ListColumn(
            "type",
            "Type",
            ListHelper.ListColumnType.String,
            "Type of Compound.");
    private final ListColumn MATERIAL_COL_UNITS = new ListColumn(
            "units",
            "Units",
            ListHelper.ListColumnType.String,
            "Measure of Units for given type.");

    private static final String COMPOUNDS_HEADER = "Compound Name\tFull Name\tCAS Number\tDensity\tMolecular Weight\n";
    private static final String COMPOUNDS_DATA_1 = "Alum\tAluminum Hydroxide\t21645-51-2\t\t78.0\n";  // adjuvant
    private static final String COMPOUNDS_DATA_2 = "Squawk\tBean Oil\t21235-51-3\t\t7.0\n";           // oil
    private static final String COMPOUNDS_DATA_3 = "Cholesterol\tCholesterol\t29935-53-9\t\t123.6\n"; // sterol
    private static final String COMPOUNDS_DATA_4 = "SPD\tSPD\t2313-23-1\t\t32.23\n";                  // buffer

    private static final String RAWMATERIALS_HEADER = "Identifier\tMaterial Name\tSupplier\tSource\tCatalogue ID\tLot ID\n";
    private static final String RAW_MATERIAL_1 = "IRM-0456";
    private static final String RAW_MATERIAL_2 = "IRM-0016";
    private static final String RAW_MATERIAL_3 = "IRM-0023";
    private static final String RAW_MATERIAL_4 = "IRM-0234";
    private static final String RAWMATERIALS_DATA_1 = RAW_MATERIAL_1 + "\tAlum\tAlum Supplier\tsynthetic\t\t99999\n";
    private static final String RAWMATERIALS_DATA_2 = RAW_MATERIAL_2 + "\tSquawk\tAlpha\tanimal\t\t123456\n";
    private static final String RAWMATERIALS_DATA_3 = RAW_MATERIAL_3 + "\tCholesterol\tFresh Supplies\tanimal\t\t314159265\n";
    private static final String RAWMATERIALS_DATA_4 = RAW_MATERIAL_4 + "\tSPD\tSPD Supplier\tsynthetic\t9123D-AS\t12331-CC\n";

    private static final String FORMULATION = "TD789";

    private static final String TEMPERATURE_HEADER = "Temperature\n";
    private static final String TEMPERATURE_DATA   = "5\n25\n37\n60\n";

    private static final String TIME_HEADER = "Time\tSort\n";
    private static final String TIME_DATA   = "T=0\t0\n1 wk\t7\n2 wk\t14\n1 mo\t30\n3 mo\t90\n6 mo\t180\n9 mo\t270\n12 mo\t360\n24 mo\t720\n36 mo\t1080\n";

    private static final String TYPES_HEADER = "Type\n";
    private static final String TYPES_DATA   = "Emulsion\nAqueous\nPowder\nLiposome\nAlum\nNiosomes\n";

    private static final String MTYPES_HEADER = "Type\tUnits\n";
    private static final String MTYPES_DATA   = "adjuvant\t%w/vol\nsterol\t%w/vol\noil\t%v/vol\nbuffer\tmM\n";

    private static final String PS_ASSAY      = "Particle Size";
    private static final String PS_ASSAY_DESC = "IDRI Particle Size Data as provided by Nano and APS machine configurations.";

    private static final String VIS_ASSAY      = "Visual";
    private static final String VIS_ASSAY_DESC = "IDRI Visual Data.";

    private static final String HPLC_ASSAY      = "HPLC";
    private static final String HPLC_PIPELINE_PATH = getSampledataPath() + "/HPLC";
    private static final String HPLC_ASSAY_DESC = "IDRI HPLC Assay Data";

    @Override
    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }
    
    @Override
    protected void doTestSteps() throws Exception
    {
        setupFormulationsProject();
        setupTimeTemperature();
        setupCompounds();
        setupRawMaterials();

        insertFormulation();
        defineParticleSizeAssay();
        uploadParticleSizeData();

        defineVisualAssay();
        uploadVisualAssayData();
        validateVisualAssayData();

        defineHPLCAssay();
        uploadHPLCAssayData();
    }


    protected void setupFormulationsProject()
    {
        _containerHelper.createProject(PROJECT_NAME, "IDRI Formulations");
        enableModule(PROJECT_NAME, "Dumbster");

        // Sample Sets should already exist
        assertLinkPresentWithText(COMPOUNDS_NAME);
        assertLinkPresentWithText(RAWMATERIALS_SET_NAME);
        assertLinkPresentWithText(FORMULATIONS_NAME);
    }

    protected void setupTimeTemperature()
    {
        clickLinkWithText(PROJECT_NAME);
        assertTextPresent("There are no user-defined lists in this folder");

        log("Add list -- " + TEMPERATURE_LIST);
        _listHelper.createList(PROJECT_NAME, TEMPERATURE_LIST, LIST_KEY_TYPE, "temperature");
        assertTextPresent(TEMPERATURE_LIST);

        log("Upload temperature data");
        _listHelper.clickImportData();
        _listHelper.submitTsvData(TEMPERATURE_HEADER + TEMPERATURE_DATA);

        clickLinkWithText("Lists");

        log("Add list -- " + TIME_LIST);
        _listHelper.createList(PROJECT_NAME, TIME_LIST, LIST_KEY_TYPE, "time", LIST_COL_SORT);
        _listHelper.clickImportData();
        _listHelper.submitTsvData(TIME_HEADER + TIME_DATA);

        clickLinkWithText("Lists");

        log("Add list -- " + TYPES_LIST);
        _listHelper.createList(PROJECT_NAME, TYPES_LIST, LIST_KEY_TYPE, "type");
        _listHelper.clickImportData();
        setFormElement(Locator.id("tsv3"), TYPES_HEADER + TYPES_DATA);
        clickButton("Submit", 0);
        _extHelper.waitForExtDialog("Success");
        assertTextPresent("6 rows inserted.");
        _extHelper.clickExtButton("Success", "OK");

        clickLinkWithText("Lists");

        log("Add list -- " + MATERIAL_TYPES_LIST);
        _listHelper.createList(PROJECT_NAME, MATERIAL_TYPES_LIST, ListHelper.ListColumnType.AutoInteger, "key", MATERIAL_COL_TYPE, MATERIAL_COL_UNITS);
        _listHelper.clickImportData();
        _listHelper.submitTsvData(MTYPES_HEADER + MTYPES_DATA);
    }

    protected void setupCompounds()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Entering compound information");
        clickLinkWithText(COMPOUNDS_NAME);

        // Add compound lookup
        clickLinkWithText("Edit Fields");
        waitAndClickButton("Add Field", 0);
        setFormElement(Locator.name("ff_name5"), "CompoundLookup");
        setFormElement(Locator.name("ff_label5"), "Type of Material");
        click(Locator.xpath("//input[@name='ff_type5']/../div[contains(@class, 'x-form-trigger-arrow')]"));
        _extHelper.waitForExtDialog("Choose Field Type", WAIT_FOR_JAVASCRIPT);

        ListHelper.LookupInfo lookup = new ListHelper.LookupInfo(PROJECT_NAME, "lists", "MaterialTypes");
        checkRadioButton(Locator.xpath("//label[text()='Lookup']/../input[@name = 'rangeURI']"));
        setFormElement(Locator.tagWithName("input", "lookupContainer"), lookup.getFolder());
        setFormElement(Locator.tagWithName("input", "schema"), lookup.getSchema());
        setFormElement(Locator.tagWithName("input", "table"), lookup.getTable());
        click(Locator.tagWithText("button", "Apply"));
        sleep(1000);
        clickButton("Save");

        clickButton("Import More Samples");
        clickRadioButtonById("insertOnlyChoice");
        setFormElement("data", COMPOUNDS_HEADER + COMPOUNDS_DATA_1 + COMPOUNDS_DATA_2 + COMPOUNDS_DATA_3 + COMPOUNDS_DATA_4);
        clickButton("Submit");

        this.setCompoundMaterial("adjuvant", 0);
        this.setCompoundMaterial("oil", 1);
        this.setCompoundMaterial("sterol", 2);
        this.setCompoundMaterial("buffer", 3);
    }

    private void setCompoundMaterial(String materialName, int rowIdx)
    {
        DataRegionTable table = new DataRegionTable("Material", this);

        table.clickLink(rowIdx,0);
        selectOptionByText(Locator.tagWithName("select", "quf_CompoundLookup"), materialName);
        clickButton("Submit");
    }

    protected void setupRawMaterials()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Enterting raw material information");
        clickLinkWithText(RAWMATERIALS_SET_NAME);
        clickButton("Import More Samples");
        clickRadioButtonById("insertOnlyChoice");
        setFormElement("data", RAWMATERIALS_HEADER + RAWMATERIALS_DATA_1 + RAWMATERIALS_DATA_2 + RAWMATERIALS_DATA_3 + RAWMATERIALS_DATA_4);
        clickButton("Submit");
    }

    protected void insertFormulation()
    {
        String addButton = "Add Another Material";

        clickLinkWithText(PROJECT_NAME);

        log("Inserting a Formulation");
        clickLinkWithText("Sample Sets");
        clickLinkWithText(FORMULATIONS_NAME, 1); // skip nav trail
        clickButton("Insert New");

        assertTextPresent("Formulation Type*");
        assertTextPresent("Stability Watch");
        assertTextPresent("Notebook Page*");

        // Describe Formulation
        setFormElement("batch", FORMULATION);
        setFormElement("type", "Alum");
        setFormElement("dm", "8/8/2008");
        setFormElement("batchsize", "100");
        setFormElement("comments", "This might fail.");
        setFormElement("nbpg", "549-87");

        clickButton(addButton, 0);
        _extHelper.selectComboBoxItem(this.getRawMaterialLocator(0), RAW_MATERIAL_1);
        waitForText("%w/vol", WAIT_FOR_JAVASCRIPT);
        setFormElement("concentration", "25.4");

        // Test Duplicate Material
        log("Test Duplicate Material");
        clickButton(addButton, 0);
        _extHelper.selectComboBoxItem(this.getRawMaterialLocator(1), RAW_MATERIAL_1);
        sleep(2000);
        setFormElements("input", "concentration", new String[]{"25.4", "66.2"});
        clickButton("Create", 0);
        waitForText("Duplicate source materials are not allowed.", WAIT_FOR_JAVASCRIPT);

        // Test empty combo
        log("Test empty combo");
        clickButton(addButton, 0);
        waitForExtMaskToDisappear();
        clickButton("Create", 0);
        waitForExtMaskToDisappear();
        waitForText("Invalid material", WAIT_FOR_JAVASCRIPT);
        
        // Test empty concentration
        log("Test empty concentration");
        _extHelper.selectComboBoxItem(this.getRawMaterialLocator(2), RAW_MATERIAL_2);
        waitForText("%v/vol", WAIT_FOR_JAVASCRIPT);
        clickButton("Create", 0);
        waitForText("Invalid material.", WAIT_FOR_JAVASCRIPT);

        // Remove duplicate material
        log("Remove duplicate material");
        click(Locator.xpath("//a[text() = 'Remove'][1]")); // remove

        // Add final material
        clickButton(addButton, 0);
        _extHelper.selectComboBoxItem(this.getRawMaterialLocator(3), RAW_MATERIAL_4);
        waitForText("mM", WAIT_FOR_JAVASCRIPT);
        
        // Create        
        setFormElements("input", "concentration", new String[]{"25.4", "66.2", "12.91"});
        clickButton("Create", 0);
        waitForText("has been created.", WAIT_FOR_JAVASCRIPT);
    }

    private Locator.XPathLocator getRawMaterialLocator(Integer index)
    {
        return Locator.xpath("//div[./input[@id='material" + index + "']]");
    }

    protected void defineParticleSizeAssay()
    {
        clickLinkWithText(PROJECT_NAME);
        
        log("Defining Particle Size Assay");
        clickLinkWithText("Manage Assays");
        clickButton("New Assay Design");

        assertTextPresent("Particle Size Data");
        checkRadioButton("providerName", "Particle Size");
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        selenium.type("//input[@id='AssayDesignerName']", PS_ASSAY);
        selenium.type("//textarea[@id='AssayDesignerDescription']", PS_ASSAY_DESC);

        // Batch Properties
        assertTextPresent("No fields have been defined.");
        
        // Run Properties
        assertTextPresent("IDRIBatchNumber");

        // Result Properties
        assertTextPresent("MeasuringTemperature");
        assertTextPresent("meanCountRate");
        assertTextPresent("AnalysisTool");

        clickButton("Save", 0);
        waitForText("Save successful.", 10000);
    }

    protected void uploadParticleSizeData()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Uploading Particle Size Data");
        clickLinkWithText(PS_ASSAY);
        clickButton("Import Data");

        assertTextPresent("Must have working sets of size");

        File dataRoot = new File(getLabKeyRoot(), "/sampledata/particleSize");
        File[] allFiles = dataRoot.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.matches("^TD789.xls");
            }
        });

        for (File file : allFiles)
        {
            log("uploading " + file.getName());
            setFormElement("upload-run-field-file", file);
            sleep(2500);
        }
    }

    protected void defineVisualAssay()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Defining Visual Assay");
        clickLinkWithText("Manage Assays");
        clickButton("New Assay Design");

        assertTextPresent("Visual Formulation Time-Point Data");
        checkRadioButton("providerName", "Visual");
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        selenium.type("//input[@id='AssayDesignerName']", VIS_ASSAY);
        selenium.type("//textarea[@id='AssayDesignerDescription']", VIS_ASSAY_DESC);

        // Batch Properties
        assertTextPresent("No fields have been defined.");

        // Run Properties
        assertTextPresent("LotNumber");

        // Result Properties
        assertTextPresent("PhaseSeparation");
        assertTextPresent("ColorChange");
        assertTextPresent("ForeignObject");

        clickButton("Save", 0);
        waitForText("Save successful.", 10000);
    }

    protected void uploadVisualAssayData()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Uploading Visual Data");
        clickLinkWithText(VIS_ASSAY);
        clickButton("Import Data");

        waitForText("What is the Lot Number?", WAIT_FOR_JAVASCRIPT);
        setFormElement("lot", FORMULATION);
        clickButton("Next", 0);

        waitForText("What temperatures are you examining?");
        checkRadioButton("time", "1 mo");
        clickButton("Next", 0);
        waitForText("Please complete this page to continue.");

        checkCheckbox("temp", "5C");
        checkCheckbox("temp", "60C");
        clickButton("Next", 0);

        waitForText("State of " + FORMULATION + " at 1 mo");
        checkRadioButton("5C", "fail");
        checkRadioButton("60C", "pass");
        clickButton("Next", 0);

        waitForText("Additional Comments for passing");
        setFormElement("comment60C", "This is a passing comment.");
        clickButton("Next", 0);

        waitForText("Failure Criteria for 5C");
        clickButton("Next", 0);
        waitForText("At least one criteria must be marked as a failure.");

        checkCheckbox("failed");    // color change
        checkCheckbox("failed", 2); // foreign object

        setFormElement("color", "Color changed.");
        setFormElement("foreign", TRICKY_CHARACTERS);
        clickButton("Next", 0);

        waitForText("Visual Inspection Summary Report");
        assertTextPresent("Color: Color changed.");
        assertTextBefore("5C", "60C");
        assertTextBefore("Failed", "Passed");
        clickButton("Submit", 0);

        waitForText("Updated successfully.");
        clickLinkWithText("More Visual Inspection");
        waitForText("Formulation Lot Information");
        waitAndClick(Locator.xpath("//div[@id='wizard-window']//div[contains(@class,'x-tool-close')]"));
    }

    protected void validateVisualAssayData()
    {
        // Assumes starting where uploadVisualAssayData left
        clickLinkWithText("Visual Batches");
        clickLinkWithText("view runs");
        clickLinkWithText(FORMULATION);

        assertTextPresent("Color changed.");
        assertTextPresent(TRICKY_CHARACTERS);
        assertTextPresent("This is a passing comment.");
    }

    protected void defineHPLCAssay()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Defining HPLC Assay");
        clickLinkWithText("Manage Assays");
        clickButton("New Assay Design");

        assertTextPresent("High performance liquid chromotography assay");
        checkRadioButton("providerName", "HPLC");
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        selenium.type("//input[@id='AssayDesignerName']", HPLC_ASSAY);
        selenium.type("//textarea[@id='AssayDesignerDescription']", HPLC_ASSAY_DESC);

        // Batch Properties
        assertTextPresent("No fields have been defined.");

        // Run Properties
        assertTextPresent("LotNumber");
        assertTextPresent("Method");

        // Result Properties
        assertTextPresent("Dilution");
        assertTextPresent("FilePath");
        assertTextPresent("Concentration");

        // Make Runs/Results editable
        checkCheckbox("editableRunProperties");
        checkCheckbox("editableResultProperties");

        clickButton("Save", 0);
        waitForText("Save successful.", 10000);
        clickButton("Save & Close");

        // Set pipeline path
        setPipelineRoot(HPLC_PIPELINE_PATH);
    }

    protected void uploadHPLCAssayData()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Uploading HPLC Data");
        clickLinkWithText(HPLC_ASSAY);
        assertTextPresent("No data to show.");

        clickButton("Import Data");
        _extHelper.selectFileBrowserItem("HPLCRun/");

        clickButton("Import HPLC", 0);
        waitForText("Selected Subfiles", 10000);

        // move files to appropriate locations for samples/standards/methods
        selenium.getEval("" +
            "var Ext4 = selenium.browserbot.getCurrentWindow().Ext4;" +
            "var g1 = Ext4.ComponentQuery.query('grid#selGrid')[0];" +
            "var g2 = Ext4.ComponentQuery.query('grid#smpGrid')[0];" +
            "var smpRec = g1.getStore().findRecord('name', '3004837A.CSV');" +
            "g2.getStore().add(smpRec); g1.getStore().remove(smpRec);" +
            "" +
            "g2 = Ext4.ComponentQuery.query('grid#stdGrid')[0];" +
            "var stdRec = g1.getStore().findRecord('name', 'STD2.txt');" +
            "g2.getStore().add(stdRec); g1.getStore().remove(stdRec);" +
            "" +
             "g2 = Ext4.ComponentQuery.query('grid#mthdGrid')[0];" +
            "var mthdRec = g1.getStore().findRecord('name', 'QDEMUL3.M');" +
            "g2.getStore().add(mthdRec); g1.getStore().remove(mthdRec);"
        );

        clickButton("Next", 0);

        // Fill out Sample Form
        waitForText("Preview not Available");
        _ext4Helper.selectComboBoxItem("Formulation", FORMULATION);
        setText("Diluent", "Starch");
        setText("Dilution", "123.45");
        _ext4Helper.selectComboBoxItem("Temperature", "5");
        _ext4Helper.selectComboBoxItem("Time", "T=0");

        clickButton("Next", 0);

//        _ext4Helper.selectComboBoxItem(this, "Compound", "Alum");
        setText("Concentration", "789.01");
        setFormElement(Locator.xpath("(//input[@name='Diluent'])[2]"), "Not Starch");

        clickButton("Next", 0);

        // Verify Review
        waitForText("Run Information");
        assertTextPresent("3004837A");

        clickButton("Save", 0);
        waitForText("Save Successful");
        clickButton("OK", 0);

        waitForText("Run Information", 10000);
        click(Locator.xpath("(//img[@class='x4-tool-close'])[1]"));
    }

    protected void performSearch()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Using Formulation search");
        setFormElement("nameContains", FORMULATION);
        clickButton("Search");

    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }
    
    @Override
    public String getAssociatedModuleDirectory()
    {
        return "none";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }
}

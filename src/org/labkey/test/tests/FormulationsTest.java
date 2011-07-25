/*
 * Copyright (c) 2011 LabKey Corporation
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
import org.labkey.test.util.ExtHelper;
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

    private static final String COMPOUNDS_HEADER = "Compound Name\tFull Name\tType of Material\tCAS Number\tDensity\tMolecular Weight\n";
    private static final String COMPOUNDS_DATA_1 = "Alum\tAluminum Hydroxide\tadjuvant\t21645-51-2\t\t78.0\n";
    private static final String COMPOUNDS_DATA_2 = "Squawk\tBean Oil\toil\t21235-51-3\t\t7.0\n";
    private static final String COMPOUNDS_DATA_3 = "Cholesterol\tCholesterol\tsterol\t29935-53-9\t\t123.6\n";
    private static final String RAWMATERIALS_HEADER = "Identifier\tMaterial Name\tSupplier\tSource\tCatalogue ID\tLot ID\n";
    private static final String RAW_MATERIAL_1 = "IRM-0456";
    private static final String RAW_MATERIAL_2 = "IRM-0016";
    private static final String RAW_MATERIAL_3 = "IRM-0023";
    private static final String FORMULATION = "TD789";
    private static final String RAWMATERIALS_DATA_1 = RAW_MATERIAL_1 + "\tAlum\tAlum Supplier\tsynthetic\t\t99999\n";
    private static final String RAWMATERIALS_DATA_2 = RAW_MATERIAL_2 + "\tSquawk\tAlpha\tanimal\t\t123456\n";
    private static final String RAWMATERIALS_DATA_3 = RAW_MATERIAL_3 + "\tCholesterol\tFresh Supplies\tanimal\t\t314159265\n";
    private static final String TEMPERATURE_HEADER = "Temperature\n";
    private static final String TEMPERATURE_DATA = "5\n25\n37\n60\n";
    private static final String TIME_HEADER = "Time\tSort\n";
    private static final String TIME_DATA = "T=0\t0\n1 wk\t7\n2 wk\t14\n1 mo\t30\n3 mo\t90\n6 mo\t180\n9 mo\t270\n12 mo\t360\n24 mo\t720\n36 mo\t1080\n";

    private static final String PS_ASSAY = "Particle Size";
    private static final String PS_ASSAY_DESC = "IDRI Particle Size Data as provided by Nano and APS machine configurations.";
    
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

        // Test Concentrations
        //
    }


    protected void setupFormulationsProject()
    {
        createProject(PROJECT_NAME, "IDRI Formulations Folder");
        enableModule(PROJECT_NAME, "Dumbster");

        //addWebPart("Sample Sets");
        //addWebPart("Lists");

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
        ListHelper.createList(this, PROJECT_NAME, TEMPERATURE_LIST, LIST_KEY_TYPE, "temperature");
        assertTextPresent(TEMPERATURE_LIST);

        log("Upload temperature data");
        ListHelper.clickImportData(this);
        ListHelper.submitTsvData(this, TEMPERATURE_HEADER + TEMPERATURE_DATA);

        clickLinkWithText("Lists");

        log("Add list -- " + TIME_LIST);
        ListHelper.createList(this, PROJECT_NAME, TIME_LIST, LIST_KEY_TYPE, "time", LIST_COL_SORT);
        ListHelper.clickImportData(this);
        ListHelper.submitTsvData(this, TIME_HEADER + TIME_DATA);
    }

    protected void setupCompounds()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Entering compound information");
        clickLinkWithText(COMPOUNDS_NAME);
        clickNavButton("Import More Samples");
        clickRadioButtonById("insertOnlyChoice");
        setFormElement("data", COMPOUNDS_HEADER + COMPOUNDS_DATA_1 + COMPOUNDS_DATA_2 + COMPOUNDS_DATA_3);
        clickNavButton("Submit");
    }

    protected void setupRawMaterials()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Enterting raw material information");
        clickLinkWithText(RAWMATERIALS_SET_NAME);
        clickNavButton("Import More Samples");
        clickRadioButtonById("insertOnlyChoice");
        setFormElement("data", RAWMATERIALS_HEADER + RAWMATERIALS_DATA_1 + RAWMATERIALS_DATA_2 + RAWMATERIALS_DATA_3);
        clickNavButton("Submit");
    }

    protected void insertFormulation()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Inserting a Formulation");
        clickLinkWithText("Sample Sets");
        clickLinkWithText(FORMULATIONS_NAME, 1); // skip nav trail
        clickNavButton("Insert New");

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

        clickButton("Add Another Material", 0);
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./input[@id='material0']]"), RAW_MATERIAL_1);
        waitForText("%w/vol", WAIT_FOR_JAVASCRIPT);
        setFormElement("concentration", "25.4");

        // Test Duplicate Material
        log("Test Duplicate Material");
        clickButton("Add Another Material", 0);
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./input[@id='material1']]"), RAW_MATERIAL_1);
        waitForText("%w/vol", WAIT_FOR_JAVASCRIPT);
        setFormElements("input", "concentration", new String[]{"25.4", "66.2"});
        clickButton("Create", 0);
        waitForText("Duplicate source materials are not allowed.", WAIT_FOR_JAVASCRIPT);

        // Test empty combo
        log("Test empty combo");
        clickButton("Add Another Material", 0);
        sleep(3000); // give form a chance to catch up
        clickButton("Create", 0);
        waitForText("Invalid material", WAIT_FOR_JAVASCRIPT);
        
        // Test empty concentration
        log("Test empty concentration");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./input[@id='material2']]"), RAW_MATERIAL_2);
        clickButton("Create", 0);
        waitForText("Invalid material", WAIT_FOR_JAVASCRIPT);

        // Remove duplicate material
        log("Remove duplicate material");
        click(Locator.xpath("//a[text() = 'Remove'][1]")); // remove
        
        // Create        
        setFormElements("input", "concentration", new String[]{"25.4", "66.2"});
        clickButton("Create", 0);
        waitForText("has been created.", WAIT_FOR_JAVASCRIPT);

        // TODO: Need to confirm it was created while still on this page.
    }

    protected void defineParticleSizeAssay()
    {
        clickLinkWithText(PROJECT_NAME);
        
        log("Defining Particle Size Assay");
        clickLinkWithText("Manage Assays");
        clickNavButton("New Assay Design");

        assertTextPresent("Particle Size Data");
        checkRadioButton("providerName", "Particle Size");
        clickNavButton("Next");

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

        clickNavButton("Save", 0);
        waitForText("Save successful.", 10000);
    }

    protected void uploadParticleSizeData()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Uploading Particle Size Data");
        clickLinkWithText(PS_ASSAY);
        clickNavButton("Import Data");

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

    protected void performSearch()
    {
        clickLinkWithText(PROJECT_NAME);

        log("Using Formulation search");
        setFormElement("nameContains", FORMULATION);
        clickNavButton("Search");

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

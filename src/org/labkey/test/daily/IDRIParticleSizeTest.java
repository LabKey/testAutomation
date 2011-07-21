/*
 * Copyright (c) 2009-2011 LabKey Corporation
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
package org.labkey.test.daily;

import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.bvt.AbstractAssayTest;

import java.io.*;

/**
 * User: jgarms
 * Date: May 19, 2009
 */
public class IDRIParticleSizeTest extends AbstractAssayTest
{
    private static final String PROJECT_NAME = "IDRI Module Validation";
    private static final String ASSAY_NAME = "IDRI";

    private final ListHelper.ListColumn _expCol1 = new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "name");
    private final ListHelper.ListColumn _formulationCol1 = new ListHelper.ListColumn("Formulation", "Formulation", ListHelper.ListColumnType.String, "formulation", new ListHelper.LookupInfo(null, "Samples", "Formulations"));
    private final ListHelper.ListColumn _formulationCol2 = new ListHelper.ListColumn("Experiment", "Experiment", ListHelper.ListColumnType.String, "expriment", new ListHelper.LookupInfo(null, "lists", "IDRI Experiments"));
    private final ListHelper.ListColumn _formulationCol3 = new ListHelper.ListColumn("Assignment", "Assignment", ListHelper.ListColumnType.Double, "assignment");

    private static final String WIKI_FORMULATIONS_PAGE =
            "<a href=\"%s/particleSize/" + PROJECT_NAME + "/populateFormulations.view\">Populate Formulations Table</a><p/>\n" +
            "<a href=\"%s/particleSize/" + PROJECT_NAME + "/formulations.view\">Formulations and IDRI Experiments</a><p/>";

    protected void doCleanup() throws Exception
    {
        deleteProject(PROJECT_NAME);
    }

    protected void runUITests() throws Exception
    {
        createProject(PROJECT_NAME);

        // module settings
        clickLinkWithText("Folder Settings");
        toggleCheckboxByTitle("Experiment");
        toggleCheckboxByTitle("ParticleSize");
        clickNavButton("Update Folder");

        setupPipeline(PROJECT_NAME);

        uploadSampleSet();
        defineParticleSizeAssay();
        uploadRuns();

        doFormulationsTest();
    }

    private void uploadSampleSet() throws Exception
    {
        log("Upload the sample set");

        clickLinkWithText(PROJECT_NAME);
        addWebPart("Sample Sets");

        clickLinkWithText("Sample Sets");
        clickNavButton("Import Sample Set");

        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        File file = new File(getLabKeyRoot(), "/sampledata/particleSize/formulations.txt");
        try {
            if (file.exists())
            {
                br = new BufferedReader(new FileReader(file));
                String l;
                while ((l = br.readLine()) != null)
                {
                    sb.append(l);
                    sb.append("\n");
                }
            }
        }
        finally
        {
            if (br != null)
                try {br.close();} catch(IOException ioe) {}
        }

        setFormElement("name", "Formulations");
        setFormElement("data", sb.toString());
        clickNavButton("Submit");
    }

    private void defineParticleSizeAssay()
    {
        log("Defining a Particle Size assay at the project level");

        clickLinkWithText(PROJECT_NAME);
        addWebPart("Assay List");

        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "IDRI Particle Size");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", ASSAY_NAME);

        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    private void uploadRuns()
    {
        log("uploading runs");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Assay List");
        clickLinkWithText(ASSAY_NAME);

        // Look for TDxxx.xls files
        File dataRoot = new File(getLabKeyRoot(), "/sampledata/particleSize");
        File[] allFiles = dataRoot.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.matches("^TD[0-9]+\\.xls");
            }
        });

        for(File file : allFiles)
        {
            clickNavButton("Import Data");
            log("uploading " + file.getName());
            setFormElement("upload-run-field-file", file);
            Boolean newFormulation = true;
            try{
                ExtHelper.waitForExtDialog(this, "New Formulation", 2500);
            }
            catch(Throwable t){
                newFormulation = false;
            }
            if (newFormulation)
            {
                // if we don't have any material, submit an empty entry
                click(getButtonLocator("Submit"));
            }
            waitForText(file.getName(), WAIT_FOR_JAVASCRIPT);
            clickNavButton("Done");
        }

        for (File file : allFiles)
        {
            assertTextPresent(file.getName());
        }

        log("Excel files uploaded");

        clickLinkWithText(PROJECT_NAME);
        clickNavButton("Manage Assays");
        clickLinkWithText(ASSAY_NAME);
        clickLinkWithText("TD220.xls");

        assertTextPresent("pass 10");
        assertTextPresent("Thu, Nov 30, 2006 at 03:55:39 PM");
        assertTextPresent("6.57E-4");
        assertTextPresent("dm+4 1");

        clickMenuButton("Views", "Z-Ave Graph");
    }

    private boolean isMaterialPopupVisible()
    {
        String divClass = selenium.getEval("this.browserbot.getCurrentWindow().document.getElementById('material').className");
        return !divClass.equals("x-hidden");
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/idriParticleSize";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    private void doFormulationsTest()
    {
        // create the IDRI experiment list and the formulations list
        ListHelper.createList(this, PROJECT_NAME, "IDRI Experiments", ListHelper.ListColumnType.AutoInteger, "Key", _expCol1);
        ListHelper.createList(this, PROJECT_NAME, "FormulationExpMap", ListHelper.ListColumnType.AutoInteger, "Key", _formulationCol1, _formulationCol2, _formulationCol3);

        // create a wiki page with links to the views included in the module
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Wiki TOC");
        clickWebpartMenuItem("Pages", "New");

        setFormElement("name", "formulations");
        setFormElement("title", "Formulations");

        String wikiText = String.format(WIKI_FORMULATIONS_PAGE, getContextPath(), getContextPath());
        setWikiBody(wikiText);
        saveWikiPage();

        // add this new page to a wiki web part
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Wiki");
        clickLinkWithText("Choose an existing page to display");
        selectOptionByValue("name", "formulations");
        clickNavButton("Submit");        
        
        clickLinkWithText("Populate Formulations Table");

        Locator.XPathLocator button = Locator.xpath("//input[@type='button' and contains(@value, 'populate tables')]");
        click(button);

        Locator.XPathLocator progress = Locator.xpath("//span[@id='progressText' and text() = 'Initial Population Completed']");
        waitForElement(progress, 90000);

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Formulations and IDRI Experiments");
    }
}

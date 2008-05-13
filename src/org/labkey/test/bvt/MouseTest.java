/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * User: brittp
 * Date: Nov 30, 2005
 * Time: 1:22:05 PM
 */
public class MouseTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "MouseVerifyProject";

    public String getAssociatedModuleDirectory()
    {
        return "mousemodels";
    }

    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        createProject(PROJECT_NAME);
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Mouse Models");
        clickLinkWithText("Mouse Models");

        clickNavButton("Insert New");

        //TODO: When we do real cleanup we don't need to randomize model name.
        String modelName = "Mdl" + (int) Math.floor(Math.random() * 1000);
        setFormElement("name", modelName);
        setFormElement("penetrance", "100%");
        setFormElement("latency", "6 weeks");
        setFormElement("investigator", "Test Investigator");
        setFormElement("tumorType", "Lung");
        setFormElement("location", "FHCRC");
        selectOptionByText("mouseStrainId", "129");
        selectOptionByText("targetGeneId", "abl");
        selectOptionByText("genotypeId", "-/-");
        selectOptionByText("treatmentId", "1Gy at birth");
        selectOptionByText("irradDoseId", "0.2Gy/24hr");
        clickNavButton("Submit");

        log("Add Breeding Pair");
        clickLinkWithText("Breeding Pairs");
        clickNavButton("Add Breeding Pair");

        log("Default Breeding Pair");
        setFormElement("pairName", "BP1");
        setFormElement("dateJoined", "1/1/05");
        clickNavButton("Submit");
        assertLinkPresentWithText("BP1");

        log("Creating Litters");
        clickNavButton("Add Litter");

        log("First Litter");
        setFormElement("litterName", "L1");
        setFormElement("birthDateString", "2/1/05");
        selectOptionByText("cages[0].sex", "M");
        checkCheckbox("cages[0].mice[0].control");
        setFormElement("cages[0].mice[0].toeNo", "1");
        setFormElement("cages[0].mice[1].toeNo", "2");
        setFormElement("cages[1].cageName", "B");
        selectOptionByText("cages[1].sex", "F");
        setFormElement("cages[1].mice[0].toeNo", "1");
        clickNavButton("Submit");

        log("Check entered mice");
        assertFalse("Page contains an error", hasError());
        clickLinkWithText("Mice");

        log("Sample Collection");
        clickLinkWithText("Sample Collection");

        clickLinkWithText("Urine Form");

        log("Add samples. ToDo: Clean up freezers");
        setFormElement("mouseNo", modelName + "-BP1L1-A1");
        setFormElement("locations[0].freezer", modelName + "Freezer1");
        setFormElement("locations[0].rack", "1");
        setFormElement("locations[0].box", "1");
        setFormElement("locations[0].cell", "1");
        clickNavButton("Submit");

        log("Look at 3 samples");
        assertFalse("Page contains an error", hasError());
        clickLinkWithText("Samples");

        log("More Collection");
        clickLinkWithText("Sample Collection");

        log("Add Necropsy Form");
        setFormElement("cageName", "B");
        clickNavButton("Go");

        log("Go to Necropsy Form");
        assertLinkPresentWithText("Necropsy");
        clickLinkWithText("Necropsy");

        log("Fill Necropsy Form");

        setFormElement("samples[0].sampleId", "802");
        selectOptionByText("samples[0].tissueId", "adrenal tumor");
        checkCheckbox("samples[0].frozen");
        setFormElement("samples[0].freezer", modelName + "Freezer1");
        setFormElement("samples[0].rack", "1");
        setFormElement("samples[0].box", "4");
        setFormElement("samples[0].cell", "1");

        setFormElement("samples[1].sampleId", "803");
        selectOptionByText("samples[1].tissueId", "bladder");
        checkCheckbox("samples[1].frozen");

        setFormElement("samples[1].freezer", modelName + "Freezer1");
        setFormElement("samples[1].rack", "1");
        setFormElement("samples[1].box", "4");
        setFormElement("samples[1].cell", "2");

        clickNavButton("Submit");

        log("Check Samples");
        assertFalse("Page contains an error", hasError());
        clickLinkWithText("Samples");
    }

    private boolean hasError()
    {
        return isElementPresent(Locator.tagWithAttribute("font", "class", "error"));
    }
}

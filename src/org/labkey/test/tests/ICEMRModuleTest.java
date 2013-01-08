/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 12/27/12
 * Time: 7:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ICEMRModuleTest extends BaseWebDriverTest
{
    public static final String ID = "myid";
    public static final String ASSAY_NAME = "Diagnostics Assay";
    public static final String SCIENTIST = "Torruk";

    @Override
    protected String getProjectName()
    {
        return "ICEMR assay test";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void doTestSteps() throws Exception
    {

        log("Create ICEMR with appropriate web parts");
        _containerHelper.createProject(getProjectName(), "Assay");
        enableModule("icemr", true);
        addWebPart("Add Diagnostics");

        createDiagnosticAssay();
        enterDataPoint();
        verifyDataInAssay();
    }

    private void verifyDataInAssay()
    {
        click(Locator.linkContainingText(ID));
        assertTextPresent(ID, SCIENTIST);
        goToProjectHome();
    }

    private void enterDataPoint()
    {
        Locator.XPathLocator link = Locator.linkContainingText("Upload data");
        waitForElement(link);
        sleep(500);
        click(link);
        waitForText("Data Import");

        enterData();

    }

    private void verifyError()
    {
        clickButton("Submit");
        waitForText("> Errors in your submission. See below.");
    }

    private void enterData()
    {
        Map<String, String> fieldAndValue = new HashMap<String, String>();
        fieldAndValue.put("scientist", SCIENTIST);
        fieldAndValue.put("id", ID);
        fieldAndValue.put("initparasitemia", (".3"));
        fieldAndValue.put("parasitedensity", "-34"); // invalid: can't have negative number
        fieldAndValue.put("initgametocytemia", "3.5");
        fieldAndValue.put("gametocytedensity", "3.4"); // invalid: can't have a float for an int
        fieldAndValue.put("patienthemoglobin", "300.4");
        fieldAndValue.put("hematocrit", "500"); // invalid: can't have percentage > 100
//        fieldAndValue.put("thinbloodsmear", "3.4");
        fieldAndValue.put("rdt", "3.4"); //this should be ignored
        fieldAndValue.put("freezerproid", "3.4");

        for(String field : fieldAndValue.keySet())
        {
            setICEMRField(field, fieldAndValue.get(field));
        }

        // we have 3 errors total, fix one at a time
        // Issue 16875: decimals in certain icemr module fields causes js exception
        // Issue 16876: need to screen invalid entries in ICEMR module
        verifyError();

        // correct negative number error
        setICEMRField("parasitedensity", "34");
        verifyError();

        // correct float in int column error
        setICEMRField("gametocytedensity", "34");
        verifyError();

        // correct > 100 percent error
        // the form should submit now
        setICEMRField("hematocrit", "5");
        clickButton("Submit");
        waitForText(ASSAY_NAME + " Runs");
    }

    private void createDiagnosticAssay()
    {
        _assayHelper.createAssayWithDefaults("ICEMR Diagnostics", ASSAY_NAME);
        waitForText(getProjectName());
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void setICEMRField(String field, String value)
    {

        Locator l =  Locator.xpath("//tr[@id='" + field + "-inputRow']/td/input");

        if(!isElementPresent(l))
        {
            l =  Locator.xpath("//td[@id='" + field + "-inputCell']/input");

        }
        setFormElement(l, value);

    }
}

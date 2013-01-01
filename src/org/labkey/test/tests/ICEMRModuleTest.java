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
    public static final String ASSAY_NAME = "Diagnostics";
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
        click(Locator.linkContainingText(ASSAY_NAME));
        click(Locator.linkContainingText("Untitled"));
        assertTextPresent(ID, SCIENTIST);
        goToProjectHome();
    }

    private void enterDataPoint()
    {
        Locator.XPathLocator link = Locator.linkContainingText("Go to my assay");
        waitForElement(link);
        sleep(500);
        clickAt(link, 1, 1);
        waitForText("Data Import");

        enterData();

    }

    private void enterData()
    {
        Map<String, String> fieldAndValue = new HashMap<String, String>();
        fieldAndValue.put("scientist", SCIENTIST);
        fieldAndValue.put("id", ID);
        fieldAndValue.put("initparasitemia", (".3"));
        fieldAndValue.put("parasitedensity", "-34");      //Issue 16875: decimals in certain icemr module fields causes js exception
        fieldAndValue.put("initgametocytemia", "3.5");
        fieldAndValue.put("gametocytedensity", "34");
        fieldAndValue.put("patienthemoglobin", "300.4");
        fieldAndValue.put("hematocrit", "5");
//        fieldAndValue.put("thinbloodsmear", "3.4");
        fieldAndValue.put("rdt", "3.4"); //this should be ignored
        fieldAndValue.put("freezerproid", "3.4");

        for(String field : fieldAndValue.keySet())
        {
            setICEMRField(field, fieldAndValue.get(field));
        }

        clickButton("Submit");
        _ext4Helper.waitForMask();//waitForExtMask(defaultWaitForPage);
        waitForText("Successfully uploaded result for patient id:  " + ID);
        _extHelper.clickExtButton("OK", 0);
        waitForText("Add Diagnostics");
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

package org.labkey.test.tests;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.ExtHelperWD;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: RyanS
 * Date: 5/31/13
 * Time: 11:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class LuminexExcludedTitrationTest extends LuminexTest
{
    protected static final Locator availableAnalytesCheckbox = Locator.xpath("//div[@class='x-grid3-hd-inner x-grid3-hd-checker']/div[@class='x-grid3-hd-checker']");
    protected static final Locator commentLocator = Locator.xpath("//input[@id='comment']") ;
    protected void ensureConfigured()
    {

        setUseXarImport(true);
        super.ensureConfigured();
    }

    @LogMethod @Override
    protected void runUITests()
    {
        runTitrationExclusionTest();
    }

    /**
     * test of titration exclusion- the ability to exclude certain titrations and add a comment as to why
     * preconditions: LUMINEX project and assay list exist.  Having the Multiple Curve data will speed up execution
     * but is not required
     * postconditions:  multiple curve data will be present, wells for the given titration will be marked excluded
     */
    @LogMethod
    protected void runTitrationExclusionTest()
    {
        ensureMultipleCurveDataPresent();

        clickAndWait(Locator.linkContainingText(MULTIPLE_CURVE_ASSAY_RUN_NAME));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("ExclusionComment");
        _customizeViewsHelper.applyCustomView();
        excludeTitration("Sample 1");
        excludeAnalyteWithinTitration("Sample 2", "ENV6");
    }

    protected void excludeTitration(String Titration)
    {
        clickButton("Exclude Titration","Analytes excluded for a replicate group will not be re-included by changes in assay level exclusions" );
        waitForElement(Locator.xpath("//td/div").withText(Titration));
        mouseDown(Locator.xpath("//td/div").withText(Titration));
        waitForElement(availableAnalytesCheckbox);
        mouseDown(availableAnalytesCheckbox);
        String exclusionMessage =  "excluding all analytes for titration " + Titration;
        setFormElement(commentLocator, exclusionMessage);
        sleep(5000);
        waitAndClickButton("Save", 0);
        _extHelper.waitForExtDialog("Save Changes?", WAIT_FOR_JAVASCRIPT);
        clickButtonContainingText("Yes");
        verifyTitrationExclusion(Titration, exclusionMessage);
    }

    protected void excludeAnalyteWithinTitration(String Titration, String Analyte)
    {
        clickButton("Exclude Titration","Analytes excluded for a replicate group will not be re-included by changes in assay level exclusions" );
        waitForElement(Locator.xpath("//td/div").withText(Titration));
        mouseDown(Locator.xpath("//td/div").withText(Titration));
        waitForElement(Locator.xpath("//td/div[@class='x-grid3-cell-inner x-grid3-col-1 x-unselectable']").containing(Analyte));
        mouseDown(Locator.xpath("//td/div[@class='x-grid3-cell-inner x-grid3-col-1 x-unselectable']").containing(Analyte));
        String exclusionMessage =  "excluding " + Analyte + " analyte for titration " + Titration;
        setFormElement(commentLocator, exclusionMessage);
        sleep(5000) ;
        waitAndClickButton("Save", 0);
        _extHelper.waitForExtDialog("Save Changes?", WAIT_FOR_JAVASCRIPT);
        clickButtonContainingText("Yes");
        verifyTitrationAnalyteExclusion(Titration, Analyte, exclusionMessage);
    }

    protected void verifyTitrationAnalyteExclusion(String excludedTitration, String excludedAnalyte, String exclusionMessage)
    {
        List<List<String>> vals = getColumnValues(DATA_TABLE_NAME, "Well", "Description", "Type", "Exclusion Comment", "Analyte");
        List<String> wells = vals.get(0);
        List<String> descriptions = vals.get(1);
        List<String> types = vals.get(2);
        List<String> comments = vals.get(3);
        List<String> analytesPresent = vals.get(4);

        String well;
        String description;
        String type;
        String comment;
        String analyte;

        for (int i=0; i<wells.size(); i++)
        {
            well = wells.get(i);
            log("well: " + well);
            description= descriptions.get(i);
            log("description: " + description);
            type = types.get(i);
            log("type: " + type);
            comment = comments.get(i);
            log("Comment: "+ comment);
            analyte= analytesPresent.get(i);
            analyte = analyte.substring(0, 4);
            log("Analyte: " + analyte);

            if (analyte.contains(excludedAnalyte) && description.equals(excludedTitration))
            {
                Assert.assertTrue(comment.contains(exclusionMessage));
            }

            if (comment.contains(exclusionMessage))
            {
                Assert.assertTrue(analyte.contains(excludedAnalyte) && description.equals(excludedTitration));
            }
        }
    }

    protected void verifyTitrationExclusion(String excludedTitration, String exclusionMessage)
    {
        List<List<String>> vals = getColumnValues(DATA_TABLE_NAME, "Well", "Description", "Type", "Exclusion Comment", "Analyte");
        List<String> wells = vals.get(0);
        List<String> descriptions = vals.get(1);
        List<String> types = vals.get(2);
        List<String> comments = vals.get(3);
        List<String> analytesPresent = vals.get(4);

        String well;
        String description;
        String type;
        String comment;
        String analyte;

        for(int i=0; i<wells.size(); i++)
        {
            well = wells.get(i);
            log("well: " + well);
            description= descriptions.get(i);
            log("description: " + description);
            type = types.get(i);
            log("type: " + type);
            comment = comments.get(i);
            log("Comment: "+ comment);
            analyte= analytesPresent.get(i);
            analyte = analyte.substring(0, 4);
            log("Analyte: " + analyte);

            if(description.equals(excludedTitration))
            {
                Assert.assertTrue(comment.contains(exclusionMessage));
            }
        }
    }

}

package org.labkey.test.tests;

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Specimen;
import org.labkey.test.util.LogMethod;

import java.io.File;

/**
 * User: Nick Arnold
 * Date: 7/25/13
 */
@Category({Specimen.class})
public class SpecimenExtendedTest extends SpecimenBaseTest
{
    protected static final String PROJECT_NAME = "SpecimenExtendedVerifyProject";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        initializeFolder();
        enableModule("nPOD", true);

        importStudyFromZip(new File(getSampledataPath(), "/study/LabkeyDemoStudy.zip"));

        setPipelineRoot(getPipelinePath());

        setupRequestabilityRules();
        startSpecimenImport(2);
        waitForSpecimenImport();
        setupRequestStatuses();
        setupActorsAndGroups();
    }

    @Override
    protected void doVerifySteps()
    {
        clickTab("Specimen Data");
        click(Locator.linkContainingText("Vial Search"));
        waitForElement(Locator.button("Search"));
        click(Locator.button("Search"));

        selectSpecimens("999320812", "999320396");

        _extHelper.clickMenuButton("Request Options", "Create New Request");
        selectOptionByText(Locator.name("destinationLocation"), DESTINATION_SITE);
        setFormElement(Locator.id("input0"), "Assay Plan");
        setFormElement(Locator.id("input1"), "Shipping");
        setFormElement(Locator.id("input2"), "Comments");
        clickButton("Save & Continue");

        waitForText("Please contact an Administrator to establish");
        clickButton("OK");
        waitForText("This request has not been submitted.");

        click(Locator.linkContainingText("Update Extended Request"));
        waitForText("Failed to load Extended Specimen Request.");
        clickButton("OK");

        click(Locator.linkContainingText("Specimen Requests"));
    }

    private void selectSpecimens(String... specimens)
    {
        for (String specimen : specimens)
        {
            checkCheckboxByNameInDataRegion(specimen);
        }
    }
}

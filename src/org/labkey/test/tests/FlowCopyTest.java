package org.labkey.test.tests;

import org.labkey.test.BaseFlowTest;
import org.labkey.test.Locator;

/**
 * User: kevink
 * Date: 6/6/12
 */
public class FlowCopyTest extends BaseFlowTest
{
    public static final String STUDY_FOLDER = "Ko";

    @Override
    protected void init()
    {
        super.init();

        createSubfolder(getProjectName(), getProjectName(), STUDY_FOLDER, "Study", null);
        clickNavButton("Create Study");
        clickNavButton("Create Study");
    }

    @Override
    protected void _doTestSteps() throws Exception
    {
        copyResultsToStudy();
    }

    private void copyResultsToStudy()
    {
        importAnalysis(getContainerPath(), "/flowjoquery/microFCS/microFCS.xml", "/flowjoquery/microFCS", false, "Copy Test", false, true);
        uploadSampleDescriptions("/sampledata/flow/flowjoquery/miniFCS/sample-set.tsv", new String[] { "File" }, new String[] { "Name" });
        setProtocolMetadata();

        // Copy the sample wells (Non-comp) to the STUDY_FOLDER
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses&query.FCSFile%2FKeyword%2FComp~in=Non-comp");
        clickCheckbox(".toggle");
        clickButton("Copy to Study");
        selectOptionByText("targetStudy", "/" + getProjectName() + "/" + STUDY_FOLDER + " (" + STUDY_FOLDER + " Study)");
        clickButton("Next");
        assertTitleContains("Copy to " + STUDY_FOLDER + " Study: Verify Results");
        setFormElement("participantId", "P4309");
        setFormElement("visitId", "9.1");
        clickNavButton("Copy to Study");

        assertTitleContains("Dataset: Flow");
        assertTrue("Expected go to STUDY_FOLDER container", getCurrentRelativeURL().contains("/" + STUDY_FOLDER));
        assertTextPresent("P4309", "9.1"); // ptid and visit entered in copy verify page
        assertTextPresent("P2301", "3.1"); // ptid and visit from sample-set.tsv
        String href = getAttribute(Locator.linkWithText("P2301"), "href");
        assertTrue("Expected PTID link to go to STUDY_FOLDER container: " + href, href.contains("/" + STUDY_FOLDER));
        href = getAttribute(Locator.linkWithText("microFCS.xml"), "href");
        assertTrue("Expected Run link to go to flow container: " + href, href.contains("/" + getFolderName()));
        href = getAttribute(Locator.linkWithText("AutoComp"), "href");
        assertTrue("Expected Compensation Matrix link to go to flow container: " + href, href.contains("/" + getFolderName()));

        // verify graph img is displayed (no error) and the src attribute goes to the flow container
        assertTextNotPresent("Error generating graph");
        href = getAttribute(Locator.xpath("//img[@title='(FSC-H:FSC-A)']"), "src");
        assertTrue("Expected graph img to go to flow container: " + href, href.contains("/" + getFolderName() + "/showGraph.view"));

        pushLocation();
        clickNavButton("View Source Assay");
        assertTitleContains("Flow Runs:");
        assertTrue("Expected source assay button to go to flow container", getCurrentRelativeURL().contains("/" + getFolderName()));
        popLocation();

        pushLocation();
        clickLinkWithText("assay");
        assertTitleContains("FCSAnalysis");
        assertTrue("Expected assay button to go to flow container", getCurrentRelativeURL().contains("/" + getFolderName()));
        popLocation();
    }
}

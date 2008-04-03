package org.labkey.test.daily;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * User: jeckels
 * Date: Dec 4, 2007
 */
public class UniprotAnnotationTest extends BaseSeleniumWebTest
{
    private static final String UNIPROT_FILENAME = "tinyuniprot.xml";
    private static final String PROJECT_NAME = "ProteinAnnotationVerifier";

    public String getAssociatedModuleDirectory()
    {
        return "ms2";
    }

    protected void doTestSteps() throws Exception
    {
        log("Starting UniprotAnnotationTest");

        ensureAdminMode();
        clickLinkWithText("Admin Console");
        clickLinkWithText("protein databases");
        assertTextNotPresent(UNIPROT_FILENAME);

        clickNavButton("Load New Annot File");
        setFormElement("fname", getLabKeyRoot() + "/sampledata/proteinAnnotations/" + UNIPROT_FILENAME);
        setFormElement("fileType", "uniprot");
        clickNavButton("Load Annotations");

        setFilter("AnnotInsertions", "FileName", "Contains", UNIPROT_FILENAME);
        setFilter("AnnotInsertions", "CompletionDate", "Is Not Blank");

        int seconds = 0;
        while (!isTextPresent(UNIPROT_FILENAME) && seconds++ < 60)
        {
            Thread.sleep(1000);
            refresh();
        }

        assertTextPresent(UNIPROT_FILENAME);

        createProject(PROJECT_NAME, "MS2");
        clickLinkWithText(PROJECT_NAME);
        setFormElement("identifier", "Ppia");
        uncheckCheckbox("restrictProteins");
        clickNavButton("Search");

        assertTextPresent("Peptidyl-prolyl cis-trans isomerase A");
        clickLinkWithText("Ppia");
        
        assertTextPresent("Q9CWJ5");
        assertTextPresent("Q9R137");
        assertTextPresent("PPIA_MOUSE");
        assertTextPresent("Mus musculus");

        clickLinkWithText(PROJECT_NAME);
        setFormElement("identifier", "Defa1");
        uncheckCheckbox("restrictProteins");
        clickNavButton("Search");

        assertTextPresent("Defensin-1 precursor");
        clickLinkWithText("Defa1");

        assertTextPresent("P11477");
        assertTextPresent("Q61448");
        assertTextPresent("DEF1_MOUSE");
        assertTextPresent("ENSMUSG00000074440");
        assertTextPresent("Mus musculus");
    }

    protected void doCleanup() throws Exception
    {
        deleteProject(PROJECT_NAME);
    }
}

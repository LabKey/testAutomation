package org.labkey.test.tests;

import org.labkey.test.Locator;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 2/27/13
 * Time: 10:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpecimenReplaceTest extends SpecimenMergeTest
{

    protected static final String LAB_EDITED_SPECIMENS = "/sampledata/study/specimens/lab19edit.specimens";
    protected static final String LAB15_SPECIMENS = "/sampledata/study/specimens/lab15.specimens";
    protected static final String LAB20_SPECIMENS = "/sampledata/study/specimens/lab20.specimens";
    protected static final String LAB21_SPECIMENS = "/sampledata/study/specimens/lab21.specimens";
    protected void doTestSteps()
    {
        setUpSteps();
        importFirstFileSet();

        verifyReplaceWithIdenticalFiles();
        verifyReplaceWithSlightlyModifiedData();
        verifyReplaceWithNewData();
    }

    private void verifyReplaceWithNewData()
    {

        SpecimenImporter importer = new SpecimenImporter(new File(_studyDataRoot), new File[] {new File(getLabKeyRoot(), LAB15_SPECIMENS)}, new File(getLabKeyRoot(), SPECIMEN_TEMP_DIR), FOLDER_NAME, ++pipelineJobCount);
        importer.setExpectError(true);
        importer.importAndWaitForComplete();
        //go to individual vial list
        goToIndividualvialsDRT();
        assertTextPresent("1 - 12 of 12");

        //entry for participant 999320812 have been replaced with 123123123
        assertTextPresent("999320812");
        assertTextNotPresent("123123123");
    }

    private void verifyReplaceWithSlightlyModifiedData()
    {

        SpecimenImporter importer = new SpecimenImporter(new File(_studyDataRoot), new File[] {new File(getLabKeyRoot(), LAB_EDITED_SPECIMENS)}, new File(getLabKeyRoot(), SPECIMEN_TEMP_DIR), FOLDER_NAME, ++pipelineJobCount);
        importer.setExpectError(true);
        importer.importAndWaitForComplete();
        //go to individual vial list
        goToIndividualvialsDRT();
        assertTextPresent("1 - 100 of 666");

        //entry for participant 999320812 have been replaced with 123123123
        assertTextNotPresent("999320812");
        assertTextPresent("123123123");

    }

    private void verifyReplaceWithIdenticalFiles()
    {
        pipelineJobCount  = 4;
        importFirstFileSet();
        goToIndividualvialsDRT();

        assertTextPresent("1 - 100 of 667");
    }

    private void goToIndividualvialsDRT()
    {
        clickTab("Specimen Data");
        clickAndWait(Locator.linkWithText("By Individual Vial"));
    }


}

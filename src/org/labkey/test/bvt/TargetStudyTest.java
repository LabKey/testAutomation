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

package org.labkey.test.bvt;

import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

import java.io.File;

/**
 * User: kevink
 * Date: Jan 6, 2011
 */
public class TargetStudyTest extends AbstractAssayTest
{
    private static final String ASSAY_NAME = "Assay";
    private static final String STUDY1_LABEL = "AwesomeStudy1";
    private static final String STUDY2_LABEL = "AwesomeStudy2";
    private static final String STUDY3_LABEL = "AwesomeStudy3";

    protected static final String TEST_RUN1 = "FirstRun";
    protected static final String TEST_RUN1_DATA1 =
            "specimenID\tparticipantID\tvisitID\tTargetStudy\n" +
            // study 1: full container path
            "AAA07XK5-05\t\t\t" + ("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1) + "\n" +
            // study 1: container id
            "AAA07XMC-02\t\t\t${Study1ContainerID}\n" +
            // study 1: study label
            "AAA07XMC-04\t\t\t" + STUDY1_LABEL + "\n" +
            // fake study / no study
            "AAA07XSF-02\t\t\tStudyNotExist\n" +
            // study 2
            "AAA07YGN-01\t\t\t" +("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY2) + "\n" +
            // study 3
            "AAA07YGN-02\t\t\t" +("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY3) + "\n"
            ;

    private String _study1ContainerId = null;

    @Override
    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(TEST_ASSAY_PRJ_SECURITY);
        }
        catch (Throwable t) { }
    }

    @Override
    protected void runUITests() throws Exception
    {
        log("** Setup");
        setupEnvironment();
        setupSpecimens();
        setupLabels();
        setupAssay();
        
        _study1ContainerId = getContainerId("/project/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1 + "/begin.view");
        log("** Study 1 container ID = " + _study1ContainerId);
        assertNotNull(_study1ContainerId);

        uploadRuns();

        // UNDONE: copy-to-study with multiple targets
    }


    protected void setupSpecimens()
    {
        log("** Import specimens into Study 1 and Study 2");
        setupPipeline(TEST_ASSAY_PRJ_SECURITY);
        SpecimenImporter importer1 = new SpecimenImporter(getTestTempDir(), new File(getLabKeyRoot(), "/sampledata/study/specimens/sample_a.specimens"), new File(getTestTempDir(), "specimensSubDir"), TEST_ASSAY_FLDR_STUDY1, 1);
        importer1.startImport();

        SpecimenImporter importer2 = new SpecimenImporter(getTestTempDir(), new File(getLabKeyRoot(), "/sampledata/study/specimens/sample_a.specimens"), new File(getTestTempDir(), "specimensSubDir"), TEST_ASSAY_FLDR_STUDY2, 1);
        importer2.startImport();

        importer1.waitForComplete();
        importer2.waitForComplete();

    }

    protected void setupLabels()
    {
        log("** Set some awesome study labels");
        beginAt("/study/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1 + "/manageStudyProperties.view");
        setFormElement("label", STUDY1_LABEL);
        clickNavButton("Update");

        beginAt("/study/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY2 + "/manageStudyProperties.view");
        setFormElement("label", STUDY2_LABEL);
        clickNavButton("Update");

        beginAt("/study/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY3 + "/manageStudyProperties.view");
        setFormElement("label", STUDY3_LABEL);
        clickNavButton("Update");
    }
    protected void setupAssay()
    {
        log("** Define GPAT Assay");
        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);
        if (!isLinkPresentWithText("Assay List"))
            addWebPart("Assay List");
        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickNavButton("Next");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", ASSAY_NAME);
        sleep(1000);

        // Remove ParticipantVisitResolver and TargetStudy from the Batch domain
        deleteField("Batch Fields", 0);
        deleteField("Batch Fields", 0);

        // Add TargetStudy to the end of the default list of Results domain
        addField("Data Fields", 4, "TargetStudy", "Target Study", ListHelper.ListColumnType.String);

        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

    }

    protected void uploadRuns()
    {
        log("** Upload Data");
        clickLinkWithText(TEST_ASSAY_PRJ_SECURITY);

        clickLinkWithText("Assay List");
        clickLinkWithText(ASSAY_NAME);

        clickNavButton("Import Data");

        selenium.type("name", TEST_RUN1);
        selenium.click("//input[@value='textAreaDataProvider']");
        String data1 = TEST_RUN1_DATA1.replace("${Study1ContainerID}", _study1ContainerId);
        selenium.type("TextAreaDataCollector.textArea", data1);
        clickNavButton("Save and Finish");
        assertTextPresent("Couldn't resolve TargetStudy 'StudyNotExist' to a study folder.");

        selenium.click("//input[@value='textAreaDataProvider']");
        String data2 = data1.replace("StudyNotExist", "");
        selenium.type("TextAreaDataCollector.textArea", data2);
        clickNavButton("Save and Finish");
        assertNoLabkeyErrors();

        log("** Test the TargetStudy renderer resolved all studies");
        clickLinkWithText(TEST_RUN1);
        // all target study values should render as either [None] or the name of the study
        assertTextNotPresent("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY1);
        assertTextNotPresent("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY2);
        assertTextNotPresent("/" + TEST_ASSAY_PRJ_SECURITY + "/" + TEST_ASSAY_FLDR_STUDIES + "/" + TEST_ASSAY_FLDR_STUDY3);

        DataRegionTable table = new DataRegionTable(ASSAY_NAME + " Data", this);
        assertEquals(STUDY1_LABEL, table.getDataAsText(0, "Target Study"));
        assertEquals(STUDY1_LABEL, table.getDataAsText(1, "Target Study"));
        assertEquals(STUDY1_LABEL, table.getDataAsText(2, "Target Study"));
        //BUGBUG: target study renders as "" instead of "[None]"
        //assertEquals("[None]", table.getDataAsText(3, "Target Study"));
        assertEquals("", table.getDataAsText(3, "Target Study"));
        assertEquals(STUDY2_LABEL, table.getDataAsText(4, "Target Study"));
        assertEquals(STUDY3_LABEL, table.getDataAsText(5, "Target Study"));

        log("** Check SpecimenID resolved the PTID in the study");
        assertEquals("999320812", table.getDataAsText(0, "Participant ID"));
        assertEquals("999320396", table.getDataAsText(1, "Participant ID"));
        assertEquals("999320396", table.getDataAsText(2, "Participant ID"));
        assertEquals("", table.getDataAsText(3, "Participant ID"));
        assertEquals("999320706", table.getDataAsText(4, "Participant ID"));
        assertEquals("", table.getDataAsText(5, "Participant ID"));
    }

}

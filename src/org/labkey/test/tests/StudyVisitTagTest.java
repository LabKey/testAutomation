/*
 * Copyright (c) 2014-2015 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Study;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyB.class, Study.class})
public class StudyVisitTagTest extends StudyBaseTest
{
    protected final String VISIT_TAG_QWP_TITLE = "VisitTag";
    protected final String VISIT_TAG_MAP_QWP_TITLE = "VisitTagMap";
    protected final String PARENT_FOLDER_STUDY = "VisitTagsStarter";
    protected final String STUDY_TEMPLATE = "StudyAxistTestTemplate";
    protected final String DATE_FOLDER_STUDY1 = "StudyAxisTest1";
    protected final String DATE_FOLDER_STUDY2 = "StudyAxisTest2";
    protected final String DATE_FOLDER_STUDY3 = "StudyAxisTest3";
    protected final String DATE_FOLDER_STUDY4 = "StudyAxisTest4";
    protected final String DATE_FOLDER_STUDY5 = "StudyAxisTest5";
    protected final String DATE_FOLDER_STUDY6 = "StudyAxisTest6";
    protected final String DATE_FOLDER_STUDY7 = "StudyAxisTest11";
    protected final String DATE_FOLDER_STUDY8 = "StudyAxisTest12";
    protected final String VISIT_FOLDER_STUDY1 = "StudyAxisTestA";
    protected final String VISIT_FOLDER_STUDY2 = "StudyAxisTestB";
    protected final String VISIT_FOLDER_STUDY3 = "StudyAxisTestC";
    protected final String VISIT_FOLDER_STUDY4 = "StudyAxisTestD";
    protected final String VISIT_FOLDER_STUDY5 = "StudyAxisTestE";
    protected final String VISIT_FOLDER_STUDY6 = "StudyAxisTestF";
    protected final String VISIT_FOLDER_STUDY7 = "StudyAxisTestG";
    protected final String VISIT_FOLDER_STUDY8 = "StudyAxisTestH";
    protected final String[] DATE_BASED_STUDIES = {DATE_FOLDER_STUDY1}; //, DATE_FOLDER_STUDY2, DATE_FOLDER_STUDY3, DATE_FOLDER_STUDY4, DATE_FOLDER_STUDY7, DATE_FOLDER_STUDY8};
    protected final String[] SINGLE_USE_TAG_ERRORS = {DATE_FOLDER_STUDY5}; //, DATE_FOLDER_STUDY6};
    protected final String[] VISIT_BASED_STUDIES = {VISIT_FOLDER_STUDY1}; //, VISIT_FOLDER_STUDY2, VISIT_FOLDER_STUDY3, VISIT_FOLDER_STUDY4, VISIT_FOLDER_STUDY5, VISIT_FOLDER_STUDY6, VISIT_FOLDER_STUDY7, VISIT_FOLDER_STUDY8};
    protected final String WIKIPAGE_NAME = "VisitTagGetDataAPITest";
    protected final String TEST_DATA_API_PATH = "server/test/data/api";
    //TODO: placeholder, need to create a new test page with appropriate js to test getData api in this context
    protected final String TEST_DATA_API_CONTENT = "/getDataVisitTest.html";
    //private Map<String, String> _visitTagMaps = new HashMap<>();
    private final PortalHelper _portalHelper = new PortalHelper(this);
    private final PortalHelper portalHelper = new PortalHelper(this);
    private final WikiHelper wikiHelper = new WikiHelper(this);

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doCreateSteps()
    {
        doCleanup(false);
        initializeFolder();
        setPipelineRoot(getStudySampleDataPath() + "VisitTags");
        importStudies();
    }

    @Override
    protected void initializeFolder()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        for(String Study : DATE_BASED_STUDIES)
        {
            createSubfolder(getProjectName(), getProjectName(), Study, "Study", null, true);
        }
        for(String Study : VISIT_BASED_STUDIES)
        {
            createSubfolder(getProjectName(), getProjectName(), Study, "Study", null, true);
        }
        for(String Study : SINGLE_USE_TAG_ERRORS)
        {
            createSubfolder(getProjectName(), getProjectName(), Study, "Study", null, true);
        }
    }

    protected void importStudies()
    {
        String visitTagsPath = TestFileUtils.getSampledataPath() + "/study/VisitTags";
        goToProjectHome();
        startImportStudyFromZip(new File(visitTagsPath, PARENT_FOLDER_STUDY + ".folder.zip"), false, false);
        goToProjectHome();
        addVisitTagAndTagMapQWP();
        for(String Study : DATE_BASED_STUDIES)
        {
            clickFolder(Study);
            startImportStudyFromZip(new File(visitTagsPath, Study + ".folder.zip"), false, false);
            waitForPipelineJobsToComplete(1, "Study import", false);
            clickFolder(Study);
            addVisitTagAndTagMapQWP();
//            setupAPITestWiki();
        }
        for(String Study : VISIT_BASED_STUDIES)
        {
            clickFolder(Study);
            startImportStudyFromZip(new File(visitTagsPath, Study + ".folder.zip"), false, false);
            waitForPipelineJobsToComplete(1, "Study import", false);
            clickFolder(Study);
            addVisitTagAndTagMapQWP();
//            setupAPITestWiki();
        }
        for(String Study : SINGLE_USE_TAG_ERRORS)
        {
            clickFolder(Study);
            startImportStudyFromZip(new File(visitTagsPath, Study + ".folder.zip"), false, false);
            waitForPipelineJobsToComplete(1, "Study import", true);
            checkExpectedErrors(1);
            clickFolder(Study);
            addVisitTagAndTagMapQWP();
//            setupAPITestWiki();
        }

    }

    @Override
    protected void doVerifySteps() throws Exception
    {
        final List<String> VISIT_TAG_NAMES = Arrays.asList("day0", "finalvaccination", "finalvisit", "firstvaccination", "notsingleuse", "peakimmunogenicity");
        final List<String> VISIT_TAG_CAPTIONS = Arrays.asList("Day 0 (meaning varies)", "Final Vaccination", "Final visit", "First Vaccination", "Not Single Use Tag", "Predicted peak immunogenicity visit");
        final List<String> VISIT_TAG_MAP_TAGS = Arrays.asList("Day 0 (meaning varies)", "First Vaccination", "First Vaccination", "Final Vaccination", "Final Vaccination", "Final visit");
        final List<String> VISIT_TAG_MAP_VISITS = Arrays.asList("Visit1", "Visit2", "Visit3", "Visit3", "Visit4", "Visit5");
        final List<String> VISIT_TAG_MAP_COHORTS = Arrays.asList(" ", "Positive", "Negative", "Negative", "Positive", " ");

        goToProjectHome();
        DataRegionTable visitTags = getVisitTagTable();
        DataRegionTable visitTagMaps = getVisitTagMapTable();
        List<String> tagNames = visitTags.getColumnDataAsText("Name");
        List<String> tagCaptions = visitTags.getColumnDataAsText("Caption");
        assertEquals("Wrong Tag Names", VISIT_TAG_NAMES, tagNames);
        assertEquals("Wrong Tag Names", VISIT_TAG_CAPTIONS, tagCaptions);

        //TODO: need to add a method to return an entire row from DataTable as delimited strings so we can do this more easily
        List<String> tagMapNames = visitTagMaps.getColumnDataAsText("Visit Tag");
        List<String> tagMapVisits = visitTagMaps.getColumnDataAsText("Visit");
        List<String> tagMapCohort = visitTagMaps.getColumnDataAsText("Cohort");

        assertEquals("Wrong Tag Map Names", VISIT_TAG_MAP_TAGS, tagMapNames);
        assertEquals("Wrong Tag Map Visits", VISIT_TAG_MAP_VISITS, tagMapVisits);
        assertEquals("Wrong Tag Map Cohorts", VISIT_TAG_MAP_COHORTS, tagMapCohort);

        verifyInsertEditVisitTags();
    }

    protected String getProjectName()
    {
        return "VisitTagStudyVerifyProject";
    }

    protected void addVisitTagAndTagMapQWP()
    {
        _portalHelper.addQueryWebPart(VISIT_TAG_QWP_TITLE, "study", "VisitTag", null);
        _portalHelper.addQueryWebPart(VISIT_TAG_MAP_QWP_TITLE, "study", "VisitTagMap", null);
    }

    protected void setupAPITestWiki()
    {
        portalHelper.addWebPart("Wiki");
        wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), WIKIPAGE_NAME);
        setFormElement(Locator.name("title"), WIKIPAGE_NAME);
        wikiHelper.setWikiBody(TestFileUtils.getFileContents(TEST_DATA_API_PATH + "/getDataVisitTest.html"));
        wikiHelper.saveWikiPage();
        waitForText("Current Config", WAIT_FOR_JAVASCRIPT);
    }

    protected DataRegionTable getVisitTagTable()
    {
        return new DataRegionTable(DataRegionTable.getTableNameByTitle("VisitTag", this), this);
    }

    protected DataRegionTable getVisitTagMapTable()
    {
        return new DataRegionTable(DataRegionTable.getTableNameByTitle("VisitTagMap", this), this);
    }

    protected void verifyInsertEditVisitTags()
    {
        goToProjectHome();
        insertVisitTag(VISIT_TAG_QWP_TITLE, new VisitTag("FollowUp1", "Follow Up 1", "", false));
        insertVisitTagMap(VISIT_TAG_MAP_QWP_TITLE, new VisitTagMap("FollowUp1", "Visit5", null));

        insertVisitTagMap(VISIT_TAG_MAP_QWP_TITLE, new VisitTagMap("FollowUp1", "Visit5", null));
        assertTextPresent("VisitTagMap may contain only one row for each (VisitTag, Visit, Cohort) combination.");
        clickButton("Cancel");
    }
}

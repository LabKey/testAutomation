/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import junit.framework.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ListPublishTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ListPublishTestProject";
    private static Set<String> _Ptids;
    private static String[] listIds = new String[4];

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        initializePtids("249318596", "249320107", "249320127", "249320489", "249320897", "249325717");
        _containerHelper.createProject(PROJECT_NAME, "Study");

        importStudyFromZip(new File(getSampledataPath(), "/study/LabkeyDemoStudy.zip"));
        goToProjectHome();
        setUpLists();
        setListIds();
        publishStudy();
        refresh();

        waitForElement(Locator.xpath("//a[text()='PublishedLists']"));

        checkListIds();
        assertPtidsNotPresent();

    }

    private void initializePtids(String... ptids)
    {
        _Ptids =  new HashSet<String>();
        for(String ptid : ptids)
        {
            _Ptids.add(ptid);
        }
    }

    public void insertPtids()
    {
        for(String ptid : _Ptids)
        {
            _listHelper.insertNewRow(Maps.<String, String>of(
                    "ParticipantId", ptid
            ));
        }
    }

    private void assertPtidsNotPresent()
    {
        for(String ptid : _Ptids)
        {
            assertTextNotPresent(ptid);
        }
    }

    private void setUpLists()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");
        ListHelper.ListColumn column = new ListHelper.ListColumn("ParticipantId", "ParticipantId", ListHelper.ListColumnType.String, "A list of Ptids");
        _listHelper.createList("ListPublishTestProject", "List4", ListHelper.ListColumnType.AutoInteger, "Key", column);
        goToProjectHome();
        goToList("List4");
        insertPtids();
    }

    private void publishStudy()
    {
        //publish the study
        goToManageStudy();
        clickButton("Publish Study", 0);
        _extHelper.waitForExtDialog("Publish Study");

        // Wizard page 1 : General Setup
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'General Setup']"));
        setFormElement(Locator.name("studyName"), "PublishedLists");
        clickButton("Next", 0);

        // Wizard page 2 : Participants
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Participants']"));
        clickButton("Next", 0);

        // Wizard page 3 : Datasets
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Datasets']"));
        click(Locator.css(".studyWizardDatasetList .x-grid3-hd-checker  div"));
        clickButton("Next", 0);

        // Wizard page 4 : Visits
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Timepoints']"));
        click(Locator.css(".studyWizardVisitList .x-grid3-hd-checker  div"));
        clickButton("Next", 0);

        // Wizard page 5 : Specimens
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Specimens']"));
        clickButton("Next", 0);

        // Wizard Page 6 : Study Objects
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Study Objects']"));
        click(Locator.css(".studyObjects .x-grid3-hd-checker  div"));
        clickButton("Next", 0);

        // Wizard page 7 : Lists
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Lists']"));
        click(Locator.css(".studyWizardListList .x-grid3-hd-checker  div"));
        clickButton("Next", 0);

        // Wizard page 8 : Views
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Views']"));
        clickButton("Next", 0);

        // Wizard Page 9 : Reports
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Reports']"));
        clickButton("Next", 0);

        // Wizard page 10 : Folder Objects
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Folder Objects']"));
        click(Locator.css(".folderObjects .x-grid3-hd-checker  div"));
        clickButton("Next", 0);

        // Wizard page 11 : Publish Options
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Publish Options']"));
        clickButton("Finish");
        waitForPipelineJobsToComplete(2, "Publish Study", false);
    }


    public void goToList(String list)
    {
        click(Locator.xpath("//a[text()='manage lists']"));
        click(Locator.xpath("//a[text()='" + list + "']"));
    }

    private void setListIds()
    {
        goToProjectHome();
        int id;
        goToList("Lab Machines");
        id = Integer.parseInt(getUrlParam(getURL().toString(), "listId", false))-1;
        listIds[id] = "Lab Machines";

        goToProjectHome();
        goToList("Reagents");
        id = Integer.parseInt(getUrlParam(getURL().toString(), "listId", false))-1;
        listIds[id] = "Reagents";

        goToProjectHome();
        goToList("Technicians");
        id = Integer.parseInt(getUrlParam(getURL().toString(), "listId", false))-1;
        listIds[id] = "Technicians";

        listIds[3] = "List4";
    }

    private void checkListIds()
    {
        for(int i = 0; i < 4; i++)
        {
            click(Locator.xpath("//a[text()='PublishedLists']"));
            goToList(listIds[i]);
            Assert.assertEquals(getUrlParam(getURL().toString(), "listId", false), ""+(i+1));
        }
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }
}

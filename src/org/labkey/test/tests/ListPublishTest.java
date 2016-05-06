/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@Category(InDevelopment.class)
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

    @Test
    public void testSteps()
    {
        initializePtids("249318596", "249320107", "249320127", "249320489", "249320897", "249325717");
        _containerHelper.createProject(PROJECT_NAME, "Study");

        importStudyFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"));
        goToProjectHome();
        setUpLists();
        setListIds();
        _studyHelper.publishStudy("PublishedLists", 2, "Participant", "Participants", "Timepoints", null);
        checkListIds();
        assertPtidsNotPresent();

    }

    private void initializePtids(String... ptids)
    {
        _Ptids =  new HashSet<>();
        for(String ptid : ptids)
        {
            _Ptids.add(ptid);
        }
    }

    public void insertPtids()
    {
        for(String ptid : _Ptids)
        {
            _listHelper.insertNewRow(Maps.of(
                    "ParticipantId", ptid
            ));
        }
    }

    private void assertPtidsNotPresent()
    {
        int i = 1;
        for(String ptid : _Ptids)
        {
            WebElement text = getDriver().findElement(By.xpath("//a[@href = '/labkey/list/ListPublishTestProject/PublishedLists/details.view?listId=4&pk="+i+"']"));
            assertTextNotPresent(ptid);
            assertTrue(!text.getText().equals(""));
            i++;
        }
    }

    private void setUpLists()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");
        ListHelper.ListColumn column = new ListHelper.ListColumn("ParticipantId", "ParticipantId", ListHelper.ListColumnType.Subject, "A list of Ptids");
        _listHelper.createList("ListPublishTestProject", "List4", ListHelper.ListColumnType.AutoInteger, "Key", column);
        goToProjectHome();
        goToList("List4");
        insertPtids();
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
        id = Integer.parseInt(getUrlParam("listId"))-1;
        listIds[id] = "Lab Machines";

        goToProjectHome();
        goToList("Reagents");
        id = Integer.parseInt(getUrlParam("listId"))-1;
        listIds[id] = "Reagents";

        goToProjectHome();
        goToList("Technicians");
        id = Integer.parseInt(getUrlParam("listId"))-1;
        listIds[id] = "Technicians";

        listIds[3] = "List4";
    }

    private void checkListIds()
    {
        for(int i = 0; i < 4; i++)
        {
            clickFolder("PublishedLists");
            goToList(listIds[i]);
            assertEquals(getUrlParam("listId"), ""+(i+1));
        }
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}

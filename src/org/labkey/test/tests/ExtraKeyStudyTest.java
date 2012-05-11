/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 4/29/12
 * Time: 1:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtraKeyStudyTest extends StudyBaseTest
{

    static String studyFolder = "/ExtraKeyStudy/folder.xml";

    @Override
    protected void doCreateSteps()
    {
        initializeFolder();
        initializePipeline();

        importFolderFromPipeline(studyFolder);
    }

    @Override
    protected void doVerifySteps()
    {
        log("TODO");
        clickLinkContainingText(getProjectName());
        clickLinkContainingText(getFolderName());
        clickLinkContainingText("datasets");
        waitForTextWithRefresh("PVDouble_Two", defaultWaitForPage);
        verifyDemoDataset();
        goBack();
        verifyDemoVisitDataset();
        goBack();
        verifyDemoVisitKeyDataset();


    }

    private void verifyDemoVisitKeyDataset()
    {
        clickLinkContainingText("PVInt_One");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        verifyElements(true, true, true);
    }

    private void verifyDemoVisitDataset()
    {
        clickLinkWithText("PV_Two");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        verifyElements(true, true, false);


    }

    private void verifyDemoDataset()
    {

        clickLinkWithText("P_One");
        CustomizeViewsHelper.openCustomizeViewPanel(this);

        verifyElements(true, false, false);

    }

    private void verifyElements(boolean demoVisibile, boolean visitVisible, boolean extraKeyVisibile)
    {
        // P lookup
        assertTrue("PandaId/PandaId should be visible", CustomizeViewsHelper.isColumnVisible(this, "PandaId/PandaId"));
        assertTrue("PandaId/DataSet lookup should not be visible", CustomizeViewsHelper.isColumnHidden(this, "PandaId/DataSet"));

        // PV lookup
        assertTrue("Panda Visit/PandaId should be visible", CustomizeViewsHelper.isColumnVisible(this, "PandaVisit/PandaId"));
        assertTrue("Panda Visit/Visit should be visible", CustomizeViewsHelper.isColumnVisible(this, "PandaVisit/Visit"));
        assertTrue("Panda Visit/PV_One should not be visible", CustomizeViewsHelper.isColumnHidden(this, "PandaVisit/PV_One"));
        assertTrue("Panda Visit/PV_Two should not be visible", CustomizeViewsHelper.isColumnHidden(this, "PandaVisit/PV_Two"));

        // PVK lookup
        assertTextPresent("DataSets");
        assertEquals("Visibility of id only data sets what was expected", demoVisibile, CustomizeViewsHelper.isColumnVisible(this, "DataSets/P_Two"));
        assertEquals("Visibility of id + visit data sets not what was expected", visitVisible, CustomizeViewsHelper.isColumnVisible(this, "DataSets/PV_Two"));
        assertEquals("Visibility of id, visit, extra key data sets not what was expected", extraKeyVisibile, CustomizeViewsHelper.isColumnVisible(this, "DataSets/PVInt_Two"));
        assertFalse("Visibility of discordant key data sets not what was expected", CustomizeViewsHelper.isColumnPresent(this, "DataSets/PVSInt_Two"));

    }
}

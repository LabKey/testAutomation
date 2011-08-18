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
package org.labkey.test.tests;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.ResetTracker;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 8/4/11
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class EmbeddedWebPartTest extends BaseSeleniumWebTest
{
    ResetTracker resetTracker = null;

    protected  final String PROJECT_NAME = TRICKY_CHARACTERS_FOR_PROJECT_NAMES + "Embedded web part test";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    public void configure()
    {
        RReportHelper.ensureRConfig(this);
        log("Setup project and list module");
        createProject(getProjectName());
        resetTracker = new ResetTracker(this);
//        clickLink(getProjectName());

    }

    @Override
    protected void doTestSteps() throws Exception
    {
        configure();
        embeddedQueryWebPartDoesNotRefreshOnChange();

        //configure for refresh monitoring
    }

    private void embeddedQueryWebPartDoesNotRefreshOnChange()
    {
        log("testing that embedded query web part does not refresh on change");

        //embed query part in wiki page
        addWebPart("Wiki");
        createNewWikiPage();
        clickLinkContainingText("Source", 0, false);
        setFormElement("name", TRICKY_CHARACTERS + "wiki page");

        setWikiBody(getFileContents("server/test/data/api/EmbeddedQueryWebPart.js"));

        clickButton("Save & Close");
        waitForPageToLoad();
        waitForText("Display Name");

        String rViewName = TRICKY_CHARACTERS + "new R view";
        CustomizeViewsHelper.createRView(this, null, rViewName);

        resetTracker.startTrackingRefresh();

        clickMenuButtonAndContinue("Views", rViewName);

        resetTracker.assertWasNotRefreshed();
    }


    @Override
    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        fail("Not implemented");
        return null;
    }
}

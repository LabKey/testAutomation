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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DevModeOnlyTest;
import org.labkey.test.util.ListHelper;

import java.util.HashMap;

/**
 * User: tchadick
 * Date: 9/26/12
 * Time: 1:37 PM
 */

public class ExperimentalFeaturesTest extends BaseWebDriverTest implements DevModeOnlyTest
{
    @Override
    protected String getProjectName()
    {
        return "Experimental Features Test";
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
    }

    private void setExperimentalFeature(ExperimentalFeature feature, boolean enable)
    {
        goToExperimentalFeatures();
        Locator.XPathLocator toggleLink = Locator.xpath("id('labkey-experimental-feature-" + feature + "')");

        if (enable)
        {
            if ("enable".equals(getText(toggleLink).toLowerCase()))
            {
                if(!_initialFeatureStates.containsKey(feature))
                {
                    _initialFeatureStates.put(feature, false);
                }
                click(toggleLink);
                waitForElement(toggleLink.append("[text()='Disable']"));
            }
            else
            {
                log("Experimental feature already enabled: " + feature);
            }
        }
        if (!enable)
        {
            if ("enable".equals(getText(toggleLink).toLowerCase()))
            {
                if(!_initialFeatureStates.containsKey(feature))
                {
                    _initialFeatureStates.put(feature, false);
                }
                click(toggleLink);
                waitForElement(toggleLink.append("[text()='Enable']"));
            }
            else
            {
                log("Experimental feature already disabled: " + feature);
            }
        }
    }

    private void goToExperimentalFeatures()
    {
        if (!_driver.getTitle().equals("Experimental Features"))
        {
            goToAdminConsole();
            clickLinkWithText("experimental features");
        }
    }

    HashMap<ExperimentalFeature, Boolean> _initialFeatureStates = new HashMap<ExperimentalFeature, Boolean>();
    private enum ExperimentalFeature
    {
        JS_DOC ("jsdoc"),
        DETAILS_URL ("details-url"),
        CONTAINER_REL_URLS ("containerRelativeURL"),
        JS_MOTHERSHITP ("javascriptMothership"),
        HIDDEN_EMAIL ("permissionToSeeEmailAddresses"),
        ISSUES_ACTIVITY ("issuesactivity");

        private final String title;
        private ExperimentalFeature (String title)
        {this.title = title;}
        public String toString()
        {return title;}
    }

    public void tearDown() throws Exception
    {
        for (ExperimentalFeature feature : ExperimentalFeature.values())
        {
            if (_initialFeatureStates.containsKey(feature))
                setExperimentalFeature(feature, _initialFeatureStates.get(feature));
        }
        super.tearDown();
    }
}

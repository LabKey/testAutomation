/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.NoSuite;
import org.labkey.test.util.DevModeOnlyTest;

import java.util.HashMap;

@Category(NoSuite.class)
public class ExperimentalFeaturesTest extends BaseWebDriverTest implements DevModeOnlyTest
{
    @Override
    protected String getProjectName()
    {
        return "Experimental Features Test";
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        for (ExperimentalFeature feature : ExperimentalFeature.values())
        {
            if (_initialFeatureStates.containsKey(feature))
                setExperimentalFeature(feature, _initialFeatureStates.get(feature));
        }
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Test
    public void testSteps()
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
                return;
            }
        }
        if (!enable)
        {
            if ("disable".equals(getText(toggleLink).toLowerCase()))
            {
                if(!_initialFeatureStates.containsKey(feature))
                {
                    _initialFeatureStates.put(feature, true);
                }
                click(toggleLink);
                waitForElement(toggleLink.append("[text()='Enable']"));
            }
            else
            {
                log("Experimental feature already disabled: " + feature);
                return;
            }
        }

        if(enable == _initialFeatureStates.get(feature))
        {
            _initialFeatureStates.remove(feature);
        }
    }

    private void goToExperimentalFeatures()
    {
        if (!getDriver().getTitle().equals("Experimental Features"))
        {
            goToAdminConsole();
            clickAndWait(Locator.linkWithText("experimental features"));
        }
    }

    HashMap<ExperimentalFeature, Boolean> _initialFeatureStates = new HashMap<>();
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
}

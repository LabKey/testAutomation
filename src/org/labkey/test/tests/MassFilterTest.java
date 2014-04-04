/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.io.File;

@Category(InDevelopment.class)
public class MassFilterTest extends  FilterTest
{
    @Override
    protected String getProjectName()
    {
        return "Mass Filter Test";
    }

    @Test
    public void testSteps()
    {
        doSetUp();
        doVerify();
    }

    private void doVerify()
    {
        windowMaximize();
        Locator advancedFilteringLoc = Locator.name("value_1");
        Locator.XPathLocator factedFilterLoc = Locator.linkContainingText("[All]");

        sleep(4000);

        startFilter("Small");
        assertElementNotVisible(advancedFilteringLoc);
        assertElementVisible(factedFilterLoc);
        click(Locator.button("CANCEL"));

        refresh(); //stupid selenium issue, it doesn't register the second filter dialogue as visible


        startFilter("ID");
        assertElementVisible(advancedFilteringLoc);
        assertElementNotVisible(factedFilterLoc);
        click(Locator.button("CANCEL"));



    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void doSetUp()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).addWebPart("Lists");
        _listHelper.importListArchive(getProjectName(), new File(getLabKeyRoot(), "/sampledata/MassFilter/massFilter.lists.zip"));
        click(Locator.linkContainingText("101 agents"));

    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

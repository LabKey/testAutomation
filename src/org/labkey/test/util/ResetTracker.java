/*
 * Copyright (c) 2011-2013 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import static org.junit.Assert.*;

/**
 * This class tracks whether or not a page has been updated.
 * currently, it does so by populating the search bar and verifying
 * that it is unchanged at a specified point.  If this proves unsatisfactory
 * in the future, we could consider doing something with javascript.
 */
public class ResetTracker
{
    BaseWebDriverTest test = null;
    protected String searchBoxId = "query";
    protected String searchBoxEntry =  null;

    public ResetTracker(BaseWebDriverTest test)
    {
        this.test=test;
        test.addWebPart("Search");
    }

    protected int resetTrackingCounter = 0;

    public void startTrackingRefresh()
    {
        searchBoxEntry = BaseWebDriverTest.TRICKY_CHARACTERS + "this should not change" + resetTrackingCounter++;
        test.setFormElement(Locator.id(searchBoxId), searchBoxEntry);
    }

    public void stopTrackingRefresh()
    {
        searchBoxEntry = null;
        test.setFormElement(Locator.id(searchBoxId), searchBoxEntry);
    }

    public boolean wasPageRefreshed()
    {
        if(searchBoxEntry==null)
        {
            fail("search box was not iniitalized to wait for refresh");
        }
        String searchBoxContents = test.getFormElement(Locator.id(searchBoxId));
        return !searchBoxContents.equals(searchBoxEntry);
    }

    public void assertWasNotRefreshed()
    {
        assertFalse("Page was unexpectedly refreshed", wasPageRefreshed());
    }

}

/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * User: brittp
 * Date: Nov 30, 2005
 * Time: 10:53:59 PM
 */
public class JUnitTest extends BaseSeleniumWebTest
{
    public String getAssociatedModuleDirectory()
    {
        return "none";
    }

    protected void doCleanup() throws Exception
    {
        // nothing to clean up
    }

    protected void doTestSteps()
    {
        beginAt("/Junit/begin.view");
        log("Run tests");
        //Wait up to 5 minutes!
        clickNavButton("Run All", 1000 * 60 * 5);
        assertTextPresent("SUCCESS");
    }
}

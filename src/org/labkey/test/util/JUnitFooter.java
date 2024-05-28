/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.tests.JUnitTest;

@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class JUnitFooter extends JUnitTest.BaseJUnitTestWrapper
{
    @Test
    public void afterJUnit()
    {
        resetErrors(); // Ignore errors logged by server-side tests.
        log("** This should follow JUnitTest.");
        log("** It will check for memory leaks and clean up the 'Shared/_junit' project.");

        if (_containerHelper.doesContainerExist("Shared/_junit"))
            _containerHelper.deleteFolder("Shared", "_junit");

        if (extraSetup)
        {
            PipelineToolsHelper pipelineToolsHelper = new PipelineToolsHelper(this);
            pipelineToolsHelper.resetPipelineToolsDirectory();

            waitForSystemMaintenanceCompletion();
        }

        super.checkErrors(); // Explicitly perform post-test error check (standard check is blocked by override below)
    }

    @Override
    public void checkErrors()
    {
        // Skip normal check. Server-side tests might generate expected errors.
    }
}

/*
 * Copyright (c) 2010 LabKey Corporation
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

package org.labkey.test.module;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * Created by IntelliJ IDEA.
 * User: Trey Chadick
 * Date: May 27, 2010
 * Time: 4:19:49 PM
 */
public class AdaptiveTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "Adaptive Project";

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        createProject(PROJECT_NAME, "Adaptive Project");
    }

    public String getAssociatedModuleDirectory()
    {
        return "adaptive";
    }

}

/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.tests.assay;

import org.junit.BeforeClass;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.util.RReportHelper;

import java.util.Collections;
import java.util.List;

/**
 * Shared setup/cleanup helpers for assay transform-related WebDriver tests.
 * Consolidates common project creation, R configuration, and project cleanup.
 */
public abstract class AbstractAssayTransformTest extends BaseWebDriverTest
{
    @BeforeClass
    public static void setupProject()
    {
        AbstractAssayTransformTest init = getCurrentTest();
        init.doSetup();
    }

    protected void doSetup()
    {
        new RReportHelper(this).ensureRConfig();
        _containerHelper.createProject(getProjectName(), "Assay");
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.emptyList();
    }
}

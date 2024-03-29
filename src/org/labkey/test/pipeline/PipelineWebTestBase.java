/*
 * Copyright (c) 2008-2019 LabKey Corporation
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
package org.labkey.test.pipeline;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;

/**
 * @author brendanx
 */
abstract public class PipelineWebTestBase extends BaseWebDriverTest
{
    private String _projectName;

    public PipelineWebTestBase(String projectName)
    {
        _projectName = projectName;
    }

    @Override
    public String getProjectName()
    {
        return _projectName;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }
}
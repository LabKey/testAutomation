/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;

/**
 * Placeholder
 * This class should contain most basic page interaction functionality
 * {@link org.labkey.test.BaseWebDriverTest} currently does this
 */
public class LabKeyPage
{
    protected BaseWebDriverTest _test;

    public LabKeyPage(BaseWebDriverTest test)
    {
        _test = test;
        waitForPage();
    }

    protected void waitForPage() {}
}

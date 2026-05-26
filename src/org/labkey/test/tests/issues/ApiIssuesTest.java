/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.test.tests.issues;

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Issues;
import org.labkey.test.util.IssuesApiHelper;

@Category({Issues.class, Daily.class, Data.class})
@BaseWebDriverTest.ClassTimeout(minutes = 20)
/**
 * A version of the issues test that uses the API-based version of the helper.
 */
public class ApiIssuesTest extends IssuesTest
{
    public ApiIssuesTest()
    {
        _issuesHelper = new IssuesApiHelper(this);
    }
}

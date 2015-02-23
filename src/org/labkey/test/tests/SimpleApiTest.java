/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.util.APITestHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @deprecated TODO: Move API test parsing and execution to a Helper class
 * This class does not leave enough flexibility in test design. Do not use except for actual API tests
 *
 * An abstract class that can be used to test recorded API request/response interactions. A typical usage
 * would be for an extending class to perform whatever project setup was required and call super.testSteps.
 * The class would also implement getTestFiles which returns an array of recorded test files, the schema is
 * apiTest.xsd and a test can be recorded using the API test page: query/apiTest.view
 */
@Deprecated
public abstract class SimpleApiTest extends BaseWebDriverTest
{
    /**
     * Returns the list of files to run tests over. Each test file contains metadata representing
     * test cases, the metadata schema can be found in apiTest.xsd
     */
    protected abstract File[] getTestFiles();

    protected Pattern[] getIgnoredElements()
    {
        return new Pattern[0];
    }

    protected void ensureConfigured()
    {

    }

    protected void cleanUp()
    {

    }

    @Test
    public void testSteps() throws Exception
    {
        ensureConfigured();
        runUITests();
        runApiTests();
        cleanUp();
    }

    protected abstract void runUITests() throws Exception;

    public void runApiTests() throws Exception
    {
        APITestHelper apiTester = new APITestHelper(this);
        apiTester.setTestFiles(getTestFiles());
        apiTester.setIgnoredElements(getIgnoredElements());
        apiTester.runApiTests();
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query");
    }
}

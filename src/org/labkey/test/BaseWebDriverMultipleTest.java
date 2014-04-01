/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.test;

import org.junit.Ignore;
import org.junit.Test;
import org.labkey.test.tests.SimpleApiTest;

import java.io.File;

import static org.junit.Assert.*;

/**
 * This class should be used to create tests containing multiple individual test cases (specified by @Test)
 * rather than the single @Test defined in BaseWebDriverTest
 * Test cases should be non-destructive and should not depend on a particular execution order
 * This class blocks execution of standard test methods in BaseWebDriverTest and SimpleApiTest
 *
 * Shared setup steps should be in a public static void method annotated with org.junit.BeforeClass
 * The name of the method is not important. The JUnit runner finds the method solely based on the BeforeClass annotation
 * The method should make an instance of itself, call doCleanup, perform any setup steps, then store the instance
 * See template below -- doSetup() -- for an example.
 *
 * org.junit.Before and org.junit.After annotations are also supported but they are not likely to be useful for Selenium tests
 *
 * org.junit.AfterClass is also supported, but should not be used to perform any destructive cleanup as it would be executed
 *  before the test class can perform its final checks -- link check, leak check, etc.; now located in WebDriverTestPostamble
 * The doCleanup method should be overridden as we've always done for initial and final project cleanup
 *
 * Note:
 * Many of these contortions -- and probably this class entirely -- would be unnecessary if the BeforeClass annotation
 * was not required to be static by JUnit. TestNG does not require BeforeClass and AfterClass methods to be static and
 * might be a good solution for cleaning up the class structure we are using to shoehorn our existing tests into
 * allowing multiple test cases.
 */
public abstract class BaseWebDriverMultipleTest extends SimpleApiTest
{
/*
    @BeforeClass
    public static void doSetup() throws Exception
    {
        MyTestClass initTest = new MyTestClass(); // TODO: replace with actual test class
        initTest.doCleanup(false);

        initTest.doSetupSteps(); // TODO: Perform shared setup steps here

        currentTest = initTest;
    }
*/

    @Override
    protected File[] getTestFiles()
    {
        return new File[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override @Test @Ignore
    public final void testSteps()
    {
        //Block Base @Test method
        fail("Test executing incorrectly");
    }

    @Override
    public final void runUITests() throws Exception
    {
        // Don't use SimpleApiTest test methodology
        // Extending SimpleApiTest for its functionality
        fail("Test executing incorrectly");
    }

    @Override
    public final void runApiTests() throws Exception
    {
        // Don't use SimpleApiTest test methodology
        // Extending SimpleApiTest for its functionality
        fail("Test executing incorrectly");
    }
}

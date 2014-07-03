/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
 * This class can be used to create tests containing multiple individual test cases (specified by @Test)
 * Test cases should be non-destructive and should not depend on a particular execution order
 * This class blocks execution of standard test methods in SimpleApiTest. You should not inherit from this class
 * unless your test requires access to helpers in SimpleApiTest
 *
 * Shared setup steps should be in a public static void method annotated with org.junit.BeforeClass
 * The name of the method is not important. The JUnit runner finds the method solely based on the BeforeClass annotation
 *
 * org.junit.AfterClass is also supported, but should not be used to perform any destructive cleanup as it is executed
 * before the base test class can perform its final checks -- link check, leak check, etc.
 * The doCleanup method should be overridden as we've always done for initial and final project cleanup
 *
 * Note:
 * Many of these contortions would be unnecessary if the BeforeClass annotation was not required to be static by JUnit.
 * TestNG does not require BeforeClass and AfterClass methods to be static and might be a good solution for cleaning up
 * the class structure we are using to shoehorn our existing tests into allowing multiple test cases.
 */
public abstract class BaseWebDriverMultipleTest extends SimpleApiTest
{
/*
    @BeforeClass
    public static void doSetup() throws Exception
    {
        MyTestClass initTest = (MyTestClass)getCurrentTest();
        initTest.setupProject(); // TODO: Perform shared setup steps here
    }
*/

    @Override
    protected File[] getTestFiles()
    {
        return new File[0];
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

/*
 * Copyright (c) 2005-2013 LabKey Corporation
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TestSet
{
    private Class _suite;
    private List<Class> _tests;
    private int _crawlerTimeout;

    TestSet(Set<Class> tests, Class suite, int crawlerTimeout)
    {
        _tests = new ArrayList(tests);
        _suite = suite;
        _crawlerTimeout = crawlerTimeout;
    }

    void setTests(List<Class> tests)
    {
        _tests = tests;
    }

    public String name()
    {
        return _suite.getSimpleName();
    }

    public Class getSuite()
    {
        return _suite;
    }

    public boolean isSuite()
    {
        try
        {
            Method isSuite = _suite.getMethod("isSuite");
            return (boolean)isSuite.invoke(null);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex)
        {
            throw new IllegalArgumentException(_suite.getSimpleName() + " is not a valid test suite", ex);
        }
    }

    public int getCrawlerTimeout()
    {
        return _crawlerTimeout;
    }

    public List<Class> getTestList()
    {
        return _tests;
    }

    public List<String> getTestNames()
    {
        List<String> testNames = new ArrayList<>();
        for (Class test : _tests)
            testNames.add(test.getSimpleName());
        return testNames;
    }

    // Move the named test to the Nth position in the list, maintaining the order of all other tests.
    public boolean prioritizeTest(Class priorityTest, int N)
    {
        if (_tests.contains(priorityTest))
        {
            _tests.remove(priorityTest);
            _tests.add(N, priorityTest);
            return true;
        }
        return false;
    }

    public void randomizeTests()
    {
        Collections.shuffle(_tests);
    }
}

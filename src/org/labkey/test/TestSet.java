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

package org.labkey.test;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.util.Order;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestSet
{
    private final String _suite;
    private List<Class<?>> _tests;

    TestSet(@NotNull Set<Class<?>> tests, @NotNull String suite)
    {
        _tests = new ArrayList<>(tests);
        _suite = suite;
    }

    public TestSet(Set<Class<?>> tests)
    {
        this(tests, "Custom");
    }

    TestSet()
    {
        this(new HashSet<>());
    }

    void setTests(List<Class<?>> tests)
    {
        _tests = tests;
    }

    void addTests(TestSet tests)
    {
        addTests(tests.getTestList());
    }

    void addTests(Collection<Class<?>> tests)
    {
        for (Class<?> test : tests)
        {
            if (!_tests.contains(test))
                _tests.add(test);
        }
    }

    void removeTests(TestSet tests)
    {
        removeTests(tests.getTestList());
    }

    void removeTests(Collection<Class<?>> tests)
    {
        _tests.removeAll(tests);
    }

    public String name()
    {
        return _suite;
    }

    public String getSuite()
    {
        return _suite;
    }

    public List<Class<?>> getTestList()
    {
        return _tests;
    }

    public List<Class<?>> getSortedTestList()
    {
        List<Class<?>> sortedTests = new ArrayList<>(_tests);
        Comparator<Class<?>> comparator = Comparator.comparingDouble(c -> {
            Order annotation = c.getAnnotation(Order.class);
            return annotation == null ? 0.0 : annotation.value();
        });
        comparator = comparator.thenComparingInt(c -> _tests.indexOf(c)); // Retain order of un-annotated tests
        sortedTests.sort(comparator);
        return sortedTests;
    }

    public List<String> getTestNames()
    {
        List<String> testNames = new ArrayList<>();
        for (Class<?> test : _tests)
            testNames.add(test.getSimpleName());
        return testNames;
    }

    // Move the named test to the Nth position in the list, maintaining the order of all other tests.
    public boolean prioritizeTest(Class<?> priorityTest, int N)
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

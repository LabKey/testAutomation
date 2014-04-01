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

import org.junit.experimental.categories.Category;
import org.labkey.test.categories.Continue;
import org.labkey.test.categories.Test;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

public class SuiteBuilder
{
    private static SuiteBuilder _instance = null;
    private static Map<Class, Set<Class>> _suites;

    private SuiteBuilder()
    {
        _suites = new HashMap<>();
        loadSuites();
    }

    public static SuiteBuilder getInstance()
    {
        if (_instance == null)
        {
            _instance = new SuiteBuilder();
        }

        return _instance;
    }

    private void loadSuites()
    {
        Reflections reflections = new Reflections("org.labkey.test");

        Set<Class<?>> tests = reflections.getTypesAnnotatedWith(Category.class);

        _suites.put(Continue.class, new HashSet<Class>()); // Not actually a suite, used to continue interrupted suite

        for (Class test : tests)
        {
            for (Class category : ((Category)test.getAnnotation(Category.class)).value())
            {
                addTestToSuite(test, category);
                Class supercategory = category.getSuperclass();

                while (Test.class.isAssignableFrom(supercategory))
                {
                    addTestToSuite(test, supercategory);
                    supercategory = supercategory.getSuperclass();
                }
            }
        }
    }

    private void addTestToSuite(Class test, Class suite)
    {
        if (!Test.class.isAssignableFrom(suite))
            throw new IllegalArgumentException(suite.getSimpleName() + " is not a valid test suite, check your tests' @Category annotations: valid suites are in org.labkey.test.categories package");

        if (!_suites.containsKey(suite))
            _suites.put(suite, new HashSet<Class>());

        _suites.get(suite).add(test);
    }

    public TestSet getTestSet(Class suite)
    {
        if (!Test.class.isAssignableFrom(suite))
            throw new IllegalArgumentException(suite.getSimpleName() + " is not a valid test suite");

        Method getCrawlerTimeout;
        int timeout;
        try
        {
            getCrawlerTimeout = suite.getMethod("getCrawlerTimeout");
            timeout = (int)getCrawlerTimeout.invoke(null);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex)
        {
            throw new IllegalArgumentException(suite.getSimpleName() + " is not a valid test suite", ex);
        }

        return new TestSet(_suites.get(suite), suite, timeout);
    }

    public TestSet getTestSet(String suiteName)
    {
        for (Class suite : _suites.keySet())
        {
            if (suite.getSimpleName().equalsIgnoreCase(suiteName))
                return getTestSet(suite);
        }

        throw new IllegalArgumentException(suiteName + " is not a valid test suite");
    }

    public Set<Class> getSuites()
    {
        return _suites.keySet();
    }
}

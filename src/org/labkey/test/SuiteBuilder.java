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

import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.categories.Continue;
import org.labkey.test.categories.Test;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SuiteBuilder
{
    private static SuiteBuilder _instance = null;
    private static Map<String, Set<Class>> _suites;

    private SuiteBuilder()
    {
        _suites = new CaseInsensitiveHashMap<>();
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

        _suites.put(Continue.class.getSimpleName(), new HashSet<Class>()); // Not actually a suite, used to continue interrupted suite

        for (Class test : tests)
        {
            for (Class category : ((Category)test.getAnnotation(Category.class)).value())
            {
                addTestToSuite(test, category.getSimpleName());
                Class supercategory = category.getSuperclass();

                while (Test.class.isAssignableFrom(supercategory))
                {
                    addTestToSuite(test, supercategory.getSimpleName());
                    supercategory = supercategory.getSuperclass();
                }
            }

            // parse test package, add module-derived suites. We expect these to follow the pattern
            //    org.labkey.test.tests.<moduleName>.<testClassName>
            String[] packageNameParts = test.getPackage().getName().split("\\.");
            if (packageNameParts != null &&
                    packageNameParts.length >= 5 &&
                    packageNameParts[3].equalsIgnoreCase("tests") )
            {
                addTestToSuite(test, packageNameParts[4]);
                addTestToSuite(test, Test.class.getSimpleName()); // Make sure test is in the master "Test" suite
            }
        }
    }

    private void addTestToSuite(Class test, String suiteName)
    {
        if (!_suites.containsKey(suiteName.toLowerCase()))
            _suites.put(suiteName, new HashSet<Class>());

        _suites.get(suiteName).add(test);
    }

    public TestSet getTestSet(String suiteName)
    {
        return new TestSet(_suites.get(suiteName),suiteName);
    }

    public Set<String> getSuites()
    {
        return _suites.keySet();
    }
}

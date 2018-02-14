/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SuiteBuilder
{
    private static SuiteBuilder _instance;

    private final Map<String, Set<Class>> _suites;

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
        List<String> testPackages = new ArrayList<>(Arrays.asList(System.getProperty("test.packages", "org.labkey.test").split("[^0-9A-Za-z._]+")));
        List<String> extraTestPackages = Arrays.asList(System.getProperty("extra.test.packages", "").split("[^0-9A-Za-z._]+"));
        testPackages.addAll(extraTestPackages);
        testPackages.removeAll(Arrays.asList("", null));

        FilterBuilder filterBuilder = new FilterBuilder();
        Collection<URL> packageUrls = new HashSet<>();
        for (String testPackage : testPackages)
        {
            filterBuilder.includePackage(testPackage);
            packageUrls.addAll(ClasspathHelper.forPackage(testPackage));
        }
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filterBuilder)
                .setUrls(packageUrls)
                .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner()));
        Set<Class<?>> tests = new HashSet<>(reflections.getTypesAnnotatedWith(Category.class));

        tests.removeIf(clz -> Modifier.isAbstract(clz.getModifiers()));
        _suites.put(Continue.class.getSimpleName(), new HashSet<>()); // Not actually a suite, used to continue interrupted suite
        _suites.put(Test.class.getSimpleName(), new HashSet<>()); // Without this, Runner will crash if 'test.packages' property is misconfigured

        for (Class test : tests)
        {
            if (Modifier.isAbstract(test.getModifiers()))
                continue; // Don't try to run abstract test classes, even if they have a @Category annotation

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
            //    <testPackage>.tests.<moduleName>.<testClassName>
            List<String> packageNameParts = Arrays.asList(test.getPackage().getName().split("\\."));
            int testsPkgIndex = packageNameParts.indexOf("tests");
            for (int i = testsPkgIndex + 1; testsPkgIndex > -1 && i < packageNameParts.size(); i++)
            {
                addTestToSuite(test, packageNameParts.get(i));
            }
            addTestToSuite(test, Test.class.getSimpleName()); // Make sure test is in the master "Test" suite
        }
    }

    private void addTestToSuite(Class test, String suiteName)
    {
        if (!_suites.containsKey(suiteName.toLowerCase()))
            _suites.put(suiteName, new HashSet<>());

        _suites.get(suiteName).add(test);
    }

    public TestSet getAllTests()
    {
        return new TestSet(new HashSet<>(_suites.get(Test.class.getSimpleName())), "All");
    }

    public TestSet getTestSet(String suiteName)
    {
        boolean optional = false;
        if (suiteName.startsWith("?"))
        {
            optional = true;
            suiteName = suiteName.substring(1);
        }
        Set<Class> tests = _suites.getOrDefault(suiteName, optional ? Collections.emptySet() : null);
        return new TestSet(tests, suiteName);
    }

    public Set<String> getSuites()
    {
        return _suites.keySet();
    }
}

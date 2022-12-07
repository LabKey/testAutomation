/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
import org.labkey.test.categories.Disabled;
import org.labkey.test.categories.Empty;
import org.labkey.test.categories.Test;
import org.labkey.test.util.TestLogger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SuiteFactory
{
    private static SuiteFactory _instance;

    private final Map<String, List<String>> _requestedMissingTests;
    private final Map<String, Set<Class<?>>> _suites;
    private final Map<String, Class<?>> _testsByName;
    private final Map<String, List<String>> _missingTests;

    private SuiteFactory()
    {
        _requestedMissingTests = new CaseInsensitiveHashMap<>();
        _suites = new CaseInsensitiveHashMap<>();
        _testsByName = new CaseInsensitiveHashMap<>();
        _missingTests = new CaseInsensitiveHashMap<>();
        loadSuites();
    }

    public static SuiteFactory getInstance()
    {
        if (_instance == null)
        {
            _instance = new SuiteFactory();
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
        Map<String, Class<?>> categoriesByName = new HashMap<>();
        reflections.getSubTypesOf(Test.class).forEach(cat ->
        {
            String key = cat.getSimpleName().toLowerCase();
            if (categoriesByName.containsKey(key))
            {
                Class<?> existingCat = categoriesByName.get(key);
                TestLogger.warn(String.format("Ambiguous test category [%s]. Defined in '%s' and '%s'", key, cat.getName(), existingCat.getName()));
            }
            categoriesByName.put(key, cat);
        });

        tests.removeIf(clz -> Modifier.isAbstract(clz.getModifiers()));
        _suites.put(Continue.class.getSimpleName(), Collections.emptySet()); // Not actually a suite, used to continue interrupted suite
        _suites.put(Test.class.getSimpleName(), new HashSet<>()); // Without this, Runner will crash if 'test.packages' property is misconfigured
        _suites.put(Empty.class.getSimpleName(), Collections.emptySet());

        for (Class<?> test : tests)
        {
            if (Modifier.isAbstract(test.getModifiers()))
                continue; // Don't try to run abstract test classes, even if they have a @Category annotation

            {
                // Ensure that test class names are unique
                String simpleName = test.getSimpleName();
                if (_testsByName.containsKey(simpleName))
                {
                    throw new IllegalStateException("Found two tests with the same class name, please rename one of them: " +
                                                        _testsByName.get(simpleName).getName() + " & " + test.getName());
                }
                _testsByName.put(simpleName, test);
            }

            List<Class<?>> categoriesFromAnnotation = new ArrayList<>(Arrays.asList((test.getAnnotation(Category.class)).value()));
            boolean disabledTest = categoriesFromAnnotation.contains(Disabled.class);
            if (disabledTest)
            {
                // Remove disabled tests from all other suites
                categoriesFromAnnotation = Collections.singletonList(Disabled.class);
            }

            /*
             * Parse test package, add module-derived suites. We expect test packages to follow the pattern
             * {testPackage}.tests.moduleName[.featureArea[.featureAreaX]].TestClass
             * Such a test would be added to the following suites:
             *  - moduleName
             *  - featureArea
             *  - featureAreaX
             *  - moduleName.featureArea
             *  - moduleName.featureArea.featureAreaX
             */
            List<String> packageNameParts = Arrays.asList(test.getPackage().getName().split("\\."));
            int testsPkgIndex = packageNameParts.indexOf("tests");
            if (testsPkgIndex > -1)
            {
                packageNameParts = packageNameParts.subList(testsPkgIndex + 1, packageNameParts.size());
                StringBuilder subSuite = new StringBuilder();
                for (int i = 0; i < packageNameParts.size(); i++)
                {
                    String suiteName = packageNameParts.get(i);

                    if (i > 0)
                    {
                        subSuite.append(".");
                    }
                    subSuite.append(suiteName);
                    String subSuiteName = subSuite.toString();

                    if (disabledTest)
                    {
                        suiteName = suiteName + "_disabled";
                        subSuiteName = subSuiteName + "_disabled";
                    }
                    else if (categoriesByName.containsKey(suiteName))
                    {
                        // Respect suite inheritance for package-inferred suites that match explicit @Category suites
                        categoriesFromAnnotation.add(categoriesByName.get(suiteName));
                    }
                    addTestToSuite(test, suiteName);
                    if (i > 0)
                    {
                        addTestToSuite(test, subSuiteName);
                    }
                }
            }

            for (Class<?> category : categoriesFromAnnotation)
            {
                addTestToSuite(test, category.getSimpleName());
                Class<?> supercategory = category.getSuperclass();

                while (supercategory != null && Test.class.isAssignableFrom(supercategory))
                {
                    addTestToSuite(test, supercategory.getSimpleName());
                    supercategory = supercategory.getSuperclass();
                }
            }

            addTestToSuite(test, Test.class.getSimpleName()); // Make sure test is in the master "Test" suite
        }

        loadFileBasedSuites();
    }

    private void loadFileBasedSuites()
    {
        List<File> suitesDirs = TestFileUtils.getSampleDatas("suites");
        for (File suiteDir : suitesDirs)
        {
            File[] suiteFiles = suiteDir.listFiles(file -> !file.getName().startsWith("_") && file.getName().endsWith(".txt"));
            for (File suiteFile : suiteFiles)
            {
                String suiteName = suiteFile.getName().split("\\.")[0]; // drop file extension
                List<String> testList = Arrays.stream(TestFileUtils.getFileContents(suiteFile).trim().split("\\s+"))
                    .filter(testName -> !testName.startsWith("#")).collect(Collectors.toList());
                for (String testName : testList)
                {
                    Class<?> testClass = _testsByName.get(testName);
                    if (testClass == null)
                    {
                        if (!_missingTests.containsKey(suiteName))
                        {
                            _missingTests.put(suiteName, new ArrayList<>());
                        }
                        _missingTests.get(suiteName).add(testName);
                    }
                    else
                    {
                        addTestToSuite(testClass, suiteName);
                    }
                }
            }
        }
    }

    private void addTestToSuite(Class<?> test, String suiteName)
    {
        if (!_suites.containsKey(suiteName.toLowerCase()))
            _suites.put(suiteName, new HashSet<>());

        _suites.get(suiteName).add(test);
    }

    public Class<?> getTestByName(String testClassName)
    {
        return _testsByName.get(testClassName);
    }

    public TestSet getAllTests()
    {
        return new TestSet(new HashSet<>(_suites.get(Test.class.getSimpleName())), "All");
    }

    public TestSet getTestSet(String suiteName)
    {
        SuiteInfo suiteInfo = new SuiteInfo(suiteName);
        boolean optional = suiteInfo.isOptional();
        int subset = suiteInfo.getSubset();
        int subsetCount = suiteInfo.getSubsetCount();
        suiteName = suiteInfo.getName();

        Set<Class<?>> tests = _suites.getOrDefault(suiteName, optional ? Collections.emptySet() : null);

        if (tests != null && _missingTests.containsKey(suiteName))
        {
            // Move missing tests to static member for MissingTestsError to see.
            _requestedMissingTests.put(suiteName, _missingTests.get(suiteName));
            tests.add(MissingTests.class);
        }

        tests = extractBatch(tests, subset, subsetCount);

        if (tests == null)
            return null;

        return new TestSet(tests, suiteName);
    }

    public static Set<Class<?>> extractBatch(Set<Class<?>> tests, int batch, int batchCount)
    {
        if (tests == null || tests.isEmpty() || batchCount == 1)
        {
            return tests;
        }

        List<Class<?>> sorted = new ArrayList<>(tests);
        sorted.sort(Comparator.comparing(Class::getName));

        int size = sorted.size();
        int index = batch - 1;
        int batchSize = size / batchCount;
        int remainder = size % batchCount;
        int isLargeBatch = index < remainder ? 1 : 0;
        int fromIndex = batchSize * index + Math.min(index, remainder);
        int toIndex = Math.min(fromIndex + batchSize + isLargeBatch, size);

        return new HashSet<>(sorted.subList(fromIndex, toIndex));
    }

    public TestSet getEmptyTestSet()
    {
        return getTestSet(Empty.class.getSimpleName());
    }

    public Set<String> getSuites()
    {
        return _suites.keySet();
    }

    public static Map<String, List<String>> getRequestedMissingTests()
    {
        return new CaseInsensitiveHashMap<>(getInstance()._requestedMissingTests);
    }

    public static class MissingTests
    {
        public MissingTests() { }

        @Override
        public String toString()
        {
            return "MissingTests";
        }

        @org.junit.Test
        public void run()
        {
            Map<String, List<String>> requestedMissingTests = SuiteFactory.getRequestedMissingTests();
            StringBuilder msg = new StringBuilder("Suite specifies non-existent test(s):");
            for (String suite : requestedMissingTests.keySet())
            {
                msg.append("\n    ").append(suite).append(": ").append(requestedMissingTests.get(suite));
            }
            throw new IllegalArgumentException(msg.toString());
        }
    }

    public static class SuiteInfo
    {
        private final String name;
        private final int subset;
        private final int subsetCount;
        private final boolean optional;

        public SuiteInfo(String suiteName)
        {
            if (suiteName.startsWith("?"))
            {
                optional = true;
                suiteName = suiteName.substring(1);
            }
            else
            {
                optional = false;
            }
            // Support syntax for splitting up test suites
            // e.g. "Daily[1/3]" to select the first third of tests in the Daily suite
            // TODO: Remove in favor of Runner.BatchInfo
            Pattern pattern = Pattern.compile("(.+)\\[(\\d+)/(\\d+)]");
            Matcher matcher = pattern.matcher(suiteName);
            if (matcher.matches())
            {
                subset = Integer.parseInt(matcher.group(2));
                subsetCount = Integer.parseInt(matcher.group(3));
                if (subset < 1 || subsetCount < 1 || subset > subsetCount)
                {
                    throw new IllegalArgumentException("Invalid subsuite specification: " + suiteName);
                }

                name = matcher.group(1);
            }
            else
            {
                subset = 1;
                subsetCount = 1;
                name = suiteName;
            }
        }

        public String getName()
        {
            return name;
        }

        public int getSubset()
        {
            return subset;
        }

        public int getSubsetCount()
        {
            return subsetCount;
        }

        public boolean isOptional()
        {
            return optional;
        }
    }
}

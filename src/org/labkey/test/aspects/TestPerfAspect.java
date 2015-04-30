/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
package org.labkey.test.aspects;

import junit.framework.Test;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.util.HashMap;

@Aspect
public class TestPerfAspect
{
    private static HashMap<String, HashMap<TestSection, Long>> _testClassMethodPerfStats = new HashMap<>();

    private static long _startTime;
    private static TestSection _currentSection = null;
    private static HashMap<TestSection, Long> _methodTimes = new HashMap<>();

    public void markSection(TestSection section)
    {
        if (_currentSection != section)
        {
            if(_currentSection != null)
            {
                Long curMethodTime = System.currentTimeMillis() - _startTime;
                _methodTimes.put(_currentSection, curMethodTime);
            }

            _startTime = System.currentTimeMillis();
            _currentSection = section;
        }
    }

    /**
     * Pointcut to reset perf number before each test class
     */
    @Pointcut(value = "execution(@org.junit.BeforeClass * org.labkey.test.BaseWebDriverTest.*())")
    void startUp(){}
    @Before(value = "startUp()", argNames = "joinPoint")
    public void beforeTestClass(JoinPoint joinPoint)
    {
        _currentSection = null;
        _startTime = 0;
        _methodTimes = new HashMap<>();

        markSection(TestSection.BEFORE);
    }

    /**
     * Pointcut for transition between @BeforeClass methods and @Test methods
     */
    @Pointcut(value = "execution(@org.junit.Test * org.labkey.test..*())")
    void testMethod(){}
    @Before(value = "testMethod()", argNames = "joinPoint")
    public void beforeTestMethod(JoinPoint joinPoint)
    {
        markSection(TestSection.TESTS);
    }

    /**
     * Pointcut for transition between @Test methods and @AfterClass methods
     */
    @Pointcut(value = "execution(@org.junit.AfterClass * org.labkey.test..*())")
    void afterClassMethod(){}
    @Before(value = "afterClassMethod()", argNames = "joinPoint")
    public void afterAfterClassMethod(JoinPoint joinPoint)
    {
        markSection(TestSection.AFTER);
    }

    /**
     * Pointcut to collect perf numbers after each test class
     */
    @Pointcut(value = "execution(@org.junit.AfterClass * org.labkey.test.BaseWebDriverTest.*())")
    void postamble(){}
    @After(value = "postamble()", argNames = "joinPoint")
    public void afterTestClass(JoinPoint joinPoint)
    {
        markSection(null);

        _testClassMethodPerfStats.put(joinPoint.getStaticPart().getSignature().getName(), _methodTimes);
    }

    public static void savePerfStats(Test test)
    {
        _testClassMethodPerfStats.put(test.toString().substring(test.toString().lastIndexOf(".") + 1), _methodTimes);
    }

    public static HashMap<TestSection, Long> getPerfStats(String testName)
    {
        if (_testClassMethodPerfStats.containsKey(testName))
            return _testClassMethodPerfStats.get(testName);
        else
            return new HashMap<>();
    }

    public enum TestSection
    {BEFORE, TESTS, AFTER}
}

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
package org.labkey.test.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.junit.Test;
import org.labkey.test.util.TestLogger;

import java.util.concurrent.TimeUnit;

@Deprecated
@Aspect
public class TestLoggingAspect
{
    private String testCaseName;
    private Long testCaseStartTimeStamp;

    @Pointcut()
    void testCaseMethod(){}

    @Before(value = "testCaseMethod()", argNames = "joinPoint")
    public void beforeTestCase(JoinPoint joinPoint)
    {
        TestLogger.resetLogger();
        testCaseStartTimeStamp = System.currentTimeMillis();
        testCaseName = joinPoint.getStaticPart().getSignature().getName();

        if (!"testSteps".equals(testCaseName))
            TestLogger.log("// Begin Test Case - " + testCaseName + " \\\\");
        else
            TestLogger.log("// Begin Test Case \\\\");

        TestLogger.increaseIndent();
    }

    @AfterReturning(value = "testCaseMethod() && @annotation(test)", argNames = "joinPoint, test")
    public void afterTestCaseSuccess(JoinPoint joinPoint, Test test)
    {
        if (!test.expected().equals(Test.None.class))
            logTestFailure(); // Sometimes an exception is expected
        else
            logTestSuccess();

    }

    @AfterThrowing(value = "testCaseMethod() && @annotation(test)", throwing = "throwable", argNames = "joinPoint, test, throwable")
    public void afterTestCaseFailure(JoinPoint joinPoint, Test test, Throwable throwable)
    {
        if (test.expected().equals(throwable.getClass()))
            logTestSuccess(); // Sometimes an exception is expected
        else
            logTestFailure();
    }

    private void logTestSuccess()
    {
        Long elapsed = System.currentTimeMillis() - testCaseStartTimeStamp;

        TestLogger.resetLogger();
        if (!"testSteps".equals(testCaseName))
            TestLogger.log("\\\\ Test Case Complete - " + testCaseName + " [" + getElapsedString(elapsed) + "] //"); // Only log on successful return
        else
            TestLogger.log("\\\\ Test Case Complete //");
    }

    private void logTestFailure()
    {
        Long elapsed = System.currentTimeMillis() - testCaseStartTimeStamp;

        TestLogger.resetLogger();
        if (!"testSteps".equals(testCaseName))
            TestLogger.log("\\\\ Failed Test Case - " + testCaseName + " [" + getElapsedString(elapsed) + "] //"); // Only log on successful return
        else
            TestLogger.log("\\\\ Failed Test Case //");
    }

    private String getElapsedString(Long elapsed)
    {
        return String.format("%dm %d.%ds",
                TimeUnit.MILLISECONDS.toMinutes(elapsed),
                TimeUnit.MILLISECONDS.toSeconds(elapsed) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)),
                elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed)));
    }
}


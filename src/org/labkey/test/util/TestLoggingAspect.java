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
package org.labkey.test.util;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * User: tchadick
 * Date: 2/15/13
 * Time: 8:27 PM
 */
@Aspect
public class TestLoggingAspect
{
    private String testCaseName;
    private Long testCaseStartTimeStamp;

    @Pointcut(value = "execution(@org.junit.Test * *(..))")
    void testCaseMethod(){}

    @Before(value = "testCaseMethod()", argNames = "joinPoint")
    public void beforeTestCase(JoinPoint joinPoint)
    {
        TestLogger.resetIndent();
        testCaseStartTimeStamp = System.currentTimeMillis();
        testCaseName = joinPoint.getStaticPart().getSignature().getName();

        if (!"testSteps".equals(testCaseName))
            TestLogger.log("// Begin Test Case - " + testCaseName + " \\\\");
        else
            TestLogger.log("// Begin Test Case \\\\");

        TestLogger.increaseIndent();
    }

    @AfterReturning(value = "testCaseMethod()", argNames = "joinPoint")
    public void afterTestCaseSuccess(JoinPoint joinPoint)
    {
        Long elapsed = System.currentTimeMillis() - testCaseStartTimeStamp;

        String elapsedStr = String.format("%dm %d.%ds",
                TimeUnit.MILLISECONDS.toMinutes(elapsed),
                TimeUnit.MILLISECONDS.toSeconds(elapsed) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)),
                elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed)));
        TestLogger.resetIndent();
        if (!"testSteps".equals(testCaseName))
            TestLogger.log("\\\\ Test Case Complete - " + testCaseName + " [" + elapsedStr + "] //"); // Only log on successful return
        else
            TestLogger.log("\\\\ Test Case Complete //");
    }

    @AfterThrowing(value = "testCaseMethod()", argNames = "joinPoint")
    public void afterTestCaseFailure(JoinPoint joinPoint)
    {
        Long elapsed = System.currentTimeMillis() - testCaseStartTimeStamp;

        String elapsedStr = String.format("%dm %d.%ds",
                TimeUnit.MILLISECONDS.toMinutes(elapsed),
                TimeUnit.MILLISECONDS.toSeconds(elapsed) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)),
                elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed)));
        TestLogger.resetIndent();
        if (!"testSteps".equals(testCaseName))
            TestLogger.log("\\\\ Failed Test Case - " + testCaseName + " [" + elapsedStr + "] //"); // Only log on successful return
        else
            TestLogger.log("\\\\ Failed Test Case //");
    }
}


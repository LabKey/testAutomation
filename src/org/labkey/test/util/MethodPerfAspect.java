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

import junit.framework.Test;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.HashMap;
import java.util.Stack;

/**
 * User: tchadick
 * Date: 2/15/13
 * Time: 8:47 PM
 */

@Aspect
public class MethodPerfAspect
{
    private static HashMap<String, HashMap<LogMethod.MethodType, Long>> _testClassMethodPerfStats = new HashMap<String, HashMap<LogMethod.MethodType, Long>>();

    private static Stack<Long> _startTimes = new Stack<Long>();
    private static Stack<LogMethod.MethodType> _methodTypesStack = new Stack<LogMethod.MethodType>();
    private static HashMap<LogMethod.MethodType, Long> _methodTimes = new HashMap<LogMethod.MethodType, Long>();

    @Pointcut(value = "execution(@org.labkey.test.util.LogMethod * *(..))")
    void loggedMethod(){}

    @Before(value = "loggedMethod() && @annotation(logMethod)", argNames = "joinPoint, logMethod")
    public void beforeLoggedMethod(JoinPoint joinPoint, LogMethod logMethod)
    {
        if (logMethod.category() != LogMethod.MethodType.UNSPECIFIED)
        {
            MethodSignature signature = (MethodSignature) joinPoint.getStaticPart().getSignature();
            String method = signature.getName();

            _startTimes.push(System.currentTimeMillis());
            if (_methodTypesStack.isEmpty() || _methodTypesStack.peek() == logMethod.category())
                _methodTypesStack.push(logMethod.category());
            else
                throw new IllegalStateException("Attempting to call a " + logMethod.category() + " method from within a " + _methodTypesStack.peek() + " method");
        }
    }

    @After(value = "loggedMethod() && @annotation(logMethod)", argNames = "logMethod")
    public void afterLoggedMethod(LogMethod logMethod)
    {
        if (logMethod.category() != LogMethod.MethodType.UNSPECIFIED)
        {
            LogMethod.MethodType methodType = _methodTypesStack.pop();
            Long curStartTime = _startTimes.pop();
            if (_methodTypesStack.isEmpty())
            {
                Long curMethodTime = System.currentTimeMillis() - curStartTime;
                if (_methodTimes.containsKey(methodType))
                {
                    Long totalTypeTime = _methodTimes.get(methodType);
                    _methodTimes.put(methodType, totalTypeTime + curMethodTime);
                }
                else
                {
                    _methodTimes.put(methodType, curMethodTime);
                }
            }
        }
    }

    /**
     * Pointcut to reset perf number before each test
     */
    @Pointcut(value = "execution(@org.junit.Test * org.labkey.test..*())")
    void testMethod(){}

    @Before(value = "testMethod()", argNames = "joinPoint")
    public void beforeTestMethod(JoinPoint joinPoint)
    {
        _startTimes = new Stack<Long>();
        _methodTypesStack = new Stack<LogMethod.MethodType>();
        _methodTimes = new HashMap<LogMethod.MethodType, Long>();
    }

    @After(value = "testMethod()", argNames = "joinPoint")
    public void afterTestMethod(JoinPoint joinPoint)
    {
        _testClassMethodPerfStats.put(joinPoint.getStaticPart().getSignature().getName(), _methodTimes);
    }

    public static void savePerfStats(Test test)
    {
        _testClassMethodPerfStats.put(test.toString().substring(test.toString().lastIndexOf(".") + 1), _methodTimes);
    }

    public static HashMap<LogMethod.MethodType, Long> getPerfStats(String testName)
    {
        if (_testClassMethodPerfStats.containsKey(testName))
            return _testClassMethodPerfStats.get(testName);
        else
            return new HashMap<LogMethod.MethodType, Long>();
    }
}

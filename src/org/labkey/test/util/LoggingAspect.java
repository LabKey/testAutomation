/*
 * Copyright (c) 2012 LabKey Corporation
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
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

/**
 * User: tchadick
 * Date: 10/1/12
 * Time: 3:17 PM
 */

@Aspect
public class LoggingAspect
{
    private static final int indentStep = 2;
    private static int currentIndent = 0;
    private static Stack<Long> startTimes = new Stack<Long>();
    private static Stack<String> methodStack = new Stack<String>();

    @Pointcut(value = "execution(@org.labkey.test.util.LogMethod * *(..))")
    void loggedMethod(){}

    @Before(value = "loggedMethod()", argNames = "joinPoint")
    public void beforeLoggedMethod(JoinPoint joinPoint)
    {
        String caller = methodStack.isEmpty() ? "" : methodStack.peek();
        String method = joinPoint.getSignature().getName();
        methodStack.push(method);
        startTimes.push(System.currentTimeMillis());
        if (!method.equals(caller)) // Don't double-log overloaded methods
        {
            log(">>" + method);
            currentIndent += indentStep;
        }
    }

    @AfterReturning(value = "loggedMethod()", argNames = "joinPoint")
    public void afterLoggedMethod(JoinPoint joinPoint)
    {
        methodStack.pop(); // Discard current method
        String caller = methodStack.isEmpty() ? "" : methodStack.peek();
        Long elapsed = System.currentTimeMillis()-startTimes.pop();
        String method = joinPoint.getSignature().getName();

        if (!method.equals(caller)) // Don't double-log overloaded methods
        {
            String elapsedStr = String.format("%dm%d.%ds",
                    TimeUnit.MILLISECONDS.toMinutes(elapsed),
                    TimeUnit.MILLISECONDS.toSeconds(elapsed) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)),
                    elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed)));
            currentIndent -= indentStep;
            log("<<" + method + " done [" + elapsedStr + "]"); // Only log on successful return
        }
    }

    @AfterThrowing(value = "loggedMethod()", argNames = "joinPoint")
    public void afterLoggedMethodException(JoinPoint joinPoint)
    {
        startTimes.pop();
        methodStack.pop();
        currentIndent -= indentStep;
    }

    private static String getIndentString()
    {
        String indentStr = "";
        for (int i = 0; i < currentIndent; i++)
            indentStr += " ";
        return indentStr;
    }

    public static void log(String str)
    {
        String d = new SimpleDateFormat("HH:mm:ss,SSS").format(new Date()); // Include time with log entry.  Use format that matches labkey log.
        System.out.println(d + " " + getIndentString() + str);
    }
}

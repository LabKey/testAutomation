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
package org.labkey.test.aspects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

@Aspect
public class MethodLoggingAspect
{
    private static final Stack<Long> startTimes = new Stack<>();
    private static final Stack<String> methodStack = new Stack<>();
    private static final Stack<String> quietMethods = new Stack<>();
    private static final Stack<String> quietMethodsArgStrings = new Stack<>();

    @Pointcut(value = "execution(@org.labkey.test.util.LogMethod * *(..))")
    void loggedMethod(){}

    @Before(value = "loggedMethod() && @annotation(logMethod)", argNames = "joinPoint, logMethod")
    public void beforeLoggedMethod(JoinPoint joinPoint, LogMethod logMethod)
    {
        MethodSignature signature = (MethodSignature) joinPoint.getStaticPart().getSignature();
        String caller = methodStack.isEmpty() ? "" : methodStack.peek();
        String method = signature.getName();
        methodStack.push(method);
        startTimes.push(System.currentTimeMillis());

        List<Object> loggedParameters = new ArrayList<>();
        Annotation[][] annotations = signature.getMethod().getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++)
        {
            Annotation[] methodAnnotations = annotations[i];
            for (Annotation annotation : methodAnnotations)
            {
                if (annotation instanceof LoggedParam)
                {
                    loggedParameters.add(joinPoint.getArgs()[i]);
                    break;
                }
            }
        }

        String argsString = getArgsString(loggedParameters);

        if (logMethod.quiet() && !TestLogger.log().isEnabled(Level.TRACE))
        {
            TestLogger.suppressLogging(true);
            quietMethods.add(method);
            quietMethodsArgStrings.add(argsString);
        }

        if (!method.equals(caller)) // Don't double-log overloaded methods
        {
            TestLogger.info(">>" + method + argsString);
            TestLogger.increaseIndent();
        }
    }

    @AfterReturning(value = "loggedMethod() && @annotation(logMethod)", argNames = "joinPoint, logMethod")
    public void afterLoggedMethod(JoinPoint joinPoint, LogMethod logMethod)
    {
        logMethodEnd(joinPoint, logMethod, "<<");
    }

    @AfterThrowing(value = "loggedMethod() && @annotation(logMethod)", throwing = "e", argNames = "joinPoint, logMethod, e")
    public void afterLoggedMethodException(JoinPoint joinPoint, LogMethod logMethod, Throwable e)
    {
        String thrown = e.getClass().getSimpleName();
        logMethodEnd(joinPoint, logMethod, "** " + thrown + " **");
    }

    private void logMethodEnd(JoinPoint joinPoint, LogMethod logMethod, String logPrefix)
    {
        methodStack.pop(); // Discard current method, duplicated in joinPoint
        String caller = methodStack.isEmpty() ? "" : methodStack.peek();
        long elapsed = System.currentTimeMillis() - startTimes.pop();
        String method = joinPoint.getStaticPart().getSignature().getName();

        String argString = " done";

        if (logMethod.quiet())
        {
            quietMethods.pop();
            argString = quietMethodsArgStrings.pop();
        }
        if (quietMethods.empty())
            TestLogger.suppressLogging(false);

        if (!method.equals(caller)) // Don't double-log overloaded methods
        {
            String elapsedStr = TestLogger.formatElapsedTime(elapsed);
            TestLogger.decreaseIndent();
            // Only log on successful return
            TestLogger.info(logPrefix + method + argString + " " + elapsedStr);
        }
    }

    private static final String T_STR = "\u2026"; // UTF ellipses
    /**
     * Generate loggable string representation of LoggedParams
     */
    private String getArgsString(List<Object> args)
    {
        if (args.isEmpty())
            return "";

        final int targetLength = 120;
        String prevArgsString = "";
        String argsString;
        int maxArgLength = Math.max(targetLength / args.size(), 30);
        boolean done = false;

        do
        {
            StringBuilder argBuilder = new StringBuilder();
            for (Object arg : args)
            {
                if (argBuilder.length() > 0)
                    argBuilder.append(", ");
                argBuilder.append(getArgString(arg, maxArgLength));
            }
            argsString = argBuilder.toString();
            if (argsString.length() >= targetLength || !argsString.contains(T_STR) || argsString.equals(prevArgsString))
            {
                done = true;
            }
            else
            {
                prevArgsString = argsString;
                int truncatedArgCount = StringUtils.countMatches(argsString, T_STR);
                int roomToGrow = targetLength - argsString.length();
                int extraPerArg = roomToGrow / truncatedArgCount;
                maxArgLength += extraPerArg;
            }
        }while (!done);

        argsString = (argsString.length() > 0 ? "(" + argsString + ")" : "");
        return argsString;
    }

    private String getArgString(Object arg, int maxArgLength)
    {
        StringBuilder argString = new StringBuilder();
        if (arg instanceof Object[])
        {
            for (Object nestedArg : (Object[])arg)
            {
                argString.append(argString.length() > 0 ? ", " : "").append(getArgString(nestedArg, maxArgLength));
            }
            argString = new StringBuilder("[" + argString + "]");
        }
        else if (arg instanceof Collection)
        {
            for (Object nestedArg : (Collection<?>)arg)
            {
                argString.append(argString.length() > 0 ? ", " : "").append(getArgString(nestedArg, maxArgLength));
            }
            argString = new StringBuilder("[" + argString + "]");
        }
        else if (arg instanceof File)
        {
            argString = new StringBuilder(((File) arg).getName());
        }
        else if (arg instanceof Duration)
        {
            argString = new StringBuilder(((Duration) arg).toString().replace("PT", ""));
        }
        else
        {
            if (arg != null && arg.toString() != null)
            {
                argString = new StringBuilder(arg.toString());
                if (!(arg instanceof Number || arg instanceof Boolean))
                {
                    if (argString.length() > maxArgLength)
                        argString = new StringBuilder(T_STR + argString.substring(argString.length() - maxArgLength)); // trim start of long arguments
                    argString = new StringBuilder("'" + argString + "'");
                }
            }
            else
                argString = new StringBuilder("null");
        }
        return argString.toString();
    }
}

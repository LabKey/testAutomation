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

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Stack;
import java.util.concurrent.TimeUnit;

/**
 * User: tchadick
 * Date: 11/6/12
 * Time: 2:55 PM
 */

@Aspect
public class ImpersonationLoggingAspect
{
    private static Stack<Long> _startTimes = new Stack<Long>();
    private static Stack<String> _impersonatingStack = new Stack<String>();

    @Pointcut(value = "execution(void impersonate*(String, ..)) && args(impersonating, ..)")
    void startImpersonation(String impersonating){}
    @Pointcut(value = "execution(void stopImpersonating*())")
    void stopImpersonation(){}

    @Before(value = "startImpersonation(impersonating)", argNames = "impersonating")
    public void beforeImpersonation(String impersonating)
    {
        _impersonatingStack.push(impersonating);
        _startTimes.push(System.currentTimeMillis());

        TestLogger.log(">>Impersonate - "+impersonating);
        TestLogger.increaseIndent();
    }

    @AfterReturning(value = "stopImpersonation()")
    public void afterImpersonation()
    {
        String impersonating = _impersonatingStack.pop();
        Long elapsed = System.currentTimeMillis()- _startTimes.pop();

        String elapsedStr = String.format("%dm %d.%ds",
                TimeUnit.MILLISECONDS.toMinutes(elapsed),
                TimeUnit.MILLISECONDS.toSeconds(elapsed) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)),
                elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed)));

        TestLogger.decreaseIndent();
        TestLogger.log("<<Stop Impersonating - " + impersonating + " [" + elapsedStr + "]");
    }

}

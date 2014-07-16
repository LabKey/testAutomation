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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.StaleElementReferenceException;

@Aspect
public class StaleElementRetryAspect
{
    private static boolean retried;

    @Pointcut(value = "execution(* org.labkey.test..*(org.labkey.test.Locator, ..))")
    void locatorMethod(){}

    @Around(value = "locatorMethod()", argNames = "joinPoint")
    public Object beforeLoggedMethod(ProceedingJoinPoint joinPoint) throws Throwable
    {
        try
        {
            retried = false;
            return joinPoint.proceed();
        }
        catch (StaleElementReferenceException staleElementException)
        {
            if (!retried)
            {
                retried = true;
                MethodSignature signature = (MethodSignature) joinPoint.getStaticPart().getSignature();
                TestLogger.log("Stale Element - Retry " + signature.getMethod().getName());
                return joinPoint.proceed();
            }
            else
                throw staleElementException;
        }
    }
}

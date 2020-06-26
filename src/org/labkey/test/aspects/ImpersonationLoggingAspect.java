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

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.labkey.test.util.TestLogger;

@Aspect
public class ImpersonationLoggingAspect
{
    private static Long _startTime;
    private static String _impersonating;

    @Pointcut(value = "execution(void impersonate*(String, ..)) && args(impersonating, ..)")
    void startImpersonation(String impersonating){}
    @Pointcut(value = "execution(void stopImpersonating*())")
    void stopImpersonation(){}

    @Before(value = "startImpersonation(impersonating)", argNames = "impersonating")
    public void beforeImpersonation(String impersonating)
    {
        if (_impersonating == null)
        {
            TestLogger.log(">>Impersonate - " + impersonating);
            TestLogger.increaseIndent();

            _impersonating = impersonating;
            _startTime = System.currentTimeMillis();
        }
        else
        {
            TestLogger.decreaseIndent();
            TestLogger.log("><Switch Impersonation : " + _impersonating + TestLogger.formatElapsedTime(System.currentTimeMillis() - _startTime) + " -> " + impersonating);
            TestLogger.increaseIndent();

            _impersonating = impersonating;
            _startTime = System.currentTimeMillis();
        }
    }

    @AfterReturning(value = "stopImpersonation()")
    public void afterImpersonation()
    {
        if (_startTime == null)
            return;

        String impersonating = _impersonating;

        String elapsedStr = TestLogger.formatElapsedTime(System.currentTimeMillis() - _startTime);

        TestLogger.decreaseIndent();
        TestLogger.log("<<Stop Impersonating - " + impersonating + elapsedStr);

        _impersonating = null;
    }

}

package org.labkey.test.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.StaleElementReferenceException;

/**
 * User: tchadick
 * Date: 11/22/13
 */

@Aspect
public class StaleElementRetryAspect
{
    private static int depth = 0;
    private static boolean retried = false;

    @Pointcut(value = "execution(* org.labkey.test..*(org.labkey.test.Locator, ..))")
    void locatorMethod(){}

    @Around(value = "locatorMethod()", argNames = "joinPoint")
    public Object beforeLoggedMethod(ProceedingJoinPoint joinPoint) throws Throwable
    {
        MethodSignature signature = (MethodSignature) joinPoint.getStaticPart().getSignature();

        depth++;

        try
        {
            return joinPoint.proceed();
        }
        catch (StaleElementReferenceException staleElementException)
        {
            if (!retried)
            {
                retried = true;
                TestLogger.log("Stale Element - Retry " + signature.getMethod().getName());
                return joinPoint.proceed();
            }
            else
                throw staleElementException;
        }
        finally
        {
            depth--;
            if (depth == 0)
                retried = false;
        }
    }
}

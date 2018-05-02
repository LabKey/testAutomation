package org.labkey.test.util;

import org.labkey.test.WebDriverWrapper;

public class ReflectionUtils
{
    public static ConfiguresSite getSiteConfigurerOrDefault(String className, WebDriverWrapper driverWrapper)
    {
        try
        {
            Class<?> aClass = Class.forName(className);
            if (ConfiguresSite.class.isAssignableFrom(aClass))
            {
                try
                {
                    ConfiguresSite siteConfigurer = (ConfiguresSite) aClass.newInstance();
                    siteConfigurer.setWrapper(driverWrapper);
                    return siteConfigurer;
                }
                catch (InstantiationException | IllegalAccessException e)
                {
                    throw new IllegalArgumentException("Unable to instantiate site configurer: " + className);
                }
            }
            else
            {
                throw new IllegalArgumentException(className + " does not implement " + ConfiguresSite.class.getName());
            }
        }
        catch (ClassNotFoundException e)
        {
            TestLogger.log("Site configurer does not exist: " + className + ". Using default.");
            return new DefaultSiteConfigurer();
        }
    }
}

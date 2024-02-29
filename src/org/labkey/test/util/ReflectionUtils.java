/*
 * Copyright (c) 2018-2019 LabKey Corporation
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

import org.labkey.test.WebDriverWrapper;

import java.lang.reflect.InvocationTargetException;

public class ReflectionUtils
{
    public static ConfiguresSite getSiteConfigurerOrDefault(String className, WebDriverWrapper driverWrapper)
    {
        try
        {
            ConfiguresSite siteConfigurer = getSiteConfigurer(className);
            siteConfigurer.setWrapper(driverWrapper);
            return siteConfigurer;
        }
        catch (ClassNotFoundException e)
        {
            TestLogger.warn("Site configurer does not exist: " + className + ". Using default.", e);
            return new DefaultSiteConfigurer();
        }
    }

    public static ConfiguresSite getSiteConfigurer(String className) throws ClassNotFoundException
    {
        Class<?> aClass = Class.forName(className);
        if (ConfiguresSite.class.isAssignableFrom(aClass))
        {
            try
            {
                return (ConfiguresSite) aClass.getDeclaredConstructor().newInstance();
            }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
            {
                throw new IllegalArgumentException("Unable to instantiate site configurer: " + className, e);
            }
        }
        else
        {
            throw new IllegalArgumentException(className + " does not implement " + ConfiguresSite.class.getName());
        }
    }
}

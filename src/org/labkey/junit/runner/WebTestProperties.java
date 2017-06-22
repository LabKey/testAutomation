/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.junit.runner;

import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTest;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class WebTestProperties
{
    private static TestMap associatedTests = new TestMap(); // Stores Tests, keyed by associated module.
    private static ModuleMap associatedModules = new ModuleMap();
    private static final List<String> installedModules = getInstalledModules();

    private static void loadTestProperties(Class testClass)
    {
        if (!WebTest.class.isAssignableFrom(testClass))
            return;

        try
        {
            WebTest test;
            Constructor<WebTest> c = testClass.getConstructor();
            test = c.newInstance();

            List<String> modules = test.getAssociatedModules();

            if (modules != null)
            {
                for (String module : modules)
                {
//                    if (!installedModules.contains(module))
//                        System.out.println("Module \"" + module + "\" specified in " + testClass.getSimpleName() + " not found.");
                    associatedTests.put(module, testClass);
                    associatedModules.put(testClass, module);
                }
            }
        }
        catch (Throwable e)
        {
            System.out.println("Error [" + testClass.getSimpleName() + "]: " + e);
        }
    }

    public static Collection<String> getAssociatedModules(Class test)
    {
        if (!associatedModules.containsKey(test))
        {
            loadTestProperties(test);
        }
        return associatedModules.getOrDefault(test, Collections.emptySet());
    }

    /**
     * This assumes that info for all relevant tests has already been stashed
     */
    public static Collection<Class> getAssociatedTests(String module)
    {
        return associatedTests.getOrDefault(module, Collections.emptySet());
    }

    // A simple MultiMap
    public static class TestMap extends CaseInsensitiveHashMap<Collection<Class>>
    {
        public Collection<Class> put(String key, Class clazz)
        {
            Collection<Class> collection = get(key);

            if (null == collection)
            {
                collection = new ArrayList<>();
                put(key, collection);
            }

            collection.add(clazz);
            return collection;
        }
    }

    // A simple MultiMap
    public static class ModuleMap extends HashMap<Class, Collection<String>>
    {
        public Collection<String> put(Class key, String module)
        {
            Collection<String> collection = get(key);

            if (null == collection)
            {
                collection = Collections.newSetFromMap(new org.labkey.remoteapi.collections.CaseInsensitiveHashMap<>());
                put(key, collection);
            }

            collection.add(module);
            return collection;
        }
    }

    private static List<String> getInstalledModules()
    {
        File modulesDir = new File(TestFileUtils.getDefaultDeployDir(), "modules");

        if (!modulesDir.exists())
            return Collections.emptyList();

        String[] moduleNames = modulesDir.list((dir, name) -> (new File(dir, name)).isDirectory());

        return Arrays.asList(moduleNames);
    }
}

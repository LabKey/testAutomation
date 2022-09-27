/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.fail;

/**
 * Utilities to compare JSON blobs.
 */
public class JSONHelper
{
    // json key elements to ignore during the comparison phase, these can be regular expressions
    static final Pattern[] GLOBALLY_IGNORED = {
            Pattern.compile("entityid", Pattern.CASE_INSENSITIVE),
            Pattern.compile("containerid", Pattern.CASE_INSENSITIVE),
            Pattern.compile("rowid", Pattern.CASE_INSENSITIVE),
            Pattern.compile("lsid", Pattern.CASE_INSENSITIVE),
            Pattern.compile("_labkeyurl_.*"),
            Pattern.compile("id", Pattern.CASE_INSENSITIVE),
            Pattern.compile("objectId", Pattern.CASE_INSENSITIVE),
            Pattern.compile("userId", Pattern.CASE_INSENSITIVE),
            Pattern.compile("groupId", Pattern.CASE_INSENSITIVE),
            Pattern.compile("message", Pattern.CASE_INSENSITIVE),
            Pattern.compile("created", Pattern.CASE_INSENSITIVE),
            Pattern.compile("FilePathRoot", Pattern.CASE_INSENSITIVE),
            Pattern.compile("displayName", Pattern.CASE_INSENSITIVE)
    };

    private final ArrayList<Pattern> _ignoredElements;

    public JSONHelper()
    {
        _ignoredElements = new ArrayList<>(Arrays.asList(GLOBALLY_IGNORED));
    }

    public JSONHelper(Pattern[] ignored)
    {
        this();
        if (ignored != null)
            _ignoredElements.addAll(Arrays.asList(ignored));
    }

    public void assertEquals(String msg, String expected, String actual)
    {
        JSONObject expectedJSON = new JSONObject(expected);
        JSONObject actualJSON = new JSONObject(actual);

        assertEquals(msg, expectedJSON, actualJSON);
    }

    public void assertEquals(String msg, JSONObject expected, JSONObject actual)
    {
        if (compareElement(expected, actual))
        {
            TestLogger.log("matched json");
        }
        else
        {
            String expectedString = expected.toString();
            String actualString = actual.toString();

            TestLogger.log("Expected:\n" + expectedString + "\n");
            TestLogger.log("Actual:\n" + actualString + "\n");

            String diff = Diff.diff(expectedString, actualString);
            fail(msg + "\n" + diff + "\n");
        }
    }

    public boolean compareMap(Map map1, Map map2, boolean fatal)
    {
        for (Object key : map1.keySet())
        {
            if (map2.containsKey(key))
            {
                if (!skipElement(String.valueOf(key)) && !compareElement(map1.get(key), map2.get(key), fatal))
                {
                    log("Error found in: " + key, fatal);
                    return false;
                }
            }
            else
            {
                log("Comparison of maps failed: could not find key: " + key, fatal);
                return false;
            }
        }
        return true;
    }

    public boolean compareList(List list1, List list2, boolean fatal)
    {
        if (list1.size() != list2.size())
        {
            log("Comparison of lists failed: sizes are different", fatal);
            return false;
        }

        // lists are not ordered
        for (int i=0; i < list1.size(); i++)
        {
            boolean matched = false;
            for (Object o : list2)
            {
                if (compareElement(list1.get(i), o, false))
                {
                    matched = true;
                    break;
                }
            }
            if (!matched)
            {
                log("Failed to match two specified lists. " + list1.get(i) + " was not found in list2.\nList 1: " +
                        list1 + "\nList 2: " + list2, fatal);
                return false;
            }
        }
        return true;
    }

    public boolean compareElement(Object o1, Object o2)
    {
        return compareElement(o1, o2, true);
    }

    private boolean compareElement(Object o1, Object o2, boolean fatal)
    {
        if (o1 instanceof Map)
            return compareMap((Map)o1, (Map)o2, fatal);
        if (o1 instanceof List)
            return compareList((List)o1, (List)o2, fatal);
        if (StringUtils.equals(String.valueOf(o1), String.valueOf(o2)))
            return true;

        log("Comparison of elements: " + o1 + " and: " + o2 + " failed", fatal);
        return false;
    }

    private void log(String msg, boolean fatal)
    {
        if (fatal)
            TestLogger.log(msg);
    }

    private boolean skipElement(String element)
    {
        for (Pattern ignore : _ignoredElements)
        {
            if (ignore.matcher(element).matches())
                return true;
        }
        return false;
    }
}

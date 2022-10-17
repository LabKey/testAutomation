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

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
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

    private final LinkedList<String> _currentPath = new LinkedList<>();

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
        if (compareMap(expected, actual, true))
        {
            TestLogger.log("matched json");
        }
        else
        {
            String expectedString = expected.toString(2);
            String actualString = actual.toString(2);

            TestLogger.log("Expected:\n" + expectedString + "\n");
            TestLogger.log("Actual:\n" + actualString + "\n");

            String diff = Diff.diff(expectedString, actualString);
            fail(msg + "\n" + diff + "\n");
        }
    }

    private boolean compareMap(JSONObject expected, JSONObject actual, boolean fatal)
    {
        for (String key : expected.keySet())
        {
            if (fatal)
            {
                _currentPath.add(key);
            }
            if (actual.has(key))
            {
                if (!skipElement(String.valueOf(key)) && !compareElement(expected.get(key), actual.get(key), fatal))
                {
                    return false;
                }
            }
            // JSONObject might omit entries with null values: https://github.com/stleary/JSON-java/issues/667
            else if (expected.get(key) != JSONObject.NULL)
            {
                log("Comparison of maps failed: could not find element: " + getPath(), fatal);
                return false;
            }
            if (fatal)
            {
                _currentPath.removeLast();
            }
        }
        return true;
    }

    private boolean compareArrays(JSONArray expected, JSONArray actual, boolean fatal)
    {
        if (expected.length() != actual.length())
        {
            log(String.format("Array size mismatch at %s. %s=/=%s", getPath(), expected.length(), actual.length()), fatal);
            return false;
        }

        if (expected.length() == 1)
        {
            if (fatal)
            {
                _currentPath.set(_currentPath.size() - 1, _currentPath.getLast() + "[0]");
            }
            return compareElement(expected.get(0), actual.get(0), fatal);
        }
        else
        {
            // lists are not ordered
            for (int i = 0; i < expected.length(); i++)
            {
                boolean matched = false;
                for (Object o : actual)
                {
                    if (compareElement(expected.get(i), o, false))
                    {
                        matched = true;
                        break;
                    }
                }
                if (!matched)
                {
                    log(String.format("Failed to match two specified lists at %s. " + expected.get(i) + " was not found in list2.\nList 1: " +
                            expected + "\nList 2: " + actual, getPath()), fatal);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean compareElement(Object expected, Object actual, boolean fatal)
    {
        if (expected instanceof JSONObject oExp && actual instanceof JSONObject oAct)
            return compareMap(oExp, oAct, fatal);
        if (expected instanceof JSONArray aExp && actual instanceof JSONArray aAct)
            return compareArrays(aExp, aAct, fatal);
        else
            return compareLeaves(expected, actual, fatal);

    }

    private boolean compareLeaves(Object expected, Object actual, boolean fatal)
    {
        if (expected instanceof JSONObject || actual instanceof JSONObject ||
                expected instanceof JSONArray || actual instanceof JSONArray)
        {
            log("Type mismatch at " + getPath() + ". expected: " + expected.getClass() + " but found:" + actual.getClass(), fatal);
            return false;
        }
        if (!Objects.equals(expected, actual))
        {
            log("Mismatch at " + getPath() + ". expected: " + expected + " but found:" + actual, fatal);
            return false;
        }
        return true;
    }

    @NotNull
    private String getPath()
    {
        return "'" + String.join(".", _currentPath) + "'";
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

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
import org.openqa.selenium.JavascriptException;

public class CodeMirrorHelper
{
    private final WebDriverWrapper wrapper;
    private final String editorId;

    public CodeMirrorHelper(WebDriverWrapper wrapper, String editorId)
    {
        this.wrapper = wrapper;
        this.editorId = editorId;
    }

    @Deprecated (since = "22.7")
    public CodeMirrorHelper(WebDriverWrapper wrapper)
    {
        this(wrapper, null);
    }

    public void setCodeMirrorValue(String value)
    {
        waitForCodeMirrorInstance(editorId);
        String script = getCodeMirrorPrefix() +
                "getCodeMirrorInstance(arguments[0]).setValue(arguments[1]);";
        wrapper.executeScript(script, editorId, value);
    }

    public String getCodeMirrorValue()
    {
        waitForCodeMirrorInstance(editorId);
        String script = getCodeMirrorPrefix() +
                "return getCodeMirrorInstance(arguments[0]).getValue();";
        return wrapper.executeScript(script, String.class, editorId);
    }

    public int getLineCount()
    {
        waitForCodeMirrorInstance(editorId);
        String script = getCodeMirrorPrefix() +
                "var cm = getCodeMirrorInstance(arguments[0]);\n" +
                "return cm.lineCount();";
        return wrapper.executeScript(script, Long.class, editorId).intValue();
    }

    @Deprecated (since = "22.7")
    public int getLineCount(String id)
    {
        return new CodeMirrorHelper(wrapper, id).getLineCount();
    }

    private void waitForCodeMirrorInstance(String id)
    {
        String script = getCodeMirrorPrefix() +
                "getCodeMirrorInstance(arguments[0]);";
        //noinspection ResultOfMethodCallIgnored
        WebDriverWrapper.waitFor(() -> {
                try
                {
                    wrapper.executeScript(script, id);
                    return true;
                }
                catch (JavascriptException retry)
                {
                    return false;
                }
        }, 10000); // No timout error. Let subsequent method throw error.
    }

    private String getCodeMirrorPrefix()
    {
        String prefix =
                "var getCodeMirrorInstance = function(id) {\n" +
                        "    try {\n" +
                        "        if (LABKEY.CodeMirror && LABKEY.CodeMirror[id]) {\n" +
                        "            return LABKEY.CodeMirror[id];\n" +
                        "        }\n" +
                        "        else {\n" +
                        "            throw 'Unable to find code mirror instance ' + id;\n" +
                        "        }\n" +
                        "    } catch (e) {\n" +
                        "        throw 'Error attempting to get code mirror instance: ' + e;\n" +
                        "    }\n" +
                        "};\n";
        return prefix;
    }
}

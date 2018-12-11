/*
 * Copyright (c) 2018 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebElement;

public class CodeMirrorHelper
{
    private final WebDriverWrapper wrapper;

    public CodeMirrorHelper(WebDriverWrapper wrapper)
    {
        this.wrapper = wrapper;
    }

    public void setCodeMirrorValue(String id, String value)
    {
        String script = getCodeMirrorPrefix() +
                "getCodeMirrorInstance(arguments[0]).setValue(arguments[1]);";
        wrapper.executeScript(script, id, value);
    }

    public String getCodeMirrorValue(String id)
    {
        String script = getCodeMirrorPrefix() +
                "return getCodeMirrorInstance(arguments[0]).getValue();";
        return wrapper.executeScript(script, String.class, id);
    }

    public int getLineCount(String id)
    {
        String script = getCodeMirrorPrefix() +
                "var cm = getCodeMirrorInstance(arguments[0]);\n" +
                "return cm.lineCount();";
        return wrapper.executeScript(script, Long.class, id).intValue();
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
                        "        throw 'Error attempting to get code mirror instance: ' + e.message;\n" +
                        "    }\n" +
                        "};\n";
        return prefix;
    }
}

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
        String script =
                "setCodeMirrorValue = function(id, value) {\n" +
                        "    try {\n" +
                        "        if (LABKEY.CodeMirror && LABKEY.CodeMirror[id]) {\n" +
                        "            var eal = LABKEY.CodeMirror[id];\n" +
                        "            eal.setValue(value);\n" +
                        "        }\n" +
                        "        else {\n" +
                        "            throw 'Unable to find code mirror instance.';\n" +
                        "        }\n" +
                        "    } catch (e) {\n" +
                        "        throw 'setCodeMirrorValue() threw an exception: ' + e.message;\n" +
                        "    }\n" +
                        "};\n" +
                        "setCodeMirrorValue(arguments[0], arguments[1]);";
        wrapper.executeScript(script, id, value);
    }

    public String getCodeMirrorValue(String id)
    {
        String script =
                "var getCodeMirrorValue = function(id) {\n" +
                        "    try {\n" +
                        "        if (LABKEY.CodeMirror && LABKEY.CodeMirror[id]) {\n" +
                        "            var eal = LABKEY.CodeMirror[id];\n" +
                        "            return eal.getValue();\n" +
                        "        }\n" +
                        "        else {\n" +
                        "            throw 'Unable to find code mirror instance.';\n" +
                        "        }\n" +
                        "    } catch (e) {\n" +
                        "        throw 'getCodeMirrorValue() threw an exception: ' + e.message;\n" +
                        "    }\n" +
                        "};\n" +
                        "return getCodeMirrorValue(arguments[0]);";
        return (String) wrapper.executeScript(script, id);
    }

    public int getLineCount()
    {
        Locator lastLineLoc = Locator.css(".CodeMirror-code > div:last-of-type .CodeMirror-linenumber");
        int lineCount = 0;
        int lastLineNum = Integer.parseInt(wrapper.getText(lastLineLoc));

        while (lineCount < lastLineNum)
        {
            lineCount = lastLineNum;
            WebElement codeEditorDiv = Locator.css(".CodeMirror-scroll").findElement(wrapper.getDriver());
            wrapper.executeScript("arguments[0].scrollTop = arguments[0].scrollHeight;", codeEditorDiv);
            lastLineNum = Integer.parseInt(wrapper.getText(lastLineLoc));
        }

        return lineCount;
    }
}

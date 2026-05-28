/*
 * Copyright (c) 2015-2026 LabKey Corporation
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
package org.labkey.test.pages;

import org.apache.tika.utils.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.util.EscapeUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

import static org.labkey.test.util.EscapeUtil.FORM_FIELD_PREFIX;

public class DatasetInsertPage extends InsertPage
{
    public DatasetInsertPage(WebDriver driver, String datasetName)
    {
        super(driver, "Insert " + datasetName);
    }

    public DatasetInsertPage(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected void waitForReady()
    {
        super.waitForReady();
        waitForElement(Locator.tag("*").attributeStartsWith("name", FORM_FIELD_PREFIX));
    }

    public void insert(Map<String, ?> values)
    {
        tryInsert(values);

        assertElementNotPresent(Locators.labkeyError);
    }

    public void insert(Map<String, ?> values, boolean b, String s)
    {
        tryInsert(values);

        assertElementNotPresent(Locators.labkeyError);
    }

    public void insertExpectingError(Map<String, ?> values)
    {
        insertExpectingError(values, null);
    }

    public void insertExpectingError(Map<String, ?> values, String errorMsg)
    {
        tryInsert(values);

        if (StringUtils.isBlank(errorMsg))
        {
            assertElementPresent(Locators.labkeyError);
        }
        else
        {
            assertTextPresent(errorMsg);
        }
    }

    private void tryInsert(Map<String, ?> values)
    {
        for (Map.Entry<String, ?> entry : values.entrySet())
        {
            WebElement fieldInput =  Locator.tag("*").attributeEndsWith("name", EscapeUtil.getFormFieldName(entry.getKey())).findElement(getDriver());
            String type = fieldInput.getAttribute("type");
            switch (type)
            {
                case "text":
                case "file":
                    setFormElement(fieldInput, entry.getValue().toString());
                    break;
                case "checkbox":
                    if (Boolean.valueOf(entry.getValue().toString()))
                        checkCheckbox(fieldInput);
                    else
                        uncheckCheckbox(fieldInput);
                    break;
                default:
                    String tag = fieldInput.getTagName();
                    switch (tag)
                    {
                        case "textarea":
                            setFormElementJS(fieldInput, entry.getValue().toString());
                            break;
                        case "select":
                            if (entry.getValue() instanceof List)
                            {
                                List<String> options = (List<String>) entry.getValue();
                                for (String option : options)
                                {
                                    selectOptionByText(fieldInput, option);
                                }
                                break;
                            }
                            selectOptionByText(fieldInput, entry.getValue().toString());
                            break;
                        default:
                            throw new IllegalArgumentException("Update " + getClass().getSimpleName() + "#insert() to support field: " + entry.getKey() + ", tag = " + tag + ", type = " + type);
                    }
            }
        }

        clickAndWait(Locator.lkButton("Submit"));
    }
}

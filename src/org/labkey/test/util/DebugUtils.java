/*
 * Copyright (c) 2016 LabKey Corporation
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

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.sleep;

public abstract class DebugUtils
{
    public static void flash(WebDriver driver, WebElement element, int count)
    {
        String originalStyle = element.getCssValue("style");
        for (int i = 0; i < count; i++) {
            setBorderStyle((JavascriptExecutor) driver, "2px solid yellow", element);
            sleep(75);
            setStyle((JavascriptExecutor) driver, originalStyle, element);
            if (i < count - 1) sleep(75);
        }
    }

    private static void setBorderStyle(JavascriptExecutor executor, String borderStyle, WebElement element) {
        executor.executeScript("arguments[0].style.border='" + borderStyle + "'", element);
    }

    private static void setStyle(JavascriptExecutor executor, String style, WebElement element) {
        executor.executeScript("arguments[0].style='" + style + "'", element);
    }
}

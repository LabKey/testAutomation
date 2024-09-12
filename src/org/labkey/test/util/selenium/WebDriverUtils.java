/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.util.selenium;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.intellij.lang.annotations.Language;
import org.openqa.selenium.Alert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.WrapsElement;

import java.util.List;

public abstract class WebDriverUtils
{
    /**
     * Modifier key that differs by OS. Has a couple of common uses:
     * <ul>
     * <li> Multi-select:
     * <pre><code>
     *    new Actions(getDriver())
     *        .keyDown(MODIFIER_KEY)
     *        .click(el1)
     *        .click(el2)
     *        .keyUp(MODIFIER_KEY)
     *        .perform();
     * </code></pre>
     * </li>
     * <li> Keyboard shortcuts (e.g. select-all):
     * <pre><code>
     *    new Actions(getDriver())
     *        .keyDown(MODIFIER_KEY)
     *        .sendKeys(input, "a")
     *        .keyUp(MODIFIER_KEY)
     *        .perform();
     * </code></pre>
     * </li>
     * </ul>
     */
    public static final Keys MODIFIER_KEY = SystemUtils.IS_OS_MAC ? Keys.COMMAND : Keys.CONTROL;

    /**
     * Extract a WebDriver instance from an arbitrarily wrapped object
     * @param peeling Object that wraps a WebDriver. Typically a Component, SearchContext, or WebElement
     * @return WebDriver instance or null if none is found
     */
    public static WebDriver extractWrappedDriver(Object peeling)
    {
        while (peeling instanceof WrapsElement wrapsElement)
        {
            peeling = wrapsElement.getWrappedElement();
        }
        while (peeling instanceof WrapsDriver wrapsDriver)
        {
            peeling = wrapsDriver.getWrappedDriver();
        }
        if (peeling instanceof WebDriver webDriver)
            return webDriver;
        else
            return null;
    }

    /**
     * {@link WebElement} cannot represent a text node. JavaScript can though, so we can use it to isolate the text
     * children of a WebElement and get their text.
     * Given a WebElement representing the following div:
     * <pre>{@code
     * <div>
     *     <span>A</span>
     *     B
     *     <button>C</button>
     *     D
     *     <span>D</span>
     * </div>
     * }</pre>
     * This method will return a list containing {@code ["B", "D"]}
     * @param element element to search
     * @return text from all child text nodes
     */
    @SuppressWarnings("unchecked")
    public static List<String> getTextNodesWithin(WebElement element)
    {
        JavascriptExecutor executor = (JavascriptExecutor) extractWrappedDriver(element);

        @Language("JavaScript")
        final String script = """
                var iterator = document.evaluate("text()", arguments[0]);
                var texts = [];

                let thisNode = iterator.iterateNext();

                while (thisNode) {
                    texts.push(thisNode.textContent);
                    thisNode = iterator.iterateNext();
                }
                return texts;
                """;

        List<Object> nodeTexts;
        try
        {
            nodeTexts = (List<Object>) executor.executeScript(script, element);
        }
        catch (WebDriverException retry)
        {
            // Script might throw if the document tree is modified during iteration. Retry once.
            nodeTexts = (List<Object>) executor.executeScript(script, element);
        }

        return nodeTexts.stream().map(t -> (String) t).toList();
    }

    /**
     * Gets text from the first text node under the specified WebElement.
     *
     * @see #getTextNodesWithin(WebElement)
     */
    public static String getTextNodeWithin(WebElement element)
    {
        List<String> textChildren = getTextNodesWithin(element);
        if (textChildren.isEmpty())
        {
            throw new NoSuchElementException("Element does not have any text children: " + element.toString());
        }
        return textChildren.get(0);
    }

    /**
     * Attempts to get alert text from an {@link UnhandledAlertException}. If exception does not supply the alert text,
     * attempt to get it from the alert directly (requires {@link org.openqa.selenium.UnexpectedAlertBehaviour#IGNORE}).
     * Either way, the alert will be dismissed if present.
     * @param uae UnhandledAlertException
     * @param driver WebDriver
     * @return Best attempt at alert text
     */
    public static String getUnhandledAlertText(UnhandledAlertException uae, WebDriver driver)
    {
        String alertText = StringUtils.trimToEmpty(uae.getAlertText());
        try
        {
            Alert alert = driver.switchTo().alert();
            if (alertText.isEmpty())
            {
                alertText = alert.getText();
            }
            alert.dismiss(); // Dismiss alert even if exception contains alert text
        }
        catch (NoAlertPresentException ignore) {}

        return StringUtils.isBlank(alertText) ? uae.getMessage() : alertText;
    }
}

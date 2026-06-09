/*
 * Copyright (c) 2025-2026 LabKey Corporation
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

import org.intellij.lang.annotations.Language;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static org.labkey.test.Locator.NBSP;

public abstract class WebElementUtils
{
    private WebElementUtils() {}

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
        JavascriptExecutor executor = (JavascriptExecutor) WebDriverUtils.extractWrappedDriver(element);

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

        return nodeTexts != null ? nodeTexts.stream().map(t -> (String) t).toList() : Collections.emptyList();
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
        return textChildren.getFirst();
    }

    /**
     * {@link WebElement#getText()} matches the browser's rendering, which collapses and trims whitespace.
     * If you need the actual text written by the server, the element's {@code textContent} property is unmodified.<br>
     * Given a WebElement representing the following div:
     * <pre>{@code
     * <div> three   spaces </div>
     * }</pre>
     * {@link WebElement#getText()} would return {@code "three spaces"} but this method will retain the extra spaces.
     * @param element element to inspect
     * @return textContent for the given element
     */
    public static String getTextContent(WebElement element)
    {
        return Optional.ofNullable(element.getDomProperty("textContent")).map(s -> s.replace(NBSP, " ")).orElse(null);
    }

    /**
     * Determines whether the specified element is visible. {@link WebElement#isDisplayed()} might return false if the
     * element is out the viewport and scrolling is disabled due to a modal dialog.<br>
     * TODO: Consider moving to {@link LazyWebElement#isDisplayed()}
     *
     * @param element element to inspect
     * @return true if the element is visible, false otherwise
     */
    public static boolean checkVisibility(WebElement element)
    {
        try
        {
            return element.isDisplayed() ||
                    Objects.requireNonNullElse(WebDriverUtils.getJavascriptExecutor(element)
                            .executeScript("return arguments[0].checkVisibility();", Boolean.class, element),
                            false);
        }
        catch (NoSuchElementException | StaleElementReferenceException e)
        {
            return false;
        }
    }

    /**
     * Convenience method to extract some attribute from a WebElement. Stale or missing elements will return a default.
     * @param element WebElement to inspect; may be stale or backed by a missing DOM node
     * @param mapper function applied to {@code element} to extract the desired value
     * @param defaultValue value returned when {@code element} is stale or missing
     * @return the mapped value, or {@code defaultValue} if the element is unavailable
     * @param <T> type of the extracted value
     */
    public static <T> T tryMapElement(WebElement element, Function<WebElement, T> mapper, T defaultValue)
    {
        try
        {
            return mapper.apply(element);
        }
        catch (NoSuchElementException | StaleElementReferenceException _) { }

        return defaultValue;
    }

    /**
     * Convenience overload of {@link #tryMapElement(WebElement, Function, Object)} that defaults to an empty string.
     * @param element WebElement to inspect; may be stale or backed by a missing DOM node
     * @param mapper function applied to {@code element} to extract a String value
     * @return the mapped value, or {@code ""} if the element is unavailable
     */
    public static String tryMapElement(WebElement element, Function<WebElement, String> mapper)
    {
        return tryMapElement(element, mapper, "");
    }
}

package org.labkey.test.util.selenium;

import org.intellij.lang.annotations.Language;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;

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
        return textChildren.get(0);
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
        return element.getDomProperty("textContent").replace(NBSP, " ");
    }
}

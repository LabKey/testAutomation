package org.labkey.test.util.selenium;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Locatable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScrollUtils
{
    private ScrollUtils() {}

    public static boolean scrollUnderStickyFormButtons(WebElement webElement)
    {
        WebDriver webDriver = extractWebDriver(webElement);
        Optional<WebElement> formButtons = Locator.css(".form-buttons").findOptionalElement(webDriver);

        if (formButtons.isPresent())
        {
            int elY = webElement.getLocation().getY();
            int height = webElement.getSize().getHeight();
            int bottom = elY + height;
            int formButtonsY = formButtons.get().getLocation().getY();

            // If the bottom of our element is past the top of the FormButtons element, then it's at least partially
            // obscured, so we should scroll the element into view.
            if (bottom > formButtonsY)
            {
                TestLogger.debug("Scrolled under sticky form buttons");
                scrollIntoView(webElement);
                return true;
            }
        }

        return false;
    }

    public static boolean scrollUnderFloatingHeader(WebElement webElement)
    {
        WebDriver webDriver = extractWebDriver(webElement);

        List<WebElement> floatingHeaders = Locator.findElements(webDriver,
                Locators.floatingHeaderContainer(),
                Locators.appFloatingHeader(),
                Locators.domainDesignerFloatingHeader(),
                DataRegionTable.Locators.floatingHeader().notHidden());

        int headerHeight = 0;
        for (WebElement floatingHeader : floatingHeaders)
        {
            headerHeight += floatingHeader.getSize().getHeight();
        }
        if (headerHeight > 0)
        {
            int elYInViewPort = webElement.getLocation().getY() - getWindowScrollY(webDriver).intValue();
            if (headerHeight > elYInViewPort)
            {
                TestLogger.debug("Scrolled under floating headers:\n" + floatingHeaders.stream().map(WebElement::toString).collect(Collectors.joining("\n")));
                ((Locatable) webElement).getCoordinates().inViewPort(); // 'inViewPort()' will scroll element into view
                return true;
            }
        }
        return false;
    }

    public static Long getWindowScrollY(WebDriver webDriver)
    {
        Number N = (Number) executeScript(webDriver, "return window.scrollY;");
        return null == N ? null : N.longValue();
    }

    public static WebElement scrollIntoView(WebElement webElement)
    {
        executeScript(webElement, "arguments[0].scrollIntoView();", webElement);
        return webElement;
    }

    public static WebElement scrollIntoView(WebElement webElement, Boolean alignToTop)
    {
        executeScript(webElement, "arguments[0].scrollIntoView(arguments[1]);", webElement, alignToTop);
        return webElement;
    }

    public static WebElement scrollIntoView(WebElement webElement, Alignment verticalAlignment, Alignment horizontalAlignment)
    {
        executeScript(webElement, "arguments[0].scrollIntoView({block: arguments[1], inline: arguments[2]});",
                webElement, verticalAlignment.toString(), horizontalAlignment.toString());
        return webElement;
    }

    public static WebElement scrollToMiddle(WebElement webElement)
    {
        return scrollIntoView(webElement, Alignment.center, Alignment.center);
    }

    public static void scrollBy(WebDriver webDriver, Integer x, Integer y)
    {
        executeScript(webDriver, "window.scrollBy(arguments[0], arguments[1]);", x, y);
    }

    public static void scrollTo(WebDriver webDriver, Integer x, Integer y)
    {
        executeScript(webDriver, "window.scrollTo(arguments[0], arguments[1]);", x, y);
    }

    private static Object executeScript(WebDriver _webDriver, @Language("JavaScript") String script, Object... args)
    {
        return ((JavascriptExecutor) _webDriver).executeScript(script, args);
    }

    private static Object executeScript(WebElement webElement, @Language("JavaScript") String script, Object... args)
    {
        return executeScript(extractWebDriver(webElement), script, args);
    }

    @NotNull
    private static WebDriver extractWebDriver(WebElement webElement)
    {
        return Objects.requireNonNull(WebDriverUtils.extractWrappedDriver(webElement));
    }

    public enum Alignment
    {
        start, center, end, nearest
    }
}

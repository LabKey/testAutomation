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

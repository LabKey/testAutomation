package org.labkey.test.util;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.sleep;

public abstract class DebugUtils
{
    public static void flash(WebDriver driver, WebElement element, int count)
    {
        String borderStyle  = element.getCssValue("border");
        for (int i = 0; i < count; i++) {
            changeBorder((JavascriptExecutor) driver, "2px solid yellow", element);
            sleep(75);
            changeBorder((JavascriptExecutor) driver, borderStyle, element);
            if (i < count - 1) sleep(75);
        }
    }

    private static void changeBorder(JavascriptExecutor executor, String style, WebElement element) {
        executor.executeScript("arguments[0].style.border='" + style + "'", element);
    }
}

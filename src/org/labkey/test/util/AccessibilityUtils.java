package org.labkey.test.util;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.selenium.AxeBuilder;
import org.labkey.test.components.Component;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.selenium.WebDriverUtils;
import org.openqa.selenium.WebDriver;

public class AccessibilityUtils
{
    public static void scanPage(WebDriver driver)
    {
        Results results = new AxeBuilder().analyze(driver);
        recordViolations(results, "page");
    }

    public static void scanPage(LabKeyPage<?> page)
    {
        Results results = new AxeBuilder().analyze(WebDriverUtils.extractWrappedDriver(page.getWrappedDriver()));
        recordViolations(results, page.getClass().getSimpleName());
    }

    public static void scanComponent(Component<?> component)
    {
        Results results = new AxeBuilder().analyze(WebDriverUtils.extractWrappedDriver(component.getComponentElement()), component.getComponentElement());
        recordViolations(results, component.getClass().getSimpleName());
    }

    private static void recordViolations(Results results, String component)
    {
        if (results.isErrored())
        {
            TestLogger.error("Accessibility violations found on " + component);
            results.getViolations().forEach(violation -> TestLogger.error(violation.getDescription()));
        }
    }
}

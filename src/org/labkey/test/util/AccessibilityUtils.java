package org.labkey.test.util;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.selenium.AxeBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.labkey.test.TestProperties;
import org.labkey.test.components.Component;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.selenium.WebDriverUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashSet;
import java.util.Set;

public class AccessibilityUtils
{
    private static final Set<String> actionsScanned = new HashSet<>();
    private static final Set<Class<?>> pagesScanned = new HashSet<>();
    private static final Set<Class<?>> componentsScanned = new HashSet<>();

    public static void scanPage(WebDriver driver)
    {
        String action = new Crawler.ControllerActionId(StringUtils.trimToEmpty(driver.getCurrentUrl())).toString();
        if (actionsScanned.add(action))
        {
            TestLogger.log("Scanning: " + action);
            Results results = getAnalyzer().analyze(driver);
            recordViolations(results, action);
        }
    }

    public static void scanPage(LabKeyPage<?> page)
    {
        if (pagesScanned.add(page.getClass()))
        {
            TestLogger.log("Scanning: " + page.getClass());
            Results results = getAnalyzer().analyze(WebDriverUtils.extractWrappedDriver(page.getWrappedDriver()));
            recordViolations(results, page.getClass().getSimpleName());
        }
    }

    public static void scanComponent(Component<?> component)
    {
        if (componentsScanned.add(component.getClass()))
        {
            TestLogger.log("Scanning: " + component.getClass());
            Results results = getAnalyzer().analyze(WebDriverUtils.extractWrappedDriver(component.getComponentElement()), component.getComponentElement());
            recordViolations(results, component.getClass().getSimpleName());
        }
    }

    private static @NonNull AxeBuilder getAnalyzer()
    {
        if (TestProperties.isAccessibilityCheckEnabled())
            return new AxeBuilder();
        else
            return NoOpAxeBuilder.get();
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

class NoOpAxeBuilder extends AxeBuilder
{
    private static final CachingSupplier<NoOpAxeBuilder> INSTANCE = new CachingSupplier<>(NoOpAxeBuilder::new);

    static NoOpAxeBuilder get()
    {
        return INSTANCE.get();
    }

    @Override
    public Results analyze(WebDriver webDriver, WebElement... context)
    {
        return new Results();
    }

    @Override
    public Results analyze(WebDriver webDriver)
    {
        return new Results();
    }

    @Override
    public Results analyze(WebDriver webDriver, boolean injectAxe)
    {
        return new Results();
    }
}

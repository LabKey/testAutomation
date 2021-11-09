package org.labkey.test;

import org.openqa.selenium.WebElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf;

public class UnexpectedNavigationWatcher implements WebDriverWrapper.PageLoadListener
{

    private static final UnexpectedNavigationWatcher INSTANCE = new UnexpectedNavigationWatcher();

    WebElement _prevPage = null;

    private UnexpectedNavigationWatcher()
    {
        // Singleton
    }

    public static UnexpectedNavigationWatcher get()
    {
        return INSTANCE;
    }

    @Override
    public void beforePageLoad(WebDriverWrapper wrapper)
    {
        if (_prevPage != null)
        {
            if (stalenessOf(_prevPage).apply(wrapper.getDriver()))
            {
                BaseWebDriverTest.getCurrentTest().checker().error("Unexpected Navigation. Update test to use 'doAndWaitForPageToLoad'");
            }
        }
    }

    @Override
    public void afterPageLoad(WebDriverWrapper wrapper)
    {
        _prevPage = wrapper.waitForElement(Locators.documentRoot);
    }
}

package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

// TODO: Tons of missing functionality
// TODO: Much ListHelper functionality belongs here
// TODO: Create subclasses for particular domain editors (e.g. Metadata)
public abstract class DomainEditorHelper
{
    BaseWebDriverTest _test;

    private static final String ROW_HIGHLIGHT = "238";

    public DomainEditorHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public abstract void waitForReady();

    public void selectField(int index)
    {
        selectField(Locator.tagWithClass("tr", "editor-field-row").withDescendant(Locator.xpath("td/img").withAttribute("id", "partstatus_" + index)));
    }

    private void selectField(final Locator.XPathLocator rowLocator)
    {
        _test.click(rowLocator.append("/td")); // First td (status) should be safe to click

        _test.waitForElement(rowLocator.withClass("selected-field-row"));
    }

    public void clickTab(String tab)
    {
        _test.click(Locator.tagWithAttribute("ul", "role", "tablist").append("/li").withText(tab));
        _test.waitForElement(Locator.tagWithClass("li", "x-tab-strip-active").withText(tab));
    }

    public static class Locators
    {
        public static Locator.XPathLocator fieldPanel(String panelTitle)
        {
            return Locator.tagWithClass("table", "labkey-wp").withPredicate(PortalHelper.Locators.webPartTitle(panelTitle));
        }
    }
}

package org.labkey.test.components.domain;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * A simple domain designer with a properties panel and a single field panel.
 */
public abstract class DomainDesigner<EC extends DomainDesigner<?>.ElementCache> extends BaseDomainDesigner<EC>
{
    public DomainDesigner(WebDriver driver)
    {
        super(driver);
    }

    protected void expandPropertiesPanel()
    {
        elementCache().propertiesPanel.expand();
    }

    public DomainFormPanel getFieldsPanel()
    {
        return elementCache().fieldsPanel.expand();
    }

    @Override
    public Object clickSave()
    {
        try
        {
            return super.clickSave();
        }
        catch (TestTimeoutException ex)
        {
            BaseWebDriverTest.getCurrentTest().getArtifactCollector().dumpPageSnapshot("domainSave");
            TestLogger.log("Failed to save domain. Opening properties panel for screenshot.");
            expandPropertiesPanel();
            throw ex;
        }
    }

    public class ElementCache extends BaseDomainDesigner<?>.ElementCache
    {
        protected final DomainPanel<?, ?> propertiesPanel = new DomainPanel.DomainPanelFinder(getDriver()).index(0)
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        protected final DomainFormPanel fieldsPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver())
                .index(getFieldPanelIndex()).timeout(1000).findWhenNeeded();

        protected int getFieldPanelIndex()
        {
            return 1;
        }
    }
}

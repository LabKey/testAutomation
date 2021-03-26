package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.components.domain.DomainPanel;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class JobTemplateDetailsPanel extends DomainPanel
{

    protected JobTemplateDetailsPanel(WebElement element, WebDriver driver)
    {
       super(element, driver);
    }

    @Override
    protected JobTemplateDetailsPanel getThis() {return this;}

    public JobTemplateDetailsPanel setName(String name)
    {
        expand();
        elementCache().jobNameInput.set(name);
        return this;
    }

    public String getName()
    {
        expand();
        return elementCache().jobNameInput.get();
    }

    public JobTemplateDetailsPanel setDescription(String description)
    {
        expand();
        elementCache().descriptionInput.set(description);
        return this;
    }

    public String getDescription()
    {
        return elementCache().descriptionInput.get();
    }


    @Override
    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends DomainPanel.ElementCache
    {
        Input jobNameInput = Input.Input(Locator.name("name"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(panelBody);
        Input descriptionInput = Input.Input(Locator.name("description"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(panelBody);
    }

    public static class JobTemplateDetailsPanelFinder extends BaseDomainPanelFinder<JobTemplateDetailsPanel, JobTemplateDetailsPanel.JobTemplateDetailsPanelFinder>
    {
        public JobTemplateDetailsPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected JobTemplateDetailsPanel construct(WebElement el, WebDriver driver)
        {
            return new JobTemplateDetailsPanel(el, driver);
        }
    }
}

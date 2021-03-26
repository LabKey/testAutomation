package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.components.domain.DomainPanel;
import org.labkey.test.components.ui.files.FileUploadPanel;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;

public class JobAttachFilesPanel extends DomainPanel
{

    protected JobAttachFilesPanel(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    @Override
    protected JobAttachFilesPanel getThis()
    {
        return this;
    }

    public JobAttachFilesPanel addFile(File file)
    {
        elementCache()._uploadPanel.uploadFile(file);
        return this;
    }

    public List<String> getAttachedFiles()
    {
        return elementCache()._uploadPanel.attachedFiles();
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
        public FileUploadPanel _uploadPanel = new FileUploadPanel.FileUploadPanelFinder(getDriver())
                .findWhenNeeded(panelBody);
    }

    /**
     * TODO:
     * For components that are, essentially, singletons on a page, you may want to omit this Finder class
     * Note that even in that case, a Finder class can be useful for lazily finding components
     * Usage: 'new Component.ComponentFinder(getDriver()).withTitle("title").findWhenNeeded();'
     */
    public static class JobAttachFilesPanelFinder extends WebDriverComponentFinder<JobAttachFilesPanel, JobAttachFilesPanelFinder>
    {
        // TODO: This locator should find all instances of this component
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "my-component");
        private String _title = null;

        public JobAttachFilesPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public JobAttachFilesPanelFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected JobAttachFilesPanel construct(WebElement el, WebDriver driver)
        {
            return new JobAttachFilesPanel(el, driver);
        }

        /**
         * TODO:
         * Add methods and fields, as appropriate, to build a Locator that will find the element(s)
         * that this component represents
         */
        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withAttribute("title", _title);
            else
                return _baseLocator;
        }
    }
}

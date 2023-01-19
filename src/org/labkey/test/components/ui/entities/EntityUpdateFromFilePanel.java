package org.labkey.test.components.ui.entities;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.grids.ResponsiveGrid;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

public class EntityUpdateFromFilePanel extends EntityInsertPanel
{
    public EntityUpdateFromFilePanel(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public ResponsiveGrid uploadFileExpectingPreview(File file, boolean allowMerge)
    {
        var panel = uploadFile(file, allowMerge);
        return new ResponsiveGrid.ResponsiveGridFinder(getDriver()).waitFor(panel);
    }

    public EntityInsertPanel uploadFile(File file, boolean allowMerge)
    {
        var panel = showFileUpload();
        panel.setMergeData(allowMerge);
        panel.fileUploadPanel().uploadFile(file);
        return panel;
    }

    public static class EntityUpdateFromFilePanelFinder extends WebDriverComponent.WebDriverComponentFinder<EntityUpdateFromFilePanel, EntityUpdateFromFilePanel.EntityUpdateFromFilePanelFinder>
    {
        private final Locator _locator;

        public EntityUpdateFromFilePanelFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locator.tagWithClass("div", "panel").child(Locator.tagWithClass("div", "panel-body"));
        }

        @Override
        protected EntityUpdateFromFilePanel construct(WebElement element, WebDriver driver)
        {
            return new EntityUpdateFromFilePanel(element, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}

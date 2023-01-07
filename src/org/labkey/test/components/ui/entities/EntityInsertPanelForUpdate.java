package org.labkey.test.components.ui.entities;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.grids.ResponsiveGrid;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

public class EntityInsertPanelForUpdate extends EntityInsertPanel
{
    public EntityInsertPanelForUpdate(WebElement element, WebDriver driver)
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
        panel.setUpdateDataForFileUpload(allowMerge);
        panel.fileUploadPanel().uploadFile(file);
        return panel;
    }

    public static class EntityInsertPanelForUpdateFinder extends WebDriverComponent.WebDriverComponentFinder<EntityInsertPanelForUpdate, EntityInsertPanelForUpdate.EntityInsertPanelForUpdateFinder>
    {
        private final Locator _locator;

        public EntityInsertPanelForUpdateFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locator.tagWithClass("div", "panel").child(Locator.tagWithClass("div", "panel-body"));
        }

        @Override
        protected EntityInsertPanelForUpdate construct(WebElement element, WebDriver driver)
        {
            return new EntityInsertPanelForUpdate(element, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}

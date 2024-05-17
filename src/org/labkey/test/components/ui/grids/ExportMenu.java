package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.react.MultiMenu;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

/**
 * Wraps `packages/components/src/public/QueryModel/ExportMenu.tsx`
 */
public class ExportMenu extends WebDriverComponent<Component.ElementCache>
{
    private final BootstrapMenu _menu;
    private final WebDriver _driver;

    protected ExportMenu(BootstrapMenu menu, WebDriver driver)
    {
        _menu = menu;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _menu.getComponentElement();
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public static SimpleWebDriverComponentFinder<ExportMenu> finder(WebDriver driver)
    {
        return new MultiMenu.MultiMenuFinder(driver).withButtonIcon("fa-download")
                .wrap((el, wd) -> new ExportMenu(new BootstrapMenu(wd, el), driver));
    }

    public File exportData(GridBar.ExportType exportType)
    {
        WebElement exportButton = getExportMenuItem(exportType);
        return getWrapper().doAndWaitForDownload(exportButton::click);
    }


    public File exportData(GridBar.ExportType exportType, int index)
    {
        WebElement exportButton = getExportMenuItem(exportType, index);
        return getWrapper().doAndWaitForDownload(exportButton::click);
    }

    public TabSelectionExportDialog openExcelTabsModal()
    {
        WebElement exportButton = getExportMenuItem(GridBar.ExportType.EXCEL);
        exportButton.click();

        return new TabSelectionExportDialog(this.getDriver());
    }

    private WebElement getExportMenuItem(GridBar.ExportType exportType)
    {
        _menu.expand();
        return Locator.css("span.export-menu-icon").withClass(exportType.buttonCssClass()).findElement(this);
    }

    private WebElement getExportMenuItem(GridBar.ExportType exportType, int index)
    {
        _menu.expand();
        return Locator.css("span.export-menu-icon").withClass(exportType.buttonCssClass()).findElements(this).get(index);
    }
}

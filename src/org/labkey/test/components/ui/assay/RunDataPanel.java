package org.labkey.test.components.ui.assay;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.ui.entities.EntityBulkInsertDialog;
import org.labkey.test.components.ui.entities.EntityInsertPanel;
import org.labkey.test.components.ui.files.FileUploadPanel;
import org.labkey.test.components.ui.grids.EditableGrid;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.components.html.Input.Input;

/**
 * Exercises behavior for /internal/components/assay/RunDataPanel.tsx
 */
public class RunDataPanel extends WebDriverComponent<RunDataPanel.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected RunDataPanel(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    private void waitForLoaded()
    {
        WebDriverWrapper.waitFor(()-> {
            try
            {
                return  isGridVisible() ||          // when uploading assay data there is no target select
                        isFileUploadVisible() ;
            }catch (NoSuchElementException nse)
            {
                return false;
            }
        }, "The insert panel did not become loaded", WAIT_FOR_JAVASCRIPT);
    }

    // grid mode

    public RunDataPanel showGrid()
    {
        if (!isGridVisible())
        {
            selectMode(PanelMode.EnterDataIntoGrid);
            clearElementCache();
            WebDriverWrapper.waitFor(() -> isGridVisible(),
                    "the grid did bot become visible", 2000);
        }
        return this;
    }

    public boolean isGridVisible()
    {
        var optionalGrid = optionalGrid();
        return optionalGrid.isPresent() && optionalGrid.get().isDisplayed();
    }

    private Optional<EditableGrid> optionalGrid()
    {
        return new EditableGrid.EditableGridFinder(_driver).findOptional(this);
    }

    public EditableGrid getEditableGrid()
    {
        showGrid();
        return elementCache().grid;
    }

    public EntityBulkInsertDialog clickBulkInsert()
    {
        showGrid();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().bulkInsertBtn));
        elementCache().bulkInsertBtn.click();
        return new EntityBulkInsertDialog(getDriver());
    }

    public boolean isBulkInsertVisible()
    {
        return mode().equals("Enter Data Into Grid") &&
                elementCache().bulkInsertBtnLoc.existsIn(this) &&
                isElementVisible(elementCache().bulkInsertBtn);
    }

    public RunDataPanel clickAddRows()
    {
        showGrid();
        elementCache().addRowsButton.click();
        return this;
    }

    // file upload mode

    public FileUploadPanel showFileUpload()
    {
        if (!isFileUploadVisible())
        {
            selectMode(PanelMode.UploadFiles);
            WebDriverWrapper.waitFor(()-> isFileUploadVisible(),
                    "the file upload panel did bot become visible", 2000);
        }
        return fileUploadPanel();
    }

    public FileUploadPanel getFileUploadPanel()
    {
        showFileUpload();
        return fileUploadPanel();
    }

    public boolean isFileUploadVisible()
    {
        return mode().equals("Upload Files") &&
                optionalFileUploadPanel().isPresent() &&
                isElementVisible(fileUploadPanel().getComponentElement());
    }

    private FileUploadPanel fileUploadPanel()
    {
        return new FileUploadPanel.FileUploadPanelFinder(_driver).timeout(WAIT_FOR_JAVASCRIPT).waitFor(this);
    }

    private Optional<FileUploadPanel> optionalFileUploadPanel()
    {
        return new FileUploadPanel.FileUploadPanelFinder(getDriver()).findOptional();
    }

    // mode select behavior

    /**
     * finds the mode select tabs, to switch between grid input and file upload
     * @param text
     * @return
     */
    private Locator.XPathLocator modeSelectListItem(String text)
    {
        return Locator.tagWithClass("li", "list-group-item").withText(text);
    }

    private String mode()
    {
        return Locator.tagWithClass("li", "list-group-item").withClass("active").findElement(this).getText();
    }

    public RunDataPanel selectMode(PanelMode mode)
    {
        if (!mode().equals(mode.getText()))
        {
            var toggle = modeSelectListItem(mode.getText()).findElement(this);
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(toggle));
            toggle.click();
            WebDriverWrapper.waitFor(()-> mode().equals(mode.getText()),
                    "did not select the expected mode", 1500);
            clearElementCache();
        }
        return this;
    }

    protected boolean isElementVisible(WebElement element)
    {
        try
        {
            return element.isDisplayed();
        }
        catch(NoSuchElementException nse)
        {
            return false;
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public ElementCache()
        {
            waitForLoaded();
        }

        Locator deleteRowsBtnLoc = Locator.XPathLocator.union(
                Locator.button("Delete rows"),
                Locator.buttonContainingText("Remove"));
        Locator bulkInsertBtnLoc = Locator.button("Bulk Insert");
        Locator bulkUpdateBtnLoc = Locator.button("Bulk Update");
        Locator runDataTextArea = Locator.textarea("rundata");

        WebElement bulkInsertBtn = bulkInsertBtnLoc.findWhenNeeded(this).withTimeout(2000);
        WebElement bulkUpdateBtn = bulkUpdateBtnLoc.findWhenNeeded(this).withTimeout(2000);
        WebElement deleteRowsBtn = deleteRowsBtnLoc.findWhenNeeded(this).withTimeout(2000);

        WebElement addRowsTxtBox = Locator.tagWithName("input", "addCount").findWhenNeeded(this);
        WebElement addRowsButton = Locator.buttonContainingText("Add").findWhenNeeded(this);

        EditableGrid grid = new EditableGrid.EditableGridFinder(_driver).timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded();
    }

    public static class RunDataPanelFinder extends WebDriverComponentFinder<RunDataPanel, RunDataPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "panel")
                .child(Locator.tagWithClass("div", "panel-body"));
        private String _title;

        public RunDataPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public RunDataPanelFinder inPanelWithTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected RunDataPanel construct(WebElement el, WebDriver driver)
        {
            return new RunDataPanel(el, driver);
        }


        @Override
        protected Locator locator()
        {
            if (_title != null)
                return Locator.tagWithClass("div", "panel")
                        .withChild(Locator.tagWithClass("div", "panel-heading").withText(_title))
                        .child(Locator.tagWithClass("div", "panel-body"));
            else
                return _baseLocator;
        }
    }

    public enum PanelMode
    {
        UploadFiles("Import Data from Files"),
        EnterDataIntoGrid("Enter Data into Grid");

        private final String _text;
        PanelMode(String text)
        {
            _text = text;
        }
        public String getText()
        {
            return _text;
        }
    }
}

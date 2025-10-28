package org.labkey.test.components.ui.files;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.labkey.test.WebDriverWrapper.waitFor;

public class ManageImportTemplatesDialog extends ModalDialog
{
    public ManageImportTemplatesDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver).withTitle("Import Templates").waitFor().getComponentElement(), driver);
    }

    public boolean isAddButtonEnabled()
    {
        // If element is enabled the class attribute not contain the word 'disabled'.
        return !elementCache().addTemplateRow.getAttribute("class").toLowerCase().contains("disabled");
    }

    public int templateRowCount()
    {
        return elementCache().templateRows().size();
    }

    public WebElement addNewTemplateRow()
    {
        int previousRowCount = templateRowCount();
        elementCache().addTemplateRow.click();

        waitFor(() -> templateRowCount() > previousRowCount,
                "Unable to add a new template.",
                1_000);

        return elementCache().templateRow(previousRowCount);
    }

    public List<String> getTemplateNames()
    {
        List<String> names = new ArrayList<>();
        List<WebElement> rows = elementCache().templateRows();
        for (int i = 0; i < rows.size(); i++)
        {
            WebElement row = rows.get(i);
            if (i == 0)
                names.add(Locator.tagWithClass("div", "col-xs-6").findElement(row).getText());
            else
            {
                Input templateNameInput = Input.Input(Locator.byClass("form-control"), getDriver()).find(row);
                names.add(templateNameInput.getValue());
            }
        }

        return names;
    }

    public List<String> getTemplateFileNames()
    {
        List<String> names = new ArrayList<>();
        List<WebElement> rows = elementCache().templateRows();
        for (int i = 0; i < rows.size(); i++)
        {
            WebElement row = rows.get(i);
            if (i == 0)
                names.add(getDefaultTemplateFileName());
            else
                names.add(Locator.tagWithClass("div", "attachment-card__name").findElement(row).getText());
        }

        return names;
    }

    public void setTemplateName(int ind, String label)
    {
        WebElement row = elementCache().templateRow(ind);
        Input templateNameInput = Input.Input(Locator.byClass("form-control"), getDriver()).find(row);
        templateNameInput.set(label);
        row.click(); // onBlur
    }

    public void deleteCustomTemplateRow(int rowInd)
    {
        int previousRowCount = templateRowCount();
        WebElement row = elementCache().templateRow(rowInd);

        WebElement icon = Locator.tagWithClass("span", "import-template-delete-icon").findElement(row);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(icon));
        icon.click();

        waitFor(() -> templateRowCount() < previousRowCount,
                String.format("Unable to delete a template at row index: %d. Previous row count: %d ", rowInd, previousRowCount),
                1_000);
    }

    public void doBlur()
    {
        WebElement row = elementCache().templateRow(0);
        row.click();
    }

    public FileUploadField getFileUpload(int rowInd)
    {
        WebElement row = elementCache().templateRow(rowInd);
        return new FileUploadField(Locator.tagWithClass("div", "col-xs-5").findElement(row), getDriver());
    }

    public void uploadFile(int rowInd, File file)
    {
        FileUploadField fileUpload = getFileUpload(rowInd);
        fileUpload.setFile(file);
    }

    public WebElement getTemplateRowByName(String templateName)
    {
        List<WebElement> rows = elementCache().templateRows();
        if (templateName.equalsIgnoreCase("default template"))
            return rows.get(0);

        for (int i = 1; i < rows.size(); i++)
        {
            WebElement row = rows.get(i);
            Input templateNameInput = Input.Input(Locator.byClass("form-control"), getDriver()).find(row);
            if (templateNameInput.get().equalsIgnoreCase(templateName))
                return row;
        }

        return null;
    }

    public boolean hasTemplateNameError(int rowInd)
    {
        return getTemplateNameError(rowInd) != null;
    }

    public String getTemplateNameError(int rowInd)
    {
        WebElement row = elementCache().templateRows().get(rowInd);
        var rowError = Locator.tagWithClass("div", "error-msg").findElementOrNull(row);
        if (rowError != null)
            return rowError.getText();
        return null;
    }

    public File downloadDefaultTemplateFile()
    {
        return getWrapper().doAndWaitForDownload(()->{
            elementCache().defaultTemplateFileLink.click();
        });
    }

    public String getDefaultTemplateFileName()
    {
        return elementCache().defaultTemplateFileLink.getText();
    }

    public String getTemplateFileName(String templateName)
    {
        WebElement row = getTemplateRowByName(templateName);
        return Locator.tagWithClass("div", "attachment-card__name").findElement(row).getText();
    }

    public File downloadTemplateFile(String templateName)
    {
        if (templateName.equalsIgnoreCase("default template"))
            return downloadDefaultTemplateFile();

        AttachmentCard attachmentCard = new AttachmentCard.FileAttachmentCardFinder(getDriver())
                .withFileName(getTemplateFileName(templateName)).find(getComponentElement());
        return attachmentCard.clickDownload();
    }

    public boolean canSave()
    {
        return isDismissEnabled("Save");
    }

    public void clickSave()
    {
        dismiss("Save");
    }

    public void clickCancel()
    {
        dismiss("Cancel");
    }

    public String clickSaveExpectError()
    {
        Locators.dismissButton("Save").findElement(getComponentElement()).click();
        return BootstrapLocators.errorBanner
                .findWhenNeeded(this).withTimeout(5_000)
                .getText();
    }

    @Override
    protected ManageImportTemplatesDialog.ElementCache newElementCache()
    {
        return new ManageImportTemplatesDialog.ElementCache();
    }


    @Override
    protected ManageImportTemplatesDialog.ElementCache elementCache()
    {
        return (ManageImportTemplatesDialog.ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        WebElement addTemplateRow = Locator.tagWithClassContaining("span", "container--action-button")
                .withText("Add a Template").findWhenNeeded(getComponentElement());

        WebElement defaultTemplateFileLink = Locator.tagWithClass("span", "clickable-text").findWhenNeeded(getComponentElement());

        public List<WebElement> templateRows()
        {
            return Locator.tagWithClass("div", "file-listing-row--container").findElements(getComponentElement());
        }

        public WebElement templateRow(int index)
        {
            return templateRows().get(index);
        }
    }
}

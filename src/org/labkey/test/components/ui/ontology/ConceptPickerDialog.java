package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.ui.ontology.OntologyTreeSearch;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class ConceptPickerDialog extends ModalDialog
{
    protected ConceptPickerDialog(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public ConceptPickerDialog(ModalDialogFinder finder)
    {
        super(finder);
    }

    // optional ontology select, in case there are multiple ontologies
    // searchPath
    // getPathInformation

    public ConceptPickerDialog searchConcept(String conceptSearchExpression, String code)
    {
        elementCache().searchBox.selectItemWithCode(conceptSearchExpression, code);
        return this;
    }



    public void clickApply()
    {
        dismiss("Apply");
    }

    @Override
    protected ModalDialog.ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        final OntologyTreeSearch searchBox = new OntologyTreeSearch.OntologyTreeSearchFinder(getDriver())
                .findWhenNeeded(this);

        final OntologyTreePanel treePanel = new OntologyTreePanel.OntologyTreePanelFinder(getDriver())
                .findWhenNeeded(this);
        // tree control
        // tab views
        // cancel/apply buttons

//        final WebElement fileTreeContainer = Locator.tagWithClass("div", "filetree-container")
//                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
//        final WebElement infoTabsContainer = Locator.id("concept-information-tabs")
//                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
    }


}

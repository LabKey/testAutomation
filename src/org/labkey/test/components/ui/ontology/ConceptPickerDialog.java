package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.ui.ontology.OntologyTreeSearch;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

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

    /**
     * uses the search bar to select an item in the ontology tree
     * @param conceptSearchExpression an expression close enough to the target node to hit
     * @param code  the intended concept code
     * @return the current dialog
     */
    public ConceptPickerDialog searchConcept(String conceptSearchExpression, String code)
    {
        elementCache().searchBox.selectItemWithCode(conceptSearchExpression, code);
        return this;
    }

    /**
     * uses the information tabs to get the name of the selected concept
     * @return the contents of the 'title' element on the overview tab panel
     */
    public String getSelectedConcept()
    {
        return elementCache().infoTabs.getTitle();
    }

    /**
     * uses the information tabs to obtain the concept code
     * @return the contents of the 'code' span on the overview tab panel
     */
    public String getSelectedConceptCode()
    {
        return elementCache().infoTabs.getCode();
    }

    /**
     * uses the information tabs to obtain the parts of the current-selected path
     * @return contents of spans in the pathContainer
     */
    public List<String> getSelectedConceptPath()
    {
        return elementCache().infoTabs.getSelectedPath();
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

        final ConceptInfoTabs infoTabs = new ConceptInfoTabs.ConceptInfoTabsFinder(getDriver())
                .findWhenNeeded(this);
    }


}

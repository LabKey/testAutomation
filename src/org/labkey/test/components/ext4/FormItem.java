package org.labkey.test.components.ext4;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.openqa.selenium.SearchContext;

public abstract class FormItem extends Component
{
    public abstract static class FormItemFinder<C extends FormItem, F extends FormItemFinder<C, F>> extends ComponentFinder<SearchContext, C, F>
    {
        private String labelText = "";
        private boolean partialText = true;

        public F withLabel(String text)
        {
            this.labelText = text;
            partialText = false;
            return (F)this;
        }

        public F withLabelContaining(String text)
        {
            this.labelText = text;
            partialText = true;
            return (F)this;
        }

        protected Locator locator()
        {
            return itemLoc().withPredicate(
                    partialText
                            ? labelLoc().containing(labelText)
                            : labelLoc().withText(labelText));
        }

        protected abstract Locator.XPathLocator itemLoc();

        protected Locator.XPathLocator labelLoc()
        {
            return Locator.xpath("(../label|../../td/label)"); // Slightly different DOM structure across versions of Ext4
        }
    }
}

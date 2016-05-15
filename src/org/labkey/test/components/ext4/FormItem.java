package org.labkey.test.components.ext4;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public abstract class FormItem extends Component
{
    public abstract static class FormItemBuilder<T extends FormItem>
    {
        private String labelText = "";
        private boolean partialText = true;
        private int index = 0;

        public FormItemBuilder<T> withLabel(String text)
        {
            this.labelText = text;
            partialText = false;
            return this;
        }

        public FormItemBuilder<T> withLabelContaining(String text)
        {
            this.labelText = text;
            partialText = true;
            return this;
        }

        public FormItemBuilder<T> index(int index)
        {
            this.index = index;
            return this;
        }

        public T build(SearchContext context)
        {
            return doBuild(buildLocator().findElement(context));
        }

        public T buildLazy(SearchContext context)
        {
            return doBuild(new LazyWebElement(buildLocator(), context));
        }

        protected abstract T doBuild(WebElement el);

        protected Locator buildLocator()
        {
            return itemLoc().withPredicate(partialText ? labelLoc().containing(labelText) : labelLoc().withText(labelText)).index(index);
        }

        protected Locator.XPathLocator itemLoc()
        {
            return Locator.tagWithClass("input", Ext4Helper.getCssPrefix() + "form-checkbox");
        }

        protected Locator.XPathLocator labelLoc()
        {
            return Locator.xpath("(../label|../../td/label)"); // Slightly different DOM structure for versions of Ext4
        }
    }
}

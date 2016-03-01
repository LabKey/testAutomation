/*
 * Copyright (c) 2007-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.test;

import com.google.common.base.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class Locator
{
    protected String _loc;
    protected Integer _index;
    protected String _contains;
    protected String _text;

    // XPATH fragments
    public static final String HIDDEN = "ancestor-or-self::*[contains(@style,'display: none') or contains(@style,'visibility: hidden') or contains(@class, 'x-hide-display') or contains(@class, 'x4-hide-offsets') or contains(@class, 'x-hide-offsets')] or (@type = 'hidden')";
    public static final String NOT_HIDDEN = "not(" + HIDDEN + ")";
    public static final String ENABLED = "not(ancestor-or-self::*[contains(@class, 'disabled')])";

    protected Locator(String loc)
    {
        _loc = loc;
    }

    private Locator(String loc, Integer index, String contains, String text)
    {
        _loc = loc;
        _index = index;
        _contains = contains;
        _text = text == null ? null : text.trim();
    }

    /**
     * Wait for a single element located by any one of the provided Locators
     * @return The first element found
     */
    public static WebElement waitForAnyElement(FluentWait<? extends SearchContext> wait, final Locator... locators)
    {
        return wait.until(new Function<SearchContext, WebElement>()
        {
            @Override
            public WebElement apply(SearchContext context)
            {
                for (Locator loc : locators)
                {
                    List<WebElement> els;
                    els = loc.findElements(context);
                    if (els.size() > 0)
                        return els.get(0);
                }
                return null;
            }
        });

    }

    /**
     * Wait for elements located by any one of the provided Locators
     * @return All elements matching any of the provided Locators
     */
    public static List<WebElement> waitForAnyElements(FluentWait<? extends SearchContext> wait, final Locator... locators)
    {
        return wait.until(new Function<SearchContext, List<WebElement>>()
        {
            @Override
            public List<WebElement> apply(SearchContext context)
            {
                List<WebElement> els = new ArrayList<>();
                for (Locator loc : locators)
                {
                    els.addAll(loc.findElements(context));
                }

                if (els.size() > 0)
                    return els;
                else
                    return null;
            }
        });

    }

    public abstract Locator containing(String contains);

    public abstract Locator withText(String text);

    /**
     * Locate the nth element matched by the selector. Can only find a single element.
     * @param index zero-based index of desired element
     * @return Locator for the element
     */
    public abstract Locator index(Integer index);

    public abstract String toString();

    public String getLocatorString()
    {
        return _loc;
    }

    public String getLoggableDescription()
    {
        return toString() +
            (_index == null ? "" : "\nIndex: " + _index) +
            (_contains == null ? "" : "\nContaining: " + _contains) +
            (_text == null ? "" : "\nWith Text: " + _text);
    }

    public abstract By toBy();

    public WebElement findElement(SearchContext context)
    {
        List<WebElement> elements = findElements(context);
        if (elements.size() < 1)
            throw new NoSuchElementException("Unable to find element: " + getLoggableDescription());
        return elements.get(0);
    }

    public List<WebElement> findElements(SearchContext context)
    {
        List<WebElement> elements = context.findElements(this.toBy());
        if (_text != null)
        {
            Iterator<WebElement> it = elements.iterator();
            WebElement el;
            while (it.hasNext())
            {
                el = it.next();
                String text;
                try
                {
                    text = el.getText().trim();
                    if (!text.equals(_text))
                        it.remove();
                }
                catch (StaleElementReferenceException ex)
                {
                    it.remove();
                }
            }
        }
        if (_contains != null && !_contains.equals(""))
        {
            Iterator<WebElement> it = elements.iterator();
            WebElement el;
            while (it.hasNext())
            {
                el = it.next();
                String text;
                try
                {
                    text = el.getText();
                    if (!text.contains(_contains))
                        it.remove();
                }
                catch (StaleElementReferenceException ex)
                {
                    it.remove();
                }
            }
        }

        if (_index == null)
            return elements;
        else
        {
            List<WebElement> zeroOrOneElement = new ArrayList<>();
            if (elements.size() > _index)
                zeroOrOneElement.add(elements.get(_index));
            return zeroOrOneElement;
        }
    }

    public List<WebElement> waitForElements(final SearchContext context, final int msTimeout)
    {
        waitForElement(context, msTimeout);
        return findElements(context);
    }

    public List<WebElement> waitForElements(FluentWait<? extends SearchContext> wait)
    {
        try
        {
            return wait.ignoring(NotFoundException.class).until(new Function<SearchContext, List<WebElement>>()
            {
                @Override
                public List<WebElement> apply(SearchContext context)
                {
                    return findElements(context);
                }
            });
        }
        catch (TimeoutException notFound)
        {
            throw new NoSuchElementException(getLoggableDescription(), notFound);
        }
    }

    public WebElement waitForElement(final SearchContext context, final int msTimeout)
    {
        FluentWait<SearchContext> wait = new FluentWait<>(context).withTimeout(msTimeout, TimeUnit.MILLISECONDS);

        return waitForElement(wait);
    }

    public WebElement waitForElement(FluentWait<? extends SearchContext> wait)
    {
        try
        {
            return wait.ignoring(NotFoundException.class).until(new Function<SearchContext, WebElement>()
            {
                @Override
                public WebElement apply(SearchContext context)
                {
                    return findElement(context);
                }
            });
        }
        catch (TimeoutException notFound)
        {
            throw new NoSuchElementException(getLoggableDescription(), notFound);
        }
    }

    public void waitForElementToDisappear(final SearchContext context, final int msTimeout)
    {
        FluentWait<SearchContext> wait = new FluentWait<>(context).withTimeout(msTimeout, TimeUnit.MILLISECONDS);

        waitForElementToDisappear(wait);
    }

    public void waitForElementToDisappear(FluentWait<? extends SearchContext> wait)
    {
        wait.ignoring(NotFoundException.class).until(new Function<SearchContext, Boolean>()
        {
            @Override
            public Boolean apply(SearchContext context)
            {
                return findElements(context).size() == 0;
            }

            @Override
            public String toString()
            {
                return "element to disappear";
            }
        });
    }

    public static IdLocator id(String id)
    {
        return new IdLocator(id);
    }

    public static NameLocator name(String name)
    {
        return new NameLocator(name);
    }

    public static CssLocator css(String selector)
    {
        return new CssLocator(selector);
    }

    public static XPathLocator xpath(String xpathExpr)
    {
        return new XPathLocator(xpathExpr);
    }

    public static XPathLocator tag(String tag)
    {
        return xpath("//" + tag);
    }

    public static XPathLocator tagWithName(String tag, String name)
    {
        return tagWithAttribute(tag, "name", name);
    }

    public static XPathLocator tagWithId(String tag, String id)
    {
        return tagWithAttribute(tag, "id", id);
    }

    public static XPathLocator tagWithAttribute(String tag, String attrName, String attrVal)
    {
        return Locator.tag(tag).withAttribute(attrName, attrVal);
    }

    public static XPathLocator tagWithClass(String tag, String cssClass)
    {
        return Locator.tag(tag).withClass(cssClass);
    }

    public static XPathLocator tagWithClassContaining(String tag, String partialCssClass)
    {
        return Locator.tag(tag).withAttributeContaining("class", partialCssClass);
    }

    public static XPathLocator tagWithText(String tag, String text)
    {
        return Locator.tag(tag).withText(text);
    }

    public static XPathLocator tagContainingText(String tag, String text)
    {
        return Locator.tag(tag).withPredicate("contains(text(), " + xq(text) + ")");
    }

    public static XPathLocator linkWithImage(String image)
    {
        return xpath("//a/img").withAttributeContaining("src", image);
    }

    public static XPathLocator gwtButton(String text)
    {
        return tag("a").withClass("gwt-Anchor").withText(text);
    }

    public static XPathLocator button(String text)
    {
        return tag("button").notHidden().withPredicate("not(contains(@class, 'tab'))").withText(text);
    }

    public static XPathLocator buttonContainingText(String text)
    {
        return tag("button").notHidden().withPredicate("not(contains(@class, 'tab'))").containing(text);
    }

    public static XPathLocator lkButton(String text)
    {
        return tag("a").notHidden().withPredicate("contains(@class, 'labkey-button') or contains(@class, 'labkey-menu-button')").withText(text);
    }

    public static XPathLocator lkLabel(String text)
    {
        return tag("td").notHidden().withPredicate("contains(@class, 'labkey-form-label')").withText(text);
    }

    public static XPathLocator navTreeExpander(String nodeText)
    {
        return Locator.xpath("//tr").withClass("labkey-nav-tree-row").withText(nodeText).append("/td").withClass("labkey-nav-tree-node").append("/a");
    }

    public static XPathLocator extButton(String text)
    {
        return  tagWithClass("button", "x-btn-text").notHidden().withText(text);
    }

    public static XPathLocator extButtonEnabled(String text)
    {
        return xpath("//table").withClass("x-btn").withoutClass("x-item-disabled").append("//button").withClass("x-btn-text").withText(text);
    }

    public static XPathLocator extButtonContainingText(String text)
    {
        return xpath("//button[@class='x-btn-text' and contains(text(), " + xq(text) + ")]");
    }

    public static XPathLocator lkButtonDisabled(String text)
    {
        return xpath("//a[normalize-space(@class)='labkey-disabled-button' or normalize-space(@class)='labkey-disabled-menu-button']/span[text() = " + xq(text) + "]");
    }

    public static XPathLocator lkButtonContainingText(String text)
    {
        return xpath("//a[normalize-space(@class)='labkey-button' or normalize-space(@class)='labkey-menu-button']/span[contains(text(),  " + xq(text) + ")]");
    }

    public static XPathLocator linkWithImage(String image, Integer index)
    {
        return xpath("(//a/img[contains(@src, " + xq(image) + ")])[" + (index + 1) + "]");
    }

    public static XPathLocator linkWithText(String text)
    {
        return new LinkLocator(text);
    }

    public static XPathLocator linkContainingText(String text)
    {
        return xpath("//a").containing(text);
    }

    public static XPathLocator menuItem(String text)
    {
        return xpath("//a/span[" + NOT_HIDDEN + " and text() = " + xq(text) + " and contains(@class, 'menu-item-text')]");
    }

    public static XPathLocator menuBarItem(String text)
    {
        return xpath("//div[@id='menubar']//a[contains(text()," + xq(text) + ")]");
    }

    public static XPathLocator linkWithTitle(String title)
    {
        return xpath("//a[@title=" + xq(title) + "]");
    }

    public static XPathLocator linkWithHref(String url)
    {
        return xpath("//a[contains(@href, " + xq(url) + ")]");
    }

    public static XPathLocator linkWithSpan(String text)
    {
        return xpath("//a//span[contains(text(), " + xq(text) + ")]");
    }

    public static XPathLocator bodyLinkWithText(String text)
    {
        return xpath("//td[@id='bodypanel']//a[contains(text(), " + xq(text) + ")]");
    }

    public static XPathLocator input(String name)
    {
        return tagWithName("input", name);
    }

    public static XPathLocator inputByNameContaining(String partialName)
    {
        return xpath("//input[@type='text' and contains(@name,'" + partialName + "')]");
    }

    public static XPathLocator inputById(String id)
    {
        return tagWithId("input", id);
    }

    public static XPathLocator textarea(String name)
    {
        return tagWithName("textarea", name);
    }

    public static XPathLocator checkboxByTitle(String title)
    {
        return xpath("//input[@type='checkbox' and @title=" + xq(title) + "]");
    }

    public static XPathLocator radioButtonByName(String name)
    {
        return xpath("//input[@type='radio' and @name=" + xq(name) + "]");
    }

    public static XPathLocator checkboxByName(String name)
    {
        return xpath("//input[@type='checkbox' and @name=" + xq(name) + "]");
    }

    public static XPathLocator radioButtonById(String id)
    {
        return xpath("//input[@type='radio' and @id=" + xq(id) + "]");
    }

    public static XPathLocator checkboxById(String id)
    {
        return xpath("//input[@type='checkbox' and @id=" + xq(id) + "]");
    }

    public static CssLocator checkedRadioInGroup(String groupName)
    {
        return css("input:checked[name=" + groupName + "][type=radio]");
    }

    public static XPathLocator radioButtonByNameAndValue(String name, String value)
    {
        return xpath("//input[@type='radio' and @name=" + xq(name) + " and @value=" + xq(value) + "]");
    }

    public static XPathLocator checkboxByNameAndValue(String name, String value)
    {
        return xpath("//input[@type='checkbox' and @name=" + xq(name) + " and @value=" + xq(value) + "]");
    }

    public static XPathLocator imageMapLinkByTitle(String imageMapName, String title)
    {
        return xpath("//map[@name=" + xq(imageMapName) + "]/area[@title=" + xq(title) + "]");
    }

    public static XPathLocator lookupLink(String schemaName, String queryName, String pkName)
    {
        String linkText = schemaName + "." + queryName + "." + (null != pkName ? pkName : "");
        return xpath("//span[contains(@class, 'labkey-link') and contains(text(), " + xq(linkText) + ")]");
    }

    public static XPathLocator gwtTextBoxByLabel(String label)
    {
        return Locator.tagWithClass("input", "gwt-TextBox").withPredicate(Locator.xpath("../preceding-sibling::td").withText(label));
    }

    public static XPathLocator gwtListBoxByLabel(String label)
    {
        return Locator.tagWithClass("select", "gwt-ListBox").withPredicate(Locator.xpath("../preceding-sibling::td/table/tbody/tr/td/div").withText(label));
    }

    public static XPathLocator gwtCheckBoxOnImportGridByColLabel(String label)
    {
        return Locator.tagWithAttribute("input", "type", "checkbox").withPredicate(Locator.xpath("../../following-sibling::td/span").containing(label));
    }

    public static XPathLocator gwtNextButtonOnImportGridByColLabel(String label)
    {
        return Locator.tagWithClass("div", "x-tbar-page-next").withPredicate(Locator.xpath("../preceding-sibling::td/span").containing(label));
    }

    public static Locator permissionRendered()
    {
        return Locators.pageSignal("policyRendered");
    }

    public static XPathLocator permissionButton(String groupName, String role)
    {
        // Supports permission types from a variety of modules.
        return tag("div").withClass("rolepanel").withDescendant(Locator.tag("h3").withText(role)).append(Locator.tag("a").withClass("x4-btn").withDescendant(Locator.tag("span").startsWith(groupName)));
    }

    public static XPathLocator closePermissionButton(String groupName, String role)
    {
        // Supports permission types from a variety of modules.
        return permissionButton(groupName, role).append(Locator.tag("span").withClass("closeicon"));
    }

    public static XPathLocator schemaTreeNode(String schemaName)
    {
        return Locator.tag("tr").withClass("x4-grid-row").append("/td/div/span").withText(schemaName);
    }

    public static XPathLocator queryTreeNode(String queryName)
    {
        // NOTE: this may mis-fire (hit the wrong node... watch for this)
        return Locator.tag("tr").withClass("x4-grid-row").append("/td/div/span").withText(queryName);
    }

    public static XPathLocator permissionsTreeNode(String folderName)
    {
        return tagWithClass("a", "x-tree-node-anchor").child("span[text()='" + folderName + "' or text()='" + folderName + "*']");
    }

    public static XPathLocator currentProject()
    {
        return id("folderBar");
    }

    /**
     * TODO: Remove in September 2015. Leaving method so as to not break feature branches
     */
    @Deprecated
    public static XPathLocator divByIdContaining(String partialId)
    {
        return tag("div").withAttributeContaining("id", partialId);
    }

    /**
     * TODO: Remove in September 2015. Leaving method so as to not break feature branches
     */
    @Deprecated
    public static XPathLocator divByClassContaining(String partialClass)
    {
        return tag("div").withAttributeContaining("class", partialClass);
    }

    public static XPathLocator divByInnerText(String text)
    {
        return xpath("//div[.='" + text + "']");
    }

    public static IdLocator folderTab(String text)
    {
        if ("+".equals(text))
            return Locator.id("addTab");
        else
            return Locator.id(text.replace(" ", "") + "Tab");
    }

    public static XPathLocator paginationText(int firstRow, int lastRow, int maxRows)
    {
        DecimalFormat numFormat = new DecimalFormat("#,###");

        int rowsPerPage = lastRow - firstRow + 1;
        int pageCount = (int)Math.ceil((double)maxRows / (double)rowsPerPage);
        int currentPage = (int)Math.ceil((double)lastRow / (double)rowsPerPage);

        boolean hasFirstLink = currentPage > 2;
        boolean hasPrevLink = currentPage > 1;
        boolean hasNextLink = currentPage < pageCount;
        boolean hasLastLink = currentPage < pageCount - 1;

        StringBuilder paginationText = new StringBuilder();
        if (hasFirstLink) paginationText.append("\u00AB First ");
        if (hasPrevLink) paginationText.append("\u2039 Prev ");
        paginationText.append(String.format("%s - %s of %s", numFormat.format(firstRow), numFormat.format(lastRow), numFormat.format(maxRows)));
        if (hasNextLink) paginationText.append(" Next \u203A");
        if (hasLastLink) paginationText.append(" Last \u00BB");

        return paginationText().withText(paginationText.toString());
    }

    /**
     * Pagination text for dataregion with one page of data
     */
    public static XPathLocator paginationText(int rowCount)
    {
        return paginationText(1, rowCount, rowCount);
    }

    public static XPathLocator paginationText()
    {
        return Locator.tagWithClass("div", "labkey-pagination");
    }

    public static XPathLocator pageHeader(String headerText)
    {
        return Locator.id("labkey-nav-trail-current-page").withText(headerText);
    }

    /**
     * Quote text to be used as literal string in xpath expression
     *     Direct port from attibuteValue function in selenium IDE locatorBuilders.js
     * @param value to be quoted
     * @return value with either ' or " around it or assembled from parts
     */
    public static String xq(String value)
    {
        if (!value.contains("'")) {
            return "'" + value + "'";
        } else if (!value.contains("\"")) {
            return '"' + value + '"';
        } else {
            String result = "concat(";
            while (true) {
                int apos = value.indexOf("'");
                int quot = value.indexOf('"');
                if (apos < 0) {
                    result += "'" + value + "'";
                    break;
                } else if (quot < 0) {
                    result += '"' + value + '"';
                    break;
                } else if (quot < apos) {
                    String part = value.substring(0, apos);
                    result += "'" + part + "'";
                    value = value.substring(part.length());
                } else {
                    String part = value.substring(0, quot);
                    result += '"' + part + '"';
                    value = value.substring(part.length());
                }
                result += ',';
            }
            result += ')';
            return result;
        }
    }

    public static class XPathLocator extends Locator
    {
        public XPathLocator(String loc)
        {
            super(loc);
        }

        public XPathLocator containing(String contains)
        {
            return this.withPredicate("contains(normalize-space(), "+xq(contains)+")");
        }

        public XPathLocator withText(String text)
        {
            return this.withPredicate("normalize-space()="+xq(text));
        }

        public XPathLocator withText()
        {
            return this.withPredicate("string-length() > 0");
        }

        public XPathLocator withoutText(String text)
        {
            return this.withPredicate("not(normalize-space()=" + xq(text) + ")");
        }

        public XPathLocator withoutText()
        {
            return this.withPredicate("string-length() == 0");
        }

        public XPathLocator withTextMatching(String regex)
        {
            return this.withPredicate("matches(normalize-space(), " + xq(regex) + ")");
        }

        public XPathLocator startsWith(String text)
        {
            return this.withPredicate("starts-with(normalize-space(), "+xq(text)+")");
        }

        public XPathLocator index(Integer index)
        {
            if (0 == index)
                return this;
            else
                return new XPathLocator("("+_loc+")["+(index+1)+"]");
        }

        public By toBy()
        {
            return By.xpath(getPath());
        }

        public XPathLocator parent()
        {
            return new XPathLocator("(" + getPath() + ")/..");
        }

        public XPathLocator child(String str)
        {
            return new XPathLocator("(" + getPath() + ")/" + str);
        }

        public XPathLocator child(XPathLocator childLocator)
        {
            return this.child(stripLeadingSlashes(childLocator.getPath()));
        }

        private String stripLeadingSlashes(String xpath)
        {
            String strippedXPath = xpath;

            while(strippedXPath.startsWith("/"))
            {
                strippedXPath = strippedXPath.substring(1);
            }

            return strippedXPath;
        }

        public XPathLocator last()
        {
            return new XPathLocator("("+_loc+")[last()]");
        }

        public XPathLocator append(String clause)
        {
            return new XPathLocator(getPath() + clause);
        }

        public XPathLocator append(XPathLocator child)
        {
            return new XPathLocator(getPath() + child.getPath());
        }

        public XPathLocator hidden()
        {
            return this.withPredicate(HIDDEN);
        }

        public XPathLocator notHidden()
        {
            return this.withPredicate(NOT_HIDDEN);
        }

        public XPathLocator enabled()
        {
            return this.withPredicate(ENABLED);
        }

        public XPathLocator withDescendant(XPathLocator descendant)
        {
            return this.withPredicate(descendant.getPath());
        }

        public XPathLocator withPredicate(XPathLocator descendant)
        {
            return this.withPredicate(descendant.getPath());
        }

        public XPathLocator withPredicate(String predicate)
        {
            if (predicate.startsWith("//"))
                predicate = predicate.replaceFirst("//", "descendant::");
            return this.append("["+predicate+"]");
        }

        public XPathLocator attributeStartsWith(String attribute, String text)
        {
            return this.withPredicate(String.format("starts-with(@%s, "+xq(text)+")", attribute));
        }

        public XPathLocator attributeEndsWith(String attribute, String substring)
        {
            return this.withPredicate(String.format("substring(@%s, string-length(@%s) - %d) = %s", attribute, attribute, substring.length() - 1, xq(substring))); // XPath 1.0 doesn't support ends-with()
        }

        public XPathLocator withClass(String cssClass)
        {
            return this.withPredicate("contains(concat(' ',normalize-space(@class),' '), " + xq(" " + cssClass + " ") + ")");
        }

        public XPathLocator withoutClass(String cssClass)
        {
            return this.withPredicate("not(contains(concat(' ',normalize-space(@class),' '), " + xq(" " + cssClass + " ") + "))");
        }

        public XPathLocator withAttribute(String attrName, String attrVal)
        {
            return this.withPredicate("@" + attrName + "=" + xq(attrVal));
        }

        public XPathLocator withAttributeContaining(String attrName, String partialAttrVal)
        {
            return this.withPredicate("contains(@" + attrName + ", " + xq(partialAttrVal) + ")");
        }

        public String getPath()
        {
            return _loc;
        }

        public String toString()
        {
            return "xpath="+toXpath();
        }

        public String toXpath()
        {
            return _loc;
        }

        public String getLoggableDescription()
        {
            return toString();
        }

        @Override
        public WebElement findElement(SearchContext context)
        {
            if (context instanceof WebElement && _loc.startsWith("//"))
                return new XPathLocator(_loc.replaceFirst("//", "descendant::")).findElement(context);
            else
                return super.findElement(context);
        }

        @Override
        public List<WebElement> findElements(SearchContext context)
        {
            if (context instanceof WebElement && _loc.startsWith("//"))
                return new XPathLocator(_loc.replaceFirst("//", "descendant::")).findElements(context);
            else
                return super.findElements(context);
        }
    }

    public static class IdLocator extends XPathLocator
    {
        private String _id;

        public IdLocator(String loc)
        {
            super(loc.length() > 0 ? "//*[@id = " + xq(loc) + "]" : "");
            _id = loc;
        }

        public CssLocator append(CssLocator locator)
        {
            return toCssLocator().append(locator);
        }

        public By toBy()
        {
            return _id.contains(" ") ? super.toBy() : By.id(_id);
        }

        public String toString()
        {
            return "id=" + _id;
        }

        public CssLocator toCssLocator()
        {
            return css(_id.length() > 0 ? "#" + _id : "");
        }
    }

    public static class NameLocator extends Locator
    {
        public NameLocator(String loc)
        {
            super(loc);
        }

        private NameLocator(String loc, Integer index, String contains, String text)
        {
            super(loc, index, contains, text);
        }

        public Locator containing(String contains)
        {
            return new NameLocator(_loc, _index, contains, _text);
        }

        public Locator withText(String text)
        {
            return new NameLocator(_loc, _index, _contains, text);
        }

        public Locator index(Integer index)
        {
            return new NameLocator(_loc, index, _contains, _text);
        }

        @Override
        public String toString()
        {
            return "name=" + _loc + (_index != null ? " index=" + _index : "");
        }

        public By toBy()
        {
            return By.name(_loc);
        }
    }

    public static class CssLocator extends Locator
    {
        public CssLocator(String loc)
        {
            super(loc);
        }

        private CssLocator(String loc, Integer index, String contains, String text)
        {
            super(loc, index, contains, text);
        }

        public static CssLocator union(CssLocator... locators)
        {
            if (locators.length == 0)
                throw new IllegalArgumentException("Specify one or more locators to union");

            for (Locator loc : locators)
            {
                if (loc._contains != null || loc._text != null || loc._index != null)
                    throw new IllegalArgumentException("Only able to union raw CSS selectors");
            }

            StringBuilder unionedLocators = new StringBuilder();
            unionedLocators.append(locators[0]._loc);
            for (int i = 1; i < locators.length; i++)
            {
                unionedLocators.append(", ");
                unionedLocators.append(locators[i]._loc);
            }

            return new CssLocator(unionedLocators.toString()){
                @Override
                public CssLocator append(CssLocator loc)
                {
                    throw new UnsupportedOperationException("Don't append to unioned CSS selectors.");
                }

                @Override
                public Locator index(Integer index)
                {
                    throw new UnsupportedOperationException("Don't index into unioned CSS selectors.");
                }
            };
        }

        public Locator containing(String contains)
        {
            if (_text != null && _text.length() > 0 || _contains != null && _contains.length() > 0)
                throw new IllegalStateException("Text content already been specified for this Locator");

            return new CssLocator(_loc, _index, contains, _text);
        }

        public Locator withText(String text)
        {
            if (_text != null && _text.length() > 0 || _contains != null && _contains.length() > 0)
                throw new IllegalStateException("Text content already been specified for this Locator");

            return new CssLocator(_loc, _index, _contains, text);
        }

        /**
         * Locate the nth element matched by the selector. Can only find a single element.
         * @param index zero-based index of desired element
         * @return Locator to find the nth instance of the base selector
         */
        public Locator index(Integer index)
        {
            if (_index != null && _index != 0)
                throw new IllegalArgumentException("An index has already been specified for this Locator");

            return new CssLocator(_loc, index, _contains, _text);
        }

        public CssLocator append(String clause)
        {
            return new CssLocator(_loc + clause);
        }

        public CssLocator append(CssLocator clause)
        {
            return append(" " + clause.getLocatorString());
        }

        @Override
        public String toString()
        {
            return "css=" + _loc;
        }

        public By toBy()
        {
            if (_loc.contains(":contains("))
                throw new IllegalArgumentException("CSS3 does not support the ':contains' pseudo-class: '" + _loc + "'");
            return By.cssSelector(_loc);
        }
    }

    public static class LinkLocator extends XPathLocator
    {
        private String _linkText;

        public LinkLocator(String linkText)
        {
            super(tag("a").withText(linkText).toXpath());
            _linkText = linkText;
        }

        @Override
        public List<WebElement> findElements(SearchContext context)
        {
            List<WebElement> elements = super.findElements(context);
            if (elements.size() == 0 && !_linkText.equals(_linkText.toUpperCase()))
                return (new LinkLocator(_linkText.toUpperCase())).findElements(context);
            else
                return elements;
        }

        @Override
        public String toString()
        {
            return "link=" + _linkText;
        }

        public By toBy()
        {
            return By.linkText(_linkText);
        }
    }
}

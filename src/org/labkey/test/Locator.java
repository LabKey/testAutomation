/*
 * Copyright (c) 2007-2014 LabKey Corporation
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

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public abstract class Locator
{
    protected String _loc;
    protected Integer _index;
    protected String _contains;
    protected String _text;

    // XPATH fragments
    public static final String NOT_HIDDEN = "not(ancestor-or-self::*[contains(@style,'display: none') or contains(@style,'visibility: hidden') or contains(@class, 'x-hide-display') or contains(@class, 'x4-hide-offsets') or contains(@class, 'x-hide-offsets')] or (@type = 'hidden'))";
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

    public abstract Locator containing(String contains);

    public abstract Locator withText(String text);

    /**
     * Locate the nth element matched by the selector. Can only find a single element.
     * @param index zero-based index of desired element
     * @return Locator for the element
     */
    public abstract Locator index(Integer index);

    /**
     * For direct use with selenium RC
     */
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

    private void turnOnImplicitWait(WebDriver driver)
    {
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }

    private void turnOffImplicitWait(WebDriver driver)
    {
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
    }

    public WebElement findElement(WebDriver driver)
    {
        List<WebElement> elements = findElements(driver);
        if (elements.size() < 1)
            throw new NoSuchElementException("Unable to find element: " + getLoggableDescription());
        return elements.get(0);
    }

    public List<WebElement> findElements(WebDriver driver)
    {
        List<WebElement> elements = driver.findElements(this.toBy());
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

    public WebElement waitForElement(final WebDriver driver, final int msTimeout)
    {
        long secTimeout = msTimeout / 1000;
        secTimeout = secTimeout > 0 ? secTimeout : 1;
        WebDriverWait wait = new WebDriverWait(driver, secTimeout);

        try
        {
            return wait.until(new ExpectedCondition<WebElement>()
            {
                @Override
                public WebElement apply(WebDriver d)
                {
                    return findElement(driver);
                }

                @Override
                public String toString()
                {
                    return "waiting for element: " + getLoggableDescription();
                }
            });
        }
        catch (TimeoutException notFound)
        {
            throw new NoSuchElementException(getLoggableDescription(), notFound);
        }
    }

    public void waitForElementToDisappear(final WebDriver driver, final int msTimeout)
    {
        long secTimeout = msTimeout / 1000;
        secTimeout = secTimeout > 0 ? secTimeout : 1;
        WebDriverWait wait = new WebDriverWait(driver, secTimeout);
        try
        {
            wait.until(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver d)
                {
                    return findElements(driver).size() == 0;
                }
            });
        }
        catch (TimeoutException ex)
        {
            fail("Timeout waiting for element to disappear [" + secTimeout + "sec]: " + getLoggableDescription());
        }
    }

    public void waitForElementToHaveValue(final WebDriver driver, final int msTimeout, final String value)
    {
        long secTimeout = msTimeout / 1000;
        secTimeout = secTimeout > 0 ? secTimeout : 1;
        WebDriverWait wait = new WebDriverWait(driver, secTimeout);
        try
        {
            wait.until(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver d)
                {
                    return findElement(driver).getText().contains(value);
                }
            });
        }
        catch (TimeoutException ex)
        {
            fail("Timeout waiting for element to disappear [" + secTimeout + "sec]: " + getLoggableDescription());
        }
    }

    public List<WebElement> waitForElements(final WebDriver driver, final int msTimeout)
    {
        waitForElement(driver, msTimeout);
        return findElements(driver);
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

    /**
     * @deprecated Use {@link NameLocator} with {@link #index(Integer)}
     * Element by name and index within the set of elements with that name
     * @param name
     * @param index
     * @return
     */
    @Deprecated public static Locator name(String name, Integer index)
    {
        return new NameLocator(name).index(index);
    }

    /**
     * Find using xpath
     * @param xpathExpr
     * @return
     */
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

    public static XPathLocator formWithName(String formName)
    {
        return tagWithName("form", formName);
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
        return xpath("//a/img[contains(@src, " + xq(image) + ")]");
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

    public static XPathLocator navTreeExpander(String nodeText)
    {
        return Locator.xpath("//tr").withClass("labkey-nav-tree-row").withText(nodeText).append("/td").withClass("labkey-nav-tree-node").append("/a");
    }

    public static XPathLocator extButton(String text)
    {
        return xpath("//button[" + NOT_HIDDEN + " and contains(@class, 'x-btn-text') and text() = " + xq(text) + "]");
    }

    public static XPathLocator extButtonEnabled(String text)
    {
        return xpath("//table").withClass("x-btn").withoutClass("x-item-disabled").append("//button").withClass("x-btn-text").withText(text);
    }

    public static XPathLocator extMenuItemEnabled(String text)
    {
        return xpath("//li").withClass("x-menu-list-item").withoutClass("x-item-disabled").append("//span").withClass("x-menu-item-text").withText(text);
    }

    public static XPathLocator extMenuItemDisabled(String text)
    {
        return xpath("//li").withClass("x-menu-list-item").withClass("x-item-disabled").append("//span").withClass("x-menu-item-text").withText(text);
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

    public static LinkLocator linkWithText(String text)
    {
        return new LinkLocator(text);
    }

    public static XPathLocator linkWithText(String text, Integer index)
    {
        return xpath("//a").withText(text).index(index);
    }

    public static XPathLocator linkContainingText(String text)
    {
        return xpath("//a").containing(text);
    }

    public static XPathLocator linkContainingText(String text, Integer index)
    {
        return linkContainingText(text).index(index);
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

    public static XPathLocator buttonWithImgSrc(String imgSrc)
    {
        return xpath("//input[@type='image' and contains(@src, " + xq(imgSrc) + ")]");
    }

    public static XPathLocator buttonWithImgSrc(String imgSrc, Integer index)
    {
        return xpath("(//input[@type='image' and contains(@src, " + xq(imgSrc) + ")])[" + (index + 1) + "]");
    }

    public static XPathLocator input(String name)
    {
        return tagWithName("input", name);
    }

    public static XPathLocator inputByNameContaining(String partialName)
    {
        return xpath("//input[@type='text' and contains(@name,'" + partialName + "')]");
    }

    public static XPathLocator radioButtonByTitle(String title)
    {
        return xpath("//input[@type='radio' and @title=" + xq(title) + "]");
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

    public static XPathLocator checkboxByIdContaining(String id)
    {
        return xpath("//input[@type='checkbox' and @id[contains(@id,'" + id + ";]");
    }

    public static XPathLocator radioButtonById(String id)
    {
        return xpath("//input[@type='radio' and @id=" + xq(id) + "]");
    }

    public static XPathLocator checkboxById(String id)
    {
        return xpath("//input[@type='checkbox' and @id=" + xq(id) + "]");
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

    public static XPathLocator imageWithSrc(String src, boolean substringMatch)
    {
        if (substringMatch)
            return xpath("//img[contains(@src, " + xq(src) + ")]");
        else
            return xpath("//img[@src=" + xq(src) + "]");
    }

    //Locator for image with src=src (if substringMatch=false
    public static XPathLocator imageWithSrc(String src, boolean substringMatch, Integer index)
    {
        if (substringMatch)
            return xpath("(//img[contains(@src, " + xq(src) + ")])[" + index + "]");
        else
            return xpath("(//img[@src=" + xq(src) + "])[" + index + "]");
    }

    public static XPathLocator imageWithAltText(String altText, boolean substringMatch)
    {
        if (substringMatch)
            return xpath("//img[contains(@alt, " + xq(altText) + ")]");
        else
            return xpath("//img[@alt=" + xq(altText) + "]");
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

    public static XPathLocator permissionRendered()
    {
        return xpath("//input[@id='policyRendered']");
    }

    public static XPathLocator permissionButton(String groupName, String role)
    {
        // Supports permission types from a variety of modules.
        return tag("div").withClass("rolepanel").withDescendant(Locator.tag("h3").withText(role)).append(Locator.tag("a").withClass("x4-btn").withDescendant(Locator.tag("span").withText(groupName)));
    }

    public static XPathLocator closePermissionButton(String groupName, String role)
    {
        // Supports permission types from a variety of modules.
        return permissionButton(groupName, role).append(Locator.tag("span").withClass("closeicon"));
    }

    public static XPathLocator fileTreeByName(String name)
    {
        return xpath("//a[@class='x-tree-node-anchor']/span[text()=" + xq(name) + "]");
    }

    public static XPathLocator schemaTreeNode(String schemaName)
    {
        return xpath("//a[@class='x-tree-node-anchor']/span[text()='" + schemaName + "']");
    }

    public static XPathLocator queryTreeNode(String schemaName, String queryName)
    {
        String[] schemaParts = schemaName.split("\\.");
        return xpath("//li[@class='x-tree-node']/div/a/span[text()='" + schemaParts[schemaParts.length - 1] + "']/../../..//span[text()='" + queryName + "']");
    }

    public static XPathLocator permissionsTreeNode(String folderName)
    {
        return xpath("//a[@class='x-tree-node-anchor']/span[text()='" + folderName + "' or text()='" + folderName + "*']");
    }

    public static XPathLocator currentProject()
    {
        return id("folderBar");
    }

    public static XPathLocator divByName(String name)
    {
        return xpath("//div[@name='" + name + "']");
    }

    public static XPathLocator divByNameContaining(String partialName)
    {
        return xpath("//div[contains(@name, '" + partialName + "')]");
    }

    public static XPathLocator divById(String id)
    {
        return xpath("//div[@id='" + id + "']");
    }

    public static XPathLocator divByIdContaining(String partialId)
    {
        return xpath("//div[contains(@id, '" + partialId + "')]");
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
     * Pagination text for dataregion with one page of data with
     * @param rowCount
     * @return
     */
    public static XPathLocator paginationText(int rowCount)
    {
        return paginationText(1, rowCount, rowCount);
    }

    public static XPathLocator paginationText()
    {
        return Locator.xpath("//div[contains(@class, 'labkey-pagination')]");
    }

    public static XPathLocator pageHeader(String headerText)
    {
        return Locator.xpath("id('labkey-nav-trail-current-page')").withText(headerText);
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

        private XPathLocator(String loc, Integer index, String contains, String text)
        {
            super(loc, index, contains, text);
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

        public XPathLocator withTextMatching(String regex)
        {
            return this.withPredicate("matches(normalize-space(), '" + xq(regex) + "')");
        }

        public XPathLocator startsWith(String text)
        {
            return this.withPredicate("starts-with(normalize-space(), "+xq(text)+")");
        }

        public XPathLocator index(Integer index)
        {
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

        public XPathLocator append(String clause)
        {
            return new XPathLocator(getPath() + clause);
        }

        public XPathLocator append(XPathLocator child)
        {
            return new XPathLocator(getPath() + child.getPath());
        }

        public XPathLocator notHidden()
        {
            return this.withPredicate(NOT_HIDDEN);
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
    }

    public static class IdLocator extends XPathLocator
    {
        private String _id;

        public IdLocator(String loc)
        {
            super(loc.length() > 0 ? "//*[@id = '" + loc + "']" : "");
            _id = loc;
        }

        public By toBy()
        {
            return By.id(_id);
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
                return null;

            for (Locator loc : locators)
            {
                if (loc._contains != null || loc._text != null || loc._index != null)
                    throw new IllegalArgumentException("Only able to union raw CSS selectors");
            }

            StringBuilder unionedLocators = new StringBuilder();
            unionedLocators.append(locators[0]._loc);
            for (int i = 1; i < locators.length; i++)
            {
                unionedLocators.append(",");
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
            return new CssLocator(_loc + " " + clause);
        }

        public CssLocator append(CssLocator clause)
        {
            return new CssLocator(_loc + " " + clause.getLocatorString());
        }

        @Override
        public String toString()
        {
            return "css=" + _loc + (_contains != null ? ":contains('" + _contains + "')" : "");
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

        public LinkLocator(String loc)
        {
            super(xpath("//a").withText(loc).toXpath());
            _linkText = loc;
        }

        @Override
        public List<WebElement> findElements(WebDriver driver)
        {
            List<WebElement> elements = super.findElements(driver);
            if (elements.size() == 0 && !_linkText.equals(_linkText.toUpperCase()))
                return (new LinkLocator(_linkText.toUpperCase())).findElements(driver);
            else
                return elements;
        }

        public XPathLocator toXPathLocator()
        {
            return xpath(_loc);
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

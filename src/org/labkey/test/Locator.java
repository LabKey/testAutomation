/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

/**
 * User: Mark Igra
 * Date: Feb 8, 2007
 * Time: 10:59:37 AM
 */
public class Locator
{
    private String loc;
    // XPATH fragments
    public static final String NOT_HIDDEN = "not(ancestor-or-self::*[contains(@style,'display: none') or contains(@style,'visibility: hidden') or contains(@class, 'x-hide-display') or contains(@class, 'x4-hide-offsets') or contains(@style, 'left: -10000px')])";
    public static final String ENABLED = "not(ancestor-or-self::*[contains(@class, 'x-item-disabled')])";


    protected Locator(String rawString)
    {
        loc = rawString;
    }

    public String toString()
    {
        return loc;
    }

    public String toXpath()
    {
        String xpath = loc.substring(loc.indexOf("=")+1);
        return xpath;

    }

    public By toBy()
    {
        if (loc.startsWith("id="))
            return By.id(loc.substring(loc.indexOf("=")+1));
        if (loc.startsWith("name="))
            return By.name(loc.substring(loc.indexOf("=")+1));
        if (loc.startsWith("xpath="))
            return By.xpath(loc.substring(loc.indexOf("=")+1));
        if (loc.startsWith("css="))
            return By.cssSelector(loc.substring(loc.indexOf("=")+1));
        if (loc.startsWith("link="))
            return By.partialLinkText(loc.substring(loc.indexOf("=")+1));
        else
            return By.name(loc.substring(loc.indexOf("=")+1)); // best guess for dom/identifier. will break
    }

    protected void setRawLocator(String locator)
    {
        loc = locator;
    }

    public static Locator raw(String str)
    {
        return new Locator(str);
    }

    public static Locator nameOrId(String str)
    {
        return new Locator("identifier=" + str);
    }

    public static Locator id(String id)
    {
        return new Locator("id=" + id);
    }

    public static Locator name(String name)
    {
        return new Locator("name=" + name);
    }

    public static Locator css(String selector)
    {
        return new Locator("css=" + selector);
    }

    /**
     * Element by name and index within the set of elements with that name
     * @param name
     * @param index
     * @return
     */
    public static Locator name(String name, int index)
    {
        return new Locator("name=" + name + " index=" + index);
    }

    /**
     * Find using script expression. Can be any amount of script,
     * result of evaluating last expression will be returned.
     * @param scriptExpr
     * @return
     */
    public static Locator dom(String scriptExpr)
    {
        return new Locator("dom=" + scriptExpr);
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

    public static XPathLocator tagWithName(String tag, String name)
    {
        return tagWithAttribute(tag, "name", name);
    }

    public static XPathLocator formWithName(String formName)
    {
        return tagWithName("form", formName);
    }

    public static XPathLocator formWithAction(String actionName)
    {
        return tagWithAttribute("form", "action", actionName);
    }

    public static XPathLocator tagWithId(String tag, String id)
    {
        return tagWithAttribute(tag, "id", id);
    }

    public static XPathLocator tagWithAttribute(String tag, String attrName, String attrVal)
    {
        return xpath("//" + tag + "[@" + attrName + "=" + xq(attrVal) + "]");
    }

    public static XPathLocator tagWithText(String tag, String text)
    {
        return xpath("//" + tag + "[text() = " + xq(text) + "]");
    }

    public static XPathLocator tagContainingText(String tag, String text)
    {
        return xpath("//" + tag + "[contains(text(), " + xq(text) + ")]");
    }

    public static XPathLocator linkWithImage(String image)
    {
        return xpath("//a/img[contains(@src, " + xq(image) + ")]");
    }

    public static XPathLocator gwtButton(String text)
    {
        return xpath("//a[contains(@class, 'gwt-Anchor') and text() = '" + text + "']");
    }

    public static XPathLocator button(String text)
    {
        return xpath("//button["+ NOT_HIDDEN +" and descendant-or-self::*[text() = '" + text + "']]");
    }

    public static XPathLocator buttonContainingText(String text)
    {
        return xpath("//button["+ NOT_HIDDEN +" and descendant-or-self::*[contains(text(), '" + text + "')]]");
    }

    public static XPathLocator navButton(String text)
    {
        return xpath("//a["+ NOT_HIDDEN +" and contains(@class, 'labkey-button') or contains(@class, 'labkey-menu-button')]/span[text() = " + xq(text) + "]");
    }

    public static XPathLocator extButton(String text)
    {
        return xpath("//button["+ NOT_HIDDEN +" and contains(@class, 'x-btn-text') and text() = " + xq(text) + "]");
    }

    public static XPathLocator ext4Button(String text)
    {
        return xpath("//button/span["+ NOT_HIDDEN +" and contains(@class, 'x4-btn-inner') and text() = " + xq(text) + "]");
    }

    public static XPathLocator ext4Button(String text, int index)
    {
        return xpath("(//button/span["+ NOT_HIDDEN +" and contains(@class, 'x4-btn-inner') and text() = " + xq(text) + "])[" + (index + 1) + "]");
    }

    public static XPathLocator extButtonEnabled(String text)
    {
        return xpath("//table[contains(@class, 'x-btn-wrap') and not (contains(@class, 'x-item-disabled'))]//button[contains(@class, 'x-btn-text') and text() = " + xq(text) + "]");
    }

    public static XPathLocator extMenuItem(String text)
    {
        return xpath("//a[@class='x-menu-item' and text() = " + xq(text) + "]");
    }

    public static XPathLocator extButton(String text, int index)
    {
        return xpath("(//button[contains(@class, 'x-btn-text') and text() = " + xq(text) + "])[" + (index + 1) + "]");
    }

    public static XPathLocator extButtonContainingText(String text)
    {
        return xpath("//button[@class='x-btn-text' and contains(text(), " + xq(text) + ")]");
    }

    public static XPathLocator ext4Checkbox(String label)
    {
        return xpath("//input[@role = 'checkbox' and following-sibling::label[text()='" + label + "']]");
    }

    public static XPathLocator ext4Radio(String label)
    {
        return xpath("//input[@role = 'radio' and following-sibling::label[contains(text(), '" + label + "')]]");
    }

    public static XPathLocator navButtonDisabled(String text)
    {
        return xpath("//a[normalize-space(@class)='labkey-disabled-button' or normalize-space(@class)='labkey-disabled-menu-button']/span[text() = " + xq(text) + "]");
    }

    public static XPathLocator navButtonContainingText(String text)
    {
        return xpath("//a[normalize-space(@class)='labkey-button' or normalize-space(@class)='labkey-menu-button']/span[contains(text(),  " + xq(text) + ")]");
    }

    public static XPathLocator navButton(String text, int index)
    {
        return xpath("(//a[normalize-space(@class)='labkey-button' or @class='labkey-menu-button']/span[text() = " + xq(text) + "])[" + (index + 1) + "]");
    }

    public static XPathLocator navSubmitButton(String text)
    {
        return xpath("//span[normalize-space(@class)='labkey-button' or @class='labkey-menu-button']/input[@type='submit' and @value=" + xq(text) + "]");
    }

    public static XPathLocator navSubmitButtonContainingText(String text)
    {
        return xpath("//input[@type='submit' and contains(@value, " + xq(text) + ")]");
    }

    public static XPathLocator navSubmitButton(String text, int index)
    {
        return xpath("(//input[@type='submit' and @value=" + xq(text) + "])[" + (index + 1) + "]");
    }

    public static XPathLocator linkWithImage(String image, int index)
    {
        return xpath("(//a/img[contains(@src, " + xq(image) + ")])[" + (index + 1) + "]");
    }

    public static Locator linkWithText(String text)
    {
        return new Locator("link=" + text);
    }

    public static XPathLocator linkWithText(String text, int index)
    {
        return xpath("(//a[text() = " + xq(text) + "])[" + (index + 1) + "]");
    }

    public static XPathLocator linkContainingText(String text)
    {
        return xpath("//a[contains(text(), " + xq(text) + ")]");
    }

    public static XPathLocator linkContainingText(String text, int index)
    {
        return xpath("(//a[contains(text(), " + xq(text) + ")])[" + (index + 1) + "]");
    }

    public static XPathLocator menuItem(String text)
    {
        return xpath("//a/span["+ NOT_HIDDEN +" and text() = " + xq(text) + " and contains(@class, 'x-menu-item-text')]");
    }

    public static XPathLocator menuBarItem(String text)
    {
        return xpath("//table[@id='menubar']//a/span[contains(text(), " + xq(text) + ")]");
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

    public static XPathLocator buttonWithImgSrc(String imgSrc, int index)
    {
        return xpath("(//input[@type='image' and contains(@src, " + xq(imgSrc) + ")])[" + (index + 1) + "]");
    }

    public static XPathLocator input(String name)
    {
        return tagWithName("input", name);
    }

    public static XPathLocator inputWithValue(String value)
    {
        return tagWithAttribute("input", "value", value);
    }

    public static Locator formElement(String formName, String elementName)
    {
        return dom("document['" + formName + "']['" +elementName + "']");
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

    public static XPathLocator imageWithAltText(String altText, boolean substringMatch)
    {
        if (substringMatch)
            return xpath("//img[contains(@src, " + xq(altText) + ")]");
        else
            return xpath("//img[@alt=" + xq(altText) + "]");
    }

    public static XPathLocator lookupLink(String schemaName, String queryName, String pkName)
    {
        String linkText = schemaName + "." + queryName + "." + (null != pkName ? pkName : "");
        return Locator.xpath("//span[contains(@class, 'labkey-link') and contains(text(), " + xq(linkText) + ")]");
    }

    /**
     *
     * @param label
     * @param labelColumn Column of label. NOTE: Use java-style 0-based indexes rather than xpath style 1-based
     * @param elementType element to find. Usually, input, select
     * @param elementColumn Column of element. NOTE: Use java-style 0-based indexes rather than xpath style 1-based
     * @return
     */
    public static XPathLocator elementByLabel(String label, int labelColumn, String elementType, int elementColumn)
    {
        //TODO: Escape Label. What is XPATH escaping?
        return xpath("//td[" + (labelColumn + 1) + " and contains(text(), " + xq(label) + ")]/../td[" + (elementColumn + 1) +"]/" + elementType);
    }

    public static XPathLocator inputByLabel(String label, int inputColumn)
    {
        return elementByLabel(label, inputColumn - 1, "input", inputColumn);
    }

    public static XPathLocator permissionSelect(String group)
    {
        return elementByLabel(group, 0, "select", 1);
    }


    public static XPathLocator permissionRendered()
    {
        return xpath("//input[@id='policyRendered']");
    }

    public static XPathLocator permissionButton(String groupName, String role)
    {
        // Supports permission types from a variety of modules.
        return xpath("//td[contains (@id, '$buttons$') and contains (@id, " + xq(role) + ")]//button[contains(text()," + xq(groupName) + ")]");
    }

    public static XPathLocator closePermissionButton(String groupName, String role)
    {
        // Supports permission types from a variety of modules.
        return xpath("//td[contains (@id, '$buttons$') and contains (@id, " + xq(role) + ")]//button[contains(text()," + xq(groupName) + ")]/../../..//i[@class='pclose']");
    }

    public static XPathLocator permissionsInput(String role)
    {
        String inputId = "$add$" + role;
        return xpath("//input[@id="+ xq(inputId) +"]");
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
        return xpath("//li[@class='x-tree-node']/div/a/span[text()='" + schemaName + "']/../../..//span[text()='" + queryName + "']");
    }

    public static XPathLocator permissionsTreeNode(String folderName)
    {
        return xpath("//a[@class='x-tree-node-anchor']/span[text()='" + folderName + "' or text()='" + folderName + "*']");
    }


//    public static XPathLocator fileTreeByPath(String path)
//    {
//        return xpath("//a[@class='x-tree-node-anchor']/span[text()=" + xq(path) + "]");
//    }

    /**
     * Quote text to be used as literal string in xpath expression
     *     Direct port from attibuteValue function in selenium IDE locatorBuilders.js
     * @param value to be quoted
     * @return value with either ' or " around it or assembled from parts
     */
    public static String xq(String value)
    {
        if (value.indexOf("'") < 0) {
            return "'" + value + "'";
        } else if (value.indexOf('"') < 0) {
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
        String path;
        public XPathLocator(String loc)
        {
            super("xpath=" + loc);
            path = loc;
        }

        /**
         * Return nth instance of all matching tags.
         * @param index Use java-style 0-based indexes not xpath style 1-based
         * @return
         */
        public XPathLocator index(int index)
        {
            return new XPathLocator("(" + path + ")[" + (index + 1) + "]");
        }

        public XPathLocator parent()
        {
            return new XPathLocator("(" + path + ")/..");
        }

        public XPathLocator child(String str)
        {
            return new XPathLocator("(" + path + ")/" + str);
        }

        public String getPath()
        {
            return path;
        }
    }
}

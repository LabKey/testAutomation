/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

/**
 * Created by IntelliJ IDEA.
 * User: Mark Igra
 * Date: Feb 8, 2007
 * Time: 10:59:37 AM
 */
public class Locator
{
    private String loc;
    protected Locator(String rawString)
    {
        loc = rawString;
    }

    public String toString()
    {
        return loc;
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


    public static XPathLocator gwtNavButton(String text)
    {
        return xpath("//div[@class='html-face']/a[@class='labkey-button']/span[text() = '" + text + "']");
    }

    public static XPathLocator gwtNavButtonContainingText(String text)
    {
        return xpath("//div[@class='html-face']/a[@class='labkey-button']/span[contains(text(), '" + text + "')]");
    }

    public static XPathLocator gwtNavButton(String text, int index)
    {
        return xpath("(//div[@class='html-face']/a[@class='labkey-button' or @class='labkey-menu-button']/span[text() = '" + text + "'])[" + (index + 1) + "]");
    }

    public static XPathLocator navButton(String text)
    {
        return xpath("//a[@class='labkey-button' or @class='labkey-menu-button']/span[text() = '" + text + "']");
    }

    public static XPathLocator navButtonContainingText(String text)
    {
        return xpath("//a[@class='labkey-button' or @class='labkey-menu-button']/span[contains(text(),  '" + text + "')]");
    }

    public static XPathLocator navButton(String text, int index)
    {
        return xpath("(//a[@class='labkey-button' or @class='labkey-menu-button']/span[text() = '" + text + "'])[" + (index + 1) + "]");
    }

    public static XPathLocator navSubmitButton(String text)
    {
        return xpath("//span[@class='labkey-button' or @class='labkey-menu-button']/input[@type='submit' and @value='" + text + "']");
    }

    public static XPathLocator navSubmitButtonContainingText(String text)
    {
        return xpath("//input[@type='submit' and contains(@value, '" + text + "')]");
    }

    public static XPathLocator navSubmitButton(String text, int index)
    {
        return xpath("(//input[@type='submit' and @value='" + text + "'])[" + (index + 1) + "]");
    }

    public static XPathLocator linkWithImage(String image, int index)
    {
        return xpath("(//a/img[contains(@src, " + xq(image) + ")])[" + (index + 1) + "]");
    }

    public static Locator linkWithText(String text)
    {
        return new Locator("link=" + text);
    }

    public static XPathLocator linkContainingText(String text)
    {
        return xpath("//a[contains(text(), " + xq(text) + ")]");
    }

    public static XPathLocator linkWithText(String text, int index)
    {
        return xpath("(//a[contains(text(), " + xq(text) + ")])[" + (index + 1) + "]");
    }

    public static XPathLocator linkWithTitle(String title)
    {
        return xpath("//a[@title=" + xq(title) + "]");
    }

    public static XPathLocator linkWithHref(String url)
    {
        return xpath("//a[contains(@href, " + xq(url) + ")]");        
    }

    public static XPathLocator bodyLinkWithText(String text)
    {
        return xpath("//td[id()='bodypanel']//a[contains(text(), " + xq(text) + ")]");
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

    public static XPathLocator checkboxByTitle(String title, boolean radio)
    {
        return xpath("//input[@type='" + (radio ? "radio" : "checkbox" )+ "' and @title=" + xq(title) + "]");
    }

    public static XPathLocator checkboxByName(String name, boolean radio)
    {
        return xpath("//input[@type='" + (radio ? "radio" : "checkbox" )+ "' and @name=" + xq(name) + "]");
    }

    public static XPathLocator checkboxByNameAndValue(String name, String value, boolean radio)
    {
        return xpath("//input[@type='" + (radio ? "radio" : "checkbox" )+ "' and @name=" + xq(name) + " and @value=" + xq(value) + "]");
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

        public String getPath()
        {
            return path;
        }
    }

}

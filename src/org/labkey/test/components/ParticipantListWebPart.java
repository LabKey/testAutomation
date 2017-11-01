/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test.components;

import org.labkey.api.util.Pair;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParticipantListWebPart extends BodyWebPart
{
    private final String _participantNounSingular;
    private final String _participantNounPlural;
    private final String _participantNounRegex;

    public ParticipantListWebPart(BaseWebDriverTest test)
    {
        this(test, "Participant", "Participants");
    }

    public ParticipantListWebPart(BaseWebDriverTest test, String participantNounSingular, String participantNounPlural)
    {
        super(test.getDriver(), participantNounSingular + " List");
        _participantNounSingular = participantNounSingular;
        _participantNounPlural = participantNounPlural;
        _participantNounRegex = String.format("(%s|%s)", _participantNounSingular.toLowerCase(), _participantNounPlural.toLowerCase());
        waitForBody();
    }

    protected void waitForBody()
    {
        getWrapper().waitForAnyElement(
                elements().statusMessageLoc.withText(String.format("No %s were found in this study. %s IDs will appear here after specimens or datasets are imported.",
                        _participantNounPlural.toLowerCase(), _participantNounSingular)),
                elements().statusMessageLoc.startsWith("Found "),
                elements().statusMessageLoc.startsWith("Showing all "));
        WebDriverWrapper.sleep(500);
    }

    public List<String> getParticipants()
    {
        return getWrapper().getTexts(Locator.css("li.ptid").findElements(getComponentElement()));
    }

    public Integer getParticipantCount()
    {
        Pair<Integer, Integer> count = getFilteredParticipantCount();
        if (count.getKey() == null)
            return count.getValue();
        else
            return count.getKey();
    }

    /**
     * Parses status message to get filtered participant count
     * @return {x,y} -- Fount x participants of y.
     * {0, null} if no participants are matched by current filter
     * {null, y} if all participants are shown
     */
    public Pair<Integer, Integer> getFilteredParticipantCount()
    {
        String message = getStatusMessage();

        if (message.equals(String.format("No matching %s.", _participantNounPlural))||
                message.startsWith(String.format("No %s IDs contain", _participantNounSingular)))
            return new Pair<>(0, null);

        Pattern messagePattern = Pattern.compile("Found (\\d+) " + _participantNounRegex + " of (\\d+)");
        Matcher messageMatcher = messagePattern.matcher(message);
        if (messageMatcher.find())
            return new Pair<>(Integer.parseInt(messageMatcher.group(1)), Integer.parseInt(messageMatcher.group(3)));

        messagePattern = Pattern.compile("Showing all (\\d+) " + _participantNounRegex);
        messageMatcher = messagePattern.matcher(message);
        if (messageMatcher.find())
            return new Pair<>(null, Integer.parseInt(messageMatcher.group(1)));

        throw new IllegalStateException("Unable to parse participant count: \n" + message);
    }

    public String getStatusMessage()
    {
        return elements().statusMessage.getText();
    }

    public Elements elements()
    {
        return new Elements();
    }

    protected class Elements extends WebPart.Elements
    {
        public Locator.XPathLocator tableLoc = Locator.tagWithText("table", "lk-participants-list-table");
        public Locator.XPathLocator statusMessageLoc = Locator.tag("span").attributeEndsWith("id", ".status");
        public WebElement statusMessage = new LazyWebElement(statusMessageLoc, this);
    }
}

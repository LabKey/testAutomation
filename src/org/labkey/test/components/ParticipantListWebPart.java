/*
 * Copyright (c) 2015 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.util.List;

public class ParticipantListWebPart extends BodyWebPart
{
    private final String _participantNounSingular;
    private final String _participantNounPlural;

    public ParticipantListWebPart(BaseWebDriverTest test)
    {
        this(test, "Participant", "Participants");
    }

    public ParticipantListWebPart(BaseWebDriverTest test, String participantNounSingular, String participantNounPlural)
    {
        super(test, participantNounSingular + " List");
        _participantNounSingular = participantNounSingular;
        _participantNounPlural = participantNounPlural;
        waitForBody();
    }

    protected void waitForBody()
    {
        _test.waitForAnyElement(
                Locator.id("participantsDiv1")
                        .withText(String.format("No %s were found in this study. %s IDs will appear here after specimens or datasets are imported.",
                                _participantNounPlural.toLowerCase(), _participantNounSingular)),
                Locator.id("participantsDiv1").append(Locator.tagWithClass("div", "lk-filter-panel-label").withText("All")));
        _test.sleep(500);
    }

    public List<String> getParticipants()
    {
        return _test.getTexts(Locator.css("li.ptid").findElements(_test.getDriver()));
    }
}

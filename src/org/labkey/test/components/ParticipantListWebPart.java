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

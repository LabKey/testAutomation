package org.labkey.test.components.assay;

import org.labkey.test.Locator;

public class AssayConstants
{
    public static final String PARTICIPANT_VISIT_RESOLVER_FIELD_NAME = "ParticipantVisitResolver";
    public static final String THAW_LIST_TYPE_FIELD_NAME = "ThawListType";
    public static final String TARGET_STUDY_FIELD_NAME = "TargetStudy";

    public static final Locator ASSAY_NAME_FIELD_LOCATOR = Locator.name("Name");
    public static final Locator COMMENTS_FIELD_LOCATOR = Locator.name("Comments");
    public static final Locator TARGET_STUDY_FIELD_LOCATOR = Locator.name(TARGET_STUDY_FIELD_NAME);
    public static final Locator TEXT_AREA_DATA_PROVIDER_LOCATOR = Locator.xpath("//input[@value='textAreaDataProvider']");
    public static final Locator TEXT_AREA_DATA_COLLECTOR_LOCATOR = Locator.textarea("TextAreaDataCollector.textArea");
}

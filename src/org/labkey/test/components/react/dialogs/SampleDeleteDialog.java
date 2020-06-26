package org.labkey.test.components.react.dialogs;

import org.labkey.test.components.experiment.DeleteConfirmationDialog;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.samplemanagement.samples.SampleSetPage;

public class SampleDeleteDialog<SourcePage extends LabKeyPage> extends DeleteConfirmationDialog<SourcePage, SampleSetPage>
{
    public SampleDeleteDialog(SourcePage sourcePage)
    {
        super(sourcePage, () -> {
            if (sourcePage instanceof SampleSetPage)
                return (SampleSetPage) sourcePage;
            else
                return new SampleSetPage(sourcePage);
        });
    }
}

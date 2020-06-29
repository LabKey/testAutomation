package org.labkey.test.components.react.dialogs;

import org.labkey.test.components.experiment.DeleteConfirmationDialog;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.samplemanagement.samples.SampleTypePage;

public class SampleDeleteDialog<SourcePage extends LabKeyPage> extends DeleteConfirmationDialog<SourcePage, SampleTypePage>
{
    public SampleDeleteDialog(SourcePage sourcePage)
    {
        super(sourcePage, () -> {
            if (sourcePage instanceof SampleTypePage)
                return (SampleTypePage) sourcePage;
            else
                return new SampleTypePage(sourcePage);
        });
    }
}

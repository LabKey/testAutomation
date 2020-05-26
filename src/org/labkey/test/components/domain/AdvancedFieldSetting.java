package org.labkey.test.components.domain;

import org.labkey.test.params.FieldDefinition;

import java.util.function.BiConsumer;

public class AdvancedFieldSetting<T>
{
    public static final AdvancedFieldSetting<Boolean> showInDefault =
            new AdvancedFieldSetting<>(AdvancedSettingsDialog::showInDefaultView);
    public static final AdvancedFieldSetting<Boolean> shownInUpdateView =
            new AdvancedFieldSetting<>(AdvancedSettingsDialog::showInUpdateView);
    public static final AdvancedFieldSetting<Boolean> shownInInsertView =
            new AdvancedFieldSetting<>(AdvancedSettingsDialog::showInInsertView);
    public static final AdvancedFieldSetting<Boolean> shownInDetailsView =
            new AdvancedFieldSetting<>(AdvancedSettingsDialog::showInDetailsView);
    public static final AdvancedFieldSetting<FieldDefinition.DefaultType> defaultType =
            new AdvancedFieldSetting<>(AdvancedSettingsDialog::setDefaultValueType);
    public static final AdvancedFieldSetting<Boolean> excludeFromShifting =
            new AdvancedFieldSetting<>(AdvancedSettingsDialog::excludeFromDateShifting);
    public static final AdvancedFieldSetting<Boolean> measure =
            new AdvancedFieldSetting<>(AdvancedSettingsDialog::setMeasure);
    public static final AdvancedFieldSetting<Boolean> dimension =
            new AdvancedFieldSetting<>(AdvancedSettingsDialog::setDimension);
    public static final AdvancedFieldSetting<Boolean> recommendedVariable =
            new AdvancedFieldSetting<>(AdvancedSettingsDialog::setRecommendedVariable);
    public static final AdvancedFieldSetting<Boolean> mvEnabled =
            new AdvancedFieldSetting<>(AdvancedSettingsDialog::setMissingValuesEnabled);
    
    private final BiConsumer<AdvancedSettingsDialog, T> action;

    public AdvancedFieldSetting(BiConsumer<AdvancedSettingsDialog, T> action)
    {
        this.action = action;
    }

    void setValue(AdvancedSettingsDialog dialog, T val)
    {
        action.accept(dialog, val);
    }
}

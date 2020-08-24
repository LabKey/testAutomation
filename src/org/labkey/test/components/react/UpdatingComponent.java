package org.labkey.test.components.react;


public interface UpdatingComponent
{
    public static final UpdatingComponent DEFAULT = Runnable::run;

    void doAndWaitForUpdate(Runnable func);
}

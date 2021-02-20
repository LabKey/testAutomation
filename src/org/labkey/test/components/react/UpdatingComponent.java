package org.labkey.test.components.react;


public interface UpdatingComponent
{
    UpdatingComponent DEFAULT = Runnable::run;

    void doAndWaitForUpdate(Runnable func);
}

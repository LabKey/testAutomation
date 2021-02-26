package org.labkey.test.components;


public interface UpdatingComponent
{
    UpdatingComponent DEFAULT = Runnable::run;

    void doAndWaitForUpdate(Runnable func);
}

package org.labkey.test.components;


public interface UpdatingComponent
{
    UpdatingComponent NO_OP = Runnable::run;

    void doAndWaitForUpdate(Runnable func);
}

package org.labkey;

import java.io.File;
import java.io.IOException;

public class AssayTransformWarning extends AssayTransform
{
    public static void main(String[] args)
    {
        AssayTransform.main(args);
    }

    public void runTransform(File inputFile, String username, String password, String host)
    {
        super.runTransform(inputFile, username, password, host);

        setMaxSeverity(1);
        try
        {
            writeWarnings();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}

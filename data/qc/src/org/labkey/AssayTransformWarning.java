package org.labkey;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class AssayTransformWarning extends AbstractAssayValidator
{
    public static void main(String[] args)
    {
        if (args.length < 4)
            throw new IllegalArgumentException("Input data file not passed in");

        File runProperties = new File(args[0]);
        if (runProperties.exists())
        {
            AssayTransformWarning transform = new AssayTransformWarning();

            transform.runTransform(runProperties, args[1], args[2], args[3]);
        }
        else
            throw new IllegalArgumentException("Input data file does not exist");
    }

    public void runTransform(File inputFile, String username, String password, String host)
    {
        setEmail(username);
        setPassword(password);
        setHost(host);
        parseRunProperties(inputFile);

        identityTransform();

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

    private void identityTransform()
    {
        try
        {
            if (getRunProperties().containsKey(Props.runDataFile.name()))
            {
                File inputFile = new File(getRunProperty(Props.runDataUploadedFile));
                File transformFile = new File(getTransformFile().get(getRunProperty(Props.runDataFile)));

                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(transformFile))))
                {
                    try (BufferedReader reader = new BufferedReader(new FileReader(inputFile)))
                    {
                        String line;
                        while ((line = reader.readLine()) != null)
                        {
                            writer.println(line);
                        }
                    }
                }
            }
            else
                writeError("Unable to locate the runDataFile", "runDataFile");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}

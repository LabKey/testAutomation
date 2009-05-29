package org.labkey;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: May 29, 2009
 */
public class AssayTransform extends AbstractAssayValidator
{
    public static void main(String[] args)
    {
        if (args.length < 4)
            throw new IllegalArgumentException("Input data file not passed in");

        File runProperties = new File(args[0]);
        if (runProperties.exists())
        {
            AssayTransform transform = new AssayTransform();

            transform.runTransform(runProperties, args[1], args[2], args[3]);
        }
        else
            throw new IllegalArgumentException("Input data file does not exist");
    }

    public void runTransform(File inputFile, String username, String password, String host)
    {
        try {
            setEmail(username);
            setPassword(password);
            setHost(host);
            parseRunProperties(inputFile);

            if (getRunProperties().containsKey(Props.runDataFile.name()))
            {
                String runDataFile = getRunProperties().get(Props.runDataFile.name());
                List<Map<String, String>> dataMap = parseRunData(new File(runDataFile));
                File transformFile = new File(getTransformFile().get(runDataFile));

                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(transformFile)));

                try {
                    String[] animals = {"bird", "dog", "horse", "monkey", "hamster", "pig", "goat"};
                    Map<String, String> ptidMap = new HashMap<String, String>();
                    List<String> columns = new ArrayList<String>();
                    int idx = 0;

                    // transform the data, adding a new column
                    for (Map<String, String> row : dataMap)
                    {
                        StringBuilder sb = new StringBuilder();
                        if (columns.isEmpty())
                        {
                            for (String col : row.keySet())
                            {
                                columns.add(col);

                                sb.append(col);
                                sb.append('\t');
                            }
                            sb.append("Animal");
                            sb.append('\n');
                            pw.write(sb.toString());
                        }
                        else
                        {
                            String ptid = row.get("participantid");
                            if (!ptidMap.containsKey(ptid))
                                ptidMap.put(ptid, ptid);
                            else
                                writeError("A duplicate PTID was discovered : " + ptid, "runDataFile");

                            for (String col : columns)
                            {
                                String value = row.get(col);

                                if (value != null)
                                    sb.append(row.get(col));
                                sb.append('\t');
                            }
                            sb.append(animals[idx % 7]);
                            sb.append('\n');
                            pw.write(sb.toString());
                            idx++;
                        }
                    }
                    insertLog("Programmatic Data Transform was run and " + getErrors().size() + " errors were found");
                }
                finally
                {
                    pw.close();
                }
            }
            else
                writeError("Unable to locate the runDataFile", "runDataFile");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

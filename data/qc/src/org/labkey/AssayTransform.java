package org.labkey;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
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

            String type = getRunProperties().get(Props.assayType.name());

            switch(type)
            {
                case "General":
                    runGPATTest();
                    break;
                case "TZM-bl Neutralization (NAb)":
                    runNAbTest();
                    break;
                case "Luminex":
                    runLuminexTest();
                    break;
                case "Viability":
                    runViabilityTest();
                    break;
                case "ELISpot":
                    runElispotTest();
                    break;
                case "TZM-bl Neutralization (NAb), High-throughput (Single Plate Dilution)":
                case "TZM-bl Neutralization (NAb), High-throughput (Cross Plate Dilution)":
                    runHTNAbTest();
                    break;
                case "Noblis Simple":
                    runFileBasedAssayTest();
                    break;
                default:
                    throw new IllegalArgumentException("Test does not exist for assay type: " + type);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void runGPATTest()
    {
        try {
            if (getRunProperties().containsKey(Props.runDataFile.name()))
            {
                String runDataFile = getRunProperties().get(Props.runDataFile.name());
                List<Map<String, String>> dataMap = parseRunData(new File(runDataFile));
                File transformFile = new File(getTransformFile().get(runDataFile));

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(transformFile))))
                {
                    String[] animals = {"bird", "dog", "horse", "monkey", "hamster", "pig", "goat"};
                    Map<String, String> ptidMap = new HashMap<>();
                    List<String> columns = new ArrayList<>();
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
            }
            else
                writeError("Unable to locate the runDataFile", "runDataFile");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void runNAbTest()
    {
        try {
            if (getRunProperties().containsKey(Props.transformedRunPropertiesFile.name()))
            {
                String runPropertiesFile = getRunProperties().get(Props.transformedRunPropertiesFile.name());
                File transformFile = new File(runPropertiesFile);

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(transformFile))))
                {
                    pw.write("FileID" + "\t" + "transformed FileID\n");
                }
            }
            if (getRunProperties().containsKey(Props.runDataFile.name()))
            {
                String runDataFile = getRunProperties().get(Props.runDataFile.name());
                List<Map<String, String>> dataMap = parseRunData(new File(runDataFile));
                File transformFile = new File(getTransformFile().get(runDataFile));

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(transformFile))))
                {
                    // transform the fit error column
                    StringBuilder sb = new StringBuilder();
                    boolean header = true;

                    // transform the data, adding a new column
                    for (Map<String, String> row : dataMap)
                    {
                        sb.setLength(0);
                        String delim = "";

                        for (Map.Entry<String, String> entry : row.entrySet())
                        {
                            sb.append(delim);
                            if (header)
                                sb.append(entry.getValue());
                            else
                            {
                                if ("Fit Error".equalsIgnoreCase(entry.getKey()))
                                    sb.append("0.0");
                                else
                                {
                                    String value = entry.getValue();
                                    if (value != null)
                                        sb.append(value);
                                }
                            }
                            delim = "\t";
                        }
                        header = false;
                        sb.append('\n');
                        pw.write(sb.toString());
                    }
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

    private void runHTNAbTest()
    {
        try {
            if (getRunProperties().containsKey(Props.transformedRunPropertiesFile.name()))
            {
                String runPropertiesFile = getRunProperties().get(Props.transformedRunPropertiesFile.name());
                File transformFile = new File(runPropertiesFile);

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(transformFile))))
                {
                    pw.write("FileID" + "\t" + "transformed FileID\n");
                }
            }
            if (getRunProperties().containsKey(Props.runDataFile.name()))
            {
                String runDataFile = getRunProperties().get(Props.runDataFile.name());
                List<Map<String, String>> dataMap = parseRunData(new File(runDataFile));
                File transformFile = new File(getTransformFile().get(runDataFile));

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(transformFile))))
                {
                    // transform the fit error column
                    StringBuilder sb = new StringBuilder();
                    boolean header = true;

                    // transform the data, adding a new column
                    for (Map<String, String> row : dataMap)
                    {
                        sb.setLength(0);
                        String delim = "";

                        for (Map.Entry<String, String> entry : row.entrySet())
                        {
                            sb.append(delim);
                            if (header)
                                sb.append(entry.getValue());
                            else
                            {
                                if ("Fit Error".equalsIgnoreCase(entry.getKey()))
                                    sb.append("0.0");
                                else
                                {
                                    String value = entry.getValue();
                                    if (value != null)
                                        sb.append(value);
                                }
                            }
                            delim = "\t";
                        }
                        header = false;
                        sb.append('\n');
                        pw.write(sb.toString());
                    }
                }
            }
            else
             {
                 writeError("Unable to locate the runDataFile", "runDataFile");

                 // Dump all properties for debugging purposes
                 for (Map.Entry entry : getRunProperties().entrySet())
                 {
                     String key = entry.getKey().toString();
                     String value = entry.getValue().toString();
                     writeError(key + " : " + value, key);
                 }
             }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void runLuminexTest()
    {
        try {
            if (getRunProperties().containsKey(Props.runDataFile.name()))
            {
                String runDataFile = getRunProperties().get(Props.runDataFile.name());
                List<Map<String, String>> dataMap = parseRunData(new File(runDataFile));
                File transformFile = new File(getTransformFile().get(runDataFile));

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(transformFile))))
                {
                    // transform the fit error column
                    List<String> columns = new ArrayList<>();
                    StringBuilder sb = new StringBuilder();
                    boolean header = true;

                    // transform the data, adding a new column
                    for (Map<String, String> row : dataMap)
                    {
                        sb.setLength(0);
                        String delim = "";

                        for (Map.Entry<String, String> entry : row.entrySet())
                        {
                            sb.append(delim);
                            if (header)
                                sb.append(entry.getValue());
                            else
                            {
                                if ("Description".equalsIgnoreCase(entry.getKey()))
                                    sb.append("Transformed");
                                else
                                {
                                    String value = entry.getValue();
                                    if (value != null)
                                        sb.append(value);
                                }
                            }
                            delim = "\t";
                        }
                        header = false;
                        sb.append('\n');
                        pw.write(sb.toString());
                    }
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

    private void runViabilityTest()
    {
        try {
            if (getRunProperties().containsKey(Props.runDataFile.name()))
            {
                String runDataFile = getRunProperties().get(Props.runDataFile.name());
                List<Map<String, String>> dataMap = parseRunData(new File(runDataFile));
                File transformFile = new File(getTransformFile().get(runDataFile));

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(transformFile))))
                {
                    // transform the fit error column
                    List<String> columns = new ArrayList<>();
                    StringBuilder sb = new StringBuilder();
                    boolean header = true;

                    // transform the data, adding a new column
                    for (Map<String, String> row : dataMap)
                    {
                        sb.setLength(0);
                        String delim = "";

                        for (Map.Entry<String, String> entry : row.entrySet())
                        {
                            sb.append(delim);
                            if (header)
                                sb.append(entry.getValue());
                            else
                            {
                                if ("SpecimenIDs".equalsIgnoreCase(entry.getKey()))
                                    sb.append("Transformed");
                                else
                                {
                                    String value = entry.getValue();
                                    if (value != null)
                                        sb.append(value);
                                }
                            }
                            delim = "\t";
                        }
                        header = false;
                        sb.append('\n');
                        pw.write(sb.toString());
                    }
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

    private void runElispotTest()
    {
        try {
            if (getRunProperties().containsKey(Props.runDataFile.name()))
            {
                String runDataFile = getRunProperties().get(Props.runDataFile.name());
                List<Map<String, String>> dataMap = parseRunData(new File(runDataFile));
                File transformFile = new File(getTransformFile().get(runDataFile));

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(transformFile))))
                {
                    // transform the spot count column
                    StringBuilder sb = new StringBuilder();
                    boolean header = true;

                    // transform the data, adding a new column
                    for (Map<String, String> row : dataMap)
                    {
                        if (header)
                            row.put("CustomElispotColumn", "CustomElispotColumn");
                        else
                            row.put("CustomElispotColumn", "transformed!");

                        sb.setLength(0);
                        String delim = "";

                        for (Map.Entry<String, String> entry : row.entrySet())
                        {
                            sb.append(delim);
                            if (header)
                                sb.append(entry.getValue());
                            else
                            {
                                if ("SpotCount".equalsIgnoreCase(entry.getKey()))
                                    sb.append("747.7");
                                else
                                {
                                    String value = entry.getValue();
                                    if (value != null)
                                        sb.append(value);
                                }
                            }
                            delim = "\t";
                        }
                        header = false;
                        sb.append('\n');
                        pw.write(sb.toString());
                    }
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

    private void runFileBasedAssayTest()
    {
        try
        {
            if (getRunProperties().containsKey(Props.runDataFile.name()))
            {
                String runDataFile = getRunProperties().get(Props.runDataFile.name());
                List<Map<String, String>> dataMap = parseRunData(new File(runDataFile));
                File transformFile = new File(getTransformFile().get(runDataFile));

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(transformFile))))
                {
                    List<String> columns = new ArrayList<>();

                    // write out transformed data
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
                            sb.append('\n');
                            pw.write(sb.toString());
                        }
                        else
                        {
                            for (String col : columns)
                            {
                                String value = row.get(col);
                                if (col.equalsIgnoreCase("HiddenData"))
                                    value = value + " transformed";

                                if (value != null)
                                    sb.append(value);
                                sb.append('\t');
                            }
                            sb.append('\n');
                            pw.write(sb.toString());
                        }
                    }
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

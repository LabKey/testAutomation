package org.labkey;

import org.labkey.remoteapi.sas.NetrcFileParser;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.InsertRowsCommand;

import java.io.*;
import java.util.*;

/**
 * User: klum
 * Date: May 29, 2009
 */
public abstract class AbstractAssayValidator
{
    private String _email;
    private String _password;
    private File _errorFile;
    private Map<String, String> _runProperties = new HashMap<String, String>();
    private Map<String, String> _transformFile = new HashMap<String, String>();
    private List<String> _errors = new ArrayList<String>();
    private String _host;

    public enum Props {
        assayId,                // the assay id from the run properties field
        runComments,            // run properties comments
        containerPath,
        assayType,              // assay definition name : general, nab, elispot etc.
        assayName,              // assay instance name
        userName,               // user email
        workingDir,             // temp directory that the script will be executed from
        protocolId,             // protocol row id
        protocolLsid,
        protocolDescription,
        runDataFile,
        runDataUploadedFile,
        errorsFile,
        transformedRunPropertiesFile,
    }

    public String getEmail() {
        return _email;
    }

    public String getPassword() {
        return _password;
    }

    public File getErrorFile() {
        return _errorFile;
    }

    public void setErrorFile(File errorFile) {
        _errorFile = errorFile;
    }

    public Map<String, String> getRunProperties() {
        return _runProperties;
    }

    public List<String> getErrors() {
        return _errors;
    }

    public void setEmail(String _email) {
        this._email = _email;
    }

    public void setPassword(String _password) {
        this._password = _password;
    }

    public void setHost(String _host) {
        this._host = _host;
    }

    public String getHost() {
        return _host;
    }

    public Map<String, String> getTransformFile() {
        return _transformFile;
    }

    protected void insertLog(String comment) throws Exception
    {
        Connection con = new Connection(getHost(), getEmail(), getPassword());
        InsertRowsCommand cmd = new InsertRowsCommand("lists", "QC Log");

        Map<String, Object> row = new HashMap<String,Object>();
        row.put("Date", new Date());
        row.put("Container", getRunProperties().get(Props.containerPath.name()));
        row.put("AssayId", getRunProperties().get(Props.assayId.name()));
        row.put("AssayName", getRunProperties().get(Props.assayName.name()));
        row.put("User", getRunProperties().get(Props.userName.name()));
        row.put("Comments", comment);

        cmd.addRow(row);
        cmd.execute(con, getRunProperties().get(Props.containerPath.name()));
    }

    protected void writeError(String message, String prop) throws IOException
    {
        if (_errorFile != null)
        {
            _errors.add(message);

            StringBuilder sb = new StringBuilder();
            sb.append("error\t");
            sb.append(prop);
            sb.append('\t');
            sb.append(message);
            sb.append('\n');

            try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(_errorFile, true))))
            {
                pw.write(sb.toString().replaceAll("\\\\", "\\\\\\\\"));
            }
        }
        else
            throw new RuntimeException("Errors file does not exist");
    }

    protected void parseRunProperties(File runProperties)
    {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(runProperties));
            String l;
            while ((l = br.readLine()) != null)
            {
                System.out.println(l);
                String[] parts = l.split("\t");
                _runProperties.put(parts[0], parts[1]);

                if (Props.runDataFile.name().equals(parts[0]) && parts.length >= 4)
                {
                    _transformFile.put(parts[1], parts[3]);
                }
            }
            if (_runProperties.containsKey(Props.errorsFile.name()))
                setErrorFile(new File(_runProperties.get(Props.errorsFile.name())));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
        finally
        {
            if (br != null)
                try {br.close();} catch(IOException ioe) {}
        }
    }

    /**
     * Parse the tab-delimitted input data file
     */
    protected List<Map<String, String>> parseRunData(File data)
    {
        BufferedReader br = null;
        Map<Integer, String> columnMap = new LinkedHashMap<Integer, String>();
        List<Map<String, String>> dataMap = new ArrayList<Map<String, String>>();

        try {
            br = new BufferedReader(new FileReader(data));
            String l;
            boolean isHeader = true;
            while ((l = br.readLine()) != null)
            {
                if (isHeader)
                {
                    int i=0;
                    for (String col : l.split("\t"))
                        columnMap.put(i++, col.toLowerCase());
                    isHeader = false;
                }
                dataMap.add(parseDataRow(l, columnMap));
            }
            return dataMap;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
        finally
        {
            if (br != null)
                try {br.close();} catch(IOException ioe) {}
        }
    }

    protected Map<String, String> parseDataRow(String row, Map<Integer, String> columnMap)
    {
        Map<String, String> props = new LinkedHashMap<String, String>();
        int i=0;
        for (String col : row.split("\t"))
        {
            props.put(columnMap.get(i), col);
            i++;
        }
        return props;
    }

    protected void setCredentials(String host) throws IOException
    {
        NetrcFileParser parser = new NetrcFileParser();
        NetrcFileParser.NetrcEntry entry = parser.getEntry(host);

        if (null != entry)
        {
            _email = entry.getLogin();
            _password = entry.getPassword();
        }
    }
}

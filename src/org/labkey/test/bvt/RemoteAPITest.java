package org.labkey.test.bvt;

import junit.framework.Assert;
import org.junit.*;
import org.labkey.remoteapi.client.*;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.Cleanable;
import org.labkey.test.WebTestHelper;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.*;

public class RemoteAPITest implements Cleanable
{
    public static final String PROJECT_NAME = "RemoteAPIUnitTestFolder";
    private static final String SIMPLE_RUN_NAME = "RemoteAPI Test Run";
    private static final String STANDARD_EXPERIMENT_NAME = "RemoteAPI test";
    private static final String STANDARD_EXPERIMENT_LSID = "urn:lsid:cpas.fhcrc.org:Experiment.RemoteAPITest:Experiment:001";
    private static final String OTHER_EXPERIMENT_NAME = "Other experiment";
    private static final String OTHER_EXPERIMENT_LSID = "urn:lsid:cpas.fhcrc.org:Experiment.RemoteAPITest:Experiment:002";
    private static final String FRACTIONATION_RUN_NAME = "RemoteAPI Fractionation";
    private static final String QUERY_TEST_PROTOCOL_LSID = "urn:lsid:cpas.fhcrc.org:Protocol.RemoteAPITest:QueryTestProtocol";
    private static final String PRIVATE_EXPERIMENT_LSID = "urn:lsid:cpas.fhcrc.org:Experiment.RemoteAPITest:Experiment:003";

    private LabKeyService _service;
    private LabKeyService _authenticatedService;
    public static final String PUBLIC_SUBFOLDER_NAME = "publicSubfolder";
    public static final String PRIVATE_SUBFOLDER_NAME = "privateSubfolder";
    private static final String PRIVATE_EXPERIMENT_RUN_LSID = "urn:lsid:cpas.fhcrc.org:Experiment.RemoteAPITest:ExperimentRun:001";
    private static final String PRIVATE_PROTOCOL_LSID = "urn:lsid:cpas.fhcrc.org:Protocol.RemoteAPITest:PrivateProtocol";

    private static final RemoteAPIExperimentLoader LOADER = new RemoteAPIExperimentLoader();

    private static boolean skipSetupAndCleanup()
    {
        return Boolean.getBoolean("remoteAPI.skipSetup");
    }

    @Before
    public void standardSetup() throws Exception
    {
        _service = LabKeyServiceFactory.createService(WebTestHelper.getBaseURL());
        _authenticatedService = LabKeyServiceFactory.createService(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    @After
    public void standardCleanup() throws Exception
    {
        _service = null;
        _authenticatedService = null;
    }

    @BeforeClass
    public static void oneTimeSetup() throws Exception
    {
        LOADER.initialize(skipSetupAndCleanup());
    }

    public void cleanup() throws Exception
    {
        oneTimeCleanup();
    }

    @AfterClass
    public static void oneTimeCleanup() throws Exception
    {
        if (skipSetupAndCleanup())
        {
            return;
        }

        LOADER.cleanup();
    }

    private Folder getProject() throws RemoteException
    {
        FolderQuery query = _service.createFolderQuery("/");
        query.setName(PROJECT_NAME);
        List<? extends Folder> folders = _service.getFolders(query);
        Assert.assertEquals(1, folders.size());
        Folder folder = folders.get(0);
        Assert.assertEquals("Wrong folder name", PROJECT_NAME, folder.getName());
        Assert.assertEquals("Wrong parent path", "/", folder.getParentPath());
        return folder;
    }

    private List<? extends ExperimentRun> getNameFilteredExperimentRuns(Experiment exp, String name, int expectedResults) throws RemoteException
    {
        ExperimentRunQuery runQuery = exp.createExperimentRunQuery();
        runQuery.setName(name);
        List<? extends ExperimentRun> runs = exp.getExperimentRuns(runQuery);
        Assert.assertEquals("Wrong number of matching experiment runs", expectedResults, runs.size());
        return runs;
    }

    private List<? extends Experiment> getFilteredExperiments(Folder folder, String name, String lsid, int expectedResults) throws RemoteException
    {
        ExperimentQuery query = folder.createExperimentQuery();
        query.setName(name);
        query.setLsid(lsid);
        List<? extends Experiment> runs = folder.getExperiments(query);
        Assert.assertEquals("Wrong number of matching experiments", expectedResults, runs.size());
        return runs;
    }

    private Experiment getSimpleExperiment() throws RemoteException
    {
        Folder folder = getProject();
        ExperimentQuery query = folder.createExperimentQuery();
        query.setName(STANDARD_EXPERIMENT_NAME);
        List<? extends Experiment> experiments = folder.getExperiments(query);
        Assert.assertEquals("Wrong number of experiments", 1, experiments.size());
        return experiments.get(0);
    }

    private Experiment getOtherExperiment() throws RemoteException
    {
        Folder folder = getProject();
        ExperimentQuery query = folder.createExperimentQuery();
        query.setLsid(OTHER_EXPERIMENT_LSID);
        List<? extends Experiment> experiments = folder.getExperiments(query);
        Assert.assertEquals("Wrong number of experiments", 1, experiments.size());
        return experiments.get(0);
    }

    @Test
    public void testProperties() throws RemoteException
    {
        Experiment experiment = getOtherExperiment();
        PropertyCollection properties = experiment.getProperties();
        List<? extends SimpleValue> simpleValues = properties.getSimpleValues();
        Assert.assertEquals(5, simpleValues.size());
        validateSimpleValuePresent(simpleValues, "StringProperty", "StringPropertyOntologyEntryURI", SimpleType.STRING, "A nice property value");
        validateSimpleValuePresent(simpleValues, "IntegerProperty", "IntegerPropertyOntologyEntryURI", SimpleType.INTEGER, new Integer(158));
        validateSimpleValuePresent(simpleValues, "FileLinkProperty", "FileLinkPropertyOntologyEntryURI", SimpleType.FILE_LINK, "/path/to/propertyFile");
        GregorianCalendar cal = new GregorianCalendar(2006, GregorianCalendar.OCTOBER, 12, 0, 0, 0);
        validateSimpleValuePresent(simpleValues, "DateTimeProperty", "DateTimePropertyOntologyEntryURI", SimpleType.DATE_TIME, cal.getTime());
        validateSimpleValuePresent(simpleValues, "DoubleProperty", "DoublePropertyOntologyEntryURI", SimpleType.DOUBLE, new Double("453.718"));

        List<? extends PropertyObject> firstChildren = properties.getPropertyObjects();
        Assert.assertEquals(1, firstChildren.size());
        PropertyObject firstChild = firstChildren.get(0);
        validateSimpleValue(firstChild, SimpleType.PROPERTY_URI, "", "FirstChildPropertyOntologyEntryURI");

        PropertyCollection childCollection = firstChild.getChildProperties();
        List<? extends SimpleValue> childSimpleValues = childCollection.getSimpleValues();
        Assert.assertEquals(1, childSimpleValues.size());
        validateSimpleValuePresent(childSimpleValues, "FirstChildStringProperty", "FirstChildStringPropertyOntologyEntryURI", SimpleType.STRING, "A nice first child property value");

        List<? extends PropertyObject> secondChildren = childCollection.getPropertyObjects();
        Assert.assertEquals(1, secondChildren.size());
        PropertyObject secondChild = secondChildren.get(0);
        validateSimpleValue(secondChild, SimpleType.PROPERTY_URI, "", "SecondChildPropertyOntologyEntryURI");

        PropertyCollection secondChildCollection = secondChild.getChildProperties();
        List<? extends SimpleValue> secondChildSimpleValues = secondChildCollection.getSimpleValues();
        Assert.assertEquals(1, secondChildSimpleValues.size());
        validateSimpleValuePresent(secondChildSimpleValues, "SecondChildStringProperty", "SecondChildStringPropertyOntologyEntryURI", SimpleType.STRING, "A nice second child property value");
    }

    @Test(expected = RemoteException.class)
    public void testWildcardParentPath() throws RemoteException
    {
        FolderQuery query = _service.createFolderQuery("/*");
        _service.getFolders(query);
    }

    private ExperimentRun getSimpleExperimentRun() throws RemoteException
    {
        Experiment exp = getSimpleExperiment();
        return getNameFilteredExperimentRuns(exp, SIMPLE_RUN_NAME, 1).get(0);
    }

    private ExperimentRun getFractionationExperimentRun() throws RemoteException
    {
        Experiment exp = getSimpleExperiment();
        return getNameFilteredExperimentRuns(exp, FRACTIONATION_RUN_NAME, 1).get(0);
    }

    @Test
    public void testExperimentRunFilter() throws RemoteException
    {
        Experiment exp = getSimpleExperiment();
        Assert.assertEquals(2, exp.getExperimentRuns().size());
        getNameFilteredExperimentRuns(exp, "Garbage no wildcards", 0);
        getNameFilteredExperimentRuns(exp, "Garbage * wildcard", 0);
        getNameFilteredExperimentRuns(exp, "*", 2);
        List<? extends ExperimentRun> runs = getNameFilteredExperimentRuns(exp, SIMPLE_RUN_NAME, 1);
        Assert.assertEquals(SIMPLE_RUN_NAME, runs.get(0).getName());
        runs = getNameFilteredExperimentRuns(exp, SIMPLE_RUN_NAME + "*", 1);
        Assert.assertEquals(SIMPLE_RUN_NAME, runs.get(0).getName());
        runs = getNameFilteredExperimentRuns(exp, "*" + SIMPLE_RUN_NAME, 1);
        Assert.assertEquals(SIMPLE_RUN_NAME, runs.get(0).getName());
        runs = getNameFilteredExperimentRuns(exp, "*" + SIMPLE_RUN_NAME + "*", 1);
        Assert.assertEquals(SIMPLE_RUN_NAME, runs.get(0).getName());

        ExperimentRunQuery query = exp.createExperimentRunQuery();
        query.setLsid("*:RemoteAPITestRun");
        Assert.assertEquals(1, exp.getExperimentRuns(query).size());

        query = exp.createExperimentRunQuery();
        query.setComments("This is a simple run");
        Assert.assertEquals(1, exp.getExperimentRuns(query).size());

        query = exp.createExperimentRunQuery();
        query.setName(SIMPLE_RUN_NAME);
        Assert.assertEquals(1, exp.getExperimentRuns(query).size());
    }

    @Test
    public void testProtocolFilter() throws RemoteException
    {
        Folder folder = getProject();

        Assert.assertTrue(folder.getProtocols().size() > 1);

        ProtocolQuery query = folder.createProtocolQuery();
        query.setLsid(QUERY_TEST_PROTOCOL_LSID);
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setProtocolDescription("Query test protocol");
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setApplicationType("QueryTestType");
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setInstrument("QueryTestInstrument");
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setMaxInputDataPerInstance(501);
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setMaxInputMaterialPerInstance(500);
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setName("*MAGIC_STRING*");
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setOutputDataPerInstance(503);
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setOutputMaterialPerInstance(502);
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setOutputDataType("QueryTestDataType");
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setOutputMaterialType("QueryTestMaterialType");
        Assert.assertEquals(1, folder.getProtocols(query).size());

        query = folder.createProtocolQuery();
        query.setSoftware("QueryTestSoftware");
        Assert.assertEquals(1, folder.getProtocols(query).size());
    }

    @Test
    public void testFolderFilter() throws RemoteException
    {
        FolderQuery query = _service.createFolderQuery("/");
        query.setName(PROJECT_NAME + "Garbage");
        Assert.assertEquals(0, _service.getFolders(query).size());

        query = _service.createFolderQuery("/GarbageFakePathNoMatch");
        Assert.assertEquals(0, _service.getFolders(query).size());

        query = _service.createFolderQuery("/");
        List<? extends Folder> children = _service.getFolders(query);
        Assert.assertTrue(children.size() >= 1);
        boolean foundRemoteAPIFolder = false;
        for (Folder child : children)
        {
            if (child.getName().equals(PROJECT_NAME))
            {
                foundRemoteAPIFolder = true;
                break;
            }
        }
        Assert.assertTrue(foundRemoteAPIFolder);

        Folder folder = getProject();
        children = folder.getChildren();
        Assert.assertEquals(1, children.size());
        Folder publicSubfolder = children.get(0);
        Assert.assertEquals(PUBLIC_SUBFOLDER_NAME, publicSubfolder.getName());
        Assert.assertEquals("/" + PROJECT_NAME, publicSubfolder.getParentPath());

        Assert.assertEquals(0, publicSubfolder.getChildren().size());

        query = _service.createFolderQuery("/" + PROJECT_NAME);
        query.setName(PRIVATE_SUBFOLDER_NAME);
        Assert.assertEquals(0, _service.getFolders(query).size());

        query = _authenticatedService.createFolderQuery("/" + PROJECT_NAME);
        query.setName(PRIVATE_SUBFOLDER_NAME);
        Assert.assertEquals(1, _authenticatedService.getFolders(query).size());

    }

    @Test(expected = RemoteException.class)
    public void testParentlessFolderRequest() throws RemoteException
    {
        FolderQuery query = _service.createFolderQuery(null);
        query.setName(PROJECT_NAME + "Garbage");
        _service.getFolders(query);
    }

    @Test(expected = RemoteException.class)
    public void testWildcardParentPathFolderRequest() throws RemoteException
    {
        FolderQuery query = _service.createFolderQuery("/*");
        _service.getFolders(query);
    }

    @Test(expected = RemoteException.class)
    public void testContainerlessExperimentRequest() throws RemoteException
    {
        ExperimentQuery query = _service.createExperimentQuery(null);
        _service.getExperiments(query);
    }

    @Test
    public void testParameterDeclarations() throws RemoteException
    {
        ProtocolQuery query = _service.createProtocolQuery("/" + PROJECT_NAME);
        query.setLsid(QUERY_TEST_PROTOCOL_LSID);
        List<? extends Protocol> protocols = _service.getProtocols(query);
        Assert.assertEquals(1, protocols.size());

        Protocol protocol = protocols.get(0);
        List<? extends SimpleValue> paramDecs = protocol.getParameterDeclarations();
        Assert.assertEquals(5, paramDecs.size());

        validateSimpleValuePresent(paramDecs, "StringParamDec", "StringOntologyEntryURI", SimpleType.STRING, "A nice value");
        validateSimpleValuePresent(paramDecs, "IntegerParamDec", "IntegerOntologyEntryURI", SimpleType.INTEGER, new Integer(10));
        validateSimpleValuePresent(paramDecs, "FileLinkParamDec", "FileLinkOntologyEntryURI", SimpleType.FILE_LINK, "/path/to/file");
        GregorianCalendar cal = new GregorianCalendar(2006, GregorianCalendar.OCTOBER, 11, 0, 0, 0);
        validateSimpleValuePresent(paramDecs, "DateTimeParamDec", "DateTimeOntologyEntryURI", SimpleType.DATE_TIME, cal.getTime());
        validateSimpleValuePresent(paramDecs, "DoubleParamDec", "DoubleOntologyEntryURI", SimpleType.DOUBLE, new Double("3.78"));
    }

    private void validateSimpleValuePresent(List<? extends SimpleValue> values, String name, String ontologyEntryURI, SimpleType type, Object value)
    {
        SimpleValue simpleValue = findSimpleValueByName(name, values);
        validateSimpleValue(simpleValue, type, value, ontologyEntryURI);
    }

    private void validateSimpleValue(SimpleValue simpleValue, SimpleType type, Object value, String ontologyEntryURI)
    {
        Assert.assertEquals(type, simpleValue.getValueType());
        Assert.assertEquals(value, simpleValue.getValue());
        Assert.assertEquals(ontologyEntryURI, simpleValue.getOntologyEntryURI());
    }

    @Test
    public void testExperimentContact() throws RemoteException
    {
        Experiment experiment = getOtherExperiment();
        Contact contact = experiment.getContact();
        Assert.assertEquals("None", contact.getContactId());
        Assert.assertEquals("me@nowhere.com", contact.getEmail());
        Assert.assertEquals("Some", contact.getFirstName());
        Assert.assertEquals("Guy", contact.getLastName());
    }

    @Test(expected = RemoteException.class)
    public void testNoPermissionsPrivateExperiment() throws RemoteException
    {
        ExperimentQuery query = _service.createExperimentQuery(PROJECT_NAME + "/" + PRIVATE_SUBFOLDER_NAME);
        query.setLsid(PRIVATE_EXPERIMENT_LSID);
        _service.getExperiments(query);
    }

    @Test
    public void testPrivateExperiment() throws RemoteException
    {
        ExperimentQuery query = _authenticatedService.createExperimentQuery(PROJECT_NAME + "/" + PRIVATE_SUBFOLDER_NAME);
        query.setLsid(PRIVATE_EXPERIMENT_LSID);
        List<? extends Experiment> experiments = _authenticatedService.getExperiments(query);
        Assert.assertEquals(1, experiments.size());
    }

    @Test(expected = RemoteException.class)
    public void testNoPermissionsPrivateExperimentRun() throws RemoteException
    {
        ExperimentRunQuery query = _service.createExperimentRunQuery(PRIVATE_EXPERIMENT_LSID);
        query.setLsid(PRIVATE_EXPERIMENT_RUN_LSID);
        _service.getExperimentRuns(query);
    }

    @Test
    public void testPrivateExperimentRun() throws RemoteException
    {
        ExperimentRunQuery query = _authenticatedService.createExperimentRunQuery(PRIVATE_EXPERIMENT_LSID);
        query.setLsid(PRIVATE_EXPERIMENT_RUN_LSID);
        List<? extends ExperimentRun> runs = _authenticatedService.getExperimentRuns(query);
        Assert.assertEquals(1, runs.size());
    }

    @Test(expected = RemoteException.class)
    public void testNoPermissionsPrivateProtocol() throws RemoteException
    {
        ProtocolQuery query = _service.createProtocolQuery(PROJECT_NAME + "/" + PRIVATE_SUBFOLDER_NAME);
        query.setLsid(PRIVATE_PROTOCOL_LSID);
        _service.getProtocols(query);
    }

    @Test
    public void testPrivateProtocol() throws RemoteException
    {
        ProtocolQuery query = _authenticatedService.createProtocolQuery(PROJECT_NAME + "/" + PRIVATE_SUBFOLDER_NAME);
        query.setLsid(PRIVATE_PROTOCOL_LSID);
        List<? extends Protocol> protocols = _authenticatedService.getProtocols(query);
        Assert.assertEquals(1, protocols.size());
    }

    @Test
    public void testProtocolContact() throws RemoteException
    {
        Folder folder = getProject();
        ProtocolQuery query = folder.createProtocolQuery();
        query.setLsid(QUERY_TEST_PROTOCOL_LSID);
        List<? extends Protocol> protocols = folder.getProtocols(query);
        Assert.assertEquals(1, protocols.size());

        Contact contact = protocols.get(0).getContact();
        Assert.assertEquals("ProtocolContactId", contact.getContactId());
        Assert.assertEquals("protocol@nowhere.com", contact.getEmail());
        Assert.assertEquals("FirstNameProtocol", contact.getFirstName());
        Assert.assertEquals("LastNameProtocol", contact.getLastName());
    }

    private SimpleValue findSimpleValueByName(String name, List<? extends SimpleValue> values)
    {
        for (SimpleValue value : values)
        {
            if (value.getName().equals(name))
            {
                return value;
            }
        }
        return null;
    }

    @Test(expected = RemoteException.class)
    public void testExperimentlessExperimentRunRequest() throws RemoteException
    {
        ExperimentRunQuery query = _service.createExperimentRunQuery(null);
        _service.getExperimentRuns(query);
    }

    @Test(expected = RemoteException.class)
    public void testWildcardExperimentLSIDExperimentRunRequest() throws RemoteException
    {
        ExperimentRunQuery query = _service.createExperimentRunQuery("something*");
        _service.getExperimentRuns(query);
    }

    @Test(expected = RemoteException.class)
    public void testContainerlessProtocolRequest() throws RemoteException
    {
        ProtocolQuery query = _service.createProtocolQuery(null);
        _service.getProtocols(query);
    }

    @Test(expected = RemoteException.class)
    public void testWildcardContainerProtocolRequest() throws RemoteException
    {
        ProtocolQuery query = _service.createProtocolQuery("something*");
        _service.getProtocols(query);
    }

    @Test
    public void testExperimentFilter() throws RemoteException
    {
        Folder folder = getProject();
        Assert.assertEquals(2, folder.getExperiments().size());
        getFilteredExperiments(folder, "Garbage no wildcards", null, 0);
        getFilteredExperiments(folder, "Garbage * wildcard", null, 0);
        getFilteredExperiments(folder, "*", null, 2);
        List<? extends Experiment> experiments = getFilteredExperiments(folder, STANDARD_EXPERIMENT_NAME, null, 1);
        Assert.assertEquals(STANDARD_EXPERIMENT_NAME, experiments.get(0).getName());
        Assert.assertEquals(STANDARD_EXPERIMENT_LSID, experiments.get(0).getLsid());
        experiments = getFilteredExperiments(folder, OTHER_EXPERIMENT_NAME, null, 1);
        Assert.assertEquals(OTHER_EXPERIMENT_NAME, experiments.get(0).getName());
        Assert.assertEquals(OTHER_EXPERIMENT_LSID, experiments.get(0).getLsid());

        experiments = getFilteredExperiments(folder, null, OTHER_EXPERIMENT_LSID, 1);
        Assert.assertEquals(OTHER_EXPERIMENT_NAME, experiments.get(0).getName());
        Assert.assertEquals(OTHER_EXPERIMENT_LSID, experiments.get(0).getLsid());
        getFilteredExperiments(folder, null, OTHER_EXPERIMENT_LSID.substring(0, OTHER_EXPERIMENT_LSID.lastIndexOf(":")) + "*", 2);

        ExperimentQuery query = folder.createExperimentQuery();
        query.setComments("Some comments");
        Assert.assertEquals(1, folder.getExperiments(query).size());

        query = folder.createExperimentQuery();
        query.setExperimentDescriptionURL("http://nowhere.com");
        Assert.assertEquals(1, folder.getExperiments(query).size());

        query = folder.createExperimentQuery();
        query.setHypothesis("This should work");
        Assert.assertEquals(1, folder.getExperiments(query).size());
    }

    private void validateProtocolApplication(ProtocolApplication protApp, String lsidSuffix, int inputDatas, int inputMaterials, int outputDatas, int outputMaterials)
    {
        Assert.assertTrue("Expected protocol application LSID to end with " + lsidSuffix + " but was " + protApp.getLsid(), protApp.getLsid().endsWith(":" + lsidSuffix));
        Assert.assertEquals("Wrong number of input materials", inputMaterials, protApp.getInputMaterials().size());
        Assert.assertEquals("Wrong number of input datas", inputDatas, protApp.getInputDatas().size());
        Assert.assertEquals("Wrong number of output materials", outputMaterials, protApp.getOutputMaterials().size());
        Assert.assertEquals("Wrong number of output datas", outputDatas, protApp.getOutputDatas().size());
    }

    @Test
    public void testSimpleProtocolApplications() throws RemoteException
    {
        ExperimentRun run = getSimpleExperimentRun();
        List<? extends ProtocolApplication> allProtApps = run.getProtocolApplications();
        Assert.assertEquals("Wrong number of protocol applications", 2, allProtApps.size());
        List<? extends ProtocolApplication> startingProtApps = run.getStartingProtocolApplications();
        Assert.assertEquals("Wrong number of starting protocol application", 1, startingProtApps.size());

        ProtocolApplication xtandemProtApp = startingProtApps.get(0);
        validateProtocolApplication(xtandemProtApp, "XTandemAnalyze", 3, 0, 1, 0);
        List<? extends ProtocolApplication> xtandemSuccessors = xtandemProtApp.getSuccessors();
        Assert.assertEquals("Wrong number of successor protocol applications", 1, xtandemSuccessors.size());

        ProtocolApplication pepXmlProtApp = xtandemSuccessors.iterator().next();
        validateProtocolApplication(pepXmlProtApp, "DoConvertToPepXml", 1, 0, 1, 0);
        List<? extends ProtocolApplication> pepXmlSuccessors = pepXmlProtApp.getSuccessors();
        Assert.assertEquals("Wrong number of successor protocol applications", 0, pepXmlSuccessors.size());
    }

    @Test
    public void testProtocolApplicationParameters() throws RemoteException
    {
        ExperimentRun run = getSimpleExperimentRun();
        List<? extends ProtocolApplication> apps = run.getStartingProtocolApplications();
        Assert.assertEquals(1, apps.size());
        ProtocolApplication app = apps.get(0);
        List<? extends SimpleValue> params = app.getProtocolApplicationParameters();
        Assert.assertEquals(5, params.size());
        validateSimpleValuePresent(params, "ApplicationLSIDTemplate", "terms.fhcrc.org#XarTemplate.ApplicationLSID", SimpleType.STRING, "${RunLSIDBase}:XTandemAnalyze");
        validateSimpleValuePresent(params, "ApplicationNameTemplate", "terms.fhcrc.org#XarTemplate.ApplicationName", SimpleType.STRING, "XTandem Database Search");
        validateSimpleValuePresent(params, "OutputDataFileTemplate", "terms.fhcrc.org#XarTemplate.OutputDataFile", SimpleType.STRING, "/xtandem/Example5/CAexample_mini.xtan.xml");
        validateSimpleValuePresent(params, "OutputDataLSIDTemplate", "terms.fhcrc.org#XarTemplate.OutputDataLSID", SimpleType.STRING, "${AutoFileLSID}");
        validateSimpleValuePresent(params, "OutputDataNameTemplate", "terms.fhcrc.org#XarTemplate.OutputDataName", SimpleType.STRING, "XTandem Search Results");
    }

    @Test
    public void testExperimentRunInputOutput() throws RemoteException
    {
        ExperimentRun simpleRun = getSimpleExperimentRun();
        List<? extends Data> simpleInputDatas = simpleRun.getInputDatas();
        Assert.assertEquals(3, simpleInputDatas.size());
        validateDataPresent(simpleInputDatas, ":..%2Fdatabases%2FBovine_mini.fasta");
        validateDataPresent(simpleInputDatas, ":%2Fxtandem%2FExample5%2Ftandem.xml");
        validateDataPresent(simpleInputDatas, ":CAexample_mini.mzXML");

        List<? extends Material> simpleInputMaterials = simpleRun.getInputMaterials();
        Assert.assertEquals(0, simpleInputMaterials.size());

        List<? extends Data> simpleOutputData = simpleRun.getOutputDatas();
        Assert.assertEquals(1, simpleOutputData.size());
        validateDataPresent(simpleOutputData, ":%2Fxtandem%2FExample5%2FCAexample_mini.pep.xml");
        SimpleValue originalURLValue = findSimpleValueByName("OriginalURL", simpleOutputData.get(0).getProperties().getSimpleValues());
        Assert.assertNotNull(originalURLValue);
        Assert.assertEquals("terms.fhcrc.org#Data.OriginalURL", originalURLValue.getOntologyEntryURI());
        Assert.assertEquals(SimpleType.STRING, originalURLValue.getValueType());
        Assert.assertTrue(((String)originalURLValue.getValue()).endsWith("RemoteAPITest/xtandem/Example5/CAexample_mini.pep.xml"));

        List<? extends Material> simpleOutputMaterial = simpleRun.getOutputMaterials();
        Assert.assertEquals(0, simpleOutputMaterial.size());

        ExperimentRun fractionationRun = getFractionationExperimentRun();
        List<? extends Data> fractionationInputDatas = fractionationRun.getInputDatas();
        Assert.assertEquals(0, fractionationInputDatas.size());

        List<? extends Material> fractionationInputMaterials = fractionationRun.getInputMaterials();
        Assert.assertEquals(2, fractionationInputMaterials.size());
        validateMaterialPresent(fractionationInputMaterials, ":Control");
        validateMaterialPresent(fractionationInputMaterials, ":Case");

        List<? extends Data> fractionationOutputDatas = fractionationRun.getOutputDatas();
        Assert.assertEquals(5, fractionationOutputDatas.size());
        validateDataPresent(fractionationOutputDatas, ":MS2Out0.mzXml");
        validateDataPresent(fractionationOutputDatas, ":MS2Out1.mzXml");
        validateDataPresent(fractionationOutputDatas, ":MS2Out2.mzXml");
        validateDataPresent(fractionationOutputDatas, ":MS2Out3.mzXml");
        validateDataPresent(fractionationOutputDatas, ":MS2Out4.mzXml");

        List<? extends Material> fractionationOutputMaterials = fractionationRun.getOutputMaterials();
        Assert.assertEquals(0, fractionationOutputMaterials.size());
    }

    private void validateDataPresent(List<? extends Data> datas, String lsid)
    {
        for (Data data : datas)
        {
            if (lsid.startsWith(":"))
            {
                if (data.getLsid().endsWith(lsid))
                {
                    return;
                }
            }
            else
            {
                if (data.getLsid().equals(lsid))
                {
                    return;
                }
            }
        }
        Assert.fail("Could not find data with LSID " + lsid);
    }

    private void validateMaterialPresent(List<? extends Material> materials, String lsid)
    {
        for (Material material : materials)
        {
            if (lsid.startsWith(":"))
            {
                if (material.getLsid().endsWith(lsid))
                {
                    return;
                }
            }
            else
            {
                if (material.getLsid().equals(lsid))
                {
                    return;
                }
            }
        }
        Assert.fail("Could not find material with LSID " + lsid);
    }

    @Test
    public void testFractionationProtocolApplications() throws RemoteException
    {
        ExperimentRun run = getFractionationExperimentRun();
        Assert.assertEquals("Wrong number of protocol applications", 14, run.getProtocolApplications().size());
        List<? extends ProtocolApplication> startingProtApps = run.getStartingProtocolApplications();
        Assert.assertEquals("Wrong number of starting protocol applications", 2, startingProtApps.size());
        ProtocolApplication cy5ProtApp = startingProtApps.get(0);
        validateProtocolApplication(cy5ProtApp, "TT.Cy5", 0, 1, 0, 1);
        Assert.assertTrue("Wrong protocol LSID", cy5ProtApp.getProtocolStep().getProtocol().getLsid().endsWith(":TaggingTreatment.Cy5"));

        ProtocolApplication cy3ProtApp = startingProtApps.get(1);
        validateProtocolApplication(cy3ProtApp, "TT.Cy3", 0, 1, 0, 1);
        Assert.assertTrue("Wrong protocol LSID", cy3ProtApp.getProtocolStep().getProtocol().getLsid().endsWith(":TaggingTreatment.Cy3"));

        List<? extends ProtocolApplication> cy5Successors = cy5ProtApp.getSuccessors();
        Assert.assertEquals("Wrong number of CY5 successor protocol applications", 1, cy5Successors.size());
        ProtocolApplication cy5PoolingProtApp = cy5Successors.iterator().next();
        validateProtocolApplication(cy5PoolingProtApp, "DoPooling", 0, 2, 0, 1);

        List<? extends ProtocolApplication> cy3Successors = cy3ProtApp.getSuccessors();
        Assert.assertEquals("Wrong number of CY3 successor protocol applications", 1, cy3Successors.size());
        ProtocolApplication cy3PoolingProtApp = cy3Successors.iterator().next();
        validateProtocolApplication(cy3PoolingProtApp, "DoPooling", 0, 2, 0, 1);

        Assert.assertSame("Pooling prot apps should be the same", cy3PoolingProtApp, cy5PoolingProtApp);

        List<? extends ProtocolApplication> poolingPredecessors = cy5PoolingProtApp.getPredecessors();
        Set<ProtocolApplication> expectedPoolingPredecessors = new HashSet<ProtocolApplication>();
        expectedPoolingPredecessors.add(cy3ProtApp);
        expectedPoolingPredecessors.add(cy5ProtApp);
        Set<ProtocolApplication> actualPoolingPredecessors = new HashSet<ProtocolApplication>(poolingPredecessors);
        Assert.assertEquals("Pooling predecessor protocol applications are wrong", expectedPoolingPredecessors, actualPoolingPredecessors);

        List<? extends ProtocolApplication> poolingSuccessors = cy5PoolingProtApp.getSuccessors();
        Assert.assertEquals("Wrong number of pooling successor protocol applications", 1, poolingSuccessors.size());
        ProtocolApplication ionExchangeProtApp = poolingSuccessors.iterator().next();
        validateProtocolApplication(ionExchangeProtApp, "DoIonExchange", 0, 1, 1, 5);
        Assert.assertTrue("Wrong ion exchange protocol LSID", ionExchangeProtApp.getProtocolStep().getProtocol().getLsid().endsWith(":ColumnSeparation.IonExch"));

        List<? extends ProtocolApplication> ionExchangeSuccessors = ionExchangeProtApp.getSuccessors();
        Assert.assertEquals("Wrong number of ion exchange successor protocol applications", 5, ionExchangeSuccessors.size());
        ProtocolApplication scan0ProtApp = ionExchangeSuccessors.iterator().next();
        validateProtocolApplication(scan0ProtApp, "DoLCMS2.0", 0, 1, 1, 0);
        Assert.assertTrue("Wrong scan 0 protocol LSID", scan0ProtApp.getProtocolStep().getProtocol().getLsid().endsWith(":LCMS2.n"));

        List<? extends ProtocolApplication> scan0Successors = scan0ProtApp.getSuccessors();
        Assert.assertEquals("Wrong number of scan 0 successor protocol applications", 1, scan0Successors.size());
        ProtocolApplication mzXMLProtApp = scan0Successors.iterator().next();
        validateProtocolApplication(mzXMLProtApp, "MS2Out0.ToMzXML", 1, 0, 1, 0);
        Assert.assertTrue("Wrong mzXML conversion protocol LSID", mzXMLProtApp.getProtocolStep().getProtocol().getLsid().endsWith(":ConvertToMzXML.n"));

        Assert.assertEquals("Wrong number of conversion successors", 0, mzXMLProtApp.getSuccessors().size());
    }

    @Test (expected = FileNotFoundException.class)
    public void testMissingFileInputStream() throws IOException
    {
        ExperimentRun run = getSimpleExperimentRun();
        List<? extends Data> outputDatas = run.getOutputDatas();
        Assert.assertEquals(1, outputDatas.size());
        Data pepXmlData = outputDatas.get(0);
        pepXmlData.getInputStream();
    }

    @Test
    public void testValidFileInputStream() throws IOException
    {
        ExperimentRun run = getSimpleExperimentRun();
        List<? extends Data> inputDatas = run.getInputDatas();
        Data tandemData = null;
        for (Data data : inputDatas)
        {
            if (data.getName().equals("Tandem Settings"))
            {
                tandemData = data;
                break;
            }
        }
        Assert.assertNotNull(tandemData);
        InputStream in = null;
        try
        {
            in = tandemData.getInputStream();
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            byte[] b = new byte[128];
            int i;
            while ((i = in.read(b)) != -1)
            {
                bOut.write(b, 0, i);
            }
            Assert.assertEquals("<tandem>Dummy file</tandem>", bOut.toString());
        }
        finally
        {
            if (in != null) { try { in.close(); } catch (IOException e) {} }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testModifyResultFolder() throws RemoteException
    {
        Folder folder = getProject();
        folder.setName("test");
    }

    @Test(expected = IllegalStateException.class)
    public void testModifyResultExperiment() throws RemoteException
    {
        Experiment exp = getSimpleExperiment();
        exp.setName("test");
    }

    @Test(expected = IllegalStateException.class)
    public void testModifyResultProtocol() throws RemoteException
    {
        Protocol protocol = getSimpleExperimentRun().getProtocol();
        protocol.setName("test");
    }

    @Test(expected = IllegalStateException.class)
    public void testModifyResultExperimentRun() throws RemoteException
    {
        ExperimentRun run = getSimpleExperimentRun();
        run.setName("test");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testModifyImmutableList() throws RemoteException
    {
        Experiment exp = getSimpleExperiment();
        List<? extends ExperimentRun> runs = exp.getExperimentRuns();
        runs.clear();
    }

    @Test
    public void testProtocolSteps() throws RemoteException
    {
        RunProtocol protocol = getSimpleExperimentRun().getProtocol();
        List<? extends ProtocolStep> firstSteps = protocol.getFirstSteps();
        Assert.assertEquals("Wrong number of first steps", 1, firstSteps.size());
        ProtocolStep firstStep = firstSteps.get(0);
        Assert.assertEquals("Wrong first step", "XTandem analysis", firstStep.getProtocol().getName());
        Assert.assertEquals("Wrong first step action sequence", 60, firstStep.getActionSequence());
        Assert.assertEquals("Wrong predecessors for first step", Collections.emptyList(), firstStep.getPredecessorSteps());

        List<? extends ProtocolStep> secondSteps = firstStep.getSuccessorSteps();
        Assert.assertEquals("Wrong number of second steps", 1, secondSteps.size());
        ProtocolStep secondStep = secondSteps.get(0);
        Assert.assertEquals("Wrong predecessors for second step", firstSteps, secondStep.getPredecessorSteps());
        Assert.assertEquals("Wrong second step", "Convert To PepXml", secondStep.getProtocol().getName());
        Assert.assertEquals("Wrong second step action sequence", 70, secondStep.getActionSequence());

        Assert.assertEquals("Wrong successors for second step", Collections.emptyList(), secondStep.getSuccessorSteps());

        Assert.assertEquals("Wrong output steps for protocol", secondSteps, protocol.getOutputSteps());
    }

    @Test
    public void testProtocolEquals() throws RemoteException
    {
        ExperimentRun run1 = getSimpleExperimentRun();
        RunProtocol protocol1 = run1.getProtocol();
        ExperimentRun run2 = getSimpleExperimentRun();
        RunProtocol protocol2 = run2.getProtocol();
        Assert.assertEquals(protocol1, protocol2);
        Assert.assertEquals(protocol1.hashCode(), protocol2.hashCode());

        ProtocolStep step1 = protocol1.getFirstSteps().get(0);
        ProtocolStep step2 = protocol2.getFirstSteps().get(0);

        Assert.assertEquals(step1, step2);
        Assert.assertEquals(step1, step2);
    }

    @Test
    public void testExperimentEquals() throws RemoteException
    {
        Experiment e1 = getOtherExperiment();
        Experiment e2 = getOtherExperiment();
        Assert.assertEquals(e1, e2);
        Assert.assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void testFolderEquals() throws RemoteException
    {
        Folder f1 = getProject();
        Folder f2 = getProject();
        Assert.assertEquals(f1, f2);
        Assert.assertEquals(f1.hashCode(), f2.hashCode());
    }

    @Test
    public void testExperimentRunEquals() throws RemoteException
    {
        ExperimentRun r1 = getFractionationExperimentRun();
        ExperimentRun r2 = getFractionationExperimentRun();
        Assert.assertEquals(r1, r2);
        Assert.assertEquals(r1.hashCode(), r2.hashCode());

        Assert.assertEquals(r1.getProtocolApplications(), r2.getProtocolApplications());
    }
}

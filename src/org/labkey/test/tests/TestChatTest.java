package org.labkey.test.tests;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import org.junit.Before;
import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.pages.TestChatPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestChatTest extends BaseWebDriverTest
{
    static final String PROJ_NAME = "SomeSillyProject";
    static final String AGENT_PROMPT = "Tell me about SampleManager.";

    // After prompting the chat agent with "Tell me about SampleManager." ten times this is the average of the vectors
    // produced from the response.
    static final float[] chatBaselineVector = {-0.03019388f, 0.005985766f, -0.038943104f, 1.2380854E-4f, 0.03959981f, -0.03969018f,
            0.008385928f, 0.0650781f, -0.034377806f, 0.021369578f, 0.032485746f, -0.07098605f, 0.018308807f, -0.049078677f,
            0.02110731f, 0.025103927f, 0.06300247f, -0.033695735f, 0.027478937f, -0.12054677f, -0.06396149f, -0.0909572f,
            -0.0016994249f, -0.013546536f, -0.082390755f, 0.050200384f, -0.009834141f, 0.033263985f, 2.144962E-4f, -0.0102791805f,
            0.035954155f, 0.016593222f, 0.019002238f, 0.04368814f, 0.02418695f, 0.016702509f, 0.016188633f, 0.04509254f,
            -0.014508081f, 0.0108033f, 0.013854737f, -0.030977119f, -0.0121150585f, -0.011303341f, 0.02243181f, -0.027660336f,
            -0.05156359f, -0.048708238f, -0.020657897f, 0.105352f, -0.096937254f, -0.09320516f, -0.002771596f, 0.0068775015f,
            -0.03265722f, -0.0071592345f, 0.048875596f, -0.055723954f, -0.008792663f, -0.008842771f, -0.028568873f, -0.03412085f,
            -0.04651943f, 0.014852325f, 0.022766655f, 0.029676199f, -0.0016782165f, 0.008890831f, 0.12608653f, -0.12625179f,
            -0.04233475f, -0.023784742f, -0.00810874f, 5.8253587E-4f, -0.09146256f, 0.020566467f, -0.10107992f, -0.053125072f,
            0.10948745f, -0.07148309f, 0.028527264f, 0.012225917f, -0.019197453f, 0.06417713f, -0.025329601f, 0.02017824f,
            -0.015511865f, 0.064494684f, 0.024872014f, 0.027644712f, -6.691076E-4f, -0.0062377816f, -0.043758385f, -0.025546502f,
            -0.04456261f, -0.028293476f, 0.030180443f, 0.0049647707f, 0.05462564f, 0.031072473f, 0.012807718f, 0.017690692f,
            0.026932884f, -0.042057615f, -0.051501762f, -0.09369942f, 0.017688313f, -0.008254238f, 0.078658f, 0.014951484f,
            0.012282652f, 0.002124861f, -0.011376065f, -0.054990627f, 0.07879907f, 0.050731517f, 0.006731376f, -0.022562737f,
            -0.0781275f, 0.05897262f, -0.036003347f, 0.021054795f, -0.05570032f, -0.062620334f, 0.023202963f, 0.04195801f,
            -0.052580297f, 2.4254074E-33f, 0.04243066f, -0.08184351f, -0.044089966f, -0.015155988f, 0.088315025f, 0.005251685f,
            0.0016051279f, 0.029601464f, -0.0485765f, -0.0071834503f, 0.051950507f, 0.03990639f, -0.036864806f, 0.030445088f,
            -0.04078031f, -0.011637151f, -0.0340612f, 0.15121043f, -0.0017582325f, -0.009440663f, -0.04721365f, -0.056454398f,
            0.028072383f, 0.039820418f, 0.07523839f, 0.118679464f, -0.05993416f, 0.09758407f, 0.035568476f, 0.013105391f,
            0.027454028f, 0.0059799305f, -0.06677597f, -0.040655755f, -0.007886192f, 0.030017648f, -0.065624095f, -0.045507856f,
            0.037406627f, 0.050265454f, 0.01404729f, -0.0032918013f, 0.10254403f, -0.060743976f, -0.08872155f, -0.046550352f,
            -0.054800462f, -0.004585274f, 0.15233898f, -0.0024047687f, -0.0065974155f, -0.027759988f, 0.016873736f, 0.025811935f,
            0.030244157f, 0.11523633f, -0.002102522f, -0.014610293f, 0.0044642515f, 0.083505385f, -0.025581622f, 0.056536622f,
            -0.10864973f, 0.08772747f, 0.09841694f, -0.0127469925f, -0.016276544f, -0.04108299f, 0.019679891f, 0.051637344f,
            -0.04988219f, -0.017821329f, -0.058820117f, 0.0018413272f, 0.046758216f, -0.06892814f, 0.016509239f, 0.06284414f,
            -0.09226079f, 0.02382822f, -0.04409612f, -0.021055017f, -0.03397707f, -0.025002161f, -0.05432595f, 0.077902004f,
            -0.0022827755f, -0.022489328f, -0.009794355f, -0.06506187f, 9.556079E-4f, 0.014758319f, -0.081193954f, 0.08589422f,
            -0.023095239f, -3.5807946E-33f, 0.0076882765f, -0.024106996f, 0.017340723f, 0.046940584f, 0.03976236f, 0.06408286f,
            0.038793515f, -0.018723648f, -0.012169516f, 0.031596184f, 0.006595213f, -0.018227678f, 0.040385712f, 0.004442801f,
            -0.0353129f, 0.0404903f, -0.07395537f, -0.076874554f, -0.029712439f, -0.02082371f, -0.046095975f, 0.05605728f,
            0.02372828f, -0.02541101f, -0.037760805f, -0.016863775f, -0.041167192f, -0.048119824f, 0.09028868f, -0.051599097f,
            -0.009274418f, 0.019141247f, -0.02239729f, -0.09848398f, 0.037428f, -0.06248361f, 0.1003523f, 0.022652518f, 0.022521332f,
            0.096326165f, 0.017058356f, 0.01432389f, -0.07351521f, -0.09105729f, 0.011416706f, 0.15413883f, -0.0034974138f,
            0.030018065f, -0.034188204f, -0.029879734f, -0.050090652f, -0.0017074157f, -0.075418726f, -0.060847033f, -0.042361654f,
            0.030342972f, -0.05415695f, 0.020506423f, -0.051692694f, 0.0010761966f, -0.02111739f, -0.002596379f, -0.014777714f,
            0.013234101f, -0.0072116293f, 0.09875045f, 0.032375146f, -0.03797715f, -0.052857857f, -0.00493684f, 0.0672756f,
            -0.0051548677f, 0.070593044f, 0.012899426f, 0.02489981f, -0.045002542f, -0.074416384f, -0.06910811f, -0.019730698f,
            -0.074781194f, 0.07508006f, -0.024290306f, 0.062818274f, 0.049903717f, 0.011283757f, 0.01512167f, -0.020251568f,
            0.031181078f, -0.06360834f, 0.010683356f, -0.025994647f, -0.030168762f, -0.03066593f, 0.07060148f, 0.040387094f,
            -4.483916E-8f, 0.0037675234f, 0.02114608f, 0.029677272f, 0.003628221f, -0.05467111f, -0.0030842654f, 0.005633498f,
            0.059147965f, 0.07015609f, 0.055218052f, 0.01710672f, -0.04591004f, -0.09770672f, 0.0032543591f, -0.0032254788f,
            0.05653432f, 0.074942134f, 0.06829107f, 1.3839379E-4f, -0.017271006f, -0.016547177f, -0.0018977622f, 0.062490083f,
            -0.008594567f, 0.03293664f, 0.012968835f, 0.06187564f, 0.08536985f, -0.017532904f, -0.06606911f, -0.025436794f,
            0.022383422f, 0.04929865f, -0.026249608f, 0.10196996f, 0.009566573f, -0.0965889f, -0.08215118f, 0.07319057f,
            0.017184425f, -0.05756793f, 0.0026009548f, -0.09378164f, -0.018914059f, -0.030732218f, 0.015425719f, -0.03156285f,
            -0.034874555f, -9.488471E-4f, 0.004473595f, -0.034989f, -0.022167904f, 0.081022024f, 0.0065548765f, 0.008132218f,
            0.1324919f, 0.050239515f, -0.06295673f, 0.01697682f, 0.010819006f, 0.12522274f, 0.017968038f, -0.00625929f, -0.08564173f};

    // Alternatively, the average could be a vector generated from a single string used as the expected result.
    static final String chatBaselineString = "LabKey Sample Manager is a sample management application designed to be easy-to-use while providing powerful lab sample tracking and workflow features.\n" +
            "Based on the available documentation, here are some key details:\n" +
            "Core Functionality: It offers powerful lab sample tracking and workflow capabilities.\n" +
            "Availability: It is a Premium Feature available with all Premium Editions of LabKey Server.\n" +
            "Integration: It can be used within a project and integrates with LabKey Studies and other resources. For example, when used with Panorama, it simplifies associating sample metadata with results data (such as targeted mass spectrometry data).\n" +
            "For more detailed information, you can view the Sample Manager documentation or the guide on Using Sample Manager with LabKey Server.";

    Criteria<String, float[]> _criteria = null;
    // Loading the model is expensive. Could / should pool it.
    ZooModel<String, float[]> _model = null;
    // Predictor is not thread safe.
    Predictor<String, float[]> _predictor = null;

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment", "issues");
    }
    @Override
    protected String getProjectName()
    {
        return PROJ_NAME;
    }

    @Before
    public void buildStuffIfNeeded()
    {

        if (null == _criteria)
        {
            _criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    // Force the PyTorch engine and specify the Hugging Face path
                    .optEngine("PyTorch")
                    //Load the model: sentence-transformers/all-MiniLM-L6-v2
                    //This is the MiniLM embedding model:
                    //	384-dimensional output vectors
                    //	Optimized for semantic similarity
                    //all-MiniLM-L6-v2:
                    //  all -> The model was trained on a massive, diverse dataset.
                    //  L6 -> Depth of the neural network. This is a 6-layer model (faster than L12).
                    //  v2 -> Second version of the model.
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
                    // This translator is often required to bridge the gap between String and the Model's Tensor input.
                    // Sentence transformer models require tokenization before inference.
                    .optArgument("tokenizer", "sentence-transformers/all-MiniLM-L6-v2")
                    .build();

            try
            {
                _model = _criteria.loadModel();
                _predictor = _model.newPredictor();
            }
            catch (IOException | ModelNotFoundException | MalformedModelException e)
            {
                throw new RuntimeException(e);
            }

        }

    }

    @Test
    public void testStrings()
    {

        StringBuilder sbLog = new StringBuilder();
        sbLog.append(printHeader());

        log("Check the math.");
        String string01 = "ABC";

        log(String.format("Compare string '%s' to it's self.", string01));
        List<Double> derivation = calculateDeviation(string01, string01);
        sbLog.append(printFormat("Same String", derivation.getFirst(), derivation.getLast()));

        String string02 = "123456789";

        log(String.format("Compare '%s' to '%s", string01, string02));
        derivation = calculateDeviation(string01, string02);
        sbLog.append(printFormat("String to Number", derivation.getFirst(), derivation.getLast()));

        string01 = "Patient is Healthy";
        string02 = "Patient is Dead";

        log(String.format("Compare '%s' to '%s", string01, string02));
        derivation = calculateDeviation(string01, string02);
        sbLog.append(printFormat("Different Meanings", derivation.getFirst(), derivation.getLast()));

        string01 = "The quick brown fox jumped over the lazy dog.";
        string02 = ".dog lazy the over jumped fox brown quick The";

        log(String.format("Compare '%s' to '%s", string01, string02));
        derivation = calculateDeviation(string01, string02);
        sbLog.append(printFormat("Same Words Different Order", derivation.getFirst(), derivation.getLast()));

        // Print out the results in a table.
        log(sbLog.toString());
    }

    @Test
    public void testChat()
    {

        String logFormat = "\n\n****\nTest %s\nResponse: %s\n****\n";

        StringBuilder sbLog = new StringBuilder();
        sbLog.append(printHeader());

        log("Test the chat app.");

        TestChatPage testChatPage = TestChatPage.beginAt(this);
        testChatPage.enterPrompt(AGENT_PROMPT);
        String response = testChatPage.getMostRecentResponse();

        log(String.format(logFormat, "Initial Ask", response));

        List<Double> derivation = calculateDeviationFromBaseline(response);
        sbLog.append(printFormat("Initial Ask (Vector Baseline)", derivation.getFirst(), derivation.getLast()));

        derivation = calculateDeviation(chatBaselineString, response);
        sbLog.append(printFormat("Initial Ask (String Baseline)", derivation.getFirst(), derivation.getLast()));

        log("Ask the same question again.");
        testChatPage.enterPrompt(AGENT_PROMPT);
        response = testChatPage.getMostRecentResponse();

        log(String.format(logFormat, "Ask Again", response));

        derivation = calculateDeviationFromBaseline(response);
        sbLog.append(printFormat("Ask Again (Vector Baseline)", derivation.getFirst(), derivation.getLast()));

        derivation = calculateDeviation(chatBaselineString, response);
        sbLog.append(printFormat("Ask Again (String Baseline)", derivation.getFirst(), derivation.getLast()));

        log("Now sign out and sign back in to try and change the response.");
        signOut();
        signIn();

        testChatPage = TestChatPage.beginAt(this);
        testChatPage.enterPrompt(AGENT_PROMPT);
        response = testChatPage.getMostRecentResponse();

        log(String.format(logFormat, "Log Out \\ In ", response));

        derivation = calculateDeviationFromBaseline(response);
        sbLog.append(printFormat("Log Out \\ In (Vector Baseline)", derivation.getFirst(), derivation.getLast()));

        derivation = calculateDeviation(chatBaselineString, response);
        sbLog.append(printFormat("Log Out \\ In (String Baseline)", derivation.getFirst(), derivation.getLast()));

        // Print out the results in a table.
        log(sbLog.toString());
    }

    private List<Double> calculateDeviationFromBaseline(String response)
    {
        List<Double> deviation;

        try {
            float[] vector = _predictor.predict(response);
            deviation = calculateCosineDiffAndDistance(chatBaselineVector, vector);
        }
        catch (TranslateException e)
        {
            throw new RuntimeException(e);
        }

        return deviation;

    }

    private List<Double> calculateDeviation(String str01, String str02)
    {
        List<Double> deviation;

        try {
            float[] vector1 = _predictor.predict(str01);
            float[] vector2 = _predictor.predict(str02);
            deviation = calculateCosineDiffAndDistance(vector1, vector2);

        }
        catch (TranslateException e)
        {
            throw new RuntimeException(e);
        }

        return deviation;

    }

    // Method using linear algebra that calculates the Cosine Similarity (how relevant they are to each other) and then
    // converts it to Cosine Distance (the "deviation" or how far apart they are).
    private List<Double> calculateCosineDiffAndDistance(float[] vectorA, float[] vectorB) {

        List<Double> returnList = new ArrayList<>();

        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension.");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        // Project against a divide by zero.
        double magnitude = (Math.sqrt(normA) * Math.sqrt(normB));
        if (magnitude == 0.0)
        {
            return List.of(-1.0, 1.0); // Maximum distance, completely dissimilar.
        }

        // Cosine Similarity Formula.
        // Measures the cosine of the angle between two vectors (the text responses converted into numbers).
        // dot(A, B) / (||A|| * ||B||)
        // Dot Product of A & B divided by the magnitude, Euclidean Norms (lengths) of the vectors multiplied together.
        // Range from -1 to 1. 1.0 means identical, 0.0 unrelated, negative is opposite.
        double similarity = dotProduct / magnitude;
        returnList.add(similarity);

        // Return Cosine Distance (Deviation)
        // 0.0 means identical, 1.0 means orthogonal (completely different)
        returnList.add(1.0 - similarity);

        return returnList;
    }

    // Some code to make the output in the logs look nice.
    private String printHeader()
    {

        // %-20s = String, left-justified, 20 spaces
        String headerFormat = "| %-35s | %-20s | %-20s |%n";

        return "\nCosine Similarity: 1.0-identical, 0.0-unrelated, -1.0-opposite\n" +
                "Cosine Distance / Deviation: 1.0-orthogonal, 0.0-no deviation\n\n" +
                String.format(headerFormat, "Test Name", "Cosine Similarity", "Cosine Distance") +
                String.format("|" + "-".repeat(37) + "|" + "-".repeat(22) + "|" + "-".repeat(22) + "|%n");
    }

    private String printFormat(String testName, double similarity, double distance)
    {
        // %-20s = String, left-justified, 20 spaces (for the numbers)
        String rowFormat    = "| %-35.35s | %-20.10e | %-20.10e |%n";
        return String.format(rowFormat, testName, similarity, distance);
    }

    // Some hacky code used to calculate the average vector from 10 responses.
//    @Test
    public void generateBaseLine() throws TranslateException
    {

        List<float[]> vectors = new ArrayList<>();

        signOut();

        for (int i = 0; i < 10; i++)
        {
            signIn();
            TestChatPage testChatPage = TestChatPage.beginAt(this);
            testChatPage.enterPrompt(AGENT_PROMPT);
            String response = testChatPage.getMostRecentResponse();
            vectors.add(_predictor.predict(response));
            sleep(1_000);
            signOut();
        }

        float[] baseLineVector = calculateAverageVector(vectors);
        log("Baseline Vector: " + baseLineVector);
    }

    private float[] calculateAverageVector(List<float[]> vectors) {
        int dimensions = vectors.getFirst().length;
        float[] centroid = new float[dimensions];

        for (float[] vector : vectors) {
            for (int i = 0; i < dimensions; i++) {
                centroid[i] += vector[i];
            }
        }

        // Divide by the number of vectors to get the mean
        for (int i = 0; i < dimensions; i++) {
            centroid[i] /= vectors.size();
        }
        return centroid;
    }

}

package org.labkey.test.tests;


import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.pages.TestChatPage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class testChatTest extends BaseWebDriverTest
{
    static final String PROJ_NAME = "SomeSillyProject";

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

    @Test
    public void testChat()
    {

        String logFormatString = "\nTest %s\nResponse 1: %s\nResponse 2: %s\nDeviation: %e";

        log("First, check your math.");
        String s1 = "Hello world!";
        String s2 = "Did you see the parade today?";

        double derivation = calculateDeviation(s1, s2);
        log(String.format(logFormatString,
                "Sanity Check", s1, s2, derivation));

        TestChatPage testChatPage = TestChatPage.beginAt(this);
        testChatPage.enterPrompt("Tell me about SampleManager.");
        String response1 = testChatPage.getMostRecentResponse();
        String response2 = testChatPage.getAllResponses().getLast();
        derivation = calculateDeviation(response1, response2);

        log(String.format(logFormatString,
                "Same Response String", response1, response2, derivation));

        log("Ask the same question again.");
        testChatPage.enterPrompt("Tell me about SampleManager.");
        response2 = testChatPage.getMostRecentResponse();
        derivation = calculateDeviation(response1, response2);

        log(String.format(logFormatString,
                "Ask The Question Again", response1, response2, derivation));

        log("Now sign out and sign back in to try and change the response.");
        signOut();
        signIn();

        testChatPage = TestChatPage.beginAt(this);
        testChatPage.enterPrompt("Tell me about SampleManager.");
        response2 = testChatPage.getMostRecentResponse();
        derivation = calculateDeviation(response1, response2);

        log(String.format(logFormatString,
                "Log Out and Back In", response1, response2, derivation));

    }

    private double calculateDeviation(String str01, String str02)
    {
        double deviation;

        // Conceptual snippet using DJL for Semantic Similarity
//        Criteria<String, float[]> criteria = Criteria.builder()
//                .setTypes(String.class, float[].class)
//                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
//                .build();

        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                // Force the PyTorch engine and specify the Hugging Face path
                .optEngine("PyTorch")
                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
                // This translator is often required to bridge the gap between String and the Model's Tensor input
                .optArgument("tokenizer", "sentence-transformers/all-MiniLM-L6-v2")
                .build();

        try (ZooModel<String, float[]> model = criteria.loadModel();
             Predictor<String, float[]> predictor = model.newPredictor()) {
            float[] vector1 = predictor.predict(str01);
            float[] vector2 = predictor.predict(str02);
            deviation = calculateCosineDistance(vector1, vector2);

        }
        catch (TranslateException | IOException | ModelNotFoundException | MalformedModelException e)
        {
            throw new RuntimeException(e);
        }

        return deviation;

    }

    // Linear algebra method that calculates the Cosine Similarity (how similar they are) and then converts it to
    // Cosine Distance (the "deviation" or how far apart they are).
    public static double calculateCosineDistance(float[] vectorA, float[] vectorB) {

        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension.");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        // Cosine Similarity Formula
        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));

        // Return Cosine Distance (Deviation)
        // 0.0 means identical, 1.0 means orthogonal (completely different)
        return 1.0 - similarity;
    }

}

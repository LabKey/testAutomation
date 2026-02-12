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
    static double _cosine_diff = 0.0;

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

        String logFormatString = "\nTest %s\nResponse 1: %s\nResponse 2: %s\nCosine Similarity (1.0-identical, 0.0-unrelated, negative-opposite): %e\nCosine Distance / Deviation (1.0-orthogonal, 0.0-no deviation): %e";

        log("First, check your math.");
        String response1 = "ABC";
        String response2 = "123456789";

        double derivation = calculateDeviation(response1, response2);
        log(String.format(logFormatString,
                "Sanity Check", response1, response2, _cosine_diff, derivation));

        response1 = "Patient is Healthy";
        response2 = "Patient is Dead";

        derivation = calculateDeviation(response1, response2);
        log(String.format(logFormatString,
                "Two Different Meanings", response1, response2, _cosine_diff, derivation));

        response1 = "The quick brown fox jumped over the lazy dog.";
        response2 = ".dog lazy the over jumped fox brown quick The";

        derivation = calculateDeviation(response1, response2);
        log(String.format(logFormatString,
                "Same Words Different Order", response1, response2, _cosine_diff, derivation));

        TestChatPage testChatPage = TestChatPage.beginAt(this);
        testChatPage.enterPrompt("Tell me about SampleManager.");
        response1 = testChatPage.getMostRecentResponse();
        response2 = testChatPage.getAllResponses().getLast();
        derivation = calculateDeviation(response1, response2);

        log(String.format(logFormatString,
                "Same Response String", response1, response2, _cosine_diff, derivation));

        log("Ask the same question again.");
        testChatPage.enterPrompt("Tell me about SampleManager.");
        response2 = testChatPage.getMostRecentResponse();
        derivation = calculateDeviation(response1, response2);

        log(String.format(logFormatString,
                "Ask The Question Again", response1, response2, _cosine_diff, derivation));

        log("Now sign out and sign back in to try and change the response.");
        signOut();
        signIn();

        testChatPage = TestChatPage.beginAt(this);
        testChatPage.enterPrompt("Tell me about SampleManager.");
        response2 = testChatPage.getMostRecentResponse();
        derivation = calculateDeviation(response1, response2);

        log(String.format(logFormatString,
                "Log Out and Back In", response1, response2, _cosine_diff, derivation));

    }

    private double calculateDeviation(String str01, String str02)
    {
        double deviation;

        Criteria<String, float[]> criteria = Criteria.builder()
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

        // Loading the model is expensive. Could / should pool it.
        // Predictor is not thread safe.
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

    // Linear algebra method that calculates the Cosine Similarity (how relevant they are to each other) and then
    // converts it to Cosine Distance (the "deviation" or how far apart they are).
    public static double calculateCosineDistance(float[] vectorA, float[] vectorB) {

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

        double magnitude = (Math.sqrt(normA) * Math.sqrt(normB));
        if (magnitude == 0.0)
        {
            return 1.0; // Maximum distance, completely dismilar.
        }
        // Cosine Similarity Formula.
        // Measures the cosine of the angle between two vectors (the text responses converted into numbers).
        // dot(A, B) / (||A|| * ||B||)
        // Dot Product of A & B divided by the magnitude, Euclidean Norms (lengths) of the vectors multiplied together.
        // Range from -1 to 1. 1.0 means identical, 0.0 unrelated, negative is opposite.
        double similarity = dotProduct / magnitude;
        _cosine_diff = similarity;

        // Return Cosine Distance (Deviation)
        // 0.0 means identical, 1.0 means orthogonal (completely different)
        return 1.0 - similarity;
    }

}

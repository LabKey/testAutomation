package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Modal that opens when the user clicks the "AI Assistant" button inside the Calculation field options.
 * Provides a chat-style interface where the user enters prompts and the assistant suggests expressions.
 */
public class CalculatedColumnAssistantDialog extends ModalDialog
{
    public static final String TITLE = "Expression AI Assistant";

    private final DomainFieldRow _row;

    public CalculatedColumnAssistantDialog(DomainFieldRow row, ModalDialogFinder finder)
    {
        super(finder);
        _row = row;
    }

    public CalculatedColumnAssistantDialog(DomainFieldRow row)
    {
        this(row, new ModalDialogFinder(row.getDriver()).withTitle(TITLE));
    }

    /**
     * Type the prompt into the textarea. The submit button stays disabled until non-empty text is present.
     */
    public CalculatedColumnAssistantDialog setPrompt(String prompt)
    {
        getWrapper().setFormElement(elementCache().promptInput, prompt);
        WebDriverWrapper.waitFor(() -> elementCache().promptSubmitButton.isEnabled(),
                "Prompt submit button did not become enabled.", 2_000);
        return this;
    }

    public String getPrompt()
    {
        return getWrapper().getFormElement(elementCache().promptInput);
    }

    /**
     * Click the submit (arrow) button. First waits for the "Thinking..." spinner to disappear (up to 60s)
     * and then for a new assistant response to render (up to 10s).
     */
    public CalculatedColumnAssistantDialog submitPrompt()
    {
        int previousCount = getAssistantResponses().size();
        elementCache().promptSubmitButton.click();
        waitForThinkingSpinnerToDisappear();
        WebDriverWrapper.waitFor(() -> getAssistantResponses().size() > previousCount,
                "No new assistant response appeared in chat history.", 10_000);
        return this;
    }

    private void waitForThinkingSpinnerToDisappear()
    {
        Locator spinner = Locator.tagWithClass("i", "fa-spinner");
        // Spinner may not appear if the response is instantaneous; that's fine.
        WebDriverWrapper.waitFor(() -> !spinner.existsIn(this), 60_000);
    }

    /**
     * Convenience: type the prompt and submit it.
     */
    public CalculatedColumnAssistantDialog sendPrompt(String prompt)
    {
        return setPrompt(prompt).submitPrompt();
    }

    /**
     * @return one entry per assistant response bubble (concatenated text of all its {@code .assistant-text} blocks),
     * in chat order. Suggested-expression SQL is not included here — see {@link #getSuggestedExpressions()}.
     */
    public List<String> getAssistantResponses()
    {
        return Locator.tagWithClass("div", "chat-item").withClass("assistant-response")
                .findElements(this).stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    /**
     * @return text of the most recent assistant response, or empty string if there are none.
     */
    public String getLastAssistantResponse()
    {
        List<String> responses = getAssistantResponses();
        return responses.isEmpty() ? "" : responses.get(responses.size() - 1);
    }

    /**
     * @return every SQL expression suggested in the most recent assistant response, in display order.
     * Usually a single entry, occasionally more.
     */
    public List<String> getSuggestedExpressions()
    {
        WebElement lastResponse = lastAssistantResponseElement();
        if (lastResponse == null)
            return List.of();
        return Locator.tagWithClass("div", "assistant-expression")
                .descendant(Locator.tag("code"))
                .findElements(lastResponse).stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    /**
     * @return the first SQL expression in the most recent assistant response, or empty string if none.
     */
    public String getFirstSuggestedExpression()
    {
        List<String> expressions = getSuggestedExpressions();
        return expressions.isEmpty() ? "" : expressions.get(0);
    }

    /**
     * Click "Apply Expression" on the first suggestion in the most recent assistant response.
     * Returns the underlying field row (the dialog stays open; call {@link #clickEndChat()} to close it).
     */
    public DomainFieldRow applyFirstSuggestedExpression()
    {
        WebElement lastResponse = lastAssistantResponseElement();
        if (lastResponse == null)
            throw new IllegalStateException("No assistant response is available to apply.");
        Locator.tagWithClass("div", "assistant-expression")
                .descendant(Locator.tagWithClass("button", "clickable-text"))
                .findElement(lastResponse)
                .click();
        return _row;
    }

    private WebElement lastAssistantResponseElement()
    {
        List<WebElement> responses = Locator.tagWithClass("div", "chat-item").withClass("assistant-response")
                .findElements(this);
        return responses.isEmpty() ? null : responses.get(responses.size() - 1);
    }

    /**
     * Click "End Chat" to close the dialog.
     */
    public DomainFieldRow clickEndChat()
    {
        elementCache().endChatButton.click();
        waitForClose();
        return _row;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        final WebElement endChatButton = Locator.tagWithClass("button", "btn")
                .withText("End Chat")
                .findWhenNeeded(this);

        final WebElement promptInput = Locator.tagWithClass("textarea", "prompt-input")
                .findWhenNeeded(this);

        final WebElement promptSubmitButton = Locator.tagWithClass("button", "prompt-button")
                .findWhenNeeded(this);
    }
}

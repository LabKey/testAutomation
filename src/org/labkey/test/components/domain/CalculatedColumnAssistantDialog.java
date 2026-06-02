package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Modal that opens when the user clicks the "AI Assistant" button inside the Calculation field options.
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
        WebDriverWrapper.waitFor(() -> !Locators.thinkingSpinner.existsIn(this), 60_000);
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
        return Locators.assistantResponse.findElements(this).stream()
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
     * @return every <em>applicable</em> SQL expression suggested in the most recent assistant response, in display
     * order. Only counts {@code .assistant-expression} blocks that include an "Apply Expression" button — read-only
     * SQL the assistant shows for illustration (e.g. an alternative custom-query example) is excluded, since the user
     * can't accept it as the field's calculation.
     */
    public List<String> getSuggestedExpressions()
    {
        WebElement lastResponse = lastAssistantResponseElement();
        if (lastResponse == null)
            return List.of();
        return Locators.applicableSqlCode.findElements(lastResponse).stream()
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
        return applySuggestedExpression(0);
    }

    /**
     * Click "Apply Expression" on the suggestion at the given index in the most recent assistant response. Waits
     * up to 5 seconds for at least one applicable expression to render — the spinner disappears as soon as the
     * bubble exists, but the inner {@code assistant-expression} block sometimes finishes rendering a moment later.
     */
    public DomainFieldRow applySuggestedExpression(int index)
    {
        List<WebElement> buttons = new ArrayList<>();
        WebDriverWrapper.waitFor(() -> {
                    buttons.clear();
                    WebElement last = lastAssistantResponseElement();
                    if (last != null)
                        buttons.addAll(Locators.applyButton.findElements(last));
                    return !buttons.isEmpty();
                },
                "No applicable expression rendered in the assistant response.",
                5_000);

        if (index >= buttons.size())
            throw new IndexOutOfBoundsException(
                    "Requested expression index " + index + " but only " + buttons.size() + " expression(s) available.");
        buttons.get(index).click();
        return _row;
    }

    /**
     * @return text of the first assistant response in the chat history, or empty string if there are none. Useful
     * for asserting the intro message in NEW / CHANGE / VALIDATE entry modes.
     */
    public String getFirstAssistantResponse()
    {
        List<String> responses = getAssistantResponses();
        return responses.isEmpty() ? "" : responses.get(0);
    }

    /**
     * @return true while the dialog is waiting for an AI response (the "Thinking..." pending bubble is shown).
     */
    public boolean isPending()
    {
        return Locators.pendingBubble.existsIn(this);
    }

    /**
     * Click the stop button to abort an in-flight AI request. The submit button toggles to a stop button (fa-stop)
     * while the dialog is in the pending state; calling this method when no request is pending will fail.
     */
    public void clickStop()
    {
        Locators.stopButton.findElement(this).click();
    }

    /**
     * Click submit without waiting for the response. Useful for tests that need to interrupt or otherwise observe
     * the pending state before the response arrives. Prefer {@link #submitPrompt()} when the caller wants to wait.
     */
    public void clickSubmitWithoutWaiting()
    {
        elementCache().promptSubmitButton.click();
    }

    private WebElement lastAssistantResponseElement()
    {
        List<WebElement> responses = Locators.assistantResponse.findElements(this);
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

    public static class Locators
    {
        public static final Locator.XPathLocator assistantResponse = Locator.tagWithClass("div", "chat-item").withClass("assistant-response");

        public static final Locator.XPathLocator pendingBubble = Locator.tagWithClass("div", "chat-item").withClass("pending");

        public static final Locator.XPathLocator thinkingSpinner = Locator.tagWithClass("i", "fa-spinner");

        public static final Locator.XPathLocator applyButton = Locator.tagWithClass("div", "assistant-expression")
                .descendant(Locator.tagWithClass("button", "clickable-text"));

        public static final Locator.XPathLocator applicableSqlCode = Locator.tagWithClass("div", "assistant-expression")
                .withDescendant(Locator.tagWithClass("button", "clickable-text"))
                        .descendant(Locator.tag("code"));

        public static final Locator.XPathLocator stopButton = Locator.tagWithClass("button", "prompt-button")
                .withDescendant(Locator.tagWithClass("i", "fa-stop"));
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        final WebElement endChatButton = Locator.tagWithClass("button", "btn").withText("End Chat").findWhenNeeded(this);

        final WebElement promptInput = Locator.tagWithClass("textarea", "prompt-input").findWhenNeeded(this);

        final WebElement promptSubmitButton = Locator.tagWithClass("button", "prompt-button").refindWhenNeeded(this);
    }
}

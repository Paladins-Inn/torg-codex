package de.paladinsinn.torg.codex.domain.markup;


import java.util.Set;

public class TorgMarkupService {

    private final ConditionalBlockProcessor conditionalProcessor;
    private final EntityReferenceProcessor entityProcessor;
    private final RawHtmlProcessor rawHtmlProcessor;
    private final GameTokenProcessor gameTokenProcessor;
    private final MarkdownProcessor markdownProcessor;

    public TorgMarkupService(
            ConditionalBlockProcessor conditionalProcessor,
            EntityReferenceProcessor entityProcessor,
            RawHtmlProcessor rawHtmlProcessor,
            GameTokenProcessor gameTokenProcessor,
            MarkdownProcessor markdownProcessor
    ) {
        this.conditionalProcessor = conditionalProcessor;
        this.entityProcessor = entityProcessor;
        this.rawHtmlProcessor = rawHtmlProcessor;
        this.gameTokenProcessor = gameTokenProcessor;
        this.markdownProcessor = markdownProcessor;
    }

    public String render(String rawText, Set<String> ownedProducts) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String result = rawText;
        result = conditionalProcessor.process(result, ownedProducts);
        result = entityProcessor.process(result);
        result = rawHtmlProcessor.process(result);
        result = gameTokenProcessor.process(result);
        result = markdownProcessor.process(result);
        return result;
    }
}

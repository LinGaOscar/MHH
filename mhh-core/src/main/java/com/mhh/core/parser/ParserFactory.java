package com.mhh.core.parser;

import com.mhh.common.entity.MessageHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service to identify and select the relevant parser for a PDF message.
 * It uses the Strategy Pattern to handle different message formats.
 */
@Service
@Slf4j
public class ParserFactory {

    private final List<PdfParser> parsers;

    @Autowired
    public ParserFactory(List<PdfParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * Finds the best parser and processes the text.
     * @param text The extracted text from PDF.
     * @return Annotated MessageHistory if a parser is found, empty otherwise.
     */
    public Optional<MessageHistory> parse(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Text content is empty, cannot parse.");
            return Optional.empty();
        }

        return parsers.stream()
                .filter(p -> p.supports(text))
                .min(Comparator.comparingInt(PdfParser::getPriority))
                .map(p -> {
                    log.info("Using parser: {}", p.getClass().getSimpleName());
                    return p.parse(text);
                });
    }
}

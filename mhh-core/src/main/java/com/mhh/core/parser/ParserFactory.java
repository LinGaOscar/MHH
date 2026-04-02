package com.mhh.core.parser;

import com.mhh.common.entity.SwiftMessageBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service to identify and select the relevant parser for a PDF message.
 * Uses the Strategy Pattern to handle different message formats.
 * Returns the concrete direction subtype ({@link com.mhh.common.entity.MsgIncoming}
 * or {@link com.mhh.common.entity.MsgOutgoing}) as determined by each parser.
 */
@Service
@Slf4j
public class ParserFactory {

    private final List<PdfParser> parsers;

    public ParserFactory(List<PdfParser> parsers) {
        this.parsers = parsers;
    }

    public Optional<SwiftMessageBase> parse(String text) {
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

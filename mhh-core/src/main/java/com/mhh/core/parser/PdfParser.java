package com.mhh.core.parser;

import com.mhh.common.entity.MessageHistory;

/**
 * Interface for PDF message parsers.
 * Each implementation handles a specific MX/MT message type.
 */
public interface PdfParser {
    
    /**
     * Determines if this parser can handle the given message text.
     * @param text The extracted text from the PDF.
     * @return true if this parser is compatible.
     */
    boolean supports(String text);

    /**
     * Parses the text into a MessageHistory entity.
     * @param text The extracted text from the PDF.
     * @return A populated MessageHistory object.
     */
    MessageHistory parse(String text);
    
    /**
     * Gets the priority of the parser (lower means higher priority).
     * Useful if multiple parsers match, to pick the most specific one.
     */
    default int getPriority() {
        return 100;
    }
}

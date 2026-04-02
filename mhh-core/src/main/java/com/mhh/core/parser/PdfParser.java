package com.mhh.core.parser;

import com.mhh.common.entity.SwiftMessageBase;

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
     * Parses the text into a concrete {@link SwiftMessageBase} subtype
     * ({@link com.mhh.common.entity.MsgIncoming} or {@link com.mhh.common.entity.MsgOutgoing}).
     * @param text The extracted text from the PDF.
     * @return A populated entity object.
     */
    SwiftMessageBase parse(String text);

    /**
     * Gets the priority of the parser (lower means higher priority).
     * Useful if multiple parsers match, to pick the most specific one.
     */
    default int getPriority() {
        return 100;
    }
}

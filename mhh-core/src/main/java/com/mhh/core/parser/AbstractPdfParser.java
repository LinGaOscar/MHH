package com.mhh.core.parser;

import com.mhh.common.entity.SwiftMessageBase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for all PDF message parsers with common utility methods.
 */
public abstract class AbstractPdfParser implements PdfParser {

    /**
     * Common helper to extract structured fields using regex from the raw text.
     */
    protected String extract(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Subclasses implement specific parsing logic and return the appropriate
     * direction subtype (MsgIncoming or MsgOutgoing).
     */
    @Override
    public abstract SwiftMessageBase parse(String text);

    /**
     * Basic check for a message type code like "MT103" or "pacs.008".
     */
    protected boolean contains(String text, String keyword) {
        return text != null && text.contains(keyword);
    }
}

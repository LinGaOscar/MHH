package com.mhh.core.parser;

import com.mhh.common.entity.MessageHistory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic parser for all SWIFT MT messages extracted from PDF.
 * Handles any MT type by extracting common SWIFT Block 4 tags.
 * Acts as a catch-all fallback; specific parsers (e.g. Mt103Parser) use
 * a lower priority number to take precedence.
 *
 * Supported tag extraction:
 *   :20:  → messageId
 *   :32A/B: → amount, currency (and value date)
 *   :50A/K/J: or :52A/D: → sender
 *   :59/59A: or :57A/D: or :58A/D: → receiver
 *   :21: or :70: → reference
 */
@Component
public class MtGenericParser extends AbstractPdfParser {

    private static final Pattern MT_TYPE_PATTERN =
            Pattern.compile("MT\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE);

    // Match :TAG: value up to the next :TAG: or end of string
    private static final Pattern TAG_PATTERN =
            Pattern.compile(":(\\d{2}[A-Z]?):\\s*(.*?)(?=:\\d{2}[A-Z]?:|\\z)", Pattern.DOTALL);

    @Override
    public boolean supports(String text) {
        return text != null && MT_TYPE_PATTERN.matcher(text).find();
    }

    @Override
    public int getPriority() {
        return 200; // fallback — specific parsers should use a lower number
    }

    @Override
    public MessageHistory parse(String text) {
        MessageHistory msg = new MessageHistory();

        // Detect message type
        Matcher typeMatcher = MT_TYPE_PATTERN.matcher(text);
        if (typeMatcher.find()) {
            msg.setMessageType("MT" + typeMatcher.group(1));
        }

        // :20: → messageId
        msg.setMessageId(extractTag(text, "20"));

        // :32A: → YYMMDD + CCY + AMOUNT   or   :32B: → CCY + AMOUNT
        String field32 = extractTag(text, "32A");
        if (field32 == null) field32 = extractTag(text, "32B");
        if (field32 != null) {
            parseAmountField(msg, field32);
        }

        // sender: :50A/K/J: > :52A/D:
        String sender = firstNonNull(
                extractTag(text, "50A"),
                extractTag(text, "50K"),
                extractTag(text, "50J"),
                extractTag(text, "52A"),
                extractTag(text, "52D")
        );
        msg.setSender(extractFirstLine(sender));

        // receiver: :59: > :59A: > :57A/D: > :58A/D:
        String receiver = firstNonNull(
                extractTag(text, "59"),
                extractTag(text, "59A"),
                extractTag(text, "57A"),
                extractTag(text, "57D"),
                extractTag(text, "58A"),
                extractTag(text, "58D")
        );
        msg.setReceiver(extractFirstLine(receiver));

        // reference: :21: > :70:
        String ref = firstNonNull(
                extractTag(text, "21"),
                extractTag(text, "70")
        );
        if (ref != null) {
            msg.setReference(ref.replaceAll("\\s+", " ").trim());
        }

        msg.setContent(text);
        return msg;
    }

    /**
     * Extract the value of a specific SWIFT tag (exact match).
     * e.g. extractTag(text, "32A") matches :32A: ... until next tag
     */
    private String extractTag(String text, String tag) {
        Pattern p = Pattern.compile(":" + Pattern.quote(tag) + ":\\s*(.*?)(?=:\\d{2}[A-Z]?:|\\z)",
                Pattern.DOTALL);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Parse :32A: → YYMMDDCCCAMOUNT  (e.g. "230101USD10000,50")
     * Parse :32B: → CCCAMOUNT        (e.g. "USD10000,50")
     * Comma is the SWIFT decimal separator.
     */
    private void parseAmountField(MessageHistory msg, String field) {
        String compact = field.replaceAll("\\s+", "");

        // :32A: format: 6 digits + 3-letter CCY + amount
        Matcher m32a = Pattern.compile("^\\d{6}([A-Z]{3})([\\d,]+)").matcher(compact);
        if (m32a.find()) {
            msg.setCurrency(m32a.group(1));
            setAmount(msg, m32a.group(2));
            return;
        }

        // :32B: format: 3-letter CCY + amount
        Matcher m32b = Pattern.compile("^([A-Z]{3})([\\d,]+)").matcher(compact);
        if (m32b.find()) {
            msg.setCurrency(m32b.group(1));
            setAmount(msg, m32b.group(2));
        }
    }

    private void setAmount(MessageHistory msg, String raw) {
        try {
            // SWIFT uses comma as decimal separator
            msg.setAmount(new BigDecimal(raw.replace(",", ".")));
        } catch (NumberFormatException ignored) {
            // Amount field exists but couldn't be parsed — leave null
        }
    }

    /**
     * For party fields (sender/receiver), skip leading account line (starts with '/')
     * and return the first meaningful line as the name or BIC.
     */
    private String extractFirstLine(String raw) {
        if (raw == null) return null;
        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("/")) {
                return t;
            }
        }
        return raw.trim();
    }

    private String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) return v;
        }
        return null;
    }
}

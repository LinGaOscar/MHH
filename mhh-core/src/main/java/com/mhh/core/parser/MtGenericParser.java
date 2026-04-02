package com.mhh.core.parser;

import com.mhh.common.entity.MsgIncoming;
import com.mhh.common.entity.SwiftMessageBase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic parser for all SWIFT MT messages extracted from PDF.
 * Acts as a catch-all fallback; specific parsers (e.g. Mt103Parser) use
 * a lower priority number to take precedence.
 * Returns {@link MsgIncoming} — MT messages parsed from PDF/SWAL are received messages.
 */
@Component
public class MtGenericParser extends AbstractPdfParser {

    private static final Pattern MT_TYPE_PATTERN =
            Pattern.compile("MT\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String text) {
        return text != null && MT_TYPE_PATTERN.matcher(text).find();
    }

    @Override
    public int getPriority() {
        return 200; // fallback — specific parsers should use a lower number
    }

    @Override
    public SwiftMessageBase parse(String text) {
        MsgIncoming msg = new MsgIncoming();

        Matcher typeMatcher = MT_TYPE_PATTERN.matcher(text);
        if (typeMatcher.find()) {
            msg.setMessageType("MT" + typeMatcher.group(1));
        }

        msg.setMessageId(extractTag(text, "20"));

        String field32 = extractTag(text, "32A");
        if (field32 == null) field32 = extractTag(text, "32B");
        if (field32 != null) {
            parseAmountField(msg, field32);
        }

        String sender = firstNonNull(
                extractTag(text, "50A"), extractTag(text, "50K"),
                extractTag(text, "50J"), extractTag(text, "52A"),
                extractTag(text, "52D")
        );
        msg.setSender(extractFirstLine(sender));

        String receiver = firstNonNull(
                extractTag(text, "59"),  extractTag(text, "59A"),
                extractTag(text, "57A"), extractTag(text, "57D"),
                extractTag(text, "58A"), extractTag(text, "58D")
        );
        msg.setReceiver(extractFirstLine(receiver));

        String ref = firstNonNull(extractTag(text, "21"), extractTag(text, "70"));
        if (ref != null) {
            msg.setReference(ref.replaceAll("\\s+", " ").trim());
        }

        msg.setContent(text);
        return msg;
    }

    private String extractTag(String text, String tag) {
        Pattern p = Pattern.compile(":" + Pattern.quote(tag) + ":\\s*(.*?)(?=:\\d{2}[A-Z]?:|\\z)",
                Pattern.DOTALL);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private void parseAmountField(MsgIncoming msg, String field) {
        String compact = field.replaceAll("\\s+", "");
        Matcher m32a = Pattern.compile("^\\d{6}([A-Z]{3})([\\d,]+)").matcher(compact);
        if (m32a.find()) {
            msg.setCurrency(m32a.group(1));
            setAmount(msg, m32a.group(2));
            return;
        }
        Matcher m32b = Pattern.compile("^([A-Z]{3})([\\d,]+)").matcher(compact);
        if (m32b.find()) {
            msg.setCurrency(m32b.group(1));
            setAmount(msg, m32b.group(2));
        }
    }

    private void setAmount(MsgIncoming msg, String raw) {
        try {
            msg.setAmount(new BigDecimal(raw.replace(",", ".")));
        } catch (NumberFormatException ignored) {}
    }

    private String extractFirstLine(String raw) {
        if (raw == null) return null;
        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("/")) return t;
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

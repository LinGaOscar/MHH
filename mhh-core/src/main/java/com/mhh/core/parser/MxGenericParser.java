package com.mhh.core.parser;

import com.mhh.common.entity.MessageHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic parser for all ISO 20022 (MX) messages extracted from PDF.
 * Supports all known MX families: pacs, camt, pain, admi, trck, xsys.
 *
 * PDFs may contain either:
 *   (a) Raw XML  → parsed via XML tag patterns
 *   (b) Formatted report text → parsed via label patterns
 * Both strategies are attempted for each field.
 *
 * Field mapping:
 *   GrpHdr/MsgId          → messageId
 *   Document root element → messageType  (e.g. pacs.008.001.08)
 *   Dbtr/Nm or InstgAgt   → sender
 *   Cdtr/Nm or InstdAgt   → receiver
 *   IntrBkSttlmAmt / Amt  → amount + currency
 *   EndToEndId / InstrId  → reference
 */
@Component
@Slf4j
public class MxGenericParser extends AbstractPdfParser {

    private static final Pattern MX_TYPE_PATTERN = Pattern.compile(
            "(pacs|camt|pain|admi|trck|xsys)\\.\\d{3}\\.\\d{3}\\.\\d{2}",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean supports(String text) {
        return text != null && MX_TYPE_PATTERN.matcher(text).find();
    }

    @Override
    public int getPriority() {
        return 200; // fallback — specific parsers can use lower number
    }

    @Override
    public MessageHistory parse(String text) {
        MessageHistory msg = new MessageHistory();

        // Detect message type from namespace or content
        Matcher typeMatcher = MX_TYPE_PATTERN.matcher(text);
        if (typeMatcher.find()) {
            msg.setMessageType(typeMatcher.group(0).toLowerCase());
        }

        // messageId: GrpHdr/MsgId  or  Assgnmt/Id
        String msgId = firstNonNull(
                extractXml(text, "MsgId"),
                extractXml(text, "Id")
        );
        msg.setMessageId(msgId);

        // amount + currency
        String amtRaw = firstNonNull(
                extractXml(text, "IntrBkSttlmAmt"),
                extractXml(text, "TtlIntrBkSttlmAmt"),
                extractXml(text, "RtrdIntrBkSttlmAmt"),
                extractXml(text, "Amt"),
                extractXml(text, "TtlChrgsAndTaxAmt")
        );
        if (amtRaw != null) {
            parseAmount(msg, text, amtRaw);
        }

        // sender: Dbtr > InstgAgt
        String sender = firstNonNull(
                extractXml(text, "Dbtr/Nm"),
                extractXml(text, "Dbtr/FinInstnId/Nm"),
                extractXml(text, "Dbtr/FinInstnId/BICFI"),
                extractXml(text, "InstgAgt/FinInstnId/Nm"),
                extractXml(text, "InstgAgt/FinInstnId/BICFI"),
                extractXml(text, "InitgPty/Nm")
        );
        msg.setSender(sender);

        // receiver: Cdtr > InstdAgt
        String receiver = firstNonNull(
                extractXml(text, "Cdtr/Nm"),
                extractXml(text, "Cdtr/FinInstnId/Nm"),
                extractXml(text, "Cdtr/FinInstnId/BICFI"),
                extractXml(text, "InstdAgt/FinInstnId/Nm"),
                extractXml(text, "InstdAgt/FinInstnId/BICFI")
        );
        msg.setReceiver(receiver);

        // reference: EndToEndId > InstrId > OrgnlInstrId
        String ref = firstNonNull(
                extractXml(text, "EndToEndId"),
                extractXml(text, "InstrId"),
                extractXml(text, "OrgnlInstrId"),
                extractXml(text, "OrgnlEndToEndId")
        );
        msg.setReference(ref);

        msg.setContent(text);
        return msg;
    }

    /**
     * Extract value for an XML element path (uses only the last segment for matching).
     *
     * Strategy 1 — XML tag:   <MsgId>VALUE</MsgId>
     * Strategy 2 — Label:     MsgId: VALUE  or  MsgId VALUE
     */
    private String extractXml(String text, String path) {
        String tag = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;

        // Strategy 1: XML element
        Pattern xmlPat = Pattern.compile(
                "<" + Pattern.quote(tag) + "(?:\\s[^>]*)?>([^<]+)</" + Pattern.quote(tag) + ">",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = xmlPat.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }

        // Strategy 2: label-based (formatted PDF report)
        Pattern labelPat = Pattern.compile(
                "\\b" + Pattern.quote(tag) + "\\s*:?\\s*([^\\n<:]{1,100})",
                Pattern.CASE_INSENSITIVE
        );
        m = labelPat.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }

        return null;
    }

    /**
     * Parse amount and currency.
     * XML attribute form:  <IntrBkSttlmAmt Ccy="USD">10000.00</IntrBkSttlmAmt>
     * Plain text form:     USD 10000.00
     */
    private void parseAmount(MessageHistory msg, String fullText, String amtRaw) {
        // Try to find Ccy attribute near the amount tag
        Matcher ccyAttr = Pattern.compile("Ccy\\s*=\\s*[\"']([A-Z]{3})[\"']", Pattern.CASE_INSENSITIVE)
                .matcher(fullText);
        if (ccyAttr.find()) {
            msg.setCurrency(ccyAttr.group(1));
        }

        // Try to extract CCY code prepended in the value: "USD10000.00"
        Matcher ccyInline = Pattern.compile("^([A-Z]{3})\\s*([\\d.]+)").matcher(amtRaw.trim());
        if (ccyInline.find()) {
            msg.setCurrency(ccyInline.group(1));
            setAmount(msg, ccyInline.group(2));
            return;
        }

        // Plain number
        setAmount(msg, amtRaw.replaceAll("[^\\d.]", ""));
    }

    private void setAmount(MessageHistory msg, String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            msg.setAmount(new BigDecimal(raw));
        } catch (NumberFormatException ignored) {
            // Amount field exists but couldn't be parsed — leave null
        }
    }

    private String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}

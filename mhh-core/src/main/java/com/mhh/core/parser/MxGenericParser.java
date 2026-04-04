package com.mhh.core.parser;

import com.mhh.common.entity.MsgIncoming;
import com.mhh.common.entity.SwiftMessageBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic parser for all ISO 20022 (MX) messages extracted from PDF.
 * Returns {@link MsgIncoming} — MX messages parsed from PDF/SWAL are received messages.
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
        return 200;
    }

    @Override
    public SwiftMessageBase parse(String text) {
        MsgIncoming msg = new MsgIncoming();

        Matcher typeMatcher = MX_TYPE_PATTERN.matcher(text);
        if (typeMatcher.find()) {
            String mxType = typeMatcher.group(0).toLowerCase();
            msg.setMxType(mxType);
            msg.setMessageType(mxType);
        }

        String msgId = firstNonNull(extractXml(text, "MsgId"), extractXml(text, "Id"));
        msg.setMessageId(msgId);

        String amtRaw = firstNonNull(
                extractXml(text, "IntrBkSttlmAmt"),
                extractXml(text, "TtlIntrBkSttlmAmt"),
                extractXml(text, "RtrdIntrBkSttlmAmt"),
                extractXml(text, "Amt"),
                extractXml(text, "TtlChrgsAndTaxAmt")
        );
        if (amtRaw != null) parseAmount(msg, text, amtRaw);

        msg.setSender(firstNonNull(
                extractXml(text, "Dbtr/Nm"),
                extractXml(text, "Dbtr/FinInstnId/Nm"),
                extractXml(text, "Dbtr/FinInstnId/BICFI"),
                extractXml(text, "InstgAgt/FinInstnId/Nm"),
                extractXml(text, "InstgAgt/FinInstnId/BICFI"),
                extractXml(text, "InitgPty/Nm")
        ));

        msg.setReceiver(firstNonNull(
                extractXml(text, "Cdtr/Nm"),
                extractXml(text, "Cdtr/FinInstnId/Nm"),
                extractXml(text, "Cdtr/FinInstnId/BICFI"),
                extractXml(text, "InstdAgt/FinInstnId/Nm"),
                extractXml(text, "InstdAgt/FinInstnId/BICFI")
        ));

        msg.setReference(firstNonNull(
                extractXml(text, "EndToEndId"),
                extractXml(text, "InstrId"),
                extractXml(text, "OrgnlInstrId"),
                extractXml(text, "OrgnlEndToEndId")
        ));

        msg.setMxContent(text);
        return msg;
    }

    private String extractXml(String text, String path) {
        String tag = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;

        Pattern xmlPat = Pattern.compile(
                "<" + Pattern.quote(tag) + "(?:\\s[^>]*)?>([^<]+)</" + Pattern.quote(tag) + ">",
                Pattern.CASE_INSENSITIVE);
        Matcher m = xmlPat.matcher(text);
        if (m.find()) return m.group(1).trim();

        Pattern labelPat = Pattern.compile(
                "\\b" + Pattern.quote(tag) + "\\s*:?\\s*([^\\n<:]{1,100})",
                Pattern.CASE_INSENSITIVE);
        m = labelPat.matcher(text);
        if (m.find()) return m.group(1).trim();

        return null;
    }

    private void parseAmount(MsgIncoming msg, String fullText, String amtRaw) {
        Matcher ccyAttr = Pattern.compile("Ccy\\s*=\\s*[\"']([A-Z]{3})[\"']", Pattern.CASE_INSENSITIVE)
                .matcher(fullText);
        if (ccyAttr.find()) msg.setCurrency(ccyAttr.group(1));

        Matcher ccyInline = Pattern.compile("^([A-Z]{3})\\s*([\\d.]+)").matcher(amtRaw.trim());
        if (ccyInline.find()) {
            msg.setCurrency(ccyInline.group(1));
            setAmount(msg, ccyInline.group(2));
            return;
        }
        setAmount(msg, amtRaw.replaceAll("[^\\d.]", ""));
    }

    private void setAmount(MsgIncoming msg, String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            msg.setAmount(new BigDecimal(raw));
        } catch (NumberFormatException ignored) {}
    }

    private String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}

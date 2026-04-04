package com.mhh.batch.job;

import com.mhh.common.entity.MsgIncoming;
import com.mhh.common.entity.MsgIncomingTx;
import com.mhh.common.entity.MsgOutgoing;
import com.mhh.common.entity.MsgOutgoingTx;
import com.mhh.common.entity.SwiftMessageBase;
import com.mhh.common.repository.MsgIncomingRepository;
import com.mhh.common.repository.MsgIncomingTxRepository;
import com.mhh.common.repository.MsgOutgoingRepository;
import com.mhh.common.repository.MsgOutgoingTxRepository;
import com.mhh.core.parser.ParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Job to scan MX/MT folders, extract PDF text, parse and save to MSG_INCOMING or MSG_OUTGOING.
 *
 * Flow per file:
 *   1. Extract text via PDFBox.
 *   2. Delegate to ParserFactory (Strategy pattern).
 *   3a. Parse success → save to correct table → move to ARCHIVE.
 *   3b. No parser found → move to ERROR (isolated for manual review).
 *   3c. Exception → log error, leave file in place for retry next run.
 */
@Component("pdfImportJob")
@Slf4j
public class PdfImportJob implements MhhJob {

    private final ParserFactory parserFactory;
    private final MsgIncomingRepository incomingRepository;
    private final MsgIncomingTxRepository incomingTxRepository;
    private final MsgOutgoingRepository outgoingRepository;
    private final MsgOutgoingTxRepository outgoingTxRepository;

    @Value("${mhh.paths.mx}")
    private String mxPath;

    @Value("${mhh.paths.mt}")
    private String mtPath;

    @Value("${mhh.paths.archive}")
    private String archivePath;

    @Value("${mhh.paths.error}")
    private String errorPath;

    public PdfImportJob(ParserFactory parserFactory,
                        MsgIncomingRepository incomingRepository,
                        MsgIncomingTxRepository incomingTxRepository,
                        MsgOutgoingRepository outgoingRepository,
                        MsgOutgoingTxRepository outgoingTxRepository) {
        this.parserFactory = parserFactory;
        this.incomingRepository = incomingRepository;
        this.incomingTxRepository = incomingTxRepository;
        this.outgoingRepository = outgoingRepository;
        this.outgoingTxRepository = outgoingTxRepository;
    }

    @Override
    public String getJobName() {
        return "PdfImportJob";
    }

    @Override
    public void execute() {
        log.info("Starting PdfImportJob...");
        processFolder(mxPath);
        processFolder(mtPath);
        log.info("PdfImportJob completed.");
    }

    private void processFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Folder does not exist or is not a directory: {}", folderPath);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files == null || files.length == 0) {
            log.info("No PDF files found in {}.", folderPath);
            return;
        }

        for (File file : files) {
            processFile(file);
        }
    }

    private void processFile(File file) {
        log.info("Processing PDF: {}", file.getName());
        try {
            String text = extractText(file);
            Optional<SwiftMessageBase> result = parserFactory.parse(text);

            if (result.isPresent()) {
                SwiftMessageBase msg = result.get();
                if (msg.getMessageId() == null) msg.setMessageId(file.getName());
                msg.setSource("PDF");
                msg.setSyncTime(LocalDateTime.now());

                if (msg instanceof MsgIncoming) {
                    incomingRepository.save((MsgIncoming) msg);
                    saveTx(incomingTxRepository, msg);
                } else if (msg instanceof MsgOutgoing) {
                    outgoingRepository.save((MsgOutgoing) msg);
                    saveTx(outgoingTxRepository, msg);
                }

                log.info("Successfully imported {} as {} ({})",
                        file.getName(), msg.getMessageType(), msg.getClass().getSimpleName());
                moveFile(file, archivePath, "archive");
            } else {
                log.warn("No suitable parser found for PDF: {} — moving to error folder.", file.getName());
                moveFile(file, errorPath, "error");
            }
        } catch (Exception e) {
            log.error("Failed to process PDF {}: {}", file.getName(), e.getMessage(), e);
        }
    }

    private void saveTx(MsgIncomingTxRepository txRepo, SwiftMessageBase msg) {
        if (msg.getMtContent() == null && msg.getMxContent() == null) return;
        MsgIncomingTx tx = new MsgIncomingTx();
        tx.setMessageId(msg.getMessageId());
        tx.setMtContent(msg.getMtContent());
        tx.setMxContent(msg.getMxContent());
        tx.setSyncTime(LocalDateTime.now());
        txRepo.save(tx);
    }

    private void saveTx(MsgOutgoingTxRepository txRepo, SwiftMessageBase msg) {
        if (msg.getMtContent() == null && msg.getMxContent() == null) return;
        MsgOutgoingTx tx = new MsgOutgoingTx();
        tx.setMessageId(msg.getMessageId());
        tx.setMtContent(msg.getMtContent());
        tx.setMxContent(msg.getMxContent());
        tx.setSyncTime(LocalDateTime.now());
        txRepo.save(tx);
    }

    private String extractText(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private void moveFile(File file, String targetDir, String label) {
        try {
            Files.createDirectories(Paths.get(targetDir));
            Path target = Paths.get(targetDir, file.getName());

            if (Files.exists(target)) {
                String name = file.getName();
                int dot = name.lastIndexOf('.');
                String base = dot >= 0 ? name.substring(0, dot) : name;
                String ext  = dot >= 0 ? name.substring(dot) : "";
                target = Paths.get(targetDir, base + "_" + System.currentTimeMillis() + ext);
            }

            Files.move(file.toPath(), target, StandardCopyOption.ATOMIC_MOVE);
            log.info("Moved {} to {} folder: {}", file.getName(), label, target);
        } catch (IOException e) {
            log.error("Failed to move {} to {} folder: {}", file.getName(), label, e.getMessage());
        }
    }
}

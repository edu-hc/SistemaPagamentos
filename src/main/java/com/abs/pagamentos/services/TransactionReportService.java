package com.abs.pagamentos.services;

import com.abs.pagamentos.model.transaction.Transaction;
import com.abs.pagamentos.repositories.TransactionRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class TransactionReportService {

    private S3Service s3Service;
    private TransactionRepository transactionRepository;

    @Autowired
    public TransactionReportService(S3Service s3Service, TransactionRepository transactionRepository) {
        this.s3Service = s3Service;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Gera e armazena o PDF da transação no S3
     * @param transactionId Transação a ser documentada
     * @return URL do PDF no S3
     */
    public String generateAndStoreTransactionPdf(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada"));

        try {
            byte[] pdfContent = generatePdfContent(transaction);
            String fileName = "receipt_" + transactionId + ".pdf";

            return s3Service.uploadFile(pdfContent, fileName, "application/pdf");

        } catch (Exception e) {
            throw new PdfGenerationException("Falha ao gerar comprovante", e);
        }
    }

    private byte[] generatePdfContent(Transaction transaction) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 36, 36, 50, 50);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, outputStream);
        document.open();

        addDocumentHeader(document, transaction);
        addTransactionDetails(document, transaction);
        addFooter(document);

        document.close();
        return outputStream.toByteArray();
    }

    private void addDocumentHeader(Document document, Transaction transaction) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);

        // Logo (opcional)
        // Image logo = Image.getInstance("path/to/logo.png");
        // logo.scaleToFit(100, 100);
        // logo.setAlignment(Element.ALIGN_CENTER);
        // document.add(logo);

        Paragraph title = new Paragraph("COMPROVANTE DE TRANSAÇÃO", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10f);
        document.add(title);

        Paragraph subtitle = new Paragraph(" - Transação #" + transaction.getId(), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20f);
        document.add(subtitle);

        addDividerLine(document);
    }

    private void addTransactionDetails(Document document, Transaction transaction) throws DocumentException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formattedDate = transaction.getTimestamp().format(formatter);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(20f);

        float borderWidth = 0.5f;
        BaseColor borderColor = BaseColor.LIGHT_GRAY;

        addTableRow(table, "Data/Hora:", formattedDate, borderWidth, borderColor);
        addTableRow(table, "Status:", "CONCLUÍDO", borderWidth, borderColor);
        addTableRow(table, "Remetente:",
                transaction.getSender().getFirstName().concat(transaction.getSender().getLastName()),
                borderWidth, borderColor);
        addTableRow(table, "Destinatário:",
                transaction.getReceiver().getFirstName().concat(transaction.getReceiver().getLastName()),
                borderWidth, borderColor);
        addTableRow(table, "Valor:",
                formatCurrency(transaction.getAmount()),
                borderWidth, borderColor);

        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        addDividerLine(document);

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);

        Paragraph footer = new Paragraph();
        footer.setFont(footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addDividerLine(Document document) throws DocumentException {
        LineSeparator line = new LineSeparator();
        line.setLineWidth(0.5f);
        line.setLineColor(BaseColor.GRAY);

        Paragraph lineParagraph = new Paragraph();
        lineParagraph.add(new Chunk(line));
        lineParagraph.setSpacingBefore(15f);
        lineParagraph.setSpacingAfter(15f);
        document.add(lineParagraph);
    }

    private void addTableRow(PdfPTable table, String label, String value,
                             float borderWidth, BaseColor borderColor) {
        PdfPCell labelCell = createCell(label, true, borderWidth, borderColor);
        PdfPCell valueCell = createCell(value, false, borderWidth, borderColor);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private PdfPCell createCell(String text, boolean isLabel,
                                float borderWidth, BaseColor borderColor) {
        Font font = isLabel
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)
                : FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderWidth(borderWidth);
        cell.setBorderColor(borderColor);
        cell.setPadding(5f);
        cell.setBackgroundColor(BaseColor.WHITE);
        cell.setPaddingLeft(10f);

        if (!isLabel) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }

        return cell;
    }

    private String formatCurrency(BigDecimal amount) {
        return "R$ " + String.format("%,.2f", amount);
    }

    public static class PdfGenerationException extends RuntimeException {
        public PdfGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public byte[] getTransactionPdfContent(Transaction transaction) throws IOException, DocumentException {
        // Gera a chave do arquivo no padrão estabelecido
        String fileKey = "receipt_" + transaction.getId() + ".pdf";

        try {
            // 1. Tenta recuperar do S3 usando o serviço dedicado
            if (s3Service.fileExists(fileKey)) {
                return s3Service.getFile(fileKey);
            }

            // 2. Se não existir, gera um novo PDF
            byte[] pdfContent = generatePdfContent(transaction);

            // 3. Armazena no S3 para futuras requisições
            s3Service.uploadFile(
                    pdfContent,
                    fileKey,
                    "application/pdf"
            );

            return pdfContent;

        } catch (AmazonS3Exception e) {
            throw new IOException("Erro ao acessar o armazenamento S3", e);
        }
    }

}
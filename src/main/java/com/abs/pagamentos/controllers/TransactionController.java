package com.abs.pagamentos.controllers;

import com.abs.pagamentos.dtos.TransactionDTO;
import com.abs.pagamentos.model.transaction.Transaction;
import com.abs.pagamentos.services.TransactionReportService;
import com.abs.pagamentos.services.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionReportService reportService;

    @PostMapping
    public ResponseEntity<Transaction> createTransaction (@RequestBody TransactionDTO transactionDTO) throws Exception {
        Transaction newTransaction = transactionService.createTransaction(transactionDTO);
        String pdfUrl = reportService.generateAndStoreTransactionPdf(newTransaction.getId());
        newTransaction.setReportUrl(pdfUrl);
        return new ResponseEntity<>(newTransaction, HttpStatus.CREATED);

    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id) {
        try {
            Transaction transaction = transactionService.getTransactionById(id);

            byte[] pdfContent = reportService.getTransactionPdfContent(transaction);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);

            String filename = "comprovante_transacao_" + id + ".pdf";
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename(filename)
                            .build());

            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(("Erro ao gerar comprovante: " + e.getMessage()).getBytes());
        }
    }
}

package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.TransactionDTO;

import java.util.List;

public interface TransactionService {
    // Creates a transfer, deposit, or withdrawal for the authenticated user.
    TransactionDTO.TransactionResponse createTransaction(TransactionDTO.CreateRequest request, String userEmail);

    // Returns transactions visible to the authenticated user after applying filters.
    List<TransactionDTO.TransactionResponse> getTransactions(TransactionDTO.FilterRequest filter, String userEmail);

    // Returns one transaction when the authenticated user may view it.
    TransactionDTO.TransactionResponse getTransactionById(Long id, String userEmail);
}

package inholland.nl.banking_project_backend.mappers;

import inholland.nl.banking_project_backend.dtos.AccountResponseDTO;
import inholland.nl.banking_project_backend.dtos.AccountSearchResponseDTO;
import inholland.nl.banking_project_backend.models.AccountModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {

    // Converts a full account entity into an account management response.
    @Mapping(target = "customerEmail", source = "account.customer.user.email")
    @Mapping(target = "customerName", expression = "java(account.getCustomer().getUser().getFirstName() + \" \" + account.getCustomer().getUser().getLastName())")
    AccountResponseDTO toResponse(AccountModel account);

    // Converts an account entity into the lightweight account search response.
    @Mapping(target = "customerName", expression = "java(account.getCustomer().getUser().getFirstName() + \" \" + account.getCustomer().getUser().getLastName())")
    AccountSearchResponseDTO toSearchResponse(AccountModel account);
}

package inholland.nl.banking_project_backend.mappers;

import inholland.nl.banking_project_backend.dtos.AccountDTO;
import inholland.nl.banking_project_backend.models.AccountModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {
    
    @Mapping(target = "customerEmail", source = "account.customer.user.email")
    @Mapping(target = "customerName", expression = "java(account.getCustomer().getUser().getFirstName() + \" \" + account.getCustomer().getUser().getLastName())")
    AccountDTO.AccountResponse toResponse(AccountModel account);
}
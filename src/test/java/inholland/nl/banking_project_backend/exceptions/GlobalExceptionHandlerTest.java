package inholland.nl.banking_project_backend.exceptions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private static final String MESSAGE = "boom";

    private MockMvc mockMvc;

    static Stream<Arguments> bankingExceptions() {
        return Stream.of(
                Arguments.of(
                        "AccountNotFoundException -> 404",
                        (Function<String, BankingException>) AccountNotFoundException::new,
                        HttpStatus.NOT_FOUND
                ),
                Arguments.of(
                        "InactiveAccountException -> 409",
                        (Function<String, BankingException>) InactiveAccountException::new,
                        HttpStatus.CONFLICT
                ),
                Arguments.of(
                        "UnauthorizedAccountAccessException -> 403",
                        (Function<String, BankingException>) UnauthorizedAccountAccessException::new,
                        HttpStatus.FORBIDDEN
                ),
                Arguments.of(
                        "InvalidTransactionException -> 400",
                        (Function<String, BankingException>) InvalidTransactionException::new,
                        HttpStatus.BAD_REQUEST
                ),
                Arguments.of(
                        "LimitExceededException -> 422",
                        (Function<String, BankingException>) LimitExceededException::new,
                        HttpStatus.UNPROCESSABLE_ENTITY
                ),
                Arguments.of(
                        "DailyLimitExceededException -> 422",
                        (Function<String, BankingException>) DailyLimitExceededException::new,
                        HttpStatus.UNPROCESSABLE_ENTITY
                ),
                Arguments.of(
                        "AbsoluteLimitExceededException -> 422",
                        (Function<String, BankingException>) AbsoluteLimitExceededException::new,
                        HttpStatus.UNPROCESSABLE_ENTITY
                ),
                Arguments.of(
                        "CustomerProfileNotFoundException -> 404",
                        (Function<String, BankingException>) CustomerProfileNotFoundException::new,
                        HttpStatus.NOT_FOUND
                ),
                Arguments.of(
                        "UserAlreadyExistsException -> 409",
                        (Function<String, BankingException>) UserAlreadyExistsException::new,
                        HttpStatus.CONFLICT
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bankingExceptions")
    void bankingException_isMappedToItsDeclaredStatusAndBody(
            String label,
            Function<String, BankingException> exceptionFactory,
            HttpStatus expectedStatus
    ) throws Exception {
        BankingException exception = exceptionFactory.apply(MESSAGE);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingControllerStub(exception))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/throw"))
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(jsonPath("$.status").value(expectedStatus.value()))
                .andExpect(jsonPath("$.message").value(MESSAGE))
                .andExpect(jsonPath("$.timestamp").exists());
    }


    @RestController
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    static class ThrowingControllerStub {
        private final BankingException exception;

        ThrowingControllerStub(BankingException exceptionToThrow) {
            this.exception = exceptionToThrow;
        }

        @GetMapping("/throw")
        public void throwIt() {
            throw exception;
        }
    }
}

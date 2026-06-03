package inholland.nl.banking_project_backend.utils;

import org.springframework.stereotype.Component;
import java.math.BigInteger;
import java.util.Random;

@Component
public class IbanGenerator {

    private static final String COUNTRY_CODE = "NL";
    private static final String BANK_CODE = "INHO";
    private final Random random = new Random();

    public String generateDutchIban() {
        StringBuilder accountNumber = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            accountNumber.append(random.nextInt(10));
        }

        String partialIban = BANK_CODE + accountNumber;

        // formula: 98 - (numeric_representation % 97)
        String checkDigits = calculateCheckDigits(partialIban);
        return COUNTRY_CODE + checkDigits + partialIban;
    }

    private String calculateCheckDigits(String partialIban) {
        // N = 23, L = 21 => 2321 + 00
        String numericString = convertToNumeric(partialIban) + "232100";
        BigInteger ibanNumber = new BigInteger(numericString);
        int mod97 = ibanNumber.remainder(BigInteger.valueOf(97)).intValue();
        int checkSum = 98 - mod97;

        return String.format("%02d", checkSum);
    }

    private String convertToNumeric(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(Character.getNumericValue(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

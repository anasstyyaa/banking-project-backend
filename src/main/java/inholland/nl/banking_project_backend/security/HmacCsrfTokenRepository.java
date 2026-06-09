package inholland.nl.banking_project_backend.security;

import inholland.nl.banking_project_backend.services.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class HmacCsrfTokenRepository implements CsrfTokenRepository {

    static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String CSRF_PARAM_NAME  = "_csrf";

    private final JWTService jwtService;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        String value = computeToken(request);
        if (value == null) value = UUID.randomUUID().toString();
        return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAM_NAME, value);
    }


    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        String value = computeToken(request);
        if (value == null) return null;
        return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAM_NAME, value);
    }

    public String computeToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String email = jwtService.extractEmail(authHeader.substring(7));
            if (email == null) return null;

            long day = LocalDate.now(ZoneOffset.UTC).toEpochDay();
            String data = email + ":" + day;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            return null;
        }
    }
}

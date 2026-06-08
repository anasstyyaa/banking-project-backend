package inholland.nl.banking_project_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.mappers.UserMapper;
import inholland.nl.banking_project_backend.services.JWTService;
import inholland.nl.banking_project_backend.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import inholland.nl.banking_project_backend.mappers.UserMapper;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(UserController.class) // Inicializa únicamente el contexto web para este controlador
@AutoConfigureMockMvc(addFilters = false)
class UserControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc; // Simulador de peticiones HTTP de Spring

    @MockBean
    private UserService userService; // Reemplaza el servicio real por un mock dentro del contexto web

    @MockBean
    private JWTService jwtService;

    @MockBean
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper; // Herramienta para convertir objetos Java a cadenas JSON

    @Test
    void dadaUnaSesionActiva_cuandoSeSolicitaPerfil_debeRetornarEstadoOkYDatosDelUsuario() throws Exception {
        // 1. Arrange (Preparar el escenario)
        String emailSimulado = "bank.user@inholland.nl";
        
        UserDTO.ProfileResponse respuestaSimulada = new UserDTO.ProfileResponse(
                emailSimulado,
                "John",
                "Doe",
                "NL01INHO000000001",
                "123456782",
                "+31612345678",
                BigDecimal.ZERO,
                List.of()
        );

        // Simulamos el comportamiento del servicio
        when(userService.getProfile(emailSimulado)).thenReturn(respuestaSimulada);

        // Simulamos un objeto Principal para representar al usuario logueado en Spring Security
        Principal principalSimulado = Mockito.mock(Principal.class);
        when(principalSimulado.getName()).thenReturn(emailSimulado);

        // 2. Act & 3. Assert (Actuar y Verificar en cadena con MockMvc)
        mockMvc.perform(get("/api/v1/users/profile")
                        .principal(principalSimulado) // Adjuntamos el usuario autenticado a la petición
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print()) // Imprime los detalles HTTP por consola para debugging
                .andExpect(status().isOk()) // Verifica HTTP 200 OK
                .andExpect(jsonPath("$.email", is(emailSimulado))) // Valida el contenido del JSON
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")));
    }

    @Test
    void dadoUnPayloadValido_cuandoSeActualizaPerfil_debeRetornarEstadoOkYConfirmacion() throws Exception {
        // 1. Arrange (Preparar el escenario)
        UserDTO.UpdateProfileRequest peticionDto = new UserDTO.UpdateProfileRequest(
                "new.email@inholland.nl",
                "+31612345678"
        );

        UserDTO.UpdateProfileResponse respuestaSimulada = new UserDTO.UpdateProfileResponse(
                "new.email@inholland.nl",
                "John",
                "Doe",
                "NL01INHO000000001",
                "123456782",
                "+31612345678",
                BigDecimal.ZERO,
                List.of(),
                "newToken",
                inholland.nl.banking_project_backend.models.RoleEnum.ROLE_CUSTOMER
        );

        // Configuramos el mock del servicio
        when(userService.updateProfile(any(UserDTO.UpdateProfileRequest.class))).thenReturn(respuestaSimulada);

        // Convertimos el objeto de petición Java a una cadena de texto JSON
        String jsonRequestBody = objectMapper.writeValueAsString(peticionDto);

        // 2. Act & 3. Assert
        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequestBody)) // Enviamos el JSON en el cuerpo del método PUT
                .andDo(print())
                .andExpect(status().isOk()) // Verifica HTTP 200 OK
                .andExpect(jsonPath("$.email", is("new.email@inholland.nl")))
                .andExpect(jsonPath("$.token", is("newToken")));
    }
}

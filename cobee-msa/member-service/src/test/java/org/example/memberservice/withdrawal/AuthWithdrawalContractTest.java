package org.example.memberservice.withdrawal;
import java.util.*;
import org.example.common.apiPayload.error.handler.GlobalExceptionHandler;
import org.example.memberservice.controller.AuthController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
public class AuthWithdrawalContractTest {
    org.springframework.test.web.servlet.MockMvc mvc() throws Exception {
        var c = AuthController.class.getConstructors()[0];
        Object[] args = Arrays.stream(c.getParameterTypes()).map(t -> mock(t)).toArray();
        return MockMvcBuilders.standaloneSetup(c.newInstance(args)).setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
    @Test
    void withdrawalIsAcceptedBeforeBackgroundCleanupCompletes() throws Exception {
        mvc().perform(post("/auth/withdraw").header("X-USER-ID", "7")).andExpect(status().isAccepted());
    }
    @Test
    void callerCanReadOwnWithdrawalStatus() throws Exception {
        mvc().perform(get("/auth/withdrawal").header("X-USER-ID", "7")).andExpect(status().isOk());
    }
}

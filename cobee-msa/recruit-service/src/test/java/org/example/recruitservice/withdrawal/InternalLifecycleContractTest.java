package org.example.recruitservice.withdrawal;
import java.net.URI;
import java.util.*;
import feign.*;
import org.example.common.interceptor.InternalApiFilter;
import org.example.memberservice.client.RecruitClient;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
public class InternalLifecycleContractTest {
    @Test
    void userIdHeaderCannotReplaceInternalCredential() throws Exception {
        var target = mock(RecruitMemberLifecycle.class);
        var mvc = MockMvcBuilders.standaloneSetup(new RecruitLifecycleController(target))
                .addFilters(new InternalApiFilter("fixture-key")).build();
        mvc.perform(put("/internal/members/8/activate").header("X-USER-ID", "7")).andExpect(status().isUnauthorized());
        verifyNoInteractions(target);
    }
    @Test
    void realFeignContractProvidesTargetIdOperationAndServiceKey() throws Exception {
        var target = mock(RecruitMemberLifecycle.class);
        var mvc = MockMvcBuilders.standaloneSetup(new RecruitLifecycleController(target))
                .addFilters(new InternalApiFilter("fixture-key")).build();
        Client transport = (request, options) -> {
            try {
                var builder = request(org.springframework.http.HttpMethod.valueOf(request.httpMethod().name()),
                        URI.create(request.url()).getPath());
                request.headers().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
                var response = mvc.perform(builder).andReturn().getResponse();
                return Response.builder().request(request).status(response.getStatus()).reason("fixture")
                        .headers(Map.of()).body(new byte[0]).build();
            } catch (Exception error) {
                throw new java.io.IOException(error);
            }
        };
        var client = Feign.builder().contract(new SpringMvcContract()).client(transport).target(RecruitClient.class,
                "http://fixture.invalid");
        UUID operation = UUID.randomUUID();
        client.activate(8, "fixture-key");
        client.block(8, operation, "fixture-key");
        client.cleanup(8, operation, "fixture-key");
        verify(target).activate(8);
        verify(target).block(8, operation);
        verify(target).cleanup(8, operation);
    }
}

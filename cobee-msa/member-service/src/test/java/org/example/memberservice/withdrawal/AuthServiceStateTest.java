package org.example.memberservice.withdrawal;

import java.util.*;
import org.example.memberservice.domain.Member;
import org.example.memberservice.jwt.JwtProvider;
import org.example.memberservice.repository.MemberRepository;
import org.example.memberservice.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceStateTest {
    final JwtProvider jwt = mock(JwtProvider.class);
    final MemberRepository members = mock(MemberRepository.class);
    @SuppressWarnings("unchecked")
    final RedisTemplate<String, String> redis = mock(RedisTemplate.class);
    final MemberAccessRegistry access = mock(MemberAccessRegistry.class);
    AuthService service() throws Exception {
        var dependencies = Map.<Class<?>, Object>of(JwtProvider.class, jwt, MemberRepository.class, members,
                RedisTemplate.class, redis, MemberAccessRegistry.class, access);
        var constructor = AuthService.class.getConstructors()[0];
        return (AuthService) constructor
                .newInstance(Arrays.stream(constructor.getParameterTypes()).map(dependencies::get).toArray());
    }
    @Test
    void invalidRefreshIsUnauthorizedWithoutStoreAccess() throws Exception {
        var target = service();
        var error = assertThrows(ResponseStatusException.class, () -> target.refreshTokens("fixture-invalid"));
        assertEquals(401, error.getStatusCode().value());
        verifyNoInteractions(redis, access, members);
    }
    @Test
    @SuppressWarnings("unchecked")
    void removedRefreshIsUnauthorized() throws Exception {
        when(jwt.validateToken("fixture")).thenReturn(true);
        when(jwt.getMemberIdFromToken("fixture")).thenReturn("7");
        when(members.findById(7L))
                .thenReturn(Optional.of(Member.builder().socialId("fixture").isCompleted(true).build()));
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var target = service();
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> target.refreshTokens("fixture"))
                .getStatusCode().value());
        verifyNoInteractions(access);
    }
    @Test
    void withdrawingMemberCannotStartRefresh() throws Exception {
        when(jwt.validateToken("fixture")).thenReturn(true);
        when(jwt.getMemberIdFromToken("fixture")).thenReturn("7");
        var member = Member.builder().socialId("fixture").isCompleted(true).build();
        ReflectionTestUtils.setField(member, "lifecycleStatus", "WITHDRAWING");
        when(members.findById(7L)).thenReturn(Optional.of(member));
        var target = service();
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> target.refreshTokens("fixture"))
                .getStatusCode().value());
        verifyNoInteractions(redis, access);
    }
}

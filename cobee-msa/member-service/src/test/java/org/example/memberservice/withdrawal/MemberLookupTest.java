package org.example.memberservice.withdrawal;

import java.util.Optional;
import org.example.common.nhn.NhnStorageService;
import org.example.memberservice.domain.Member;
import org.example.memberservice.repository.MemberRepository;
import org.example.memberservice.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MemberLookupTest {
    @Test
    void internalDisplayLookupRetainsExistingBehaviorWhileWithdrawalIsPending() {
        var member = Member.builder().socialId("fixture").name("fixture").birthDate("1999-01-01").isCompleted(true)
                .ocrValidation(false).build();
        ReflectionTestUtils.setField(member, "id", 7L);
        ReflectionTestUtils.setField(member, "lifecycleStatus", "WITHDRAWING");
        var repository = mock(MemberRepository.class);
        when(repository.findById(7L)).thenReturn(Optional.of(member));
        // Recruit also uses this lookup for other comment authors, not only the
        // authenticated actor.
        var service = new MemberService(repository, mock(NhnStorageService.class));
        assertDoesNotThrow(() -> service.getMyInfo(7L));
    }
}

package org.example.memberservice.service;

import org.example.memberservice.domain.Member;
import org.example.memberservice.dto.sync.MemberSyncDto;
import org.example.memberservice.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberSyncServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberSyncService service;

    @Test
    void initialSyncReturnsAllMembersWithTheirSyncFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime updatedAt = createdAt.plusDays(1);
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(42L);
        when(member.getGender()).thenReturn("MAN");
        when(member.getBirthDate()).thenReturn("2000-01-02");
        when(member.getCreatedAt()).thenReturn(createdAt);
        when(member.getUpdatedAt()).thenReturn(updatedAt);
        when(memberRepository.findAllWithDetails()).thenReturn(List.of(member));

        List<MemberSyncDto> result = service.getUpdatedMembers(null);

        assertThat(result).hasSize(1);
        MemberSyncDto dto = result.getFirst();
        assertThat(dto.getMember_id()).isEqualTo(42L);
        assertThat(dto.getGender()).isEqualTo("MAN");
        assertThat(dto.getBirth_date()).isEqualTo("2000-01-02");
        assertThat(dto.getCreated_at()).isEqualTo(createdAt);
        assertThat(dto.getUpdated_at()).isEqualTo(updatedAt);
        assertThat(dto.getPreferred_gender()).isNull();
        assertThat(dto.getMy_lifestyle()).isNull();
        verify(memberRepository).findAllWithDetails();
        verifyNoMoreInteractions(memberRepository);
    }

    @Test
    void incrementalSyncUsesTheExactRequestedTimestamp() {
        LocalDateTime lastSyncTime = LocalDateTime.of(2026, 10, 2, 10, 30);
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(84L);
        when(memberRepository.findAllByUpdatedAtAfter(lastSyncTime))
                .thenReturn(List.of(member));

        List<MemberSyncDto> result = service.getUpdatedMembers(lastSyncTime);

        assertThat(result).extracting(MemberSyncDto::getMember_id).containsExactly(84L);
        verify(memberRepository).findAllByUpdatedAtAfter(lastSyncTime);
        verifyNoMoreInteractions(memberRepository);
    }

    @Test
    void incrementalSyncReturnsAnEmptyListWhenNothingChanged() {
        LocalDateTime lastSyncTime = LocalDateTime.of(2026, 10, 2, 10, 30);
        when(memberRepository.findAllByUpdatedAtAfter(lastSyncTime)).thenReturn(List.of());

        assertThat(service.getUpdatedMembers(lastSyncTime)).isEmpty();

        verify(memberRepository).findAllByUpdatedAtAfter(lastSyncTime);
        verifyNoMoreInteractions(memberRepository);
    }
}

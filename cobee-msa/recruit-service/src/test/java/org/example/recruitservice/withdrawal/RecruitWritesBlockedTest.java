package org.example.recruitservice.withdrawal;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.recruitservice.client.*;
import org.example.recruitservice.domain.*;
import org.example.recruitservice.dto.MemberCoreResponse;
import org.example.recruitservice.dto.comment.CommentRequest;
import org.example.recruitservice.repository.*;
import org.example.recruitservice.service.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
public class RecruitWritesBlockedTest {
    Map<Class<?>, Object> dependencies = new HashMap<>();
    <T> T make(Class<T> type) throws Exception {
        var constructor = type.getConstructors()[0];
        return type.cast(constructor
                .newInstance(Arrays.stream(constructor.getParameterTypes()).map(dependencies::get).toArray()));
    }
    @BeforeEach
    void fixture() throws Exception {
        for (Class<?> t : List.of(MemberWriteGuard.class, RecruitRepository.class, BookmarkRepository.class,
                CommentRepository.class, ApplyRepository.class, MemberClient.class, AlarmClient.class))
            dependencies.put(t, mock(t));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "MEMBER_WRITES_BLOCKED"))
                .when((MemberWriteGuard) dependencies.get(MemberWriteGuard.class)).requireWritable(any(Long[].class));
        var post = RecruitPost.builder().id(12L).ownerId(8L).title("fixture").build();
        when(((RecruitRepository) dependencies.get(RecruitRepository.class)).findById(12L))
                .thenReturn(Optional.of(post));
        when(((MemberClient) dependencies.get(MemberClient.class)).getMyInfo(7L)).thenReturn(
                ApiResponse.success("fixture", "200", MemberCoreResponse.builder().id(7L).name("fixture").build()));
        for (String name : List.of("CommentWriter", "ApplyWriter"))
            try {
                var writer = Class.forName("org.example.recruitservice.withdrawal." + name);
                dependencies.put(writer, make(writer));
            } catch (ClassNotFoundException beforeImplementation) {
            }
    }
    @Test
    void blockedMemberCannotBookmark() throws Exception {
        var service = make(BookmarkService.class);
        assertThrows(ResponseStatusException.class, () -> service.addBookmark(7L, 12L));
        verify((BookmarkRepository) dependencies.get(BookmarkRepository.class), never()).save(any());
    }
    @Test
    void blockedMemberCannotApply() throws Exception {
        var service = make(ApplyService.class);
        assertThrows(ResponseStatusException.class, () -> service.applyForRecruit(7L, 12L));
        verify((ApplyRepository) dependencies.get(ApplyRepository.class), never()).save(any());
    }
    @Test
    void blockedMemberCannotComment() throws Exception {
        var service = make(CommentService.class);
        var request = new ObjectMapper().readValue("{\"content\":\"fixture\",\"isPrivate\":false}",
                CommentRequest.class);
        assertThrows(ResponseStatusException.class, () -> service.createComment(12L, request, 7L));
        verify((CommentRepository) dependencies.get(CommentRepository.class), never()).save(any());
    }
}

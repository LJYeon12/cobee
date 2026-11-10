package org.example.recruitservice.withdrawal;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.recruitservice.client.AlarmClient;
import org.example.recruitservice.domain.RecruitPost;
import org.example.recruitservice.dto.comment.CommentRequest;
import org.example.recruitservice.repository.*;
import org.example.recruitservice.service.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@Tag("integration")
public class NotificationBoundaryIntegrationTest {
    @Test
    void externalNotificationStartsAfterWriterTransactionAndConnectionAreReleased() throws Exception {
        try (var f = new JpaWithdrawalIntegrationTest.Fixture()) {
            f.reset();
            f.sql.update("INSERT INTO member_write_state(member_id,state) VALUES(8,'ACTIVE')");
            var post = f.context.getBean(RecruitRepository.class)
                    .saveAndFlush(RecruitPost.builder().ownerId(8L).title("fixture").build());
            var alarms = mock(AlarmClient.class);
            var calls = new AtomicInteger();
            doAnswer(i -> {
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                assertFalse(TransactionSynchronizationManager.hasResource(f.ds));
                calls.incrementAndGet();
                return null;
            }).when(alarms).createAlarm(any());
            var beans = f.context.getBeanFactory();
            beans.registerSingleton("fixtureAlarms", alarms);
            beans.registerSingleton("commentWriter", beans.createBean(CommentWriter.class));
            beans.registerSingleton("applyWriter", beans.createBean(ApplyWriter.class));
            var comments = beans.createBean(CommentService.class);
            var applies = beans.createBean(ApplyService.class);
            var request = new ObjectMapper().readValue("{\"content\":\"fixture\",\"isPrivate\":false}",
                    CommentRequest.class);
            comments.createComment(post.getId(), request, 7L);
            applies.applyForRecruit(7L, post.getId());
            assertEquals(2, calls.get());
            doThrow(new IllegalStateException("synthetic push unavailable")).when(alarms).createAlarm(any());
            assertNotNull(comments.createComment(post.getId(), request, 7L));
            assertEquals(2, f.context.getBean(CommentRepository.class).count());
        }
    }
}

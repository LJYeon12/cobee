package org.example.recruitservice.withdrawal;
import java.util.UUID;
import org.example.recruitservice.domain.*;
import org.example.recruitservice.repository.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
@Tag("integration")
public class JpaCleanupIntegrationTest {
    @Test
    void cleanupRemovesOwnedAndAuthoredRowsUnderActualForeignKeys() throws Exception {
        try (var f = new JpaWithdrawalIntegrationTest.Fixture()) {
            f.reset();
            f.sql.update("INSERT INTO member_write_state(member_id,state) VALUES(8,'ACTIVE'),(9,'ACTIVE')");
            var posts = f.context.getBean(RecruitRepository.class);
            var comments = f.context.getBean(CommentRepository.class);
            var bookmarks = f.context.getBean(BookmarkRepository.class);
            var applies = f.context.getBean(ApplyRepository.class);
            var images = f.context.getBean(ImagesRepository.class);
            var mine = posts.saveAndFlush(RecruitPost.builder().ownerId(7L).title("mine").build());
            var other = posts.saveAndFlush(RecruitPost.builder().ownerId(8L).title("other").build());
            images.saveAndFlush(Images.builder().recruitPost(mine).imageUrl("fixture://image").build());
            var parent = comments
                    .saveAndFlush(Comment.builder().post(other).commentAuthorId(7L).content("mine").build());
            comments.saveAndFlush(
                    Comment.builder().post(other).parent(parent).commentAuthorId(8L).content("reply").build());
            comments.saveAndFlush(Comment.builder().post(mine).commentAuthorId(8L).content("on mine").build());
            bookmarks.saveAndFlush(Bookmark.builder().memberId(7L).recruitPost(other).build());
            bookmarks.saveAndFlush(Bookmark.builder().memberId(9L).recruitPost(other).build());
            applies.saveAndFlush(ApplyRecord.builder().appliedMemberId(7L).post(other).build());
            applies.saveAndFlush(ApplyRecord.builder().appliedMemberId(8L).post(mine).build());
            var lifecycle = f.context.getBean(RecruitMemberLifecycle.class);
            UUID id = UUID.randomUUID();
            lifecycle.block(7, id);
            lifecycle.cleanup(7, id);
            lifecycle.cleanup(7, id);
            assertEquals(1, posts.count());
            assertEquals(0, comments.count());
            assertEquals(1, bookmarks.count());
            assertEquals(0, applies.count());
            assertEquals(0, images.count());
        }
    }
}

package com.momo.backend.service;

import com.momo.backend.entity.Comment;
import com.momo.backend.entity.User;
import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;

class CommentReactionServiceTest {

    @Test
    void updatesExistingCommentReactionInsteadOfInsertingDuplicate() {
        CommentReactionRepository reactionRepository = mock(CommentReactionRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = mock(User.class);
        CommentReaction existing = mock(CommentReaction.class);

        when(user.getUserId()).thenReturn(9L);
        when(userRepository.findByLoginIdForUpdate("member")).thenReturn(Optional.of(user));
        when(commentRepository.findById(21L)).thenReturn(Optional.of(mock(Comment.class)));
        when(reactionRepository.findByUser_UserIdAndComment_CommentId(9L, 21L))
                .thenReturn(Optional.of(existing));

        CommentReactionService service = new CommentReactionService(
                reactionRepository, commentRepository, userRepository);

        service.saveReaction("member", 21L, true);

        verify(existing).updateReaction(true);
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void writesCommentReactionWithAtomicUpsert() {
        CommentReactionRepository reactionRepository = mock(CommentReactionRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = mock(User.class);

        when(user.getUserId()).thenReturn(9L);
        when(userRepository.findByLoginIdForUpdate("member")).thenReturn(Optional.of(user));
        when(commentRepository.findById(21L)).thenReturn(Optional.of(mock(Comment.class)));

        CommentReactionService service = new CommentReactionService(
                reactionRepository, commentRepository, userRepository);

        service.saveReaction("member", 21L, false);

        verify(reactionRepository).findByUser_UserIdAndComment_CommentId(9L, 21L);
        verify(reactionRepository).save(any(com.momo.backend.entity.CommentReaction.class));
    }
}

package karm.van.repo;

import karm.van.model.CommentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepo extends JpaRepository<CommentModel,Long> {
    List<CommentModel> getCommentModelByCardId(Long id);

    List<CommentModel> getAllByUserId(Long userId);

    Page<CommentModel> getCommentModelByCardIdAndParentCommentIsNull(Long id, Pageable pageable);

    @Query("SELECT c.parentComment.id FROM CommentModel c WHERE c.id = :commentId")
    Optional<Long> getParentCommentId(@Param("commentId") Long commentId);

    Page<CommentModel> getCommentModelsByParentComment_Id(Long parentCommentId, Pageable pageable);

    void deleteAllByCardId(Long id);

    Optional<CommentModel> getCommentModelById(Long id);
}

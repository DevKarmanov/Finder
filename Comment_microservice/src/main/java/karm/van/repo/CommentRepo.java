package karm.van.repo;

import karm.van.model.CommentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepo extends JpaRepository<CommentModel,Long> {
    List<CommentModel> getCommentModelByCardId(Long id);

    Page<CommentModel> getCommentModelByCardIdAndParentCommentIsNull(Long id, Pageable pageable);

    Page<CommentModel> getCommentModelsByParentComment_Id(Long parentCommentId, Pageable pageable);

    CommentModel findCommentModelById(Long id);

    void deleteAllByCardId(Long id);

    Optional<CommentModel> getCommentModelById(Long id);
}

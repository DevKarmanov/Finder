package karm.van.repo.jpaRepo;

import karm.van.model.CardModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface CardRepo extends JpaRepository<CardModel,Long> {
    Optional<CardModel> getCardModelById(Long id);

    List<CardModel> findAllByUserId(Long userId);

}

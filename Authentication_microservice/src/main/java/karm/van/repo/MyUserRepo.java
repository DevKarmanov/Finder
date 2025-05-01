package karm.van.repo;

import karm.van.model.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface MyUserRepo extends JpaRepository<MyUser, Long> {

    Optional<MyUser> findByName(String name);

    boolean existsByName(String name);
    boolean existsByEmail(String email);

    Optional<MyUser> findByEmail(String email);

    List<MyUser> findAllByFavoriteCardsContaining(Long cardId);
}

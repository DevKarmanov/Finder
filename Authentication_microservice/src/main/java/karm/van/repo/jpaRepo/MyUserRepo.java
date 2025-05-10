package karm.van.repo.jpaRepo;

import karm.van.model.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface MyUserRepo extends JpaRepository<MyUser, Long> {



    Optional<MyUser> findByName(String name);

    MyUser getByName(String name);

    boolean existsByName(String name);
    boolean existsByEmail(String email);

    Optional<MyUser> findByEmail(String email);

    List<MyUser> findAllByFavoriteCardsContaining(Long cardId);
}

package karm.van.repository;

import karm.van.AdsApp;
import karm.van.model.CardModel;
import karm.van.repo.jpaRepo.CardRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@Transactional
public class CardRepoTest {

    @Autowired
    private CardRepo cardRepo;

    @Test
    void checkSavePatchDeleteCardModel(){
        CardModel cardModel = CardModel.builder()
                .text("text")
                .title("title")
                .createTime(LocalDate.now())
                .imgIds(new ArrayList<>())
                .userId(1L)
                .build();

        cardRepo.save(cardModel);

        assertTrue(cardRepo.existsById(cardModel.getId()), "CardModel should be present in the repository after save");

        cardModel.setTitle("changedTitle");
        cardRepo.save(cardModel);

        CardModel updatedCard = cardRepo.findById(cardModel.getId()).orElseThrow(() -> new AssertionError("CardModel not found after update"));
        assertEquals("changedTitle", updatedCard.getTitle(), "Card title should be updated");


        cardRepo.delete(cardModel);
        assertFalse(cardRepo.existsById(cardModel.getId()), "CardModel should be deleted from the repository after delete");
    }
}

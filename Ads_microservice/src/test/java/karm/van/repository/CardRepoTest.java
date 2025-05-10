package karm.van.repository;

import karm.van.model.CardModel;
import karm.van.repo.jpaRepo.CardRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

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

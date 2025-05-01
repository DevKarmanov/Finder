package karm.van.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "Card")
public class CardModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String text;

    private LocalDate createTime;

    private Long userId;

    @ElementCollection
    @CollectionTable(name = "card_tags", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "tag")
    private List<String> tags;

    @ElementCollection
    @CollectionTable(name = "card_images", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "image_id")
    private List<Long> imgIds = new ArrayList<>();

}

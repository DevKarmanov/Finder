package karm.van.model;


import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "card_index")
public class CardDocument {

    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String title;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String text;

    @Field(type = FieldType.Keyword, normalizer = "lowercase_normalizer")
    private List<String> tags;

    @Field(type = FieldType.Date)
    private LocalDate createTime;
}

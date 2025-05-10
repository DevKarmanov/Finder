package karm.van.model;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "my_user_index")
public class MyUserDocument {
    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String name;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String country;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String description;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String firstName;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String lastName;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String skills;

    @Field(type = FieldType.Text, analyzer = "custom_russian_english")
    private String roleInCommand;
}

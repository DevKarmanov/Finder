package karm.van.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
public class AdminKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id = 1;

    @Getter
    @Setter
    private UUID adminKey;

}

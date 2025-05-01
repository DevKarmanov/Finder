package karm.van.repo;

import karm.van.model.AdminKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface KeyRepo extends JpaRepository<AdminKey, Integer> {
}

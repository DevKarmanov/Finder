package karm.van.config;

import karm.van.model.AdminKey;
import karm.van.repo.KeyRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminKeyInitializer implements ApplicationRunner {
    private final KeyRepo keyRepo;

    @Override
    public void run(ApplicationArguments args){
        if (keyRepo.count() == 0) {
            AdminKey adminKey = new AdminKey();
            adminKey.setAdminKey(UUID.randomUUID());
            keyRepo.save(adminKey);
            System.out.println("AdminKey created: " + adminKey.getAdminKey());
        }
    }
}

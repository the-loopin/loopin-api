package com.loopin.api.seed;


import com.loopin.api.auth.enums.Role;
import com.loopin.api.entity.User;
import com.loopin.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserSeeder {

    private final UserRepository userRepository;

    public List<User> seedUsers() {
        if (userRepository.count() > 0) {
            return userRepository.findAll();
        }

        List<User> users = new ArrayList<>();

        // 1 Admin
        User admin = new User("admin@loopin.com", "System Admin", "google-admin-123");
        admin.setRole(Role.ADMIN);
        users.add(admin);

        // 5 Realistic Regular Users
        String[][] mockUserData = {
            {"john.doe@gmail.com", "John Doe", "google-user-john-123"},
            {"jane.smith@gmail.com", "Jane Smith", "google-user-jane-123"},
            {"alice.johnson@gmail.com", "Alice Johnson", "google-user-alice-123"},
            {"bob.miller@gmail.com", "Bob Miller", "google-user-bob-123"},
            {"charlie.brown@gmail.com", "Charlie Brown", "google-user-charlie-123"}
        };

        for (String[] userData : mockUserData) {
            User user = new User(userData[0], userData[1], userData[2]);
            user.setRole(Role.USER);
            users.add(user);
        }

        return userRepository.saveAll(users);
    }

    /**
     * Returns the admin user from a list previously produced by seedUsers()
     * (or loaded via findAll()). Avoids relying on list ordering/index,
     * since findAll() order is not guaranteed to match insertion order.
     */
    public User getAdmin(List<User> users) {
        return users.stream()
            .filter(u -> u.getRole() == Role.ADMIN)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No admin user found among seeded users"));
    }

    /**
     * Returns only the regular (non-admin) users from a list previously
     * produced by seedUsers() (or loaded via findAll()).
     */
    public List<User> getRegularUsers(List<User> users) {
        return users.stream()
            .filter(u -> u.getRole() == Role.USER)
            .toList();
    }
}

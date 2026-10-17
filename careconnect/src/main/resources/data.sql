-- Seed verified caregivers and sample users if not already present
INSERT IGNORE INTO users (id, name, email, password_hash, role) VALUES 
(1, 'Admin CareConnect', 'admin@careconnect.com', '$2a$10$Y5n2bA52v6pD8hF9C1XkZeQ3wzP6zU.1R9mH8xT6G4rK3oE0u2y5G', 'ADMIN'),
(2, 'Aarohi Sharma', 'aarohi@careconnect.com', '$2a$10$Y5n2bA52v6pD8hF9C1XkZeQ3wzP6zU.1R9mH8xT6G4rK3oE0u2y5G', 'CAREGIVER'),
(3, 'Meera Patil', 'meera@careconnect.com', '$2a$10$Y5n2bA52v6pD8hF9C1XkZeQ3wzP6zU.1R9mH8xT6G4rK3oE0u2y5G', 'CAREGIVER'),
(4, 'Ananya Deshmukh', 'ananya@careconnect.com', '$2a$10$Y5n2bA52v6pD8hF9C1XkZeQ3wzP6zU.1R9mH8xT6G4rK3oE0u2y5G', 'CAREGIVER');

INSERT IGNORE INTO caregivers (id, user_id, skills, experience, hourly_rate, available, verified) VALUES
(1, 2, 'Infant care, CPR certified, Early childhood play', 4, 25.00, TRUE, TRUE),
(2, 3, 'Elder care, Mobility support, Medication reminders', 6, 30.00, TRUE, TRUE),
(3, 4, 'All-around family care, Meal prep, First Aid', 3, 22.00, TRUE, TRUE);

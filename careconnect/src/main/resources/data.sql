-- Seed verified caregivers and sample users if not already present
INSERT IGNORE INTO users (id, name, email, password_hash, role) VALUES 
(1, 'Admin CareConnect', 'admin@careconnect.com', '$2a$10$Y5n2bA52v6pD8hF9C1XkZeQ3wzP6zU.1R9mH8xT6G4rK3oE0u2y5G', 'ADMIN'),
(2, 'Aarohi Sharma', 'aarohi@careconnect.com', '$2a$10$Y5n2bA52v6pD8hF9C1XkZeQ3wzP6zU.1R9mH8xT6G4rK3oE0u2y5G', 'CAREGIVER'),
(3, 'Meera Patil', 'meera@careconnect.com', '$2a$10$Y5n2bA52v6pD8hF9C1XkZeQ3wzP6zU.1R9mH8xT6G4rK3oE0u2y5G', 'CAREGIVER'),
(4, 'Ananya Deshmukh', 'ananya@careconnect.com', '$2a$10$Y5n2bA52v6pD8hF9C1XkZeQ3wzP6zU.1R9mH8xT6G4rK3oE0u2y5G', 'CAREGIVER');

INSERT INTO caregivers (id, user_id, skills, experience, hourly_rate, available, verified) VALUES
(1, 2, 'Infant care, CPR certified, Early childhood play', 4, 100.00, TRUE, TRUE),
(2, 3, 'Elder care, Mobility support, Medication reminders', 6, 100.00, TRUE, TRUE),
(3, 4, 'All-around family care, Meal prep, First Aid', 3, 100.00, TRUE, TRUE)
ON DUPLICATE KEY UPDATE hourly_rate = VALUES(hourly_rate);

-- Set caregiver rate to 100 Rs per hour
UPDATE caregivers SET hourly_rate = 100.00;

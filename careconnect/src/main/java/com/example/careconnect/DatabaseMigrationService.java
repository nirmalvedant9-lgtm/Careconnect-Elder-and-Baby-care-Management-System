package com.example.careconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DatabaseMigrationService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationService.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public DatabaseMigrationService(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Checking and updating CareConnect database schema and seed data...");
            createTablesIfNotExist();
            upgradeColumns();
            seedDefaultUsersAndCaregivers();
            seedSampleDependentsAndBookings();
            log.info("CareConnect database initialization completed successfully.");
        } catch (Exception e) {
            log.error("Error during database migration and seeding: {}", e.getMessage(), e);
        }
    }

    private void createTablesIfNotExist() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL,
                phone VARCHAR(50),
                address VARCHAR(255),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS caregivers (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL UNIQUE,
                care_type VARCHAR(50) DEFAULT 'BOTH',
                skills TEXT,
                experience INT DEFAULT 0,
                hourly_rate DECIMAL(10,2) DEFAULT 0,
                bio TEXT,
                rating DECIMAL(3,2) DEFAULT 5.0,
                available BOOLEAN DEFAULT TRUE,
                verified BOOLEAN DEFAULT TRUE,
                phone VARCHAR(50),
                location VARCHAR(100) DEFAULT 'Metro Area',
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS dependents (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                name VARCHAR(255) NOT NULL,
                type VARCHAR(50) NOT NULL,
                birth_date DATE,
                gender VARCHAR(20) DEFAULT 'Not specified',
                emergency_contact VARCHAR(100),
                medical_conditions TEXT,
                allergies TEXT,
                dietary_needs TEXT,
                notes TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS bookings (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                caregiver_id BIGINT NOT NULL,
                dependent_id BIGINT NOT NULL,
                care_type VARCHAR(50) DEFAULT 'BABY',
                service_date DATETIME NOT NULL,
                hours INT DEFAULT 4,
                hourly_rate DECIMAL(10,2) DEFAULT 25.00,
                total_amount DECIMAL(10,2) DEFAULT 100.00,
                notes TEXT,
                status VARCHAR(50) DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (caregiver_id) REFERENCES caregivers(id) ON DELETE CASCADE,
                FOREIGN KEY (dependent_id) REFERENCES dependents(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS care_logs (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                booking_id BIGINT NOT NULL,
                log_type VARCHAR(50) DEFAULT 'GENERAL',
                note TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS reminders (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                dependent_id BIGINT NOT NULL,
                title VARCHAR(255) NOT NULL,
                category VARCHAR(50) DEFAULT 'MEDICATION',
                scheduled_at DATETIME NOT NULL,
                completed BOOLEAN DEFAULT FALSE,
                FOREIGN KEY (dependent_id) REFERENCES dependents(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS reviews (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                booking_id BIGINT NOT NULL UNIQUE,
                caregiver_id BIGINT,
                user_id BIGINT,
                rating INT NOT NULL,
                comment TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS payments (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                booking_id BIGINT NOT NULL UNIQUE,
                user_id BIGINT,
                amount DECIMAL(10,2) NOT NULL,
                status VARCHAR(50) DEFAULT 'UNPAID',
                payment_method VARCHAR(50) DEFAULT 'CARD',
                transaction_id VARCHAR(100),
                paid_at DATETIME,
                FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS vitals_logs (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                dependent_id BIGINT NOT NULL,
                metric_name VARCHAR(100) NOT NULL,
                metric_value VARCHAR(100) NOT NULL,
                notes TEXT,
                logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (dependent_id) REFERENCES dependents(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);
    }

    private void upgradeColumns() {
        addColumnIfNotExists("users", "phone", "VARCHAR(50)");
        addColumnIfNotExists("users", "address", "VARCHAR(255)");

        addColumnIfNotExists("caregivers", "care_type", "VARCHAR(50) DEFAULT 'BOTH'");
        addColumnIfNotExists("caregivers", "bio", "TEXT");
        addColumnIfNotExists("caregivers", "phone", "VARCHAR(50)");
        addColumnIfNotExists("caregivers", "rating", "DECIMAL(3,2) DEFAULT 5.0");
        addColumnIfNotExists("caregivers", "location", "VARCHAR(100) DEFAULT 'Metro Area'");

        addColumnIfNotExists("dependents", "gender", "VARCHAR(20) DEFAULT 'Not specified'");
        addColumnIfNotExists("dependents", "emergency_contact", "VARCHAR(100)");
        addColumnIfNotExists("dependents", "medical_conditions", "TEXT");
        addColumnIfNotExists("dependents", "allergies", "TEXT");
        addColumnIfNotExists("dependents", "dietary_needs", "TEXT");

        addColumnIfNotExists("bookings", "care_type", "VARCHAR(50) DEFAULT 'BABY'");
        addColumnIfNotExists("bookings", "hourly_rate", "DECIMAL(10,2) DEFAULT 25.00");
        addColumnIfNotExists("bookings", "total_amount", "DECIMAL(10,2) DEFAULT 100.00");
        addColumnIfNotExists("bookings", "notes", "TEXT");
        addColumnIfNotExists("bookings", "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        addColumnIfNotExists("care_logs", "log_type", "VARCHAR(50) DEFAULT 'GENERAL'");
        addColumnIfNotExists("care_logs", "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        addColumnIfNotExists("reminders", "category", "VARCHAR(50) DEFAULT 'MEDICATION'");

        addColumnIfNotExists("payments", "user_id", "BIGINT");
        addColumnIfNotExists("payments", "payment_method", "VARCHAR(50) DEFAULT 'CARD'");
        addColumnIfNotExists("payments", "transaction_id", "VARCHAR(100)");
        addColumnIfNotExists("payments", "paid_at", "DATETIME");

        addColumnIfNotExists("reviews", "caregiver_id", "BIGINT");
        addColumnIfNotExists("reviews", "user_id", "BIGINT");
        addColumnIfNotExists("reviews", "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
    }

    private boolean columnExists(String table, String column) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void addColumnIfNotExists(String table, String column, String definition) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column
            );
            if (count == null || count == 0) {
                jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                log.info("Added column {}.{}", table, column);
            }
        } catch (Exception e) {
            log.warn("Could not add column {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void seedDefaultUsersAndCaregivers() {
        // 1. Admin
        upsertUser("Admin CareConnect", "admin@careconnect.com", "admin1234", "ADMIN", "+1 (555) 010-0001", "Headquarters, Suite 100");

        // 2. Family User
        long familyUserId = upsertUser("Priya & Rahul Sharma", "family@careconnect.com", "family1234", "FAMILY", "+1 (555) 012-3456", "42 Sunshine Blvd, Greenfield");

        // 3. Baby Care Specialist
        long aarohiId = upsertUser("Aarohi Sharma", "aarohi@careconnect.com", "care1234", "CAREGIVER", "+1 (555) 019-8761", "Northside District");
        upsertCaregiver(aarohiId, "BABY",
            "Infant care, CPR certified, Early childhood play, Sensory development, Diaper hygiene, Bottle sterilization",
            5, 25.00,
            "Certified infant nanny with over 5 years of dedicated neonatal and toddler care. Passionate about gentle routines, milestone tracking, and creating warm, safe environments for infants.",
            4.9, true, true, "Northside");

        // 4. Elder Care Specialist
        long meeraId = upsertUser("Meera Patil", "meera@careconnect.com", "care1234", "CAREGIVER", "+1 (555) 019-8762", "Westend Garden");
        upsertCaregiver(meeraId, "ELDER",
            "Elder care, Mobility support, Medication reminders, Dementia & Alzheimer's gentle care, Vitals check",
            7, 30.00,
            "Compassionate geriatric care assistant experienced in mobility therapy, stroke recovery, and dignified companionship. Dedicated to keeping seniors engaged, active, and well-cared for.",
            5.0, true, true, "Westend");

        // 5. All-around Family Caregiver
        long ananyaId = upsertUser("Ananya Deshmukh", "ananya@careconnect.com", "care1234", "CAREGIVER", "+1 (555) 019-8763", "Central Park West");
        upsertCaregiver(ananyaId, "BOTH",
            "All-around family care, Meal prep, First Aid, Homework assistance, Senior walking companion",
            4, 22.00,
            "Versatile care provider who loves supporting multi-generational households with both playful infant companionship and respectful elderly assistance.",
            4.8, true, true, "Central Metro");

        // 6. Senior Nurse & Physical Therapist
        long rahulCgId = upsertUser("Rahul Verma", "rahul@careconnect.com", "care1234", "CAREGIVER", "+1 (555) 019-8764", "South Heights");
        upsertCaregiver(rahulCgId, "ELDER",
            "Physiotherapy exercises, Blood pressure & sugar monitoring, Post-op rehabilitation, Senior nutrition",
            8, 35.00,
            "Licensed senior physical therapist specializing in joint mobility exercises, fall prevention routines, and post-surgery rehabilitation care.",
            5.0, true, true, "South Metro");
    }

    private long upsertUser(String name, String email, String plainPassword, String role, String phone, String address) {
        String cleanEmail = email.trim().toLowerCase();
        var existing = jdbc.queryForList("SELECT id FROM users WHERE LOWER(email) = ?", cleanEmail);
        String hash = encoder.encode(plainPassword);
        if (!existing.isEmpty()) {
            long id = ((Number) existing.get(0).get("id")).longValue();
            jdbc.update("UPDATE users SET name = ?, password_hash = ?, role = ?, phone = ?, address = ? WHERE id = ?",
                name, hash, role, phone, address, id);
            return id;
        } else {
            jdbc.update("INSERT INTO users(name, email, password_hash, role, phone, address) VALUES(?, ?, ?, ?, ?, ?)",
                name, cleanEmail, hash, role, phone, address);
            return jdbc.queryForObject("SELECT id FROM users WHERE LOWER(email) = ?", Long.class, cleanEmail);
        }
    }

    private void upsertCaregiver(long userId, String careType, String skills, int exp, double rate, String bio, double rating, boolean available, boolean verified, String loc) {
        var existing = jdbc.queryForList("SELECT id FROM caregivers WHERE user_id = ?", userId);
        if (!existing.isEmpty()) {
            long id = ((Number) existing.get(0).get("id")).longValue();
            jdbc.update("""
                UPDATE caregivers SET care_type = ?, skills = ?, experience = ?, hourly_rate = ?, bio = ?, rating = ?, available = ?, verified = ?, location = ?
                WHERE id = ?
            """, careType, skills, exp, rate, bio, rating, available, verified, loc, id);
        } else {
            jdbc.update("""
                INSERT INTO caregivers(user_id, care_type, skills, experience, hourly_rate, bio, rating, available, verified, location)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, userId, careType, skills, exp, rate, bio, rating, available, verified, loc);
        }
    }

    private void seedSampleDependentsAndBookings() {
        // Find family user id
        var familyUsers = jdbc.queryForList("SELECT id FROM users WHERE email = 'family@careconnect.com'");
        if (familyUsers.isEmpty()) return;
        long familyId = ((Number) familyUsers.get(0).get("id")).longValue();

        // 1. Seed Grandpa Ramesh (Elder)
        long grandpaId;
        var existingGrandpa = jdbc.queryForList("SELECT id FROM dependents WHERE user_id = ? AND name = 'Grandpa Ramesh'", familyId);
        if (existingGrandpa.isEmpty()) {
            jdbc.update("""
                INSERT INTO dependents(user_id, name, type, birth_date, gender, emergency_contact, medical_conditions, allergies, dietary_needs, notes)
                VALUES(?, 'Grandpa Ramesh', 'ELDER', '1948-06-15', 'Male', '+1 (555) 012-3456 (Daughter Priya)',
                       'Hypertension, Mild Osteoarthritis', 'Penicillin', 'Low sodium, Diabetic-friendly',
                       'Enjoys gentle morning garden walks, reading the daily newspaper, and light herbal tea at 4 PM.')
            """, familyId);
            grandpaId = jdbc.queryForObject("SELECT id FROM dependents WHERE user_id = ? AND name = 'Grandpa Ramesh'", Long.class, familyId);
        } else {
            grandpaId = ((Number) existingGrandpa.get(0).get("id")).longValue();
        }

        // 2. Seed Baby Aarav (Baby)
        long babyId;
        var existingBaby = jdbc.queryForList("SELECT id FROM dependents WHERE user_id = ? AND name = 'Baby Aarav'", familyId);
        if (existingBaby.isEmpty()) {
            jdbc.update("""
                INSERT INTO dependents(user_id, name, type, birth_date, gender, emergency_contact, medical_conditions, allergies, dietary_needs, notes)
                VALUES(?, 'Baby Aarav', 'BABY', '2026-01-10', 'Male', '+1 (555) 012-3456 (Mom Priya)',
                       'Healthy 9-month infant', 'None known', 'Breastmilk/Formula + pureed fruits & oatmeal',
                       'Afternoon nap routine is 1:00 PM to 3:00 PM with soft lullabies. Loves peek-a-boo and sensory toys.')
            """, familyId);
            babyId = jdbc.queryForObject("SELECT id FROM dependents WHERE user_id = ? AND name = 'Baby Aarav'", Long.class, familyId);
        } else {
            babyId = ((Number) existingBaby.get(0).get("id")).longValue();
        }

        // 3. Seed Reminders
        var existingReminders = jdbc.queryForList("SELECT id FROM reminders WHERE dependent_id IN (?, ?)", grandpaId, babyId);
        if (existingReminders.isEmpty()) {
            jdbc.update("INSERT INTO reminders(dependent_id, title, category, scheduled_at, completed) VALUES(?, 'Morning Blood Pressure Medication (Amlodipine 5mg)', 'MEDICATION', ?, false)",
                grandpaId, LocalDateTime.now().withHour(8).withMinute(0).withSecond(0));
            jdbc.update("INSERT INTO reminders(dependent_id, title, category, scheduled_at, completed) VALUES(?, 'Gentle Assisted Mobility Walk & Stretch (15 mins)', 'EXERCISE', ?, false)",
                grandpaId, LocalDateTime.now().withHour(16).withMinute(30).withSecond(0));
            jdbc.update("INSERT INTO reminders(dependent_id, title, category, scheduled_at, completed) VALUES(?, 'Mid-day Infant Formula Feeding (180 ml) & Burping', 'FEEDING', ?, true)",
                babyId, LocalDateTime.now().withHour(12).withMinute(30).withSecond(0));
            jdbc.update("INSERT INTO reminders(dependent_id, title, category, scheduled_at, completed) VALUES(?, 'Afternoon Nap Time Routine (Soothing Lullaby)', 'ROUTINE', ?, false)",
                babyId, LocalDateTime.now().withHour(13).withMinute(0).withSecond(0));
        }

        // 4. Seed Vitals for Grandpa and Baby
        var existingVitals = jdbc.queryForList("SELECT id FROM vitals_logs WHERE dependent_id IN (?, ?)", grandpaId, babyId);
        if (existingVitals.isEmpty()) {
            jdbc.update("INSERT INTO vitals_logs(dependent_id, metric_name, metric_value, notes, logged_at) VALUES(?, 'Blood Pressure', '122/80 mmHg', 'Normal morning reading after medication', ?)",
                grandpaId, Timestamp.valueOf(LocalDateTime.now().minusDays(1).withHour(9).withMinute(0)));
            jdbc.update("INSERT INTO vitals_logs(dependent_id, metric_name, metric_value, notes, logged_at) VALUES(?, 'Fasting Blood Sugar', '104 mg/dL', 'Controlled reading within target', ?)",
                grandpaId, Timestamp.valueOf(LocalDateTime.now().minusDays(1).withHour(8).withMinute(30)));
            jdbc.update("INSERT INTO vitals_logs(dependent_id, metric_name, metric_value, notes, logged_at) VALUES(?, 'Pulse Rate', '72 bpm', 'Steady resting pulse', ?)",
                grandpaId, Timestamp.valueOf(LocalDateTime.now().minusHours(4)));

            jdbc.update("INSERT INTO vitals_logs(dependent_id, metric_name, metric_value, notes, logged_at) VALUES(?, 'Infant Weight', '8.9 kg', '9-month pediatric check milestone', ?)",
                babyId, Timestamp.valueOf(LocalDateTime.now().minusDays(2)));
            jdbc.update("INSERT INTO vitals_logs(dependent_id, metric_name, metric_value, notes, logged_at) VALUES(?, 'Temperature', '98.4 F', 'Normal healthy temperature', ?)",
                babyId, Timestamp.valueOf(LocalDateTime.now().minusHours(3)));
            jdbc.update("INSERT INTO vitals_logs(dependent_id, metric_name, metric_value, notes, logged_at) VALUES(?, 'Milk Intake', '180 ml', 'Finished full bottle happily', ?)",
                babyId, Timestamp.valueOf(LocalDateTime.now().minusHours(2)));
        }

        // 5. Seed Bookings & Care Logs
        var existingBookings = jdbc.queryForList("SELECT id FROM bookings WHERE user_id = ?", familyId);
        if (existingBookings.isEmpty()) {
            // Get caregiver IDs
            var aarohi = jdbc.queryForList("SELECT c.id FROM caregivers c JOIN users u ON u.id = c.user_id WHERE u.email = 'aarohi@careconnect.com'");
            var meera = jdbc.queryForList("SELECT c.id FROM caregivers c JOIN users u ON u.id = c.user_id WHERE u.email = 'meera@careconnect.com'");

            if (!aarohi.isEmpty() && !meera.isEmpty()) {
                long aarohiCgId = ((Number) aarohi.get(0).get("id")).longValue();
                long meeraCgId = ((Number) meera.get(0).get("id")).longValue();

                // Booking 1: Baby Care (Completed with review and payment)
                LocalDateTime date1 = LocalDateTime.now().minusDays(2).withHour(10).withMinute(0);
                jdbc.update("""
                    INSERT INTO bookings(user_id, caregiver_id, dependent_id, care_type, service_date, hours, hourly_rate, total_amount, notes, status)
                    VALUES(?, ?, ?, 'BABY', ?, 4, 25.00, 100.00, 'Morning supervision, formula feeding, and tummy time play session.', 'COMPLETED')
                """, familyId, aarohiCgId, babyId, date1);
                long booking1Id = jdbc.queryForObject("SELECT MAX(id) FROM bookings", Long.class);

                // Care Logs for Booking 1
                String careLogTimeCol = columnExists("care_logs", "created_at") ? "created_at" : "logged_at";
                jdbc.update("INSERT INTO care_logs(booking_id, log_type, note, " + careLogTimeCol + ") VALUES(?, 'FEEDING', 'Prepared and fed 160ml warm formula bottle. Baby burped well and was happy.', ?)",
                    booking1Id, Timestamp.valueOf(date1.plusHours(1)));
                jdbc.update("INSERT INTO care_logs(booking_id, log_type, note, " + careLogTimeCol + ") VALUES(?, 'ACTIVITY', 'Engaged in 20 mins sensory play and tummy time with rattle. Rolled over twice!', ?)",
                    booking1Id, Timestamp.valueOf(date1.plusHours(2)));
                jdbc.update("INSERT INTO care_logs(booking_id, log_type, note, " + careLogTimeCol + ") VALUES(?, 'SLEEP', 'Baby drifted off peacefully at 1:15 PM into afternoon nap.', ?)",
                    booking1Id, Timestamp.valueOf(date1.plusHours(3)));

                // Payment for Booking 1
                jdbc.update("""
                    INSERT INTO payments(booking_id, user_id, amount, status, payment_method, transaction_id, paid_at)
                    VALUES(?, ?, 100.00, 'PAID', 'CARD', 'TXN-CC-98231', ?)
                """, booking1Id, familyId, Timestamp.valueOf(date1.plusHours(4)));

                // Review for Booking 1
                jdbc.update("""
                    INSERT INTO reviews(booking_id, caregiver_id, user_id, rating, comment, created_at)
                    VALUES(?, ?, ?, 5, 'Aarohi was phenomenal! She took such tender care of baby Aarav and logged every feeding meticulously. We felt completely at ease.', ?)
                """, booking1Id, aarohiCgId, familyId, Timestamp.valueOf(date1.plusHours(5)));

                // Booking 2: Elder Care (Accepted / Active)
                LocalDateTime date2 = LocalDateTime.now().plusDays(1).withHour(9).withMinute(30);
                jdbc.update("""
                    INSERT INTO bookings(user_id, caregiver_id, dependent_id, care_type, service_date, hours, hourly_rate, total_amount, notes, status)
                    VALUES(?, ?, ?, 'ELDER', ?, 5, 30.00, 150.00, 'Assistance with morning routine, BP check, mobility walking, and doctor appointment escort.', 'ACCEPTED')
                """, familyId, meeraCgId, grandpaId, date2);
                long booking2Id = jdbc.queryForObject("SELECT MAX(id) FROM bookings", Long.class);

                // Payment pending for Booking 2
                jdbc.update("""
                    INSERT INTO payments(booking_id, user_id, amount, status, payment_method, transaction_id)
                    VALUES(?, ?, 150.00, 'UNPAID', 'CARD', 'INV-CC-40192')
                """, booking2Id, familyId);

                // Booking 3: Elder Care (Pending new request)
                LocalDateTime date3 = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0);
                jdbc.update("""
                    INSERT INTO bookings(user_id, caregiver_id, dependent_id, care_type, service_date, hours, hourly_rate, total_amount, notes, status)
                    VALUES(?, ?, ?, 'ELDER', ?, 3, 30.00, 90.00, 'Afternoon companionship, medication supervision, and memory puzzles.', 'PENDING')
                """, familyId, meeraCgId, grandpaId, date3);
                long booking3Id = jdbc.queryForObject("SELECT MAX(id) FROM bookings", Long.class);

                jdbc.update("""
                    INSERT INTO payments(booking_id, user_id, amount, status, payment_method, transaction_id)
                    VALUES(?, ?, 90.00, 'UNPAID', 'CARD', 'INV-CC-40193')
                """, booking3Id, familyId);
            }
        }
    }
}

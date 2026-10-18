package com.example.careconnect;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:3000"})
public class Api {

    private final NamedParameterJdbcTemplate db;
    private final PasswordEncoder encoder;
    private final SecretKey key;

    public Api(NamedParameterJdbcTemplate db, PasswordEncoder encoder, @Value("${app.jwt.secret}") String secret) {
        this.db = db;
        this.encoder = encoder;
        this.key = Keys.hmacShaKeyFor(Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 64));
    }

    public record User(long id, String role) {}

    private Map<String, Object> one(String sql, Map<String, ?> p) {
        var rows = db.queryForList(sql, p);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> rows(String sql, Map<String, ?> p) {
        return db.queryForList(sql, p);
    }

    private int run(String sql, Map<String, ?> p) {
        return db.update(sql, p);
    }

    private long inserted(String sql, Map<String, ?> p) {
        GeneratedKeyHolder k = new GeneratedKeyHolder();
        db.update(sql, new MapSqlParameterSource(p), k);
        return Objects.requireNonNull(k.getKey()).longValue();
    }

    private Map<String, Object> p(Object... a) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < a.length; i += 2) {
            m.put((String) a[i], a[i + 1]);
        }
        return m;
    }

    private ResponseStatusException error(HttpStatus status, String msg) {
        return new ResponseStatusException(status, msg);
    }

    private void check(boolean ok) {
        if (!ok) throw error(HttpStatus.FORBIDDEN, "Access denied");
    }

    public User tokenUser(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        long id = Long.parseLong(claims.getSubject());
        var u = one("SELECT id, role FROM users WHERE id = :id", p("id", id));
        if (u == null) throw new IllegalArgumentException("Unknown user");
        return new User(id, (String) u.get("role"));
    }

    private User me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) return u;
        throw error(HttpStatus.UNAUTHORIZED, "Please log in to continue");
    }

    private boolean admin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return "ADMIN".equalsIgnoreCase(u.role());
        }
        return false;
    }

    private boolean caregiver() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return "CAREGIVER".equalsIgnoreCase(u.role());
        }
        return false;
    }

    private long number(Map<String, Object> m, String k) {
        Object val = m.get(k);
        if (val == null) return 0;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(val).trim());
    }

    private double doubleNumber(Map<String, Object> m, String k) {
        Object val = m.get(k);
        if (val == null) return 0.0;
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(val).trim());
    }

    private String string(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : String.valueOf(v);
    }

    private void ownsDependent(long id) {
        check(one("SELECT id FROM dependents WHERE id = :id AND user_id = :u", p("id", id, "u", me().id())) != null || admin());
    }

    private Map<String, Object> booking(long id) {
        var b = one("""
            SELECT b.*, c.user_id caregiver_user_id 
            FROM bookings b 
            JOIN caregivers c ON b.caregiver_id = c.id 
            WHERE b.id = :id
        """, p("id", id));
        if (b == null) throw error(HttpStatus.NOT_FOUND, "Booking not found");
        check(admin() || number(b, "user_id") == me().id() || number(b, "caregiver_user_id") == me().id());
        return b;
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> v) {
        String role = string(v, "role").toUpperCase(Locale.ROOT);
        if ("USER".equals(role)) role = "FAMILY";
        if (!List.of("FAMILY", "CAREGIVER").contains(role)) {
            throw error(HttpStatus.BAD_REQUEST, "Invalid role. Must be FAMILY or CAREGIVER.");
        }
        String name = string(v, "name").trim();
        String email = string(v, "email").trim().toLowerCase(Locale.ROOT);
        String pass = string(v, "password");
        String phone = string(v, "phone");
        String address = string(v, "address");

        if (name.isBlank() || !email.contains("@") || pass.length() < 6) {
            throw error(HttpStatus.BAD_REQUEST, "Name, valid email and at least 6-character password required");
        }
        if (one("SELECT id FROM users WHERE email = :e", p("e", email)) != null) {
            throw error(HttpStatus.CONFLICT, "Email is already registered");
        }
        long id = inserted("""
            INSERT INTO users(name, email, password_hash, role, phone, address) 
            VALUES(:n, :e, :p, :r, :ph, :ad)
        """, p("n", name, "e", email, "p", encoder.encode(pass), "r", role, "ph", phone, "ad", address));

        if ("CAREGIVER".equals(role)) {
            String careType = string(v, "care_type");
            if (careType.isBlank()) careType = "BOTH";
            run("""
                INSERT INTO caregivers(user_id, care_type, skills, experience, hourly_rate, bio, rating, available, verified)
                VALUES(:id, :ct, 'General Care', 1, 20.00, 'New caregiver on CareConnect', 5.0, TRUE, FALSE)
            """, p("id", id, "ct", careType));
        }

        return p("message", "Registered successfully", "id", id);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> v) {
        String email = string(v, "email").trim().toLowerCase(Locale.ROOT);
        String pass = string(v, "password");
        var u = one("SELECT * FROM users WHERE email = :e", p("e", email));
        if (u == null || !encoder.matches(pass, (String) u.get("password_hash"))) {
            throw error(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        String token = Jwts.builder()
                .subject(String.valueOf(u.get("id")))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofHours(24))))
                .signWith(key)
                .compact();

        Map<String, Object> res = p(
                "token", token,
                "id", u.get("id"),
                "name", u.get("name"),
                "email", u.get("email"),
                "role", u.get("role"),
                "phone", u.get("phone"),
                "address", u.get("address")
        );

        if ("CAREGIVER".equals(u.get("role"))) {
            var cg = one("SELECT * FROM caregivers WHERE user_id = :uid", p("uid", u.get("id")));
            if (cg != null) {
                res.put("caregiverId", cg.get("id"));
                res.put("verified", cg.get("verified"));
                res.put("available", cg.get("available"));
                res.put("care_type", cg.get("care_type"));
            }
        }
        return res;
    }

    @GetMapping("/me")
    public Map<String, Object> profile() {
        var u = one("SELECT id, name, email, role, phone, address FROM users WHERE id = :id", p("id", me().id()));
        if (u == null) throw error(HttpStatus.NOT_FOUND, "User profile not found");
        if ("CAREGIVER".equalsIgnoreCase(me().role())) {
            var cg = one("SELECT * FROM caregivers WHERE user_id = :uid", p("uid", me().id()));
            if (cg != null) {
                Map<String, Object> enriched = new HashMap<>(u);
                enriched.put("caregiver", cg);
                return enriched;
            }
        }
        return u;
    }

    // ==========================================
    // CAREGIVERS DIRECTORY & PROFILES
    // ==========================================

    @GetMapping("/caregivers")
    public List<Map<String, Object>> caregivers(
            @RequestParam(required = false) String careType,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String q) {

        StringBuilder sql = new StringBuilder("""
            SELECT c.*, 
                   100.00 AS hourly_rate,
                   u.name, u.email, u.phone contact_phone
            FROM caregivers c 
            JOIN users u ON u.id = c.user_id 
            WHERE (c.verified = TRUE OR :admin = TRUE)
        """);

        Map<String, Object> params = p("admin", admin());

        if (careType != null && !careType.isBlank() && !"ALL".equalsIgnoreCase(careType)) {
            sql.append(" AND (c.care_type = :ct OR c.care_type = 'BOTH' OR :ct = 'BOTH')");
            params.put("ct", careType.toUpperCase(Locale.ROOT));
        }

        if (available != null) {
            sql.append(" AND c.available = :avail");
            params.put("avail", available);
        }

        if (q != null && !q.isBlank()) {
            sql.append(" AND (LOWER(u.name) LIKE :query OR LOWER(c.skills) LIKE :query OR LOWER(c.location) LIKE :query)");
            params.put("query", "%" + q.trim().toLowerCase(Locale.ROOT) + "%");
        }

        sql.append(" ORDER BY c.rating DESC, c.experience DESC, c.id DESC");
        return rows(sql.toString(), params);
    }

    @GetMapping("/caregivers/{id}")
    public Map<String, Object> caregiverDetails(@PathVariable long id) {
        var cg = one("""
            SELECT c.*, 
                   100.00 AS hourly_rate,
                   u.name, u.email, u.phone contact_phone, u.address
            FROM caregivers c 
            JOIN users u ON u.id = c.user_id 
            WHERE c.id = :id
        """, p("id", id));
        if (cg == null) throw error(HttpStatus.NOT_FOUND, "Caregiver not found");

        var reviews = rows("""
            SELECT r.*, u.name reviewer_name, b.service_date, b.care_type
            FROM reviews r
            JOIN bookings b ON b.id = r.booking_id
            JOIN users u ON u.id = b.user_id
            WHERE b.caregiver_id = :id
            ORDER BY r.id DESC
        """, p("id", id));

        Map<String, Object> res = new HashMap<>(cg);
        res.put("reviews", reviews);
        res.put("review_count", reviews.size());
        return res;
    }

    @PutMapping("/caregivers/me")
    public Map<String, Object> updateCaregiverProfile(@RequestBody Map<String, Object> v) {
        check(caregiver());
        String skills = string(v, "skills");
        String bio = string(v, "bio");
        String careType = string(v, "care_type");
        if (careType.isBlank()) careType = "BOTH";
        String location = string(v, "location");
        if (location.isBlank()) location = "Metro Area";
        String phone = string(v, "phone");

        long exp = number(v, "experience");
        double rate = doubleNumber(v, "hourly_rate");
        boolean avail = v.containsKey("available") ? Boolean.TRUE.equals(v.get("available")) : true;

        run("""
            UPDATE caregivers 
            SET skills = :s, experience = :e, hourly_rate = :h, bio = :bio, care_type = :ct, available = :a, location = :loc, phone = :ph
            WHERE user_id = :u
        """, p("s", skills, "e", exp, "h", rate, "bio", bio, "ct", careType, "a", avail, "loc", location, "ph", phone, "u", me().id()));

        if (v.containsKey("phone") || v.containsKey("address")) {
            run("UPDATE users SET phone = :ph WHERE id = :u", p("ph", phone, "u", me().id()));
        }

        return p("message", "Caregiver profile updated successfully");
    }

    // ==========================================
    // DEPENDENTS (BABY & ELDER FAMILY PROFILES)
    // ==========================================

    @GetMapping("/dependents")
    public List<Map<String, Object>> dependents() {
        return rows("""
            SELECT d.*,
                   (SELECT COUNT(*) FROM reminders r WHERE r.dependent_id = d.id AND r.completed = FALSE) pending_reminders,
                   (SELECT COUNT(*) FROM vitals_logs vl WHERE vl.dependent_id = d.id) vitals_count
            FROM dependents d 
            WHERE d.user_id = :u 
            ORDER BY d.id DESC
        """, p("u", me().id()));
    }

    @PostMapping("/dependents")
    public Map<String, Object> addDependent(@RequestBody Map<String, Object> v) {
        check(me().role().equals("FAMILY") || admin());
        String type = string(v, "type").toUpperCase(Locale.ROOT);
        if (!List.of("ELDER", "BABY").contains(type)) {
            throw error(HttpStatus.BAD_REQUEST, "Dependent type must be ELDER or BABY");
        }
        String name = string(v, "name").trim();
        if (name.isBlank()) throw error(HttpStatus.BAD_REQUEST, "Name is required");

        LocalDate bDate = null;
        if (v.containsKey("birth_date") && v.get("birth_date") != null && !string(v, "birth_date").isBlank()) {
            try {
                bDate = LocalDate.parse(string(v, "birth_date").substring(0, 10));
            } catch (Exception ignored) {}
        }

        long id = inserted("""
            INSERT INTO dependents(user_id, name, type, birth_date, gender, emergency_contact, medical_conditions, allergies, dietary_needs, notes)
            VALUES(:u, :n, :t, :b, :g, :ec, :mc, :al, :dn, :notes)
        """, p(
                "u", me().id(),
                "n", name,
                "t", type,
                "b", bDate,
                "g", v.getOrDefault("gender", "Not specified"),
                "ec", v.getOrDefault("emergency_contact", ""),
                "mc", v.getOrDefault("medical_conditions", ""),
                "al", v.getOrDefault("allergies", ""),
                "dn", v.getOrDefault("dietary_needs", ""),
                "notes", v.getOrDefault("notes", "")
        ));

        return p("id", id, "message", "Dependent profile created successfully");
    }

    @PutMapping("/dependents/{id}")
    public Map<String, Object> updateDependent(@PathVariable long id, @RequestBody Map<String, Object> v) {
        ownsDependent(id);
        LocalDate bDate = null;
        if (v.containsKey("birth_date") && v.get("birth_date") != null && !string(v, "birth_date").isBlank()) {
            try {
                bDate = LocalDate.parse(string(v, "birth_date").substring(0, 10));
            } catch (Exception ignored) {}
        }

        run("""
            UPDATE dependents 
            SET name = :n, birth_date = :b, gender = :g, emergency_contact = :ec, 
                medical_conditions = :mc, allergies = :al, dietary_needs = :dn, notes = :notes
            WHERE id = :id
        """, p(
                "id", id,
                "n", string(v, "name"),
                "b", bDate,
                "g", v.getOrDefault("gender", "Not specified"),
                "ec", v.getOrDefault("emergency_contact", ""),
                "mc", v.getOrDefault("medical_conditions", ""),
                "al", v.getOrDefault("allergies", ""),
                "dn", v.getOrDefault("dietary_needs", ""),
                "notes", v.getOrDefault("notes", "")
        ));

        return p("message", "Dependent profile updated");
    }

    @DeleteMapping("/dependents/{id}")
    public Map<String, Object> deleteDependent(@PathVariable long id) {
        ownsDependent(id);
        run("DELETE FROM dependents WHERE id = :id", p("id", id));
        return p("message", "Dependent profile deleted");
    }

    // ==========================================
    // HEALTH VITALS & BABY ROUTINE TRACKING
    // ==========================================

    @GetMapping("/dependents/{id}/vitals")
    public List<Map<String, Object>> getVitals(@PathVariable long id) {
        ownsDependent(id);
        return rows("SELECT * FROM vitals_logs WHERE dependent_id = :id ORDER BY logged_at DESC LIMIT 50", p("id", id));
    }

    @PostMapping("/dependents/{id}/vitals")
    public Map<String, Object> addVital(@PathVariable long id, @RequestBody Map<String, Object> v) {
        ownsDependent(id);
        String name = string(v, "metric_name");
        String val = string(v, "metric_value");
        if (name.isBlank() || val.isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "Metric name and value are required");
        }
        long vitalId = inserted("""
            INSERT INTO vitals_logs(dependent_id, metric_name, metric_value, notes, logged_at)
            VALUES(:id, :m, :v, :notes, CURRENT_TIMESTAMP)
        """, p("id", id, "m", name, "v", val, "notes", v.getOrDefault("notes", "")));

        return p("id", vitalId, "message", "Vital sign recorded");
    }

    // ==========================================
    // BOOKINGS MANAGEMENT
    // ==========================================

    @GetMapping("/bookings")
    public List<Map<String, Object>> bookings() {
        String where = admin() ? "1=1" : caregiver() ? "c.user_id = :u" : "b.user_id = :u";
        return rows("""
            SELECT b.*, 
                   cu.name caregiver_name, cu.email caregiver_email, cu.phone caregiver_phone,
                   fu.name family_name, fu.email family_email, fu.phone family_phone,
                   d.name dependent_name, d.type dependent_type, d.medical_conditions, d.emergency_contact,
                   p.status payment_status, p.amount payment_amount, p.payment_method payment_method, p.id payment_id,
                   r.rating review_rating, r.comment review_comment,
                   (SELECT COUNT(*) FROM care_logs cl WHERE cl.booking_id = b.id) logs_count
            FROM bookings b 
            JOIN caregivers c ON b.caregiver_id = c.id 
            JOIN users cu ON cu.id = c.user_id 
            JOIN users fu ON fu.id = b.user_id
            JOIN dependents d ON b.dependent_id = d.id 
            LEFT JOIN payments p ON p.booking_id = b.id
            LEFT JOIN reviews r ON r.booking_id = b.id
            WHERE """ + where + """
            ORDER BY b.id DESC
        """, p("u", me().id()));
    }

    @PostMapping("/bookings")
    public Map<String, Object> addBooking(@RequestBody Map<String, Object> v) {
        check(me().role().equals("FAMILY") || admin());

        // 1. Resolve Caregiver
        Long cg = null;
        if (v.containsKey("caregiver_id") && v.get("caregiver_id") != null && !string(v, "caregiver_id").isBlank()) {
            cg = number(v, "caregiver_id");
        } else if (v.containsKey("caregiverId") && v.get("caregiverId") != null && !string(v, "caregiverId").isBlank()) {
            cg = number(v, "caregiverId");
        }

        String careType = v.containsKey("careType") ? string(v, "careType") : string(v, "care_type");
        if (careType.isBlank()) careType = "BABY";
        careType = careType.toUpperCase(Locale.ROOT).contains("ELDER") ? "ELDER" : "BABY";

        if (cg == null || cg <= 0) {
            var matchingCg = one("""
                SELECT id FROM caregivers 
                WHERE verified = TRUE AND available = TRUE AND (care_type = :ct OR care_type = 'BOTH') 
                ORDER BY rating DESC LIMIT 1
            """, p("ct", careType));
            if (matchingCg == null) {
                matchingCg = one("SELECT id FROM caregivers WHERE verified = TRUE LIMIT 1", p());
            }
            if (matchingCg == null) {
                matchingCg = one("SELECT id FROM caregivers LIMIT 1", p());
            }
            if (matchingCg == null) throw error(HttpStatus.BAD_REQUEST, "No caregivers available in the system");
            cg = number(matchingCg, "id");
        }

        // 2. Resolve Dependent
        Long dependent = null;
        if (v.containsKey("dependent_id") && v.get("dependent_id") != null && !string(v, "dependent_id").isBlank()) {
            dependent = number(v, "dependent_id");
        } else if (v.containsKey("dependentId") && v.get("dependentId") != null && !string(v, "dependentId").isBlank()) {
            dependent = number(v, "dependentId");
        }

        if (dependent == null || dependent <= 0) {
            var userDeps = rows("SELECT id FROM dependents WHERE user_id = :u AND type = :t ORDER BY id ASC LIMIT 1",
                    p("u", me().id(), "t", careType));
            if (!userDeps.isEmpty()) {
                dependent = number(userDeps.get(0), "id");
            } else {
                var anyDep = rows("SELECT id FROM dependents WHERE user_id = :u ORDER BY id ASC LIMIT 1", p("u", me().id()));
                if (!anyDep.isEmpty()) {
                    dependent = number(anyDep.get(0), "id");
                } else {
                    String defaultName = "BABY".equals(careType) ? "Baby Loved One" : "Elder Loved One";
                    dependent = inserted("""
                        INSERT INTO dependents(user_id, name, type, notes) 
                        VALUES(:u, :n, :t, :notes)
                    """, p("u", me().id(), "n", defaultName, "t", careType, "notes", v.getOrDefault("notes", "Auto-created profile")));
                }
            }
        } else {
            ownsDependent(dependent);
        }

        // 3. Resolve Service Date
        LocalDateTime sDate;
        String dateStr = v.containsKey("service_date") && v.get("service_date") != null ? string(v, "service_date") :
                (v.containsKey("bookingDate") && v.get("bookingDate") != null ? string(v, "bookingDate") : null);

        if (dateStr == null || dateStr.isBlank()) {
            sDate = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0);
        } else {
            try {
                if (dateStr.length() == 10) {
                    sDate = LocalDate.parse(dateStr).atTime(9, 0);
                } else {
                    sDate = LocalDateTime.parse(dateStr.replace("Z", "").substring(0, 19));
                }
            } catch (Exception e) {
                sDate = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0);
            }
        }

        // 4. Resolve Hours & Calculate Total Amount
        long hours = 4;
        if (v.containsKey("hours") && v.get("hours") != null && !string(v, "hours").isBlank()) {
            hours = Math.max(1, number(v, "hours"));
        }

        var cgRecord = one("SELECT hourly_rate FROM caregivers WHERE id = :id", p("id", cg));
        double rate = cgRecord != null && doubleNumber(cgRecord, "hourly_rate") > 0 ? doubleNumber(cgRecord, "hourly_rate") : 100.0;
        if (rate <= 0) rate = 100.0;
        double total = hours * rate;

        long id = inserted("""
            INSERT INTO bookings(user_id, caregiver_id, dependent_id, care_type, service_date, hours, hourly_rate, total_amount, notes, status)
            VALUES(:u, :c, :d, :ct, :date, :h, :rate, :total, :notes, 'PENDING')
        """, p(
                "u", me().id(),
                "c", cg,
                "d", dependent,
                "ct", careType,
                "date", sDate,
                "h", hours,
                "rate", rate,
                "total", total,
                "notes", v.getOrDefault("notes", "")
        ));

        // Create initial pending invoice
        String payMethod = string(v, "paymentMethod");
        if (payMethod.isBlank()) payMethod = string(v, "payment_method");
        if (payMethod.isBlank()) payMethod = "CARD";

        run("""
            INSERT INTO payments(booking_id, user_id, amount, status, payment_method, transaction_id)
            VALUES(:bid, :uid, :amount, 'UNPAID', :pm, :tx)
        """, p("bid", id, "uid", me().id(), "amount", total, "pm", payMethod.toUpperCase(Locale.ROOT), "tx", "INV-CC-" + (10000 + id)));

        return p(
                "id", id,
                "totalAmount", total,
                "hourlyRate", rate,
                "hours", hours,
                "paymentMethod", payMethod.toUpperCase(Locale.ROOT),
                "paymentStatus", "UNPAID",
                "message", "Care request submitted successfully"
        );
    }

    @PutMapping("/bookings/{id}/status")
    public Map<String, Object> updateBookingStatus(@PathVariable long id, @RequestBody Map<String, Object> v) {
        var b = booking(id);
        String s = string(v, "status").toUpperCase(Locale.ROOT);
        if (!List.of("ACCEPTED", "REJECTED", "COMPLETED", "CANCELLED").contains(s)) {
            throw error(HttpStatus.BAD_REQUEST, "Invalid status: " + s);
        }

        if ("CANCELLED".equals(s)) {
            check(admin() || number(b, "user_id") == me().id());
        } else if ("COMPLETED".equals(s)) {
            check(admin() || number(b, "user_id") == me().id() || number(b, "caregiver_user_id") == me().id());
        } else {
            // ACCEPTED or REJECTED can only be actioned by caregiver or admin
            check(admin() || number(b, "caregiver_user_id") == me().id());
        }

        run("UPDATE bookings SET status = :s WHERE id = :id", p("s", s, "id", id));
        return p("message", "Booking status updated to " + s);
    }

    // ==========================================
    // DAILY CARE LOGS
    // ==========================================

    @GetMapping("/bookings/{id}/logs")
    public List<Map<String, Object>> logs(@PathVariable long id) {
        booking(id);
        return rows("SELECT * FROM care_logs WHERE booking_id = :id ORDER BY id DESC", p("id", id));
    }

    @PostMapping("/bookings/{id}/logs")
    public Map<String, Object> addLog(@PathVariable long id, @RequestBody Map<String, Object> v) {
        var b = booking(id);
        check(admin() || number(b, "caregiver_user_id") == me().id() || number(b, "user_id") == me().id());

        String note = string(v, "note").trim();
        if (note.isBlank()) throw error(HttpStatus.BAD_REQUEST, "Care log note cannot be empty");
        String type = string(v, "log_type");
        if (type.isBlank()) type = "GENERAL";

        long newId = inserted("""
            INSERT INTO care_logs(booking_id, log_type, note, created_at) 
            VALUES(:id, :t, :n, CURRENT_TIMESTAMP)
        """, p("id", id, "t", type.toUpperCase(Locale.ROOT), "n", note));

        return p("id", newId, "message", "Care log posted successfully");
    }

    // ==========================================
    // REMINDERS & SCHEDULE
    // ==========================================

    @GetMapping("/reminders")
    public List<Map<String, Object>> reminders() {
        return rows("""
            SELECT r.*, d.name dependent_name, d.type dependent_type
            FROM reminders r 
            JOIN dependents d ON d.id = r.dependent_id 
            WHERE d.user_id = :u 
            ORDER BY r.completed ASC, r.scheduled_at ASC
        """, p("u", me().id()));
    }

    @PostMapping("/reminders")
    public Map<String, Object> addReminder(@RequestBody Map<String, Object> v) {
        Long d = null;
        if (v.containsKey("dependent_id") && v.get("dependent_id") != null && !string(v, "dependent_id").isBlank()) {
            d = number(v, "dependent_id");
        } else if (v.containsKey("dependentId") && v.get("dependentId") != null && !string(v, "dependentId").isBlank()) {
            d = number(v, "dependentId");
        }

        if (d == null || d <= 0) {
            var userDeps = rows("SELECT id FROM dependents WHERE user_id = :u ORDER BY id ASC LIMIT 1", p("u", me().id()));
            if (!userDeps.isEmpty()) {
                d = number(userDeps.get(0), "id");
            } else {
                d = inserted("""
                    INSERT INTO dependents(user_id, name, type, notes) 
                    VALUES(:u, 'Family Care', 'ELDER', 'Auto-created for reminders')
                """, p("u", me().id()));
            }
        } else {
            ownsDependent(d);
        }

        LocalDateTime sDate = LocalDateTime.now().plusHours(2);
        if (v.containsKey("scheduled_at") && v.get("scheduled_at") != null && !string(v, "scheduled_at").isBlank()) {
            try {
                String s = string(v, "scheduled_at").replace("Z", "");
                if (s.length() == 10) {
                    sDate = LocalDate.parse(s).atTime(10, 0);
                } else {
                    sDate = LocalDateTime.parse(s.substring(0, 19));
                }
            } catch (Exception ignored) {}
        }

        String cat = string(v, "category");
        if (cat.isBlank()) cat = "MEDICATION";

        String title = string(v, "title").trim();
        if (title.isBlank()) throw error(HttpStatus.BAD_REQUEST, "Title is required");

        long id = inserted("""
            INSERT INTO reminders(dependent_id, title, category, scheduled_at, completed) 
            VALUES(:d, :t, :c, :s, FALSE)
        """, p("d", d, "t", title, "c", cat.toUpperCase(Locale.ROOT), "s", sDate));

        return p("id", id, "message", "Reminder created successfully");
    }

    @PutMapping("/reminders/{id}/complete")
    public Map<String, Object> completeReminder(@PathVariable long id) {
        int n = run("""
            UPDATE reminders r 
            JOIN dependents d ON d.id = r.dependent_id 
            SET r.completed = NOT r.completed 
            WHERE r.id = :id AND d.user_id = :u
        """, p("id", id, "u", me().id()));

        if (n == 0) throw error(HttpStatus.NOT_FOUND, "Reminder not found");
        return p("message", "Reminder updated");
    }

    @DeleteMapping("/reminders/{id}")
    public Map<String, Object> deleteReminder(@PathVariable long id) {
        int n = run("""
            DELETE r FROM reminders r 
            JOIN dependents d ON d.id = r.dependent_id 
            WHERE r.id = :id AND d.user_id = :u
        """, p("id", id, "u", me().id()));

        if (n == 0) throw error(HttpStatus.NOT_FOUND, "Reminder not found");
        return p("message", "Reminder deleted");
    }

    // ==========================================
    // REVIEWS & FEEDBACK
    // ==========================================

    @PostMapping("/bookings/{id}/review")
    public Map<String, Object> review(@PathVariable long id, @RequestBody Map<String, Object> v) {
        var b = booking(id);
        check(number(b, "user_id") == me().id());
        long rating = number(v, "rating");
        if (rating < 1 || rating > 5) throw error(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        long cgId = number(b, "caregiver_id");

        run("""
            INSERT INTO reviews(booking_id, caregiver_id, user_id, rating, comment, created_at) 
            VALUES(:id, :cg, :u, :r, :c, CURRENT_TIMESTAMP) 
            ON DUPLICATE KEY UPDATE rating = :r, comment = :c, created_at = CURRENT_TIMESTAMP
        """, p("id", id, "cg", cgId, "u", me().id(), "r", rating, "c", v.getOrDefault("comment", "")));

        // Recalculate average rating for caregiver
        var avg = one("SELECT AVG(rating) avg_rating FROM reviews WHERE caregiver_id = :cg", p("cg", cgId));
        if (avg != null && avg.get("avg_rating") != null) {
            double newAvg = Math.round(doubleNumber(avg, "avg_rating") * 10.0) / 10.0;
            run("UPDATE caregivers SET rating = :r WHERE id = :cg", p("r", newAvg, "cg", cgId));
        }

        return p("message", "Review submitted successfully");
    }

    @GetMapping("/caregivers/{id}/reviews")
    public List<Map<String, Object>> caregiverReviews(@PathVariable long id) {
        return rows("""
            SELECT r.*, u.name reviewer_name, b.service_date, b.care_type
            FROM reviews r
            JOIN bookings b ON b.id = r.booking_id
            JOIN users u ON u.id = b.user_id
            WHERE r.caregiver_id = :id
            ORDER BY r.id DESC
        """, p("id", id));
    }

    // ==========================================
    // PAYMENTS & INVOICES
    // ==========================================

    @GetMapping("/payments")
    public List<Map<String, Object>> payments() {
        String where = admin() ? "1=1" : "p.user_id = :u";
        return rows("""
            SELECT p.*, b.service_date, b.hours, b.care_type, b.status booking_status, b.hourly_rate, b.total_amount,
                   cu.name caregiver_name, fu.name family_name, d.name dependent_name
            FROM payments p 
            JOIN bookings b ON b.id = p.booking_id 
            JOIN caregivers c ON c.id = b.caregiver_id
            JOIN users cu ON cu.id = c.user_id
            JOIN users fu ON fu.id = b.user_id
            JOIN dependents d ON d.id = b.dependent_id
            WHERE """ + where + """
            ORDER BY p.id DESC
        """, p("u", me().id()));
    }

    @PutMapping("/payments/{id}/pay")
    public Map<String, Object> payInvoice(@PathVariable long id, @RequestBody Map<String, Object> v) {
        var pRecord = one("SELECT * FROM payments WHERE id = :id", p("id", id));
        if (pRecord == null) throw error(HttpStatus.NOT_FOUND, "Invoice not found");
        check(admin() || number(pRecord, "user_id") == me().id());

        String method = string(v, "payment_method");
        if (method.isBlank()) method = string(v, "paymentMethod");
        if (method.isBlank()) method = "CARD";

        String txnId = "TXN-CC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        run("""
            UPDATE payments 
            SET status = 'PAID', payment_method = :m, transaction_id = :tx, paid_at = CURRENT_TIMESTAMP
            WHERE id = :id
        """, p("id", id, "m", method.toUpperCase(Locale.ROOT), "tx", txnId));

        return p("message", "Payment processed successfully!", "transactionId", txnId, "status", "PAID");
    }

    // ==========================================
    // ADMIN CONSOLE
    // ==========================================

    @GetMapping("/admin/stats")
    public Map<String, Object> stats() {
        check(admin());
        return p(
                "users", one("SELECT COUNT(*) count FROM users", p()).get("count"),
                "caregivers", one("SELECT COUNT(*) count FROM caregivers", p()).get("count"),
                "bookings", one("SELECT COUNT(*) count FROM bookings", p()).get("count"),
                "dependents", one("SELECT COUNT(*) count FROM dependents", p()).get("count"),
                "pending_verification", one("SELECT COUNT(*) count FROM caregivers WHERE verified = FALSE", p()).get("count"),
                "total_revenue", one("SELECT COALESCE(SUM(amount), 0) sum FROM payments WHERE status = 'PAID'", p()).get("sum")
        );
    }

    @GetMapping("/admin/users")
    public List<Map<String, Object>> adminUsers() {
        check(admin());
        return rows("""
            SELECT u.id, u.name, u.email, u.role, u.phone, u.created_at,
                   (SELECT COUNT(*) FROM bookings b WHERE b.user_id = u.id) total_bookings
            FROM users u
            ORDER BY u.id DESC
        """, p());
    }

    @PutMapping("/admin/caregivers/{id}/verify")
    public Map<String, Object> verifyCaregiver(@PathVariable long id, @RequestBody Map<String, Object> v) {
        check(admin());
        boolean verified = Boolean.TRUE.equals(v.get("verified"));
        run("UPDATE caregivers SET verified = :v WHERE id = :id", p("id", id, "v", verified));
        return p("message", "Caregiver verification status updated to " + verified);
    }

    // ==========================================
    // EXCEPTION HANDLER
    // ==========================================

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason() == null ? "Request failed" : e.getReason()));
    }
}

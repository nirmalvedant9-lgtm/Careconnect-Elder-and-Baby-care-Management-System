# CareConnect - Elder Care & Baby Care Management System

A full-stack web application built for elder and baby care support, connecting families with verified caregivers.

---

## 🚀 Quick Start (One-Click)

Double-click the **`run-careconnect.bat`** file in the project root directory.

It will automatically:
1. Free port `8080` if another process was holding it.
2. Launch the Spring Boot backend on `http://localhost:8080`.
3. Launch the React + Vite frontend on `http://localhost:5173`.
4. Open the app in your browser at `http://localhost:5173`.

---

## 🛠️ Manual Start

### 1. Backend (Spring Boot)
Ensure MySQL is running on port `3306` with database `careconnect`.
```cmd
cd careconnect
mvnw.cmd spring-boot:run
```
*Port:* `8080`  
*Health Check:* `http://localhost:8080/careconnect`

### 2. Frontend (React + Vite)
```cmd
cd careconnect/careconnectfrontend
npm run dev
```
*URL:* `http://localhost:5173`

---

## 🔑 Default Accounts & Credentials

| Role | Email | Password |
| :--- | :--- | :--- |
| **Admin** | `admin@careconnect.com` | `admin1234` |
| **Caregiver** | `aarohi@careconnect.com` | `care1234` |
| **New Users** | You can register directly from the **/register** page | (minimum 6 characters) |

---

## 📁 Key Improvements Applied

1. **Spring Security & CORS**:
   - Registered `TokenFilter` before `UsernamePasswordAuthenticationFilter` so JWT Bearer tokens authenticate every request.
   - Configured `CorsConfigurationSource` to allow all methods (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`) from Vite dev servers (`http://localhost:5173`).
   - Permitted public access to `/api/auth/**`, `/api/caregivers`, `/error`, and preflight `OPTIONS` requests.
2. **Registration & Auth**:
   - Supported both `FAMILY` and `USER` role names seamlessly.
   - Synchronized minimum password length between frontend and backend.
3. **Database Schema & Seed Data**:
   - Added `schema.sql` and `data.sql` so all required tables and sample verified caregivers are automatically seeded on launch.
   - Aligned JPA `User` entity with MySQL database schema.
4. **Enhanced Frontend Pages**:
   - `Booking.jsx`: Added dynamic caregiver dropdown and auto-assigned fallback.
   - `Dashboard.jsx`: Live counts for active bookings, network caregivers, and reminders.
   - `Reminders.jsx`: Synced reminders with backend database API and completion toggle.
   - `AdminDashboard.jsx`: Enhanced metrics view with informative status feedback.

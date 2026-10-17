# CareConnect Frontend

React + Vite frontend for the CareConnect baby-care and elder-care mini-project.

## Requirements
- Node.js and npm installed
- Spring Boot backend (for database-backed login, registration, caregiver list, bookings and admin statistics)

## Run
Open a terminal in this folder:

```bash
npm install
npm run dev
```

Open the local URL printed by Vite (usually http://localhost:5173).

## Backend configuration
`.env` contains:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Change this only if your Spring Boot API uses a different host or base path. Restart Vite after editing `.env`.

Expected API endpoints:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/caregivers`
- `POST /api/bookings`
- `GET /api/admin/stats`

The login code expects a response with a token (or accessToken) and optionally a `user` object. Adjust `src/services/authService.js` and the page code if your Java DTOs use different field names.

## Important
- Caregiver cards fall back to sample profiles when the API is unavailable; these are not database records.
- Reminders are currently held in page state and are not saved to MySQL.
- This is a college project starter. Add backend validation, password hashing, authorization and role checks before using real personal data.

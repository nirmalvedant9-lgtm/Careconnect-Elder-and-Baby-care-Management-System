import { apiRequest } from './api'

// These paths should match your Spring Boot controller mappings.
export const authService = {
  login: (credentials) =>
    apiRequest('/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),
  register: (user) =>
    apiRequest('/auth/register', { method: 'POST', body: JSON.stringify(user) })
}

import { createContext, useContext, useMemo, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('careconnect_user')) } catch { return null }
  })

  function signIn(userData, token) {
    setUser(userData)
    localStorage.setItem('careconnect_user', JSON.stringify(userData))
    if (token) localStorage.setItem('careconnect_token', token)
  }

  function signOut() {
    setUser(null)
    localStorage.removeItem('careconnect_user')
    localStorage.removeItem('careconnect_token')
  }

  const value = useMemo(() => ({ user, signIn, signOut, isAuthenticated: Boolean(user) }), [user])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}

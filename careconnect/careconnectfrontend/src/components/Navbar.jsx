import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, signOut } = useAuth()
  return (
    <header className="navbar">
      <Link to="/" className="brand">
        <span className="brand-mark">♡</span>
        <span>Care<span className="brand-green">Connect</span><small>Care You Can Trust</small></span>
      </Link>
      <nav className="nav-links">
        <NavLink to="/">Home</NavLink>
        <NavLink to="/baby-care">Baby Care</NavLink>
        <NavLink to="/elder-care">Elder Care</NavLink>
        <NavLink to="/caregivers">Caregivers</NavLink>
      </nav>
      <div className="nav-actions">
        {user ? <>
          <Link className="btn btn-outline btn-small" to="/payments">💳 Payments</Link>
          <Link className="btn btn-outline btn-small" to="/dashboard">Dashboard</Link>
          <button className="btn btn-primary btn-small" onClick={signOut}>Log out</button>
        </> : <>
          <Link className="login-link" to="/login">Log in</Link>
          <Link className="btn btn-primary btn-small" to="/register">Get started</Link>
        </>}
      </div>
    </header>
  )
}

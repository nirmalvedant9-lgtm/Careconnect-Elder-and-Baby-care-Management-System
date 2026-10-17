import { Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import ProtectedRoute from './components/ProtectedRoute'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import ElderCare from './pages/ElderCare'
import BabyCare from './pages/BabyCare'
import Caregivers from './pages/Caregivers'
import Booking from './pages/Booking'
import Reminders from './pages/Reminders'
import AdminDashboard from './pages/AdminDashboard'

function Protected({ children }) {
  return <ProtectedRoute>{children}</ProtectedRoute>
}

export default function App() {
  return <div className="app-shell">
    <Navbar />
    <main className="main-content">
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/baby-care" element={<BabyCare />} />
        <Route path="/elder-care" element={<ElderCare />} />
        <Route path="/caregivers" element={<Caregivers />} />
        <Route path="/dashboard" element={<Protected><Dashboard /></Protected>} />
        <Route path="/booking" element={<Protected><Booking /></Protected>} />
        <Route path="/reminders" element={<Protected><Reminders /></Protected>} />
        <Route path="/admin" element={<Protected><AdminDashboard /></Protected>} />
        <Route path="*" element={<section className="section page-section"><h1>Page not found</h1><p className="muted">The page you're looking for doesn't exist.</p></section>} />
      </Routes>
    </main>
    <Footer />
  </div>
}

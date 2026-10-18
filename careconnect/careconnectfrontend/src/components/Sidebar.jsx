import { NavLink } from 'react-router-dom'

const links = [
  ['Dashboard', '/dashboard', '▦'],
  ['Caregivers', '/caregivers', '♡'],
  ['Book care', '/booking', '＋'],
  ['Payments', '/payments', '💳'],
  ['Reminders', '/reminders', '◷'],
  ['Admin', '/admin', '⚙']
]

export default function Sidebar() {
  return <aside className="sidebar">
    <p className="eyebrow">YOUR SPACE</p>
    {links.map(([label, to, icon]) => <NavLink key={to} to={to} className="side-link">
      <span>{icon}</span>{label}
    </NavLink>)}
  </aside>
}

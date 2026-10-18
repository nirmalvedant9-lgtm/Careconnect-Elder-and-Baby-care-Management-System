export default function CaregiverCard({ caregiver, onBook }) {
  const initials = (caregiver.name || 'Caregiver').split(' ').map(part => part[0]).slice(0, 2).join('').toUpperCase()
  return <article className="caregiver-card">
    <div className="caregiver-top">
      <div className="avatar">{initials}</div>
      <span className={`status ${caregiver.available === false ? 'status-busy' : ''}`}>
        {caregiver.available === false ? 'Unavailable' : 'Available'}
      </span>
    </div>
    <h3>{caregiver.name || 'Care professional'}</h3>
    <p className="muted">{caregiver.skills || caregiver.careType || caregiver.care_type || 'Family care specialist'}</p>
    <div className="caregiver-meta">
      <span>✦ {caregiver.experience ?? 2}+ years experience</span>
      <span className="rate-badge">💵 ₹{caregiver.hourly_rate ? Number(caregiver.hourly_rate).toFixed(2) : '100.00'} / hr</span>
      <span>✓ Verified profile</span>
    </div>
    <button className="btn btn-primary full-width" disabled={caregiver.available === false} onClick={() => onBook?.(caregiver)}>
      Book caregiver
    </button>
  </article>
}

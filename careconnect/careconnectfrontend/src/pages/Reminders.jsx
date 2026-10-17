import { useEffect, useState } from 'react'
import { apiRequest } from '../services/api'

export default function Reminders() {
  const [reminders, setReminders] = useState([])
  const [title, setTitle] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchReminders()
  }, [])

  function fetchReminders() {
    apiRequest('/reminders')
      .then(data => {
        if (Array.isArray(data) && data.length > 0) {
          setReminders(data.map(r => ({ id: r.id, title: r.title, done: Boolean(r.completed) })))
        } else {
          setReminders([
            { id: 1, title: 'Confirm care requirements', done: false },
            { id: 2, title: 'Prepare questions for caregiver', done: false }
          ])
        }
      })
      .catch(() => {
        setReminders([
          { id: 1, title: 'Confirm care requirements', done: false },
          { id: 2, title: 'Prepare questions for caregiver', done: false }
        ])
      })
  }

  async function addReminder(e) {
    e.preventDefault()
    if (!title.trim()) return
    const text = title.trim()
    setTitle('')
    try {
      setLoading(true)
      await apiRequest('/reminders', {
        method: 'POST',
        body: JSON.stringify({ title: text })
      })
      fetchReminders()
    } catch {
      setReminders(r => [...r, { id: Date.now(), title: text, done: false }])
    } finally {
      setLoading(false)
    }
  }

  async function toggle(id) {
    setReminders(items => items.map(item => item.id === id ? { ...item, done: !item.done } : item))
    try {
      await apiRequest(`/reminders/${id}/complete`, { method: 'PUT' })
    } catch {}
  }

  return <section className="section page-section narrow-page"><p className="eyebrow">STAY ORGANIZED</p><h1>Reminders</h1><p className="muted">Create and manage reminders for your family's care schedule.</p>
    <form className="inline-form" onSubmit={addReminder}><input value={title} onChange={e => setTitle(e.target.value)} placeholder="Add a reminder…" aria-label="Reminder title" disabled={loading} /><button className="btn btn-primary" disabled={loading}>{loading ? 'Adding…' : 'Add reminder'}</button></form>
    <div className="reminder-list">{reminders.map(item => <label key={item.id} className={`reminder-item ${item.done ? 'reminder-done' : ''}`}><input type="checkbox" checked={item.done} onChange={() => toggle(item.id)} /><span>{item.title}</span><small>{item.done ? 'Completed' : 'To do'}</small></label>)}</div>
  </section>
}

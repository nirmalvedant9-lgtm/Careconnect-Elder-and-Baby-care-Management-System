const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export async function apiRequest(path, options = {}) {
  const token = localStorage.getItem('careconnect_token')
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers
  }
     
  console.log("Calling:", `${API_BASE_URL}${path}`);
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  })

  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    const message = typeof data === 'string' ? data : data.message || data.error || 'Request failed'
    throw new Error(message)
  }
  return data
}

export { API_BASE_URL }

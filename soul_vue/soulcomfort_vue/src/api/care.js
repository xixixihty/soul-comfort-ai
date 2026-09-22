import request from './request'

export function fetchCarePending() {
  return request.get('/care/pending', { silent: true })
}

export function ackCare(careId, action) {
  return request.post(`/care/${careId}/ack`, { action })
}

export function fetchCareSettings() {
  return request.get('/care/settings', { silent: true })
}

export function updateCareSwitch(enabled) {
  return request.put('/care/switch', { enabled })
}

export function openCareOpener({ convId, refDate } = {}) {
  return request.post('/care/opener', { convId, refDate })
}

export function fetchMoodCalendar(year) {
  return request.get('/care/mood-calendar', { params: { year } })
}

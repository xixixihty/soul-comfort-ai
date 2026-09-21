import request from './request'

export function doCheckin(emotion, note) {
  return request.post('/checkin', { emotion, note })
}

export function fetchTodayCheckin() {
  return request.get('/checkin/today')
}

export function fetchCheckinList(page = 1, size = 20, keyword = '') {
  return request.get('/checkin/list', { params: { page, size, keyword } })
}

export function fetchCheckinCalendar(year, month) {
  return request.get('/checkin/calendar', { params: { year, month } })
}
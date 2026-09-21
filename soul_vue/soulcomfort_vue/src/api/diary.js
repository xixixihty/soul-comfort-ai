import request from './request'

export function fetchDiaryList(page = 1, size = 10) {
  return request.get('/diary/list', { params: { page, size } })
}

export function fetchDiary(id) {
  return request.get(`/diary/${id}`)
}

export function createDiary(data) {
  return request.post('/diary', data)
}

export function updateDiary(id, data) {
  return request.put(`/diary/${id}`, data)
}

export function deleteDiary(id) {
  return request.delete(`/diary/${id}`)
}
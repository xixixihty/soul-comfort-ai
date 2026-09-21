import request from './request'

export function fetchConversationList(page = 1, size = 20) {
  return request.get('/conversation/list', { params: { page, size } })
}

export function fetchConversationListByTag(tag, page = 1, size = 20) {
  return request.get('/conversation/listByTag', { params: { tag, page, size } })
}

export function fetchConversation(id) {
  return request.get(`/conversation/${id}`)
}

export function createConversation(title, tag) {
  return request.post('/conversation', { title: title || '新对话', tag })
}

export function renameConversation(id, title) {
  return request.put(`/conversation/${id}/title`, { title })
}

export function updateConversationTag(id, tag) {
  return request.put(`/conversation/${id}/tag`, { tag })
}

export function getUserTags() {
  return request.get('/conversation/tags')
}

export function deleteConversation(id) {
  return request.delete(`/conversation/${id}`)
}
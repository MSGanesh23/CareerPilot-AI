import apiClient from './client';

// ----------------------------------------------------------------
// Auth API
// ----------------------------------------------------------------
export const authApi = {
  login:    (data) => apiClient.post('/auth/login', data),
  register: (data) => apiClient.post('/auth/register', data),
};

// ----------------------------------------------------------------
// User API
// ----------------------------------------------------------------
export const userApi = {
  getProfile:    ()     => apiClient.get('/users/me'),
  updateProfile: (data) => apiClient.put('/users/me', data),
};

// ----------------------------------------------------------------
// Resume API
// ----------------------------------------------------------------
export const resumeApi = {
  list:      (page = 0, size = 10) => apiClient.get(`/resumes?page=${page}&size=${size}`),
  getActive: ()                     => apiClient.get('/resumes/active'),
  get:       (id)                   => apiClient.get(`/resumes/${id}`),
  upload:    (formData)             => apiClient.post('/resumes', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  update:    (id, data)             => apiClient.patch(`/resumes/${id}`, data),
  delete:    (id)                   => apiClient.delete(`/resumes/${id}`),
};

// ----------------------------------------------------------------
// Job Applications API
// ----------------------------------------------------------------
export const jobApi = {
  list:    (params = {}) => apiClient.get('/jobs', { params }),
  get:     (id)          => apiClient.get(`/jobs/${id}`),
  create:  (data)        => apiClient.post('/jobs', data),
  update:  (id, data)    => apiClient.put(`/jobs/${id}`, data),
  delete:  (id)          => apiClient.delete(`/jobs/${id}`),
  analyze: (id)          => apiClient.post(`/jobs/${id}/analyze`),
};

// ----------------------------------------------------------------
// Interview API
// ----------------------------------------------------------------
export const interviewApi = {
  list:         (params = {})        => apiClient.get('/interviews', { params }),
  get:          (sessionId)          => apiClient.get(`/interviews/${sessionId}`),
  startSession: (data)               => apiClient.post('/interviews', data),
  submitAnswer: (sessionId, questionId, data) =>
    apiClient.post(`/interviews/${sessionId}/questions/${questionId}/answer`, data),
  abandon:      (sessionId)          => apiClient.patch(`/interviews/${sessionId}/abandon`),
  delete:       (sessionId)          => apiClient.delete(`/interviews/${sessionId}`),
};

// ----------------------------------------------------------------
// Analytics API
// ----------------------------------------------------------------
export const analyticsApi = {
  getDashboard: () => apiClient.get('/analytics/dashboard'),
};

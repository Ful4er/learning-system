import axios from 'axios';

axios.defaults.baseURL = import.meta.env.VITE_API_BASE || '';

axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

export default {
  auth: {
    login: (email, password) => axios.post('/api/auth/login', { email, password }),
    register: (payload) => axios.post('/api/auth/register', payload),
    logout: () => axios.post('/api/auth/logout'),
    refresh: () => axios.post('/api/auth/refresh')
  },

  users: {
    me: () => axios.get('/api/users/me'),
    byId: (id) => axios.get(`/api/users/${id}`)
  },
  usersSearch: {
    byEmail: (email) => axios.get('/api/users/search', { params: { email } })
  },

  studentExams: {
    list: () => axios.get('/api/student/exams'),
    details: (examId) => axios.get(`/api/student/exams/${examId}`),
    questions: (examId) => axios.get(`/api/student/exams/${examId}/questions`),
    attempts: (examId) => axios.get(`/api/student/exams/${examId}/attempts`),
    startAttempt: (examId) => axios.post('/api/student/exams/attempts', { examId }),
    getCurrentAttempt: (examId) => axios.get(`/api/student/exams/${examId}/current-attempt`),
    submitAnswer: (attemptId, payload) => axios.post(`/api/student/exams/attempts/${attemptId}/answers`, payload),
    finishAttempt: (attemptId) => axios.post(`/api/student/exams/attempts/${attemptId}/finish`),
    getAttemptDetails: (attemptId) => axios.get(`/api/student/exams/attempts/${attemptId}`)
  },

  teacherExams: {
    list: () => axios.get('/api/teacher/exams'),
    create: (payload) => axios.post('/api/teacher/exams', payload),
    update: (examId, payload) => axios.put(`/api/teacher/exams/${examId}`, payload),
    delete: (examId) => axios.delete(`/api/teacher/exams/${examId}`),
    publish: (examId) => axios.post(`/api/teacher/exams/${examId}/publish`),
    archive: (examId) => axios.post(`/api/teacher/exams/${examId}/archive`),
    details: (examId) => axios.get(`/api/teacher/exams/${examId}`),
    assignments: (examId) => axios.get(`/api/teacher/exams/${examId}/assignments`),
    assign: (examId, studentEmails) => axios.post(`/api/teacher/exams/${examId}/assign`, { studentEmails }),
    removeAssignment: (examId, studentId) => axios.delete(`/api/teacher/exams/${examId}/assignments/${studentId}`),
    attempts: (examId) => axios.get(`/api/teacher/exams/${examId}/attempts`),
    attemptDetails: (examId, attemptId) => axios.get(`/api/teacher/exams/${examId}/attempts/${attemptId}`)
    ,
    // Questions
    addQuestion: (examId, payload) => axios.post(`/api/teacher/exams/${examId}/questions`, payload),
    getExamQuestions: (examId) => axios.get(`/api/teacher/exams/${examId}/questions`),
    updateQuestion: (questionId, payload) => axios.put(`/api/teacher/exams/questions/${questionId}`, payload),
    deleteQuestion: (questionId) => axios.delete(`/api/teacher/exams/questions/${questionId}`)
  },

  teacherStudents: {
    list: () => axios.get('/api/teacher/students'),
    details: (studentId) => axios.get(`/api/users/${studentId}`),
    results: (studentId) => axios.get(`/api/teacher/students/${studentId}/results`)
  },

  teacherProfile: {
    get: () => axios.get('/api/teacher/profile'),
    update: (payload) => axios.put('/api/teacher/profile', payload)
  },

  studentProfile: {
    get: () => axios.get('/api/student/profile'),
    update: (payload) => axios.put('/api/student/profile', payload)
  }
};


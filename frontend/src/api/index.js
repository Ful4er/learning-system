import axios from 'axios';

export default {
  auth: {
    login: (email, password) => axios.post('/api/auth/login', { email, password }),
    register: (payload) => axios.post('/api/auth/register', payload),
    logout: () => axios.post('/api/auth/logout')
  },

  users: {
    me: () => axios.get('/api/users/me'),
    byId: (id) => axios.get(`/api/users/${id}`)
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
    details: (examId) => axios.get(`/api/teacher/exams/${examId}`),
    students: (examId) => axios.get(`/api/teacher/exams/${examId}/students`),
    addStudent: (examId, studentEmail) => axios.post(`/api/teacher/exams/${examId}/students`, { email: studentEmail }),
    removeStudent: (examId, studentId) => axios.delete(`/api/teacher/exams/${examId}/students/${studentId}`),
    attempts: (examId) => axios.get(`/api/teacher/exams/${examId}/attempts`),
    attemptDetails: (examId, attemptId) => axios.get(`/api/teacher/exams/${examId}/attempts/${attemptId}`)
  },

  teacherStudents: {
    list: () => axios.get('/api/teacher/students'),
    details: (studentId) => axios.get(`/api/teacher/students/${studentId}`),
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

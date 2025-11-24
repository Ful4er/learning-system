import AuthView from './views/AuthView.vue';
import TeacherProfileView from './views/teacher/TeacherProfileView.vue';
import TeacherExamsView from './views/teacher/TeacherExamsView.vue';
import TeacherStudentsView from './views/teacher/TeacherStudentsView.vue';
import StudentProfileView from './views/student/StudentProfileView.vue';
import StudentExamsView from './views/student/StudentExamsView.vue';
import StudentExamAttemptView from './views/student/StudentExamAttemptView.vue';

export default [
  { path: '/', redirect: '/auth' },
  { path: '/auth', component: AuthView, meta: { guest: true } },
  { path: '/teacher/profile', component: TeacherProfileView, meta: { requiresAuth: true } },
  { path: '/teacher/exams', component: TeacherExamsView, meta: { requiresAuth: true } },
  { path: '/teacher/students', component: TeacherStudentsView, meta: { requiresAuth: true } },
  { path: '/student/profile', component: StudentProfileView, meta: { requiresAuth: true } },
  { path: '/student/exams', component: StudentExamsView, meta: { requiresAuth: true } },
  { path: '/student/exams/:examId/attempt/:attemptId', component: StudentExamAttemptView, meta: { requiresAuth: true } }
];
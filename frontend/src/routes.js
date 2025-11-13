import AuthView from './views/AuthView.vue';
import TeacherProfileView from './views/teacher/TeacherProfileView.vue';
import TeacherExamsView from './views/teacher/TeacherExamsView.vue';
import TeacherStudentsView from './views/teacher/TeacherStudentsView.vue';
import StudentProfileView from './views/student/StudentProfileView.vue';
import StudentExamsView from './views/student/StudentExamsView.vue';
import StudentExamAttemptView from './views/student/StudentExamAttemptView.vue';

export default [
  { path: '/', redirect: '/auth' },
  { path: '/auth', component: AuthView },
  { path: '/teacher/profile', component: TeacherProfileView },
  { path: '/teacher/exams', component: TeacherExamsView },
  { path: '/teacher/students', component: TeacherStudentsView }
  ,{ path: '/student/profile', component: StudentProfileView }
  ,{ path: '/student/exams', component: StudentExamsView }
  ,{ path: '/student/exams/:examId/attempt/:attemptId', component: StudentExamAttemptView }
];
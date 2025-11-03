import AuthView from './views/AuthView.vue';
import StudentProfileView from './views/student/StudentProfileView.vue';
import TeacherProfileView from './views/teacher/TeacherProfileView.vue';
import TeacherExamsView from './views/teacher/TeacherExamsView.vue';
import TeacherStudentsView from './views/teacher/TeacherStudentsView.vue';

export default [
  { path: '/', redirect: '/auth' },
  { path: '/auth', component: AuthView },
  { path: '/student/profile', component: StudentProfileView },
  { path: '/teacher/profile', component: TeacherProfileView },
  { path: '/teacher/exams', component: TeacherExamsView },
  { path: '/teacher/students', component: TeacherStudentsView }
];
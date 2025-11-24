<template>
  <div>
    <NavBar :links="links" />

    <main class="main-content">
      <div class="container">
        <div v-if="loading" class="empty-state">
          <div class="loading-spinner"></div>
          <p class="empty-state-message">Loading your profile...</p>
        </div>

        <div v-else class="dashboard-layout">
          <!-- Profile Card -->
          <section class="profile-card">
            <div class="profile-image">{{ userInitials }}</div>
            <h1 class="profile-name">{{ user.firstName }} {{ user.lastName }}</h1>
            <p class="profile-email">{{ user.email }}</p>
            
            <div class="stats-section">
              <div class="stats-grid">
                <div class="stat-item">
                  <p class="stat-number">{{ completedExams }}</p>
                  <p class="stat-label">Completed</p>
                </div>
                <div class="stat-item">
                  <p class="stat-number">{{ averageScore }}</p>
                  <p class="stat-label">Avg Score</p>
                </div>
              </div>
            </div>
          </section>

          <!-- Content Section -->
          <div class="content-section">
            <!-- Recent Exams -->
            <section class="section-card">
              <h2 class="card-title">Recent Exams</h2>
              
              <div v-if="recentExams.length === 0" class="empty-state">
                <div class="empty-state-message">No exams yet</div>
              </div>

              <div v-else class="exams-grid">
                <ExamCard v-for="exam in recentExams" :key="exam.id" :exam="exam">
                  <template #actions>
                    <router-link 
                      v-if="exam.currentAttemptId"
                      :to="`/student/exams/${exam.id}/attempt/${exam.currentAttemptId}`"
                      class="exam-btn exam-btn-primary"
                    >
                      Continue
                    </router-link>
                    <button 
                      v-else
                      @click="startExam(exam)"
                      class="exam-btn exam-btn-primary"
                      :disabled="startingAttemptFor === exam.id"
                    >
                      {{ startingAttemptFor === exam.id ? 'Starting...' : 'Start' }}
                    </button>
                  </template>
                </ExamCard>
              </div>
            </section>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../../api';
import NavBar from '../../components/NavBar.vue';
import ExamCard from '../../components/ExamCard.vue';

const links = [
  { to: '/student/profile', label: 'Dashboard' },
  { to: '/student/exams', label: 'Exams' }
];

const router = useRouter();

const user = ref({
  firstName: '',
  lastName: '',
  email: ''
});

const recentExams = ref([]);
const loading = ref(true);
const startingAttemptFor = ref(null);

const userInitials = computed(() => {
  if (user.value.firstName && user.value.lastName) {
    return (user.value.firstName[0] + user.value.lastName[0]).toUpperCase();
  }
  return '?';
});

const completedExams = computed(() => {
  return recentExams.value.filter(exam => exam.lastAttempt).length;
});

const averageScore = computed(() => {
  const examsWithScores = recentExams.value.filter(exam => exam.lastAttempt && exam.lastAttempt.score);
  if (examsWithScores.length === 0) return 0;
  
  const totalScore = examsWithScores.reduce((sum, exam) => sum + exam.lastAttempt.score, 0);
  return Math.round(totalScore / examsWithScores.length);
});

async function logout() {
  try {
    // Try to notify backend; ignore errors so client always logs out locally
    await api.auth.logout().catch(() => {});
  } catch (error) {
    console.error('Logout error (server):', error);
  } finally {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userRole');
    // Authorization header is set per-request by interceptor, so clearing token is sufficient
    router.push('/auth');
  }
}

async function loadUserProfile() {
  try {
    const response = await api.users.me();
    user.value = response.data;
  } catch (error) {
    console.error('Failed to load user profile:', error);
  }
}

async function loadExams() {
  try {
    const response = await api.studentExams.list();
    recentExams.value = response.data.slice(0, 4);
  } catch (error) {
    console.error('Failed to load exams:', error);
  }
}

async function startExam(exam) {
  startingAttemptFor.value = exam.id;
  try {
    const response = await api.studentExams.startAttempt(exam.id);
    exam.currentAttemptId = response.data.id;
    window.location.href = `/student/exams/${exam.id}/attempt/${response.data.id}`;
  } catch (error) {
    console.error('Failed to start exam:', error);
  } finally {
    startingAttemptFor.value = null;
  }
}

async function initialize() {
  loading.value = true;
  try {
    await Promise.all([loadUserProfile(), loadExams()]);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  initialize();
});
</script>

<style scoped>
@import '../../assets/css/teacher/profile.css';
</style>

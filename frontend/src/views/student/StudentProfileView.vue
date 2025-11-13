<template>
  <div>
    <nav class="nav-bar">
      <div class="container">
        <div class="nav-content">
          <ul class="nav-links">
            <li><router-link to="/student/profile" class="nav-link active">Dashboard</router-link></li>
            <li><router-link to="/student/exams" class="nav-link">Exams</router-link></li>
          </ul>
          <button class="notification-btn" @click="logout" aria-label="Logout" title="Logout">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></path>
              <polyline points="16 17 21 12 16 7" stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></polyline>
              <line x1="21" y1="12" x2="9" y2="12" stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></line>
            </svg>
          </button>
        </div>
      </div>
    </nav>

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
                <div v-for="exam in recentExams" :key="exam.id" class="exam-card">
                  <div class="exam-card-header">
                    <div class="exam-icon">
                      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                      </svg>
                    </div>
                    <div>
                      <h3 class="exam-card-title">{{ exam.title }}</h3>
                    </div>
                  </div>

                  <p v-if="exam.description" class="exam-card-description">{{ exam.description }}</p>

                  <div class="exam-meta">
                    <div class="exam-meta-item">
                      <span>⏱️ {{ exam.durationMinutes }} min</span>
                    </div>
                    <div class="exam-meta-item">
                      <span>✓ {{ exam.questionCount || 0 }} questions</span>
                    </div>
                  </div>

                  <div v-if="exam.lastAttempt" class="exam-meta" style="margin-top: 0;">
                    <div class="exam-meta-item">
                      <span>Score: {{ exam.lastAttempt.score }}/100</span>
                    </div>
                  </div>

                  <div class="exam-actions">
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
                  </div>
                </div>
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
import axios from 'axios';
import api from '../../api';

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
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userRole');
    delete axios.defaults.headers.common['Authorization'];
    router.push('/auth');
  } catch (error) {
    console.error('Logout error:', error);
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
@import '../../styles/student.css';
</style>

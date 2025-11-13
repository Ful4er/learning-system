<template>
  <div>
    <nav class="nav-bar">
      <div class="container">
        <div class="nav-content">
          <ul class="nav-links">
            <li><router-link to="/student/profile" class="nav-link">Dashboard</router-link></li>
            <li><router-link to="/student/exams" class="nav-link active">Exams</router-link></li>
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
          <p class="empty-state-message">Loading exams...</p>
        </div>

        <div v-else>
          <h2 class="card-title" style="margin-bottom: 24px;">Available Exams</h2>
          
          <div v-if="exams.length === 0" class="empty-state">
            <div class="empty-state-title">No exams assigned yet</div>
            <p class="empty-state-message">Ask your teacher to assign an exam to you</p>
          </div>

          <div v-else class="exams-grid">
            <div v-for="exam in exams" :key="exam.id" class="exam-card">
              <div class="exam-card-header">
                <div class="exam-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                    <line x1="16" y1="13" x2="8" y2="13"></line>
                    <line x1="16" y1="17" x2="8" y2="17"></line>
                    <polyline points="10 9 9 9 8 9"></polyline>
                  </svg>
                </div>
                <div style="flex: 1;">
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

              <div v-if="exam.passingScore" class="exam-meta" style="margin-top: 0;">
                <div class="exam-meta-item">
                  <span>Pass: {{ exam.passingScore }}/100</span>
                </div>
              </div>

              <div class="exam-actions">
                <button 
                  @click="startExam(exam)"
                  class="exam-btn exam-btn-primary"
                  :disabled="startingAttemptFor === exam.id"
                >
                  {{ startingAttemptFor === exam.id ? 'Starting...' : 'Start Exam' }}
                </button>
                <button 
                  @click="viewDetails(exam)"
                  class="exam-btn exam-btn-secondary"
                >
                  Details
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import api from '../../api';

const router = useRouter();
const exams = ref([]);
const loading = ref(true);
const startingAttemptFor = ref(null);

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

async function fetchExams() {
  loading.value = true;
  try {
    const res = await api.studentExams.list();
    exams.value = res.data || [];
  } catch (e) {
    console.error('Failed to load exams:', e);
    if (e.response?.status === 401) {
      router.push('/auth');
    }
  } finally {
    loading.value = false;
  }
}

function viewDetails(exam) {
  alert(`Exam: ${exam.title}\n\nDescription: ${exam.description || 'N/A'}\nDuration: ${exam.durationMinutes} min\nQuestions: ${exam.questionCount || 0}\nPassing Score: ${exam.passingScore || 0}/100`);
}

async function startExam(exam) {
  startingAttemptFor.value = exam.id;
  try {
    const res = await api.studentExams.startAttempt(exam.id);
    const attempt = res.data;
    if (attempt && attempt.id) {
      router.push(`/student/exams/${exam.id}/attempt/${attempt.id}`);
    } else {
      throw new Error('No attempt ID returned');
    }
  } catch (e) {
    console.error('Failed to start exam:', e);
    if (e.response?.status === 401) {
      router.push('/auth');
    } else {
      alert(`Failed to start exam: ${e.response?.data?.message || e.message}`);
    }
  } finally {
    startingAttemptFor.value = null;
  }
}

onMounted(fetchExams);
</script>

<style scoped>
@import '../../styles/student.css';
</style>

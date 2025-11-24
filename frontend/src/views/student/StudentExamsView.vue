<template>
  <div>
    <NavBar :links="links" />

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
            <ExamCard v-for="exam in exams" :key="exam.id" :exam="exam">
              <template #actions>
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
              </template>
            </ExamCard>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../../api';
import NavBar from '../../components/NavBar.vue';
import ExamCard from '../../components/ExamCard.vue';

const links = [
  { to: '/student/profile', label: 'Dashboard' },
  { to: '/student/exams', label: 'Exams' }
];

const router = useRouter();
const exams = ref([]);
const loading = ref(true);
const startingAttemptFor = ref(null);

async function logout() {
  try {
    await api.auth.logout().catch(() => {});
  } catch (error) {
    console.error('Logout error (server):', error);
  } finally {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userRole');
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
@import '../../assets/css/teacher/profile.css';
</style>

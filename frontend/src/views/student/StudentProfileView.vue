<template>
  <div class="student-dashboard">
    <NavBar :links="links" />

    <main class="main-content">
      <div class="container">
        <div v-if="loading" class="panel loading-panel">
          <div class="loading-spinner"></div>
          <p class="muted">Loading your space...</p>
        </div>

        <div v-else class="dashboard-layout">
          <section class="profile-card">
            <div class="avatar">{{ userInitials }}</div>
            <h1>{{ user.firstName }} {{ user.lastName }}</h1>
            <p class="muted">{{ user.email }}</p>

            <div class="profile-stats">
              <div>
                <span class="stat-value">{{ completedCount }}</span>
                <span class="stat-label">Completed</span>
              </div>
              <div>
                <span class="stat-value">{{ averageScore }}%</span>
                <span class="stat-label">Avg score</span>
              </div>
              <div>
                <span class="stat-value">{{ inProgressCount }}</span>
                <span class="stat-label">In progress</span>
              </div>
            </div>

            <router-link class="link-btn" to="/student/exams">Go to exams →</router-link>
          </section>

          <section class="content-column">
            <article class="section-card">
              <div class="section-header">
                <div>
                  <h2>Recent exams</h2>
                  <p class="muted small">Last four assessments you interacted with</p>
                </div>
                <router-link class="link-btn" to="/student/exams">View all</router-link>
              </div>

              <div v-if="recentExams.length === 0" class="empty-state">
                <p>No activity yet. Start with the exam list.</p>
              </div>
              <div v-else class="exams-grid">
                <div
                    v-for="exam in recentExams"
                    :key="exam.id"
                    class="recent-exam-card"
                >
                  <div class="recent-exam-header">
                    <div>
                      <p class="exam-title">{{ exam.title }}</p>
                      <p class="muted small">
                        {{ exam.questionCount || 0 }} questions · {{ exam.durationMinutes }} min
                      </p>
                    </div>
                    <span class="status-pill" :class="getExamState(exam.id).status">
                      {{ statusLabel(getExamState(exam.id).status) }}
                    </span>
                  </div>
                  <div class="recent-exam-meta">
                    <div>
                      <span class="label">Passing</span>
                      <span class="value">{{ exam.passingScore }}%</span>
                    </div>
                    <div v-if="getExamState(exam.id).lastScore != null">
                      <span class="label">Last score</span>
                      <span class="value">{{ formatScore(getExamState(exam.id).lastScore) }}</span>
                    </div>
                    <div>
                      <span class="label">Updated</span>
                      <span class="value">{{ formatDate(exam.updatedAt || exam.createdAt) }}</span>
                    </div>
                  </div>
                  <div class="card-actions">
                    <button
                        class="exam-btn exam-btn-primary"
                        :disabled="startingAttemptFor === exam.id"
                        @click="startExam(exam)"
                    >
                      <span v-if="startingAttemptFor === exam.id">Starting...</span>
                      <span v-else-if="getExamState(exam.id).activeAttemptId">Continue exam</span>
                      <span v-else>Start exam</span>
                    </button>
                    <router-link
                        class="exam-btn exam-btn-secondary"
                        :to="{ path: '/student/exams', query: { examId: exam.id } }"
                    >
                      View details
                    </router-link>
                  </div>
                </div>
              </div>
            </article>

            <article class="section-card">
              <div class="section-header">
                <h2>Active attempts</h2>
              </div>
              <div v-if="activeAttempts.length === 0" class="empty-state">
                <p>You don't have active attempts right now.</p>
              </div>
              <ul v-else class="attempt-list">
                <li v-for="attempt in activeAttempts" :key="attempt.id" class="attempt-item">
                  <div>
                    <p class="exam-title">{{ attempt.examTitle }}</p>
                    <p class="muted small">Started {{ formatDateWithTime(attempt.startedAt) }}</p>
                  </div>
                  <button class="link-btn" @click="resumeAttempt(attempt.examId, attempt.id)">
                    Resume →
                  </button>
                </li>
              </ul>
            </article>
          </section>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '../../api';
import NavBar from '../../components/NavBar.vue';

const links = [
  { to: '/student/profile', label: 'Dashboard' },
  { to: '/student/exams', label: 'Exams' }
];

const router = useRouter();

const user = ref({ firstName: '', lastName: '', email: '' });
const exams = ref([]);
const examProgress = ref({});
const loading = ref(true);
const startingAttemptFor = ref(null);

const recentExams = computed(() => {
  return [...exams.value]
      .sort((a, b) => new Date(getExamState(b.id).lastFinishedAt || b.updatedAt || b.createdAt) -
          new Date(getExamState(a.id).lastFinishedAt || a.updatedAt || a.createdAt))
      .slice(0, 4);
});

const activeAttempts = computed(() => {
  return Object.values(examProgress.value)
      .flatMap(state => (state.attempts || []).filter(a => a.status === 'IN_PROGRESS'));
});

const completedCount = computed(() =>
    Object.values(examProgress.value).filter(state => state.status === 'completed').length
);

const inProgressCount = computed(() =>
    Object.values(examProgress.value).filter(state => state.status === 'in-progress').length
);

const averageScore = computed(() => {
  const scores = Object.values(examProgress.value)
      .map(state => state.lastScore)
      .filter(score => score != null);
  if (!scores.length) return 0;
  return Math.round(scores.reduce((sum, value) => sum + value, 0) / scores.length);
});

const userInitials = computed(() => {
  if (user.value.firstName || user.value.lastName) {
    return `${user.value.firstName?.[0] || ''}${user.value.lastName?.[0] || ''}`.toUpperCase();
  }
  return '?';
});

const defaultState = () => ({
  status: 'available',
  activeAttemptId: null,
  lastScore: null,
  lastFinishedAt: null,
  attempts: []
});

function getExamState(examId) {
  return examProgress.value[examId] || defaultState();
}

function setExamState(examId, updates) {
  examProgress.value = {
    ...examProgress.value,
    [examId]: {
      ...defaultState(),
      ...(examProgress.value[examId] || {}),
      ...updates
    }
  };
}

function statusLabel(status) {
  switch (status) {
    case 'completed':
      return 'Completed';
    case 'in-progress':
      return 'In progress';
    default:
      return 'Ready';
  }
}

async function loadUserProfile() {
  try {
    const res = await api.users.me();
    user.value = res.data || user.value;
  } catch (error) {
    console.error('Failed to load user profile:', error);
  }
}

async function loadExams() {
  try {
    const res = await api.studentExams.list();
    exams.value = res.data || [];
    await Promise.all(exams.value.map(exam => hydrateExamState(exam.id)));
  } catch (error) {
    console.error('Failed to load exams:', error);
    if (error.response?.status === 401) {
      router.push('/auth');
    }
  }
}

async function hydrateExamState(examId) {
  try {
    const res = await api.studentExams.attempts(examId);
    const attempts = res.data || [];
    const activeAttempt = attempts.find(a => a.status === 'IN_PROGRESS');
    const finishedAttempts = attempts
        .filter(a => a.status === 'FINISHED')
        .sort((a, b) => new Date(b.finishedAt || b.startedAt) - new Date(a.finishedAt || a.startedAt));
    const lastFinished = finishedAttempts[0];

    setExamState(examId, {
      attempts,
      activeAttemptId: activeAttempt?.id || null,
      lastScore: lastFinished?.score ?? null,
      lastFinishedAt: lastFinished?.finishedAt || lastFinished?.startedAt || null,
      status: activeAttempt ? 'in-progress' : lastFinished ? 'completed' : 'available'
    });
  } catch (error) {
    console.error('Failed to hydrate exam state', examId, error);
  }
}

async function startExam(exam) {
  const state = getExamState(exam.id);
  if (state.activeAttemptId) {
    resumeAttempt(exam.id, state.activeAttemptId);
    return;
  }

  startingAttemptFor.value = exam.id;
  try {
    const res = await api.studentExams.startAttempt(exam.id);
    const attempt = res.data;
    if (!attempt?.id) throw new Error('No attempt ID returned');
    await hydrateExamState(exam.id);
    router.push(`/student/exams/${exam.id}/attempt/${attempt.id}`);
  } catch (error) {
    console.error('Failed to start exam:', error);
    alert(error.response?.data?.message || error.message);
  } finally {
    startingAttemptFor.value = null;
  }
}

function resumeAttempt(examId, attemptId) {
  router.push(`/student/exams/${examId}/attempt/${attemptId}`);
}

function formatScore(score) {
  return `${Math.round(score)}%`;
}

function formatDate(dateString) {
  if (!dateString) return '—';
  return new Date(dateString).toLocaleDateString();
}

function formatDateWithTime(dateString) {
  if (!dateString) return '—';
  return new Date(dateString).toLocaleString();
}

async function initialize() {
  loading.value = true;
  try {
    await Promise.all([loadUserProfile(), loadExams()]);
  } finally {
    loading.value = false;
  }
}

onMounted(initialize);
</script>

<style scoped>
.student-dashboard {
  min-height: 100vh;
  background: var(--light-blue);
}

.main-content {
  padding: 32px 0 48px;
}

.panel {
  background: white;
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.loading-panel .loading-spinner {
  margin-bottom: 12px;
}

.muted {
  color: var(--text-gray);
}

.muted.small {
  font-size: 13px;
}

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(280px, 320px) 1fr;
  gap: 24px;
}

.profile-card {
  background: white;
  border-radius: 20px;
  padding: 32px 28px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: var(--primary-blue);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
}

.profile-card h1 {
  margin: 0;
}

.profile-stats {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin: 16px 0;
}

.profile-stats div {
  text-align: center;
  flex: 1;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-dark);
}

.stat-label {
  font-size: 13px;
  color: var(--text-gray);
}

.link-btn {
  text-decoration: none;
  color: var(--primary-blue);
  font-weight: 600;
  border: none;
  background: none;
  cursor: pointer;
}

.content-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-card {
  background: white;
  border-radius: 20px;
  padding: 28px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
}

.empty-state {
  text-align: center;
  padding: 24px;
  border: 1px dashed var(--border-color);
  border-radius: 12px;
  background: #f8fafc;
}

.exams-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.recent-exam-card {
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recent-exam-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.exam-title {
  margin: 0;
  font-weight: 600;
  color: var(--text-dark);
}

.status-pill {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4338ca;
  height: fit-content;
}

.status-pill.completed {
  background: #ecfdf5;
  color: #15803d;
}

.status-pill.in-progress {
  background: #fff7ed;
  color: #c2410c;
}

.recent-exam-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.recent-exam-meta .label {
  font-size: 12px;
  color: var(--text-gray);
}

.recent-exam-meta .value {
  font-weight: 600;
  color: var(--text-dark);
}

.card-actions {
  display: flex;
  gap: 12px;
}

.exam-btn {
  flex: 1;
  text-align: center;
  text-decoration: none;
}

.attempt-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.attempt-item {
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 992px) {
  .dashboard-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .card-actions {
    flex-direction: column;
  }

  .recent-exam-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .attempt-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>

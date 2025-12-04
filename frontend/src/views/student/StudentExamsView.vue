<template>
  <div class="student-exams">
    <NavBar :links="links" />

    <main class="main-content">
      <div class="container">
        <section class="overview-card">
          <div>
            <p class="eyebrow">Assigned exams</p>
            <h1>Stay prepared for every check-in</h1>
            <p class="muted">Track your progress, continue active attempts and review your history in one place.</p>
          </div>
          <div class="stats-grid">
            <div class="stat">
              <span class="stat-value">{{ summaryStats.total }}</span>
              <span class="stat-label">Total</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ summaryStats.inProgress }}</span>
              <span class="stat-label">In progress</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ summaryStats.completed }}</span>
              <span class="stat-label">Completed</span>
            </div>
          </div>
        </section>

        <section class="controls">
          <input
              v-model="searchTerm"
              type="text"
              class="search-input"
              placeholder="Search by title or description"
          >
          <select v-model="statusFilter" class="filter-select">
            <option value="all">All</option>
            <option value="available">Ready to start</option>
            <option value="in-progress">In progress</option>
            <option value="completed">Completed</option>
          </select>
        </section>

        <div v-if="loading" class="panel loading-panel">
          <div class="loading-spinner"></div>
          <p class="muted">Loading exams...</p>
        </div>

        <div v-else-if="filteredExams.length === 0" class="panel empty-state">
          <div class="empty-state-icon">📚</div>
          <h3>No exams yet</h3>
          <p>You will see assigned assessments here once your teacher publishes them.</p>
        </div>

        <div v-else class="exams-grid">
          <div
              v-for="exam in filteredExams"
              :key="exam.id"
              class="exam-card-wrapper"
          >
            <div class="exam-status-row">
              <span class="status-pill" :class="getExamState(exam.id).status">
                {{ statusLabel(getExamState(exam.id).status) }}
              </span>
              <span class="score-pill" v-if="getExamState(exam.id).lastScore != null">
                Last score: {{ formatScore(getExamState(exam.id).lastScore) }}
              </span>
            </div>

            <ExamCard :exam="transformExam(exam)">
              <template #actions>
                <button
                    @click.stop="startExam(exam)"
                    class="exam-btn exam-btn-primary"
                    :disabled="startingAttemptFor === exam.id"
                >
                  <span v-if="startingAttemptFor === exam.id">Starting...</span>
                  <span v-else-if="getExamState(exam.id).activeAttemptId">Continue exam</span>
                  <span v-else>Start exam</span>
                </button>
                <button
                    @click.stop="viewDetails(exam)"
                    class="exam-btn exam-btn-secondary"
                >
                  Details
                </button>
              </template>
            </ExamCard>

            <div class="exam-info-footer">
              <div>
                <span class="label">Duration</span>
                <span class="value">{{ exam.durationMinutes }} min</span>
              </div>
              <div>
                <span class="label">Questions</span>
                <span class="value">{{ exam.questionCount || 0 }}</span>
              </div>
              <div>
                <span class="label">Passing score</span>
                <span class="value">{{ exam.passingScore }}%</span>
              </div>
              <div>
                <span class="label">Updated</span>
                <span class="value">{{ formatDate(exam.updatedAt || exam.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Exam details modal -->
    <div v-if="showDetailsModal" class="modal" @click.self="closeDetails">
      <div class="modal-content">
        <button class="close" @click="closeDetails" aria-label="Close">&times;</button>
        <div v-if="selectedExam" class="details-body">
          <h2>{{ selectedExam.title }}</h2>
          <p class="muted" style="margin-bottom: 16px;">{{ selectedExam.description || 'No description provided' }}</p>
          <div class="info-grid">
            <div>
              <span class="label">Duration</span>
              <span class="value">{{ selectedExam.durationMinutes }} min</span>
            </div>
            <div>
              <span class="label">Passing score</span>
              <span class="value">{{ selectedExam.passingScore }}%</span>
            </div>
            <div>
              <span class="label">Questions</span>
              <span class="value">{{ selectedExam.questionCount || 0 }}</span>
            </div>
            <div v-if="selectedExamTeacher">
              <span class="label">Teacher</span>
              <span class="value">
                {{ selectedExamTeacher.firstName }} {{ selectedExamTeacher.lastName }}
                <span v-if="selectedExamTeacher.email" class="muted small"> · {{ selectedExamTeacher.email }}</span>
              </span>
            </div>
          </div>

          <section class="history-section">
            <div class="history-header">
              <h3>Attempt history</h3>
              <button class="link-btn" @click="startExam(selectedExam)">
                {{ getExamState(selectedExam.id).activeAttemptId ? 'Continue attempt' : 'Start again' }}
              </button>
            </div>

            <div v-if="attemptsLoading" class="muted small">Loading attempts...</div>
            <div v-else-if="attemptsError" class="message error">{{ attemptsError }}</div>
            <div v-else-if="attemptsHistory.length === 0" class="muted small">No attempts yet.</div>
            <ul v-else class="attempt-list">
              <li v-for="attempt in attemptsHistory" :key="attempt.id" class="attempt-item">
                <div>
                  <p class="attempt-title">
                    {{ attempt.status === 'IN_PROGRESS' ? 'In progress' : 'Completed' }}
                  </p>
                  <p class="muted small">
                    Started {{ formatDateWithTime(attempt.startedAt) }}
                    <span v-if="attempt.finishedAt"> · Finished {{ formatDateWithTime(attempt.finishedAt) }}</span>
                  </p>
                </div>
                <div class="attempt-meta">
                  <span v-if="attempt.score != null" class="score-pill small">
                    {{ formatScore(attempt.score) }}
                  </span>
                  <button
                      v-if="attempt.status === 'IN_PROGRESS'"
                      class="link-btn"
                      @click="resumeAttempt(selectedExam.id, attempt.id)"
                  >
                    Resume
                  </button>
                </div>
              </li>
            </ul>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '../../api';
import NavBar from '../../components/NavBar.vue';
import ExamCard from '../../components/ExamCard.vue';

const links = [
  { to: '/student/profile', label: 'Dashboard' },
  { to: '/student/exams', label: 'Exams' }
];

const router = useRouter();
const route = useRoute();
const exams = ref([]);
const loading = ref(true);
const startingAttemptFor = ref(null);
const searchTerm = ref('');
const statusFilter = ref('all');
const examProgress = ref({});

const selectedExam = ref(null);
const showDetailsModal = ref(false);
const attemptsHistory = ref([]);
const attemptsLoading = ref(false);
const attemptsError = ref('');
const selectedExamTeacher = ref(null);

const defaultState = () => ({
  status: 'available',
  activeAttemptId: null,
  lastScore: null,
  lastFinishedAt: null,
  attempts: []
});

const filteredExams = computed(() => {
  const query = searchTerm.value.trim().toLowerCase();
  return exams.value.filter(exam => {
    const state = getExamState(exam.id);
    const matchesQuery = !query ||
        exam.title?.toLowerCase().includes(query) ||
        exam.description?.toLowerCase().includes(query);
    const matchesFilter =
        statusFilter.value === 'all' ||
        state.status === statusFilter.value;
    return matchesQuery && matchesFilter;
  });
});

const summaryStats = computed(() => {
  const total = exams.value.length;
  let completed = 0;
  let inProgress = 0;
  exams.value.forEach(exam => {
    const state = getExamState(exam.id);
    if (state.status === 'completed') completed++;
    if (state.status === 'in-progress') inProgress++;
  });
  return { total, completed, inProgress };
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

function transformExam(exam) {
  return {
    ...exam,
    teacherFirstName: 'Assigned',
    teacherLastName: '',
    teacherEmail: '',
    questionCount: exam.questionCount || 0
  };
}

function statusLabel(status) {
  switch (status) {
    case 'in-progress':
      return 'In progress';
    case 'completed':
      return 'Completed';
    default:
      return 'Ready';
  }
}

async function fetchExams() {
  loading.value = true;
  try {
    const res = await api.studentExams.list();
    exams.value = res.data || [];
    await Promise.all(exams.value.map(exam => hydrateExamState(exam.id)));
    // open details if we came here with examId in query (e.g. from profile dashboard)
    const initialExamId = route.query.examId ? Number(route.query.examId) : null;
    if (initialExamId && exams.value.some(e => e.id === initialExamId)) {
      const exam = exams.value.find(e => e.id === initialExamId);
      await viewDetails(exam);
    }
  } catch (error) {
    console.error('Failed to load exams:', error);
    if (error.response?.status === 401) {
      router.push('/auth');
    }
  } finally {
    loading.value = false;
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
    console.error('Failed to load attempts for exam', examId, error);
    setExamState(examId, { status: 'available' });
  }
}

async function startExam(exam) {
  const state = getExamState(exam.id);
  if (state.activeAttemptId) {
    router.push(`/student/exams/${exam.id}/attempt/${state.activeAttemptId}`);
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
    const message = error.response?.data?.message || error.message;
    alert(`Failed to start exam: ${message}`);
    if (error.response?.status === 401) {
      router.push('/auth');
    }
  } finally {
    startingAttemptFor.value = null;
  }
}

async function viewDetails(exam) {
  selectedExam.value = exam;
  selectedExamTeacher.value = null;
  showDetailsModal.value = true;

  await Promise.all([
    loadAttemptHistory(exam.id),
    loadTeacherInfo(exam.teacherId)
  ]);
}

async function loadAttemptHistory(examId) {
  attemptsLoading.value = true;
  attemptsError.value = '';
  try {
    if (!examProgress.value[examId]?.attempts || examProgress.value[examId].attempts.length === 0) {
      await hydrateExamState(examId);
    }
    const state = getExamState(examId);
    attemptsHistory.value = [...state.attempts].sort((a, b) =>
        new Date(b.startedAt) - new Date(a.startedAt)
    );
  } catch (error) {
    attemptsError.value = error.response?.data?.message || 'Failed to load attempts';
  } finally {
    attemptsLoading.value = false;
  }
}

function closeDetails() {
  showDetailsModal.value = false;
  selectedExam.value = null;
  selectedExamTeacher.value = null;
  attemptsHistory.value = [];
  attemptsError.value = '';
}

function resumeAttempt(examId, attemptId) {
  router.push(`/student/exams/${examId}/attempt/${attemptId}`);
}

function formatDate(dateString) {
  if (!dateString) return '—';
  return new Date(dateString).toLocaleDateString();
}

function formatDateWithTime(dateString) {
  if (!dateString) return 'Unknown';
  return new Date(dateString).toLocaleString();
}

function formatScore(value) {
  return `${Math.round(value)}%`;
}

async function loadTeacherInfo(teacherId) {
  if (!teacherId) return;
  try {
    const res = await api.users.byId(teacherId);
    selectedExamTeacher.value = res.data || null;
  } catch (error) {
    console.error('Failed to load teacher info', error);
    selectedExamTeacher.value = null;
  }
}

watch(
  () => route.query.examId,
  async (newId) => {
    if (!newId) return;
    const examId = Number(newId);
    const exam = exams.value.find(e => e.id === examId);
    if (exam) {
      await viewDetails(exam);
    }
  }
);

onMounted(fetchExams);
</script>

<style scoped>
.student-exams {
  min-height: 100vh;
  background: var(--light-blue);
}

.main-content {
  padding: 32px 0 48px;
}

.overview-card {
  background: white;
  border-radius: 16px;
  padding: 28px;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 24px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  margin-bottom: 24px;
}

.eyebrow {
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-gray);
  margin: 0 0 8px 0;
}

.overview-card h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  color: var(--text-dark);
}

.muted {
  color: var(--text-gray);
}

.stats-grid {
  display: flex;
  gap: 20px;
  align-items: center;
}

.stat {
  text-align: center;
  min-width: 110px;
}

.stat-value {
  display: block;
  font-size: 26px;
  font-weight: 700;
  color: var(--text-dark);
}

.stat-label {
  display: block;
  font-size: 13px;
  color: var(--text-gray);
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 24px;
}

.search-input {
  flex: 1;
  min-width: 220px;
  padding: 10px 14px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  font-size: 14px;
}

.filter-select {
  padding: 10px 14px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  font-size: 14px;
  background: white;
}

.panel {
  background: white;
  border-radius: 16px;
  padding: 40px 24px;
  text-align: center;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.loading-panel .loading-spinner {
  margin: 0 auto 12px auto;
}

.empty-state-icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.exams-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.exam-card-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px;
}

.status-pill {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4338ca;
}

.status-pill.in-progress {
  background: #fff7ed;
  color: #c2410c;
}

.status-pill.completed {
  background: #ecfdf5;
  color: #15803d;
}

.score-pill {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
}

.score-pill.small {
  font-size: 11px;
  padding: 3px 8px;
}

.exam-info-footer {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  background: white;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.08);
}

.exam-info-footer .label {
  display: block;
  font-size: 12px;
  color: var(--text-gray);
}

.exam-info-footer .value {
  font-weight: 600;
  color: var(--text-dark);
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: min(640px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  padding: 32px;
  position: relative;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.2);
}

.close {
  position: absolute;
  top: 16px;
  right: 20px;
  font-size: 28px;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--text-gray);
}

.details-body h2 {
  margin: 0 0 8px 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
  margin: 20px 0;
}

.info-grid .label {
  display: block;
  font-size: 12px;
  color: var(--text-gray);
}

.info-grid .value {
  display: block;
  font-weight: 600;
  color: var(--text-dark);
}

.history-section {
  margin-top: 24px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-header h3 {
  margin: 0;
}

.link-btn {
  border: none;
  background: none;
  color: var(--primary-blue);
  cursor: pointer;
  font-weight: 600;
}

.attempt-list {
  list-style: none;
  padding: 0;
  margin: 16px 0 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.attempt-item {
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.attempt-title {
  margin: 0 0 4px 0;
  font-weight: 600;
  color: var(--text-dark);
}

.muted.small {
  font-size: 13px;
}

.exam-btn {
  min-width: 110px;
}

@media (max-width: 992px) {
  .exam-info-footer {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .overview-card h1 {
    font-size: 24px;
  }

  .stats-grid {
    width: 100%;
    justify-content: space-between;
  }

  .exam-info-footer {
    grid-template-columns: 1fr 1fr;
  }

  .controls {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .exam-info-footer {
    grid-template-columns: 1fr;
  }

  .attempt-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

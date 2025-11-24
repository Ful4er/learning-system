<template>
  <div class="teacher-students">
    <NavBar :links="links" />

    <!-- Main Content -->
    <main class="main-content">
      <div class="container">
        <div class="dashboard-layout-students">
          <div class="header-section header-controls">
            <h2 class="card-title">My Students</h2>
            <div class="controls">
              <input v-model="searchTerm" placeholder="Search students by name or email" class="search-input" />
              <select v-model="sortOption" class="sort-select">
                <option value="name">Sort: Name</option>
                <option value="score">Sort: Avg score</option>
                <option value="completed">Sort: Completed</option>
              </select>
            </div>
          </div>

          <!-- Loading State -->
          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <p>Loading students...</p>
          </div>

          <!-- Error State -->
          <div v-else-if="error" class="error-state">
            <p>{{ error }}</p>
            <button @click="fetchStudents" class="retry-btn">Try Again</button>
          </div>

          <!-- Students Grid -->
          <div v-else class="students-grid">
            <div v-for="student in filteredStudents"
                 :key="student.id"
                 class="student-card"
                 @click="showStudentDetails(student.id)">
              <div class="student-avatar-grid">
                <div class="student-avatar">
                  <span>{{ getInitials(student.firstName, student.lastName) }}</span>
                </div>
              </div>

              <div class="student-info">
                <h3>{{ student.firstName }} {{ student.lastName }}</h3>
                <p class="muted">{{ student.email }}</p>
                <div class="student-stats">
                  <div class="stat">
                    <span class="stat-label">Completed</span>
                    <span class="stat-value">{{ student.completedCount || 0 }}</span>
                  </div>
                  <div class="stat" style="margin-left:12px;">
                    <span class="stat-label">Avg</span>
                    <span class="stat-value">{{ student.averageScore != null ? student.averageScore : '-' }}</span>
                  </div>
                  <div class="stat" style="margin-left:12px;">
                    <span class="stat-label">Last</span>
                    <span class="stat-value">{{ student.lastCompletedAt ? formatDate(student.lastCompletedAt) : '-' }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <button class="btn small" @click.stop="showStudentDetails(student.id)">View Results</button>
                </div>
              </div>
            </div>

            <!-- Empty State -->
            <div v-if="filteredStudents.length === 0" class="empty-state">
              <p>No students found — assign exams or invite students to register.</p>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Student Details Modal -->
    <div v-if="showModal" class="modal" @click="closeModal">
      <div class="modal-content" @click.stop>
        <span class="close" @click="closeModal">&times;</span>
        <div v-if="selectedStudent" class="student-details">
          <h3>Student Details</h3>
          <div class="student-detail-item">
            <strong>Name:</strong> {{ selectedStudent.firstName }} {{ selectedStudent.lastName }}
          </div>
          <div class="student-detail-item">
            <strong>Email:</strong> {{ selectedStudent.email }}
          </div>
          <div class="student-detail-item">
            <strong>Joined:</strong> {{ formatDate(selectedStudent.createdAt) }}
          </div>

          <!-- Enrolled Exams Section -->
          <div v-if="studentEnrolledExams.length > 0" class="enrolled-exams-section">
            <h4>Enrolled Exams</h4>
            <div class="exams-list">
              <div v-for="exam in studentEnrolledExams" :key="exam.id" class="exam-item">
                <div class="exam-title">{{ exam.title }}</div>
                <div class="exam-details-row">
                  <span class="detail-label">Duration:</span>
                  <span class="detail-value">{{ exam.durationMinutes }} min</span>
                </div>
                <div class="exam-details-row">
                  <span class="detail-label">Status:</span>
                  <span :class="['status-badge', exam.lastAttemptFinishedAt ? 'completed' : 'pending']">
                    {{ exam.lastAttemptFinishedAt ? 'Completed' : 'Pending' }}
                  </span>
                </div>
                <div v-if="exam.lastAttemptScore != null" class="exam-details-row">
                  <span class="detail-label">Score:</span>
                  <span class="detail-value">{{ exam.lastAttemptScore }}</span>
                </div>
                <div v-if="exam.lastAttemptFinishedAt" class="exam-details-row">
                  <span class="detail-label">Completed:</span>
                  <span class="detail-value">{{ formatDate(exam.lastAttemptFinishedAt) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="no-exams">
            <p>No enrolled exams yet</p>
          </div>
        </div>
        <div v-else class="loading-state">
          <p>Loading student details...</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '../../api'
import NavBar from '../../components/NavBar.vue';

const links = [
  { to: '/teacher/profile', label: 'Profile' },
  { to: '/teacher/exams', label: 'Exams' },
  { to: '/teacher/students', label: 'My Students' }
];
import { useRouter } from 'vue-router'

const students = ref([])
const searchTerm = ref('')
const sortOption = ref('name')
const loading = ref(false)
const error = ref('')
const showModal = ref(false)
const selectedStudent = ref(null)
const studentEnrolledExams = ref([])
const router = useRouter()

onMounted(() => {
  fetchStudents()
})

async function fetchStudents() {
  loading.value = true
  error.value = ''
  try {
    let ids = []
    const loaded = []
    try {
      const studentsRes = await api.teacherStudents.list();
      const users = studentsRes.data || [];
      if (Array.isArray(users) && users.length > 0) {
        ids = users.map(u => u.id);
        // preload basic user info into loaded
        for (const u of users) {
          loaded.push({ id: u.id, firstName: u.firstName, lastName: u.lastName, email: u.email });
        }
      }
    } catch (e) {
      const examsRes = await api.teacherExams.list();
      const exams = examsRes.data || []
      const studentIdSet = new Set()
      for (const exam of exams) {
        try {
          const assignmentsRes = await api.teacherExams.assignments(exam.id)
          const assignments = assignmentsRes.data || []
          assignments.forEach(a => studentIdSet.add(a.studentId))
        } catch (e) {
          // ignore per-exam assignment errors
        }
      }
      ids = Array.from(studentIdSet)
    }
    for (const id of ids) {
      try {
        const userRes = await api.users.byId(id)
        const user = userRes.data

        const resultsRes = await api.teacherStudents.results(id)
        const results = resultsRes.data || []

        let completed = 0
        let scores = []
        let lastCompletedAt = null
        results.forEach(r => {
          if (r.lastAttemptFinishedAt) {
            completed += 1
            if (r.lastAttemptScore != null) scores.push(r.lastAttemptScore)
            const d = new Date(r.lastAttemptFinishedAt)
            if (!lastCompletedAt || d > new Date(lastCompletedAt)) lastCompletedAt = r.lastAttemptFinishedAt
          }
        })

        const avgScore = scores.length ? Math.round((scores.reduce((s, v) => s + v, 0) / scores.length) * 100) / 100 : null

        loaded.push({
          id: user.id,
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
          completedCount: completed,
          averageScore: avgScore,
          lastCompletedAt
        })
      } catch (err) {
        console.error('Error loading student', id, err)
      }
    }

    students.value = loaded
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to load students'
    console.error('Error fetching students:', err)
  } finally {
    loading.value = false
  }
}

const filteredStudents = computed(() => {
  let list = students.value.slice()
  if (searchTerm.value && searchTerm.value.trim()) {
    const q = searchTerm.value.trim().toLowerCase()
    list = list.filter(s => (s.firstName + ' ' + s.lastName + ' ' + s.email).toLowerCase().includes(q))
  }
  if (sortOption.value === 'name') {
    list.sort((a, b) => (a.firstName + ' ' + a.lastName).localeCompare(b.firstName + ' ' + b.lastName))
  } else if (sortOption.value === 'score') {
    list.sort((a, b) => (b.averageScore || 0) - (a.averageScore || 0))
  } else if (sortOption.value === 'completed') {
    list.sort((a, b) => (b.completedCount || 0) - (a.completedCount || 0))
  }
  return list
})

async function showStudentDetails(studentId) {
  showModal.value = true
  selectedStudent.value = null
  studentEnrolledExams.value = []

  try {
    const response = await api.teacherStudents.details(studentId)
    selectedStudent.value = response.data

    try {
      const resultsRes = await api.teacherStudents.results(studentId)
      const results = resultsRes.data || []

      const examsData = []
      for (const result of results) {
        try {
          const examRes = await api.teacherExams.details(result.examId)
          examsData.push({
            id: result.examId,
            title: examRes.data.title,
            durationMinutes: examRes.data.durationMinutes,
            lastAttemptScore: result.lastAttemptScore,
            lastAttemptFinishedAt: result.lastAttemptFinishedAt
          })
        } catch (e) {
          console.error('Error fetching exam details:', e)
        }
      }
      studentEnrolledExams.value = examsData
    } catch (err) {
      console.error('Error fetching student exam results:', err)
    }
  } catch (err) {
    console.error('Error fetching student details:', err)
  }
}

function closeModal() {
  showModal.value = false
  selectedStudent.value = null
}

function getInitials(firstName, lastName) {
  return `${firstName?.charAt(0) || ''}${lastName?.charAt(0) || ''}`.toUpperCase()
}

function formatDate(dateString) {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function toggleNotifications() {
  console.log('Toggle notifications')
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('userRole')
  router.push('/auth')
}
</script>
<style>
:root {
  --primary-color: #2563eb;
  --primary-light: #f0f5ff;
  --text-dark: #1a1a1a;
  --text-medium: #333333;
  --text-light: #666666;
  --border-color: #e6e6e6;
  --white: #ffffff;
  --shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  --shadow-sm: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.teacher-students {
  min-height: 100vh;
  background-color: #f8f9fa;
}


.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

/* Main Content */
.main-content {
  padding: 2rem 0;
}

.dashboard-layout-students {
  background: var(--white);
  border-radius: 12px;
  padding: 2rem;
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
}

.header-section {
  margin-bottom: 2rem;
}

.card-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-dark);
  margin: 0;
}

/* Students Grid */
.students-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.student-card {
  background: var(--white);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  align-items: center;
  text-align: center;
}

.student-card:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-sm);
  transform: translateY(-2px);
}

.student-avatar-grid {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
}

.student-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--white);
  font-weight: 600;
  font-size: 1.25rem;
  flex-shrink: 0;
}

.student-info {
  flex: 1;
  width: 100%;
}

.student-info h3 {
  margin: 0.5rem 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-dark);
  line-height: 1.3;
  word-wrap: break-word;
}

.student-info p {
  margin: 0.3rem 0;
  color: var(--text-light);
  font-size: 0.875rem;
  word-break: break-word;
  line-height: 1.4;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: var(--text-light);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-color);
  border-top: 4px solid var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Error State */
.error-state {
  text-align: center;
  padding: 3rem;
  color: #dc2626;
}

.retry-btn {
  background: var(--primary-color);
  color: var(--white);
  border: none;
  border-radius: 6px;
  padding: 0.5rem 1rem;
  margin-top: 1rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.retry-btn:hover {
  background-color: #1d4ed8;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 3rem;
  color: var(--text-light);
  grid-column: 1 / -1;
}

/* Modal */
.modal {
  display: flex;
  position: fixed;
  z-index: 1000;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  align-items: center;
  justify-content: center;
}

.modal-content {
  background-color: var(--white);
  padding: 2rem;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  position: relative;
  box-shadow: var(--shadow);
}

.close {
  position: absolute;
  top: 1rem;
  right: 1.5rem;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--text-light);
}

.close:hover {
  color: var(--text-dark);
}

.student-details h3 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  color: var(--text-dark);
}

.student-detail-item {
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--border-color);
}

.student-detail-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.student-detail-item strong {
  color: var(--text-dark);
  margin-right: 0.5rem;
}

/* Enrolled Exams Section */
.enrolled-exams-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-color);
}

.enrolled-exams-section h4 {
  color: var(--text-dark);
  margin-top: 0;
  margin-bottom: 1rem;
  font-size: 1rem;
}

.exams-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.exam-item {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 6px;
  border-left: 3px solid var(--primary-color);
}

.exam-title {
  font-weight: 600;
  color: var(--text-dark);
  margin-bottom: 0.5rem;
  font-size: 1rem;
}

.exam-details-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.875rem;
  margin: 0.25rem 0;
}

.detail-label {
  color: var(--text-light);
  font-weight: 500;
}

.detail-value {
  color: var(--text-dark);
  font-weight: 600;
}

.status-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
}

.status-badge.completed {
  background: #dcfce7;
  color: #166534;
}

.status-badge.pending {
  background: #fef3c7;
  color: #92400e;
}

.no-exams {
  text-align: center;
  padding: 1.5rem;
  color: var(--text-light);
  font-style: italic;
}

/* Responsive Design */
@media (max-width: 768px) {
  .container {
    padding: 0 1rem;
  }

  .students-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-layout-students{
    padding: 1.5rem;
  }

  .modal-content {
    margin: 1rem;
    width: calc(100% - 2rem);
  }
}

/* Controls and enhanced card styles */
.header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}
.header-controls .controls {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}
.search-input {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  min-width: 220px;
}
.sort-select {
  padding: 0.45rem 0.6rem;
  border-radius: 6px;
  border: 1px solid var(--border-color);
  background: #fff;
}
.muted { color: var(--text-light); font-size: 0.9rem; margin: 0.25rem 0; }
.student-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
  margin-top: 0.75rem;
  flex-wrap: wrap;
  width: 100%;
}
.student-stats .stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-label { font-size: 0.75rem; color: var(--text-light); font-weight: 500; }
.stat-value { font-weight: 700; color: var(--text-dark); font-size: 1rem; margin-top: 0.25rem; }
.card-actions {
  margin-top: 0.75rem;
  width: 100%;
}
.btn.small {
  padding: 0.35rem 0.6rem;
  font-size: 0.85rem;
  border-radius: 6px;
  border: none;
  background: var(--primary-color);
  color: #fff;
  cursor: pointer;
  width: 100%;
}
.btn.small:hover { background: #1d4ed8; }
</style>

<template>
  <div class="teacher-exams">
    <NavBar :links="links" />

    <main class="main-content">
      <div class="container">
        <div class="dashboard-layout-exams">
          <div class="header-section">
            <h2 class="card-title">My Exams</h2>
            <button @click="showExamModal = true" class="add-button" aria-label="Add new exam">
              <svg width="41" height="40" viewBox="0 0 41 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="20.5251" cy="20" r="20" fill="#F6F5F5" />
                <line y1="-1" x2="20" y2="-1" transform="matrix(0 1 1 0 21.5253 10)" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                <line x1="10.5253" y1="20" x2="30.5253" y2="20" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
              </svg>
            </button>
          </div>

          <!-- Create/Edit Exam Modal -->
          <div v-if="showExamModal" class="modal" @click.self="closeCreateModal">
            <div class="modal-content">
              <span class="close" @click="closeCreateModal">&times;</span>
              <h2>{{ isEditingExam ? 'Edit Exam' : 'Add New Exam' }}</h2>

              <div v-if="createError" class="message error">{{ createError }}</div>

              <form @submit.prevent="createExam">
                <div class="form-group">
                  <label for="title">Title:</label>
                  <input type="text" id="title" v-model="newExam.title" required>
                </div>
                <div class="form-group">
                  <label for="description">Description:</label>
                  <textarea id="description" v-model="newExam.description"></textarea>
                </div>
                <div class="form-group">
                  <label for="duration">Duration (minutes):</label>
                  <input type="number" id="duration" v-model.number="newExam.durationMinutes" required min="1">
                </div>
                <div class="form-group">
                  <label for="passingScore">Passing Score (%):</label>
                  <input type="number" id="passingScore" v-model.number="newExam.passingScore" required min="0" max="100">
                </div>
                <div class="form-actions">
                  <button type="submit" class="submit-btn primary">{{ isEditingExam ? 'Update Exam' : 'Create Exam' }}</button>
                  <button type="button" class="submit-btn secondary" @click="closeCreateModal">Cancel</button>
                </div>
              </form>
            </div>
          </div>

          <!-- Exam Details Modal -->
          <div v-if="selectedExam" class="modal" @click.self="selectedExam = null">
            <div class="modal-content wide-modal">
              <span class="close" @click="selectedExam = null">&times;</span>
              <div class="exam-details">
                <div class="exam-header">
                  <div class="exam-icon large">
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                      <polyline points="14 2 14 8 20 8"></polyline>
                      <line x1="16" y1="13" x2="8" y2="13"></line>
                      <line x1="16" y1="17" x2="8" y2="17"></line>
                      <polyline points="10 9 9 9 8 9"></polyline>
                    </svg>
                  </div>
                  <h2>{{ selectedExam.title }}</h2>
                  <p class="exam-description">{{ selectedExam.description || 'No description provided' }}</p>
                </div>

                <div class="exam-info-section">
                  <h3>Exam Information</h3>
                  <div class="info-grid">
                    <div class="info-item">
                      <span class="info-label">Duration:</span>
                      <span class="info-value">{{ selectedExam.durationMinutes }} minutes</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">Passing Score:</span>
                      <span class="info-value">{{ selectedExam.passingScore }}%</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">Total Students:</span>
                      <span class="info-value">{{ studentAssignments.length }}</span>
                    </div>
                  </div>
                </div>

                <div class="exam-students-section">
                  <h3>Enrolled Students</h3>
                  <div class="add-student-section">
                    <h4>Add Student by Email</h4>
                    <div class="add-student-form">
                      <div class="email-input-wrapper">
                        <input
                            type="email"
                            v-model="studentEmail"
                            @input="onEmailInput"
                            @focus="showSearchResults = true"
                            @blur="hideSearchResults"
                            placeholder="Enter student's email"
                            class="email-input">
                        <div v-if="showSearchResults && searchResults.length > 0" class="search-dropdown">
                          <div v-for="student in searchResults" :key="student.id"
                               class="search-result-item"
                               @mousedown="selectStudentFromSearch(student)">
                            <strong>{{ student.firstName }} {{ student.lastName }}</strong>
                            <small>{{ student.email }}</small>
                          </div>
                        </div>
                      </div>
                      <button @click="addStudentToExam" class="add-student-btn" :disabled="!studentEmail">Add Student</button>
                    </div>
                    <div v-if="addStudentMessage" :class="['message', addStudentMessageType]">
                      {{ addStudentMessage }}
                    </div>
                  </div>

                  <div class="students-table-container">
                    <table class="results-table">
                      <thead>
                      <tr>
                        <th>First Name</th>
                        <th>Last Name</th>
                        <th>Email</th>
                        <th>Status</th>
                        <th>Score</th>
                        <th>Completed</th>
                        <th>Actions</th>
                      </tr>
                      </thead>
                      <tbody>
                      <tr v-if="studentAssignments.length === 0">
                        <td colspan="7" class="no-data">No students enrolled yet</td>
                      </tr>
                      <tr v-for="assignment in studentAssignments" :key="assignment.id">
                        <td class="name-cell">
                          {{ assignment.studentFirstName || 'N/A' }}
                        </td>
                        <td class="name-cell">
                          {{ assignment.studentLastName || 'N/A' }}
                        </td>
                        <td class="email-cell">
                          {{ assignment.studentEmail || 'N/A' }}
                        </td>
                        <td>
                  <span :class="['status', assignment.completedAt ? 'completed' : 'pending']">
                    {{ assignment.completedAt ? 'Completed' : 'Pending' }}
                  </span>
                        </td>
                        <td class="score-cell">{{ assignment.score || '-' }}</td>
                        <td class="date-cell">{{ assignment.completedAt ? formatDate(assignment.completedAt) : '-' }}</td>
                        <td>
                          <button class="delete-btn" @click="removeStudent(assignment.studentId)" aria-label="Remove student">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                              <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                            </svg>
                          </button>
                        </td>
                      </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="exams-grid" v-if="exams.length > 0">
            <ExamCard
                v-for="(exam, index) in exams"
                :key="exam.id"
                :exam="{
                  ...exam,
                  questionCount: exam.questionCount || 0,
                  teacherFirstName: 'You',
                  teacherLastName: '',
                  teacherEmail: ''
                }"
            >
              <template #actions>
                <button @click.stop="editExam(exam)" class="exam-btn edit-btn">Edit</button>
                <button @click.stop="showExamDetails(exam.id)" class="exam-btn view-btn">View</button>
              </template>
            </ExamCard>
          </div>

          <div v-else class="empty-state">
            <div class="empty-state-icon">📝</div>
            <h3 class="empty-state-title">No Exams Yet</h3>
            <p class="empty-state-message">Create your first exam to get started</p>
            <button @click="showExamModal = true" class="submit-btn primary">Create Exam</button>
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

function debounce(func, delay) {
  let timeoutId;
  return function(...args) {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func.apply(this, args), delay);
  };
}

const links = [
  { to: '/teacher/profile', label: 'Profile' },
  { to: '/teacher/exams', label: 'Exams' },
  { to: '/teacher/students', label: 'My Students' }
];

const router = useRouter();

const exams = ref([]);
const showExamModal = ref(false);
const selectedExam = ref(null);
const studentAssignments = ref([]);
const studentEmail = ref('');
const addStudentMessage = ref('');
const addStudentMessageType = ref('');
const isEditingExam = ref(false);
const searchResults = ref([]);
const showSearchResults = ref(false);

const newExam = ref({
  title: '',
  description: '',
  durationMinutes: 60,
  passingScore: 60
});

const createError = ref('');

const debouncedSearchStudents = debounce(async (email) => {
  if (!email || email.length < 3) {
    searchResults.value = [];
    return;
  }

  try {
    const response = await api.usersSearch.byEmail(email);
    const data = response.data || [];
    if (Array.isArray(data)) {
      searchResults.value = data.filter(u => u.role === 'STUDENT');
    } else {
      searchResults.value = [];
    }
  } catch (error) {
    console.error('Error searching students:', error);
    searchResults.value = [];
  }
}, 400);

async function fetchExams() {
  try {
    const res = await api.teacherExams.list();
    exams.value = res.data || [];

    // Fetch assignment counts for each exam
    for (let exam of exams.value) {
      try {
        const assignments = await api.teacherExams.assignments(exam.id);
        exam.assignedStudentCount = assignments.data?.length || 0;
      } catch (e) {
        exam.assignedStudentCount = 0;
      }
    }
  } catch (error) {
    console.error('Failed to fetch exams:', error);
    if (error.response?.status === 401) {
      router.push('/auth');
    }
  }
}

async function createExam() {
  createError.value = '';

  if (!newExam.value.title?.trim()) {
    createError.value = 'Please provide a title';
    return;
  }
  if (newExam.value.durationMinutes <= 0) {
    createError.value = 'Duration must be greater than 0';
    return;
  }
  if (newExam.value.passingScore < 0 || newExam.value.passingScore > 100) {
    createError.value = 'Passing score must be between 0 and 100';
    return;
  }

  try {
    const payload = {
      title: newExam.value.title.trim(),
      description: newExam.value.description?.trim() || '',
      durationMinutes: newExam.value.durationMinutes,
      passingScore: newExam.value.passingScore
    };

    if (isEditingExam.value && selectedExam.value?.id) {
      await api.teacherExams.update(selectedExam.value.id, payload);
    } else {
      await api.teacherExams.create(payload);
    }

    await fetchExams();
    closeCreateModal();
  } catch (error) {
    console.error('Failed to create/update exam:', error);
    createError.value = error.response?.data?.message || 'Failed to create/update exam';
  }
}

async function showExamDetails(examId) {
  try {
    const res = await api.teacherExams.details(examId);
    selectedExam.value = res.data;

    const assignmentsRes = await api.teacherExams.assignments(examId);
    console.log('Student assignments:', assignmentsRes.data); // Добавьте эту строку для отладки
    studentAssignments.value = assignmentsRes.data || [];
  } catch (error) {
    console.error('Failed to fetch exam details:', error);
  }
}

function onEmailInput(e) {
  const email = e.target.value.trim();
  debouncedSearchStudents(email);
}

function hideSearchResults() {
  setTimeout(() => {
    showSearchResults.value = false;
  }, 200);
}

function selectStudentFromSearch(student) {
  studentEmail.value = student.email;
  searchResults.value = [];
  showSearchResults.value = false;
}

async function addStudentToExam() {
  if (!studentEmail.value || !selectedExam.value) return;

  addStudentMessage.value = '';
  addStudentMessageType.value = 'error';

  try {
    const userRes = await api.usersSearch.byEmail(studentEmail.value);
    let found = null;

    if (Array.isArray(userRes.data)) {
      found = userRes.data.find(u => u.email?.toLowerCase() === studentEmail.value.toLowerCase());
    } else {
      found = userRes.data;
    }

    if (!found || !found.id) {
      addStudentMessage.value = 'Student not found';
      return;
    }

    await api.teacherExams.assign(selectedExam.value.id, [found.id]);

    addStudentMessageType.value = 'success';
    addStudentMessage.value = 'Student added successfully';
    studentEmail.value = '';
    searchResults.value = [];

    await showExamDetails(selectedExam.value.id);
  } catch (error) {
    console.error('Failed to add student:', error);
    addStudentMessage.value = error.response?.data?.message || 'Failed to add student';
  }
}

async function removeStudent(studentId) {
  if (!confirm('Are you sure you want to remove this student?')) return;

  try {
    await api.teacherExams.removeAssignment(selectedExam.value.id, studentId);
    await showExamDetails(selectedExam.value.id);
  } catch (error) {
    console.error('Failed to remove student:', error);
    alert('Failed to remove student');
  }
}

function formatDate(dateString) {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString();
}

function getInitials(name) {
  if (!name) return '??';
  return name.split(' ').map(n => n.charAt(0)).join('').toUpperCase().substring(0, 2);
}

function editExam(exam) {
  isEditingExam.value = true;
  newExam.value = {
    title: exam.title,
    description: exam.description,
    durationMinutes: exam.durationMinutes,
    passingScore: exam.passingScore
  };
  selectedExam.value = exam;
  showExamModal.value = true;
}

function closeCreateModal() {
  showExamModal.value = false;
  isEditingExam.value = false;
  selectedExam.value = null;
  newExam.value = { title: '', description: '', durationMinutes: 60, passingScore: 60 };
  createError.value = '';
}

onMounted(fetchExams);
</script>

<style scoped>
.teacher-exams {
  min-height: 100vh;
  background-color: var(--light-blue);
}

.dashboard-layout-exams {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.card-title {
  margin: 0;
  color: var(--text-dark);
  font-size: 24px;
  font-weight: 600;
}

.add-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.add-button:hover {
  background-color: var(--light-gray);
}

/* Exams Grid */
.exams-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.name-cell {
  font-weight: 500;
  color: var(--text-dark);
}

.email-cell {
  color: var(--text-gray);
  font-size: 13px;
}
/* Exam Details Styles */
.exam-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.exam-icon.large {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F3F6FF;
  border-radius: 12px;
  margin-bottom: 8px;
}

.exam-description {
  color: #666;
  margin: 0;
  line-height: 1.5;
  font-size: 14px;
}

.exam-info-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.exam-info-section h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: var(--text-dark);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.info-label {
  color: #666;
  font-weight: 500;
  font-size: 14px;
}

.info-value {
  color: #333;
  font-weight: 600;
  font-size: 14px;
}

.exam-students-section h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: var(--text-dark);
}

.add-student-section {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.add-student-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #374151;
}

/* Table styles for student details */
.name-cell {
  font-weight: 500;
  color: var(--text-dark);
  font-size: 14px;
}

.email-cell {
  color: var(--text-gray);
  font-size: 13px;
}

.score-cell {
  font-weight: 600;
  text-align: center;
}

.date-cell {
  font-size: 13px;
  color: var(--text-gray);
}

/* Responsive table */
@media (max-width: 768px) {
  .exam-info-section {
    padding: 12px;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .info-item {
    padding: 10px;
  }

  .name-cell,
  .email-cell {
    font-size: 13px;
  }
}

.exam-btn {
  flex: 1;
  padding: 8px 16px;
  border: 1px solid;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.edit-btn {
  background: white;
  border-color: var(--primary-blue);
  color: var(--primary-blue);
}

.edit-btn:hover {
  background: var(--primary-blue);
  color: white;
}

.view-btn {
  background: white;
  border-color: var(--text-gray);
  color: var(--text-gray);
}

.view-btn:hover {
  background: var(--text-gray);
  color: white;
}

/* Modal Styles */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
}

.wide-modal {
  max-width: 800px;
}

.close {
  position: absolute;
  top: 16px;
  right: 16px;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-gray);
  background: none;
  border: none;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close:hover {
  color: var(--text-dark);
}

/* Form Styles */
.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: var(--text-dark);
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--primary-blue);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.submit-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  flex: 1;
}

.submit-btn.primary {
  background: var(--primary-blue);
  color: white;
}

.submit-btn.primary:hover {
  background: var(--secondary-blue);
}

.submit-btn.secondary {
  background: var(--light-gray);
  color: var(--text-dark);
}

.submit-btn.secondary:hover {
  background: #e0e0e0;
}

/* Student Search */
.email-input-wrapper {
  position: relative;
  flex: 1;
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid var(--border-color);
  border-top: none;
  border-radius: 0 0 6px 6px;
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.search-result-item {
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--light-gray);
  transition: background-color 0.2s;
}

.search-result-item:hover {
  background-color: var(--light-blue);
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item strong {
  display: block;
  font-size: 14px;
  margin-bottom: 2px;
}

.search-result-item small {
  font-size: 12px;
  color: var(--text-gray);
}

.add-student-form {
  display: flex;
  gap: 12px;
  margin: 16px 0;
  flex-wrap: wrap;
}

.add-student-btn {
  padding: 10px 20px;
  background: var(--primary-blue);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}

.add-student-btn:disabled {
  background: var(--text-gray);
  cursor: not-allowed;
}

.add-student-btn:not(:disabled):hover {
  background: var(--secondary-blue);
}

/* Students Table */
.students-table-container {
  overflow-x: auto;
  margin-top: 20px;
}

.results-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.results-table th {
  background: var(--light-gray);
  padding: 12px;
  text-align: left;
  font-weight: 600;
  color: var(--text-dark);
  border-bottom: 1px solid var(--border-color);
}

.results-table td {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  vertical-align: middle;
}

.results-table tr:hover {
  background-color: var(--light-blue);
}

.no-data {
  text-align: center;
  color: var(--text-gray);
  font-style: italic;
  padding: 40px !important;
}

.status {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status.completed {
  background: #dcfce7;
  color: #166534;
}

.status.pending {
  background: #fef3c7;
  color: #92400e;
}

.delete-btn {
  background: none;
  border: none;
  padding: 6px;
  cursor: pointer;
  color: #dc2626;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.delete-btn:hover {
  background: #fee2e2;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-gray);
}

.empty-state-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-dark);
}

.empty-state-message {
  margin-bottom: 24px;
}

/* Message Styles */
.message {
  padding: 12px;
  border-radius: 6px;
  margin: 12px 0;
  font-size: 14px;
}

.message.success {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.message.error {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

/* Responsive Design */
@media (max-width: 768px) {
  .container {
    padding-left: 16px;
    padding-right: 16px;
  }

  .dashboard-layout-exams {
    padding: 16px;
  }

  .header-section {
    flex-direction: column;
    align-items: stretch;
  }

  .card-title {
    font-size: 20px;
  }

  .exams-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .modal {
    padding: 10px;
  }

  .modal-content {
    padding: 20px;
  }

  .wide-modal {
    max-width: 100%;
  }

  .form-actions {
    flex-direction: column;
  }

  .add-student-form {
    flex-direction: column;
  }

  .students-table-container {
    font-size: 12px;
  }

  .results-table th,
  .results-table td {
    padding: 8px;
  }
}

@media (max-width: 480px) {
  .dashboard-layout-exams{
    padding: 12px;
  }

  .modal-content {
    padding: 16px;
  }

  .results-table {
    font-size: 12px;
  }

  .results-table th,
  .results-table td {
    padding: 6px 4px;
  }

  .status {
    font-size: 11px;
    padding: 3px 6px;
  }
}

/* Print Styles */
@media print {
  .nav-bar,
  .add-button,
  .delete-btn {
    display: none !important;
  }

  .dashboard-layout-exams {
    box-shadow: none;
    border: 1px solid #ccc;
  }
}
</style>
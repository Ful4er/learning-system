<template>
  <div>
    <nav class="nav-bar">
      <div class="container">
        <div class="nav-content">
          <ul class="nav-links">
            <li>
              <router-link to="/teacher/profile" class="nav-link">Profile</router-link>
            </li>
            <li>
              <router-link to="/teacher/exams" class="nav-link active">Exams</router-link>
            </li>
            <li>
              <router-link to="/teacher/students" class="nav-link">My Students</router-link>
            </li>
          </ul>
          <button class="notification-btn">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12.5 14.1667H16.6667L15.4959 12.9958C15.3386 12.8386 15.2139 12.6519 15.1289 12.4464C15.0438 12.2409 15 12.0207 15 11.7983V9.16667C15.0002 8.13245 14.6797 7.12362 14.0827 6.27907C13.4858 5.43453 12.6417 4.7958 11.6667 4.45083V4.16667C11.6667 3.72464 11.4911 3.30072 11.1786 2.98816C10.866 2.67559 10.4421 2.5 10 2.5C9.55801 2.5 9.13409 2.67559 8.82153 2.98816C8.50897 3.30072 8.33337 3.72464 8.33337 4.16667V4.45083C6.39171 5.1375 5.00004 6.99 5.00004 9.16667V11.7992C5.00004 12.2475 4.82171 12.6783 4.50421 12.9958L3.33337 14.1667H7.50004M12.5 14.1667V15C12.5 15.663 12.2366 16.2989 11.7678 16.7678C11.299 17.2366 10.6631 17.5 10 17.5C9.337 17.5 8.70111 17.2366 8.23227 16.7678C7.76343 16.2989 7.50004 15.663 7.50004 15V14.1667M12.5 14.1667H7.50004" stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></path>
            </svg>
          </button>
        </div>
      </div>
    </nav>

    <main class="main-content">
      <div class="container">
        <div class="dashboard-layout">
          <div class="header-section">
            <h2 class="card-title">My exams</h2>
            <button @click="showExamModal = true" class="add-button">
              <svg width="41" height="40" viewBox="0 0 41 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="20.5251" cy="20" r="20" fill="#F6F5F5" />
                <line y1="-1" x2="20" y2="-1" transform="matrix(0 1 1 0 21.5253 10)" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                <line x1="10.5253" y1="20" x2="30.5253" y2="20" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
              </svg>
            </button>
          </div>

          <!-- Create Exam Modal -->
          <div v-if="showExamModal" class="modal" @click.self="showExamModal = false">
            <div class="modal-content">
              <span class="close" @click="showExamModal = false">&times;</span>
              <h2>Add New Exam</h2>
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
                  <input type="number" id="duration" v-model.number="newExam.durationMinutes" required>
                </div>
                <div class="form-group">
                  <label for="passingScore">Passing Score:</label>
                  <input type="number" id="passingScore" v-model.number="newExam.passingScore" required>
                </div>
                <button type="submit" class="submit-btn">Create Exam</button>
              </form>
            </div>
          </div>

          <!-- Exam Details Modal -->
          <div v-if="selectedExam" class="modal" @click.self="selectedExam = null">
            <div class="modal-content">
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
                      <span class="info-label">Created at:</span>
                      <span class="info-value">{{ formatDate(selectedExam.createdAt) }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">Duration:</span>
                      <span class="info-value">{{ selectedExam.durationMinutes }} minutes</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">Passing Score:</span>
                      <span class="info-value">{{ selectedExam.passingScore }}</span>
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
                    <h3>Add Student by Email</h3>
                    <div class="add-student-form">
                      <input type="email" v-model="studentEmail" placeholder="Enter student's email">
                      <button @click="addStudentToExam" class="add-student-btn" :disabled="!studentEmail">Add Student</button>
                    </div>
                    <div v-if="addStudentMessage" :class="['message', addStudentMessageType]">
                      {{ addStudentMessage }}
                    </div>
                  </div>
                  <table class="results-table">
                    <thead>
                      <tr>
                        <th>Student</th>
                        <th>Status</th>
                        <th>Score</th>
                        <th>Completed</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="studentAssignments.length === 0">
                        <td colspan="5">No students enrolled yet</td>
                      </tr>
                      <tr v-for="assignment in studentAssignments" :key="assignment.id">
                        <td>
                          <div class="student-info">
                            <div class="student-avatar small">
                              {{ getInitials(assignment.studentName) }}
                            </div>
                            <div>
                              <div class="student-name">{{ assignment.studentName }}</div>
                              <div class="student-email">{{ assignment.studentEmail }}</div>
                            </div>
                          </div>
                        </td>
                        <td>
                          <span :class="['status', assignment.completedAt ? 'completed' : 'pending']">
                            {{ assignment.completedAt ? 'Completed' : 'Pending' }}
                          </span>
                        </td>
                        <td>{{ assignment.score || '-' }}</td>
                        <td>{{ assignment.completedAt ? formatDate(assignment.completedAt) : '-' }}</td>
                        <td>
                          <button class="delete-btn" @click="removeStudent(assignment.studentId)">
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

          <div class="exams-grid">
            <div 
              v-for="(exam, index) in exams" 
              :key="exam.id" 
              class="exam-card"
              @click="showExamDetails(exam.id)">
              <a href="#" class="exam-link">Exam №{{ index + 1 }}. {{ exam.title }}</a>
              <p class="exam-stats">Number of students: {{ exam.assignedStudentCount || 0 }}</p>
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

const router = useRouter();

const exams = ref([]);
const showExamModal = ref(false);
const selectedExam = ref(null);
const studentAssignments = ref([]);
const studentEmail = ref('');
const addStudentMessage = ref('');
const addStudentMessageType = ref('');

const newExam = ref({
  title: '',
  description: '',
  durationMinutes: 60,
  passingScore: 60
});

async function fetchExams() {
  try {
    const res = await axios.get('/api/teacher/exams');
    exams.value = res.data || [];
  } catch (error) {
    console.error('Failed to fetch exams:', error);
    if (error.response?.status === 401) {
      router.push('/auth');
    }
  }
}

async function createExam() {
  try {
    await axios.post('/api/teacher/exams', {
      title: newExam.value.title,
      description: newExam.value.description,
      durationMinutes: newExam.value.durationMinutes,
      passingScore: newExam.value.passingScore
    });
    
    showExamModal.value = false;
    newExam.value = { title: '', description: '', durationMinutes: 60, passingScore: 60 };
    await fetchExams();
  } catch (error) {
    console.error('Failed to create exam:', error);
  }
}

async function showExamDetails(examId) {
  try {
    const res = await axios.get(`/api/teacher/exams/${examId}`);
    selectedExam.value = res.data;
    
    // Fetch assignments - note: this endpoint returns assignments not students directly
    const assignmentsRes = await axios.get(`/api/teacher/exams/${examId}/assignments`);
    studentAssignments.value = assignmentsRes.data || [];
  } catch (error) {
    console.error('Failed to fetch exam details:', error);
  }
}

async function addStudentToExam() {
  if (!studentEmail.value || !selectedExam.value) return;
  
  addStudentMessage.value = '';
  addStudentMessageType.value = 'error';
  addStudentMessage.value = 'Finding student by email feature not yet implemented. Please use student ID manually for now.';
  setTimeout(() => { addStudentMessage.value = ''; }, 5000);
  
  // TODO: Implement finding student by email
  // This requires either:
  // 1. Adding an endpoint in exam-service to find students by email
  // 2. Or calling user-service directly from the frontend
}

async function removeStudent(studentId) {
  if (!confirm('Are you sure you want to remove this student?')) return;
  
  try {
    await axios.delete(`/api/teacher/exams/${selectedExam.value.id}/assignments/${studentId}`);
    await showExamDetails(selectedExam.value.id);
  } catch (error) {
    console.error('Failed to remove student:', error);
  }
}

function formatDate(dateString) {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleString();
}

function getInitials(name) {
  if (!name) return '';
  return name.split(' ').map(n => n.charAt(0)).join('');
}

onMounted(fetchExams);
</script>

<style>
@import '../../assets/css/teacher/exams.css';
</style>

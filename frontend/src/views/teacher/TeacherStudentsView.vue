<template>
  <div class="teacher-students">
    <!-- Navigation Bar -->
    <nav class="nav-bar">
      <div class="container">
        <div class="nav-content">
          <ul class="nav-links" role="menubar">
            <li role="none">
              <router-link to="/teacher/profile" class="nav-link" role="menuitem">
                Profile
              </router-link>
            </li>
            <li role="none">
              <router-link to="/teacher/exams" class="nav-link" role="menuitem">
                Exams
              </router-link>
            </li>
            <li role="none">
              <router-link to="/teacher/students" class="nav-link active" role="menuitem">
                My Students
              </router-link>
            </li>
          </ul>
          <button class="notification-btn" aria-label="Notifications" @click="toggleNotifications">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12.5 14.1667H16.6667L15.4959 12.9958C15.3386 12.8386 15.2139 12.6519 15.1289 12.4464C15.0438 12.2409 15 12.0207 15 11.7983V9.16667C15.0002 8.13245 14.6797 7.12362 14.0827 6.27907C13.4858 5.43453 12.6417 4.7958 11.6667 4.45083V4.16667C11.6667 3.72464 11.4911 3.30072 11.1786 2.98816C10.866 2.67559 10.4421 2.5 10 2.5C9.55801 2.5 9.13409 2.67559 8.82153 2.98816C8.50897 3.30072 8.33337 3.72464 8.33337 4.16667V4.45083C6.39171 5.1375 5.00004 6.99 5.00004 9.16667V11.7992C5.00004 12.2475 4.82171 12.6783 4.50421 12.9958L3.33337 14.1667H7.50004M12.5 14.1667V15C12.5 15.663 12.2366 16.2989 11.7678 16.7678C11.299 17.2366 10.6631 17.5 10 17.5C9.337 17.5 8.70111 17.2366 8.23227 16.7678C7.76343 16.2989 7.50004 15.663 7.50004 15V14.1667M12.5 14.1667H7.50004"
                    stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="main-content">
      <div class="container">
        <div class="dashboard-layout">
          <div class="header-section">
            <h2 class="card-title">My Students</h2>
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
            <div v-for="student in students"
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
                <p>{{ student.email }}</p>
              </div>
            </div>

            <!-- Empty State -->
            <div v-if="students.length === 0" class="empty-state">
              <p>No students found</p>
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
          <!-- Add more student details as needed -->
        </div>
        <div v-else class="loading-state">
          <p>Loading student details...</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

// Reactive data
const students = ref([])
const loading = ref(false)
const error = ref('')
const showModal = ref(false)
const selectedStudent = ref(null)

// Fetch students on component mount
onMounted(() => {
  fetchStudents()
})

// Methods
async function fetchStudents() {
  loading.value = true
  error.value = ''
  try {
    const response = await axios.get('/api/teacher/students')
    students.value = response.data
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to load students'
    console.error('Error fetching students:', err)
  } finally {
    loading.value = false
  }
}

async function showStudentDetails(studentId) {
  showModal.value = true
  selectedStudent.value = null

  try {
    const response = await axios.get(`/api/teacher/students/${studentId}`)
    selectedStudent.value = response.data
  } catch (err) {
    console.error('Error fetching student details:', err)
    // You might want to show an error message in the modal
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
  // Implement notification functionality
  console.log('Toggle notifications')
}
</script>
<style>
@import '../../assets/css/teacher/students.css';
</style>

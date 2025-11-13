<template>
  <div>
    <nav class="nav-bar">
      <div class="container">
        <div class="nav-content">
          <ul class="nav-links">
            <li>
              <router-link to="/teacher/profile" class="nav-link active">Profile</router-link>
            </li>
            <li>
              <router-link to="/teacher/exams" class="nav-link">Exams</router-link>
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
          <section class="profile-card">
            <span class="profile-image">
              {{ initials }}
            </span>
            <h1 class="profile-name">{{ user.firstName }} {{ user.lastName }}</h1>
            <p class="profile-email">{{ user.email }}</p>
            <div class="stats-section">
              <div class="stats-grid">
                <div class="stat-item">
                  <p class="stat-number">{{ studentCount }}</p>
                  <p class="stat-label">Students</p>
                </div>
                <div class="stat-item">
                  <p class="stat-number">{{ examCount }}</p>
                  <p class="stat-label">Exams</p>
                </div>
              </div>
            </div>
          </section>

          <section class="action-cards">
            <div class="action-card">
              <h2 class="card-title">My students</h2>
              <button class="add-button">
                <svg width="41" height="40" viewBox="0 0 41 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="20.5251" cy="20" r="20" fill="#F6F5F5" />
                  <line y1="-1" x2="20" y2="-1" transform="matrix(0 1 1 0 21.5253 10)" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                  <line x1="10.5253" y1="20" x2="30.5253" y2="20" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                </svg>
              </button>
            </div>

            <div class="action-card">
              <h2 class="card-title">My exams</h2>
              <button class="add-button">
                <svg width="41" height="40" viewBox="0 0 41 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="20.709" cy="20" r="20" fill="#F6F5F5" />
                  <line y1="-1" x2="20" y2="-1" transform="matrix(0 1 1 0 21.7091 10)" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                  <line x1="10.7091" y1="20" x2="30.7091" y2="20" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                </svg>
              </button>
            </div>
          </section>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();

const user = ref({ firstName: '', lastName: '', email: '' });
const studentCount = ref(0);
const examCount = ref(0);

const initials = computed(() => {
  return user.value.firstName?.charAt(0) + ' ' + user.value.lastName?.charAt(0);
});

async function fetchProfile() {
  try {
    const token = localStorage.getItem('token');
    if (!token) {
      router.push('/auth');
      return;
    }
    
    // Fetch user data
    const userRes = await axios.get('/api/users/me');
    user.value = userRes.data;
    
    // Fetch exams to get counts
    const examsRes = await axios.get('/api/teacher/exams');
    examCount.value = examsRes.data?.length || 0;
    
    // Get unique student count from all exam assignments
    const studentMap = new Set();
    for (const exam of examsRes.data) {
      try {
        const assignmentsRes = await axios.get(`/api/teacher/exams/${exam.id}/assignments`);
        assignmentsRes.data.forEach(a => studentMap.add(a.studentId));
      } catch (error) {
        // Skip if can't fetch assignments
      }
    }
    studentCount.value = studentMap.size;
  } catch (error) {
    console.error('Failed to fetch profile:', error);
    if (error.response?.status === 401) {
      router.push('/auth');
    }
  }
}

onMounted(fetchProfile);
</script>

<style>
@import '../../assets/css/teacher/profile.css';
</style>


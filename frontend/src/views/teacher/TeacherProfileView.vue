<template>
  <div>
    <NavBar :links="links" />

    <main class="main-content">
      <div class="container">
        <div class="dashboard-layout-profile">
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
            <router-link to="/teacher/students" class="action-card action-link">
              <h2 class="card-title">My students</h2>
              <button class="add-button">
                <svg width="41" height="40" viewBox="0 0 41 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="20.5251" cy="20" r="20" fill="#F6F5F5" />
                  <line y1="-1" x2="20" y2="-1" transform="matrix(0 1 1 0 21.5253 10)" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                  <line x1="10.5253" y1="20" x2="30.5253" y2="20" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                </svg>
              </button>
            </router-link>

            <router-link to="/teacher/exams" class="action-card action-link">
              <h2 class="card-title">My exams</h2>
              <button class="add-button">
                <svg width="41" height="40" viewBox="0 0 41 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="20.709" cy="20" r="20" fill="#F6F5F5" />
                  <line y1="-1" x2="20" y2="-1" transform="matrix(0 1 1 0 21.7091 10)" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                  <line x1="10.7091" y1="20" x2="30.7091" y2="20" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                </svg>
              </button>
            </router-link>
          </section>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../../api';
import NavBar from '../../components/NavBar.vue';

const links = [
  { to: '/teacher/profile', label: 'Profile' },
  { to: '/teacher/exams', label: 'Exams' },
  { to: '/teacher/students', label: 'My Students' }
];

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

    const userRes = await api.users.me();
    user.value = userRes.data;

    const examsRes = await api.teacherExams.list();
    examCount.value = examsRes.data?.length || 0;

    const studentMap = new Set();
    for (const exam of examsRes.data) {
      try {
        const assignmentsRes = await api.teacherExams.assignments(exam.id);
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

function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('userId');
  localStorage.removeItem('userRole');
  router.push('/auth');
}
</script>

<style>
:root {
  --primary-blue: #2563eb;
  --secondary-blue: #3870EC;
  --light-blue: #F0F5FF;
  --text-dark: #1A1A1A;
  --text-gray: #666666;
  --light-gray: #F6F5F5;
  --border-color: #E6E6E6;
  --white: #FFFFFF;
}

body {
  margin: 0;
  font-family: 'Inter', sans-serif;
  background-color: var(--light-blue);
  min-height: 100vh;
}

.container {
  max-width: 1140px;
  margin-left: auto;
  margin-right: auto;
  padding-left: 24px;
  padding-right: 24px;
}

.main-content {
  padding: 40px 0;
}

.dashboard-layout-profile {
  gap: 31px;
}

.profile-card {
  width: 100%;
  max-width: 348px;
  background-color: var(--white);
  border-radius: 12px;
  box-shadow: 0px 4px 24px rgba(0, 0, 0, 0.08);
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-sizing: border-box;
}

.profile-image {
  width: 120px;
  height: 120px;
  margin-bottom: 16px;
  border-radius: 50%;
  background-color: var(--primary-blue);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 36px;
  text-transform: uppercase;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-dark);
  margin: 0;
  text-align: center;
}

.profile-email {
  font-size: 14px;
  color: var(--text-gray);
  margin-top: 8px;
  margin-bottom: 0;
  text-align: center;
  word-break: break-all;
}

.stats-section {
  width: 100%;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

.stats-grid {
  display: flex;
  justify-content: center;
  gap: 40px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-dark);
  margin: 0;
}

.stat-label {
  font-size: 14px;
  color: var(--text-gray);
  margin-top: 4px;
  margin-bottom: 0;
}

.action-cards {
  flex: 1;
  min-width: 0;
}

.action-card {
  background-color: var(--white);
  border-radius: 12px;
  box-shadow: 0px 4px 24px rgba(0, 0, 0, 0.08);
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  text-decoration: none;
}

.action-card.action-link {
  color: inherit;
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0px 6px 28px rgba(0, 0, 0, 0.12);
}

.action-card:last-child {
  margin-bottom: 0;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-dark);
  margin: 0;
}

.add-button {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.add-button:hover {
  transform: scale(1.05);
}

.add-button svg circle {
  transition: fill 0.2s ease;
}

.add-button:hover svg circle {
  fill: #e0e0e0;
}
.dashboard-layout-profile {
  display: flex;
  flex-direction: row;
}
@media (max-width: 992px) {
  .dashboard-layout-profile {
    gap: 20px;
  }

  .stats-grid {
    gap: 30px;
  }
}

@media (max-width: 768px) {
  .dashboard-layout-profile {
    flex-direction: column;
    align-items: center;
  }

  .profile-card {
    max-width: 100%;
  }
}

@media (max-width: 576px) {
  .container {
    padding-left: 16px;
    padding-right: 16px;
  }

  .main-content {
    padding: 20px 0;
  }

  .profile-image {
    width: 100px;
    height: 100px;
    font-size: 30px;
  }

  .profile-name {
    font-size: 18px;
  }

  .stats-grid {
    gap: 20px;
  }

  .stat-number {
    font-size: 20px;
  }

  .action-card {
    padding: 16px;
  }

  .card-title {
    font-size: 16px;
  }

  .add-button svg {
    width: 36px;
    height: 36px;
  }
}

@media (max-width: 400px) {

  .profile-image {
    width: 80px;
    height: 80px;
    font-size: 24px;
  }

  .stats-grid {
    gap: 16px;
  }

  .stat-number {
    font-size: 18px;
  }

  .stat-label {
    font-size: 12px;
  }
}
</style>


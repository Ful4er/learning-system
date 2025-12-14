<template>
  <nav class="nav-bar">
    <div class="container">
      <div class="nav-content">
        <ul class="nav-links">
          <li v-for="link in links" :key="link.to">
            <router-link :to="link.to" class="nav-link" :class="{ active: isActive(link.to) }">{{ link.label }}</router-link>
          </li>
        </ul>
        <div class="nav-actions">
          <button class="notification-btn" @click="onLogout" aria-label="Logout" title="Logout">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></path>
              <polyline points="16 17 21 12 16 7" stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></polyline>
              <line x1="21" y1="12" x2="9" y2="12" stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></line>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import api from '../api';

const route = useRoute();
const router = useRouter();

const props = defineProps({
  links: { type: Array, default: () => [] }
});

function isActive(path) {
  return route.path === path;
}

async function onLogout() {
  try {
    await api.auth.logout().catch(() => {});
  } catch (e) {
    console.error('Logout error:', e);
  } finally {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userRole');
    router.push('/auth');
  }
}
</script>

<style scoped>
.nav-bar {
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
}
.nav-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 20px;
  padding-bottom: 10px;
}
.nav-links {
  display: flex;
  gap: 16px;
  list-style: none;
  margin: 0;
  padding: 0;
  font-size: large;
}
.nav-link {
  color: #333;
  text-decoration: none;
  font-weight: 500;
}
.nav-link.active {
  color: #2563eb;
}
.notification-btn {
  background: transparent;
  border: none;
  cursor: pointer;
}
</style>
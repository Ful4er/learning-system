<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <h1>Learning System</h1>
        <p>{{ isLogin ? 'Sign in to your account' : 'Create a new account' }}</p>
      </div>

      <div v-if="error" class="auth-error-message">
        <span>{{ error }}</span>
      </div>

      <div class="auth-toggle">
        <button
            class="auth-toggle-btn"
            :class="{ active: isLogin }"
            @click="isLogin = true">
          Sign In
        </button>
        <button
            class="auth-toggle-btn"
            :class="{ active: !isLogin }"
            @click="isLogin = false">
          Register
        </button>
      </div>

      <form v-if="isLogin" @submit.prevent="login" class="auth-form">
        <div class="auth-input-group">
          <label for="login-email">Email</label>
          <input
              type="email"
              id="login-email"
              v-model="loginEmail"
              required />
        </div>
        <div class="auth-input-group">
          <label for="login-password">Password</label>
          <input
              type="password"
              id="login-password"
              v-model="loginPassword"
              required />
        </div>
        <button type="submit" class="auth-submit-btn" :disabled="loading">
          Sign In
        </button>
      </form>

      <form v-else @submit.prevent="register" class="auth-form">
        <div class="auth-input-group">
          <label for="register-first-name">First Name</label>
          <input
              type="text"
              id="register-first-name"
              v-model="registerFirstName"
              required />
        </div>
        <div class="auth-input-group">
          <label for="register-last-name">Last Name</label>
          <input
              type="text"
              id="register-last-name"
              v-model="registerLastName"
              required />
        </div>
        <div class="auth-input-group">
          <label for="register-email">Email</label>
          <input
              type="email"
              id="register-email"
              v-model="registerEmail"
              required />
        </div>
        <div class="auth-input-group">
          <label for="register-password">Password</label>
          <input
              type="password"
              id="register-password"
              v-model="registerPassword"
              required
              minlength="6" />
        </div>
        <div class="auth-input-group">
          <label for="register-role">I am a</label>
          <select id="register-role" v-model="registerRole" required>
            <option value="STUDENT">Student</option>
            <option value="TEACHER">Teacher</option>
          </select>
        </div>
        <button type="submit" class="auth-submit-btn" :disabled="loading">
          Create Account
        </button>
      </form>

      <div class="auth-footer">
        <span>{{ isLogin ? "Don't have an account?" : 'Already have an account?' }}</span>
        <a href="#" @click.prevent="isLogin = !isLogin">
          {{ isLogin ? 'Register' : 'Sign in' }}
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();

const isLogin = ref(true);
const loading = ref(false);
const error = ref('');

const loginEmail = ref('');
const loginPassword = ref('');

const registerFirstName = ref('');
const registerLastName = ref('');
const registerEmail = ref('');
const registerPassword = ref('');
const registerRole = ref('STUDENT');

function getRedirectPath(userRole) {
  switch (userRole) {
    case 'TEACHER':
      return '/teacher/profile';
    case 'STUDENT':
      return '/student/profile';
    case 'ADMIN':
      return '/admin/dashboard';
    default:
      return error;
  }
}

async function fetchUserProfile(userId, token) {
  try {
    const response = await axios.get(`/api/users/${userId}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  } catch (e) {
    console.error('Failed to fetch user profile:', e);
    return null;
  }
}

async function login() {
  loading.value = true;
  error.value = '';
  try {
    const res = await axios.post('/api/auth/login', {
      email: loginEmail.value,
      password: loginPassword.value
    });

    if (res.data && res.data.token) {
      const { token, userId, role } = res.data;

      localStorage.setItem('token', token);
      localStorage.setItem('userId', userId);
      if (role) {
        localStorage.setItem('userRole', role);
      }

      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;

      let userRole = role;

      if (!userRole) {
        const userProfile = await fetchUserProfile(userId, token);
        userRole = userProfile?.role;
        if (userRole) {
          localStorage.setItem('userRole', userRole);
        }
      }

      const redirectPath = getRedirectPath(userRole);
      router.push(redirectPath);
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Login failed';
    console.error('Login error:', e);
  } finally {
    loading.value = false;
  }
}

async function register() {
  loading.value = true;
  error.value = '';
  try {
    const res = await axios.post('/api/auth/register', {
      firstName: registerFirstName.value,
      lastName: registerLastName.value,
      email: registerEmail.value,
      password: registerPassword.value,
      role: registerRole.value
    });

    if (res.data && res.data.token) {
      const { token, userId } = res.data;

      localStorage.setItem('token', token);
      localStorage.setItem('userId', userId);
      localStorage.setItem('userRole', registerRole.value);
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;

      const redirectPath = getRedirectPath(registerRole.value);
      router.push(redirectPath);
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Registration failed';
    console.error('Registration error:', e);
  } finally {
    loading.value = false;
  }
}
</script>

<style>
@import '../assets/css/auth.css';
</style>
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
import api from '../api';

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
    const response = await api.users.byId(userId);
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
    const res = await api.auth.login(loginEmail.value, loginPassword.value);

    if (res.data && res.data.token) {
      const { token, userId, role } = res.data;

      localStorage.setItem('token', token);
      localStorage.setItem('userId', userId);
      if (role) {
        localStorage.setItem('userRole', role);
      }

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
    const res = await api.auth.register({
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
:root {
  --primary: #2563eb;
  --primary-hover: #3870EC;
  --bg: #f8fafc;
  --white: #ffffff;
  --text: #1e293b;
  --text-light: #64748b;
  --border: #e2e8f0;
  --error-bg: #fef2f2;
  --error: #dc2626;
  --shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  --radius: 12px;
  --input-radius: 8px;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Inter', sans-serif;
  background-color: var(--bg);
  color: var(--text);
  line-height: 1.5;
}

.auth-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
}

.auth-card {
  background: var(--white);
  width: 100%;
  max-width: 420px;
  padding: 32px;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-header h1 {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 8px;
}

.auth-header p {
  color: var(--text-light);
}

.auth-error-message {
  background: var(--error-bg);
  color: var(--error);
  padding: 12px;
  border-radius: var(--input-radius);
  font-size: 14px;
  margin-bottom: 16px;
}

.auth-toggle {
  display: flex;
  background: #f1f5f9;
  border-radius: var(--input-radius);
  padding: 4px;
  margin-bottom: 24px;
}

.auth-toggle-btn {
  flex: 1;
  padding: 8px 16px;
  border: none;
  background: none;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-light);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.auth-toggle-btn.active {
  background: var(--white);
  color: var(--primary);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-form.hidden {
  display: none;
}

.auth-input-group {
  display: flex;
  flex-direction: column;
}

.auth-input-group label {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.auth-input-group input,
.auth-input-group select {
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--input-radius);
  font-size: 14px;
  transition: all 0.2s;
}

.auth-input-group input:focus,
.auth-input-group select:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.auth-submit-btn {
  width: 100%;
  padding: 10px 16px;
  background: var(--primary);
  color: var(--white);
  border: none;
  border-radius: var(--input-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.auth-submit-btn:hover {
  background: var(--primary-hover);
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: var(--text-light);
}

.auth-footer a {
  color: var(--primary);
  font-weight: 500;
  margin-left: 4px;
  text-decoration: none;
  transition: color 0.2s;
}

.auth-footer a:hover {
  color: var(--primary-hover);
}
</style>
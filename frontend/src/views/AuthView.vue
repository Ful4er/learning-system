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

async function login() {
  loading.value = true;
  error.value = '';
  try {
    const res = await axios.post('/api/auth/login', { 
      email: loginEmail.value, 
      password: loginPassword.value 
    });
    
    if (res.data && res.data.token) {
      localStorage.setItem('token', res.data.token);
      localStorage.setItem('userId', res.data.userId);
      axios.defaults.headers.common['Authorization'] = `Bearer ${res.data.token}`;
      
      // Redirect based on role - you might need to fetch user info
      router.push('/teacher/profile');
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Login failed';
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
      localStorage.setItem('token', res.data.token);
      localStorage.setItem('userId', res.data.userId);
      axios.defaults.headers.common['Authorization'] = `Bearer ${res.data.token}`;
      
      // Redirect based on role
      if (registerRole.value === 'TEACHER') {
        router.push('/teacher/profile');
      } else {
        router.push('/student/profile');
      }
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Registration failed';
  } finally {
    loading.value = false;
  }
}
</script>

<style>
@import '../assets/css/auth.css';
</style>




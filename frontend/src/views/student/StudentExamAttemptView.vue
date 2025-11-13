<template>
  <div>
    <nav class="nav-bar">
      <div class="container">
        <div class="nav-content">
          <ul class="nav-links">
            <li><router-link to="/student/profile" class="nav-link">Dashboard</router-link></li>
            <li><router-link to="/student/exams" class="nav-link">Exams</router-link></li>
          </ul>
          <button class="notification-btn" @click="logout" aria-label="Logout" title="Logout">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></path>
              <polyline points="16 17 21 12 16 7" stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></polyline>
              <line x1="21" y1="12" x2="9" y2="12" stroke="#3870EC" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></line>
            </svg>
          </button>
        </div>
      </div>
    </nav>

    <main class="main-content">
      <div class="container">
        <div v-if="loading" class="empty-state">
          <div class="loading-spinner"></div>
          <p class="empty-state-message">Loading exam...</p>
        </div>

        <div v-else-if="!attemptId || attemptId === '0'" class="empty-state">
          <div class="empty-state-title">No Active Attempt</div>
          <p class="empty-state-message">You need to start the exam first</p>
          <router-link to="/student/exams" class="exam-btn exam-btn-primary" style="display: inline-block; text-decoration: none;">
            Back to Exams
          </router-link>
        </div>

        <div v-else>
          <!-- Exam Header -->
          <div style="background-color: white; border-radius: 12px; box-shadow: 0px 4px 24px rgba(0, 0, 0, 0.08); padding: 24px; margin-bottom: 24px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
              <h2 style="font-size: 24px; font-weight: 600; color: #1A1A1A; margin: 0;">{{ exam?.title || 'Exam' }}</h2>
              <div style="display: flex; gap: 24px;">
                <div style="text-align: center;">
                  <div style="font-size: 14px; color: #666666;">Time Remaining</div>
                  <div style="font-size: 24px; font-weight: 600; color: #2563eb;">{{ timeRemaining }}</div>
                </div>
                <div style="text-align: center;">
                  <div style="font-size: 14px; color: #666666;">Questions</div>
                  <div style="font-size: 24px; font-weight: 600; color: #2563eb;">{{ questions.length }}</div>
                </div>
              </div>
            </div>
            <p v-if="exam?.description" style="color: #666666; margin: 0;">{{ exam.description }}</p>
          </div>

          <!-- Questions Form -->
          <form @submit.prevent="finishAttempt">
            <div class="questions-container">
              <div v-for="(q, index) in questions" :key="q.id" class="question-card">
                <div style="display: flex; gap: 16px; margin-bottom: 16px;">
                  <div style="width: 40px; height: 40px; border-radius: 50%; background-color: #2563eb; color: white; display: flex; align-items: center; justify-content: center; font-weight: 600; flex-shrink: 0;">
                    {{ index + 1 }}
                  </div>
                  <div style="flex: 1;">
                    <h3 style="font-size: 16px; font-weight: 600; color: #1A1A1A; margin: 0 0 12px 0;">{{ q.text }}</h3>

                    <!-- Multiple Choice / Single Choice -->
                    <div v-if="q.type === 'SINGLE' || q.type === 'MULTIPLE'" class="options-group">
                      <label v-for="opt in q.options" :key="opt.id" class="option-label">
                        <input
                          v-if="q.type === 'SINGLE'"
                          type="radio"
                          :name="'q-' + q.id"
                          :value="opt.id"
                          @change="onOptionChange(q.id, opt.id, $event)"
                          :checked="isOptionSelected(q.id, opt.id)"
                        />
                        <input
                          v-else
                          type="checkbox"
                          :name="'q-' + q.id"
                          :value="opt.id"
                          @change="onOptionChange(q.id, opt.id, $event)"
                          :checked="isOptionSelected(q.id, opt.id)"
                        />
                        <span>{{ opt.text }}</span>
                      </label>
                    </div>

                    <!-- Text Answer -->
                    <div v-else-if="q.type === 'TEXT'" class="text-answer-group">
                      <textarea
                        v-model="textAnswers[q.id]"
                        placeholder="Type your answer here..."
                        style="width: 100%; min-height: 100px; padding: 12px; border: 1px solid #E6E6E6; border-radius: 8px; font-family: inherit; font-size: 14px;"
                      ></textarea>
                    </div>
                  </div>
                </div>

                <div style="display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px;">
                  <button
                    type="button"
                    @click="saveAnswer(q.id)"
                    class="exam-btn exam-btn-secondary"
                    style="padding: 10px 16px; font-size: 13px;"
                  >
                    Save Answer
                  </button>
                </div>
              </div>
            </div>

            <!-- Actions -->
            <div style="display: flex; gap: 12px; justify-content: center; margin-top: 32px;">
              <router-link to="/student/exams" class="exam-btn exam-btn-secondary" style="text-decoration: none; display: inline-flex; align-items: center; padding: 12px 24px;">
                Cancel
              </router-link>
              <button
                type="submit"
                class="exam-btn exam-btn-primary"
                style="padding: 12px 24px;"
                :disabled="finishing"
              >
                {{ finishing ? 'Submitting...' : 'Submit Exam' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import api from '../../api';

const route = useRoute();
const router = useRouter();
const examId = route.params.examId;
const attemptId = route.params.attemptId;

const loading = ref(true);
const finishing = ref(false);
const exam = ref(null);
const questions = ref([]);
const attempt = ref(null);
const selectedAnswers = ref({});
const textAnswers = ref({});

const timeRemaining = computed(() => {
  if (!attempt.value?.expiresAt) return 'N/A';
  const now = new Date();
  const expires = new Date(attempt.value.expiresAt);
  const diffMs = expires - now;
  if (diffMs <= 0) return 'Time\'s up!';
  const minutes = Math.floor(diffMs / 60000);
  const seconds = Math.floor((diffMs % 60000) / 1000);
  return `${minutes}m ${seconds}s`;
});

async function logout() {
  try {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userRole');
    delete axios.defaults.headers.common['Authorization'];
    router.push('/auth');
  } catch (error) {
    console.error('Logout error:', error);
    router.push('/auth');
  }
}

function isOptionSelected(questionId, optionId) {
  const selected = selectedAnswers.value[questionId];
  return selected && selected.has(optionId);
}

function onOptionChange(questionId, optionId, event) {
  if (!selectedAnswers.value[questionId]) {
    selectedAnswers.value[questionId] = new Set();
  }
  
  const question = questions.value.find(q => q.id === questionId);
  if (question?.type === 'SINGLE') {
    selectedAnswers.value[questionId].clear();
    if (event.target.checked) {
      selectedAnswers.value[questionId].add(optionId);
    }
  } else {
    if (event.target.checked) {
      selectedAnswers.value[questionId].add(optionId);
    } else {
      selectedAnswers.value[questionId].delete(optionId);
    }
  }
}

async function saveAnswer(questionId) {
  try {
    const selectedOptions = selectedAnswers.value[questionId] 
      ? Array.from(selectedAnswers.value[questionId]) 
      : [];
    const textAnswer = textAnswers.value[questionId] || null;
    
    await api.studentExams.submitAnswer(attemptId, {
      questionId,
      selectedOptionIds: selectedOptions,
      textAnswer
    });
    
  } catch (e) {
    console.error('Failed to save answer:', e);
    alert(`Failed to save answer: ${e.response?.data?.message || e.message}`);
  }
}

async function loadAttempt() {
  loading.value = true;
  try {
    const examRes = await api.studentExams.details(examId);
    exam.value = examRes.data;

    const qRes = await api.studentExams.questions(examId);
    questions.value = qRes.data || [];

    try {
      const attemptRes = await api.studentExams.getAttemptDetails(attemptId);
      attempt.value = attemptRes.data;

      if (attemptRes.data.answers && Array.isArray(attemptRes.data.answers)) {
        attemptRes.data.answers.forEach(answer => {
          if (answer.selectedOptionIds && answer.selectedOptionIds.length > 0) {
            selectedAnswers.value[answer.questionId] = new Set(answer.selectedOptionIds);
          }
          if (answer.textAnswer) {
            textAnswers.value[answer.questionId] = answer.textAnswer;
          }
        });
      }
    } catch (e) {
      console.error('Could not load attempt details:', e);
      attempt.value = null;
    }
  } catch (e) {
    console.error('Failed to load attempt:', e);
    if (e.response?.status === 401) {
      router.push('/auth');
    } else {
      alert('Failed to load exam. Please try again.');
    }
  } finally {
    loading.value = false;
  }
}

async function finishAttempt() {
  if (!confirm('Are you sure you want to submit your exam? You won\'t be able to change your answers.')) {
    return;
  }

  finishing.value = true;
  try {
    await api.studentExams.finishAttempt(attemptId);
    alert('Exam submitted successfully!');
    router.push('/student/profile');
  } catch (e) {
    console.error('Failed to finish attempt:', e);
    alert(`Failed to submit exam: ${e.response?.data?.message || e.message}`);
  } finally {
    finishing.value = false;
  }
}

onMounted(loadAttempt);
</script>

<style scoped>
@import '../../styles/student.css';

.questions-container {
  display: grid;
  gap: 20px;
  margin-bottom: 32px;
}

.question-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0px 2px 12px rgba(0, 0, 0, 0.06);
  padding: 24px;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-label {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #E6E6E6;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.option-label:hover {
  background-color: #F0F5FF;
  border-color: #2563eb;
}

.option-label input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.option-label span {
  font-size: 14px;
  color: #1A1A1A;
}

.text-answer-group {
  margin-top: 8px;
}
</style>

<template>
  <div>
    <NavBar :links="links" />

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
                        @input="onTextChange(q.id)"
                        placeholder="Type your answer here..."
                        style="width: 100%; min-height: 100px; padding: 12px; border: 1px solid #E6E6E6; border-radius: 8px; font-family: inherit; font-size: 14px;"
                      ></textarea>
                      <div style="margin-top:8px; font-size:12px; color:#666;">
                        <span v-if="savingState[q.id] === 'saving'">Saving...</span>
                        <span v-else-if="savingState[q.id] === 'saved'">Saved</span>
                        <span v-else-if="savingState[q.id] === 'error'">Save failed</span>
                      </div>
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
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '../../api';
import NavBar from '../../components/NavBar.vue';

const links = [
  { to: '/student/profile', label: 'Dashboard' },
  { to: '/student/exams', label: 'Exams' }
];

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

// autosave state: per-question saving indicator and debounce timers
const savingState = ref({}); // { [questionId]: 'idle'|'saving'|'saved'|'error' }
const saveTimers = {};
const dirtyQuestions = ref(new Set());

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
    await api.auth.logout().catch(() => {});
  } catch (error) {
    console.error('Logout error (server):', error);
  } finally {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userRole');
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
  // trigger debounced save
  // mark as dirty so we can warn on unload
  dirtyQuestions.value.add(questionId);
  debouncedSaveAnswer(questionId);
}

function onTextChange(questionId) {
  // mark as dirty and trigger debounced save when text changes
  dirtyQuestions.value.add(questionId);
  debouncedSaveAnswer(questionId);
}

async function saveAnswer(questionId) {
  savingState.value[questionId] = 'saving';
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

    savingState.value[questionId] = 'saved';
    // remove dirty mark for this question
    dirtyQuestions.value.delete(questionId);
    // reset to idle after a short delay
    setTimeout(() => {
      if (savingState.value[questionId] === 'saved') savingState.value[questionId] = 'idle';
    }, 1200);
  } catch (e) {
    console.error('Failed to save answer:', e);
    savingState.value[questionId] = 'error';
    // if save failed (e.g. offline), persist draft locally so user doesn't lose changes
    try {
      const draftKey = `attempt_draft_${attemptId}`;
      const payload = {
        selectedAnswers: Object.fromEntries(Object.entries(selectedAnswers.value).map(([k, set]) => [k, Array.from(set)])),
        textAnswers: textAnswers.value
      };
      localStorage.setItem(draftKey, JSON.stringify(payload));
      // leave dirty mark so beforeunload will warn
      dirtyQuestions.value.add(questionId);
    } catch (ee) {}
  }
}

function debouncedSaveAnswer(questionId) {
  if (saveTimers[questionId]) clearTimeout(saveTimers[questionId]);
  savingState.value[questionId] = 'idle';
  saveTimers[questionId] = setTimeout(() => {
    saveAnswer(questionId);
    delete saveTimers[questionId];
  }, 800);
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
      // restore local draft if any (fallback when offline or before saved to server)
      try {
        const draftKey = `attempt_draft_${attemptId}`;
        const draft = localStorage.getItem(draftKey);
        if (draft) {
          const parsed = JSON.parse(draft);
          // merge selectedOptions
          Object.keys(parsed.selectedAnswers || {}).forEach(qid => {
            selectedAnswers.value[qid] = new Set(parsed.selectedAnswers[qid]);
          });
          Object.assign(textAnswers.value, parsed.textAnswers || {});
        }
      } catch (e) {}
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

// Warn user about unsaved changes when trying to close or reload the page
function handleBeforeUnload(e) {
  const hasSaving = Object.values(savingState.value).some(s => s === 'saving');
  const hasError = Object.values(savingState.value).some(s => s === 'error');
  const hasDirty = dirtyQuestions.value && dirtyQuestions.value.size > 0;
  if (hasSaving || hasError || hasDirty) {
    e.preventDefault();
    e.returnValue = '';
    return '';
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload);
});

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload);
});

</script>

<style scoped>
@import '../../assets/css/teacher/profile.css';

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

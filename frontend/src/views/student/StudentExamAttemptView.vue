<template>
  <div class="student-attempt">
    <NavBar :links="links" />

    <main class="main-content">
      <div class="container">
        <div v-if="loading" class="panel loading-panel">
          <div class="loading-spinner"></div>
          <p class="muted">Loading exam...</p>
        </div>

        <div v-else-if="!attemptId || attemptId === '0'" class="panel empty-panel">
          <h2>No active attempt</h2>
          <p class="muted">Start the exam from the assignments page first.</p>
          <router-link to="/student/exams" class="exam-btn exam-btn-primary">Back to exams</router-link>
        </div>

        <div v-else class="attempt-layout">
          <section class="exam-overview">
            <div>
              <p class="eyebrow">{{ exam?.title || 'Exam' }}</p>
              <p class="muted">{{ exam?.description || 'Stay focused and good luck!' }}</p>
            </div>
            <div class="overview-stats">
              <div>
                <span class="label">Time left</span>
                <span class="value">{{ timeRemaining }}</span>
              </div>
              <div>
                <span class="label">Questions</span>
                <span class="value">{{ questions.length }}</span>
              </div>
              <div>
                <span class="label">Progress</span>
                <span class="value">{{ answeredCount }}/{{ questions.length }}</span>
              </div>
            </div>
          </section>

          <form class="attempt-form" @submit.prevent="finishAttempt">
            <div class="questions-container">
              <article
                  v-for="(question, index) in questions"
                  :key="question.id"
                  class="question-card"
              >
                <div class="question-header">
                  <div class="question-number">{{ index + 1 }}</div>
                  <div>
                    <p class="question-text">{{ question.text }}</p>
                    <p class="muted small">
                      {{ formatQuestionType(question.type) }} · {{ question.points }} pts
                    </p>
                  </div>
                </div>

                <div v-if="isChoiceQuestion(question)" class="options-group">
                  <label v-for="option in question.options" :key="option.id" class="option-label">
                    <input
                        :type="question.type === 'SINGLE_CHOICE' ? 'radio' : 'checkbox'"
                        :name="'question-' + question.id"
                        :value="option.id"
                        :checked="isOptionSelected(question.id, option.id)"
                        @change="onOptionChange(question, option.id, $event)"
                    >
                    <span>{{ option.text }}</span>
                  </label>
                </div>

                <div v-else class="text-answer-group">
                  <textarea
                      :value="textAnswers[question.id] || ''"
                      @input="onTextInput(question.id, $event.target.value)"
                      placeholder="Type your answer here..."
                  />
                  <div class="saving-indicator">
                    <span v-if="savingState[question.id] === 'saving'">Saving...</span>
                    <span v-else-if="savingState[question.id] === 'saved'">Saved ✓</span>
                    <span v-else-if="savingState[question.id] === 'error'">Save failed — retry</span>
                  </div>
                </div>

                <div class="question-actions">
                  <button type="button" class="exam-btn exam-btn-secondary ghost" @click="saveAnswer(question.id)">
                    Save answer
                  </button>
                </div>
              </article>
            </div>

            <div class="attempt-footer">
              <router-link to="/student/exams" class="exam-btn exam-btn-secondary">Cancel</router-link>
              <button class="exam-btn exam-btn-primary" :disabled="finishing">
                {{ finishing ? 'Submitting...' : 'Submit exam' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
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
const savingState = ref({});
const dirtyQuestions = ref(new Set());
const now = ref(Date.now());
const saveTimers = {};
let countdownTimer = null;

const timeRemaining = computed(() => {
  if (!attempt.value?.startedAt || !exam.value?.durationMinutes) return '—';
  const start = new Date(attempt.value.startedAt).getTime();
  const durationMs = (exam.value.durationMinutes || 0) * 60000;
  if (durationMs === 0) return '—';
  const endsAt = start + durationMs;
  const diff = endsAt - now.value;
  if (diff <= 0) return "Time's up";
  const minutes = Math.floor(diff / 60000);
  const seconds = String(Math.floor((diff % 60000) / 1000)).padStart(2, '0');
  return `${minutes}m ${seconds}s`;
});

const answeredCount = computed(() => {
  return questions.value.reduce((count, question) => {
    const selections = selectedAnswers.value[question.id] || [];
    const textValue = (textAnswers.value[question.id] || '').trim();
    const answered = selections.length > 0 || !!textValue;
    return answered ? count + 1 : count;
  }, 0);
});

function isChoiceQuestion(question) {
  return question.type === 'SINGLE_CHOICE' || question.type === 'MULTIPLE_CHOICE';
}

function formatQuestionType(type) {
  switch (type) {
    case 'SINGLE_CHOICE':
      return 'Single choice';
    case 'MULTIPLE_CHOICE':
      return 'Multiple choice';
    default:
      return 'Open response';
  }
}

function isOptionSelected(questionId, optionId) {
  return (selectedAnswers.value[questionId] || []).includes(optionId);
}

function markDirty(questionId) {
  dirtyQuestions.value.add(questionId);
  debouncedSaveAnswer(questionId);
}

function onOptionChange(question, optionId, event) {
  const current = selectedAnswers.value[question.id] || [];
  let next = [...current];

  if (question.type === 'SINGLE_CHOICE') {
    next = event.target.checked ? [optionId] : [];
  } else {
    if (event.target.checked) {
      if (!next.includes(optionId)) next.push(optionId);
    } else {
      next = next.filter(id => id !== optionId);
    }
  }

  updateSelectedAnswers(question.id, next);
  markDirty(question.id);
}

function onTextInput(questionId, value) {
  textAnswers.value = {
    ...textAnswers.value,
    [questionId]: value
  };
  markDirty(questionId);
}

function updateSelectedAnswers(questionId, values) {
  selectedAnswers.value = {
    ...selectedAnswers.value,
    [questionId]: values
  };
}

async function saveAnswer(questionId) {
  savingState.value = { ...savingState.value, [questionId]: 'saving' };
  try {
    const selectedOptionIds = selectedAnswers.value[questionId] || [];
    const textAnswer = (textAnswers.value[questionId] || '').trim() || null;

    await api.studentExams.submitAnswer(attemptId, {
      questionId,
      selectedOptionIds,
      textAnswer
    });

    savingState.value = { ...savingState.value, [questionId]: 'saved' };
    dirtyQuestions.value.delete(questionId);
    setTimeout(() => {
      if (savingState.value[questionId] === 'saved') {
        savingState.value = { ...savingState.value, [questionId]: 'idle' };
      }
    }, 1200);
  } catch (error) {
    console.error('Failed to save answer', error);
    savingState.value = { ...savingState.value, [questionId]: 'error' };
    persistDraft();
  }
}

function debouncedSaveAnswer(questionId) {
  if (saveTimers[questionId]) clearTimeout(saveTimers[questionId]);
  saveTimers[questionId] = setTimeout(() => {
    saveAnswer(questionId);
    delete saveTimers[questionId];
  }, 800);
}

function persistDraft() {
  try {
    const draftKey = `attempt_draft_${attemptId}`;
    const payload = {
      selectedAnswers: selectedAnswers.value,
      textAnswers: textAnswers.value
    };
    localStorage.setItem(draftKey, JSON.stringify(payload));
  } catch (error) {
    console.warn('Failed to persist draft', error);
  }
}

async function loadAttempt() {
  loading.value = true;
  try {
    const [examRes, questionsRes, attemptRes] = await Promise.all([
      api.studentExams.details(examId),
      api.studentExams.questions(examId),
      api.studentExams.getAttemptDetails(attemptId)
    ]);

    exam.value = examRes.data;
    questions.value = questionsRes.data || [];
    attempt.value = attemptRes.data;

    if (Array.isArray(attemptRes.data?.answers)) {
      const choiceMap = {};
      const textMap = {};
      attemptRes.data.answers.forEach(answer => {
        if (answer.selectedOptionIds?.length) {
          choiceMap[answer.questionId] = [...answer.selectedOptionIds];
        }
        if (answer.textAnswer) {
          textMap[answer.questionId] = answer.textAnswer;
        }
      });
      selectedAnswers.value = choiceMap;
      textAnswers.value = textMap;
    }

    try {
      const draftKey = `attempt_draft_${attemptId}`;
      const draft = localStorage.getItem(draftKey);
      if (draft) {
        const parsed = JSON.parse(draft);
        selectedAnswers.value = { ...selectedAnswers.value, ...(parsed.selectedAnswers || {}) };
        textAnswers.value = { ...textAnswers.value, ...(parsed.textAnswers || {}) };
      }
    } catch (error) {
      console.warn('Failed to restore draft', error);
    }
  } catch (error) {
    console.error('Failed to load attempt', error);
    if (error.response?.status === 401) {
      router.push('/auth');
    } else {
      alert(error.response?.data?.message || 'Failed to load exam');
    }
  } finally {
    loading.value = false;
  }
}

async function finishAttempt() {
  if (!confirm('Submit your answers? You will not be able to edit afterwards.')) {
    return;
  }
  finishing.value = true;
  try {
    await api.studentExams.finishAttempt(attemptId);
    localStorage.removeItem(`attempt_draft_${attemptId}`);
    router.push('/student/profile');
  } catch (error) {
    console.error('Failed to submit exam', error);
    alert(error.response?.data?.message || 'Submission failed');
  } finally {
    finishing.value = false;
  }
}

function handleBeforeUnload(e) {
  const hasSaving = Object.values(savingState.value).some(state => state === 'saving');
  const hasDirty = dirtyQuestions.value.size > 0;
  if (hasSaving || hasDirty) {
    e.preventDefault();
    e.returnValue = '';
    return '';
  }
}

function startCountdown() {
  countdownTimer = setInterval(() => {
    now.value = Date.now();
  }, 1000);
}

onMounted(() => {
  loadAttempt();
  window.addEventListener('beforeunload', handleBeforeUnload);
  startCountdown();
});

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload);
  if (countdownTimer) clearInterval(countdownTimer);
  Object.values(saveTimers).forEach(timer => clearTimeout(timer));
});
</script>

<style scoped>
.student-attempt {
  min-height: 100vh;
  background: var(--light-blue);
}

.main-content {
  padding: 32px 0 48px;
}

.panel {
  background: white;
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.muted {
  color: var(--text-gray);
}

.attempt-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.exam-overview {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.eyebrow {
  font-size: 18px;
  margin: 0;
  font-weight: 600;
  color: var(--text-dark);
}

.overview-stats {
  display: flex;
  gap: 24px;
}

.overview-stats .label {
  display: block;
  font-size: 12px;
  color: var(--text-gray);
}

.overview-stats .value {
  display: block;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-dark);
}

.attempt-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.questions-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
}

.question-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.question-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary-blue);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.question-text {
  margin: 0;
  font-weight: 600;
  color: var(--text-dark);
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
  border-radius: 10px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.option-label:hover {
  border-color: var(--primary-blue);
  background: var(--light-blue);
}

.text-answer-group textarea {
  width: 100%;
  min-height: 120px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 12px;
  font-family: inherit;
  font-size: 14px;
  resize: vertical;
}

.saving-indicator {
  font-size: 12px;
  color: var(--text-gray);
  margin-top: 6px;
}

.question-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.attempt-footer {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
}

.ghost {
  background: transparent;
  color: var(--text-dark);
}

.exam-btn-primary,
.exam-btn-secondary {
  border-radius: 999px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
}

.exam-btn-primary {
  background: var(--primary-blue);
  color: #ffffff;
  border: none;
}

.exam-btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.exam-btn-secondary {
  background: #ffffff;
  color: var(--text-dark);
  border: 1px solid var(--border-color);
}

.exam-btn-secondary:hover {
  background: #f3f4ff;
}

@media (max-width: 768px) {
  .exam-overview {
    flex-direction: column;
    align-items: flex-start;
  }

  .overview-stats {
    width: 100%;
    justify-content: space-between;
  }

  .attempt-footer {
    flex-direction: column;
  }
}
</style>
<template>
  <div class="teacher-exams">
    <NavBar :links="links" />

    <main class="main-content">
      <div class="container">
        <div class="dashboard-layout-exams">
          <div class="header-section">
            <h2 class="card-title">My Exams</h2>
            <button @click="showExamModal = true" class="add-button" aria-label="Add new exam">
              <svg width="41" height="40" viewBox="0 0 41 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="20.5251" cy="20" r="20" fill="#F6F5F5" />
                <line y1="-1" x2="20" y2="-1" transform="matrix(0 1 1 0 21.5253 10)" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
                <line x1="10.5253" y1="20" x2="30.5253" y2="20" stroke="#2563EB" stroke-opacity="0.7" stroke-width="2"/>
              </svg>
            </button>
          </div>

          <!-- Create/Edit Exam Modal -->
          <div v-if="showExamModal" class="modal" @click.self="closeCreateModal">
            <div class="modal-content wide-modal">
              <span class="close" @click="closeCreateModal">&times;</span>
              <h2 class="modal-title">{{ isEditingExam ? 'Edit Exam' : 'Add New Exam' }}</h2>

              <div class="modal-body-grid">
                <section class="exam-form-section" aria-label="Exam basic settings">
                  <div v-if="createError" class="message error">{{ createError }}</div>
                  <form @submit.prevent="createExam">
                    <div class="form-group">
                      <label for="title">Title</label>
                      <input type="text" id="title" v-model="newExam.title" required>
                    </div>
                    <div class="form-group">
                      <label for="description">Description</label>
                      <textarea id="description" v-model="newExam.description" rows="3" placeholder="Short exam summary"></textarea>
                    </div>
                    <div class="form-group two-columns">
                      <div>
                        <label for="duration">Duration (minutes)</label>
                        <input type="number" id="duration" v-model.number="newExam.durationMinutes" required min="1">
                      </div>
                      <div>
                        <label for="passingScore">Passing Score (%)</label>
                        <input type="number" id="passingScore" v-model.number="newExam.passingScore" required min="0" max="100">
                      </div>
                    </div>
                    <div class="form-actions">
                      <button type="submit" class="submit-btn primary">{{ isEditingExam ? 'Update Exam' : 'Create Exam' }}</button>
                      <button type="button" class="submit-btn secondary" @click="closeCreateModal">Cancel</button>
                    </div>
                    <button
                        v-if="isEditingExam"
                        type="button"
                        class="danger-btn"
                        @click="confirmDeleteExam"
                    >
                      Delete exam
                    </button>
                  </form>
                </section>

                <section v-if="isEditingExam" class="question-manager" aria-label="Exam questions">
                  <div class="question-manager-header">
                    <div>
                      <h3>Questions ({{ examQuestions.length }})</h3>
                      <p class="muted">Edit, reorder and curate exam content</p>
                    </div>
                    <button class="submit-btn tertiary" type="button" @click="startAddQuestion">
                      + Add question
                    </button>
                  </div>

                  <div v-if="questionsLoading" class="muted info-block">Loading questions...</div>
                  <div v-else>
                    <div v-if="questionError" class="message error">{{ questionError }}</div>
                    <div v-if="examQuestions.length === 0" class="muted info-block">
                      No questions yet — add your first one.
                    </div>
                    <ul class="question-list" v-else>
                      <li v-for="question in examQuestions" :key="question.id" class="question-item">
                        <div>
                          <p class="question-text">{{ question.text }}</p>
                          <div class="question-meta">
                            <span>{{ formatQuestionType(question.type) }}</span>
                            <span>Points: {{ question.points }}</span>
                            <span v-if="question.options?.length">Options: {{ question.options.length }}</span>
                          </div>
                        </div>
                        <div class="question-actions">
                          <button type="button" class="link-btn" @click="startEditQuestion(question)">Edit</button>
                          <button type="button" class="link-btn danger" @click="deleteQuestion(question.id)">Delete</button>
                        </div>
                      </li>
                    </ul>
                  </div>

                  <div v-if="showQuestionForm" class="question-form">
                    <h4>{{ editingQuestionId ? 'Update question' : 'Add question' }}</h4>
                    <form @submit.prevent="saveQuestion">
                      <div class="form-group">
                        <label>Question text</label>
                        <textarea v-model="questionForm.text" rows="3" required placeholder="E.g. What is the capital of France?"></textarea>
                      </div>
                      <div class="form-group two-columns">
                        <div>
                          <label>Type</label>
                          <select v-model="questionForm.type">
                            <option value="SINGLE_CHOICE">Single choice</option>
                            <option value="MULTIPLE_CHOICE">Multiple choice</option>
                            <option value="TEXT">Open text</option>
                          </select>
                        </div>
                        <div>
                          <label>Points</label>
                          <input type="number" v-model.number="questionForm.points" min="1" max="100">
                        </div>
                      </div>

                      <div v-if="questionForm.type !== 'TEXT'" class="options-builder">
                        <div class="options-header">
                          <h5>Options</h5>
                          <button type="button" class="link-btn" @click="addOption">+ Option</button>
                        </div>
                        <div v-if="questionForm.options.length === 0" class="muted info-block">
                          Add at least one option.
                        </div>
                        <div v-for="(option, index) in questionForm.options" :key="index" class="option-row">
                          <input
                              type="text"
                              v-model="option.text"
                              :placeholder="`Option ${index + 1}`"
                              required
                          >
                          <label class="checkbox">
                            <input type="checkbox" v-model="option.isCorrect">
                            Correct
                          </label>
                          <button type="button" class="icon-btn" @click="removeOption(index)" aria-label="Remove option">&times;</button>
                        </div>
                      </div>

                      <div class="form-actions">
                        <button type="submit" class="submit-btn primary">
                          {{ editingQuestionId ? 'Save changes' : 'Save question' }}
                        </button>
                        <button type="button" class="submit-btn secondary" @click="cancelQuestionForm">Cancel</button>
                      </div>
                    </form>
                  </div>
                </section>
              </div>
            </div>
          </div>

          <!-- Exam Details Modal -->
          <div v-if="selectedExamDetails" class="modal" @click.self="closeExamDetails">
            <div class="modal-content wide-modal">
              <span class="close" @click="closeExamDetails">&times;</span>
              <div class="exam-details">
                <div class="exam-header">
                  <h2>{{ selectedExamDetails.title }}</h2>
                  <p class="exam-description">{{ selectedExamDetails.description || 'No description provided' }}</p>
                </div>

                <div class="exam-info-section">
                  <h3>Exam Information</h3>
                  <div class="info-grid">
                    <div class="info-item">
                      <span class="info-label">Duration:</span>
                      <span class="info-value">{{ selectedExamDetails.durationMinutes }} minutes</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">Passing Score:</span>
                      <span class="info-value">{{ selectedExamDetails.passingScore }}%</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">Total Students:</span>
                      <span class="info-value">{{ studentAssignments.length }}</span>
                    </div>
                  </div>
                </div>

                <div class="exam-students-section">
                  <h3>Enrolled Students</h3>
                  <div class="add-student-section">
                    <h4>Add Student by Email</h4>
                    <div class="add-student-form">
                      <div class="email-input-wrapper">
                        <input
                            type="email"
                            v-model="studentEmail"
                            @input="onEmailInput"
                            @focus="showSearchResults = true"
                            @blur="hideSearchResults"
                            placeholder="Enter student's email"
                            class="email-input">
                        <div v-if="showSearchResults && searchResults.length > 0" class="search-dropdown">
                          <div v-for="student in searchResults" :key="student.id"
                               class="search-result-item"
                               @mousedown="selectStudentFromSearch(student)">
                            <strong>{{ student.firstName }} {{ student.lastName }}</strong>
                            <small>{{ student.email }}</small>
                          </div>
                        </div>
                      </div>
                      <button @click="addStudentToExam" class="add-student-btn" :disabled="!studentEmail">Add Student</button>
                    </div>
                    <div v-if="addStudentMessage" :class="['message', addStudentMessageType]">
                      {{ addStudentMessage }}
                    </div>
                  </div>

                  <div class="students-table-container">
                    <table class="results-table">
                      <thead>
                      <tr>
                        <th>First Name</th>
                        <th>Last Name</th>
                        <th>Email</th>
                        <th>Status</th>
                        <th>Score</th>
                        <th>Completed</th>
                        <th>Actions</th>
                      </tr>
                      </thead>
                      <tbody>
                      <tr v-if="studentAssignments.length === 0">
                        <td colspan="7" class="no-data">No students enrolled yet</td>
                      </tr>
                      <tr v-for="assignment in studentAssignments" :key="assignment.id">
                        <td class="name-cell">
                          {{ assignment.studentFirstName || 'N/A' }}
                        </td>
                        <td class="name-cell">
                          {{ assignment.studentLastName || 'N/A' }}
                        </td>
                        <td class="email-cell">
                          {{ assignment.studentEmail || 'N/A' }}
                        </td>
                        <td>
                    <span :class="['status', assignmentStatus(assignment)]">
                      {{ assignmentStatusLabel(assignment) }}
                    </span>
                        </td>
                        <td class="score-cell">{{ assignment.score || '-' }}</td>
                        <td class="date-cell">{{ assignment.completedAt ? formatDate(assignment.completedAt) : '-' }}</td>
                        <td>
                          <button class="delete-btn" @click="removeStudent(assignment.studentId)" aria-label="Remove student">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                              <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                            </svg>
                          </button>
                        </td>
                      </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="exams-grid" v-if="exams.length > 0">
            <div
                v-for="exam in exams"
                :key="exam.id"
                class="exam-card-wrapper"
                @click="showExamDetails(exam.id)"
            >
              <div class="exam-card-status">
                <span class="status-pill" :class="exam.status?.toLowerCase()">{{ examStatusLabel(exam.status) }}</span>
                <span v-if="exam.status !== 'PUBLISHED'" class="status-hint">Publish to make it visible for students</span>
              </div>
              <ExamCard
                  :exam="{
                    ...exam,
                    // teacher identity is obvious here, no need to show name block
                    questionCount: exam.questionCount || 0
                  }"
              >
                <template #actions>
                  <button @click.stop="editExam(exam)" class="exam-btn edit-btn">Edit</button>
                    <button
                        v-if="exam.status !== 'PUBLISHED'"
                        @click.stop="publishExam(exam)"
                        class="exam-btn publish-btn"
                        :disabled="actionLoadingId === exam.id"
                    >
                      {{ actionLoadingId === exam.id ? 'Publishing...' : 'Publish' }}
                    </button>
                    <button
                        v-else
                        @click.stop="archiveExam(exam)"
                        class="exam-btn archive-btn"
                        :disabled="actionLoadingId === exam.id"
                    >
                      {{ actionLoadingId === exam.id ? 'Archiving...' : 'Archive' }}
                    </button>
                </template>
              </ExamCard>
            </div>
          </div>

          <div v-else class="empty-state">
            <div class="empty-state-icon">📝</div>
            <h3 class="empty-state-title">No Exams Yet</h3>
            <p class="empty-state-message">Create your first exam to get started</p>
            <button @click="showExamModal = true" class="submit-btn primary">Create Exam</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../../api';
import NavBar from '../../components/NavBar.vue';
import ExamCard from '../../components/ExamCard.vue';

function debounce(func, delay) {
  let timeoutId;
  return function(...args) {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func.apply(this, args), delay);
  };
}

const links = [
  { to: '/teacher/profile', label: 'Profile' },
  { to: '/teacher/exams', label: 'Exams' },
  { to: '/teacher/students', label: 'My Students' }
];

const router = useRouter();

const exams = ref([]);
const showExamModal = ref(false);
const selectedExamDetails = ref(null);
const studentAssignments = ref([]);
const studentEmail = ref('');
const addStudentMessage = ref('');
const addStudentMessageType = ref('');
const isEditingExam = ref(false);
const editingExamId = ref(null);
const searchResults = ref([]);
const showSearchResults = ref(false);
const examQuestions = ref([]);
const questionsLoading = ref(false);
const questionError = ref('');
const showQuestionForm = ref(false);
const editingQuestionId = ref(null);
const questionForm = ref(getDefaultQuestionForm());
const actionLoadingId = ref(null);
function examStatusLabel(status) {
  switch (status) {
    case 'PUBLISHED':
      return 'Published';
    case 'ARCHIVED':
      return 'Archived';
    default:
      return 'Draft';
  }
}

async function publishExam(exam) {
  if (!exam?.id) return;
  if (!confirm('Publish this exam so that assigned students can see it?')) return;
  actionLoadingId.value = exam.id;
  try {
    await api.teacherExams.publish(exam.id);
    await fetchExams();
  } catch (error) {
    console.error('Failed to publish exam:', error);
    alert(error.response?.data?.message || 'Unable to publish exam');
  } finally {
    actionLoadingId.value = null;
  }
}

async function archiveExam(exam) {
  if (!exam?.id) return;
  if (!confirm('Archive this exam? Students will no longer see it.')) return;
  actionLoadingId.value = exam.id;
  try {
    await api.teacherExams.archive(exam.id);
    await fetchExams();
  } catch (error) {
    console.error('Failed to archive exam:', error);
    alert(error.response?.data?.message || 'Unable to archive exam');
  } finally {
    actionLoadingId.value = null;
  }
}


const newExam = ref({
  title: '',
  description: '',
  durationMinutes: 60,
  passingScore: 60
});

const createError = ref('');

const debouncedSearchStudents = debounce(async (email) => {
  if (!email || email.length < 3) {
    searchResults.value = [];
    return;
  }

  try {
    const response = await api.usersSearch.byEmail(email);
    const data = response.data || [];
    if (Array.isArray(data)) {
      searchResults.value = data.filter(u => u.role === 'STUDENT');
    } else {
      searchResults.value = [];
    }
  } catch (error) {
    console.error('Error searching students:', error);
    searchResults.value = [];
  }
}, 400);

async function fetchExams() {
  try {
    const res = await api.teacherExams.list();
    exams.value = res.data || [];

    // Fetch assignment counts for each exam
    for (let exam of exams.value) {
      try {
        const assignments = await api.teacherExams.assignments(exam.id);
        exam.assignedStudentCount = assignments.data?.length || 0;
      } catch (e) {
        exam.assignedStudentCount = 0;
      }
    }
  } catch (error) {
    console.error('Failed to fetch exams:', error);
    if (error.response?.status === 401) {
      router.push('/auth');
    }
  }
}

async function createExam() {
  createError.value = '';

  if (!newExam.value.title?.trim()) {
    createError.value = 'Please provide a title';
    return;
  }
  if (newExam.value.durationMinutes <= 0) {
    createError.value = 'Duration must be greater than 0';
    return;
  }
  if (newExam.value.passingScore < 0 || newExam.value.passingScore > 100) {
    createError.value = 'Passing score must be between 0 and 100';
    return;
  }

  try {
    const payload = {
      title: newExam.value.title.trim(),
      description: newExam.value.description?.trim() || '',
      durationMinutes: newExam.value.durationMinutes,
      passingScore: newExam.value.passingScore
    };

    if (isEditingExam.value && editingExamId.value) {
      await api.teacherExams.update(editingExamId.value, payload);
    } else {
      await api.teacherExams.create(payload);
    }

    await fetchExams();
    closeCreateModal();
  } catch (error) {
    console.error('Failed to create/update exam:', error);
    createError.value = error.response?.data?.message || 'Failed to create/update exam';
  }
}

async function showExamDetails(examId) {
  try {
    const res = await api.teacherExams.details(examId);
    selectedExamDetails.value = res.data;

    const assignmentsRes = await api.teacherExams.assignments(examId);
    studentAssignments.value = assignmentsRes.data || [];
  } catch (error) {
    console.error('Failed to fetch exam details:', error);
  }
}

function assignmentStatus(assignment) {
  if (!assignment || !assignment.completedAt) return 'pending';
  const passing = selectedExamDetails.value?.passingScore;
  if (passing == null || assignment.score == null) return 'completed';
  return assignment.score >= passing ? 'passed' : 'failed';
}

function assignmentStatusLabel(assignment) {
  const s = assignmentStatus(assignment);
  switch (s) {
    case 'passed': return 'Passed';
    case 'failed': return 'Failed';
    case 'completed': return 'Completed';
    default: return 'Pending';
  }
}

function onEmailInput(e) {
  const email = e.target.value.trim();
  debouncedSearchStudents(sanitizeEmailString(email));
}

function sanitizeEmailString(email) {
  if (!email) return '';
  return email.replace(/[\u200B\uFEFF\u2060]/g, '').trim();
}

function hideSearchResults() {
  setTimeout(() => {
    showSearchResults.value = false;
  }, 200);
}

function selectStudentFromSearch(student) {
  studentEmail.value = sanitizeEmailString(student.email);
  searchResults.value = [];
  showSearchResults.value = false;
}

async function addStudentToExam() {
  if (!studentEmail.value || !selectedExamDetails.value) return;

  addStudentMessage.value = '';
  addStudentMessageType.value = 'error';

  try {
    const cleanEmail = sanitizeEmailString(studentEmail.value);
    const userRes = await api.usersSearch.byEmail(cleanEmail);
    let found = null;

    if (Array.isArray(userRes.data)) {
      found = userRes.data.find(u => u.email?.toLowerCase() === studentEmail.value.toLowerCase());
    } else {
      found = userRes.data;
    }

    if (!found || !found.email) {
      addStudentMessage.value = 'Student not found';
      return;
    }

    // Quick client-side check to avoid calling server if already assigned
    const alreadyAssigned = (studentAssignments.value || []).some(a => (a.studentEmail || '').toLowerCase() === (found.email || '').toLowerCase());
    if (alreadyAssigned) {
      addStudentMessage.value = 'Student is already assigned to this exam';
      addStudentMessageType.value = 'error';
      return;
    }

    await api.teacherExams.assign(selectedExamDetails.value.id, [sanitizeEmailString(found.email)]);

    addStudentMessageType.value = 'success';
    addStudentMessage.value = 'Student added successfully';
    studentEmail.value = '';
    searchResults.value = [];

    await showExamDetails(selectedExamDetails.value.id);
  } catch (error) {
    console.error('Failed to add student:', error);
    const msg = error.response?.data?.message || 'Failed to add student';
    if (error.response?.status === 404) {
      addStudentMessage.value = 'Student not found';
    } else if (error.response?.status === 409) {
      addStudentMessage.value = 'Student is already assigned';
    } else {
      addStudentMessage.value = msg;
    }
  }
}

async function removeStudent(studentId) {
  if (!confirm('Are you sure you want to remove this student?')) return;

  try {
    if (!selectedExamDetails.value) return;
    await api.teacherExams.removeAssignment(selectedExamDetails.value.id, studentId);
    await showExamDetails(selectedExamDetails.value.id);
  } catch (error) {
    console.error('Failed to remove student:', error);
    alert('Failed to remove student');
  }
}

function formatDate(dateString) {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString();
}

function getInitials(name) {
  if (!name) return '??';
  return name.split(' ').map(n => n.charAt(0)).join('').toUpperCase().substring(0, 2);
}

async function editExam(exam) {
  isEditingExam.value = true;
  editingExamId.value = exam.id;
  newExam.value = {
    title: exam.title,
    description: exam.description,
    durationMinutes: exam.durationMinutes,
    passingScore: exam.passingScore
  };
  await fetchExamQuestions(exam.id);
  showExamModal.value = true;
}

function closeCreateModal() {
  showExamModal.value = false;
  isEditingExam.value = false;
  editingExamId.value = null;
  newExam.value = { title: '', description: '', durationMinutes: 60, passingScore: 60 };
  createError.value = '';
  examQuestions.value = [];
  questionsLoading.value = false;
  questionError.value = '';
  showQuestionForm.value = false;
  editingQuestionId.value = null;
  questionForm.value = getDefaultQuestionForm();
}

onMounted(fetchExams);

function getDefaultQuestionForm() {
  return {
    text: '',
    type: 'SINGLE_CHOICE',
    points: 1,
    options: [
      { text: '', isCorrect: true },
      { text: '', isCorrect: false }
    ]
  };
}

async function fetchExamQuestions(examId) {
  questionsLoading.value = true;
  questionError.value = '';
  try {
    const res = await api.teacherExams.getExamQuestions(examId);
    examQuestions.value = Array.isArray(res.data) ? res.data : [];
  } catch (error) {
    console.error('Failed to load exam questions', error);
    questionError.value = error.response?.data?.message || 'Unable to load questions';
  } finally {
    questionsLoading.value = false;
  }
}

function startAddQuestion() {
  questionForm.value = getDefaultQuestionForm();
  editingQuestionId.value = null;
  showQuestionForm.value = true;
  questionError.value = '';
}

function startEditQuestion(question) {
  editingQuestionId.value = question.id;
  questionForm.value = {
    text: question.text,
    type: question.type,
    points: question.points,
    options: (question.options || []).map(opt => ({
      text: opt.text,
      isCorrect: Boolean(opt.isCorrect)
    }))
  };
  showQuestionForm.value = true;
  questionError.value = '';
}

function cancelQuestionForm() {
  showQuestionForm.value = false;
  questionError.value = '';
  editingQuestionId.value = null;
  questionForm.value = getDefaultQuestionForm();
}

function addOption() {
  questionForm.value.options.push({ text: '', isCorrect: false });
}

function removeOption(index) {
  questionForm.value.options.splice(index, 1);
}

async function saveQuestion() {
  if (!editingExamId.value) return;
  questionError.value = '';

  const payload = {
    text: (questionForm.value.text || '').trim(),
    type: questionForm.value.type,
    points: questionForm.value.points || 1,
    options: questionForm.value.type === 'TEXT'
        ? []
        : questionForm.value.options.map((option, index) => ({
          text: option.text,
          isCorrect: Boolean(option.isCorrect),
          orderIndex: index
        }))
  };

  if (!payload.text) {
    questionError.value = 'Question text is required';
    return;
  }
  if (payload.type !== 'TEXT') {
    if (payload.options.length === 0) {
      questionError.value = 'Add at least one option for choice questions';
      return;
    }
    if (!payload.options.some(opt => opt.isCorrect)) {
      questionError.value = 'Mark at least one option as correct';
      return;
    }
  }

  try {
    if (editingQuestionId.value) {
      await api.teacherExams.updateQuestion(editingQuestionId.value, payload);
    } else {
      await api.teacherExams.addQuestion(editingExamId.value, payload);
    }
    await fetchExamQuestions(editingExamId.value);
    cancelQuestionForm();
  } catch (error) {
    console.error('Failed to save question', error);
    questionError.value = error.response?.data?.message || 'Unable to save question';
  }
}

async function deleteQuestion(questionId) {
  if (!editingExamId.value || !confirm('Delete this question?')) return;
  try {
    await api.teacherExams.deleteQuestion(questionId);
    await fetchExamQuestions(editingExamId.value);
  } catch (error) {
    console.error('Failed to delete question', error);
    alert(error.response?.data?.message || 'Unable to delete question');
  }
}

function formatQuestionType(type) {
  switch (type) {
    case 'SINGLE_CHOICE':
      return 'Single choice';
    case 'MULTIPLE_CHOICE':
      return 'Multiple choice';
    case 'TEXT':
    default:
      return 'Open text';
  }
}

async function confirmDeleteExam() {
  if (!editingExamId.value || !confirm('Delete this exam? This cannot be undone.')) return;
  try {
    await api.teacherExams.delete(editingExamId.value);
    closeCreateModal();
    await fetchExams();
  } catch (error) {
    console.error('Failed to delete exam', error);
    createError.value = error.response?.data?.message || 'Unable to delete exam';
  }
}

function closeExamDetails() {
  selectedExamDetails.value = null;
  studentAssignments.value = [];
  studentEmail.value = '';
  addStudentMessage.value = '';
  addStudentMessageType.value = '';
}
</script>

<style scoped>
.teacher-exams {
  min-height: 100vh;
  background-color: var(--light-blue);
}

.main-content {
  padding: 32px 0 48px;
}

  .dashboard-layout-exams {
    background: white;
    border-radius: 12px;
    padding: 2rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    display: flex;
    flex-direction: column;
  }

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--border-color);
  flex-wrap: nowrap;
  gap: 12px;
}

.card-title {
  margin: 0;
  color: var(--text-dark);
  font-size: 24px;
  font-weight: 600;
  flex: 1;
  min-width: 0;
}

.add-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.add-button:hover {
  background-color: var(--light-gray);
}

/* Exams Grid */
.exams-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.name-cell {
  font-weight: 500;
  color: var(--text-dark);
}

.email-cell {
  color: var(--text-gray);
  font-size: 13px;
}

.muted {
  color: var(--text-gray);
  font-size: 14px;
}
/* Exam Details Styles */
.exam-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.exam-description {
  color: #666;
  margin: 0;
  line-height: 1.5;
  font-size: 14px;
}

.exam-info-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.exam-info-section h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: var(--text-dark);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.info-label {
  color: #666;
  font-weight: 500;
  font-size: 14px;
}

.info-value {
  color: #333;
  font-weight: 600;
  font-size: 14px;
}

.exam-students-section h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: var(--text-dark);
}

.exam-students-section {
  margin-top: 32px;
}

.add-student-section {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.add-student-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #374151;
}

/* Table styles for student details */
.name-cell {
  font-weight: 500;
  color: var(--text-dark);
  font-size: 14px;
}

.email-cell {
  color: var(--text-gray);
  font-size: 13px;
}

.score-cell {
  font-weight: 600;
  text-align: center;
}

.date-cell {
  font-size: 13px;
  color: var(--text-gray);
}

/* Responsive table */
@media (max-width: 768px) {
  .exam-info-section {
    padding: 12px;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .info-item {
    padding: 10px;
  }

  .modal-body-grid {
    grid-template-columns: 1fr;
  }

  .name-cell,
  .email-cell {
    font-size: 13px;
  }
}

.exam-btn {
  flex: 1;
  padding: 8px 16px;
  border: 1px solid;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.edit-btn {
  background: white;
  border-color: var(--primary-blue);
  color: var(--primary-blue);
}

.edit-btn:hover {
  background: var(--primary-blue);
  color: white;
}

.view-btn {
  background: white;
  border-color: var(--text-gray);
  color: var(--text-gray);
}

.view-btn:hover {
  background: var(--text-gray);
  color: white;
}

/* Modal Styles */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 100%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
}

.wide-modal {
  max-width: 900px;
}

.close {
  position: absolute;
  top: 16px;
  right: 16px;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-gray);
  background: none;
  border: none;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close:hover {
  color: var(--text-dark);
}

/* Form Styles */
.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: var(--text-dark);
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--primary-blue);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.submit-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  flex: 1;
}

.submit-btn.primary {
  background: var(--primary-blue);
  color: white;
}

.submit-btn.primary:hover {
  background: var(--secondary-blue);
}

.submit-btn.secondary {
  background: var(--light-gray);
  color: var(--text-dark);
}

.submit-btn.secondary:hover {
  background: #e0e0e0;
}

.submit-btn.tertiary {
  background: #eef2ff;
  color: var(--primary-blue);
}

.submit-btn.tertiary:hover {
  background: #dbe2ff;
}

.danger-btn {
  width: 100%;
  margin-top: 16px;
  padding: 10px 16px;
  background: #fee2e2;
  color: #b91c1c;
  border: 1px solid #fecaca;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.danger-btn:hover {
  background: #fecaca;
}

.modal-title {
  margin: 0 0 12px 0;
  font-size: 24px;
}

.modal-body-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.exam-form-section,
.question-manager {
  background: #f9fafb;
  padding: 18px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.question-manager-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.question-list {
  list-style: none;
  margin: 16px 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-item {
  background: white;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.question-text {
  margin: 0 0 8px 0;
  font-weight: 600;
  color: #111827;
}

.question-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: var(--text-gray);
  flex-wrap: wrap;
}

.question-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.link-btn {
  background: none;
  border: none;
  color: var(--primary-blue);
  cursor: pointer;
  font-weight: 600;
}

.link-btn.danger {
  color: #dc2626;
}

.question-form {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.form-group.two-columns {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.options-builder {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.options-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.option-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.option-row input[type="text"] {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-dark);
}

.icon-btn {
  background: none;
  border: none;
  color: #dc2626;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  padding: 2px 6px;
}

.info-block {
  background: white;
  border-radius: 8px;
  padding: 12px;
  border: 1px dashed var(--border-color);
  margin-top: 12px;
}

.exam-card-wrapper {
  height: 100%;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-card-wrapper:hover {
  transform: translateY(-2px);
}

.exam-card-wrapper :deep(.exam-card) {
  height: 100%;
}

.exam-card-status {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 4px;
}

.status-pill {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4338ca;
  text-transform: capitalize;
}

.status-pill.published {
  background: #ecfdf5;
  color: #15803d;
}

.status-pill.archived {
  background: #fef3c7;
  color: #92400e;
}

.status-hint {
  font-size: 12px;
  color: var(--text-gray);
}

.publish-btn {
  border-color: #16a34a;
  color: #16a34a;
}

.publish-btn:hover {
  background: #16a34a;
  color: white;
}

.archive-btn {
  border-color: #b91c1c;
  color: #b91c1c;
}

.archive-btn:hover {
  background: #b91c1c;
  color: white;
}
/* Student Search */
.email-input-wrapper {
  position: relative;
  flex: 1;
}

.email-input {
  width: 100%;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.email-input:focus {
  outline: none;
  border-color: var(--primary-blue);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid var(--border-color);
  border-top: none;
  border-radius: 0 0 6px 6px;
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.search-result-item {
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--light-gray);
  transition: background-color 0.2s;
}

.search-result-item:hover {
  background-color: var(--light-blue);
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item strong {
  display: block;
  font-size: 14px;
  margin-bottom: 2px;
}

.search-result-item small {
  font-size: 12px;
  color: var(--text-gray);
}

.add-student-form {
  display: flex;
  gap: 12px;
  margin: 16px 0;
  flex-wrap: wrap;
}

.add-student-btn {
  padding: 10px 20px;
  background: var(--primary-blue);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}

.add-student-btn:disabled {
  background: var(--text-gray);
  cursor: not-allowed;
}

.add-student-btn:not(:disabled):hover {
  background: var(--secondary-blue);
}

/* Students Table */
.students-table-container {
  overflow-x: auto;
  margin-top: 20px;
  padding-bottom: 12px;
}

.results-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.results-table th {
  background: var(--light-gray);
  padding: 12px;
  text-align: left;
  font-weight: 600;
  color: var(--text-dark);
  border-bottom: 1px solid var(--border-color);
}

.results-table td {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  vertical-align: middle;
}

.results-table tr:hover {
  background-color: var(--light-blue);
}

.no-data {
  text-align: center;
  color: var(--text-gray);
  font-style: italic;
  padding: 40px !important;
}

.status {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status.completed,
.status.passed {
  background: #dcfce7;
  color: #166534;
}

.status.failed {
  background: #fee2e2;
  color: #b91c1c;
}

.status.pending {
  background: #fef3c7;
  color: #92400e;
}

.delete-btn {
  background: none;
  border: none;
  padding: 6px;
  cursor: pointer;
  color: #dc2626;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.delete-btn:hover {
  background: #fee2e2;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-gray);
}

.empty-state-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-dark);
}

.empty-state-message {
  margin-bottom: 24px;
}

/* Message Styles */
.message {
  padding: 12px;
  border-radius: 6px;
  margin: 12px 0;
  font-size: 14px;
}

.message.success {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.message.error {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

/* Responsive Design */
@media (max-width: 768px) {
  .container {
    padding-left: 16px;
    padding-right: 16px;
  }

  .dashboard-layout-exams {
    padding: 16px;
  }

  .header-section {
    flex-direction: row;
    align-items: center;
    flex-wrap: wrap;
  }

  .card-title {
    font-size: 20px;
  }

  .exams-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .card-title {
    font-size: 20px;
  }

  .modal {
    padding: 10px;
  }

  .modal-content {
    padding: 20px;
  }

  .wide-modal {
    max-width: 100%;
  }

  .form-actions {
    flex-direction: column;
  }

  .add-student-form {
    flex-direction: column;
  }

  .students-table-container {
    font-size: 12px;
  }

  .results-table th,
  .results-table td {
    padding: 8px;
  }
}

@media (max-width: 480px) {
  .dashboard-layout-exams{
    padding: 12px;
  }

  .modal-content {
    padding: 16px;
  }

  .results-table {
    font-size: 12px;
  }

  .results-table th,
  .results-table td {
    padding: 6px 4px;
  }

  .status {
    font-size: 11px;
    padding: 3px 6px;
  }
}

/* Print Styles */
@media print {
  .nav-bar,
  .add-button,
  .delete-btn {
    display: none !important;
  }

  .dashboard-layout-exams {
    box-shadow: none;
    border: 1px solid #ccc;
  }
}
</style>
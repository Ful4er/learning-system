<template>
  <div class="exam-card">
    <div class="exam-card-header">
        <h3 class="exam-card-title">{{ exam.title }}</h3>
        <div v-if="status" class="exam-status-badge">
          <span class="badge" :class="status">{{ statusLabel(status) }}</span>
        </div>
      </div>

    <p v-if="exam.description" class="exam-card-description">{{ exam.description }}</p>

    <!-- Teacher Info (if provided) -->
    <div v-if="exam.teacherFirstName || exam.teacherLastName" class="exam-teacher-info">
      <div class="info-row">
        <span class="info-label">Teacher:</span>
        <span class="info-value">{{ exam.teacherFirstName }} {{ exam.teacherLastName }}</span>
      </div>
      <div v-if="exam.teacherEmail" class="info-row">
        <span class="info-label">Email:</span>
        <span class="info-value">{{ exam.teacherEmail }}</span>
      </div>
    </div>

    <div class="exam-meta">
      <div class="exam-meta-item">⏱️ {{ exam.durationMinutes }} min</div>
      <div class="exam-meta-item">✓ {{ exam.questionCount || 0 }} questions</div>
    </div>

    <div class="exam-actions">
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  exam: { type: Object, required: true },
  status: { type: String, required: false },
  lastScore: { type: [Number, null], required: false }
});

function statusLabel(s) {
  switch (s) {
    case 'in-progress': return 'In progress';
    case 'passed': return 'Passed';
    case 'failed': return 'Failed';
    case 'completed': return 'Completed';
    default: return 'Ready';
  }
}
</script>

<style scoped>
.exam-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0px 2px 12px rgba(0,0,0,0.06);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.exam-card-header {
  display: flex;
  align-items: flex-start;
}
.exam-card-title {
  font-size: 16px;
  margin: 0;
  font-weight: 600;
}
.exam-status-badge {
  margin-left: auto;
  display: flex;
  align-items: center;
}
.exam-status-badge .badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 999px;
}
.exam-status-badge .badge.passed,
.exam-status-badge .badge.completed {
  background: #ecfdf5;
  color: #15803d;
}
.exam-status-badge .badge.failed {
  background: #fee2e2;
  color: #b91c1c;
}
.exam-status-badge .badge.in-progress {
  background: #fff7ed;
  color: #c2410c;
}
.exam-card-description {
  color: #666666;
  margin: 0;
  font-size: 14px;
}
.exam-teacher-info {
  background: #f8f9fa;
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 13px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0;
}
.info-row:last-child {
  margin-bottom: 0;
}
.info-label {
  color: #666666;
  font-weight: 500;
}
.info-value {
  color: #333333;
  font-weight: 600;
}
.exam-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #333333;
}
.exam-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .exam-card { padding: 14px; }
}
</style>
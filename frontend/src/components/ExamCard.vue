<template>
  <div class="exam-card">
    <div class="exam-card-header">
      <h3 class="exam-card-title">{{ exam.title }}</h3>
      <span v-if="exam.statusLabel" class="exam-status-pill" :class="exam.statusClass">{{ exam.statusLabel }}</span>
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
  exam: { type: Object, required: true }
});
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
  align-items: center;
  justify-content: space-between;
}
.exam-card-title {
  font-size: 16px;
  margin: 0;
  font-weight: 600;
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
  margin-top: 8px;
}
.exam-status-pill {
  margin-left: 16px;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  background: #f0f4fa;
  color: #2563eb;
  min-width: 90px;
  text-align: center;
}
.exam-status-pill.completed {
  background: #e6f7e6;
  color: #22c55e;
}
.exam-status-pill.pending {
  background: #fff4e6;
  color: #f59e42;
}
@media (max-width: 900px) {
  .exam-card { padding: 14px; }
}
</style>
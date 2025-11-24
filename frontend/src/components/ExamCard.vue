<template>
  <div class="exam-card">
    <div class="exam-card-header">
      <div class="exam-icon">
        <slot name="icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
        </slot>
      </div>
      <div style="flex:1;">
        <h3 class="exam-card-title">{{ exam.title }}</h3>
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
  gap: 12px;
  align-items: center;
}
.exam-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F3F6FF;
  border-radius: 10px;
  flex-shrink: 0;
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
}

@media (max-width: 900px) {
  .exam-card { padding: 14px; }
}
</style>
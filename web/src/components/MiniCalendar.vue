<template>
  <div class="glass-card mini-calendar">
    <div class="calendar-head">
      <button type="button" aria-label="上个月" @click="changeMonth(-1)">‹</button>
      <h3>{{ title }}</h3>
      <button type="button" aria-label="下个月" @click="changeMonth(1)">›</button>
    </div>
    <div class="calendar-grid calendar-weekdays" aria-hidden="true">
      <span v-for="weekday in weekDays" :key="weekday">{{ weekday }}</span>
    </div>
    <div class="calendar-grid">
      <span v-for="day in days" :key="day.key" :class="{ blank: !day.value, today: day.isToday }">
        {{ day.value }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const weekDays = ['日', '一', '二', '三', '四', '五', '六']
const current = ref(new Date())

const title = computed(() => `${current.value.getFullYear()} 年 ${current.value.getMonth() + 1} 月`)

const days = computed(() => {
  const year = current.value.getFullYear()
  const month = current.value.getMonth()
  const firstWeekday = new Date(year, month, 1).getDay()
  const dayCount = new Date(year, month + 1, 0).getDate()
  const now = new Date()
  const result = Array.from({ length: firstWeekday }, (_, index) => ({
    key: `blank-${index}`,
    value: '',
    isToday: false,
  }))
  for (let day = 1; day <= dayCount; day += 1) {
    result.push({
      key: `${year}-${month}-${day}`,
      value: day,
      isToday: year === now.getFullYear() && month === now.getMonth() && day === now.getDate(),
    })
  }
  return result
})

const changeMonth = (offset) => {
  current.value = new Date(current.value.getFullYear(), current.value.getMonth() + offset, 1)
}
</script>

<style scoped>
.mini-calendar {
  padding: 18px;
}

.calendar-head {
  margin-bottom: 12px;
  display: grid;
  grid-template-columns: 32px 1fr 32px;
  align-items: center;
  gap: 5px;
}

.calendar-head h3 {
  margin: 0;
  color: var(--text-strong);
  text-align: center;
  font-size: 15px;
}

.calendar-head button {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 9px;
  color: var(--text-muted);
  background: transparent;
  cursor: pointer;
  font-size: 22px;
}

.calendar-head button:hover {
  color: var(--theme-pink);
  background: var(--accent-soft);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 3px;
}

.calendar-grid span {
  aspect-ratio: 1;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: var(--text-muted);
  font-size: 12px;
}

.calendar-weekdays span {
  aspect-ratio: auto;
  margin-bottom: 3px;
  color: var(--text-faint);
  font-size: 11px;
}

.calendar-grid span.today {
  color: white;
  background: var(--theme-pink);
  font-weight: 800;
}

.calendar-grid span.blank {
  visibility: hidden;
}
</style>

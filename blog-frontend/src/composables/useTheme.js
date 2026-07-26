import { computed, ref } from 'vue'

const STORAGE_KEY = 'yukiss-theme'
const allowedModes = new Set(['system', 'light', 'dark'])
const storedMode = localStorage.getItem(STORAGE_KEY)
const themeMode = ref(allowedModes.has(storedMode) ? storedMode : 'system')
const systemDark = ref(window.matchMedia?.('(prefers-color-scheme: dark)').matches || false)

const effectiveTheme = computed(() => {
  if (themeMode.value === 'system') {
    return systemDark.value ? 'dark' : 'light'
  }
  return themeMode.value
})

function applyTheme() {
  document.documentElement.dataset.theme = effectiveTheme.value
  document.documentElement.style.colorScheme = effectiveTheme.value
}

const mediaQuery = window.matchMedia?.('(prefers-color-scheme: dark)')
mediaQuery?.addEventListener('change', event => {
  systemDark.value = event.matches
  applyTheme()
})

applyTheme()

export function useTheme() {
  const setTheme = (mode) => {
    if (!allowedModes.has(mode)) return
    themeMode.value = mode
    localStorage.setItem(STORAGE_KEY, mode)
    applyTheme()
  }

  return {
    themeMode,
    effectiveTheme,
    setTheme,
  }
}

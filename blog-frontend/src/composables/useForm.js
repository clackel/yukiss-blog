import { ref } from 'vue'

export function useForm() {
  const showForm = ref(false)
  const showDialog = ref(false)
  const newArticle = ref({ title: '', content: '' })
  const resetForm = () => {
    newArticle.value = { title: '', content: '' }
    showForm.value = false
    showDialog.value = false
  }
  return { showForm, showDialog, newArticle, resetForm }
}

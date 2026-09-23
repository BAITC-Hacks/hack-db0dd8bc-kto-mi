export const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
export const statusLabels = {DRAFT:'Черновик',QUESTIONS:'Вопросы ИИ',CARD:'Карточка',RATED:'Оценён',PUBLISHED:'Опубликован',PENDING:'Ожидает решения',ACCEPTED:'Принят',REJECTED:'Отклонён'};
export const stageFor = business => ({DRAFT:'draft',QUESTIONS:'questions',CARD:'card',RATED:'rating',PUBLISHED:'published'}[business.status] || 'draft');
export function ratingLevel(score) {
  if (score >= 80) return {tone:'high',label:'Высокий',description:'Предложение раскрыто подробно'};
  if (score >= 60) return {tone:'medium',label:'Хороший',description:'Есть основа для обсуждения'};
  return {tone:'low',label:'Требует уточнения',description:'Добавьте детали предложения'};
}
export const formatDate = value => new Intl.DateTimeFormat('ru-RU',{day:'numeric',month:'short'}).format(new Date(value));

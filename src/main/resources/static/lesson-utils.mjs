export function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, character => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[character]));
}

export function highlightedText(text, keywords = []) {
  const words = [...new Set(keywords.filter(Boolean))].sort((a, b) => b.length - a.length);
  if (!words.length) return escapeHtml(text);
  const pattern = words.map(word => word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|');
  const expression = new RegExp(pattern, 'giu');
  let result = '', offset = 0;
  for (const match of text.matchAll(expression)) {
    result += escapeHtml(text.slice(offset, match.index)) + '<mark>' + escapeHtml(match[0]) + '</mark>';
    offset = match.index + match[0].length;
  }
  return result + escapeHtml(text.slice(offset));
}

export function readingSteps(content, settings) {
  const chunks = [];
  const labels = {FIRST: 'Сначала', THEN: 'Затем', FINALLY: 'В завершение'};
  // Keep source order, including unexpected or missing section values.
  for (const sentence of content.sentences) {
    const section = settings.showSequenceStructure ? sentence.section : null;
    const previous = chunks.at(-1);
    if (!previous || previous.section !== section || previous.sentences.length >= 4) {
      chunks.push({kind: 'read', section, title: labels[section] || 'Разбираемся в теме', sentences: []});
    }
    chunks.at(-1).sentences.push(sentence);
  }
  return [...chunks, {kind: 'cards', title: 'Ключевые понятия'}, ...content.quiz.map((question, index) => ({kind: 'quiz', title: `Вопрос ${index + 1}`, question}))];
}

export function safeImageUrl(value) {
  if (!value) return null;
  try {
    const url = new URL(value);
    return url.protocol === 'https:' ? url.href : null;
  } catch { return null; }
}

export function createLearningState(id, lesson) {
  return {
    id, lesson, steps: readingSteps(lesson.content, lesson.displaySettings),
    index: 0, completed: new Set(), answers: new Map(), checked: new Set(),
    size: lesson.displaySettings.fontSizePx, finished: false,
  };
}

export function selectAnswer(state, option) {
  const question = state.steps[state.index].question;
  if (!question || !Number.isInteger(option) || option < 0 || option >= question.options.length) return;
  state.answers.set(state.index, option);
  state.checked.delete(state.index);
  state.completed.delete(state.index);
}

export function checkAnswer(state) {
  if (state.steps[state.index].kind !== 'quiz' || !state.answers.has(state.index)) return false;
  state.checked.add(state.index);
  return true;
}

export function advanceStep(state) {
  if (state.steps[state.index].kind === 'quiz' && !state.checked.has(state.index)) return false;
  state.completed.add(state.index);
  if (state.index < state.steps.length - 1) state.index++;
  else if (state.completed.size === state.steps.length) state.finished = true;
  else state.index = state.steps.findIndex((_, index) => !state.completed.has(index));
  return true;
}

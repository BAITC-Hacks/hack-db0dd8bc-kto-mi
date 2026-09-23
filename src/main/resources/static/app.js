import {escapeHtml as e, highlightedText, safeImageUrl, createLearningState, selectAnswer, checkAnswer, advanceStep} from './lesson-utils.mjs';

const icons = {
  arrow: '<path d="M5 12h14m-5-5 5 5-5 5"/>', back: '<path d="M19 12H5m5-5-5 5 5 5"/>',
  sparkle: '<path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3Z"/><path d="m20 2 .5 1.5L22 4l-1.5.5L20 6l-.5-1.5L18 4l1.5-.5Z"/>',
  book: '<path d="M12 5v15M3 4c4-1 7 0 9 2 2-2 5-3 9-2v15c-4-1-7 0-9 2-2-2-5-3-9-2Z"/>',
  check: '<path d="m5 12 4 4L19 6"/>', shield: '<path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z"/><path d="m8 11 3 3 5-5"/>',
  steps: '<path d="M3 20h6v-6h6V8h6V3"/>', text: '<path d="M4 5h16M8 5v15m8-15v15M5 20h6m2 0h6"/>',
  structure: '<rect x="4" y="3" width="16" height="5" rx="1"/><rect x="4" y="11" width="7" height="9" rx="1"/><path d="M15 12h5m-5 4h5m-5 4h5"/>',
  cards: '<rect x="3" y="6" width="14" height="15" rx="2"/><path d="M7 3h12a2 2 0 0 1 2 2v12M7 12h6m-6 4h4"/>',
  heart: '<path d="M20 5c-3-3-7-1-8 1-1-2-5-4-8-1-4 5 4 11 8 15 4-4 12-10 8-15Z"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6m0-10v.1"/>',
  leaf: '<path d="M20 3C9 2 3 7 5 14c2 7 14 4 15-11Z"/><path d="M3 21 15 9"/>',
  refresh: '<path d="M20 8A8 8 0 1 0 21 14M20 3v5h-5"/>', edit: '<path d="m16 3 5 5L9 20l-6 1 1-6L16 3Z"/>',
  flag: '<path d="M5 21V3c6-4 8 4 15 0v10c-7 4-9-4-15 0"/>'
};
const icon = name => `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true">${icons[name] || icons.book}</svg>`;
const main = document.querySelector('#main');
let profiles = [], routeToken = 0, currentLesson = null, learning = null;
let formDraft = {title: '', text: '', profile: ''};
let toastTimer;

async function api(path, {method = 'GET', body, signal} = {}) {
  let response;
  try { response = await fetch(`/api${path}`, {method, signal, headers: body ? {'Content-Type': 'application/json'} : {}, body: body ? JSON.stringify(body) : undefined}); }
  catch (error) { if (error.name === 'AbortError') throw error; throw new Error('Не удалось связаться с Qadam. Проверьте соединение и попробуйте ещё раз.'); }
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.message || 'Не удалось выполнить действие. Попробуйте ещё раз.');
  return data;
}
function toast(message) { const box = document.querySelector('#toast'); box.textContent = message; box.hidden = false; clearTimeout(toastTimer); toastTimer = setTimeout(() => box.hidden = true, 4500); }
function errorBox(message) { return `<div class="notice error" role="alert">${e(message)}</div>`; }
function showError(error, target = '#action-error') { const box = document.querySelector(target); if (box) { box.innerHTML = errorBox(error.message); box.scrollIntoView({block:'nearest'}); } else toast(error.message); }
function setBusy(button, busy, label) { button.disabled = busy; button.innerHTML = busy ? 'Подождите…' : label; }
function profileDescription(profile) {
  const settings = profile.displaySettings;
  if (settings.showSequenceStructure) return 'Последовательные шаги, конкретные примеры и ясные инструкции.';
  if (settings.dyslexiaFont) return 'Короткие предложения, выделение главного и комфортное чтение.';
  return 'Структура и оформление материала с учётом выбранного способа восприятия.';
}
function howMarkup() { return `<section class="how-section" id="how"><div class="container"><div class="section-heading"><div><span class="eyebrow">От материала к пониманию</span><h2>Три шага. Свой путь.</h2></div><span class="quiet-label">Вы задаёте темп</span></div><div class="how-grid"><div class="how-item"><span class="step-index">01</span><h3>Добавьте материал</h3><p>Текст из учебника, конспект или объяснение. Начните с того, что хотите изучить.</p></div><div class="how-item"><span class="step-index">02</span><h3>Выберите способ подачи</h3><p>Qadam подготовит объяснение, карточки понятий и вопросы. Проверьте, что всё важное сохранено.</p></div><div class="how-item"><span class="step-index">03</span><h3>Изучайте в своём темпе</h3><p>Двигайтесь небольшими шагами, возвращайтесь к понятиям и проверяйте понимание.</p></div></div></div></section>`; }
function homeMarkup() {
  return `<div class="container"><section class="hero"><div class="hero-copy"><div class="eyebrow">${icon('sparkle')}Обучение, в котором есть место каждому</div><h1>Один материал.<br><span>Разные пути<br>к пониманию.</span></h1><p>Qadam адаптирует учебный материал под способ восприятия ученика, сохраняя знания и учебную цель.</p><div class="button-row"><a class="button" href="#create">Адаптировать материал ${icon('arrow')}</a><a class="button text" href="#how">Как это работает ${icon('back').replace('class="icon"','class="icon" style="transform:rotate(-90deg)"')}</a></div><div class="hero-footnote">${icon('heart')}В своём темпе. С уверенностью в своих возможностях.</div></div>
  <div class="hero-art" role="img" aria-label="Один учебный материал превращается в последовательные шаги к пониманию"><div class="art-orbit"></div><svg class="art-path" viewBox="0 0 210 195" fill="none" aria-hidden="true"><path d="M0 10C155-10 25 180 210 145" stroke="#9bb08e" stroke-width="2" stroke-dasharray="5 6"/></svg><div class="art-card source-card"><div class="art-label">${icon('book')}Исходный материал</div><h3>Круговорот воды<br>в природе</h3><div class="fake-line"></div><div class="fake-line"></div><div class="fake-line"></div><div class="fake-line"></div><div class="fake-line"></div><div class="fake-line"></div></div><div class="agent-orb">${icon('sparkle')}</div><div class="art-card adapted-card"><div class="art-label"><span class="status-dot"></span>Мой путь к пониманию</div><h3>Как вода путешествует?</h3><div class="art-step"><span class="step-dot">1</span><span>Солнце нагревает воду.</span></div><div class="art-step"><span class="step-dot">2</span><span>Вода превращается в пар.</span></div><div class="art-step"><span class="step-dot">3</span><span>Пар поднимается вверх.</span></div></div><div class="art-chip">${icon('check')}Те же знания. Удобный путь.</div><span class="art-star" aria-hidden="true">✳</span></div></section>
  <div class="principles"><div class="principle">${icon('book')}<div><strong>Сохраняем суть</strong><span>Знания и учебная цель — в центре</span></div></div><div class="principle">${icon('steps')}<div><strong>Учитываем способ восприятия</strong><span>Структура, темп и понятная подача</span></div></div><div class="principle">${icon('heart')}<div><strong>Поддерживаем самостоятельность</strong><span>Понятно, что делать на каждом шаге</span></div></div></div>
  <section class="workspace" id="create"><div class="section-heading"><div><span class="eyebrow">Начнём с вашего материала</span><h2>Новый путь к знаниям</h2><p>Добавьте текст, а Qadam поможет найти подходящую форму.</p></div><span class="quiet-label">${icon('shield')}Без оценок и сравнений</span></div><div class="creation-grid"><form class="form-panel" id="adapt-form"><h3 class="form-heading"><span class="number-pill">1</span>Что будем изучать?</h3><div class="field"><label for="lesson-title">Название урока</label><input id="lesson-title" name="title" placeholder="Например, круговорот воды в природе" minlength="3" maxlength="120" required value="${e(formDraft.title)}"></div><div class="field"><label for="lesson-text">Учебный материал</label><textarea id="lesson-text" name="text" placeholder="Вставьте текст, который хотите изучить…" minlength="50" maxlength="5000" required aria-describedby="text-hint text-count">${e(formDraft.text)}</textarea><div class="field-meta"><span id="text-hint">От 50 до 5 000 символов</span><span id="text-count">${formDraft.text.length} / 5 000</span></div><button class="inline-button" id="use-example" type="button" style="margin-top:12px">Попробовать на примере: круговорот воды</button></div><div class="divider"></div><fieldset class="profiles"><legend><span class="number-pill">2</span>Как удобнее воспринимать материал?</legend><div class="profile-grid" id="profile-options"><p class="profile-hint">Загружаем доступные профили…</p></div><p class="profile-hint">Профиль определяет подачу материала. Выберите подходящий вам формат.</p></fieldset><div id="form-error"></div><div class="form-submit"><span>Следующий шаг — проверить материал<br>и перейти к изучению.</span><button class="button" id="adapt-button" type="submit" disabled>${icon('sparkle')}Адаптировать материал ${icon('arrow')}</button></div><p class="loading-note" id="adapt-loading" role="status" hidden>Qadam готовит объяснение, карточки и вопросы. Это может занять до двух минут.</p></form>
  <aside><div class="side-panel">${icon('leaf')}<h3>Знания остаются.<br>Подача меняется.</h3><div class="benefit">${icon('text')}<div><strong>Понятная структура</strong><p>Небольшие смысловые блоки и выделенные ключевые слова.</p></div></div><div class="benefit">${icon('cards')}<div><strong>Опора на понятия</strong><p>Карточки с объяснениями и доступными пиктограммами.</p></div></div><div class="benefit">${icon('check')}<div><strong>Проверка понимания</strong><p>Вопросы по теме, чтобы закрепить изученное.</p></div></div><div class="side-quote">Не меньше знаний.<br><span>Другой путь к пониманию.</span></div></div><div class="sidebar-note">${icon('info')}<span>Материал может добавить сам ученик, учитель, родитель или наставник.</span></div></aside></div></section></div>${howMarkup()}`;
}
async function loadProfiles() {
  if (!profiles.length) profiles = await api('/profiles');
}
async function setupHome(token) {
  const form = document.querySelector('#adapt-form');
  form.addEventListener('input', () => { formDraft.title = form.elements.title.value; formDraft.text = form.elements.text.value; formDraft.profile = form.elements.profile?.value || ''; document.querySelector('#text-count').textContent = `${formDraft.text.length} / 5 000`; });
  document.querySelector('#use-example').onclick = async () => {
    const button = document.querySelector('#use-example'); button.disabled = true;
    try {
      const lessons = await api('/lessons');
      const sample = lessons.find(item => item.title === 'Круговорот воды в природе');
      const lesson = sample ? await api(`/lessons/${sample.id}`) : null;
      if (token !== routeToken) return;
      form.elements.title.value = lesson?.title || 'Круговорот воды в природе';
      form.elements.text.value = lesson?.originalText || 'Вода на нашей планете находится в постоянном движении. Под действием солнечного тепла вода испаряется с поверхности морей, рек и озёр, превращаясь в водяной пар. В атмосфере пар охлаждается и конденсируется: образуются капельки воды и кристаллики льда, из которых состоят облака. Когда капли становятся тяжёлыми, они выпадают в виде дождя, снега или града. Вода попадает в реки, моря и почву. Затем всё повторяется. Этот процесс называется круговоротом воды в природе.';
      form.dispatchEvent(new Event('input')); toast('Пример добавлен. Теперь выберите формат.');
    } catch (error) { showError(error, '#form-error'); }
    finally { button.disabled = false; }
  };
  form.onsubmit = async event => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const button = document.querySelector('#adapt-button'), label = button.innerHTML;
    document.querySelector('#form-error').innerHTML = '';
    const title = form.elements.title.value.trim(), text = form.elements.text.value.trim(), profile = form.elements.profile.value;
    if (title.length < 3 || text.length < 50) return showError(new Error('Введите название от 3 символов и материал от 50 символов без учёта пробелов по краям.'), '#form-error');
    setBusy(button, true); form.setAttribute('aria-busy', 'true'); form.querySelectorAll('input,textarea').forEach(input => input.disabled = true); document.querySelector('#adapt-loading').hidden = false;
    try {
      const lesson = await api('/lessons', {method:'POST', body:{title, text, profile}});
      if (token !== routeToken) return;
      currentLesson = lesson; location.hash = `review/${lesson.id}`;
    } catch (error) { if (token === routeToken) showError(error, '#form-error'); }
    finally { if (token === routeToken) { setBusy(button, false, label); form.removeAttribute('aria-busy'); form.querySelectorAll('input,textarea').forEach(input => input.disabled = false); document.querySelector('#adapt-loading').hidden = true; } }
  };
  try {
    await loadProfiles(); if (token !== routeToken) return;
    document.querySelector('#profile-options').innerHTML = profiles.map(profile => `<label class="profile-option"><input type="radio" name="profile" value="${e(profile.code)}" ${profile.code === formDraft.profile ? 'checked' : ''} required><div class="profile-card">${icon(profile.displaySettings.showSequenceStructure ? 'structure' : 'text')}<h3>${e(profile.name)}</h3><p>${e(profileDescription(profile))}</p></div></label>`).join('');
    document.querySelector('#adapt-button').disabled = !profiles.length;
  } catch (error) { showError(error, '#form-error'); document.querySelector('#profile-options').innerHTML = '<button type="button" class="button secondary small" id="retry-config">Повторить загрузку профилей</button>'; document.querySelector('#retry-config').onclick = () => route(); }
}
async function library(token) {
  const lessons = await api('/lessons'); if (token !== routeToken) return;
  main.innerHTML = `<div class="container page"><div class="section-heading"><div><span class="eyebrow">Учиться в своём темпе</span><h1>Мои материалы</h1><p>Возвращайтесь к изученному или начните новый урок.</p></div><a class="button" href="#create">Новый материал ${icon('arrow')}</a></div><p class="profile-hint">Материалы этого пространства доступны всем, кто им пользуется. В текущей конфигурации они хранятся до перезапуска сервера.</p>${lessons.length ? `<div class="library-grid">${lessons.map(lesson => `<a href="#${lesson.status === 'APPROVED' ? 'learn' : 'review'}/${lesson.id}" class="lesson-card"><span class="badge ${lesson.status === 'DRAFT' ? 'draft' : ''}">${icon(lesson.status === 'DRAFT' ? 'edit' : 'book')}${lesson.status === 'DRAFT' ? 'Ожидает проверки' : 'Готов к изучению'}</span><h2>${e(lesson.title)}</h2><p>${e(profiles.find(p => p.code === lesson.profile)?.name || lesson.profileName || '')}</p><div class="card-bottom"><span>${lesson.status === 'DRAFT' ? 'Проверить материал' : 'Начать урок'}</span>${icon('arrow')}</div></a>`).join('')}</div>` : '<div class="empty-state"><h2>Первый шаг — ваш материал</h2><p>Добавьте текст, чтобы подготовить первый урок.</p><a class="button" href="#create">Адаптировать материал</a></div>'}</div>`;
}
function reviewContent(lesson) {
  let previous;
  return lesson.content.sentences.map(sentence => {
    const heading = sentence.section && previous !== sentence.section ? `<h3 class="preview-section">${e({FIRST:'Сначала',THEN:'Затем',FINALLY:'В завершение'}[sentence.section] || '')}</h3>` : ''; previous = sentence.section;
    return `${heading}<p class="preview-sentence">${highlightedText(sentence.text, sentence.keywords)}</p>`;
  }).join('') + `<div class="divider"></div><h2>Ключевые понятия</h2>${lesson.content.cards.map(card => `<p class="preview-sentence"><strong>${e(card.word)}</strong> — ${e(card.explanation)}</p>`).join('')}<div class="divider"></div><h2>Проверка понимания</h2><ol class="review-questions">${lesson.content.quiz.map(question => `<li>${e(question.question)}<small>Ответ: ${e(question.options[question.correctIndex])}</small><span>${question.options.map(e).join(' · ')}</span></li>`).join('')}</ol>`;
}
async function review(id, token) {
  const lesson = await api(`/lessons/${id}`); if (token !== routeToken) return;
  currentLesson = lesson;
  main.innerHTML = `<div class="container page"><a class="back-link" href="#library">${icon('back')}К материалам</a><div class="review-header"><div><h1>${e(lesson.title)}</h1><span class="badge">${e(lesson.profileName)}</span></div><div class="button-row"><button class="button secondary small" id="edit-content">${icon('edit')}Редактировать</button><button class="button secondary small" id="regenerate">${icon('refresh')}Адаптировать заново</button></div></div><p class="page-description">Проверьте, что объяснение передаёт исходный смысл, а все важные понятия сохранены.</p><div id="action-error"></div><div class="review-grid"><section class="content-panel"><h2>Исходный материал</h2><p class="original-text">${e(lesson.originalText)}</p></section><section class="content-panel"><h2>Ваш будущий урок</h2><div id="review-content">${reviewContent(lesson)}</div></section></div>${lesson.status !== 'APPROVED' ? '<label class="review-check"><input type="checkbox" id="reviewed"><span>Я проверил(а) объяснение, понятия и ответы. Учебная цель и основные знания сохранены.</span></label>' : ''}<div class="button-row"><button class="button" id="approve" ${lesson.status !== 'APPROVED' ? 'disabled' : ''}>${lesson.status === 'APPROVED' ? 'Начать урок' : 'Подтвердить и начать урок'} ${icon('arrow')}</button></div></div>`;
  document.querySelector('#reviewed')?.addEventListener('change', event => document.querySelector('#approve').disabled = !event.target.checked);
  document.querySelector('#approve').onclick = async event => {
    const button = event.currentTarget, label = button.innerHTML; setBusy(button, true);
    try { if (currentLesson.status !== 'APPROVED') await api(`/lessons/${id}/approve`, {method:'POST'}); if (token === routeToken) location.hash = `learn/${id}`; }
    catch (error) { showError(error); setBusy(button, false, label); }
  };
  document.querySelector('#regenerate').onclick = async event => {
    if (!confirm('Подготовить новую адаптацию? Сохранённые правки будут заменены, и материал нужно будет проверить снова.')) return;
    const button = event.currentTarget, label = button.innerHTML; setBusy(button, true);
    document.querySelector('#approve').disabled = true; document.querySelector('#edit-content').disabled = true;
    try { await api(`/lessons/${id}/regenerate`, {method:'POST'}); if (token === routeToken) await review(id, token); }
    catch (error) { showError(error); setBusy(button, false, label); document.querySelector('#edit-content').disabled = false; document.querySelector('#approve').disabled = lesson.status !== 'APPROVED' && !document.querySelector('#reviewed')?.checked; }
  };
  document.querySelector('#edit-content').onclick = () => editContent(lesson, token);
}
function editContent(lesson, token) {
  document.querySelector('#edit-content').disabled = true; document.querySelector('#regenerate').disabled = true; document.querySelector('#approve').disabled = true;
  const reviewed = document.querySelector('#reviewed'); if (reviewed) { reviewed.checked = false; reviewed.disabled = true; }
  document.querySelector('#review-content').innerHTML = `<form id="edit-form"><p class="profile-hint">Уточните формулировки, сохранив учебные понятия. После сохранения проверьте материал повторно.</p>${lesson.content.sentences.map((sentence,index) => `<label class="edit-label" for="sentence-${index}">Шаг ${index + 1}</label><textarea class="review-editor" id="sentence-${index}" name="sentence-${index}" required>${e(sentence.text)}</textarea>`).join('')}<h2>Понятия</h2>${lesson.content.cards.map((card,index) => `<label class="edit-label" for="word-${index}">Понятие ${index + 1}</label><input class="review-editor" id="word-${index}" name="word-${index}" required value="${e(card.word)}"><label class="edit-label" for="explanation-${index}">Объяснение</label><textarea class="review-editor" id="explanation-${index}" name="explanation-${index}" required>${e(card.explanation)}</textarea>`).join('')}<h2>Вопросы и ответы</h2>${lesson.content.quiz.map((question,index) => `<div class="question-editor"><label class="edit-label" for="question-${index}">Вопрос ${index + 1}</label><textarea class="review-editor" id="question-${index}" name="question-${index}" required>${e(question.question)}</textarea>${question.options.map((option,optionIndex) => `<label class="edit-label" for="option-${index}-${optionIndex}">Вариант ${optionIndex+1}</label><input class="review-editor" id="option-${index}-${optionIndex}" name="option-${index}-${optionIndex}" required value="${e(option)}">`).join('')}<label class="edit-label" for="correct-${index}">Правильный вариант</label><select id="correct-${index}" name="correct-${index}">${question.options.map((_,optionIndex) => `<option value="${optionIndex}" ${optionIndex === question.correctIndex ? 'selected' : ''}>Вариант ${optionIndex+1}</option>`).join('')}</select></div>`).join('')}<div class="button-row"><button class="button small" type="submit">Сохранить изменения</button><button class="button secondary small" id="cancel-edit" type="button">Отмена</button></div><div id="edit-error"></div></form>`;
  document.querySelector('#cancel-edit').onclick = () => review(lesson.id, token).catch(showError);
  document.querySelector('#edit-form').onsubmit = async event => {
    event.preventDefault(); const form = event.currentTarget; const content = structuredClone(lesson.content);
    content.sentences.forEach((sentence,index) => { sentence.text = form.elements[`sentence-${index}`].value.trim(); sentence.keywords = sentence.keywords.filter(word => sentence.text.toLowerCase().includes(word.toLowerCase())); });
    content.cards.forEach((card,index) => { const word = form.elements[`word-${index}`].value.trim(); if (word !== card.word) card.pictogramUrl = null; card.word = word; card.explanation = form.elements[`explanation-${index}`].value.trim(); });
    content.quiz.forEach((question,index) => { question.question = form.elements[`question-${index}`].value.trim(); question.options = question.options.map((_,j) => form.elements[`option-${index}-${j}`].value.trim()); question.correctIndex = Number(form.elements[`correct-${index}`].value); });
    const button = form.querySelector('[type=submit]'); setBusy(button, true);
    try { await api(`/lessons/${lesson.id}/content`, {method:'PUT', body:content}); if (token === routeToken) { await review(lesson.id, token); toast('Изменения сохранены. Проверьте урок перед началом.'); } }
    catch (error) { showError(error, '#edit-error'); setBusy(button, false, 'Сохранить изменения'); }
  };
}
function imageMarkup(card) { const url = safeImageUrl(card?.pictogramUrl); return url ? `<img src="${e(url)}" alt="${e(card.word)}" loading="lazy" referrerpolicy="no-referrer">` : ''; }
async function learn(id, token) {
  const lesson = await api(`/lessons/${id}/student`); if (token !== routeToken) return;
  learning = createLearningState(id, lesson);
  renderLearning();
}
function renderLearning() {
  const state = learning, {lesson, steps} = state, step = steps[state.index], settings = lesson.displaySettings;
  const fontSize = Math.min(30, Math.max(16, state.size));
  const background = /^#[0-9a-f]{6}$/i.test(settings.backgroundColor) ? settings.backgroundColor : '#ffffff';
  const style = `--lesson-bg:${background};--reading-size:${fontSize}px;--reading-line:${Math.min(2.5,Math.max(1.5,settings.lineHeight))};${settings.dyslexiaFont ? '--lesson-font:Verdana,Arial,sans-serif;--reading-spacing:.025em;' : ''}`;
  const progress = Math.round(state.completed.size / steps.length * 100);
  main.innerHTML = `<div class="lesson-shell" style="${style}"><div class="lesson-top"><a class="back-link" href="#library">${icon('back')}К материалам</a><div class="text-controls"><span>Размер текста</span><button id="smaller" aria-label="Уменьшить текст" ${fontSize<=16?'disabled':''}>A−</button><button id="larger" aria-label="Увеличить текст" ${fontSize>=30?'disabled':''}>A+</button></div></div><h1>${e(lesson.title)}</h1><p class="lesson-subtitle">${state.finished ? 'Урок завершён' : `Шаг ${state.index+1} из ${steps.length}`} · В своём темпе, без спешки</p><div class="lesson-progress" role="progressbar" aria-label="Прогресс урока" aria-valuenow="${progress}" aria-valuemin="0" aria-valuemax="100">${steps.map((_,index) => `<span class="progress-stop ${state.completed.has(index)?'done':''}"></span>`).join('')}</div><div class="lesson-layout"><nav class="lesson-nav" aria-label="Шаги урока">${steps.map((item,index) => `<button data-step="${index}" class="${index === state.index && !state.finished ? 'current':''}" ${index === state.index && !state.finished ? 'aria-current="step"':''}><span>${state.completed.has(index)?'✓':String(index+1).padStart(2,'0')}</span>${e(item.kind === 'read' ? `${item.title}` : item.title)}</button>`).join('')}</nav><div><section class="reading-panel" id="reading-panel" tabindex="-1">${state.finished ? completionMarkup() : stepMarkup(step)}</section>${state.finished ? '' : `<div class="lesson-actions"><button class="button secondary" id="previous" ${state.index===0?'disabled':''}>${icon('back')}Назад</button><button class="button" id="next" ${step.kind==='quiz' && !state.checked.has(state.index)?'disabled':''}>${state.index===steps.length-1 ? 'Завершить урок' : step.kind==='quiz' ? 'Следующий шаг' : 'Дальше'} ${icon('arrow')}</button></div><p class="learning-tip">Можно вернуться к любому шагу. Здесь нет таймера.</p>`}</div></div></div>`;
  document.querySelector('#attribution').hidden = !lesson.content.cards.some(card => safeImageUrl(card.pictogramUrl));
  document.querySelectorAll('.reading-panel img').forEach(img => img.addEventListener('error', () => img.hidden = true));
  document.querySelector('#smaller').onclick = () => {state.size = fontSize-2; renderLearning(); document.querySelector('#smaller').focus();};
  document.querySelector('#larger').onclick = () => {state.size = fontSize+2; renderLearning(); document.querySelector('#larger').focus();};
  document.querySelectorAll('[data-step]').forEach(button => button.onclick = () => { state.index = Number(button.dataset.step); state.finished = false; renderLearning(); focusLesson(); });
  document.querySelector('#previous')?.addEventListener('click', () => {state.index--;renderLearning();focusLesson();});
  document.querySelector('#next')?.addEventListener('click', () => {
    const previousIndex = state.index;
    if (!advanceStep(state)) return;
    if (!state.finished && state.index < previousIndex) toast('Вернёмся к шагу, который ещё не завершён.');
    renderLearning(); focusLesson();
  });
  document.querySelectorAll('[data-option]').forEach(button => button.onclick = () => {selectAnswer(state,Number(button.dataset.option));renderLearning();document.querySelector('#check-answer').focus();});
  document.querySelector('#check-answer')?.addEventListener('click', () => {checkAnswer(state);renderLearning();document.querySelector('#quiz-feedback').focus();});
  document.querySelector('#restart')?.addEventListener('click', () => {state.index=0;state.completed.clear();state.answers.clear();state.checked.clear();state.finished=false;renderLearning();focusLesson();});
}
function focusLesson() { document.querySelector('#reading-panel').focus({preventScroll:true}); document.querySelector('#reading-panel').scrollIntoView({block:'nearest'}); }
function stepMarkup(step) {
  const state = learning;
  if (step.kind === 'read') return `<span class="eyebrow">${icon('book')}Изучаем материал</span><h2>${e(step.title)}</h2><div class="reading-text">${step.sentences.map(sentence => {const card = state.lesson.displaySettings.pictogramPerSentence ? state.lesson.content.cards.find(card => safeImageUrl(card.pictogramUrl) && sentence.keywords.some(keyword => keyword.toLowerCase() === card.word.toLowerCase())) : null;return `<div class="sentence-row">${imageMarkup(card)}<p>${highlightedText(sentence.text,sentence.keywords)}</p></div>`;}).join('')}</div>`;
  if (step.kind === 'cards') return `<span class="eyebrow">${icon('cards')}Собираем главное</span><h2>Ключевые понятия</h2><div class="vocabulary">${state.lesson.content.cards.map(card => `<article class="vocab-card">${imageMarkup(card)}<h3>${e(card.word)}</h3><p>${e(card.explanation)}</p></article>`).join('')}</div>`;
  const question = step.question, selected = state.answers.get(state.index), checked = state.checked.has(state.index), correct = selected === question.correctIndex;
  return `<span class="eyebrow">${icon('check')}Проверяем понимание · ${e(step.title)}</span><h2 id="quiz-question">${e(question.question)}</h2><div class="quiz-options" role="group" aria-labelledby="quiz-question">${question.options.map((option,index) => `<button class="quiz-option ${selected === index ? 'selected':''}" data-option="${index}" aria-pressed="${selected === index}"><span class="option-letter">${index+1}</span>${e(option)}</button>`).join('')}</div>${checked ? `<div class="quiz-feedback ${correct?'':'retry'}" id="quiz-feedback" tabindex="-1" role="status"><strong>${correct ? 'Верно. Вы разобрались!' : 'Давайте разберёмся.'}</strong><p>${correct ? 'Можно переходить к следующему шагу.' : `Правильный ответ: ${e(question.options[question.correctIndex])}. Можно вернуться к объяснению или выбрать ответ ещё раз.`}</p></div>` : `<div class="button-row"><button class="button" id="check-answer" ${selected === undefined?'disabled':''}>Проверить ответ</button></div>`}`;
}
function completionMarkup() { return `<div class="completion"><div class="completion-icon">${icon('flag')}</div><span class="eyebrow" style="justify-content:center">Ещё один шаг вперёд</span><h2>Вы изучили новую тему</h2><p>Объяснение, ключевые понятия и вопросы —<br>все шаги пройдены. К уроку можно вернуться.</p><div class="button-row"><a class="button" href="#library">К моим материалам ${icon('arrow')}</a><button class="button secondary" id="restart">Пройти ещё раз</button></div></div>`; }
async function route() {
  const hash = location.hash.slice(1) || 'home', [view,id] = hash.split('/');
  if (['home','create','how'].includes(view) && document.querySelector('#adapt-form')) { if (view !== 'home') document.getElementById(view)?.scrollIntoView({block:'start'}); else window.scrollTo(0,0); return; }
  const token = ++routeToken;
  document.querySelector('#attribution').hidden = true;
  document.querySelectorAll('[data-nav]').forEach(link => {link.classList.toggle('active', link.dataset.nav === view);if(link.dataset.nav===view)link.setAttribute('aria-current','page');else link.removeAttribute('aria-current');});
  main.innerHTML = '<div class="loading" role="status"><div class="spinner"></div>Готовим пространство для учёбы…</div>'; window.scrollTo(0,0);
  try {
    if (['home','create','how'].includes(view)) { main.innerHTML = homeMarkup(); await setupHome(token); if (token === routeToken && view !== 'home') document.getElementById(view)?.scrollIntoView({block:'start'}); }
    else { await loadProfiles(); if (token !== routeToken) return; if (view === 'library') await library(token); else if (/^\d+$/.test(id) && view === 'review') await review(id,token); else if (/^\d+$/.test(id) && view === 'learn') await learn(id,token); else main.innerHTML = '<div class="container page"><h1>Такой страницы нет</h1><a class="button" href="#home">На главную</a></div>'; }
    if (token === routeToken) {document.title = `${view==='learn' ? learning?.lesson.title : view==='library'?'Мои материалы':view==='review'?'Проверка материала':'Разные пути к пониманию'} — Qadam`;main.focus({preventScroll:true});}
  } catch (error) { if (token !== routeToken) return; main.innerHTML = `<div class="container page"><h1>Пока не удалось открыть материал</h1>${errorBox(error.message)}<div class="button-row"><button class="button" id="retry-page">Попробовать снова</button><a class="button secondary" href="#library">К материалам</a>${view==='learn'&&/^\d+$/.test(id)?`<a class="button secondary" href="#review/${id}">Проверить материал</a>`:''}</div></div>`;document.querySelector('#retry-page').onclick=route; }
}
document.querySelector('.skip-link').addEventListener('click', event => {
  event.preventDefault();
  main.focus();
  main.scrollIntoView({block: 'start'});
});
window.addEventListener('hashchange', route);
route();

<<<<<<< HEAD
import {api} from './api.mjs';
import {escapeHtml as e, statusLabels, stageFor, ratingLevel, formatDate} from './ui-utils.mjs';

const icons = {
  arrow:'<path d="M5 12h14m-5-5 5 5-5 5"/>', back:'<path d="M19 12H5m5-5-5 5 5 5"/>',
  plus:'<path d="M12 5v14M5 12h14"/>', check:'<path d="m5 12 4 4L19 6"/>', close:'<path d="m6 6 12 12M6 18 18-12"/>',
  spark:'<path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3Z"/>',
  pin:'<path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 0 1 14 0Z"/><circle cx="12" cy="10" r="2"/>',
  briefcase:'<rect x="3" y="7" width="18" height="14" rx="2"/><path d="M8 7V3h8v4M3 12c6 4 12 4 18 0M12 12v4"/>',
  chart:'<path d="M4 3v17h17M8 15l4-5 4 2 5-7"/>',
  mail:'<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 6 9 7 9-7"/>',
  clock:'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  edit:'<path d="m16 3 5 5L9 20l-6 1 1-6L16 3Z"/>',
  search:'<circle cx="10" cy="10" r="6"/><path d="m15 15 6 6"/>',
  shield:'<path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z"/><path d="m8 11 3 3 5-5"/>',
};
const icon = name => `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${icons[name]||icons.briefcase}</svg>`;
const main = document.querySelector('#main');
const dialog = document.querySelector('#application-dialog');
let meta, token = 0, toastTimer, catalogFilters = {}, applicationFilters = {direction:'received',status:''};
const formMemory = new Map();
const workflow = [['draft','Черновик'],['questions','Вопросы ИИ'],['card','Карточка'],['rating','Рейтинг'],['published','Публикация']];

function notify(message) {
  const toast=document.querySelector('#toast');toast.textContent=message;toast.hidden=false;
  clearTimeout(toastTimer);toastTimer=setTimeout(()=>toast.hidden=true,4500);
}
function report(error, target='#action-error') {
  const box=document.querySelector(target);
  if(box){box.innerHTML=`<div class="error" role="alert">${e(error.message||'Не удалось выполнить действие. Попробуйте ещё раз.')}</div>`;box.scrollIntoView({block:'nearest'});}
  else notify(error.message);
}
function loading(label='Загружаем пространство…'){return `<div class="loading" role="status"><span class="spinner"></span>${e(label)}</div>`;}
function statusBadge(status){return `<span class="status-badge ${e(status.toLowerCase())}"><span></span>${e(statusLabels[status]||status)}</span>`;}
function ratingBadge(analysis){const level=ratingLevel(analysis.score);return `<span class="rating-badge ${level.tone}">${icon('chart')}<strong>${analysis.score}</strong><span>· ${level.label}</span></span>`;}
function options(items,value,placeholder){return `<option value="">${e(placeholder)}</option>${items.map(item=>`<option value="${e(item)}" ${item===value?'selected':''}>${e(item)}</option>`).join('')}`;}
function field(name,label,value,{textarea=false,min=1,max=2000,placeholder='',hint='',rows=3}={}) {
  return `<div class="field"><label for="${name}">${label}</label>${textarea?`<textarea id="${name}" name="${name}" rows="${rows}" minlength="${min}" maxlength="${max}" required ${hint?`aria-describedby="${name}-hint"`:''} placeholder="${e(placeholder)}">${e(value)}</textarea>`:`<input id="${name}" name="${name}" value="${e(value)}" minlength="${min}" maxlength="${max}" required placeholder="${e(placeholder)}">`}${hint?`<small id="${name}-hint">${e(hint)}</small>`:''}</div>`;
}
function selectors(item){return `<div class="fields-three"><div class="field"><label for="category">Сфера</label><select id="category" name="category" required>${options(meta.categories,item.category,'Выберите сферу')}</select></div><div class="field"><label for="city">Город</label><select id="city" name="city" required>${options(meta.cities,item.city,'Выберите город')}</select></div><div class="field"><label for="partnershipType">Ищете</label><select id="partnershipType" name="partnershipType" required>${options(meta.partnershipTypes,item.partnershipType,'Формат сотрудничества')}</select></div></div>`;}
function formData(form){return Object.fromEntries(new FormData(form));}
function remember(form,key){form.addEventListener('input',()=>formMemory.set(key,formData(form)));}
async function pending(container,label,work){
  if(container.dataset.busy==='true')return;
  container.dataset.busy='true';container.setAttribute('aria-busy','true');
  const controls=[...container.querySelectorAll('button,input,textarea,select')];
  const wasDisabled=controls.map(control=>control.disabled);controls.forEach(control=>control.disabled=true);
  const button=container.matches('button')?container:container.querySelector('button[type=submit]');
  const original=button?.innerHTML;if(button){button.disabled=true;button.innerHTML=`<span class="spinner small"></span>${e(label)}`;}
  try {return await work();} finally {
    container.dataset.busy='false';container.removeAttribute('aria-busy');controls.forEach((control,index)=>control.disabled=wasDisabled[index]);
    if(button){button.innerHTML=original;button.disabled=false;}
  }
}
function go(path){if(location.hash===`#${path}`)route();else location.hash=path;}
function pageHeading(eyebrow,title,description,action=''){return `<div class="page-heading"><div><p class="eyebrow">${eyebrow}</p><h1>${title}</h1><p class="lead">${description}</p></div>${action}</div>`;}
function empty(title,text,action=''){return `<div class="empty">${icon('search')}<h2>${title}</h2><p>${text}</p>${action}</div>`;}
function renderWorkflowHeader(stage,item){
  const active=workflow.findIndex(([key])=>key===stage);
  return `<a class="back-link" href="#business">${icon('back')}Мой бизнес</a><div class="workflow-heading"><div><p class="eyebrow">Новая возможность</p><h1>${item?.title?e(item.title):'Расскажите о своём бизнесе'}</h1></div>${statusBadge(item?.status||'DRAFT')}</div><ol class="stepper" aria-label="Этапы создания">${workflow.map(([key,label],index)=>`<li class="${index===active?'active':index<active?'complete':''}" ${index===active?'aria-current="step"':''}><span>${index<active?icon('check'):index+1}</span>${label}</li>`).join('')}</ol>`;
}
function cardMarkup(item,{mine=false}={}){
  return `<article class="business-card"><div class="card-top"><span class="company-logo tone-${meta.categories.indexOf(item.category)%5}">${e(item.title.slice(0,2).toUpperCase())}</span>${item.analysis?ratingBadge(item.analysis):statusBadge(item.status)}</div><div class="card-category">${e(item.category)} <span>·</span> ${e(item.partnershipType)}</div><h2><a href="#${mine?`business/${item.id}/${stageFor(item)}`:`opportunity/${item.id}`}">${e(item.title)}</a></h2><p class="card-summary">${e(item.tagline||item.description)}</p><div class="card-footer"><span>${icon('pin')}${e(item.city)}</span><a href="#${mine?`business/${item.id}/${stageFor(item)}`:`opportunity/${item.id}`}" class="card-link">${mine&&item.status!=='PUBLISHED'?'Продолжить':'Подробнее'}${icon('arrow')}</a></div></article>`;
}
async function businessPage(routeId){
  const items=await api.listMine();if(routeId!==token)return;
  main.innerHTML=`<div class="container">${pageHeading('От идеи к партнёрству','Мой бизнес','Соберите понятное предложение и найдите тех, с кем можно расти.',`<a class="button" href="#business/new">${icon('plus')}Создать проект</a>`)}<section class="intro-strip"><div class="intro-icon">${icon('spark')}</div><div><h2>Хорошее партнёрство начинается с ясности</h2><p>Опишите идею → ответьте на вопросы → проверьте карточку → получите оценку → опубликуйте.</p></div><a href="#business/new" class="text-link">Начать ${icon('arrow')}</a></section><div class="section-title"><h2>Ваши проекты <span class="count">${items.length}</span></h2><span class="muted">Черновики видны только вам</span></div>${items.length?`<div class="card-grid">${items.map(item=>cardMarkup(item,{mine:true})).join('')}</div>`:empty('Первый шаг — ваша идея','Добавьте проект, чтобы подготовить предложение для партнёров.','<a href="#business/new" class="button">Создать проект</a>')}</div>`;
}
async function draftPage(id,routeId){
  const item=id?await api.getBusiness(id):null;if(routeId!==token)return;
  if(item&&item.status!=='DRAFT'){go(`business/${id}/${stageFor(item)}`);return;}
  const key=id||'new',draft=formMemory.get(key)||item||{};
  main.innerHTML=`<div class="container workflow">${renderWorkflowHeader('draft',item)}<div class="two-column"><section class="panel"><div class="panel-heading"><span class="mini-icon">${icon('briefcase')}</span><div><h2>Начнём с самого важного</h2><p>Опишите проект своими словами. Детали уточним на следующем шаге.</p></div></div><form id="draft-form">${field('title','Название проекта',draft.title,{min:3,max:100,placeholder:'Как называется ваш проект?'})}${selectors(draft)}${field('description','Что вы делаете и кого ищете?',draft.description,{textarea:true,min:30,max:3000,rows:6,placeholder:'Для кого ваш продукт? Какую задачу решает? Какая помощь или партнёрство нужны?',hint:'От 30 до 3 000 символов. На следующем шаге можно добавить детали.'})}<div id="action-error"></div><div class="form-actions"><button type="button" class="button ghost" id="example">Заполнить пример</button><button type="submit" class="button">Продолжить ${icon('arrow')}</button></div><p class="form-footnote">Сохраним черновик и подготовим вопросы. В каталоге он пока не появится.</p></form></section><aside><div class="side-panel"><span class="eyebrow">Что будет дальше</span><h2>Идея становится<br>предложением</h2><ul class="benefits"><li>${icon('spark')}Вопросы помогут раскрыть недостающие детали</li><li>${icon('edit')}Карточку можно отредактировать перед публикацией</li><li>${icon('chart')}Рейтинг покажет, что стоит уточнить</li></ul><div class="side-note">Вы решаете, когда проект готов к публикации.</div></div></aside></div></div>`;
  const form=document.querySelector('#draft-form');remember(form,key);
  document.querySelector('#example').onclick=()=>{for(const [name,value] of Object.entries(meta.exampleDraft||{}))if(form.elements[name])form.elements[name].value=value;formMemory.set(key,formData(form));notify('Пример добавлен. Его можно изменить.');};
  form.onsubmit=event=>{event.preventDefault();if(!form.reportValidity())return;const data=formData(form);pending(form,'Сохраняем…',async()=>{try{const saved=id?await api.updateDraft(id,data):await api.createDraft(data);formMemory.delete(key);if(routeId===token)go(`business/${saved.id}/questions`);}catch(error){if(routeId===token)report(error);}});};
}
async function questionsPage(id,routeId){
  const [item,questions]=await Promise.all([api.getBusiness(id),api.getQuestions(id)]);if(routeId!==token)return;
  if(item.status==='PUBLISHED'){go(`business/${id}/published`);return;}
  const remembered=formMemory.get(`answers-${id}`)||item.answers||{};
  main.innerHTML=`<div class="container workflow">${renderWorkflowHeader('questions',{...item,status:'QUESTIONS'})}<div class="two-column"><section class="panel"><div class="panel-heading"><span class="mini-icon">${icon('spark')}</span><div><h2>Давайте уточним предложение</h2><p>${questions.length} вопроса, чтобы будущему партнёру было проще вас понять.</p></div></div><p class="inline-note">Демонстрационные вопросы. Ответы войдут в вашу карточку без изменения смысла.</p><form id="questions-form">${questions.map((q,index)=>`<div class="question-number">Вопрос ${index+1} из ${questions.length}</div>${field(q.id,e(q.title),remembered[q.id],{textarea:true,min:10,max:2000,rows:3,hint:q.hint,placeholder:'Ваш ответ…'})}`).join('')}<div id="action-error"></div><div class="form-actions"><button type="button" class="button ghost" id="answer-examples">Подставить примеры ответов</button><button type="submit" class="button">Получить карточку ${icon('arrow')}</button></div></form></section><aside class="side-panel"><span class="eyebrow">Ваш проект</span><h2>${e(item.title)}</h2><p>${e(item.description)}</p><div class="side-note">Конкретные ответы полезнее общих обещаний. Если данных пока нет, опишите, как планируете их получить.</div></aside></div></div>`;
  const form=document.querySelector('#questions-form');remember(form,`answers-${id}`);
  document.querySelector('#answer-examples').onclick=()=>{questions.forEach(q=>form.elements[q.id].value=q.example||'');formMemory.set(`answers-${id}`,formData(form));notify('Примеры добавлены. Уточните их под свой проект.');};
  form.onsubmit=event=>{event.preventDefault();if(!form.reportValidity())return;const answers=formData(form);pending(form,'Готовим карточку…',async()=>{try{await api.saveAnswers(id,answers);formMemory.delete(`answers-${id}`);if(routeId===token)go(`business/${id}/card`);}catch(error){if(routeId===token)report(error);}});};
}
async function editableCardPage(id,routeId){
  const item=await api.getBusiness(id);if(routeId!==token)return;
  if(!['CARD','RATED'].includes(item.status)){go(`business/${id}/${stageFor(item)}`);return;}
  const draft={...item,...formMemory.get(`card-${id}`)};
  main.innerHTML=`<div class="container workflow">${renderWorkflowHeader('card',item)}<div class="two-column card-editor"><section class="panel"><div class="panel-heading"><span class="mini-icon">${icon('edit')}</span><div><h2>Карточка готова к проверке</h2><p>Уточните формулировки. После сохранения можно получить рейтинг.</p></div></div><form id="card-form">${field('title','Название проекта',draft.title,{min:3,max:100})}${field('tagline','Краткое предложение',draft.tagline,{min:10,max:160,hint:'Одно предложение для карточки в каталоге.'})}${selectors(draft)}${field('description','О проекте',draft.description,{textarea:true,min:30,max:3000})}${field('audience','Клиенты и их задача',draft.audience,{textarea:true,min:10,max:2000})}${field('traction','Текущие результаты',draft.traction,{textarea:true,min:10,max:2000})}${field('offer','Предложение партнёру',draft.offer,{textarea:true,min:10,max:2000})}<div id="action-error"></div><div class="form-actions"><button type="submit" value="save" class="button secondary">Сохранить карточку</button><button type="submit" value="analyze" class="button">Посмотреть рейтинг ${icon('arrow')}</button></div><p class="form-footnote">Любое изменение карточки обновляет черновик и сбрасывает предыдущую оценку.</p></form></section><aside><div class="preview-label">Так вас увидят в каталоге</div><div id="card-preview">${cardMarkup({...draft,analysis:null})}</div><div class="side-note standalone">${icon('shield')}Публикация произойдёт только после вашего подтверждения.</div></aside></div></div>`;
  const form=document.querySelector('#card-form');remember(form,`card-${id}`);
  form.addEventListener('input',()=>document.querySelector('#card-preview').innerHTML=cardMarkup({...item,...formData(form),analysis:null}));
  form.onsubmit=event=>{event.preventDefault();if(!form.reportValidity())return;const data=formData(form),analyze=event.submitter?.value==='analyze';pending(form,analyze?'Готовим оценку…':'Сохраняем…',async()=>{try{await api.updateCard(id,data);formMemory.delete(`card-${id}`);if(analyze){await api.analyze(id);if(routeId===token)go(`business/${id}/rating`);}else notify('Карточка сохранена. Можно переходить к рейтингу.');}catch(error){if(routeId===token)report(error);}});};
}
function analysisMarkup(analysis,{compact=false}={}){
  const level=ratingLevel(analysis.score);
  return `<div class="analysis ${compact?'compact':''}"><div class="rating-summary ${level.tone}"><div class="rating-orb" style="--score:${analysis.score}%"><span><strong>${analysis.score}</strong><small>из 100</small></span></div><div><span class="eyebrow">Рейтинг карточки</span><h2>${level.label}</h2><p>${level.description}</p></div><span class="badge">Демо-оценка</span></div><p class="analysis-note">${e(analysis.explanation)}</p><div class="panel"><h2>Из чего складывается оценка</h2><div class="breakdown">${analysis.breakdown.map(part=>`<div class="breakdown-item"><div><strong>${e(part.title)}</strong><span>${part.score} <small>/ ${part.weight}</small></span></div><progress value="${part.score}" max="${part.weight}" aria-label="${e(part.title)}"></progress><p>${e(part.detail)}</p></div>`).join('')}</div></div><div class="analysis-columns"><section class="panel"><h3>${icon('check')}Сильные стороны</h3><ul class="analysis-list">${analysis.strengths.map(text=>`<li>${e(text)}</li>`).join('')}</ul></section><section class="panel"><h3>${icon('shield')}Что стоит учесть</h3><ul class="analysis-list">${analysis.risks.map(text=>`<li>${e(text)}</li>`).join('')}</ul></section></div><section class="panel recommendations"><div class="section-title"><h2>${icon('spark')}Рекомендации</h2><span class="muted">Следующий шаг к сильной карточке</span></div>${analysis.recommendations.map((item,index)=>`<div class="recommendation"><span>${index+1}</span><div><h3>${e(item.title)}</h3><p>${e(item.text)}</p></div></div>`).join('')}</section></div>`;
}
async function ratingPage(id,routeId){
  const item=await api.getBusiness(id);if(routeId!==token)return;
  if(item.status!=='RATED'){go(`business/${id}/${stageFor(item)}`);return;}
  main.innerHTML=`<div class="container workflow">${renderWorkflowHeader('rating',item)}${analysisMarkup(item.analysis)}<div class="publish-bar"><div><h2>Готовы обсудить партнёрство?</h2><p>После публикации карточка появится в каталоге.</p></div><div class="button-row"><a class="button secondary" href="#business/${id}/card">${icon('edit')}Улучшить карточку</a><button class="button" id="publish">Опубликовать ${icon('arrow')}</button></div></div><div id="action-error"></div></div>`;
  document.querySelector('#publish').onclick=event=>pending(event.currentTarget,'Публикуем…',async()=>{try{await api.publish(id);if(routeId===token){notify('Проект опубликован в каталоге');go(`business/${id}/published`);}}catch(error){if(routeId===token)report(error);}});
}
async function publishedPage(id,routeId){
  const item=await api.getBusiness(id);if(routeId!==token)return;
  if(item.status!=='PUBLISHED'){go(`business/${id}/${stageFor(item)}`);return;}
  main.innerHTML=`<div class="container workflow">${renderWorkflowHeader('published',item)}<section class="published-panel"><div class="success-icon">${icon('check')}</div><p class="eyebrow">Следующий шаг сделан</p><h1>Ваш проект в каталоге</h1><p>Карточка опубликована. Теперь потенциальные партнёры могут познакомиться с предложением и откликнуться.</p><div class="published-card">${cardMarkup(item)}</div><div class="button-row"><a class="button" href="#opportunity/${id}">Открыть карточку ${icon('arrow')}</a><a class="button secondary" href="#catalog">Перейти в каталог</a></div></section></div>`;
}
async function catalogPage(routeId){
  main.innerHTML=`<div class="container">${pageHeading('Здесь начинаются партнёрства','Каталог возможностей','Найдите проект, которому пригодится ваш опыт, продукт или команда.',`<a class="button" href="#business/new">${icon('plus')}Добавить проект</a>`)}<form class="filters" id="catalog-filters"><div class="search-field">${icon('search')}<label class="sr-only" for="search">Поиск по проектам</label><input id="search" name="search" placeholder="Название или ключевые слова" value="${e(catalogFilters.search||'')}"></div><div><label for="filter-category">Сфера</label><select id="filter-category" name="category">${options(meta.categories,catalogFilters.category,'Все сферы')}</select></div><div><label for="filter-city">Город</label><select id="filter-city" name="city">${options(meta.cities,catalogFilters.city,'Все города')}</select></div><div><label for="filter-type">Формат</label><select id="filter-type" name="partnershipType">${options(meta.partnershipTypes,catalogFilters.partnershipType,'Все форматы')}</select></div><div><label for="filter-rating">Рейтинг</label><select id="filter-rating" name="minimumScore"><option value="0">Любой</option value="80" ${catalogFilters.minimumScore==='80'?'selected':''}>Высокий · от 80</option><option value="60" ${catalogFilters.minimumScore==='60'?'selected':''}>От 60</option></select></div><button class="button" type="submit">Найти</button></form><div class="catalog-toolbar"><p id="catalog-count" role="status">Загружаем проекты…</p><div><button class="text-link" id="reset-filters">Сбросить фильтры</button><label class="sr-only" for="sort">Сортировка</label><select id="sort"><option value="rating">Сначала высокий рейтинг</option><option value="rating-asc">Сначала низкий рейтинг</option><option value="newest">Сначала новые</option></select></div></div><div id="catalog-results">${loading()}</div></div>`;
  const form=document.querySelector('#catalog-filters'),sort=document.querySelector('#sort');sort.value=catalogFilters.sort||'rating';let request=0;
  const refresh=async()=>{const sequence=++request;catalogFilters={...formData(form),sort:sort.value};const results=document.querySelector('#catalog-results');results.innerHTML=loading('Ищем подходящие проекты…');try{const items=await api.listCatalog(catalogFilters);if(routeId!==token||sequence!==request)return;document.querySelector('#catalog-count').textContent=`Найдено проектов: ${items.length}`;results.innerHTML=items.length?`<div class="card-grid">${items.map(item=>cardMarkup(item)).join('')}</div>`:empty('Пока нет совпадений','Попробуйте другую сферу, город или снизьте минимальный рейтинг.');}catch(error){if(routeId===token&&sequence===request){results.innerHTML='<div id="catalog-error"></div>';report(error,'#catalog-error');}}};
  form.onsubmit=event=>{event.preventDefault();refresh();};form.querySelectorAll('select').forEach(select=>select.onchange=refresh);sort.onchange=refresh;
  document.querySelector('#reset-filters').onclick=()=>{for(const element of form.elements)if(element.name)element.value=element.name==='minimumScore'?'0':'';sort.value='rating';refresh();};
  await refresh();
}
async function detailPage(id,routeId){
  const [item,sent]=await Promise.all([api.getBusiness(id),api.listApplications({direction:'sent'})]);if(routeId!==token)return;
  if(item.status!=='PUBLISHED'){go(item.ownerId===meta.user.id?`business/${id}/${stageFor(item)}`:'catalog');return;}
  const own=item.ownerId===meta.user.id,application=sent.find(entry=>entry.opportunityId===id);
  main.innerHTML=`<div class="container detail"><a class="back-link" href="#catalog">${icon('back')}В каталог</a><div class="detail-heading"><span class="company-logo large tone-${meta.categories.indexOf(item.category)%5}">${e(item.title.slice(0,2).toUpperCase())}</span><div><p class="eyebrow">${e(item.category)} · ${e(item.city)}</p><h1>${e(item.title)}</h1><p class="lead">${e(item.tagline)}</p></div>${ratingBadge(item.analysis)}</div><div class="two-column"><div><section class="panel detail-copy"><h2>О проекте</h2><p>${e(item.description)}</p><h3>Клиенты и их задача</h3><p>${e(item.audience)}</p><h3>Текущие результаты</h3><p>${e(item.traction)}</p><h3>Предложение партнёру</h3><p>${e(item.offer)}</p></section>${analysisMarkup(item.analysis,{compact:true})}</div><aside><section class="panel action-panel"><span class="eyebrow">Возможность для сотрудничества</span><h2>${e(item.partnershipType)}</h2><p>Расскажите, чем можете быть полезны и какой следующий шаг предлагаете.</p>${own?'<p class="inline-note">Это ваш проект. Новые заявки появятся в разделе «Отклики».</p><a class="button full" href="#applications">Открыть отклики</a>':application?`${statusBadge(application.status)}<p class="success-text">Вы уже откликнулись на этот проект.</p><a class="button secondary full" href="#applications/sent">Посмотреть отклик</a>`:`<button class="button full" id="apply">Откликнуться ${icon('arrow')}</button>`}<p class="form-footnote">Демо: заявка сохранится только в вашем браузере.</p></section></aside></div></div>`;
  document.querySelector('#apply')?.addEventListener('click',()=>applyDialog(item,routeId));
}
function applyDialog(item,routeId){
  dialog.innerHTML=`<form id="application-form"><div class="dialog-header"><div><p class="eyebrow">Новый контакт</p><h2 id="dialog-title">Откликнуться на проект</h2></div><button type="button" class="icon-button" id="close-dialog" aria-label="Закрыть">${icon('close')}</button></div><p class="lead">${e(item.title)}</p>${field('message','Ваше предложение','',{textarea:true,min:10,max:2000,rows:4,placeholder:'Чем вы можете помочь? Как предлагаете начать сотрудничество?'})}${field('contact','Как с вами связаться','',{min:3,max:150,placeholder:'Почта, телефон или Telegram'})}<p class="inline-note">Демонстрация: сообщение не отправляется реальному получателю.</p><div id="dialog-error"></div><button type="submit" class="button full">Отправить отклик ${icon('arrow')}</button></form>`;
  dialog.showModal();document.querySelector('#close-dialog').onclick=()=>dialog.close();const form=document.querySelector('#application-form');
  form.onsubmit=event=>{event.preventDefault();if(!form.reportValidity())return;const data=formData(form);pending(form,'Сохраняем отклик…',async()=>{try{await api.apply(item.id,data);dialog.close();notify('Отклик сохранён. Он появился в разделе «Отправленные».');if(routeId===token)await detailPage(item.id,routeId);}catch(error){report(error,'#dialog-error');}});};
}
async function applicationsPage(routeId,direction){
  if(direction)applicationFilters.direction=direction;
  const filters={...applicationFilters};const items=await api.listApplications(filters);if(routeId!==token)return;
  const received=filters.direction==='received';
  main.innerHTML=`<div class="container">${pageHeading('От знакомства к сотрудничеству','Отклики',received?'Люди и команды, которым интересно ваше предложение.':'Ваши предложения другим проектам и их текущий статус.')}<div class="applications-toolbar"><div class="tabs" role="group" aria-label="Направление откликов"><button data-direction="received" class="${received?'active':''}" aria-pressed="${received}">Полученные</button><button data-direction="sent" class="${!received?'active':''}" aria-pressed="${!received}">Отправленные</button></div><div class="status-filter"><label for="response-status">Статус</label><select id="response-status"><option value="">Все отклики</option value="PENDING" ${filters.status==='PENDING'?'selected':''}>Ожидают решения</option><option value="ACCEPTED" ${filters.status==='ACCEPTED'?'selected':''}>Принятые</option><option value="REJECTED" ${filters.status==='REJECTED'?'selected':''}>Отклонённые</option></select></div></div><div id="action-error"></div><div class="section-title"><h2>${received?'Полученные':'Отправленные'} <span class="count">${items.length}</span></h2><span class="muted">Контакты доступны в каждом отклике</span></div>${items.length?`<div class="applications-list">${items.map(item=>`<article class="application-card" data-application="${e(item.id)}"><div class="application-person"><span class="person-avatar">${e(item.name.split(' ').map(word=>word[0]).slice(0,2).join(''))}</span><div><h2>${e(received?item.name:item.opportunityTitle)}</h2><p>${e(received?item.company:'Ваш отклик')}</p></div><time datetime="${e(item.createdAt)}">${formatDate(item.createdAt)}</time></div><p class="application-project">К проекту <a href="#opportunity/${e(item.opportunityId)}">${e(item.opportunityTitle)}</a></p><p class="application-message">${e(item.message)}</p><div class="application-contact">${icon('mail')}${e(item.contact)}</div><div class="application-bottom">${statusBadge(item.status)}${received&&item.status==='PENDING'?`<div class="button-row"><button class="button secondary small" data-respond="REJECTED" data-id="${e(item.id)}">Отклонить</button><button class="button small" data-respond="ACCEPTED" data-id="${e(item.id)}">${icon('check')}Принять</button></div>`:''}</div></article>`).join('')}</div>`:empty(received?'Таких откликов пока нет':'Здесь появятся ваши отклики',received?'Выберите другой статус или дождитесь новых предложений.':'Найдите интересный проект в каталоге и предложите сотрудничество.',!received?'<a class="button" href="#catalog">Открыть каталог</a>':'')}</div>`;
  document.querySelectorAll('[data-direction]').forEach(button=>button.onclick=()=>{applicationFilters={direction:button.dataset.direction,status:''};go(`applications/${button.dataset.direction}`);});
  document.querySelector('#response-status').onchange=event=>{applicationFilters.status=event.target.value;route();};
  document.querySelectorAll('[data-respond]').forEach(button=>button.onclick=()=>pending(button.closest('article'),'Сохраняем…',async()=>{try{await api.respond(button.dataset.id,button.dataset.respond);notify(button.dataset.respond==='ACCEPTED'?'Отклик принят. Можно связаться с партнёром.':'Отклик отклонён. Статус сохранён.');if(routeId===token)await applicationsPage(routeId);}catch(error){if(routeId===token)report(error);}}));
}
async function route(){
  const routeId=++token;const [view='business',id,stage]=(location.hash.slice(1)||'business').split('/');
  if(dialog.open)dialog.close();main.innerHTML=loading();window.scrollTo(0,0);
  const nav=view==='opportunity'?'catalog':view;
  document.querySelectorAll('[data-nav]').forEach(link=>{const active=link.dataset.nav===nav;link.classList.toggle('active',active);if(active)link.setAttribute('aria-current','page');else link.removeAttribute('aria-current');});
  try{
    if(!meta){meta=await api.getMeta();document.querySelector('#storage-note').textContent=meta.persistent?'Изменения сохраняются в этом браузере':'Временная сессия: после обновления изменения сбросятся';}if(routeId!==token)return;
    if(view==='business'&&!id)await businessPage(routeId);
    else if(view==='business'&&id==='new')await draftPage(null,routeId);
    else if(view==='business'&&id){const business=await api.getBusiness(id);if(routeId!==token)return;if(business.ownerId!==meta.user.id)throw new Error('Эта карточка принадлежит другому автору. Откройте её в каталоге.');const screens={draft:draftPage,questions:questionsPage,card:editableCardPage,rating:ratingPage,published:publishedPage};await (screens[stage]||screens[stageFor(business)])(id,routeId);}
    else if(view==='catalog')await catalogPage(routeId);
    else if(view==='opportunity'&&id)await detailPage(id,routeId);
    else if(view==='applications')await applicationsPage(routeId,['sent','received'].includes(id)?id:undefined);
    else throw new Error('Страница не найдена. Вернитесь в «Мой бизнес».');
    if(routeId===token){document.title=`${{business:'Мой бизнес',catalog:'Каталог',opportunity:'Карточка проекта',applications:'Отклики'}[view]||'Бизнес-возможности'} — Qadam`;main.focus({preventScroll:true});}
  }catch(error){if(routeId!==token)return;main.innerHTML=`<div class="container">${empty('Не получилось открыть страницу',e(error.message),'<button class="button" id="retry">Попробовать снова</button><a class="button secondary" href="#business">Мой бизнес</a>')}</div>`;document.querySelector('#retry').onclick=route;}
}
document.querySelector('.skip-link').onclick=event=>{event.preventDefault();main.focus();main.scrollIntoView({block:'start'});};
window.addEventListener('hashchange',route);
=======
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
>>>>>>> origin/main
route();

import {test} from 'node:test';
import assert from 'node:assert/strict';
import {escapeHtml, highlightedText, readingSteps, safeImageUrl} from '../../main/resources/static/lesson-utils.mjs';

test('lesson text and highlighted keywords cannot introduce HTML', () => {
  assert.equal(escapeHtml('<script>"&'), '&lt;script&gt;&quot;&amp;');
  assert.equal(highlightedText('<img onerror=alert(1)> Вода и вода.', ['Вода', 'вода']), '&lt;img onerror=alert(1)&gt; <mark>Вода</mark> и <mark>вода</mark>.');
  assert.equal(highlightedText('C++ и (вода)', ['C++', '(вода)']), '<mark>C++</mark> и <mark>(вода)</mark>');
});

test('reading chunks preserve every sentence and order across section boundaries', () => {
  const sentences = Array.from({length: 12}, (_, index) => ({text: String(index), section: index < 5 ? 'FIRST' : index < 9 ? 'THEN' : 'FINALLY'}));
  const quiz = [{question:'Question', options:['A','B'], correctIndex:1}];
  for (const showSequenceStructure of [true, false]) {
    const steps = readingSteps({sentences, quiz}, {showSequenceStructure});
    const reading = steps.filter(step => step.kind === 'read');
    assert.deepEqual(reading.flatMap(step => step.sentences), sentences);
    assert.ok(reading.every(step => step.sentences.length <= 4));
    assert.equal(steps.at(-2).kind, 'cards');
    assert.equal(steps.at(-1).question, quiz[0]);
    if (showSequenceStructure) assert.ok(reading.every(step => step.sentences.every(sentence => sentence.section === step.section)));
  }
});

test('pictograms accept only valid HTTPS sources and handle missing images', () => {
  for (const url of [null, '', 'javascript:alert(1)', 'data:image/svg+xml,evil', 'http://example.com/image.png']) assert.equal(safeImageUrl(url), null);
  assert.equal(safeImageUrl('https://static.arasaac.org/pictograms/1/1_500.png'), 'https://static.arasaac.org/pictograms/1/1_500.png');
});

const {createLearningState, selectAnswer, checkAnswer, advanceStep} = await import('../../main/resources/static/lesson-utils.mjs');
const {readFile} = await import('node:fs/promises');

for (const profile of ['dyslexia', 'autism']) {
  test(`complete learner journey preserves the original ${profile} backend content`, async () => {
    const content = JSON.parse(await readFile(new URL(`../../main/resources/mock/water-cycle-${profile}.json`, import.meta.url), 'utf8'));
    const original = JSON.stringify(content);
    const state = createLearningState(42, {content, displaySettings: {fontSizePx:20, showSequenceStructure:profile === 'autism'}});
    let visited = 0;
    while (!state.finished && visited < 50) {
      const step = state.steps[state.index];
      if (step.kind === 'quiz') {
        assert.equal(advanceStep(state), false, 'unanswered questions must not be completed');
        assert.equal(checkAnswer(state), false);
        selectAnswer(state, step.question.correctIndex);
        assert.equal(checkAnswer(state), true);
      }
      assert.equal(advanceStep(state), true);
      visited++;
    }
    assert.equal(state.finished, true);
    assert.equal(state.completed.size, state.steps.length);
    assert.equal(JSON.stringify(content), original, 'presentation must not change backend content');
  });
}

test('jumping ahead cannot skip unfinished reading or a changed answer', () => {
  const content = {sentences:[{text:'Material', keywords:[], section:null}], cards:[], quiz:[{question:'Q',options:['A','B'],correctIndex:1}]};
  const state = createLearningState(1, {content,displaySettings:{fontSizePx:18}});
  state.index = 2;
  selectAnswer(state, 0);
  checkAnswer(state);
  advanceStep(state);
  assert.equal(state.index, 0);
  assert.equal(state.finished, false);
  state.index = 2;
  selectAnswer(state, 1);
  assert.equal(state.completed.has(2), false);
  assert.equal(state.checked.has(2), false);
  assert.equal(advanceStep(state), false);
});

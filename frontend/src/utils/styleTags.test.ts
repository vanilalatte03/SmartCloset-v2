import { describe, expect, it } from 'vitest';

import { getEulParticle, getRoParticle } from './koreanParticles';
import {
  hasStyleTag,
  mergeStyleTags,
  normalizeStyleTags,
  parseStyleTagInput,
  removeStyleTag,
} from './styleTags';

describe('style tag utilities', () => {
  it('normalizes blank values and ASCII duplicates while preserving first display label', () => {
    expect(normalizeStyleTags([' casual ', '', 'CASUAL', '미니멀', '미니멀 '])).toEqual([
      'casual',
      '미니멀',
    ]);
  });

  it('parses comma input and merges/removes tags by normalized keys', () => {
    expect(parseStyleTagInput('casual, 미니멀, , work')).toEqual(['casual', '미니멀', 'work']);
    expect(mergeStyleTags(['casual'], ['CASUAL', 'work'])).toEqual(['casual', 'work']);
    expect(removeStyleTag(['casual', 'work'], 'CASUAL')).toEqual(['work']);
    expect(hasStyleTag(['casual', 'work'], 'WORK')).toBe(true);
  });
});

describe('Korean particle utilities', () => {
  it('selects particles from final Hangul jongseong', () => {
    expect(getRoParticle('코트')).toBe('로');
    expect(getRoParticle('셔츠')).toBe('로');
    expect(getRoParticle('가방')).toBe('으로');
    expect(getEulParticle('코트')).toBe('를');
    expect(getEulParticle('가방')).toBe('을');
  });
});

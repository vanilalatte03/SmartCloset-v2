export const maxStyleTagLength = 30;

export function getStyleTagKey(tag: string): string {
  const trimmed = tag.trim();
  return /^[\x00-\x7F]+$/.test(trimmed) ? trimmed.toLowerCase() : trimmed;
}

export function normalizeStyleTags(tags: string[]): string[] {
  const seen = new Set<string>();
  const normalized: string[] = [];

  for (const tag of tags) {
    const trimmed = tag.trim();
    if (!trimmed) {
      continue;
    }

    const key = getStyleTagKey(trimmed);
    if (seen.has(key)) {
      continue;
    }

    seen.add(key);
    normalized.push(trimmed);
  }

  return normalized;
}

export function parseStyleTagInput(input: string): string[] {
  return input.split(',').map((tag) => tag.trim()).filter(Boolean);
}

export function hasStyleTag(tags: string[], tag: string): boolean {
  const key = getStyleTagKey(tag);
  return tags.some((candidate) => getStyleTagKey(candidate) === key);
}

export function mergeStyleTags(currentTags: string[], nextTags: string[]): string[] {
  return normalizeStyleTags([...currentTags, ...nextTags]);
}

export function removeStyleTag(tags: string[], tag: string): string[] {
  const key = getStyleTagKey(tag);
  return tags.filter((candidate) => getStyleTagKey(candidate) !== key);
}

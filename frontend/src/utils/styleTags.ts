export const maxStyleTagLength = 30;

export function getStyleTagKey(tag: string): string {
  const trimmed = tag.trim();
  // ASCII 태그는 대소문자 차이를 무시하고, 한글 태그는 입력 표기를 그대로 비교한다.
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
    // 화면 표시는 사용자가 처음 입력한 trimmed 값을 유지한다.
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

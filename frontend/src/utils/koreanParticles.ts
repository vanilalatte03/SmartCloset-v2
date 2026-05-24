export function getRoParticle(text: string): '로' | '으로' {
  const lastChar = Array.from(text).at(-1);

  if (!lastChar) {
    return '로';
  }

  const code = lastChar.charCodeAt(0);
  const hangulStart = 0xac00;
  const hangulEnd = 0xd7a3;

  if (code < hangulStart || code > hangulEnd) {
    return '로';
  }

  const jongseong = (code - hangulStart) % 28;

  if (jongseong === 0 || jongseong === 8) {
    return '로';
  }

  return '으로';
}

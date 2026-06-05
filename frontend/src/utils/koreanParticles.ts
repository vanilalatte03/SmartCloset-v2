export function getRoParticle(text: string): '로' | '으로' {
  const jongseong = getLastHangulJongseong(text);

  if (jongseong === null || jongseong === 0 || jongseong === 8) {
    return '로';
  }

  return '으로';
}

export function getEulParticle(text: string): '를' | '을' {
  const jongseong = getLastHangulJongseong(text);

  if (jongseong === null || jongseong === 0) {
    return '를';
  }

  return '을';
}

function getLastHangulJongseong(text: string): number | null {
  const lastChar = Array.from(text).at(-1);

  if (!lastChar) {
    return null;
  }

  const code = lastChar.charCodeAt(0);
  const hangulStart = 0xac00;
  const hangulEnd = 0xd7a3;

  if (code < hangulStart || code > hangulEnd) {
    return null;
  }

  return (code - hangulStart) % 28;
}

# AI 분석 daily limit counter 정리

## 배경

MVP10 AI 옷 등록 보조는 OpenAI 호출 비용을 방어하기 위해 user별 in-memory daily limit을 사용한다. Redis나 영구 저장소를 추가하지 않는 결정은 유지하지만, 장기 실행 process의 local 상태도 날짜가 지날수록 무한히 늘어나지 않아야 한다.

## 문제

기존 `ClothingAnalysisDailyLimiter`는 `(userId, date)` key를 `ConcurrentHashMap`에 저장하고 요청마다 count를 증가시켰다. 날짜가 바뀐 뒤에도 과거 날짜 key를 제거하지 않아, long-running local/공유 환경에서 사용자 수와 날짜 수만큼 counter entry가 누적될 수 있었다.

## 변경

- 요청 시 `Clock` 기준 현재 날짜를 계산하고, 마지막 cleanup 날짜보다 이후 날짜면 과거 날짜 counter를 제거한다.
- cleanup은 `AtomicReference<LocalDate>` compare-and-set으로 같은 날짜에 한 번만 map scan을 수행하고, 서로 다른 날짜 cleanup 경쟁에서는 더 늦은 날짜가 다시 CAS를 시도한다.
- 같은 날짜 daily limit 판정은 기존과 동일하다.
- cleanup 완료 후 오래된 날짜 request가 경계상 뒤늦게 들어오면 마지막 cleanup 날짜 key로 다시 집계해 과거 날짜 entry와 추가 allowance가 생기지 않게 한다.

## 계약 유지

- user별 in-memory daily limit 기본값 20회를 유지한다.
- `dailyLimit < 1`이면 기존처럼 `CLOTHING_ANALYSIS_LIMIT_EXCEEDED`로 실패하고 counter를 증가시키지 않는다.
- 외부 저장소, Redis, DB schema를 추가하지 않는다.
- 분석 이미지는 저장하지 않고, 분석 결과도 옷/추천 DB row에 반영하지 않는다.

## 검증

- `ClothingAnalysisDailyLimiterTest`
  - 날짜가 바뀌면 이전 날짜 counter가 제거되고 현재 날짜 counter만 남는다.
  - 같은 날짜의 limit count와 초과 실패 동작은 유지된다.
  - `dailyLimit < 1` 설정은 counter 증가 없이 `CLOTHING_ANALYSIS_LIMIT_EXCEEDED`로 실패한다.
  - cleanup이 발생하는 날짜 전환 시 동시 요청도 현재 날짜 counter 기준으로 하나만 성공한다.
  - cleanup 이후 오래된 날짜로 관측된 요청은 마지막 cleanup 날짜 limit에 묶여 반복 성공하지 않는다.

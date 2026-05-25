# MVP5 Design Notes

## 목표

MVP5 디자인 목표는 옷장을 실제 옷 이미지 중심으로 읽히게 만들고, 추천 결과에서 상의/하의/아우터를 빠르게 식별하게 하는 것이다.

## 화면 원칙

- 기존 Today, Closet, Preferences, Location, History 앱 셸은 유지한다.
- 새 landing page를 만들지 않는다.
- 이미지 업로드는 Closet view 안에서 자연스럽게 수행한다.
- 추천 결과와 이력은 이미지가 있으면 썸네일을 우선하고, 없으면 기존 category glyph, 색상 swatch, 소재 chip으로 fallback한다.
- 이미지 업로드 여부를 추천이 더 좋아지는 것처럼 표현하지 않는다.

## Closet

- 옷 카드에는 고정 비율 썸네일 영역을 둔다.
- 이미지가 없는 카드는 category visual과 색상 swatch가 같은 영역을 채운다.
- 등록/수정 form에서는 파일 선택, 로컬 미리보기, 교체, 삭제 상태가 분명해야 한다.
- 업로드 실패는 옷 정보 저장 실패와 분리해서 안내한다.

## Recommendation

- outfit slot 카드에서 이미지가 있으면 가장 먼저 보인다.
- color swatch와 material chip은 이미지가 있어도 계속 보조 정보로 유지한다.
- 이미지가 없어서 추천이 실패한 것처럼 보이면 안 된다.

## History

- 추천 이력은 현재 옷 이미지 상태를 보여준다.
- 삭제된 이미지는 fallback visual로 표시한다.
- 상세 이유/점수보다 추천 옷 조합 식별이 먼저 보여야 한다.

## Mobile

- 375px 너비에서 썸네일, 옷 이름, CTA가 겹치지 않아야 한다.
- hover에 의존하는 업로드/삭제 액션을 만들지 않는다.
- 썸네일 로딩 중에도 카드 높이가 크게 변하지 않도록 placeholder를 둔다.

## 제외

- 다중 이미지 carousel
- 이미지 크롭/편집
- 카메라 직접 촬영
- drag-and-drop 전용 흐름
- AI 자동 태깅 UI
- 이미지 기반 추천 이유 UI

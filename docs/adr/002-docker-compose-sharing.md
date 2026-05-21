# Share MVP with Docker Compose

## Status
Accepted

## Context
SmartCloset 1차 MVP는 Spring Boot 애플리케이션과 MySQL을 함께 실행해야 한다. 다음 주 공유 가능성을 높이려면 실행 환경을 쉽게 재현할 수 있어야 한다.

AWS 수동 배포나 CD 자동화는 인프라 변수, 계정 설정, 네트워크 설정, 배포 실패 대응 비용이 크다. 1차 MVP의 핵심은 추천 백엔드와 API 계약을 검증하는 것이다.

## Decision
1차 MVP 공유 방식은 Docker Compose로 고정한다.

필수 제공 대상은 다음과 같다.

- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `README.md`
- seed data

README에는 Docker Compose 실행 방법, seed user 정보, seed data 설명, Swagger 접속 경로, P1 Demo UI 구현 시 데모 UI 접속 경로, 공유용 테스트 시나리오를 포함한다.

## Consequences
- Spring Boot + MySQL 실행 환경을 재현하기 쉬워진다.
- 공유 대상자는 로컬에서 같은 조건으로 서비스를 검증할 수 있다.
- AWS 수동 배포 리스크를 1차 MVP에서 제거한다.
- CD 자동화는 1차 MVP 범위에서 제외한다.
- AWS 배포와 CD 자동화는 후속 MVP 후보로 이동한다.

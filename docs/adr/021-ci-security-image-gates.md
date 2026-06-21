# CI 보안 스캔과 이미지 게이트

## 상태

Accepted

## 배경

운영 준비 이슈 #198은 기존 PR CI의 테스트와 빌드만으로는 dependency 취약점, production image build 실패, image 취약점을 merge 전에 차단하지 못한다는 위험을 다룬다. #204에서 app/frontend production runtime 산출물이 분리됐으므로 CI가 두 image를 모두 빌드하고 스캔할 수 있다.

## 결정

- PR CI는 기존 `./gradlew test`, frontend build, `./gradlew build`를 유지한다.
- Frontend dependency는 `npm audit --audit-level=high`로 high 이상 취약점을 차단한다.
- Trivy filesystem scan은 frontend dependency tree를 `HIGH,CRITICAL` severity 기준으로 검사한다.
- CI는 backend app image와 frontend Nginx static image를 각각 빌드한다.
- Trivy image scan은 두 image의 OS package와 library 취약점을 `HIGH,CRITICAL` severity 기준으로 검사한다. Backend runtime library 취약점은 app image의 `app.jar` 분석 결과로 차단한다.
- Trivy scan은 `ignore-unfixed=true`를 사용해 현재 fix가 없는 항목은 CI 차단 대신 추적 대상으로 둔다.
- 현재 scan suppression file은 두지 않는다. 예외가 필요하면 취약점 ID, 영향 범위, 만료일, 보완 통제를 문서화한 뒤 별도 변경으로 추가한다.
- App image base는 `eclipse-temurin:21-jdk-noble`/`21-jre-noble`로 고정해 Ubuntu LTS runtime과 재현 가능한 image scan 대상을 유지한다.
- Dockerfile은 runtime package 설치 전에 `apt-get upgrade -y`를 실행해 base image에 이미 fix가 배포된 OS package 취약점을 CI scan 전에 반영한다.
- Frontend image base는 `nginx:1.29-alpine`으로 올리고 `apk upgrade --no-cache`를 실행해 Alpine base image의 fixed OS package를 CI scan 전에 반영한다.
- Netty와 embedded Tomcat은 현재 CI 보안 게이트의 high/critical library 취약점 fix 버전으로 Gradle resolution strategy에서 고정한다.

## 결과

- PR에서 dependency vulnerability scan, Docker image build, image vulnerability scan이 함께 검증된다.
- Fix 가능한 high/critical 취약점은 CI 실패로 드러난다.
- Fix가 없는 취약점은 CI를 막지 않지만, 운영 판단이 필요한 항목으로 별도 추적해야 한다.
- Spring Boot 4.0.6 baseline과 MVP10 API/DB/추천 계약은 변경하지 않는다.

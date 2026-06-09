# 옷 이미지 트랜잭션 정리 성능 기록

## 문서 목적
이 문서는 옷 이미지 업로드, 이미지 삭제, 계정 삭제에서 파일 시스템 정리를 DB transaction commit 이후로 분리한 기록이다.

이 문서는 ADR이 아니며 공개 API, DB schema, 추천 점수 규칙을 변경하지 않는다. 관련 GitHub Issue는 `#154`다.

## 문제
파일 시스템 삭제는 DB transaction처럼 rollback되지 않는다.

기존 흐름은 DB metadata 변경을 `flush()`한 뒤 같은 transaction 안에서 이전 이미지 파일을 삭제했다. `flush()`는 SQL 실행 성공을 확인할 수 있지만 transaction commit 성공을 보장하지 않는다. 이후 rollback 또는 commit 실패가 발생하면 DB는 기존 `image_stored_filename`을 유지하는데 실제 파일만 사라지는 불일치가 생길 수 있다.

계정 삭제도 같은 위험을 가진다. 사용자 소유 row 삭제가 commit되기 전에 이미지 파일을 먼저 삭제하면, 삭제 transaction이 rollback된 뒤 계정과 옷 metadata는 남아 있는데 이미지 bytes만 사라질 수 있다.

## Before
기존 구조의 위험 요소는 다음과 같았다.

- 이미지 교체는 새 파일을 저장하고 DB metadata를 `flush()`한 뒤 이전 파일을 즉시 삭제했다.
- 이미지 삭제는 저장소 파일을 즉시 삭제한 뒤 DB metadata를 clear했다.
- 계정 삭제는 사용자 소유 DB row를 `flush()`한 뒤 commit 전에 이미지 파일을 삭제했다.
- 새 파일 저장 후 DB metadata 갱신이 commit 단계에서 rollback되면 새 파일 orphan을 정리할 보상 경로가 부족했다.

## After
개선 후 파일 정리 전략은 transaction 결과 기준이다.

1. 이미지 업로드/교체는 새 파일을 먼저 저장한다.
2. 새 파일은 transaction rollback 시 삭제되도록 `afterCompletion(ROLLED_BACK)`에 보상 cleanup을 등록한다.
3. DB metadata를 새 파일로 갱신하고 `flush()` 실패 시 새 파일을 즉시 한 번 cleanup한다.
4. 기존 파일 삭제는 `afterCommit`에 등록해 DB commit 이후에만 실행한다.
5. 이미지 삭제는 DB metadata clear와 `flush()`를 먼저 수행하고, 기존 파일 삭제는 `afterCommit`에 등록한다.
6. 계정 삭제는 사용자 소유 DB row 삭제와 `flush()`를 먼저 수행하고, 사용자 이미지 파일 삭제는 `afterCommit`에 등록한다.
7. cleanup 실패는 warn log로 남기며 이미 성공한 DB transaction 결과를 뒤집지 않는다.

이 방식은 commit 전 파일 삭제로 인한 metadata/file 불일치를 막고, rollback 시 새로 저장된 파일 orphan을 줄인다.

## 성능 영향
DB transaction 안에서 실행되던 파일 삭제 I/O가 commit 이후 callback으로 이동했다.

이 변경은 DB lock 보유 시간 안에서 수행되는 파일 삭제 작업을 줄인다. 로컬 파일 삭제 자체는 보통 짧지만, 후속 storage adapter가 S3 같은 remote I/O로 바뀌더라도 application service의 DB transaction은 storage cleanup 성공 여부에 묶이지 않는다.

다만 after-commit cleanup은 request 처리 흐름 안에서 실행될 수 있다. 그래서 cleanup 실패는 예외를 전파하지 않고 로그로만 남긴다. 실제 파일 삭제가 느려지는 storage adapter를 도입할 때는 현재 `ClothingImageStorage` 경계 뒤에서 비동기 cleanup queue나 retry 정책을 추가할 수 있다.

## 회귀 방지 기준
이미지 파일 정리에서는 다음 기준을 지킨다.

- 기존 파일 삭제는 DB commit 이후에만 실행한다.
- 새 파일 저장 후 DB 변경이 rollback되면 새 파일을 cleanup한다.
- `flush()` 성공을 commit 성공으로 간주하지 않는다.
- cleanup 실패가 DB transaction 성공 응답을 실패로 바꾸지 않게 한다.
- 계정 삭제 이미지 cleanup도 사용자 row hard delete commit 이후에만 실행한다.

## 확인한 테스트
이번 변경은 다음 회귀 테스트를 추가했다.

- 이미지 교체 rollback 시 기존 파일은 보존되고 새 파일은 삭제된다.
- 이미지 교체 commit 시 기존 파일은 삭제되고 새 파일은 유지된다.
- 이미지 삭제 rollback 시 기존 파일과 metadata는 보존된다.
- 이미지 삭제 commit 시 metadata와 기존 파일이 모두 제거된다.
- 계정 삭제 rollback 시 사용자/옷 metadata와 이미지 파일이 보존된다.
- 계정 삭제 commit 시 사용자 row와 이미지 파일이 제거된다.

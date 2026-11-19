# ADR 003. 감사 대상 관리와 로그 이관

## 배경

직원 권한을 변경하거나 감사 이력을 내려받았을 때 누가 어떤 작업을 했는지 추적할 수 있어야 했습니다. 여러 Controller에서 같은 형식으로 기록하면서도 업무 코드마다 로그 작성 로직을 반복하지 않는 방법이 필요했습니다.

## 고려한 방법

먼저 각 메서드에 `@Auditable`을 붙이는 방식을 고려했습니다. 메서드를 읽을 때 감사 대상인지 바로 알 수 있지만, 전체 목록을 확인하거나 바꾸려면 여러 Controller를 찾아야 합니다.

저장 방식으로는 요청을 처리할 때 감사 DB에 바로 쓰는 방법이 있습니다. 구조는 간단하지만 사용자 요청이 끝나려면 매번 감사 DB 쓰기까지 기다려야 합니다.

## 선택한 방법

Controller에는 AOP를 적용하고 `AuditLogPolicyRegistry` 한 곳에서 HTTP 메서드와 URI 패턴, 기록할 동작, 대상 ID 추출 규칙을 관리하도록 했습니다. 필터에서 끝나는 로그인은 성공 핸들러와 실패 핸들러에서 기록합니다.

이벤트는 먼저 Redis 리스트에 넣고 앱 내부 Scheduler가 감사 전용 PostgreSQL DB로 옮깁니다. Redis에 넣지 못하면 같은 감사 DB의 `audit_fallback_log`에 원본 JSON을 저장합니다.

## 선택한 이유

수집 정책을 한 곳에 모아 API를 추가할 때 기록할 행위와 대상 ID를 함께 검토할 수 있습니다. 정상 요청은 Redis에 기록한 뒤 응답할 수 있고, 감사 DB 쓰기는 Scheduler에서 일괄 처리합니다. 별도 작업 서버를 운영할 필요가 없지만 앱이 실행 중이어야 로그가 이관됩니다.

업무 DB에는 직원과 권한을, 감사 DB에는 행위 당시의 사번, 역할, 대상 식별자를 저장했습니다. 두 DB의 DataSource와 트랜잭션 매니저를 분리해 감사 이력을 조회할 때 업무 테이블의 외래 키에 의존하지 않도록 했습니다. 로컬 Compose에서는 두 DB가 PostgreSQL 컨테이너 하나를 공유합니다.

## 적용 범위와 한계

- API 경로가 바뀌면 Registry도 함께 수정해야 합니다. 필터에서 거부된 업무 요청은 Controller에 도달하지 않아 AOP로 기록할 수 없습니다.
- DB 저장 후 Redis `trim` 전에 작업이 멈추면 같은 이벤트를 다시 저장할 수 있습니다. `trace_id`에는 일반 인덱스만 있습니다. 여러 Scheduler가 같은 구간을 읽고 각각 큐를 정리하면 아직 저장하지 않은 이벤트까지 삭제할 수 있습니다.
- Redis 적재가 실패하면 사용자 요청 안에서 감사 DB에 쓰기를 시도합니다. 직렬화에 실패하거나 Redis와 감사 DB가 함께 실패하면 원본을 잃을 수 있습니다. 업무 변경과 감사 기록도 한 트랜잭션에서 커밋하지 않습니다.
- 같은 주기에 Redis 이관이 예외로 끝나면 fallback 작업도 건너뜁니다. 잘못된 이벤트 하나가 배치 전체의 재처리를 반복해서 막을 수 있습니다.

먼저 이벤트 식별자로 중복 저장을 막고 한 소비자만 같은 큐 구간을 처리하도록 보완할 필요가 있습니다. 이후 두 이관 작업을 따로 재시도하고 반복 실패한 이벤트를 분리하는 방법을 검토할 수 있습니다.

## 관련 코드

- HTTP 메서드와 경로에 따라 감사 대상을 판별합니다. [AuditLogPolicyRegistry](../../src/main/java/office_mate_2605/management/audit_log/policy/AuditLogPolicyRegistry.java)
- 큐와 fallback에 저장한 로그를 이관합니다. [AuditLogFlushScheduler](../../src/main/java/office_mate_2605/management/audit_log/batch/AuditLogFlushScheduler.java)

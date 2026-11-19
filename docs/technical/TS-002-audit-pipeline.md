# TS 002. 감사 로그 수집과 이관

## 감사 대상 판별

관리자 API를 처리할 때 Controller마다 감사 코드를 추가하지 않도록 `AuditLogAspect.writeAuditLog`로 Controller 메서드를 감쌌습니다. HTTP 요청이 있으면 `AuditLogPolicyRegistry.find`에서 메서드와 URI에 맞는 첫 정책을 찾습니다. `AuditLogPolicy.matches`는 `AntPathMatcher`로 경로를 비교하고, `resolveTargetId`는 정책에서 지정한 경로 변수의 값을 꺼냅니다. 직원 등록처럼 경로에 ID가 없으면 `target_id`는 비어 있을 수 있습니다.

| 대상 | 기록하는 값 |
| --- | --- |
| 직원 등록 / 관리 정보 수정 | 대상은 `EMPLOYEE`이며 동작은 각각 `CREATE`, `PERMISSION_CHANGE`입니다. 관리 정보 수정 시 경로의 `employeeNo`를 기록합니다. |
| 상태 신청 승인 또는 반려 | 동작은 `STATUS_CHANGE`, 대상은 `EMPLOYEE_STATUS_REQUEST`이며 경로의 `requestId`를 기록합니다. |
| 감사 로그 CSV 내보내기 | `AUDIT_LOG`에 대한 `EXPORT`로 기록합니다. 일반 목록 조회는 수집 대상에서 제외했습니다. |
| 로그인 | 로그인 필터의 핸들러에서 `AuditLoginLogService.recordSuccess` 또는 `recordFail`을 호출해 `LOGIN` 이벤트를 만듭니다. |

Controller 호출 직전에 발생 시각을 기록합니다. 메서드가 정상 반환하면 `SUCCESS` 이벤트를 생성해 적재합니다. Controller 호출과 성공 이벤트 적재는 같은 `try` 블록에서 실행합니다. 따라서 업무 처리가 끝났더라도 감사 기록 중 발생한 예외가 요청에 전파될 수 있습니다. 예외를 잡은 Aspect는 최대 500자의 사유를 담은 `FAIL` 이벤트 적재를 시도한 뒤 예외를 다시 던집니다. 각 이벤트에는 UUID인 `traceId`, 행위자의 사번과 역할, 동작과 대상, URI, HTTP 메서드, 클라이언트 IP, User-Agent를 담습니다. [감사 Aspect](../../src/main/java/office_mate_2605/management/audit_log/aspect/AuditLogAspect.java)

필터에서 거부된 업무 요청은 Controller까지 도달하지 않습니다. 로그인 성공 핸들러에서 휴직이나 퇴사를 확인해 먼저 반환하는 경우에도 로그인 감사 이벤트가 생성되지 않습니다.

## Redis 적재와 fallback 저장

`AuditLogRedisService.append`에서 `rightPush`로 이벤트를 `audit:log:queue` 끝에 넣습니다. 예외가 발생하면 `AuditLogFallbackService.saveFallback`에서 이벤트를 JSON으로 직렬화해 감사 DB의 `audit_fallback_log.payload`(jsonb)에 저장하고, 실패 원인을 최대 300자로 남깁니다.

fallback 메서드 내부에서 직렬화나 저장에 실패하면 오류를 로그에 남깁니다. 그 뒤에 원본을 보관할 저장소는 없습니다. 트랜잭션 시작이나 커밋 단계에서 발생한 예외도 메서드 내부의 catch 범위를 벗어날 수 있습니다.

## Scheduler의 이관 순서와 완료 기준

`AuditLogFlushScheduler.flushAuditLogs`는 직전 실행이 끝난 뒤 5분 후에 다시 실행합니다(기본 설정 300,000ms). 한 번에 Redis 큐에서 최대 100건을 처리한 뒤 fallback 테이블에서 최대 100건을 처리합니다. 큐가 비어 있으면 fallback으로 넘어갑니다. 두 호출이 같은 try 안에 있으므로 Redis 이관이 예외로 끝나면 그 주기의 fallback 이관도 건너뜁니다.

| 단계 | Redis 큐 ([AuditLogFlushService.flush](../../src/main/java/office_mate_2605/management/audit_log/service/AuditLogFlushService.java)) | fallback 테이블 (`AuditLogFallbackFlushService.flush`) |
| --- | --- | --- |
| 조회 | `range(0, batchSize - 1)`로 큐 앞부분을 읽고 원본을 남겨 둡니다. | `processed_at IS NULL`인 행을 `id ASC` 순으로 읽습니다. |
| 저장 | 이벤트를 DTO로 변환하고 `AuditLogJdbcRepository.saveAll`에서 `auditJdbcTemplate.batchUpdate`로 INSERT합니다. | JSON을 DTO로 복원한 뒤 같은 `saveAll`을 호출합니다. |
| 완료 | INSERT 후 `trim(events.size(), -1)`으로 큐를 정리한 뒤 처리 건수를 반환합니다. | `auditTxManager` 트랜잭션 안에서 INSERT와 `markProcessed(ids)`를 실행합니다. 커밋 후 `processed_at`에 시각이 남습니다. |
| 실패 | 변환 또는 INSERT 실패로 trim 전에 중단되면 큐에 남은 이벤트를 다음 실행에서 다시 읽습니다. | 역직렬화나 쓰기 실패로 롤백되면 다음 실행에서 미처리 행을 다시 읽습니다. |

`occurred_at`은 이벤트가 발생한 시각이고 `flushed_at`은 감사 테이블에 적재한 시각입니다. fallback 작업의 반환값은 선택한 ID 개수입니다. `markProcessed`가 실제로 갱신한 행 수와 일치하는지는 검사하지 않습니다.

## 감사 이력 조회

`AuditLogRestController.searchAuditLogs`에서 SUPER가 동작, 결과, 사번, 기간을 기준으로 검색하고 페이지별로 조회하도록 했습니다. 상세 모달에는 선택한 행의 데이터를 표시합니다. `exportAuditLogsCsv`는 같은 검색 조건에 맞는 로그를 발생 시각이 최신인 순서로 최대 5,000건까지 CSV로 반환합니다. 큐에 대기 중인 이벤트는 감사 DB에 이관된 뒤부터 조회할 수 있습니다. [CSV 조회 한도](../../src/main/java/office_mate_2605/management/audit_log/service/AuditLogQueryService.java)

## 재처리할 때의 한계

- INSERT 성공 후 trim에 실패하면 다음 실행에서 같은 이벤트를 다시 저장할 수 있습니다. 두 소비자가 같은 구간을 읽고 각각 trim하면 뒤에 대기하던 이벤트까지 삭제할 수 있습니다.
- fallback 조회에는 행을 선점하는 잠금이 없고 이벤트 ID에도 유일 제약이 없습니다. 따라서 여러 앱 인스턴스가 같은 미처리 행을 동시에 선택할 수 있습니다.
- 재시도 횟수 제한, 지수 백오프, 반복 실패한 레코드의 분리 처리는 구현하지 않았습니다. 큐 앞의 이벤트를 변환하지 못하면 다음 주기에도 같은 배치가 멈출 수 있습니다. Redis와 fallback 저장이 함께 실패하면 재처리할 원본이 남지 않을 수 있습니다.

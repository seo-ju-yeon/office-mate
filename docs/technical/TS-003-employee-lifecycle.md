# TS 003. 직원 상태와 계정 접근 관리

휴직한 직원이 복직을 신청하면 SUPER가 승인할 때 재직 상태를 `ACTIVE`로 바꿉니다. SUPER는 직원 관리 API에서 재직 상태를 직접 변경할 수도 있습니다. 로그인 실패로 잠긴 계정은 관리자가 별도로 잠금을 해제해야 합니다. 그래서 직원 정보와 재직 상태는 `employee`에, 로그인 실패 횟수와 잠금 상태는 사번을 PK와 FK로 사용하는 `account_security_status`에 나눠 저장했습니다. 역할 매핑은 `employee_roles`, 상태 신청과 결정 이력은 `employee_status_request`에서 관리합니다.

## 직원 등록과 정보 수정

| 작업 | 권한 | 처리 방식 |
| --- | --- | --- |
| 등록 | ADMIN은 USER만, SUPER는 모든 역할을 등록할 수 있습니다. | `EmployeeService.createEmployee`에서 사번과 이메일의 앞뒤 공백을 제거하고 각각 대문자와 소문자로 바꿉니다. 중복 확인을 거쳤는지와 실제 DB 중복 여부를 검사한 뒤 직원과 역할 매핑을 한 트랜잭션에서 저장합니다. 매핑에는 부여자의 사번도 기록합니다. |
| 조회 | 조직도는 인증된 직원, 관리 목록과 상세는 ADMIN과 SUPER가 조회합니다. | 관리 목록에는 삭제되지 않은 직원을 표시합니다. `getOrganizationEmployees`에서는 그중 `ACTIVE`와 `ON_LEAVE`만 조회합니다. |
| 기본 정보 수정 | ADMIN, SUPER | `updateEmployee`에서 이름, 이메일, 부서, 직급을 변경하고 다른 직원의 이메일과 중복되는지 확인합니다. 역할 변경을 함께 요청하면 거절합니다. |
| 관리 정보 수정 | SUPER | `updateEmployeeManagementInfo`에서 역할이 바뀌면 기존 매핑을 지우고 새 역할과 부여자를 저장합니다. 재직 상태도 직접 변경할 수 있으며, 비활성 상태로 바꾸면 Refresh Token을 폐기합니다. |
| 직접 퇴사 또는 논리 삭제 | ADMIN, SUPER | `resignEmployee`는 퇴사일을, `deleteEmployee`는 `deleted_at`에 삭제 시각을 기록합니다. 두 경우 모두 미폐기 Refresh Token을 폐기합니다. |
| 잠금 계정 관리 | ADMIN, SUPER | `AccountSecurityController`에서 목록과 잠금 해제 API를 제공합니다. 직원 관리 API에서도 잠금을 해제할 수 있습니다. |

신규 `Employee`는 `ACTIVE`, `temp_password_required=true`로 등록합니다. 공통 초기 비밀번호는 BCrypt로 저장하고 첫 로그인 후 비밀번호를 바꿀 때까지 업무 API 접근을 제한했습니다. 직원별 일회용 초기 비밀번호를 발급하면 공통 초기값을 사용하는 문제를 줄일 수 있습니다.

관리 정보를 직접 바꾸거나 퇴사, 삭제, 상태 신청 승인으로 마지막 활성 SUPER가 사라질 수 있는 경우에는 삭제되지 않은 `ACTIVE SUPER`의 수를 먼저 확인합니다. [EmployeeService](../../src/main/java/office_mate_2605/common/employee/service/EmployeeService.java)

## 휴직, 복직, 퇴사 신청

| 유형 | 신청 조건 | 승인 결과 |
| --- | --- | --- |
| `LEAVE` | 재직 직원이 `POST /api/my/status-requests`로 본인의 휴직을 신청합니다. | `ACTIVE`에서 `ON_LEAVE`로 바꾸고 Refresh Token을 폐기합니다. |
| `RETURN_FROM_LEAVE` | 휴직자는 일반 로그인이 차단되므로 공개 경로 `POST /api/auth/return-from-leave/request`에서 사번과 비밀번호를 확인합니다. 삭제되지 않은 `ON_LEAVE` 직원만 신청할 수 있습니다. | `ON_LEAVE`에서 `ACTIVE`로 바꿉니다. 이미 폐기된 Refresh Token은 복원하지 않습니다. |
| `RESIGN` | 로그인한 직원이 본인 명의로 신청합니다. 서비스는 퇴사한 직원의 신청을 거절하며, 휴직자의 해당 API 접근은 필터에서 차단합니다. | `RESIGNED`로 바꾸고 승인일을 퇴사일로 기록하며 Refresh Token을 폐기합니다. |

1. `EmployeeStatusRequestService.createRequest`와 `createReturnFromLeaveRequest`에서 같은 직원의 `PENDING` 신청을 조회합니다. 대기 중인 신청이 있으면 종류와 관계없이 추가 접수를 거절합니다. 접수 시점에는 신청만 저장합니다.
2. SUPER가 대기 목록에서 승인하면 `approveRequest`에서 대기 상태, 처리자, 대상 직원의 현재 상태와 마지막 활성 SUPER 조건을 다시 확인합니다. 승인 이력 저장, 직원 상태 변경, 필요한 토큰 폐기를 한 트랜잭션에서 처리합니다.
3. 반려하면 `rejectRequest`가 신청만 `REJECTED`로 바꿉니다. 직원 상태와 토큰은 유지합니다. 승인과 반려 모두 `EmployeeStatusRequest`에 처리자, 시각, 의견을 남기며 처리한 신청은 대기 목록에서 제외합니다.

신청 중복 여부와 마지막 SUPER의 수는 변경 전에 조회합니다. 직원별 `PENDING` 유일 제약이나 동시 심사를 위한 조회 잠금은 적용하지 않았으므로, 신청이나 승인이 동시에 들어올 때의 결과는 별도 검증이 필요합니다. [신청 서비스](../../src/main/java/office_mate_2605/common/employee/service/EmployeeStatusRequestService.java)

## 계정 잠금과 해제

`AccountSecurityStatusService.recordLoginFailure`에서 삭제되지 않은 직원의 보안 상태 행을 찾거나 새로 만듭니다. 로그인 실패 횟수와 마지막 실패 시각을 기록하고 5회 이상이면 계정을 잠그고 최초 잠금 시각을 남깁니다. 로그인에 성공하면 잠기지 않은 계정의 실패 횟수는 초기화합니다. 잠긴 계정은 시간이 지나도 자동으로 해제되지 않습니다.

`unlockAccount`에서 처리자가 ADMIN 또는 SUPER인지 확인한 뒤 잠금 상태, 실패 횟수, 관련 시각을 초기화합니다. `unlocked_by`와 `unlocked_at`에는 처리자와 해제 시각을 기록합니다. 잠금은 `employee.status`와 Refresh Token을 바꾸지 않습니다. [보안 상태 서비스](../../src/main/java/office_mate_2605/management/auth/service/AccountSecurityStatusService.java)

## 변경 전 발급한 토큰의 처리

| 변경 사항 | 다음 보호 API 요청 | Refresh Token |
| --- | --- | --- |
| 휴직 또는 퇴사 | 로그아웃을 제외하고 423 `ACCOUNT_INACTIVE`를 반환합니다. | 상태를 변경할 때 폐기합니다. |
| 논리 삭제 | 삭제되지 않은 직원 조회에 실패합니다. 최종 HTTP 응답 형식은 통합 검증이 필요합니다. | 삭제할 때 폐기합니다. |
| 로그인 실패 잠금 | 로그아웃을 제외하고 423 `ACCOUNT_LOCKED`를 반환합니다. | 잠금으로 폐기되지 않으며 재발급 필터에서도 잠금을 확인하지 않습니다. |
| 복직 또는 잠금 해제 | Access Token이 아직 유효하면 현재 상태로 다시 평가합니다. | 복직해도 폐기한 행은 복원하지 않습니다. 잠금 해제도 토큰 행을 바꾸지 않습니다. |
| 역할만 변경 | DB의 최신 역할로 메서드 권한을 평가합니다. | 폐기하지 않습니다. |

공개 복직 신청에서 비밀번호를 확인할 때는 로그인 실패 누적과 잠금 처리를 호출하지 않습니다. 이 경로에 적용할 잠금 정책도 추가 검토가 필요합니다.

# ADR 001. 기존 Access Token에 최신 계정 상태 반영

## 배경

직원이 로그인한 뒤 휴직하거나 계정이 잠겨도, 발급 당시 JWT에 담은 정보(claim)는 바뀌지 않습니다. 관리자 역할을 회수할 때도 마찬가지입니다. 인사 변경이 끝나면 다음 업무 요청부터 새 권한을 적용해야 했습니다.

## 고려한 방법

JWT의 서명과 만료만 검사하면 인증할 때마다 DB에 접근할 필요가 없습니다. 다만 이 방식에서는 토큰의 남은 유효기간 동안 이전 권한이 그대로 남습니다. 다른 방법은 토큰으로 사용자를 식별한 뒤 업무 DB에서 현재 상태를 읽는 것입니다.

## 선택한 방법

저는 보호 API를 호출할 때 업무 DB에서 최신 계정 상태를 읽도록 했습니다. `TokenCheckFilter.doFilterInternal`에서 서명과 만료를 확인한 후 사번(`employeeNo`)으로 `APIUserDetailsService.loadUserByUsername`을 호출합니다. 삭제되지 않은 직원의 기본 역할, 추가 역할, 잠금 상태로 `EmployeePrincipal`을 구성합니다. 재직 상태, 잠금, 임시 비밀번호 검사를 통과하면 DB에서 읽은 권한을 현재 요청에 적용합니다.

## 선택한 이유

휴직 승인과 권한 변경의 기준은 업무 DB에 있습니다. 예를 들어 SUPER가 휴직을 승인하면 해당 트랜잭션에서 직원 상태가 `ON_LEAVE`로 바뀝니다. 커밋 후 들어오는 요청은 이 값을 읽으므로 토큰 만료까지 기다릴 필요가 없습니다. 역할이 바뀐 경우에도 `@PreAuthorize`가 최신 권한으로 접근을 판단합니다.

## 적용 범위와 한계

- `SessionCreationPolicy.STATELESS`로 로그인 세션을 저장하지 않도록 했습니다. 보호 API는 요청마다 직원, 역할 매핑, 잠금 상태를 읽으므로 DB의 응답 지연과 장애에 영향을 받습니다. 조회 비용은 부하 시험으로 측정하지 않았습니다.
- 화면과 공개 API는 이 검사에서 제외했습니다. 잠기거나 비활성인 계정도 유효한 토큰으로 로그아웃할 수 있습니다. 논리 삭제한 직원은 조회 단계에서 제외되며, 토큰 필터에는 이 경우를 위한 별도 오류 응답이 없습니다.
- 복직하거나 잠금을 해제하면 아직 만료되지 않은 Access Token으로 다시 접근할 수 있습니다. 로그아웃과 비밀번호 변경도 Access Token을 개별 폐기하지 않습니다. 세션별 즉시 폐기가 필요해지면 토큰 버전이나 세션 식별자를 검사하는 방식과 조회 비용을 함께 검토할 수 있습니다.

## 관련 코드

- 요청 검사와 사용자 조회에 사용한 클래스입니다. [TokenCheckFilter](../../src/main/java/office_mate_2605/security/filter/TokenCheckFilter.java), [APIUserDetailsService](../../src/main/java/office_mate_2605/security/APIUserDetailsService.java)

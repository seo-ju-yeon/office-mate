# ADR 002. Refresh Token의 보관과 폐기

## 배경

업무 API에는 유효기간이 30분인 Access Token을 사용했습니다. 로그인 상태를 연장하는 Refresh Token은 30일간 유효하므로 브라우저에서 원문이 노출되는 범위를 줄이고, 서버에서도 재발급을 중단할 방법이 필요했습니다.

## 고려한 방법

처음에는 두 토큰을 모두 JSON으로 내려주고 브라우저의 localStorage에 보관했습니다. 읽고 쓰기는 간단했지만 페이지의 JavaScript가 장기 토큰까지 읽을 수 있었습니다. 그래서 두 토큰의 보관 위치를 나눴습니다.

## 선택한 방법

| 구분 | Access Token | Refresh Token |
| --- | --- | --- |
| 유효기간과 전달 | 30분입니다. JSON으로 반환하고 브라우저가 localStorage에 저장한 뒤 Authorization 헤더로 보냅니다. | 30일입니다. `refreshToken` HttpOnly 쿠키로 전달하며 경로는 `/api/auth`, SameSite는 Lax입니다. |
| 서버 검증 | 서명, 만료, 최신 계정 상태를 확인합니다. | 서명과 만료를 검사한 뒤 사번과 SHA-256 해시가 일치하고 `revoked_at IS NULL`이며 `expires_at > now()`인 행을 찾습니다. |
| 재발급 | 유효한 Refresh Token이 있으면 새로 발급합니다. | 남은 기간이 2일 이하일 때만 새 30일 토큰으로 교체합니다. |

로그인하거나 Refresh Token을 교체할 때 `saveRefreshToken`에서 같은 사번의 기존 토큰을 폐기한 뒤 새 해시를 저장하도록 했습니다. 두 작업은 같은 메인 DB 트랜잭션에서 처리합니다. 로그아웃, 비밀번호 변경 또는 재설정, 휴직, 퇴사, 논리 삭제 시에도 미폐기 토큰을 폐기합니다. 로그아웃 응답에서는 같은 경로의 쿠키를 `Max-Age=0`으로 지웁니다.

## 선택한 이유

HttpOnly를 사용하면 페이지 스크립트가 Refresh Token 원문을 읽을 수 없습니다. 서버는 요청에 담긴 토큰을 다시 해시해 비교하므로 DB에도 원문을 저장할 필요가 없습니다. 만료가 가까울 때만 토큰을 교체해 평소 재발급에서는 DB 쓰기와 쿠키 재설정을 줄였습니다. 교체 전까지는 같은 Refresh Token을 여러 번 사용할 수 있습니다.

## 보안상 한계와 개선할 점

- 쿠키의 `Secure` 기본값은 `false`입니다. HTTPS 배포 시에는 `app.auth.refresh-cookie-secure=true`로 설정해야 합니다. 브라우저는 HttpOnly 쿠키도 자동 전송합니다. 현재 재발급 경로에는 HTTP 메서드 제한이 없고 CSRF 검사는 비활성화돼 있습니다. localStorage의 Access Token은 JavaScript로 읽을 수 있습니다.
- 두 JWT는 같은 서명 키와 claim 구조를 사용합니다. Access Token 검사 시 용도를 구분하는 claim도 확인해야 합니다. 서로 다른 용도의 토큰을 거부하는 테스트가 필요합니다.
- 계정 잠금만으로 Refresh Token을 폐기하지는 않습니다. 재발급 필터는 토큰 테이블을 확인하고 기존 claim을 복사합니다. 잠금 여부는 다음 보호 API에서 확인하므로 잠긴 계정의 재발급까지 제한하려면 재발급 시점에도 상태를 검사해야 합니다.
- 사번 단위로 폐기하므로 새 기기에서 로그인하면 이전 기기의 재발급도 중단됩니다. 기기별 세션과 폐기 토큰의 재사용 탐지는 구현하지 않았습니다. `token_hash`의 유일 제약은 같은 해시의 중복 삽입을 막지만 동시 발급 시 사번별 활성 행 하나를 보장하지는 않습니다.

## 관련 코드

- Refresh Token 쿠키의 옵션을 설정합니다. [쿠키 속성](../../src/main/java/office_mate_2605/security/cookie/RefreshTokenCookieProvider.java)
- 재발급 조건과 DB 해시를 확인합니다. [검증과 교체](../../src/main/java/office_mate_2605/security/filter/RefreshTokenFilter.java), [저장과 조회 SQL](../../src/main/java/office_mate_2605/security/service/RefreshTokenService.java)

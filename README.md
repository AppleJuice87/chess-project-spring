# ReadME

# version
- jdk : Oracle OpenJDK 22.0.1
- MySql : 9.0.1 (8버전 이상 사용가능)

# Test
Front 테스트하실때 `src/main/resources/application.properties` 수정해서 사용해주시면 됩니다.

<br/>

- 처음 실행 시 Mysql 에서 스키마를 먼저 생성해 주어야 합니다.
- your_db_name 부분을 생성한 db 이름으로 바꿔주시고,
- 아래의 ${} 부분에 MySQL id pw 직접 넣어주시거나, 환경변수로 등록해서 사용해주시면 됩니다.

```
spring.datasource.url=jdbc:mysql://localhost:3306/your_db_name
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```
<br>

- 처음 실행 시 아래처럼 `create`로 설정하셔야 설정한 your_db_name db에 자동으로 테이블이 만들어집니다.
```
spring.jpa.hibernate.ddl-auto=create
```
<br>

- 두번째 실행 시 `create`를 유지하면 테이블이 새롭게 생성되어 대체되므로, `none`으로 설정해주시면 됩니다.
```
spring.jpa.hibernate.ddl-auto=none
```

---
# chess ws stomp flow for FrontEnd SIde
# 서론
이 프로젝트는 spring, nextjs 를 사용하여 SPA 기반 멀티세션 online-chess-game Web APP 을 구현한다.
이 문서는 Frontend 측면에서 chess-game backend server와 STOMP 통신을 어떻게 하면 되는가에 대한 flow 를 설명한다.

## sub / pub 모델
sub/, pub/ prefix 에 대해 설명.
- `sub/` (subscribe) : 클라이언트는 서버를 구독하고, 서버에서 오는 메세지를 수신함.
- `pub/` (publish): 클라이언트가 서버에게 보내는(발행하는) 메세지.

기본적인 flow
1. WS 연결
2. 구독
3. 구독해제
4. WS 연결끊기
   (구독해제는 ws 연결끊기랑 다름.)

## JSON
메세지 송/수신 시 항상 JSON 형식을 준수함.
각 path 마다 정해진 DTO 에 맞는 JSON 으로 통신.

## web socket 연결
`http://server-ip:server-port/connection`

### JS 예시코드
```js
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

// SockJS를 사용하여 WebSocket 연결 생성
const socket = new SockJS('http://localhost:8080/connection');
const stompClient = Stomp.over(socket);

// 연결 설정
stompClient.connect({}, (frame) => {
	console.log('Connected: ' + frame);
	
	// 메시지 구독(subscribe)
	stompClient.subscribe('/sub/messages', (message) => {
		console.log('Received: ' + message.body);
	});
	
	// 메시지 전송(발행, publish)
	stompClient.send('/pub/send', {}, JSON.stringify(
		{ content: 'Hello, World!' }
	));
});
```

 
---
# FLOW

## 1. 게임 찾기

유저가 게임찾기 버튼을 누르면 ?
1. ws connect
2. *1.2. sub/find-game* 구독하기
3. 이외의 모든 *sub/* 요소 미리 구독하기
4. *1.1. pub/find-game* 로 FindGameRequest 메세지 보내기

### 1.1. `pub/find-game`
해당 path로 *FindGameRequest* 메세지 발행

#### FindGameRequest DTO
```java
Long uid; // User 의 uid
Long timeLeft = 1800000; // 부여되는 시간 (30분)
Long timeToAddEveryTurnStart = 10000; // 매 턴 시작 시 추가되는 시간 (10초)
```

```JSON
{
	"uid":9,
	"timeLeft":1800000, // 30분
	"timeToAddEveryTurnStart":10000 // 10초
}
```

대기큐에 게임을 찾는중인 플레이어가 1명이상 있으면 그사람과 바로 매칭. *-> 1.2. sub 참조*
아무도없으면 30초간 대기. (이 때 프론트에서는 게임찾는중... 이거 표시되게 하기.)
30초 동안 게임이 매칭되지 않으면 매칭실패 *-> 1.2. sub 참조*

(다음에 시간나면 추가할사항 : 취소 버튼 (아직 구현계획 없음)
- 누르면 cancle-find-game 으로 게임찾기 취소요청 publish.
- 프론트 취소버튼 비활성화 해서 더이상 못누르게하기
- 백단에서 즉시 대기큐에서 uid 제거.
- 3초 후 게임찾는중... 로딩화면에서 나가짐.)

### 1.2. `sub/find-game/{uid}`

#### 1.2.1. 게임 찾기 성공
```JSON
{"type" : "game-found",
"message" : "게임을 찾았습니다. 연결 준비중입니다."}
```

게임 찾기를 성공하면, 프론트에서 취소버튼 비활성화. (꼬임 방지)
"게임 시작" 메세지가 올 때 까지 로딩화면에서 계속 대기 유지.

(텀이 너무 작아서 얘는 무시해도 될수도?)

#### 1.2.2. 게임 시작 (FindGameResponse DTO)
```java
	String type = "game-start"
	String message = "게임이 시작되었습니다.";
	
	String color; // white or black

	User opponent; // 상대방 정보 (닉네임, 전적 등)
```

"게임 찾기 완료" 와 "게임 시작" 사이에는 약간의 텀이 존재할 수 있음.
- 게임 매칭되었다는 메세지를 받으면
- *1.2 sub/find-game/{uid}* 는 더 이상 필요없음 -> 구독취소하기
- 게임화면으로 이동, 게임 시작, 시간 줄어들기 시작함.

##### User DTO (원래 있던거 사용)
```java
Long uid = null;
String userName = null;
String password = null;

String nickname;
Record record;
```

##### Record DTO (있던거 사용)
```java
private Long id = null;
private User user = null;

private int wins = 0;
private int losses = 0;
private int draws = 0;
```

#### 1.2.3. 나머지의 경우(대기, 에러, 매칭실패)

```JSON
{"type" : "waiting",
"message" : "매칭 상대 찾는 중"}
```

게임 찾기 요청했지만, 현재 게임을 대기중인 사람이 나 혼자 뿐일 때. -> *로딩 창 띄우기.*

```JSON
{"type" : "waiting",
"message" : "이미 대기 중인 상태입니다."}
```

게임 찾기를 요청한 후, 대기중인 상태에서 다시한번 게임찾기를 요청했을 때. -> *로딩 창 유지. 해당 메시지 무시*

```JSON
{"type" : "error",
"message" : "존재하지 않는 uid 입니다."}
```

Alart 같은거 띄워준 후, home 화면 으로 리디렉션. (적절하게)

```JSON
{"type" : "error",
"message" : "이미 게임 중인 상태입니다."}
```

해당 uid 가 속한 GameRoom 이 이미 존재할 때.

```JSON
{"type" : "fail",
"message" : "매칭에 실패했습니다. 다시 시도해 주세요."}
```

*게임 매칭에 실패*하면 실패했다는 메세지 옴.
- 매칭 실패 메세지를 받으면
1. 즉시 1.1 과 1.2 를 unsubscribe (구독취소) 하기 -> (?)
2. ws 연결을 끊기 (ws 연결을 끊으면 unsubscribe 도 같이될것같음)
3. 매칭실패했다는 메세지 띄워주기. -> 홈화면으로 이동.


## 2. 게임 중 (move)

### 2.1. `pub/move`
사용자가 말을 움직이면 해당 경로에 MoveRequest DTO 에 맞춰서 메세지를 보내야 함.

#### MoveRequest DTO
```java
Long uid; // User 객체의 uid
String move; // 형식:e3e4
```

> 캐슬링 같은 경우에도 move (String) 에 move 정보를 하나만 보내면 나중에 FEN 에 캐슬링이 완료 된 상태의 FEN 문자열을 보내줌.

> 혹은 그냥 success 된 move string (SAN형식)을 서버로 부터 받으면 JS 라이브러리에 board.move(SAN문자열) 이런식으로 수행하면 (캐슬링 가능한) 두개의 말을 동시에 움직여 주는 기능이 탑재되어 있을것으로 예상됨.

> 프로모션 같은 경우 다음과 같이 표기함. `e7e8=Q` -> 폰이 e7에서 e8로 이동 후 Queen 으로 승격함.

### 2.2 `sub/move/{uid}`

> [!warning] 주의
> 이미 구독되어 있어야 함.
> stomp 연결 후 즉시 구독하는것을 권장.

플레이어가 말을 움직인 후 전송되는 메세지.
(상대방에게도 전송되고, 본인에게도 전송된다.)

> [!question] Why?
>본인도 move 메시지를 받아야 자신의 move 가 올바른지 backend server에서 검증가능.
> 따라서 본인의 말을 움직인 후, 본인의 말의 움직임에 대한 move 정보를 받은 후
> 해당 정보를 바탕으로 실제 board UI 에 업데이트 해 주어야 한다.

- 예를들어 Black Player 가 말을 움직였다.
- (자신의 차례가 맞으면)
	- 프론트에서 백으로 메세지 보냄.
	- 백에서도 정상적인 움직임이라는것이 검증되면 *플레이어 두명에게 모두* FEN 을 포함한 MoveResponse 메세지를 보냄.
	- 프론트에서는 자신의 말 움직임과, 상대방의 말 움직임을 업데이트.
	- (프론트에서 FEN 을 사용해서 board 를 전부 refresh 할지, Move String 을 사용해서 움직임만 수행할지 몰라서 일단 FEN 이랑 move string이랑 둘다 반환함.)

만약 현재 자신의 차례인데 *sub/move/{uid}* 로 메세지를 받았다면? -> 무시하기.

#### MoveResponse DTO
```java
String type;
String message; // 성공, 실패(not your turn, invalid move)
String fen; // 말 이동 후 변경된 현재 chess-board 의 FEN 문자열을 반환.
String move;
String turn; // 말 이동 후 현재의 turn 을 반환 (white or black)
Long timeLeft; // 자신의 남은 시간을 ms(밀리세컨드) 단위로 반환
```

```JSON
{
	"type" : "success" | "not-your-turn" | "invalid-move",
	"message" : "부연설명 string",
	"fen" : "move 완료 후 현재 board 의 FEN 형식 string",
	"move" : "e6e5" | "e7e8=Q" /*등등*/,
	"turn" : "black" | "white",
	"timeLeft" : long /*Long type의 현재 플레이어의 남은시간 밀리세컨드*/
}
```

##### timeLeft
- 자신의 남은시간.
- 자신이 말을 움직이고, move 객체를 받으면,
- timeLeft에는 10초(매 턴이 끝나면 추가되는 시간)가 이미 추가된 남은시간이 들어있음.

#### 에러
```JSON
{
	"type" : "invalid-move",
	"message" : "유효하지 않은 움직임 입니다."
}
```

- 자신의 King이 Check 상태이지만, 적절한 방어가 아닌 Move를 요청하면 *invalid-move* 처리됨.
	- (프론트 라이브러리 단에서 기물이 아예 움직이지 못할것으로 예상됨)
- 나머지의 모든 chess 규칙에 맞지 않는 move를 요청하여도 *invalid-move* 처리됨.

```JSON
{
	"type" : "error",
	"message" : "진행중인 게임을 찾을 수 없습니다."
}
```

```JSON
{
	"type" : "error",
	"message" : "본인의 턴이 아닙니다."
}
```

#### 체크 상태일 때
>[!info] 정보
>Move 이후의 플레이어가 체크 당한 상태라면 알려주어야 한다.

- 해당 메세지는 *정상적인 move 동작 직후*에 수행되며,
- 현재 자신의 King이 공격받고 있는 경우 해당 메시지를 수신한다.

```JSON
{
	"type" : "checked",
	"message" : "현재 King이 위협받고 있습니다."
}
```


> [!failure] 주의!
>현재 자신이 check 상태이고, 체크메이트가 아니면
>플레이어는 자신의 체크를 해제하는 move 만 할 수 있음.
>
>마찬가지로, 자기자신을 체크메이트 상태로 만드는 move 는 아예 할 수 없음

- type:checked 를 받으면, move 하지 않고, 간단한 Alert 만 띄워줘도 될 것 같음.
- 혹은 프론트 라이브러리에서 그냥 해당 무브가 막혀있으면 백엔드로 따로 보내지 않고 움직일수 없는 무브라고 출력해 줘도 될것 같음.

## 3. 게임 종료

### 3.1. `sub/game-over/{uid}`

> [!warning] 주의
> 이미 구독되어 있어야 함.
> stomp 연결 후 즉시 구독하는것을 권장.

해당 경로로 GameOverResponse DTO 메시지가 오자마자 프론트에서는 게임을 멈추고, 승패판정 오버레이 화면 등으로 승리 or 패배 or 무승부 정보를 표시함. (자신의 승패무 정보를 함께 표시해도 좋을것 같음.)

#### GameOverResponse DTO
```java
String message;
String gameResult; // win | lose | draw
String type;
Record record; // 자신의 0승0패0무 정보를 담은 record 객체
```

```JSON
{
	"message":string,
	"gameResult":"win"|"lose"|"draw",
	"type":"timeover"|"surrender"|"checkmate"|"stalemate",
	"record":"{record 객체}"
}
```

##### Record DTO (있던거 사용)
```java
private Long id = null;
private User user = null;

private int wins = 0;
private int losses = 0;
private int draws = 0;
```

### 3.2 WS 연결끊기
게임이 끝나면 "당신의 승리입니다"등을 표시하여 주는 오버레이 화면나옴
- 해당 오버레이 화면에서 "홈화면으로 돌아가기" 등의 버튼을 누르면
	- WS 연결을 끊음.
	- 홈 화면으로 리디랙션

추가 구현 사항: (해당 유저에게 재경기 요청) -> **현재 개발계획 없음.**

---

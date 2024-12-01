package com.chessprojectspring.websocket;

import com.chessprojectspring.game.GameRoom;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class GameManager {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> waitingQueue = new ConcurrentLinkedQueue<>(); // 스레드 안전한 큐
    private final Object queueLock = new Object();


    public synchronized void createGameRoom() {
        // 게임룸 생성 로직
    }

    public synchronized void startGame(GameRoom gameRoom) {
        // 게임 시작 로직
    }

    /**
     * 게임 대기 큐에 플레이어 추가
     * @param uid 플레이어의 UID
     */
    public void addToWaitingQueue(Long uid) {
        synchronized (queueLock) {
            waitingQueue.add(uid);
        }
    }

    /**
     * 대기 큐에 있는 플레이어 수 반환
     * @return 대기 큐에 있는 플레이어 수
     */
    public int getWaitingQueueSize() {
        return waitingQueue.size();
    }

    /**
     * 대기 큐에 있는 플레이어 중 한명을 꺼내어 반환
     * @return 플레이어의 UID, 대기 큐에 없으면 null 반환
     */
    public Long pollOneFromQueue() {
        return waitingQueue.poll();
    }

//    /**
//     * 대기 큐에서 플레이어 두 명을 꺼내어 반환
//     * @return 두 명의 UID가 담긴 배열, 충분하지 않으면 null 반환
//     */
//    public Long[] pollTwoFromQueue() {
//        synchronized (queueLock) {
//            if (waitingQueue.size() >= 2) {
//                Long first = waitingQueue.poll();
//                Long second = waitingQueue.poll();
//                if (first != null && second != null) {
//                    return new Long[]{first, second};
//                }
//            }
//            return null;
//        }
//    }
} 
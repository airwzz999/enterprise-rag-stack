import { Client, IFrame, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WS_BASE_URL, WS_CONFIG } from '@/config';
import type { EntityId } from '@/types';

/**
 * WebSocket push payload type
 */
export interface WsNotificationPayload {
  eventType?: string;
  notificationType: string;
  title: string;
  content: string;
  link?: string;
  documentId?: EntityId;
  documentTitle?: string;
  timestamp?: string;
}

type NotificationCallback = (payload: WsNotificationPayload) => void;

/**
 * WebSocket connection service
 *
 * Connects to the backend /ws/notification endpoint using STOMP over SockJS,
 * supporting automatic reconnection, heartbeat keep-alive, and subscription
 * to both the personal notification channel and the reviewer broadcast channel.
 */
class WebSocketService {
  private client: Client | null = null;
  private notificationCallbacks: Set<NotificationCallback> = new Set();
  private reviewerCallbacks: Set<NotificationCallback> = new Set();
  private reconnectAttempts = 0;
  private maxReconnect = WS_CONFIG.maxReconnectTimes;
  private connected = false;

  /**
   * Register a personal notification callback
   */
  onNotification(callback: NotificationCallback): () => void {
    this.notificationCallbacks.add(callback);
    return () => {
      this.notificationCallbacks.delete(callback);
    };
  }

  /**
   * Register a reviewer broadcast callback (used only for the reviewer role)
   */
  onReviewerNotification(callback: NotificationCallback): () => void {
    this.reviewerCallbacks.add(callback);
    return () => {
      this.reviewerCallbacks.delete(callback);
    };
  }

  /**
   * Establish the WebSocket connection
   *
   * @param authToken JWT token (Bearer xxx format)
   * @param isReviewer Whether the current user has the reviewer role
   */
  connect(authToken: string, isReviewer: boolean): void {
    if (this.client?.active) {
      return;
    }

    const socketUrl = `${WS_BASE_URL}/notification`;
    const tokenParam = authToken.replace(/^Bearer\s+/i, '');
    const sockJsUrl = `${socketUrl}?token=${encodeURIComponent(tokenParam)}`;

    this.client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl),
      connectHeaders: {
        Authorization: authToken,
      },
      heartbeatIncoming: WS_CONFIG.heartbeatInterval,
      heartbeatOutgoing: WS_CONFIG.heartbeatInterval,
      reconnectDelay: 0, // We control reconnection manually
      debug: (msg: string) => {
        if (import.meta.env.DEV) {
          console.debug('[WS]', msg);
        }
      },

      onConnect: (_frame: IFrame) => {
        this.connected = true;
        this.reconnectAttempts = 0;
        console.info('[WS] Connected to the notification service');

        // Subscribe to the personal notification channel
        this.client?.subscribe('/user/queue/notifications', (message: IMessage) => {
          try {
            const payload: WsNotificationPayload = JSON.parse(message.body);
            this.notificationCallbacks.forEach((cb) => {
              try {
                cb(payload);
              } catch (e) {
                console.error('[WS] Notification callback failed:', e);
              }
            });
          } catch (e) {
            console.error('[WS] Failed to parse notification message:', e);
          }
        });

        // Reviewers subscribe to the public broadcast channel
        if (isReviewer) {
          this.client?.subscribe('/topic/reviewers', (message: IMessage) => {
            try {
              const payload: WsNotificationPayload = JSON.parse(message.body);
              this.reviewerCallbacks.forEach((cb) => {
                try {
                  cb(payload);
                } catch (e) {
                  console.error('[WS] Reviewer callback failed:', e);
                }
              });
            } catch (e) {
              console.error('[WS] Failed to parse review message:', e);
            }
          });
          console.info('[WS] Subscribed to the reviewer broadcast channel');
        }
      },

      onDisconnect: () => {
        this.connected = false;
        console.warn('[WS] Connection closed');
        this.attemptReconnect(authToken, isReviewer);
      },

      onStompError: (frame: IFrame) => {
        console.error('[WS] STOMP error:', frame.headers['message']);
        this.attemptReconnect(authToken, isReviewer);
      },

      onWebSocketError: (event: Event) => {
        console.error('[WS] WebSocket error:', event);
      },
    });

    this.client.activate();
  }

  /**
   * Disconnect the WebSocket connection
   */
  disconnect(): void {
    this.maxReconnect = 0; // Prevent automatic reconnection
    try {
      this.client?.deactivate();
    } catch {
      // ignore
    }
    this.connected = false;
    this.reconnectAttempts = 0;
    console.info('[WS] Disconnected intentionally');
  }

  /**
   * Whether the connection is active
   */
  isConnected(): boolean {
    return this.connected;
  }

  /**
   * Automatic reconnection
   */
  private attemptReconnect(authToken: string, isReviewer: boolean): void {
    if (this.reconnectAttempts >= this.maxReconnect) {
      console.warn(`[WS] Reached max reconnect attempts (${this.maxReconnect}), stopping`);
      return;
    }

    this.reconnectAttempts++;
    const delay = WS_CONFIG.reconnectInterval;
    console.info(
      `[WS] Retrying connection (attempt ${this.reconnectAttempts}) in ${delay / 1000}s...`
    );

    setTimeout(() => {
      if (!this.connected) {
        this.connect(authToken, isReviewer);
      }
    }, delay);
  }
}

export const webSocketService = new WebSocketService();
export default webSocketService;

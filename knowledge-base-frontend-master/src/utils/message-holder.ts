import type { MessageInstance } from 'antd/es/message/interface';
import { message as antMessage } from 'antd';

/**
 * Global message API holder
 *
 * Used when calling antd's message from non-React-component code (e.g. the request.ts
 * interceptor), avoiding the "Static function can not consume context" warning that a
 * static call would trigger.
 *
 * Usage:
 * 1. In App.tsx, obtain `message` via App.useApp() and call setMessageApi(message)
 * 2. Other non-component files get the proxy object via getMessageProxy()
 * 3. If messageApi hasn't been initialized yet, it automatically falls back to the static message API
 */
let messageApi: MessageInstance | null = null;

export function setMessageApi(api: MessageInstance) {
  messageApi = api;
}

export function hasMessageApi(): boolean {
  return messageApi !== null;
}

/**
 * Returns the message proxy object.
 * Prefers the hook-injected messageApi (which can consume the dynamic theme);
 * falls back to the static message API when uninitialized (produces a warning, but works fine).
 */
export const messageHolder = {
  success: (content: Parameters<MessageInstance['success']>[0]) =>
    messageApi ? messageApi.success(content) : antMessage.success(content),
  error: (content: Parameters<MessageInstance['error']>[0]) =>
    messageApi ? messageApi.error(content) : antMessage.error(content),
  warning: (content: Parameters<MessageInstance['warning']>[0]) =>
    messageApi ? messageApi.warning(content) : antMessage.warning(content),
  info: (content: Parameters<MessageInstance['info']>[0]) =>
    messageApi ? messageApi.info(content) : antMessage.info(content),
  loading: (content: Parameters<MessageInstance['loading']>[0]) =>
    messageApi ? messageApi.loading(content) : antMessage.loading(content),
  open: (config: Parameters<MessageInstance['open']>[0]) =>
    messageApi ? messageApi.open(config) : antMessage.open(config),
  destroy: (messageKey?: React.Key) =>
    messageApi ? messageApi.destroy(messageKey) : antMessage.destroy(messageKey),
};

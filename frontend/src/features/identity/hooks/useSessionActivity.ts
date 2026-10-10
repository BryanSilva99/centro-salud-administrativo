import { useEffect } from 'react';
import { ApiError } from '../../../shared/api/http';
import { auth } from '../api/auth';

export function useSessionActivity(accountId: string, onExpired: () => void, onConnection: (connected: boolean) => void) {
  useEffect(() => {
    const controller = new AbortController();
    let lastActivity = Date.now();
    let checking = false;
    let sending = false;
    const handleError = (error: unknown) => {
      if (controller.signal.aborted) return;
      if (error instanceof ApiError && error.status === 401) onExpired();
      else onConnection(false);
    };
    const check = async () => {
      if (checking) return;
      checking = true;
      try {
        const cuenta = await auth.me(controller.signal);
        if (!controller.signal.aborted) {
          if (!cuenta || cuenta.id !== accountId) onExpired();
          else onConnection(true);
        }
      } catch (error) { handleError(error); }
      finally { checking = false; }
    };
    const activity = async (event: Event) => {
      // Solo interacción real; un intervalo o evento sintético no renueva la sesión.
      if (!event.isTrusted || sending || Date.now() - lastActivity < 60_000) return;
      if (event.target instanceof Element && event.target.closest('[data-no-activity]')) return;
      sending = true;
      try {
        await auth.activity(controller.signal);
        if (!controller.signal.aborted) { lastActivity = Date.now(); onConnection(true); }
      } catch (error) { handleError(error); }
      finally { sending = false; }
    };
    const visible = () => { if (document.visibilityState === 'visible') void check(); };
    const interval = window.setInterval(() => { if (document.visibilityState === 'visible') void check(); }, 60_000);
    document.addEventListener('visibilitychange', visible);
    document.addEventListener('pointerdown', activity);
    document.addEventListener('keydown', activity);
    return () => {
      controller.abort();
      window.clearInterval(interval);
      document.removeEventListener('visibilitychange', visible);
      document.removeEventListener('pointerdown', activity);
      document.removeEventListener('keydown', activity);
    };
  }, [accountId, onExpired, onConnection]);
}

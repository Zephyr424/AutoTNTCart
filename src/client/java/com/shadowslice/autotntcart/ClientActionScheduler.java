package com.shadowslice.autotntcart;

import java.util.ArrayList;
import java.util.List;

public class ClientActionScheduler {
    private static final List<ScheduledAction> QUEUE = new ArrayList<>();

    public static void schedule(int delayTicks, Runnable action) {
        QUEUE.add(new ScheduledAction(delayTicks, action));
    }

    public static void clear() {
        QUEUE.clear();
    }

    public static boolean isEmpty() {
        return QUEUE.isEmpty();
    }

    public static void tick() {
        if (QUEUE.isEmpty()) return;
        for (int i = QUEUE.size() - 1; i >= 0; i--) {
            ScheduledAction a = QUEUE.get(i);
            a.delay--;
            if (a.delay <= 0) {
                try { a.action.run(); } catch (Exception ignored) {}
                QUEUE.remove(i);
            }
        }
    }

    private static class ScheduledAction {
        int delay;
        final Runnable action;
        ScheduledAction(int delay, Runnable action) {
            this.delay = delay;
            this.action = action;
        }
    }
}
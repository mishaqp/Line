package cn.lineai.mvp;

import cn.lineai.navigation.LineDestination;
import cn.lineai.navigation.LineDestinations;
import java.util.ArrayList;

/**
 * Navigation stack backed by typed Navigation 3 keys.
 *
 * <p>The Host still renders legacy screen ids while Java Views are migrated.
 * String conversion is isolated to this compatibility boundary.</p>
 */
public final class ScreenNavigationController {
    public interface Host {
        void hideOverlays();

        void showScreen(String screenId);

        default void showScreen(String screenId, boolean forward) {
            showScreen(screenId);
        }

        default void showScreen(String screenId, boolean forward, boolean animate) {
            showScreen(screenId, forward);
        }

        void showChatScreen();
    }

    private final ArrayList<LineDestination> screenStack = new ArrayList<>();

    public void showScreen(String screenId, Host host) {
        showDestination(LineDestinations.fromScreenId(screenId), host);
    }

    public void showDestination(LineDestination destination, Host host) {
        if (destination == null || destination instanceof LineDestination.Chat) {
            return;
        }
        if (screenStack.isEmpty() || !destination.equals(screenStack.get(screenStack.size() - 1))) {
            screenStack.add(destination);
        }
        showVisibleScreen(destination, host);
    }

    public void refreshVisibleScreen(String screenId, Host host) {
        LineDestination destination = LineDestinations.fromScreenId(screenId);
        if (destination instanceof LineDestination.Chat) {
            return;
        }
        if (screenStack.isEmpty()) {
            screenStack.add(destination);
        } else if (!destination.equals(screenStack.get(screenStack.size() - 1))) {
            int existingIndex = screenStack.lastIndexOf(destination);
            if (existingIndex >= 0) {
                while (screenStack.size() > existingIndex + 1) {
                    screenStack.remove(screenStack.size() - 1);
                }
            } else {
                screenStack.add(destination);
            }
        }
        showVisibleScreen(destination, host, true, false);
    }

    public void returnToScreen(String screenId, Host host) {
        LineDestination destination = LineDestinations.fromScreenId(screenId);
        if (destination instanceof LineDestination.Chat) {
            return;
        }
        int existingIndex = screenStack.lastIndexOf(destination);
        if (existingIndex >= 0) {
            while (screenStack.size() > existingIndex + 1) {
                screenStack.remove(screenStack.size() - 1);
            }
        } else {
            screenStack.clear();
            screenStack.add(destination);
        }
        showVisibleScreen(destination, host);
    }

    public void backFrom(String visibleScreenId, Host host) {
        LineDestination current = LineDestinations.fromScreenId(visibleScreenId);
        if (screenStack.isEmpty()) {
            if (current instanceof LineDestination.Chat) {
                return;
            }
            screenStack.add(current);
        } else if (!(current instanceof LineDestination.Chat)
                && !current.equals(screenStack.get(screenStack.size() - 1))) {
            int visibleIndex = screenStack.lastIndexOf(current);
            if (visibleIndex >= 0) {
                while (screenStack.size() > visibleIndex + 1) {
                    screenStack.remove(screenStack.size() - 1);
                }
            } else {
                screenStack.add(current);
            }
        }

        current = screenStack.remove(screenStack.size() - 1);
        LineDestination previous = screenStack.isEmpty()
                ? LineDestinations.parentOf(current)
                : screenStack.get(screenStack.size() - 1);

        if (previous instanceof LineDestination.Chat) {
            if (host != null) {
                host.showChatScreen();
            }
            return;
        }

        if (screenStack.isEmpty()) {
            screenStack.add(previous);
        }
        showVisibleScreen(previous, host, false);
    }

    /** Compatibility snapshot for existing Java tests and diagnostics. */
    public ArrayList<String> stackSnapshot() {
        ArrayList<String> result = new ArrayList<>(screenStack.size());
        for (LineDestination destination : screenStack) {
            result.add(destination.getScreenId());
        }
        return result;
    }

    public ArrayList<LineDestination> destinationStackSnapshot() {
        return new ArrayList<>(screenStack);
    }

    public static String normalizeScreenId(String screenId) {
        return LineDestinations.fromScreenId(screenId).getScreenId();
    }

    public static String parentScreenFor(String screenId) {
        return LineDestinations.parentOf(LineDestinations.fromScreenId(screenId)).getScreenId();
    }

    private void showVisibleScreen(LineDestination destination, Host host) {
        showVisibleScreen(destination, host, true);
    }

    private void showVisibleScreen(LineDestination destination, Host host, boolean forward) {
        showVisibleScreen(destination, host, forward, true);
    }

    private void showVisibleScreen(
            LineDestination destination,
            Host host,
            boolean forward,
            boolean animate
    ) {
        if (host == null || destination == null) {
            return;
        }
        host.hideOverlays();
        host.showScreen(destination.getScreenId(), forward, animate);
    }
}

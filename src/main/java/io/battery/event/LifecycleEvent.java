package io.battery.event;

/**
 * Marker for scenario lifecycle events, which are delivered synchronously on the
 * publishing thread so that listeners observe them in publication order.
 * Other events are delivered asynchronously.
 */
public interface LifecycleEvent {
}

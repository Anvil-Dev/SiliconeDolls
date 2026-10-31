package dev.anvilcraft.rg.sd.entity;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Pending names belong to one concrete server, never to a later world or request. */
final class SpawnPreparation<S> {
    private final Map<S, Map<String, Ticket<S>>> owners = new IdentityHashMap<>();

    synchronized Ticket<S> begin(S owner, String name) {
        var pending = owners.computeIfAbsent(owner, ignored -> new HashMap<>());
        var key = name.toLowerCase(Locale.ROOT);
        if (pending.containsKey(key)) return null;
        var ticket = new Ticket<>(owner, key, System.nanoTime());
        pending.put(key, ticket);
        return ticket;
    }

    synchronized boolean current(Ticket<S> ticket) {
        var pending = owners.get(ticket.owner);
        return pending != null && pending.get(ticket.name) == ticket;
    }

    synchronized boolean finish(Ticket<S> ticket) {
        var pending = owners.get(ticket.owner);
        if (pending == null || !pending.remove(ticket.name, ticket)) return false;
        if (pending.isEmpty()) owners.remove(ticket.owner);
        return true;
    }

    synchronized boolean cancel(S owner, String name) {
        var pending = owners.get(owner);
        if (pending == null || pending.remove(name.toLowerCase(Locale.ROOT)) == null) return false;
        if (pending.isEmpty()) owners.remove(owner);
        return true;
    }

    synchronized void cancelAll(S owner) {
        owners.remove(owner);
    }

    static <P> CompletableFuture<P> profile(Supplier<P> identity, Executor workers, Predicate<P> needsSkin,
        Function<P, CompletableFuture<P>> skin) {
        try {
            return CompletableFuture.supplyAsync(identity, workers)
                .thenCompose(profile -> needsSkin.test(profile) ? skin.apply(profile)
                    : CompletableFuture.completedFuture(profile));
        } catch (RuntimeException rejected) {
            return CompletableFuture.failedFuture(rejected);
        }
    }

    record Ticket<S>(S owner, String name, long startedNanos) {}
}

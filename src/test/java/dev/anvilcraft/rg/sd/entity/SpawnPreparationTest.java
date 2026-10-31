package dev.anvilcraft.rg.sd.entity;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpawnPreparationTest {
    @Test
    void rejectedWorkerReachesTheNormalFailurePath() {
        var result = SpawnPreparation.profile(() -> "profile", task -> {
            throw new java.util.concurrent.RejectedExecutionException("closed worker");
        }, profile -> false, profile -> CompletableFuture.completedFuture(profile));
        var failure = assertThrows(CompletionException.class, result::join);
        assertInstanceOf(java.util.concurrent.RejectedExecutionException.class, failure.getCause());
    }

    @Test
    void lookupDoesNotRunOnTheCallingThreadAndOfflineIdentitySkipsSkin() {
        var worker = new ArrayDeque<Runnable>();
        var identities = new AtomicInteger();
        var skins = new AtomicInteger();
        var result = SpawnPreparation.profile(() -> { identities.incrementAndGet(); return "offline-profile"; },
            worker::add, profile -> false, profile -> { skins.incrementAndGet(); return new CompletableFuture<>(); });
        assertFalse(result.isDone());
        assertEquals(0, identities.get());
        worker.remove().run();
        assertEquals("offline-profile", result.join());
        assertEquals(1, identities.get());
        assertEquals(0, skins.get());
    }

    @Test
    void onlineIdentityStillWaitsForItsSkinResult() {
        var worker = new ArrayDeque<Runnable>();
        var skin = new CompletableFuture<String>();
        var result = SpawnPreparation.profile(() -> "online-profile", worker::add, profile -> true, profile -> skin);
        worker.remove().run();
        assertFalse(result.isDone());
        skin.complete("online-profile-with-textures");
        assertEquals("online-profile-with-textures", result.join());
    }

    @Test
    void lookupFailureDoesNotStartTheSkinStage() {
        var worker = new ArrayDeque<Runnable>();
        var result = SpawnPreparation.profile(() -> { throw new IllegalStateException("lookup failure"); },
            worker::add, profile -> { throw new AssertionError("No identity"); },
            profile -> { throw new AssertionError("No identity"); });
        worker.remove().run();
        var failure = assertThrows(CompletionException.class, result::join);
        assertEquals("lookup failure", failure.getCause().getMessage());
    }

    @Test
    void sameNameIsReservedCaseInsensitively() {
        var pending = new SpawnPreparation<Object>();
        var server = new Object();
        var ticket = pending.begin(server, "TestPlayer");
        assertNotNull(ticket);
        assertNull(pending.begin(server, "testplayer"));
        assertTrue(pending.current(ticket));
        assertTrue(pending.finish(ticket));
        assertNotNull(pending.begin(server, "TESTPLAYER"));
    }

    @Test
    void cancelledLateCompletionCannotRemoveOrCreateTheReplacement() {
        var pending = new SpawnPreparation<Object>();
        var server = new Object();
        var first = pending.begin(server, "test");
        assertTrue(pending.cancel(server, "TEST"));
        var second = pending.begin(server, "test");
        assertFalse(pending.current(first));
        assertFalse(pending.finish(first));
        assertTrue(pending.current(second));
    }

    @Test
    void stoppingOneServerCancelsItsLateRequestsWithoutAffectingAnother() {
        var pending = new SpawnPreparation<Object>();
        var server = new Object();
        var other = new Object();
        var first = pending.begin(server, "test");
        var second = pending.begin(other, "test");
        pending.cancelAll(server);
        assertFalse(pending.current(first));
        assertTrue(pending.current(second));
    }
}

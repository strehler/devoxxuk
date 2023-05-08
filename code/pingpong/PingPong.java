//JAVA 19
//JAVAC_OPTIONS --enable-preview --release 19
//JAVA_OPTIONS  --enable-preview
//SOURCES TypedActor.java
package io.github.evacchi.typed.examples;

import io.github.evacchi.TypedActor;

import java.lang.System;
import java.util.concurrent.Executors;

import static io.github.evacchi.TypedActor.*;

public interface PingPong {
    sealed interface PingProtocol {};
    record Ping(Address<PingProtocol> sender, Integer count) implements PingProtocol {};
    record PoisonPill() implements PingProtocol {}; 

    static Effect<PingProtocol> pingerBehavior(Address<PingProtocol> self, PingProtocol msg) {
        System.out.println("Received message: " + msg);
        return switch (msg) {
            case Ping(var sender, var count) when count > 0 -> {
                sender.tell(new Ping(self, count - 1));
                yield Stay();
            }
            case Ping p -> {
                p.sender().tell(new PoisonPill());
                yield Stay();
            }
            case PoisonPill d -> { yield Die(); }
        };
    }
    static void main(String... args) {
        var actorSystem = new TypedActor.System(Executors.newCachedThreadPool());
        Address<PingProtocol> ponger = actorSystem.actorOf(self -> msg -> pingerBehavior(self, msg));
        Address<PingProtocol> pinger = actorSystem.actorOf(self -> msg -> pingerBehavior(self, msg));
        ponger.tell(new Ping(pinger, 10));
    }
}

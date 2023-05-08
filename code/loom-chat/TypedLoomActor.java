//JAVA 19
//JAVAC_OPTIONS --enable-preview --release 19
//JAVA_OPTIONS  --enable-preview

package io.github.evacchi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import static java.lang.System.out;

public interface TypedLoomActor {
    interface Effect<T> { Behavior<T> transition(Behavior<T> next); }
    interface Behavior<T> { Effect<T> receive(T o); }
    interface Address<T> { Address<T> tell(T msg); }

    static <T> Effect<T> Become(Behavior<T> next) { return current -> next; }
    static <T> Effect<T> Stay() { return current -> current; }
    static <T> Effect<T> Die() { return Become(msg -> { out.println("Dropping msg [" + msg + "] due to severe case of death."); return Stay(); }); }

    record System() {
        private static ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        // private static ExecutorService executorService = Executors.newFixedThreadPool(5); // clientManager + (2 threads X client)

        public <T> Address<T> actorOf(Function<Address<T>, Behavior<T>> initial) {
            var addr = new RunnableAddress<T>(initial, new LinkedBlockingQueue<>());
            executorService.execute(addr);
            return addr;
        }
    }

    record RunnableAddress<T>(Function<Address<T>, Behavior<T>> initial, LinkedBlockingQueue<T> mailbox)
        implements Address<T>, Runnable {

        public Address<T> tell(T msg) {
            mailbox.offer(msg);
            return this;
        }

        public void run() {
            Behavior<T> behavior = initial.apply(this);
            while (true) {
                try {
                    T message = mailbox.take();
                    Effect<T> effect = behavior.receive(message);
                    behavior = effect.transition(behavior);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

}

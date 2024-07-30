// Problem 1
// There are patients [1, 2, 3, 4, 5, 6] needs to be seen, every 2 second put 1 new user into waiting queue
// There are doctors to see the patients, there are two doctors [1, 2]. It took doctors 4 secs to see a patient
// Design a system to simulate the scenario.

// Print: when patient join the waiting queue
// When patient get picked up by a doctor
// When waiting queue is empty. 

// Follow up:
// If the patient has priority, low priority number go first, if the priority are the same, the one waited longer has priority


import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main5 {

    public class User{
        int id;

        public User(int id) {
            this.id = id;
        }
    }

    public class Referee {
        int id;
        Instant instantAddedToQue;
        public Referee(int id) {
            this.id = id;
        }
        public void setTime() {
            this.instantAddedToQue = Instant.now();
        }
    }

    public void simulate() throws InterruptedException {
        List<User> userList = new ArrayList<>(
                List.of(new User(1), new User(2), new User(3), new User(4), new User(5), new User(6)));

        List<Referee> refList = new ArrayList<>(
                List.of(new Referee(1), new Referee(2))
        );

        // a user queue and a ref queue
        LinkedList<User> waitingQue = new LinkedList<>();
        LinkedList<Referee> refQue = new LinkedList<>();
        refQue.addAll(refList);
        LinkedList<Referee> processQue = new LinkedList<>();

        // a scheduled pool
        ScheduledExecutorService userPool = Executors.newScheduledThreadPool(1);
        ScheduledExecutorService refPool = Executors.newScheduledThreadPool(3);
        ScheduledExecutorService processPool = Executors.newScheduledThreadPool(2);

        userPool.scheduleAtFixedRate(() -> {
            var user = userList.remove(0);
            synchronized (waitingQue) {
                waitingQue.add(user);
                System.out.println(
                        Instant.now() + ": added User " + user.id + " and total users waiting are " + waitingQue.size());
            }
        }, 0, 2* 1000, TimeUnit.MILLISECONDS);

        refPool.scheduleAtFixedRate(()-> {
            Referee ref = null;
            synchronized (waitingQue) {
                System.out.println(Instant.now() + ": ref thread entered waiting queue and there are "
                        + waitingQue.size() + " waiting");
                if (waitingQue.size() > 0) {
                    synchronized (refQue) {
                        System.out.println(Instant.now() + ": ref thread entered ref queue and there are "
                                + refQue.size() + " waiting");
                        if (refQue.size() > 0) {
                            var user = waitingQue.removeFirst();
                            ref = refQue.removeFirst();
                            System.out.println(
                                    Instant.now() + ": User " + user.id + " is picked by Referee " + ref.id);
                        }
                    }
                } else {
                    System.out.println(Instant.now() + ": There is no user waiting.");
                }
            }

            if (ref != null) {
                synchronized (processQue) {
                    ref.setTime();
                    processQue.add(ref);
                }
            }
        }, 10, 1*1000, TimeUnit.MILLISECONDS);

        processPool.scheduleAtFixedRate(()-> {
            List<Referee> finishedRef = new ArrayList<>();
            synchronized (refQue) {
                if (refQue.size() < 2) {
                    synchronized (processQue) {
                        Instant now = Instant.now();
                        for (var ref : processQue) {
                            if (ref.instantAddedToQue.plusSeconds(4).isBefore(now)) {
                                processQue.remove(ref);
                                finishedRef.add(ref);
                            }
                        }
                    }
                    System.out.println(Instant.now() + ": found " + finishedRef.size() + " refs finished verification");
                    refQue.addAll(finishedRef);
                }
            }
        }, 0, 1* 1000, TimeUnit.MILLISECONDS);

        Thread.sleep(30 * 1000);
        userPool.shutdown();
        refPool.shutdown();
        processPool.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {

        Main5 main = new Main5();
        main.simulate();
    }
}

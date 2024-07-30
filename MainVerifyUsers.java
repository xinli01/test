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

    public class Patient {
        int id;

        public Patient(int id) {
            this.id = id;
        }
    }

    public class Doctor {
        int id;
        Instant instantAddedToQue;
        public Doctor(int id) {
            this.id = id;
        }
        public void setTime() {
            this.instantAddedToQue = Instant.now();
        }
    }

    public void simulate() throws InterruptedException {
        List<Patient> patientList = new ArrayList<>(
                List.of(new Patient(1), new Patient(2), new Patient(3), new Patient(4), new Patient(5), new Patient(6)));

        List<Doctor> doctorList = new ArrayList<>(
                List.of(new Doctor(1), new Doctor(2))
        );

        // a user queue and a ref queue
        LinkedList<Patient> waitingQue = new LinkedList<>();
        LinkedList<Doctor> doctorQue = new LinkedList<>();
        doctorQue.addAll(doctorList);
        LinkedList<Doctor> processQue = new LinkedList<>();

        // a scheduled pool
        ScheduledExecutorService patientThreadPool = Executors.newScheduledThreadPool(1);
        ScheduledExecutorService doctorThreadPool = Executors.newScheduledThreadPool(3);
        ScheduledExecutorService processThreadPool = Executors.newScheduledThreadPool(2);

        patientThreadPool.scheduleAtFixedRate(() -> {
            var user = patientList.remove(0);
            synchronized (waitingQue) {
                waitingQue.add(user);
                System.out.println(
                        Instant.now() + ": added User " + user.id + " and total users waiting are " + waitingQue.size());
            }
        }, 0, 2* 1000, TimeUnit.MILLISECONDS);

        doctorThreadPool.scheduleAtFixedRate(()-> {
            Doctor ref = null;
            synchronized (waitingQue) {
                System.out.println(Instant.now() + ": doctor thread entered waiting queue and there are "
                        + waitingQue.size() + " waiting");
                if (waitingQue.size() > 0) {
                    synchronized (doctorQue) {
                        System.out.println(Instant.now() + ": ref thread entered ref queue and there are "
                                + doctorQue.size() + " waiting");
                        if (doctorQue.size() > 0) {
                            var user = waitingQue.removeFirst();
                            ref = doctorQue.removeFirst();
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

        processThreadPool.scheduleAtFixedRate(()-> {
            List<Doctor> finishedRef = new ArrayList<>();
            synchronized (doctorQue) {
                if (doctorQue.size() < 2) {
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
                    doctorQue.addAll(finishedRef);
                }
            }
        }, 0, 1* 1000, TimeUnit.MILLISECONDS);

        Thread.sleep(30 * 1000);
        patientThreadPool.shutdown();
        doctorThreadPool.shutdown();
        processThreadPool.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {

        Main5 main = new Main5();
        main.simulate();
    }
}

import java.util.*;
public class MLFQScheduling {
    static final int TIME_PERIOD_S = 400;
    static int[] timeSlices = {10, 50, 60};
    static int[] allotments = {30, 100, 120};

    public static void main(String[] args) {
        List<Process> processes = new ArrayList<>();
        processes.add(new Process(53, 10, 130));
        processes.add(new Process(165, 10, 125));
        processes.add(new Process(472, 80, 128));
        processes.add(new Process(305, 90, 90));
        processes.add(new Process(235, 175, 85));
        processes.add(new Process(366, 445, 80));

        List<Queue<Process>> queues = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            queues.add(new LinkedList<>());
        }

        for (int currentTime = 0; currentTime <= 1000; currentTime++) {
            System.out.println("Current Time-Slot: " + currentTime);

            // Check for new arrivals and prioritize by PID if arriving at the same time
            checkForNewArrivals(processes, queues, currentTime);

            // Periodic Boost
            if (currentTime % TIME_PERIOD_S == 0 && currentTime > 0) {
                System.out.println("Periodic Boost Occurred.");
                boostAllProcesses(queues, processes);
            }

            // Execute Processes for 1 time unit each iteration
            executeProcesses(queues, currentTime);

            // Print the state of each queue
            printQueuesState(queues);
        }
    }

    private static void checkForNewArrivals(List<Process> processes, List<Queue<Process>> queues, int currentTime) {
        processes.stream()
                .filter(p -> p.arrivalTime == currentTime)
                .forEach(p -> {
                    queues.get(0).add(p); // Add new arrivals to the highest priority queue
                    System.out.println("New Arrival - " + p);
                });
    }

    private static void executeProcesses(List<Queue<Process>> queues, int currentTime) {
        for (int i = 0; i < queues.size(); i++) {
            Queue<Process> queue = queues.get(i);
            if (!queue.isEmpty()) {
                Process process = queue.peek();
                // Execute the process for 1 time unit
                process.remainingTime--;
                process.timeSliceRemaining--;
                process.timeAllottedTotal--;
    
                System.out.printf("Time %d: Executing - PID: %d, Remaining Time: %d, Queue: %d\n",
                                  currentTime, process.pid, process.remainingTime, i + 1);
    
                if (process.remainingTime == 0) {
                    System.out.printf("Time %d: Process PID: %d completed.\n", currentTime, process.pid);
                    queue.poll(); // Process completed; remove it from the queue
                } else if (process.timeSliceRemaining == 0) {
                    queue.poll(); // Time slice finished; remove from current queue
                    if (process.timeAllottedTotal <= 0 && i < queues.size() - 1) {
                        // Time allotment finished; move to next lower priority queue
                        process.currentQueue++;
                        process.timeSliceRemaining = timeSlices[process.currentQueue];
                        process.timeAllottedTotal = allotments[process.currentQueue];
                        queues.get(process.currentQueue).add(process);
                        System.out.printf("Time %d: Process PID: %d moved to Queue %d.\n", currentTime, process.pid, process.currentQueue + 1);
                    } else {
                        // Stay in the same queue but reset time slice
                        process.timeSliceRemaining = timeSlices[i];
                        queue.add(process); // Re-add process to the same queue
                    }
                }
                break; // Only one process executed per unit of time
            }
        }
    }
    
    private static void boostAllProcesses(List<Queue<Process>> queues, List<Process> processes) {
        for (Queue<Process> queue : queues) {
            queue.clear();
        }
    
        // Reversing the order for the boost to add largest PID first
        processes.sort(Comparator.comparingInt(Process::getPid).reversed());
    
        for (Process process : processes) {
            process.currentQueue = 0;
            process.timeSliceRemaining = timeSlices[0];
            process.timeAllottedTotal = allotments[0];
            queues.get(0).add(process);
        }
    
        System.out.println("Time " + TIME_PERIOD_S + ": Boost completed, largest PID first in the highest-priority queue.");
    }
    
    // You might need to add getters for pid and arrivalTime in your Process class for the sorting to work correctly.
    
    

    private static void printQueuesState(List<Queue<Process>> queues) {
        for (int i = 0; i < queues.size(); i++) {
            System.out.println("Queue " + (i + 1) + ": " + queues.get(i));
        }
    }

}



class Process {
    int pid;
    int arrivalTime;
    int executionTime;
    int remainingTime;
    int currentQueue;
    int timeSliceRemaining; // Time slice remaining for current execution
    int timeSliceAllotted; // Time slice initially allotted based on current queue
    int timeAllottedTotal; // Total time allotted before moving to a lower queue

    public Process(int pid, int arrivalTime, int executionTime) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.executionTime = executionTime;
        this.remainingTime = executionTime;
        this.currentQueue = 0; // Start in the highest priority queue
        this.timeSliceRemaining = MLFQScheduling.timeSlices[currentQueue];
        this.timeSliceAllotted = this.timeSliceRemaining;
        this.timeAllottedTotal = MLFQScheduling.allotments[currentQueue];
    }

    @Override
    public String toString() {
        return String.format("PID: %d, Arrival Time: %d, Remaining Time: %d, Queue: %d, Time Slice Remaining: %d",
                pid, arrivalTime, remainingTime, currentQueue, timeSliceRemaining);
    }

    public int getPid() {
        return pid;
    }
}

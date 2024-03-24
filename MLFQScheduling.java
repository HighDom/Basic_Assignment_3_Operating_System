import java.util.*;

class Process {
    int pid;
    int arrivalTime;
    int executionTime;
    int remainingTime;
    int currentQueue;
    int timeSliceAllotted;

    public Process(int pid, int arrivalTime, int executionTime) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.executionTime = executionTime;
        this.remainingTime = executionTime;
        this.currentQueue = 0; // Start in the highest priority queue
        this.timeSliceAllotted = timeSlices[0]; // Assign initial time slice based on queue
    }

    @Override
    public String toString() {
        return String.format("PID: %d, Arrival Time: %d, Remaining Time: %d, Queue: %d", pid, arrivalTime, remainingTime, currentQueue);
    }
}

public class MLFQScheduling {
    static final int TIME_PERIOD_S = 400;
    static int[] timeSlices = {60, 50, 10};
    static int[] allotments = {120, 100, 30};

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

            // Check for new arrivals
            for (Process process : processes) {
                if (process.arrivalTime == currentTime) {
                    queues.get(process.currentQueue).add(process); // Add new arrivals to the appropriate queue
                    System.out.println("New Arrival - " + process.toString());
                }
            }

            // Periodic Boost
            if (currentTime % TIME_PERIOD_S == 0 && currentTime > 0) {
                System.out.println("Periodic Boost Occurred.");
                boostAllProcesses(queues, processes);
            }

            // Execute Processes
            executeProcesses(queues, currentTime);

            // Print the state of each queue
            for (int i = 0; i < queues.size(); i++) {
                System.out.println("Queue " + (i + 1) + ": " + queues.get(i).toString());
            }
        }
    }

    private static void boostAllProcesses(List<Queue<Process>> queues, List<Process> processes) {
        queues.forEach(Queue::clear); // Clear existing queues
        processes.stream()
                .sorted(Comparator.comparingInt(p -> p.pid)) // Sort by PID
                .forEach(p -> {
                    p.currentQueue = 0; // Reset queue to highest priority
                    queues.get(0).add(p);
                    p.timeSliceAllotted = timeSlices[0]; // Reset time slice
                });
    }

    private static void executeProcesses(List<Queue<Process>> queues, int currentTime) {
        for (int i = 0; i < queues.size(); i++) {
            Queue<Process> queue = queues.get(i);
            if (!queue.isEmpty()) {
                Process process = queue.poll(); // Remove the process from the queue for execution
                int timeSlice = Math.min(process.remainingTime, timeSlices[i]); // Determine the actual time slice for execution
                System.out.printf("Executing PID: %d from Queue: %d for %d units\n", process.pid, i + 1, timeSlice);
                process.remainingTime -= timeSlice; // Decrement remaining time

                // Handling Queue Transition or Re-enqueue
                if (process.remainingTime > 0) {
                    // Check if it needs to move to a lower priority queue
                    process.currentQueue = Math.min(process.currentQueue + 1, 2); // Ensure it doesn't go below the lowest queue
                    queues.get(process.currentQueue).add(process); // Re-enqueue the process
                } else {
                    System.out.printf("Process PID: %d has completed execution.\n", process.pid);
                }

                // Only one process is executed per time unit
                break;
            }
        }
    }
}

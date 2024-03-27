import java.util.*;
public class MLFQScheduling {
    static final int TIME_PERIOD_S = 400;
    static int[] timeSlices = {10, 50, 60};
    static int[] allotments = {30, 100, 120};

    public static void main(String[] args) {

        //All Processes Hardcoded
        List<Process> processes = new ArrayList<>();
        processes.add(new Process(53, 10, 130));
        processes.add(new Process(165, 10, 125));
        processes.add(new Process(472, 80, 128));
        processes.add(new Process(305, 90, 90));
        processes.add(new Process(235, 175, 85));
        processes.add(new Process(366, 445, 80));

        //Adds All Queues
        List<Queue<Process>> queues = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            queues.add(new LinkedList<>());
        }


        //Start the processes.
        for (int currentTime = 0; currentTime <= 700; currentTime++) {
            //Printout Time-Slot
            //System.out.println("Current Time-Slot: " + currentTime);

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
                    System.out.print("New Arrival - " + p + "; \t");
                    System.out.println("Current Time-Slot: " + currentTime);
                });
    }

    private static void executeProcesses(List<Queue<Process>> queues, int currentTime) {
        // First Pass: Check if any process is currently in progress and execute it
        for (Queue<Process> queue : queues) {
            for (Process process : queue) {
                if (process.isInProgress()) {
                    process.executeOneUnit(queues, timeSlices, allotments, currentTime);
                    return; // Exit after executing the in-progress process for this time unit
                }
            }
        }

        // Second Pass: If no process is in progress, find the first available process to execute
        for (int i = 0; i < queues.size(); i++) {
            Queue<Process> queue = queues.get(i);
            if (!queue.isEmpty()) {
                Process process = queue.peek();
                process.executeOneUnit(queues, timeSlices, allotments, currentTime);
    
                if (!process.isInProgress()) {
                    queue.poll(); // Remove process from the queue if it's not in progress
                    if (process.timeSliceRemaining > 0 && process.remainingTime > 0) {
                        // If the process has a time slice remaining and is not completed, re-add it to the same queue
                        queue.add(process);
                    }
                }
                break; // Only one process executed per unit of time
            }
        }
    }
    
    
    private static void boostAllProcesses(List<Queue<Process>> queues, List<Process> processes) {
        // First, reset all processes to not in progress
        for (Process process : processes) {
            process.setInProgress(false);
        }
    
        // Clear all queues to prepare for the boost
        for (Queue<Process> queue : queues) {
            queue.clear();
        }
    
        // Reversing the order for the boost to add the largest PID first
        processes.sort(Comparator.comparingInt(Process::getPid).reversed());
    
        // Add all processes back into the highest-priority queue with their time slice and allotment reset
        for (Process process : processes) {
            process.currentQueue = 0; // Reset to highest priority queue
            process.timeSliceRemaining = timeSlices[0]; // Reset time slice based on the highest queue
            process.timeAllottedTotal = allotments[0]; // Reset allotment time based on the highest queue
            queues.get(0).add(process); // Add the process to the queue
        }
    
        System.out.println("Time " + TIME_PERIOD_S + ": Boost completed, largest PID first in the highest-priority queue.");
    }
    
    
    // You might need to add getters for pid and arrivalTime in your Process class for the sorting to work correctly.
    
    

    private static void printQueuesState(List<Queue<Process>> queues) {
        for (int i = 0; i < queues.size(); i++) {
            //System.out.println("Queue " + (i + 1) + ": " + queues.get(i));
        }
    }
}


//----------------------PROCESSES----------------------
class Process {
    int pid;
    int arrivalTime;
    int executionTime;
    int remainingTime;
    int currentQueue;
    int timeSliceRemaining; // Time slice remaining for current execution
    int timeSliceAllotted; // Time slice initially allotted based on current queue
    int timeAllottedTotal; // Total time allotted before moving to a lower queue
    boolean inProgress; // Indicates if the process is currently being executed
    int executionStartTime; // Tracks the start time of the current execution slice



    public Process(int pid, int arrivalTime, int executionTime) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.executionTime = executionTime;
        this.remainingTime = executionTime;
        this.currentQueue = 0; // Start in the highest priority queue
        this.timeSliceRemaining = MLFQScheduling.timeSlices[currentQueue];
        this.timeSliceAllotted = this.timeSliceRemaining;
        this.timeAllottedTotal = MLFQScheduling.allotments[currentQueue];
        this.inProgress = false; // Initialize as not in progress
        this.executionStartTime = -1; // Initialize to -1 indicating no execution has started yet

        
    }

    public void executeOneUnit(List<Queue<Process>> queues, int[] timeSlices, int[] allotments, int currentTime) {
        // Mark the process as in progress if it's starting execution
        if (!this.isInProgress()) {
            this.executionStartTime = currentTime; // Start time of current execution
            this.setInProgress(true);
        }
    
        this.remainingTime--; // Decrease remaining time by one unit
        this.timeSliceRemaining--; // Decrease time slice by one unit
    
        // Check conditions for time slice completion or process completion
        if (this.timeSliceRemaining <= 0 || this.remainingTime <= 0) {
            // Print the execution time frame
            System.out.printf("%d-%d [PID %d] [ArrivalTime %d] [Remaining_Time %d]\n", 
                              this.executionStartTime, currentTime + 1, this.pid, this.arrivalTime, Math.max(this.remainingTime, 0));
    
            this.setInProgress(false); // Process is no longer actively executing
    
            if (this.remainingTime > 0) {
                // Time slice is exhausted, but the process still has remaining time
                // Check if the process's allotment time in the current queue is also exhausted
                if (this.timeAllottedTotal <= 0 && this.currentQueue < queues.size() - 1) {
                    // Move to a lower priority queue if possible
                    this.currentQueue++;
                    queues.get(this.currentQueue).add(this); // Add to the end of the next lower priority queue
                } else {
                    // Otherwise, re-add to the end of the current queue for round-robin scheduling
                    queues.get(this.currentQueue).add(this);
                }
            }
    
            // Reset the time slice for the next execution cycle, regardless of the queue
            this.timeSliceRemaining = timeSlices[this.currentQueue];
            // Reset the execution start time for the next cycle
            this.executionStartTime = currentTime + 1;
        }
    
        // Decrement allotment time if the process is within its time slice
        if (this.timeSliceRemaining > 0) {
            this.timeAllottedTotal--;
        }
    }
    
    
    
    

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
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

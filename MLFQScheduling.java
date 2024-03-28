import java.util.*;
import java.util.stream.Collectors;
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
        List<Deque<Process>> queues = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            queues.add(new LinkedList<>());
        }


        //Start the processes.
        for (int currentTime = 0; currentTime <= 700; currentTime++) {

            // Check for new arrivals and prioritize by PID if arriving at the same time
            checkForNewArrivals(processes, queues, currentTime);

            // Periodic Boost
            if (currentTime == TIME_PERIOD_S) {
                System.out.println("Periodic Boost Occurred.");
                boostAllProcesses(queues, processes, currentTime);
            }

            // Execute Processes for 1 time unit each iteration
            executeProcesses(queues, currentTime);

            // Print the state of each queue
            //printQueuesState(queues);
        }
    }

    private static void checkForNewArrivals(List<Process> processes, List<Deque<Process>> queues, int currentTime) {
        // Filter processes that are arriving at the current time
        List<Process> arrivingProcesses = processes.stream()
                .filter(p -> p.arrivalTime == currentTime)
                .sorted(Comparator.comparingInt(Process::getPid)) // Sort by PID in ascending order
                .collect(Collectors.toList());
    
        // Add sorted arrivals to the front of the highest priority queue in reverse order
        // so the lowest PID is at the front
        for (int i = arrivingProcesses.size() - 1; i >= 0; i--) {
            Process p = arrivingProcesses.get(i);
            queues.get(0).addFirst(p);
            System.out.print("New Arrival - " + p + "; \t");
            System.out.println("Current Time-Slot: " + currentTime);
        }
    }
    

    private static void executeProcesses(List<Deque<Process>> queues, int currentTime) {

            // Pre-process step: Remove completed processes from each queue, not super clean but I just want to get it done!
        for (Deque<Process> queue : queues) {
            Iterator<Process> iterator = queue.iterator();
            while (iterator.hasNext()) {
                Process process = iterator.next();
                if (process.remainingTime <= 0) {
                    //System.out.println("Removed PID: " + process.pid);
                    iterator.remove(); // Safely remove the process if it's completed
                }
            }
        }

        // First Pass: Check if any process is currently in progress and execute it
        // First Pass: Check if any process is currently in progress and execute it
        for (Deque<Process> queue : queues) {
            for (Process process : queue) {
                if (process.isInProgress()) {
                    process.executeOneUnit(queues, timeSlices, allotments, currentTime);
                    if (!process.isInProgress()) {
                        queue.remove(process); // Directly remove the specified process
                        if (process.timeAllottedTotal > 0 && process.remainingTime > 0) {
                            queues.get(process.currentQueue).add(process); // Re-add if needed based on conditions
                        }
                    }
                    return; // Exit after executing the in-progress process for this time unit
                }
            }
        }

        
        // Second Pass: If no process is in progress, find the first available process to execute
        for (int i = 0; i < queues.size(); i++) {
            Deque<Process> queue = queues.get(i);
            if (!queue.isEmpty()) {
                
                Process process = queue.peek();
                process.executeOneUnit(queues, timeSlices, allotments, currentTime);

                return;
            }
        }
    }
    
    
    private static void boostAllProcesses(List<Deque<Process>> queues, List<Process> processes, int currentTime) {
        // Reset all processes to not in progress and clear all queues
        for (Process process : processes) {
            process.setInProgress(false);
        }
        for (Deque<Process> queue : queues) {
            queue.clear();
        }

        // Filter processes based on their arrival time before boosting
        List<Process> arrivedProcesses = processes.stream()
                .filter(p -> p.arrivalTime <= currentTime)
                .collect(Collectors.toList());

        // Sort arrived processes by PID in descending order
        arrivedProcesses.sort(Comparator.comparingInt(Process::getPid).reversed());

        // Add arrived processes back to the highest-priority queue with reset time slice and allotment
        for (Process process : arrivedProcesses) {
            process.currentQueue = 0; // Reset to highest priority queue
            resetProcessTimeSliceAndAllotment(process, 0); // Use a new method to reset time slice and allotment
            queues.get(0).add(process);
        }

        System.out.println("Time " + currentTime + ": Boost completed, largest PID first in the highest-priority queue.");
    }

    // New method to reset process's time slice and allotment based on its current queue
    private static void resetProcessTimeSliceAndAllotment(Process process, int newQueue) {
        process.currentQueue = newQueue;
        process.timeSliceRemaining = timeSlices[newQueue];
        process.timeAllottedTotal = allotments[newQueue];
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

    private static void resetProcessTimeSliceAndAllotment(Process process, int newQueue) {
        process.currentQueue = newQueue;
        process.timeSliceRemaining = MLFQScheduling.timeSlices[newQueue];
        process.timeAllottedTotal = MLFQScheduling.allotments[newQueue];
    }

    public void executeOneUnit(List<Deque<Process>> queues, int[] timeSlices, int[] allotments, int currentTime) {

        if (this.remainingTime == 1) {
            this.remainingTime--; // Process completes execution
            this.timeAllottedTotal--;
            this.timeSliceRemaining--; // Decrease time slice by one unit
            this.setInProgress(false); // Mark as not in progress
            printExecutionStatementAndQueueOrder(queues, currentTime);
            // Printing before removal as it still technically executes in this time unit
            /*
            System.out.printf("%d-%d [PID %d] [ArrivalTime %d] [Remaining_Time %d] [TimeAllottedTotal %d]\n",
                    this.executionStartTime, currentTime + 1, this.pid, this.arrivalTime, 0, this.timeAllottedTotal);
            */
            // The process should be removed from the queue; this is handled in executeProcesses
            return;
        }

        // Mark the process as in progress if it's starting execution
        if (!this.isInProgress()) {
            this.executionStartTime = currentTime; // Start time of current execution
            this.setInProgress(true);
        }
    
        this.remainingTime--; // Decrease remaining time by one unit
        this.timeSliceRemaining--; // Decrease time slice by one unit
        this.timeAllottedTotal--;
    
        // Check conditions for time slice completion or process completion
        if (this.timeSliceRemaining <= 0 || this.remainingTime <= 0) {
            printExecutionStatementAndQueueOrder(queues, currentTime); 
            
            this.setInProgress(false); // Process is no longer actively executing
    
            if (this.remainingTime > 0) {
                // Time slice is exhausted, but the process still has remaining time
                // Check if the process's allotment time in the current queue is also exhausted
                if (this.timeAllottedTotal <= 0 && this.currentQueue < queues.size() - 1) {
                    // Move to the next higher priority queue if the time allotment is exhausted
                    int newQueue = this.currentQueue + 1;
                    resetProcessTimeSliceAndAllotment(this, newQueue); // Reset time slice and allotment for the new queue
                    //queues.get(newQueue).add(this);
                } else {
                    // If the time slice is exhausted but there's still remaining time
                    if (this.timeSliceRemaining <= 0 && this.remainingTime > 0) {
                        // Re-add to the end of the current queue for round-robin scheduling
                        // Reset the time slice for the next execution cycle, regardless of the queue
                        this.timeSliceRemaining = timeSlices[this.currentQueue];
                    }
                }
                
            }
    
            // Reset the time slice for the next execution cycle, regardless of the queue
            this.timeSliceRemaining = timeSlices[this.currentQueue];
            // Reset the execution start time for the next cycle
            this.executionStartTime = currentTime + 1;
        }
    
        // Decrement allotment time if the process is within its time slice
        
    }
    
    private void printExecutionStatementAndQueueOrder(List<Deque<Process>> queues, int currentTime) {
        System.out.printf("\n%d-%d [PID %d] [ArrivalTime %d] [Remaining_Time %d]\n",
                          this.executionStartTime, currentTime + 1, this.pid, this.arrivalTime, 
                          Math.max(this.remainingTime, 0));
        for (int i = 0; i < queues.size(); i++) {
            System.out.print("Queue " + (i + 1) + ": ");
            queues.get(i).forEach(process -> System.out.print(process.getPid() + " "));
            System.out.println();
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

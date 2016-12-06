import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Jae on 12/4/16.
 */
public class Scheduler {
    public static int system_time = 0;
    public static int system_max_main_memory = 0;
    public static int system_available_memory = 0;
    public static int system_max_devices = 0;
    public static int system_available_devices = 0;
    public static int system_quantum = 0;
    public static int system_timeStamp = 0;
    public static ArrayList<Job> allJobs = new ArrayList<>();
    public static LinkedList<Job> submit_queue = new LinkedList<>();
    public static HoldQueue hold_queue1 = new HoldQueue(JobSchedule.SJF);
    public static HoldQueue hold_queue2 = new HoldQueue(JobSchedule.FIFO);
    public static HoldQueue[] hold_queues = {hold_queue1, hold_queue2};
    public static LinkedList<Job> ready_queue = new LinkedList<>();
    public static LinkedList<Job> wait_queue = new LinkedList<>();
    public static LinkedList<Job> complete_queue = new LinkedList<>();
    public static LinkedList<Job> rejected_queue = new LinkedList<>();

    /* ------ RUNNER FOR SCHEDULER ------ */

    public static void main(String args[]) {
        if (args.length!=1) {
            System.out.println("ERROR: Formatting issue, scheduler requires exactly 1 argument");
        } else {
            File f = new File(args[0]);
            try {
                Scanner s = new Scanner(f);
                while (true) { // do all the scheduling work here
                    try {
                        processInternalEvents(s);
                        processExternalEvents(s);
                        system_time+=system_quantum;
                    } catch (Exception e) {
                        System.out.println("ERROR:" + e.getMessage());
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println("ERROR: Please make sure at " + args[0] + " is a valid path to a file");
                e.printStackTrace();
            }

        }

    }

    /* ------ SYSTEM METHODS ------ */

    static int readInput(Scanner s) throws Exception {
        if(s.hasNextLine()) {
            String input = s.nextLine();
            Pattern p = Pattern.compile("C ([0-9]+) M=([0-9]+) S=([0-9]+) Q=([0-9]+)"); // compiling new patterns is expensive
            Matcher m = p.matcher(input);
            if (m.find()) {
                return systemInit(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
            } else {
                p = Pattern.compile("A ([0-9]+) J=([0-9]+) M=([0-9]+) S=([0-9]+) R=([0-9]+) P=([0-9]+)");
                m = p.matcher(input);
                if (m.find()) {
                    return processNewJob(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));
                } else {
                    p = Pattern.compile("Q ([0-9]+) J=([0-9]+) D=([0-9]+)");
                    m = p.matcher(input);
                    if (m.find()) {
                        return processRequest(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
                    } else {
                        p = Pattern.compile("L ([0-9]+) J=([0-9]+) D=([0-9]+)");
                        m = p.matcher(input);
                        if (m.find()) {
                            return processRelease(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
                        } else {
                            p = Pattern.compile("D ([0-9]+)");
                            m=p.matcher(input);
                            if (m.find()) {
                                return printDisplay(Integer.parseInt(m.group(1)));
                            } else {
                                // TODO: throw error
                                return -1;
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("Scheduling Complete");
            System.exit(0);
            return 0;
        }
    }

    private static int printDisplay(int time) {
        for(Job j: allJobs) {
            System.out.println(j.display());
        }
        return time;
        //TODO: print display

    }

    private static int processRelease(int release_time, int job_no, int released_devices) {

        for(int i=0; i<allJobs.size(); i++) {
            if (allJobs.get(i).getJob_no()==job_no) {
                allJobs.get(i).scheduleRelease(release_time, released_devices);
                return release_time;
            }
        }
        System.err.println("ERROR: processing release for a job that does not exist");
        return -1;
    }

    private static int processRequest(int request_time, int job_no, int devices_requested) {
        for(int i=0; i<allJobs.size(); i++) {
            if (allJobs.get(i).getJob_no()==job_no) {
                allJobs.get(i).addRequest(request_time, devices_requested);
                return request_time;
            }
        }
        System.err.println("ERROR: processing request for a job that does not exist");
        return -1;
    }

    private static int processNewJob(int arrival_time, int job_no, int mem_required, int max_devices, int run_time, int priority) {
        // TODO: error detecting
        Job j = new Job(arrival_time, job_no, mem_required, max_devices, run_time, priority);
        allJobs.add(j);
        if (j.getMemory_required() > system_max_main_memory || j.getMax_devices() > system_max_devices) {
            j.setState(State.REJECTED);
            rejected_queue.add(j);
        } else {
            submit_queue.add(j);
        }
        return arrival_time;
    }

    private static int systemInit(int my_start_time, int my_main_memory, int my_devices, int my_quantum) {
        system_time = my_start_time;
        system_max_main_memory = my_main_memory;
        system_available_memory = my_main_memory;
        system_max_devices = my_devices;
        system_available_devices = my_devices;
        system_quantum = my_quantum;
        return my_start_time;
    }

    private static void releaseMemory(int memory) {
        system_available_memory +=memory;
    }

    private static void processInternalEvents(Scanner s) {
        if (!ready_queue.isEmpty()) {
            processCPU();
        }

        if (!wait_queue.isEmpty()) {
            processWaitQueue(system_quantum);
        }

        for(HoldQueue holdQueue: hold_queues) {
            //processHoldQueue(holdQueue, system_quantum);
        }
        if (system_time+system_quantum >= system_timeStamp && !submit_queue.isEmpty()) {
            processSubmitQueue();
        }
    }

    private static void processWaitQueue(int quantum_interrupt) {
        Iterator<Job> jit = wait_queue.iterator();
        while( jit.hasNext() ) {
            Job j = jit.next();
            if (j.hasRequest(quantum_interrupt)) {
                if (j.processRequest()) { // ********** IMPLEMENT BANKERS ALGORITHM HERE
                    // request granted
                    system_available_devices-=j.getRequestedDevices();
                    j.setState(State.REQUESTING);
                    ready_queue.add(j);
                    jit.remove();
                } else {
                    // request denied
                }
            }
        }
    }

    private static void processCPU() { // round robin
        Job j = ready_queue.pop();
        for(int i=0; i<system_quantum; i++) {
            if (j.getState()==State.READY && j.hasRequest(i)) {
                if (j.processRequest()) { // ********** IMPLEMENT BANKERS ALGORITHM HERE
                    // request granted
                    system_available_devices-=j.getRequestedDevices();
                    ready_queue.add(j);
                    if (!ready_queue.isEmpty()){
                        j = ready_queue.pop();
                    }
                } else {
                    // request denied
                    j.setState(State.WAITING);
                    wait_queue.add(j);
                    if (!ready_queue.isEmpty()) {
                        j=ready_queue.pop();
                    }
                }
            }

            if (j.getState()==State.REQUESTING && j.hasRelease(i)) {
                system_available_devices+=j.getRequestedDevices();
                j.setState(State.READY);
                processWaitQueue(i);
            }

            if (j.getState()== State.FINISHED) {
                releaseMemory(j.getMemory_required());
                complete_queue.add(j);
                if (!ready_queue.isEmpty()) {
                    j = ready_queue.pop();
                }
            }
            j.run();
        }
        ready_queue.add(j);
    }

    private static void processExternalEvents(Scanner s) throws Exception {
        if (system_timeStamp <= system_time) {
            system_timeStamp = readInput(s);
        }
    }

    private static void processSubmitQueue() {
        Job j = submit_queue.peek();
        if (j.getArrival_time() > system_time) {
            // job hasn't arrived yet
        } else if (j.getMemory_required() > system_available_memory) {
            j.setState(State.INHOLDQUEUE);
            hold_queues[j.getPriority()-1].add(j); // hold queues start at 1
            submit_queue.pop();
        } else {
            allocateMemory(j.getMemory_required());
            j.setState(State.READY);
            ready_queue.add(j);
            submit_queue.pop();
        }
    }

    private static void allocateMemory(int memory_required) {
        system_available_memory-=memory_required;
        System.out.println(system_available_memory);
    }



        /* ------ ENUM DEFINITIONS ------ */


    public enum State {
        INHOLDQUEUE, FINISHED, READY, REJECTED, REQUESTING, WAITING
    }

    public enum JobSchedule {
        SJF, FIFO
    }

            /* ------ HOLD QUEUE STRUCT DEFINITION ------ */

    public static class HoldQueue {
        private LinkedList<Job> queue;
        private JobSchedule scheduling;


        public HoldQueue(JobSchedule schedule) {
            this.queue = new LinkedList<>();
            this.scheduling = schedule;
        }

        public void add(Job j) {
            this.queue.add(j);
        }

        public Job peek() {
            return this.queue.peek();
        }

        public Job pop() {
            return this.queue.pop();
        }

        public Job schedule() {
            if (this.scheduling==JobSchedule.SJF) {
                Job shortest=this.queue.peek();
                for (Job j:this.queue) {
                    if (j!=shortest && j.getMemory_required()<shortest.getMemory_required()) {
                        shortest = j;
                    }
                }
                return shortest;
            } else if (this.scheduling == JobSchedule.FIFO){
                return this.queue.pop();
            } else {
                return null;
            }
        }
    }

        /* ------ JOB STRUCT DEFINITION ------ */


    public static class Job {
        private int arrival_time;
        private int job_no;
        private int memory_required;
        private int max_devices;
        private int run_time;
        private int priority;
        private State state;
        private Request req;
        private Release release;

        public boolean hasRequest(int quantum_interrupt) {
            if (req!=null && req.getTime() <= system_time + quantum_interrupt) {
                    return true;
            } else {
                return false;
            }
        }

        public boolean hasRelease(int quantum_interrupt) {
            if (release!=null && release.getTime() <=system_time + quantum_interrupt) {
                return true;
            } else {
                return false;
            }
        }

        public void addRequest(int request_time, int devices_requested) {
            if (devices_requested > this.max_devices) {
                System.err.println("ERROR: requested more devices than job's maximum number of requests");
            } else {
                this.req = new Request(request_time, devices_requested);
            }
        }
        public void scheduleRelease(int release_time, int devices_released) {
            this.req = new Request(release_time, devices_released);
        }

        public boolean processRequest() {
            if (this.req.getDevices()>system_available_devices) {
                this.setState(State.WAITING);
                return false;
            } else {
                this.setState(State.REQUESTING);
                return true;
            }
        }

        public int getRequestedDevices() {
            return this.req.getDevices();
        }

        public void run() {
            this.run_time--;
            if (run_time<=0) {
                if (this.state==State.REQUESTING) {
                    this.releaseDevices();
                }
                this.state=State.FINISHED;
            }
        }

        private void releaseDevices() {
            system_available_devices+=this.getRequestedDevices();
        }


        /* ------ REQUEST STRUCT DEFINITION ------ */

        public static class Request {
            private int time;
            private int devices;

            Request(int t, int d) {
                time=t;
                devices=d;
            }

            public int getTime() {
                return time;
            }

            public void setTime(int time) {
                this.time = time;
            }

            public int getDevices() {
                return devices;
            }

            public void setDevices(int devices) {
                this.devices = devices;
            }
        }

        /* ------ RELEASE STRUCT DEFINITION ------ */

        public static class Release {
            private int time;
            private int devices;

            Release(int t, int d) {
                time=t;
                devices=d;
            }

            public int getTime() {
                return time;
            }

            public void setTime(int time) {
                this.time = time;
            }

            public int getDevices() {
                return devices;
            }

            public void setDevices(int devices) {
                this.devices = devices;
            }
        }

        public void setState(State state) {
            this.state = state;
        }

        public State getState() {
            return this.state;
        }

        public Job(int arrival_time, int job_no, int memory_required, int max_devices, int run_time, int priority) {
            this.arrival_time = arrival_time;
            this.job_no = job_no;
            this.memory_required = memory_required;
            this.max_devices = max_devices;
            this.run_time = run_time;
            this.priority = priority;
            this.state = State.INHOLDQUEUE;
        }

        @Override
        public String toString() { // for debugging purposes
            return ("Arrival time: " + arrival_time + " | job number: " + job_no + " | memory required: " + memory_required + " | max devices required: " +max_devices + " | run time: " + run_time + " | priority: " + priority);
        }

        public String display() {
            // TODO: calculate turnaround time
            if (this.state==State.FINISHED) {
                return "[Job no]  STATE | turnaround time | weighted turnouround time";
            } else if (this.state==State.REJECTED) {
                return "REJECTED, insufficient main memory or devices";
            } else {
                return ("[" + this.job_no + "] State: " + this.state.toString() + " | Remaining service time: " + run_time);
            }
        }

        public int getArrival_time() {
            return arrival_time;
        }

        public int getJob_no() {
            return job_no;
        }

        public int getMemory_required() {
            return memory_required;
        }

        public int getMax_devices() {
            return max_devices;
        }

        public int getRun_time() {
            return run_time;
        }

        public int getPriority() {
            return priority;
        }
    }
}

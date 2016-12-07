package schel;
import java.util.ArrayList;
import java.util.Scanner;

public final class BankAlgo{
	private BankAlgo(){}//private constructor

	public static boolean safeCheck(int [] avali, int[][] max, int[][] alloc){
		//check if the current state is safe
		int n = max.length;//number of processes
		int[][] need = matSub(max, alloc);
		int[] work = avali;
		boolean[] finish = new boolean[n];
		for (int i = 0; i<n; i++){
			finish[i] = false;
		}
		
		int doneJobs = 0;
		while(doneJobs<n){
			boolean didAlloc = false;
			for(int i = 0; i < n; i++){
				if(finish[i] == false && check(work, need[i])){
					work = vecAdd(work, alloc[i]);
					finish[i] = true;
					didAlloc = true;
					doneJobs++;
				}

			}
			if(didAlloc == false){
				if(doneJobs<n){
					return false;
				}
			}
		}
		return true;
	}

	public static boolean reqCheck(int[] req, int procNum, int[] avali, int[][] max, int[][] alloc){
		//check if a request can be allocatted and still be in a safe state
		int[][] need = matSub(max, alloc);
		if (check(need[procNum],req)){
			if(check(avali,req)){
				avali = vecSub(avali, req);
				int[] allocTmp = vecAdd(alloc[procNum], req);
				alloc[procNum] = allocTmp;
				int[] needTmp = vecSub(need[procNum], req);
				need[procNum] = needTmp;
				if(safeCheck(avali, max, alloc)){
					return true;
				}
			}
		}
		return false;
	}

	public static boolean runReq(Scheduler.Job reqJob, ArrayList<Scheduler.Job> jobs, int avaliDev){
			int[] req = {reqJob.getRequestedDevices()};
			int procNum = reqJob.getJob_no();
			int[] avali = {avaliDev};
			int[][] max = new int[jobs.size()][1];
			int[][] alloc = new int[jobs.size()][1];
			for(int i = 0 ; i < jobs.size(); i++){
				max[i][0] = jobs.get(i).getMax_devices();
				if(i != procNum){
					alloc[i][0] = jobs.get(i).getRequestedDevices();
				}
			}
			return reqCheck(req, procNum, avali, max, alloc);
	}
		

	private static boolean check(int[] avali, int[] need){
		//checks that everything in avali is greater than the need
		for(int j=0;j<need.length;j++){
			if(avali[j]<need[j]){
				return false;
			}
		}
		return true;
	}

	public static int[] vecAdd(int[] x, int[] y){
		//adds two integer arrays
		int n = x.length;//length of vector
		int[] result = new int[n];
		for (int i = 0; i < n; i++){
			result[i] = x[i] + y[i];
		}
		return result;
	}

	public static int[] vecSub(int[] x, int[] y){
		int n = x.length;//length of vector
		int[] result = new int[n];
		for (int i = 0; i < n; i++){
			result[i] = x[i] - y[i];
		}
		return result;
	}

	public static int[][] matAdd(int[][] x, int[][] y){
		int n = x.length;//number of rows
		int m = x[0].length;//number of columns
		int[][] result = new int[n][m];
		for (int i = 0; i < n; i++){
			for (int j = 0; j < m; j++){
				result[i][j] = x[i][j] + y[i][j];
			}
		}
		return result;
	}

	public static int[][] matSub(int[][] x, int[][] y){
		int n = x.length;//numbe:wr of rows
		int m = x[0].length;//number of columns
		int[][] result = new int[n][m];
		for (int i = 0; i < n; i++){
			for (int j = 0; j < m; j++){
				result[i][j] = x[i][j] - y[i][j];
			}
		}
		return result;
	}


	public static void main(String args[]){
		//derived from http://javaingrab.blogspot.com/2013/06/implementation-of-bankers-algorithm.html
		int need[][],allocate[][],max[][],avail[][],req[][],np,nr;
		Scanner sc=new Scanner(System.in);
		System.out.print("Enter no. of processes and resources : ");
		np=sc.nextInt();  //no. of process
		nr=sc.nextInt();  //no. of resources
		need=new int[np][nr];  //initializing arrays
		max=new int[np][nr];
		allocate=new int[np][nr];
		avail=new int[1][nr];
		req=new int[1][nr];

		System.out.println("Enter allocation matrix -->");
		for(int i=0;i<np;i++)
			for(int j=0;j<nr;j++)
				allocate[i][j]=sc.nextInt();  //allocation matrix

		System.out.println("Enter max matrix -->");
		for(int i=0;i<np;i++)
			for(int j=0;j<nr;j++)
				max[i][j]=sc.nextInt();  //max matrix

		System.out.println("Enter available matrix -->");
		for(int j=0;j<nr;j++)
			avail[0][j]=sc.nextInt();  //available matrix
		
		System.out.println("Enter request vector -->");
		for(int j=0;j<nr;j++)
			req[0][j]=sc.nextInt();  //request matrix

		sc.close();		
		System.out.println(safeCheck(avail[0], max, allocate));
		System.out.println(reqCheck(req[0], 0, avail[0], max, allocate));
	}

}

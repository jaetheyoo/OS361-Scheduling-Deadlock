all: comp run-scheduler run-bankAlgo

comp: BankAlgo.java Scheduler.java
	mkdir -p ../../bin
	javac BankAlgo.java Scheduler.java -d ../../bin

run-scheduler: comp
	java -cp ../../bin/ schel.Scheduler example.txt

run-bankAlgo: comp
	java -cp ../../bin/ schel.BankAlgo

clean:
	rm -r ../../bin

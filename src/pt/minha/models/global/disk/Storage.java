package pt.minha.models.global.disk;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.TreeMap;

import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.Timeline;

public class Storage extends Event{
	/* Global */
	private final int diskDebit; //bytes per milisecond
	private boolean running = false;
	private TreeMap<Integer, Integer> streams;
	private Map<Integer, List<Event>> blocked = new TreeMap<Integer, List<Event>>();
	/* --- */
	
	/*Write info*/
	private final int bucketSize;
	private int bucketFreeSize;
	private final double writeStable, writeDecl; 
	
	private LinkedList<Request> bucket;
	private LinkedList<Request> queue;
	/* ---------- */
	
	/*Read info*/
	private final double readStable, readDecl;
	/* ---------- */
	
	/***
	 * Constructor for clone
	 * @param s - Storage instance
	 */
	private Storage(Storage s){
		super(s.getTimeline());
		this.diskDebit = s.getDiskDebit();
		this.bucketSize = s.getBucketSize();
		this.bucketFreeSize = this.bucketSize;
		this.bucket = new LinkedList<Request>();
		this.queue = new LinkedList<Request>();
		this.streams = new TreeMap<Integer, Integer>();
		this.readDecl = s.getReadDecl();
		this.readStable = s.getReadStable();
		this.writeDecl = s.getWriteDecl();
		this.writeStable = s.getWriteStable();
	}
	
	/***
	 * Constructor for properties
	 * @param prop - properties
	 */
	public Storage(final Properties prop, Timeline timeline){
		super(timeline);
		this.diskDebit = Integer.parseInt(prop.getProperty("diskDebit"));
		this.bucketSize = Integer.parseInt(prop.getProperty("bucketSize"));
		this.bucketFreeSize = this.bucketSize;
		this.bucket = new LinkedList<Request>();
		this.queue = new LinkedList<Request>();
		this.streams = new TreeMap<Integer, Integer>();
		this.readDecl = Double.parseDouble(prop.getProperty("readDecl"));
		this.readStable = Double.parseDouble(prop.getProperty("readStable"));
		this.writeDecl = Double.parseDouble(prop.getProperty("writeDecl"));
		this.writeStable = Double.parseDouble(prop.getProperty("writeStable"));
	}
	
	// Getters
	
	private int getDiskDebit(){
		return this.diskDebit;
	}
	
	private int getBucketSize(){
		return this.bucketSize;
	}
	
	private double getReadDecl(){
		return this.readDecl;
	}
	
	private double getReadStable(){
		return this.readStable;
	}
	
	private double getWriteDecl(){
		return this.writeDecl;
	}
	
	private double getWriteStable(){
		return this.writeStable;
	}

	// ---
	
	public void addBlock(Event ev, int idStream){
		List<Event> events = this.blocked.get(idStream);
		
		if(events == null) events = new LinkedList<Event>();
		
		events.add(ev);
		
		this.blocked.put(idStream, events);
	}
	
	private void notifyEvents(int idStream){
		if(!this.blocked.containsKey(idStream)) return;
		
		List<Event> events = this.blocked.get(idStream);
		
		for(Event ev : events)
			ev.schedule(0);
		
		events.clear();
	}
	
	private long calcCost(int length, RequestType type){
		double stable = 0, decl = 0;
		
		if(type.equals(RequestType.WRITE)){
			stable = this.writeStable;
			decl = this.writeDecl;
		}
		if(type.equals(RequestType.READ)){
			stable = this.readStable;
			decl = this.readDecl;
		}
		
		//TODO: Review this return
		return (long) (decl * length + stable);
	}
	
	public boolean hasStream(int idStream){
		if(!this.streams.containsKey(idStream)) return false;
		if(this.streams.get(idStream) > 0) return true;
		
		return false;
	}
	
	private void incStream(final Request r){
		Integer n = this.streams.get(r.getIdStream());
		
		if(n==null) n = 0;
		
		this.streams.put(r.getIdStream(), ++n);
	}
	
	private void decStream(final Request r){
		int n = this.streams.get(r.getIdStream());
		
		this.streams.put(r.getIdStream(), --n);
		
		if(n == 0) {
			this.notifyEvents(r.getIdStream());
		}
	}
	
	/***
	 * Function for adding a new request to a disk and delegate it
	 * @param r request
	 */
	public void addRequest(Request r) {
		if(r.getReqType().equals(RequestType.READ)) this.handleReads(r);

		if(r.getReqType().equals(RequestType.WRITE)) this.handleWrites(r);
	}
	
	/***
	 * Function responsible for handling read request
	 * @param r - request
	 */
	private void handleReads(Request r){
		long cost = this.calcCost(r.getLength(), r.getReqType());
		
		r.done(cost);
	}
	
	/***
	 * Function responsible for handling write request
	 * @param r - request
	 */
	private void handleWrites(Request r){
		this.incStream(r);
		
		if(!this.queue.isEmpty()) {
			this.addQueue(r);
			return;
		}
		
		if(r.buffer(this.bucketFreeSize) > 0){
			this.addQueue(r);
		}else{
			this.addBucket(r);
		}
	}
	
	/***
	 * Function for adding the request to the bucket
	 * @param r request
	 */
	private void addBucket(Request r) {
		r.setInBucket();
		
		long cost = this.calcCost(r.getLength(), r.getReqType());
			
		r.done(cost);
				
		this.bucket.add(r);
		this.bucketFreeSize -= r.getLength();
		
		if(!this.running) {
			this.schedule(0);
			this.running = true;
		}
	}

	/***
	 * Function for adding the request to the queue
	 * @param r request
	 */
	private synchronized void addQueue(Request r) {
		this.queue.add(r);
	}
	
	/***
	 * Function responsible for free a bucket size
	 * @param size - space to be freed
	 */
	private void freeBucket(int size){
		this.bucketFreeSize += size;
		
		do{
			Request r = null;
			
			try{
				r = this.queue.getFirst();
			}catch(NoSuchElementException e){
				return;
			}
	
			if(r.buffer(this.bucketFreeSize) < 1) {
				this.queue.removeFirst();
				this.notifyEvents(r.getIdStream());
			}
			
			if(!r.inBucket()) this.addBucket(r);
			
		}while(this.bucketFreeSize > 0);
	}
	
	/***
	 * Function that manages the process of processing orders
	 */
	private void processPack(){
		int diskDeb = this.diskDebit;
		//TODO: review code
		Request r = null;
		
		do{
			try{
				r = this.bucket.getFirst();
			}catch(NoSuchElementException e){
				this.freeBucket(0);
				break;
			}
			
			diskDeb -= r.process(diskDeb);
			
			if(r.isDone()) {
				this.bucket.removeFirst();
				this.decStream(r);
			}
			
		}while(diskDeb > 0);
		
		this.freeBucket(this.diskDebit - diskDeb);
	}

	@Override
	public void run() {
		this.processPack();
		if(this.bucket.isEmpty()) this.running = false;
		else this.schedule(1000000);
	}
	
	public Storage clone(){
		return new Storage(this);
	}
	
	public String toString(){
		StringBuilder str = new StringBuilder();
		str.append("Storage calibration\n");
		str.append("diskDebit: " + this.diskDebit + " bytes/ms\n");
		str.append("bucketSize: " + this.bucketSize + " bytes\n");
		
		return str.toString();
	}

}

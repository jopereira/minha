package pt.minha.models.global.disk;


public class Request {
	
	private final RequestType reqType;
	private int length;
	private int waitingSize;
	private final int idStream;
	private final Storage st;
	private long cost;
	private boolean done;
	private boolean inBucket;

	public Request(RequestType requestType, int length, int idStream, Storage st) {
		this.done = false;
		this.st = st;
		this.cost = 0;
		this.reqType = requestType;
		this.length = length;
		this.waitingSize = this.length;
		this.idStream = idStream;
		this.inBucket = false;
	}

	/**
	 * @return the reqType
	 */
	public RequestType getReqType() {
		return reqType;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * @return the idFile
	 */
	public int getIdStream() {
		return this.idStream;
	}
	
	/**
	 * @return the done
	 */
	public boolean isDone() {
		return this.done;
	}
	
	public int getWaitingSize(){
		return this.waitingSize;
	}
	
	/**
	 * Function for decrement the number of bytes that will be copy to the bucket
	 * @param size - number of bytes to be copy
	 * @return number of byte remaining
	 */
	public int buffer(int size){
		this.waitingSize -= size;
		
		return this.waitingSize;
	}
	
	/**
	 * Function for subtract the length
	 * This function is just for bucket control
	 * @return the quantity of debit used
	 */
	public int process(int diskDebit) {
		this.length -= diskDebit;
		if(this.length < 0) {
			this.done = true;
			return diskDebit + this.length;
		}
		return diskDebit;
	}
	
	/**
	 * Function for test if request is already in the bucket
	 * @return if in bucket
	 */
	public boolean inBucket(){
		return this.inBucket;
	}
	
	public void setInBucket(){
		this.inBucket = true;
	}

	/***
	 * Function for added the request to be handle by disk
	 */
	public void execute(){
		this.st.addRequest(this);
	}
	
	public long getCost(){
		return this.cost;
	}
	
	/***
	 * Function responsible for return and calculate the request time to handle
	 * @param cost - time to handle
	 */
	public void done(long cost) {
		this.cost = cost;
	}

}

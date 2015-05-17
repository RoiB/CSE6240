package movie.util;

public class Pair<T1, T2> {
	
	private T1 first;
	private T2 second;
	
	public Pair(T1 t1, T2 t2) {
		this.first = t1;
		this.second = t2;
	}
	
	public T1 getFirst() {
		return this.first;
	}
	
	public T2 getSecond() {
		return this.second;
	}
	
}

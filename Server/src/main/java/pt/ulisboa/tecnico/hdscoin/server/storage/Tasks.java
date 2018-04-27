package pt.ulisboa.tecnico.hdscoin.server.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tasks {
	
	private Map<Integer, List<String>> receivedTasks;
	
	public Tasks() {
		this.receivedTasks = new HashMap<Integer, List<String>>();
	}
	public Tasks(String filename) {
		if(/*filename existes*/ true){
			this.receivedTasks = new HashMap<Integer, List<String>>();//eleminar depois de feito abaixo
			//this.receivedTasks = //ir buscar ao storage o ficheiro guardado e passar para mapa
		}else {
			this.receivedTasks = new HashMap<Integer, List<String>>();
		}
	}
	
	
	public List<String> getTask(Integer id){
		return this.receivedTasks.get(id);
	}
	public List<String> getLastTask(){
		return this.receivedTasks.get(this.receivedTasks.size());
	}
	
	public void addTask(int id, List<String> tasks) {
		this.receivedTasks.put(id, tasks);
	}
	public void removeTask(int id) {
		this.receivedTasks.remove(id);
	}
	
	
	
	//criar ficheiro com as tasks todas json?
	
	

}

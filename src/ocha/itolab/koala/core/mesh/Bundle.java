package ocha.itolab.koala.core.mesh;

import java.util.*;
import ocha.itolab.koala.core.data.*;

public class Bundle {
	/*id1->id2ÇÃñÓàÛ*/
	int id1;
	int id2;
	int rotation=0;
	int num_connected; //id1->id2ÇÃñ{êî
	int num_connecting; //id2->id1ÇÃñ{êî
	double torqen_connected;
	double torqen_connecting;
	int verts_connected;
	int verts_connecting;
	ArrayList<Integer> merge_connecting; //vertÇÃî‘çÜÇ™äiî[Ç≥ÇÍÇƒÇÈ
	ArrayList<Integer> merge_connected;
	double angle; //connectedIDÅ®connectingIDÇÃäpìx
	
	double connecting_angle;
	double connected_angle;
	
	public Bundle(int num1, int num2){
		id1 = num1;
		id2 = num2;
		rotation=0;
		num_connected=0;
		num_connecting=0;
		torqen_connected=0;
		torqen_connecting=0;
		verts_connected=0;
		verts_connecting=0;
		angle=-1;
		merge_connecting = null;
		merge_connected = null;
		connecting_angle=-1;
		connected_angle=-1;
	}
	
	public void resetBundle(int id1, int id2){
		rotation=0;
		num_connected=0;
		num_connecting=0;
		torqen_connected=0;
		torqen_connecting=0;
		verts_connected=0;
		verts_connecting=0;
		angle=-1;
		merge_connecting = null;
		merge_connected = null;
	}
	
	public void setRotation(int i){
		rotation = i;
	}
	
	public int getRotation(){
		return rotation;
	}
	
	public int getID1(){
		return id1;
	}

	public int getID2(){
		return id2;
	}
	
	public void changeRotation(){
		rotation = rotation*(-1);
	}
	
	public void changeRotation(int i){
		if(rotation!=0)
			rotation = i;
	}
	
	public void setNum(int connecting, int connected){
		num_connecting = connecting;
		num_connected = connected;
	}
	
	public int getConnectedNum(){
		return num_connected;
	}
	
	public int getConnectingNum(){
		return num_connecting;
	}
	
	public void setTorqen(double num1,double num2){
		torqen_connected = num1;
		torqen_connecting = num2;
	}	

	public double getTorqenConnected(){
		return torqen_connected;
	}
	
	public double getTorqenConnecting(){
		return torqen_connecting;
	}
	
	public int getVertsConnected(){
		return verts_connected;
	}
	
	public int getVertsConnecting(){
		return verts_connecting;
	}
	
	public void setVertsConnected(int num){
		verts_connected = num;
	}
	
	public void setVertsConnecting(int num){
		verts_connecting = num;
	}
	
	public void setAngle(double a){
		angle = a;
	}
	
	public double getAngle(){
		return angle;
	}
	
	public void setConnectingMerge(int i){
		if(merge_connecting==null)
			merge_connecting = new ArrayList<Integer>();
		
		merge_connecting.add(i);
	}
	
	public void setConnectedMerge(int i){
		if(merge_connected==null)
			merge_connected = new ArrayList<Integer>();
		
		merge_connected.add(i);
	}
	
	public void resetConnectedMerge(){
		merge_connected=null;
	}
	
	public void resetConnecingdMerge(){
		merge_connecting=null;
	}
	
	public void setConnectingMerge(ArrayList<Integer> list){
		merge_connecting = list;
	}
	
	public void setConnectedMerge(ArrayList<Integer> list){
		merge_connected = list;
	}
	
	public void addConnectingMerge(ArrayList<Integer> list){
		for(int l=0;l<list.size();l++){
			int n = list.get(l);
			if(merge_connecting.indexOf(n) == -1){
				merge_connecting.add(n);
			}
		}
	}
	
	public void addConnectedMerge(ArrayList<Integer> list){
		for(int l=0;l<list.size();l++){
			int n = list.get(l);
			if(merge_connected.indexOf(n) == -1){
				merge_connected.add(n);
			}
		}
	}
	
	public ArrayList<Integer> getConnectingMerge(){
		return merge_connecting;
	}
	
	public ArrayList<Integer> getConnectedMerge(){
		return merge_connected;
	}
	
}



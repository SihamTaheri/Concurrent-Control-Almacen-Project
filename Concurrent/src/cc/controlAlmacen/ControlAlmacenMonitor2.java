package cc.controlAlmacen;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;

import es.upm.babel.cclib.Monitor;


public class ControlAlmacenMonitor2 implements ControlAlmacen {
	
	
	//ORDEN DEL ARRAY !!!!!!!!!!
	
	private Map<String,Integer>  enCamino;
	private Map<String,Integer>  compradosGEN;

	
	private Map<String,Integer>  disponibles;
	
	private Map<String, Integer> prodmin;
	
	private Map<String,Map<String,Integer>> clientesypro; // y su productos
	
	
	
	// Iterar a traves de las claves y establecer el valor en 0
	
	//private Map<String, Integer[]> productos;
	//idea descartada porque para el constructor no podemos obtener los strings 
	
	// si no, como relacion un producto con todas sus caracteristicas???
	private Monitor mutex;
	//private Monitor.Cond condComprar;

	private Collection<PetBuf> espBuf;
	
	
	
	class PetBuf{
		
		public String producto; 
		public boolean esEntrega;
		public boolean esOfrecer;
		
		Monitor.Cond condicionENTR;
		Monitor.Cond condicionREAB;
		
		   int cantidad;
		   int disponibles1;
		   int enCamino1;
		   int comprados1;
		   int min1;
		   
	
		public PetBuf(String itemId, int cantidad, Monitor m, int disponibles) {
			esEntrega=true;
			esOfrecer=false;
			producto=itemId;
			disponibles1=disponibles;
			this.cantidad=cantidad;
			condicionENTR=m.newCond();
		}
		public PetBuf(String itemId, Monitor m, int disponible, int encam, int compr, int mini) {
			esOfrecer=true;
			esEntrega=false;
			producto=itemId;
			this.condicionREAB=m.newCond();
			this.disponibles1=disponible;
		    this.enCamino1=encam;
			this.comprados1=compr;
			this.min1=mini;
		}
	}
	
	// Resource state
   // ...
//HACER LO MISMO DE TENEDORES CON PRODUCTOS DE ESTE ALMACEN? PERO COMO HAREMOS EL REABASTECER
  // Monitors and conditions
  // ... 
//establecer un maximo no?
	
  public ControlAlmacenMonitor2(Map<String,Integer> tipoProductos) {
	  
	  mutex=new Monitor();
	  espBuf= new LinkedList<PetBuf>();
	  
	  clientesypro = new HashMap<String,Map<String,Integer>>();
	  
	  
	  this.prodmin=new HashMap<String,Integer>(tipoProductos);
	  this.disponibles=new HashMap<String,Integer>();
			  
	  enCamino=new HashMap<String,Integer>();
	  compradosGEN=new HashMap<String,Integer>();
	  
  }
  
  public boolean comprar(String clientId, String itemId, int cantidad) {
	  
	 boolean verdadero=false;
	  
	  if(cantidad <=0 || !prodmin.containsKey(itemId)) {
		  throw new IllegalArgumentException(new Exception("NO cumple la precondicion"));
	  }
	  
	   mutex.enter(); // siempre // chequeo de la CPRE y posible bloqueo
	  //if (clientesypro.get(clientId)[0].get(itemId) + clientesypro.get(clientId)[1].get(itemId) >=  clientesypro.get(clientId)[2].get(itemId) + cantidad ) { 
	//	  condComprar.await();
	 // } 
	    // implementacion de la POST Aqu hay que hacer lo que te pida devolver la acci . No es comn porque depende de la acci que hay que realizar 
  		
	    if(compradosGEN.get(itemId) == null) { //NUNCA SE HA HECHO UNA COMPRA DE ESTE PRODUCTO
	    	  compradosGEN.put(itemId, 0);
	    }
	    
	    if(enCamino.get(itemId)== null) {
			enCamino.put(itemId,0);
		}

	    if(clientesypro.get(clientId) == null || clientesypro.get(clientId).get(itemId) == null ) {
	    	Map<String, Integer> meter = new HashMap<String,Integer>();
	    	meter.put(itemId, 0);
	    	clientesypro.put(clientId, meter);
	    }
	    
	    if(disponibles.get(itemId) == null) { //NUNCA SE HA HECHO UNA COMPRA DE ESTE PRODUCTO
	    	  disponibles.put(itemId, 0);
	    }
	      
   	    verdadero = disponibles.get(itemId) + enCamino.get(itemId) >=  compradosGEN.get(itemId) + cantidad;
	    
   	    if(verdadero)  {
	  
	    this.clientesypro.get(clientId).put(itemId, clientesypro.get(clientId).get(itemId) + cantidad);
	    
	    compradosGEN.put(itemId, compradosGEN.get(itemId)+cantidad); 
	    }
	    	
		  
 
	  	desbloqueo_generico(); // cdigo de desbloqueo 
  		// invariante en el caso de que algo deba // cumplirse siempre. Debe saltar fallo si no lo // hace . 
  		mutex.leave(); 
  		
  		int valorCam=0;

    return verdadero;
  }

  
  

  public void entregar(String clientId, String itemId, int cantidad) {
	 
	  
	  
	  if(cantidad <=0 || !prodmin.containsKey(itemId)) {
		  throw new IllegalArgumentException(new Exception("NO cumple la precondicion"));
	  }
	  mutex.enter();
	  
	  int disponibl=0;
	  
	  if(disponibles.get(itemId) == null) { //NUNCA SE HA HECHO UNA COMPRA DE ESTE PRODUCTO
    	  disponibles.put(itemId, 0);
    }
	  
	  if(clientesypro.get(clientId) == null ) {
	    	Map<String, Integer> meter = new HashMap<String,Integer>();
	    	meter.put(itemId, 0);
	    	clientesypro.put(clientId, meter);
	    }
	  
	  
	  if(clientesypro.get(clientId).get(itemId) == null ) {
		  Map<String, Integer> meter = new HashMap<String,Integer>();
	    	meter.put(itemId, 0);
	    	clientesypro.put(clientId, meter);
	  }
	  
	  
	  if (disponibles.get(itemId) < cantidad) {  //aqui meto la precondicion de entregar
		  PetBuf pet = new PetBuf(itemId,cantidad,mutex,disponibl);
		  espBuf.add(pet);
		  pet.condicionENTR.await();
	  }
	  
	  
	  disponibles.put(itemId, disponibles.get(itemId)- cantidad);
	  

	  this.clientesypro.get(clientId).put(itemId, clientesypro.get(clientId).get(itemId) - cantidad);
	  compradosGEN.put(itemId, compradosGEN.get(itemId)-cantidad);

	  
	  desbloqueo_generico();
	 
	  mutex.leave();
  }

  public void devolver(String clientId, String itemId, int cantidad) {
	 
	  if(cantidad <=0 || !prodmin.containsKey(itemId)) {
		  throw new IllegalArgumentException(new Exception("NO cumple la precondicion"));
	  }
	  
	  mutex.enter();
	  
	  
	  disponibles.put(itemId, disponibles.get(itemId)+cantidad);
	  
	  desbloqueo_generico();
	 
	  mutex.leave();
  }

  public void ofrecerReabastecer(String itemId, int cantidad) {
	  
	  if(cantidad <=0 || !prodmin.containsKey(itemId)) {
		  throw new IllegalArgumentException(new Exception("NO cumple la precondicion"));
	  }
	  
	  mutex.enter(); // siempre // chequeo de la CPRE y posible bloqueo

		
		if(enCamino.get(itemId)==null) {
			enCamino.put(itemId,0);
		}
		
		if(compradosGEN.get(itemId) == null) { //NUNCA SE HA HECHO UNA COMPRA DE ESTE PRODUCTO
	    	  compradosGEN.put(itemId, 0);
	    }
		if(disponibles.get(itemId) == null) { //NUNCA SE HA HECHO UNA COMPRA DE ESTE PRODUCTO
	    	  disponibles.put(itemId, 0);
	    }
	 
	   //EN EL ULTIMO NO ES LA SUMA YA QUE DEBERIA SER EL MISMO PRECIO MINIMO PARA TODOS LOS CLIENTES EN EL MISMO PRODUCTO
	
	  if (disponibles.get(itemId)+enCamino.get(itemId) - compradosGEN.get(itemId) >= prodmin.get(itemId)) { 
		  PetBuf pet = new PetBuf(itemId,mutex,disponibles.get(itemId),enCamino.get(itemId),compradosGEN.get(itemId),prodmin.get(itemId));
		  espBuf.add(pet);
		  pet.condicionREAB.await();
		  
	  } 
	  
		enCamino.put(itemId, enCamino.get(itemId)+cantidad);
		
		desbloqueo_generico(); // cdigo de desbloqueo 
 		//invariante(); // invariante en el caso de que algo deba // cumplirse siempre. Debe saltar fallo si no lo // hace . 
 		mutex.leave(); 

  }

  public void reabastecer(String itemId, int cantidad) {
	  
	  
	  if(cantidad <=0 || !prodmin.containsKey(itemId)) {
		  throw new IllegalArgumentException(new Exception("NO cumple la precondicion"));
	  }
	  
	  mutex.enter();
	 
	  disponibles.put(itemId, disponibles.get(itemId)+cantidad);
	  
	  enCamino.put(itemId, enCamino.get(itemId)-cantidad);
  
	  desbloqueo_generico(); // cigo de desbloqueo 
	 // invariante en el caso de que algo deba // cumplirse siempre. Debe saltar fallo si no lo // hace . 
	mutex.leave();
  }
  
  public void desbloqueo_generico() {
	  

	 
	  Iterator<PetBuf> it = espBuf.iterator();
	  
	  boolean encontradoREAB=false;
	  boolean encontradoENTRE=false;
	  
	  boolean elanterior=true;
	
	  while( it.hasNext() && elanterior) {
          PetBuf p = it.next();
          // Realiza las operaciones necesarias con petBuf
		  
		  if(p.esEntrega) {
			  encontradoENTRE=disponibles.get(p.producto)>= p.cantidad;
		  }
		  else if(p.esOfrecer) {
			  encontradoREAB= (disponibles.get(p.producto)+ enCamino.get(p.producto) - compradosGEN.get(p.producto) < prodmin.get(p.producto));
		  }
		  
		  elanterior=false;
		  
		  if(encontradoREAB && p.condicionREAB !=null && p.condicionREAB.waiting() > 0) {
			   it.remove();
			   p.condicionREAB.signal();
			 
			   elanterior=true;
			  
		  }else	if(encontradoENTRE && p.condicionENTR !=null && p.condicionENTR.waiting() > 0) {
			  it.remove();
			  p.condicionENTR.signal();
			  
			  elanterior=true;
		  }
		  
	  }
	  
	  
  }
 
}


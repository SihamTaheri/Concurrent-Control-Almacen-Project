package cc.controlAlmacen;



import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import es.upm.babel.cclib.Monitor;

//---------------------------------------------------
//TRABAJO REALIZADO POR SIHAM TAHERI Y MIRIAM BERNAT 
//---------------------------------------------------

public class ControlAlmacenMonitor implements ControlAlmacen {

	private Map<String,Integer>  enCamino;
	private Map<String,Integer>  compradosGEN;
	private Map<String,Integer>  disponibles;
	private Map<String, Integer> prodmin;
	
	private Map<String,Map<String,Integer>> clientesypro; // y su productos
	
	public Map<String,String> valoresiesta;
	
	
	
	private Monitor mutex;// Monitor mutex que se asegura que se respete la exclusin mutua


	private Collection<PetBuf> espBufentrega; //Buffer que almacena todas las peticiones de entrega
	private Collection<PetBuf> espBufofrece;//Buffer que almacena todas las peticiones de ofrece
	
	
//CREAMOS UNA CLASE PETICIONES PARA ALMACENAR TANTO ELLAS COMO SUS CARACTERISTICAS
	class PetBuf{		
		public String producto; //String que va a almacenar el nombre del producto, y que accederemos en el desbloqueo_generico 
    	Monitor.Cond entrega; //Monitor.cond que gestiona las entregas
		Monitor.Cond reabastecer; //Monitor.cond que gestiona las reabasticiones
		
		int cantidad;
		   
		public PetBuf(String itemId, int cantidad, Monitor m) {	//Constructor empleado para las peticiones de entrega y sus elementos
			producto=itemId;
			entrega=m.newCond();	//establecemos un nuevo monitor.condition para las entregas con nuestro monitor mutex pasado como parametro
			this.cantidad=cantidad;
			
		}
		public PetBuf(String itemId, Monitor m) {//Constructor empleado para las peticiones de entrega y sus elementos
			producto=itemId; 
			reabastecer=m.newCond();  //establecemos un nuevo monitor.condition para las entregas con nuestro monitor mutex pasado como parametro	
		}
	}
	
	
  public ControlAlmacenMonitor(Map<String,Integer> tipoProductos) {
	  
	  mutex=new Monitor();
	  espBufofrece= new LinkedList<PetBuf>();
	  espBufentrega= new LinkedList<PetBuf>();
	  
	  clientesypro = new HashMap<String,Map<String,Integer>>(); //Mapa que utilizaremos para gestionar los clientes y los productos comprados por ellos
	  
	  
	  this.prodmin=new HashMap<String,Integer>(tipoProductos); //establecemos que el mapa tipoProductos pasado como parametro sea nuestro mapa prodmin perteneciente a la clase 
	  
	  //Mapas que utilizaremos en nuestra clase para gestionar cada producto(String) con la cantidad de disponibles, enCamino y comprados que tiene
	  this.disponibles=new HashMap<String,Integer>();		  
	  this.enCamino=new HashMap<String,Integer>();
	  this.compradosGEN=new HashMap<String,Integer>();
	  
  }
  
  public boolean comprar(String clientId, String itemId, int cantidad) {  
	 
	  //Evaluamos la precondicion antes de entrar en el cuerpo del metodo, en caso de que esta no se cumpla, lance una excepcion
	  
	  
	   mutex.enter();
	   
	   if(cantidad <=0 || !prodmin.containsKey(itemId)) {
		   mutex.leave();
		  throw new IllegalArgumentException(new Exception("No cumple la precondicion"));
	  }
	   boolean verdadero=false;  
	   //Inicializamos cada uno de los mapas con el producto, en caso de que nunca se haya hecho una compra con ese producto
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
	      
	    //Evaluamos la POST del metodo comprar y almacenamos su valor en verdadero para devolverlo como resultado del metodo
	    
	    //Sabemos que si se cumple, y por lo tanto devuelve true, se agrega a comprados la cantidad de productos pasado, por lo contrario si devuelve false, dejamos los valores como estaban. 
   	    verdadero = disponibles.get(itemId) + enCamino.get(itemId) >=  compradosGEN.get(itemId) + cantidad;
	    
   	    if(verdadero)  {
	    this.clientesypro.get(clientId).put(itemId, clientesypro.get(clientId).get(itemId) + cantidad);
	    this.compradosGEN.put(itemId, compradosGEN.get(itemId)+cantidad); 
	    }
	    	
		  
 
   	    desbloqueo_general(); //Llamamos a desbloqueo_general en caso de que alguno de los parametros cambiados permiten liberar alguna peticion en espera 
  		mutex.leave(); 
    return verdadero;
  }

  
  

  public void entregar(String clientId, String itemId, int cantidad) {
	 
	  
	//Evaluamos la precondicion antes de entrar en el cuerpo del metodo, en caso de que esta no se cumpla, lance una excepcion
	 
	  
	  
	  mutex.enter();
	  if(cantidad <=0 || !prodmin.containsKey(itemId)) {
		   mutex.leave();
		  throw new IllegalArgumentException(new Exception("No cumple la precondicion"));
	  }
 
	  //Inicializamos cada uno de los mapas con el producto, en caso de que nunca se haya hecho una entrega con ese producto
	  
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
	  
	  
	  //EVALUAMOS LA CPRE DE ENTREGAR 
	  if (disponibles.get(itemId) < cantidad) { 
		  //Si no se cumple, y por lo tanto a entrado aqui, guardamos la peticion
		  PetBuf pet = new PetBuf(itemId,cantidad,mutex); 
		  espBufentrega.add(pet);//lo anadimos a nuestro buffer entrega, para luego sacarlo al hacer el desbloqueo
		  pet.entrega.await(); //lo mantenemos a la espera
	  }
	  
	  //Cuerpo del metodo(POST)
	  disponibles.put(itemId, disponibles.get(itemId)- cantidad);
	  compradosGEN.put(itemId, compradosGEN.get(itemId)-cantidad);

	  
	  desbloqueo_general();//Llamamos a desbloqueo_general en caso de que alguno de los parametros cambiados permiten liberar alguna peticion en espera 
	  mutex.leave();
  }

  public void devolver(String clientId, String itemId, int cantidad) {
	 
	  //Evaluamos la precondicion antes de entrar en el cuerpo del metodo, en caso de que esta no se cumpla, lance una excepcion
	  
	  
	  mutex.enter();
	  
	  if(cantidad <=0 || !prodmin.containsKey(itemId)) {
		   mutex.leave();
		  throw new IllegalArgumentException(new Exception("No cumple la precondicion"));
	  }
	  //CUERPO DEL METODO, anadimos la cantidad a nuestro mapa disponibles 
	  disponibles.put(itemId, disponibles.get(itemId)+cantidad);
	  
	  desbloqueo_general();//Llamamos a desbloqueo_general en caso de que alguno de los parametros cambiados permiten liberar alguna peticion en espera 

	  mutex.leave();
  }

  public void ofrecerReabastecer(String itemId, int cantidad) {
		
	  //Evaluamos la precondicion antes de entrar en el cuerpo del metodo, en caso de que esta no se cumpla, lance una excepcion
	  
	   mutex.enter(); 
	   
	   if(cantidad <=0 || !prodmin.containsKey(itemId)) {
		   mutex.leave();
		  throw new IllegalArgumentException(new Exception("No cumple la precondicion"));
	  }

	    //Inicializamos cada uno de los mapas con el producto, en caso de que nunca se haya hecho un ofrecerReabastecer  con ese producto

		if(enCamino.get(itemId)==null) {
			enCamino.put(itemId,0);
		}
		
		if(compradosGEN.get(itemId) == null) { 
	    	  compradosGEN.put(itemId, 0);
	    }
		if(disponibles.get(itemId) == null) { 
	    	  disponibles.put(itemId, 0);
	    }
	 
		//EVALUAMOS LA CPRE DE OFRECERREABASTECER 
	  if (disponibles.get(itemId)+enCamino.get(itemId) - compradosGEN.get(itemId) >= prodmin.get(itemId)) { 
		//Si no se cumple, y por lo tanto a entrado aqui, guardamos la peticion
		  PetBuf pet = new PetBuf(itemId,mutex);
		  espBufofrece.add(pet);//lo anadimos a nuestro buffer ofrecereabastecer, para luego sacarlo al hacer el desbloqueo
		  pet.reabastecer.await();//lo mantenemos a la espera
		  
	  } 
	  
	  //CUERPO DEL METODO
	   enCamino.put(itemId, enCamino.get(itemId)+cantidad);
		
	  desbloqueo_general();//Llamamos a desbloqueo_general en caso de que alguno de los parametros cambiados permiten liberar alguna peticion en espera 
		
 	  mutex.leave(); 

  }

  public void reabastecer(String itemId, int cantidad) {
	  
	 //Evaluamos la precondicion antes de entrar en el cuerpo del metodo, en caso de que esta no se cumpla, lance una excepcion
	 
	  
	  mutex.enter();
	  
	  if(cantidad <=0 || !prodmin.containsKey(itemId)) {
		   mutex.leave();
		  throw new IllegalArgumentException(new Exception("No cumple la precondicion"));
	  }
	 
	  //CUERPO DEL METODO
	  disponibles.put(itemId, disponibles.get(itemId)+cantidad);
	  enCamino.put(itemId, enCamino.get(itemId)-cantidad);
  
	  desbloqueo_general();//Llamamos a desbloqueo_general en caso de que alguno de los parametros cambiados permiten liberar alguna peticion en espera 
	
	  mutex.leave();
  }
  
  public void desbloqueo_general() {
	  //CREAMOS UN ITERADOR PARA CADA UNO DE NUESTROS BUFFERS QUE ALMACENAN NUESTRAS PETICIONES
	  Iterator<PetBuf> iteradorentrega = espBufentrega.iterator(); 
	  Iterator<PetBuf> iteradoreabastecer = espBufofrece.iterator();
	  
	  
	  boolean encontrado=false;
	  //inicializamos un mapa que usaremos para ver si este objeto ya se habia evaluado previamente
	  valoresiesta= new HashMap<String,String>();
	  
	  //recorremos nuestro iterador en busca de alguna peticion que desbloquear
	  while( iteradorentrega.hasNext() && !encontrado) {
		  
		  PetBuf p = iteradorentrega.next();	  
		  
		  if(!valoresiesta.containsKey(p.producto) && p.entrega.waiting() >0) { // si no esta en el mapa (no hemos evaluado ese producto), y ademas esta en la espera la evaluamos 
			  encontrado= disponibles.get(p.producto)>= p.cantidad;
		  }  
		  //lo anadimos en el mapa, como indicador de que ya hemos pasado por el 
		  valoresiesta.put(p.producto, p.producto);
		  
		  //Si se ha encontrado, cumple la cpre, por lo tanto hacemos un signal y lo quitamos de nuestro buffer de peticiones
		  if(encontrado) {
			  iteradorentrega.remove();
			  p.entrega.signal();
		  }
		  
		  
	  } 
	  

	  
	//recorremos nuestro iterador en busca de alguna peticion que desbloquear
	  while( iteradoreabastecer.hasNext()) {
		  PetBuf p = iteradoreabastecer.next();
		  
		  //Si se ha encontrado, cumple la cpre, por lo tanto hacemos un signal y lo quitamos de nuestro buffer de peticiones
		 if(disponibles.get(p.producto)+ enCamino.get(p.producto) - compradosGEN.get(p.producto) < prodmin.get(p.producto) && p.reabastecer.waiting() > 0) {
			 iteradoreabastecer.remove();
			 p.reabastecer.signal();
		 }
		 
			 
	     }
		 
	  }
	  
//---------------------------------------------------
//TRABAJO REALIZADO POR SIHAM TAHERI Y MIRIAM BERNAT 
//---------------------------------------------------
  }
 

 
 


